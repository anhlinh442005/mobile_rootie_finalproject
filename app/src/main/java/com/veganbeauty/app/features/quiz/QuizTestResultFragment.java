package com.veganbeauty.app.features.quiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.SkinHistoryLocalStore;
import com.veganbeauty.app.databinding.QuizTestResultBinding;
import com.veganbeauty.app.features.ai.SkinAiRoutineRecommender;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.data.local.SkinProfileMetricsHelper;
import com.veganbeauty.app.utils.CoinRewardDialogHelper;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.RewardPointsHelper;
import com.veganbeauty.app.utils.SkinHistoryIdHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class QuizTestResultFragment extends RootieFragment {

    private static final int QUIZ_REWARD_POINTS = 100;
    public static final String ARG_FROM_SKIN_PROFILE = "FROM_SKIN_PROFILE";
    public static final String ARG_IS_FIRST_TEST = "IS_FIRST_TEST";
    public static final String ARG_SKIN_TYPE = "SKIN_TYPE_RESULT";
    public static final String ARG_RECOMMENDATION = "RECOMMENDATION";
    public static final String ARG_FLAGGED_GROUPS = "FLAGGED_GROUPS";
    public static final String ARG_SENSITIVITY = "SENSITIVITY_PERCENT";
    public static final String ARG_HYDRATION = "HYDRATION_PERCENT";
    public static final String ARG_ELASTICITY = "ELASTICITY_PERCENT";
    public static final String ARG_SEBUM = "SEBUM_PERCENT";
    public static final String ARG_SKIN_AREAS = "SKIN_AREAS_DESC";

    private interface OnSkinProfileSavedListener {
        void onSaved(boolean rewardGranted, int rewardPoints);
    }

    private QuizTestResultBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private final String GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY;
    private final List<AiSkincareStep> morningSteps = new ArrayList<>();
    private final List<AiSkincareStep> eveningSteps = new ArrayList<>();
    private boolean isMorningTab = true;
    /** Đã ghi lịch sử quiz trong lần mở màn hình kết quả này. */
    private boolean historyLoggedThisOpen = false;
    /** Đủ điều kiện thưởng (tính trước khi auto-save cập nhật lastTestTime). */
    private boolean pendingQuizReward = false;
    private boolean quizRewardGrantedThisOpen = false;
    /** Chờ onResume rồi mới cộng xu — tránh show dialog khi fragment chưa ổn định (crash → ra Welcome). */
    private boolean quizRewardPendingUntilResume = false;
    /** Hoãn load AI routine đến sau khi đóng popup xu (tránh OOM). */
    private boolean aiRoutinePendingAfterReward = false;
    private String pendingAiSkinType;
    private int pendingAiHydration;
    private int pendingAiSebum;
    private int pendingAiSensitivity;
    private int pendingAiElasticity;
    private Set<String> pendingAiFlagged;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        if (binding == null) return;

        try {
            setupReportUi();
        } catch (Exception e) {
            e.printStackTrace();
            if (isAdded()) {
                android.widget.Toast.makeText(
                        requireContext(),
                        "Không thể mở báo cáo. Vui lòng thử lại.",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                // Không popBackStack — tránh nhảy màn / mất session cảm giác "bị out"
            }
        }
    }

    private void setupReportUi() {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        Bundle args = getArguments();
        boolean fromSkinProfile = args != null && args.getBoolean(ARG_FROM_SKIN_PROFILE, false);
        boolean isFirstTest = args != null && args.getBoolean(ARG_IS_FIRST_TEST, false);

        ReportSnapshot snapshot = resolveReportSnapshot(ctx, prefs, args, fromSkinProfile);
        String skinType = snapshot.skinType;
        String recommendation = snapshot.recommendation;
        Set<String> flaggedSet = snapshot.flaggedGroups;
        int sensitivity = snapshot.sensitivity;
        int hydration = snapshot.hydration;
        int elasticity = snapshot.elasticity;
        int sebum = snapshot.sebum;

        binding.tvSkinTypeResult.setText(skinType);
        binding.tvRecommendation.setText(recommendation);
        binding.tvSkinTypeDesc.setText(getSkinTypeDesc(skinType));

        // Lưu hồ sơ da ngay; cộng xu hoãn tới onResume để tránh crash khi show dialog sớm
        if (!fromSkinProfile) {
            ProfileSession.touchSession(ctx);
            pendingQuizReward = ProfileSession.isQuizRewardEligible(ctx);
            persistSkinProfileData(prefs, skinType, recommendation, flaggedSet,
                    sensitivity, hydration, elasticity, sebum, true);
            if (pendingQuizReward) {
                quizRewardPendingUntilResume = true;
            }
        }

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        String finalSkinType = skinType;
        String finalRecommendation = recommendation;
        Set<String> finalFlaggedSet = flaggedSet;

        if (fromSkinProfile) {
            binding.llAiRoutineSection.setVisibility(View.GONE);
            binding.llFooterButtons.setVisibility(View.GONE);
            binding.btnApplyRoutine.setVisibility(View.GONE);
            binding.btnDone.setVisibility(View.GONE);
            populateReportSectionsDeferred(finalFlaggedSet);
        } else {
            binding.llAiRoutineSection.setVisibility(View.VISIBLE);
            setupAiRoutineListeners(prefs, finalSkinType, finalRecommendation, finalFlaggedSet,
                    sensitivity, hydration, elasticity, sebum, false);

            if (isFirstTest) {
                binding.llFooterButtons.setVisibility(View.GONE);
                binding.btnApplyRoutine.setVisibility(View.VISIBLE);
            } else {
                // Vừa hoàn thành quiz tuần này — không hiện nút làm lại trên màn kết quả
                binding.llFooterButtons.setVisibility(View.VISIBLE);
                binding.btnSaveProfile.setVisibility(View.VISIBLE);
                binding.btnSuggestRoutine.setVisibility(View.VISIBLE);
                binding.btnApplyRoutine.setVisibility(View.GONE);
                binding.btnDone.setVisibility(View.GONE);
                binding.btnSaveProfile.setOnClickListener(v ->
                        saveSkinProfile(prefs, finalSkinType, finalRecommendation, finalFlaggedSet,
                                sensitivity, hydration, elasticity, sebum, this::onSaveProfileFinished));
                binding.btnSuggestRoutine.setOnClickListener(v -> {
                    if (binding.quizScroll != null && binding.llAiRoutineSection != null) {
                        binding.quizScroll.smoothScrollTo(0, binding.llAiRoutineSection.getTop());
                    }
                    if (morningSteps.isEmpty() && eveningSteps.isEmpty()) {
                        loadAiRoutine(finalSkinType, hydration, sebum, sensitivity, elasticity, finalFlaggedSet);
                    }
                });
            }

            // Hoãn AI routine đến sau popup xu — tránh OOM (ảnh túi tiền + products.json cùng lúc)
            if (pendingQuizReward) {
                aiRoutinePendingAfterReward = true;
                pendingAiSkinType = finalSkinType;
                pendingAiHydration = hydration;
                pendingAiSebum = sebum;
                pendingAiSensitivity = sensitivity;
                pendingAiElasticity = elasticity;
                pendingAiFlagged = finalFlaggedSet != null ? new HashSet<>(finalFlaggedSet) : new HashSet<>();
                if (binding.llAiRoutineStepsContainer != null) {
                    showRoutineLoading(true);
                }
            } else {
                binding.getRoot().postDelayed(() -> {
                    if (!isAdded() || binding == null) return;
                    loadAiRoutine(finalSkinType, hydration, sebum, sensitivity, elasticity, finalFlaggedSet);
                }, 400);
            }
            populateReportSectionsDeferred(finalFlaggedSet);
        }

        binding.cardAiAdvice.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.ai.SkinAiChatFragment())
                        .addToBackStack(null).commitAllowingStateLoss());

        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_myskin, tabId -> {
            BottomNavHelper.navigate(this, tabId);
        });

        setupScrollHideHeader();
    }

    private void populateReportSectionsDeferred(Set<String> flaggedGroups) {
        if (binding == null) {
            return;
        }
        binding.getRoot().post(() -> {
            if (!isAdded() || binding == null) {
                return;
            }
            try {
                processIngredients(flaggedGroups);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Gợi ý SP chạy nền — tránh OOM khi đọc products.json trên main thread
            recommendProductsAsync(flaggedGroups);
        });
    }

    private void recommendProductsAsync(Set<String> flaggedGroups) {
        final Context appContext = requireContext().getApplicationContext();
        final Set<String> flaggedCopy = flaggedGroups != null ? new HashSet<>(flaggedGroups) : new HashSet<>();
        new Thread(() -> {
            List<ProductRecommendItem> items = buildProductRecommendations(appContext, flaggedCopy);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) return;
                try {
                    bindProductRecommendations(items);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    private static final class ProductRecommendItem {
        final String name;
        final String mainImage;
        final String badge;
        final String badgeColor;
        final int badgeIcon;
        final String desc;

        ProductRecommendItem(String name, String mainImage, String badge, String badgeColor,
                             int badgeIcon, String desc) {
            this.name = name;
            this.mainImage = mainImage;
            this.badge = badge;
            this.badgeColor = badgeColor;
            this.badgeIcon = badgeIcon;
            this.desc = desc;
        }
    }

    @NonNull
    private List<ProductRecommendItem> buildProductRecommendations(@NonNull Context context,
                                                                   @NonNull Set<String> flaggedGroups) {
        List<ProductRecommendItem> result = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("products.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray jsonArray = root.getJSONArray("products");

            BufferedReader br2 = new BufferedReader(new InputStreamReader(context.getAssets().open("quiz_thanhphan.json")));
            StringBuilder sb2 = new StringBuilder();
            String line2;
            while ((line2 = br2.readLine()) != null) sb2.append(line2);
            br2.close();
            JSONObject tpObject = new JSONObject(sb2.toString());
            JSONArray tpArray = tpObject.getJSONArray("ingredients");

            Set<String> avoidChemicals = new HashSet<>();
            Set<String> cautionChemicals = new HashSet<>();
            for (int i = 0; i < tpArray.length(); i++) {
                JSONObject ing = tpArray.getJSONObject(i);
                String name = ing.getString("name");
                String category = ing.getString("category");
                String risk = ing.getString("risk");
                if (flaggedGroups.contains(category)) {
                    if ("avoid".equals(risk)) avoidChemicals.add(name.toLowerCase(Locale.ROOT));
                    else if ("caution".equals(risk)) cautionChemicals.add(name.toLowerCase(Locale.ROOT));
                }
            }

            int count = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                if (count >= 5) break;
                JSONObject prodObj = jsonArray.getJSONObject(i);
                String prodName = prodObj.getString("name");
                String mainImage = prodObj.optString("mainImage", "");

                List<String> detailedIngredientsList = new ArrayList<>();
                if (prodObj.has("detailedIngredients")) {
                    JSONArray detailedArray = prodObj.getJSONArray("detailedIngredients");
                    for (int j = 0; j < detailedArray.length(); j++) {
                        detailedIngredientsList.add(detailedArray.getString(j).toLowerCase(Locale.ROOT));
                    }
                }

                List<String> triggeredAvoids = new ArrayList<>();
                List<String> triggeredCautions = new ArrayList<>();
                for (String chem : avoidChemicals) {
                    for (String ing : detailedIngredientsList) {
                        if (ing.contains(chem)) triggeredAvoids.add(chem);
                    }
                }
                for (String chem : cautionChemicals) {
                    for (String ing : detailedIngredientsList) {
                        if (ing.contains(chem)) triggeredCautions.add(chem);
                    }
                }

                String badge;
                String badgeColor;
                int badgeIcon;
                String desc;
                if (!triggeredAvoids.isEmpty()) {
                    badgeIcon = R.drawable.ic_warning_triangle;
                    badge = "Nguy cơ kích ứng cao";
                    badgeColor = "#F04758";
                    Set<String> uniqueNames = new HashSet<>();
                    for (String c : triggeredAvoids) uniqueNames.add(getViName(c));
                    desc = "Sản phẩm chứa " + String.join(", ", uniqueNames)
                            + " không phù hợp với da nhạy cảm của bạn.";
                } else if (!triggeredCautions.isEmpty()) {
                    badgeIcon = R.drawable.ic_warning_outline;
                    badge = "Cần thận trọng khi dùng";
                    badgeColor = "#E29400";
                    Set<String> uniqueNames = new HashSet<>();
                    for (String c : triggeredCautions) uniqueNames.add(getViName(c));
                    desc = "Sản phẩm chứa " + String.join(", ", uniqueNames)
                            + " cần lưu ý theo dõi khi sử dụng.";
                } else {
                    badgeIcon = R.drawable.ic_check;
                    badge = "Lành tính - Khuyên dùng";
                    badgeColor = "#375633";
                    desc = "Công thức tối giản, lành tính giúp củng cố lớp màng ẩm tự nhiên mà không gây bí da.";
                }
                result.add(new ProductRecommendItem(prodName, mainImage, badge, badgeColor, badgeIcon, desc));
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void bindProductRecommendations(@NonNull List<ProductRecommendItem> items) {
        if (binding == null) return;
        binding.llProductsContainer.removeAllViews();
        for (ProductRecommendItem item : items) {
            View itemView = LayoutInflater.from(getContext())
                    .inflate(R.layout.quiz_item_product_recommendation, binding.llProductsContainer, false);
            TextView tvProdName = itemView.findViewById(R.id.tv_product_name);
            TextView tvProdDesc = itemView.findViewById(R.id.tv_product_desc);
            TextView tvBadge = itemView.findViewById(R.id.tv_compatibility_badge);
            ImageView ivBadgeIcon = itemView.findViewById(R.id.iv_badge_icon);
            ImageView ivImage = itemView.findViewById(R.id.iv_product_image);

            tvProdName.setText(item.name);
            tvProdDesc.setText(item.desc);
            tvBadge.setText(item.badge);
            tvBadge.setTextColor(Color.parseColor(item.badgeColor));
            ivBadgeIcon.setImageResource(item.badgeIcon);
            ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(item.badgeColor)));

            if (item.mainImage != null && !item.mainImage.isEmpty()) {
                com.bumptech.glide.Glide.with(ivImage.getContext())
                        .load(item.mainImage)
                        .placeholder(R.drawable.myphamxanh)
                        .into(ivImage);
            } else {
                ivImage.setImageResource(R.drawable.myphamxanh);
            }
            binding.llProductsContainer.addView(itemView);
        }
    }

    private void recommendProducts(Set<String> flaggedGroups) {
        // Giữ method cũ cho tương thích — chuyển sang async
        recommendProductsAsync(flaggedGroups);
    }

    private static final class ReportSnapshot {
        final String skinType;
        final String recommendation;
        final Set<String> flaggedGroups;
        final int sensitivity;
        final int hydration;
        final int elasticity;
        final int sebum;

        ReportSnapshot(String skinType, String recommendation, Set<String> flaggedGroups,
                       int sensitivity, int hydration, int elasticity, int sebum) {
            this.skinType = skinType;
            this.recommendation = recommendation;
            this.flaggedGroups = flaggedGroups;
            this.sensitivity = sensitivity;
            this.hydration = hydration;
            this.elasticity = elasticity;
            this.sebum = sebum;
        }
    }

    @NonNull
    private ReportSnapshot resolveReportSnapshot(@NonNull Context ctx,
                                                 @NonNull SharedPreferences prefs,
                                                 @Nullable Bundle args,
                                                 boolean fromSkinProfile) {
        if (args != null && args.containsKey(ARG_SKIN_TYPE)) {
            return snapshotFromArgs(args);
        }
        // Màn kết quả sau quiz: luôn lấy prefs quiz vừa làm, không lấy hồ sơ cũ
        if (fromSkinProfile) {
            return snapshotFromSavedProfile(ctx);
        }
        return snapshotFromQuizPrefs(prefs);
    }

    @NonNull
    private ReportSnapshot snapshotFromArgs(@NonNull Bundle args) {
        String skinType = args.getString(ARG_SKIN_TYPE, "Da thường");
        String recommendation = args.getString(ARG_RECOMMENDATION,
                "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày.");
        ArrayList<String> flaggedList = args.getStringArrayList(ARG_FLAGGED_GROUPS);
        Set<String> flaggedGroups = flaggedList != null ? new HashSet<>(flaggedList) : new HashSet<>();
        return new ReportSnapshot(
                skinType,
                recommendation,
                flaggedGroups,
                args.getInt(ARG_SENSITIVITY, 50),
                args.getInt(ARG_HYDRATION, 50),
                args.getInt(ARG_ELASTICITY, 75),
                args.getInt(ARG_SEBUM, 50)
        );
    }

    @NonNull
    private ReportSnapshot snapshotFromSavedProfile(@NonNull Context ctx) {
        SkinWeatherProfileHelper.UserSkinProfile profile = SkinWeatherProfileHelper.load(ctx);
        Set<String> flaggedGroups = profile.flaggedGroups != null
                ? new HashSet<>(profile.flaggedGroups)
                : new HashSet<>();
        String skinType = profile.skinType != null && !profile.skinType.trim().isEmpty()
                ? profile.skinType
                : "Da thường";
        String recommendation = profile.recommendation != null && !profile.recommendation.isEmpty()
                ? profile.recommendation
                : "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày.";
        return new ReportSnapshot(
                skinType,
                recommendation,
                flaggedGroups,
                profile.sensitivity,
                profile.hydration,
                profile.elasticity,
                profile.sebum
        );
    }

    @NonNull
    private ReportSnapshot snapshotFromQuizPrefs(@NonNull SharedPreferences prefs) {
        Set<String> storedFlagged = prefs.getStringSet("FLAGGED_GROUPS", null);
        Set<String> flaggedGroups = storedFlagged != null ? new HashSet<>(storedFlagged) : new HashSet<>();
        return new ReportSnapshot(
                prefs.getString("SKIN_TYPE_RESULT", "Da thường"),
                prefs.getString("RECOMMENDATION",
                        "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày."),
                flaggedGroups,
                prefs.getInt("SENSITIVITY_PERCENT", 50),
                prefs.getInt("HYDRATION_PERCENT", 50),
                prefs.getInt("ELASTICITY_PERCENT", 75),
                prefs.getInt("SEBUM_PERCENT", 50)
        );
    }

    @NonNull
    private String getSkinTypeDesc(@Nullable String skinType) {
        if (skinType == null || skinType.trim().isEmpty()) {
            return "Làn da cần được chăm sóc cân bằng và bảo vệ hàng ngày với các sản phẩm phù hợp.";
        }
        String lower = skinType.toLowerCase(Locale.getDefault());
        if (lower.contains("dầu") && lower.contains("nhạy cảm")) {
            return "Làn da của bạn tiết nhiều dầu nhờn, lỗ chân lông to và rất dễ bị kích ứng, đỏ rát hoặc nổi mẩn khi gặp thành phần hoạt chất mạnh hoặc thay đổi thời tiết.";
        }
        if (lower.contains("khô") && lower.contains("nhạy cảm")) {
            return "Làn da thiếu ẩm, thường xuyên bong tróc, căng rát và có hàng rào bảo vệ da yếu, cực kỳ nhạy cảm với cồn khô và hương liệu.";
        }
        if (lower.contains("mụn")) {
            return "Nền da dễ bị bít tắc lỗ chân lông, sinh nhân mụn đầu đen, mụn ẩn hoặc mụn viêm. Cần chú trọng làm sạch dịu nhẹ và kháng khuẩn.";
        }
        if (lower.contains("mất nước")) {
            return "Da có thể tiết dầu nhưng bề mặt vẫn căng khô, thô ráp do thiếu hụt lượng nước trong tế bào biểu bì.";
        }
        if (lower.contains("nhạy cảm") || lower.contains("kích ứng")) {
            return "Làn da có hàng rào bảo vệ mỏng yếu, dễ đỏ rát, châm chích khi thay đổi thời tiết hoặc sử dụng sản phẩm có cồn/hương liệu.";
        }
        if (lower.contains("lão hóa")) {
            return "Làn da bắt đầu xuất hiện các nếp nhăn nông sâu, độ đàn hồi kém, có thể có sạm nám và cần bổ sung chất chống oxy hóa mạnh.";
        }
        if (lower.contains("khô")) {
            return "Làn da thiếu hụt độ ẩm tự nhiên, thường xuyên có cảm giác căng chặt, bong tróc vảy nhỏ và dễ hình thành nếp nhăn sớm.";
        }
        if (lower.contains("dầu")) {
            return "Lượng bã nhờn hoạt động quá mức gây bóng loáng toàn mặt, lỗ chân lông to và dễ bám bụi bẩn hình thành mụn.";
        }
        if (lower.contains("hỗn hợp")) {
            return "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu nhờn, trong khi vùng chữ U (hai bên má) lại khô hoặc bình thường.";
        }
        if (lower.contains("thường")) {
            return "Làn da lý tưởng với độ ẩm cân bằng, lỗ chân lông nhỏ, da mịn màng khỏe mạnh và ít khi gặp các vấn đề kích ứng.";
        }
        return "Làn da cần được chăm sóc cân bằng và bảo vệ hàng ngày với các sản phẩm phù hợp.";
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.rlHeader,
                binding.quizScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.quizScroll);
    }

    private void addPill(ViewGroup container, String text, int backgroundResId, String textColorStr) {
        TextView pillView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_pill, container, false);
        pillView.setText(text);
        pillView.setBackgroundResource(backgroundResId);
        pillView.setTextColor(Color.parseColor(textColorStr));
        container.addView(pillView);
    }

    private void processIngredients(Set<String> flaggedGroups) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("quiz_thanhphan.json")));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray ingredientsArray = jsonObject.getJSONArray("ingredients");

            List<String> avoidList = new ArrayList<>();
            List<String> cautionList = new ArrayList<>();
            List<String> safeList = new ArrayList<>();

            for (int i = 0; i < ingredientsArray.length(); i++) {
                JSONObject ing = ingredientsArray.getJSONObject(i);
                String viName = ing.getString("vi");
                String category = ing.getString("category");
                String risk = ing.getString("risk");

                if (flaggedGroups.contains(category)) {
                    if ("avoid".equals(risk)) avoidList.add(viName);
                    else if ("caution".equals(risk)) cautionList.add(viName);
                    else if ("safe".equals(risk)) safeList.add(viName);
                } else {
                    if ("safe".equals(risk)) safeList.add(viName);
                    else if ("caution".equals(risk)) cautionList.add(viName);
                }
            }

            binding.llAvoidPillsContainer.removeAllViews();
            binding.llCautionPillsContainer.removeAllViews();
            binding.llSuitablePillsContainer.removeAllViews();

            Set<String> uniqueAvoid = new HashSet<>(avoidList);
            for (String item : uniqueAvoid) addPill(binding.llAvoidPillsContainer, item, R.drawable.quiz_bg_pill_avoid, "#E91B2F");
            if (uniqueAvoid.isEmpty()) {
                addPill(binding.llAvoidPillsContainer, "Cồn khô", R.drawable.quiz_bg_pill_avoid, "#E91B2F");
                addPill(binding.llAvoidPillsContainer, "Hương liệu", R.drawable.quiz_bg_pill_avoid, "#E91B2F");
            }

            Set<String> uniqueCaution = new HashSet<>(cautionList);
            for (String item : uniqueCaution) addPill(binding.llCautionPillsContainer, item, R.drawable.quiz_bg_pill_caution, "#677559");
            if (uniqueCaution.isEmpty()) {
                addPill(binding.llCautionPillsContainer, "Retinol", R.drawable.quiz_bg_pill_caution, "#677559");
                addPill(binding.llCautionPillsContainer, "BHA", R.drawable.quiz_bg_pill_caution, "#677559");
            }

            Set<String> uniqueSafe = new HashSet<>(safeList);
            for (String item : uniqueSafe) addPill(binding.llSuitablePillsContainer, item, R.drawable.quiz_bg_pill_suitable, "#67814D");
            if (uniqueSafe.isEmpty()) {
                addPill(binding.llSuitablePillsContainer, "Glycerin", R.drawable.quiz_bg_pill_suitable, "#67814D");
                addPill(binding.llSuitablePillsContainer, "HA", R.drawable.quiz_bg_pill_suitable, "#67814D");
                addPill(binding.llSuitablePillsContainer, "Rau má", R.drawable.quiz_bg_pill_suitable, "#67814D");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getViName(String chemicalName) {
        String lower = chemicalName.toLowerCase();
        switch (lower) {
            case "alcohol denat": case "ethanol": return "Cồn khô";
            case "fragrance": return "Hương liệu";
            case "essential oil": return "Tinh dầu";
            case "sodium lauryl sulfate": return "Chất tẩy rửa mạnh (SLS)";
            case "sodium laureth sulfate": return "Chất làm sạch mạnh";
            case "cocamidopropyl betaine": return "Chất tạo bọt";
            case "retinol": return "Retinol";
            case "salicylic acid": return "BHA";
            case "glycolic acid": return "AHA";
            case "phenoxyethanol": case "paraben": return "Chất bảo quản";
            case "propylene glycol": return "Propylene Glycol";
            case "glycerin": return "Glycerin";
            case "hyaluronic acid": return "HA";
            case "centella asiatica": return "Rau má";
            case "green tea": return "Trà xanh";
            case "aloe vera": return "Nha đam";
            case "silicone": return "Silicone";
            case "petrolatum": return "Vaseline";
            default: return chemicalName;
        }
    }

    private void setupAiRoutineListeners(SharedPreferences prefs, String skinType, String recommendation,
                                         Set<String> flaggedSet, int sensitivity, int hydration,
                                         int elasticity, int sebum, boolean fromSkinProfile) {
        binding.tabMorning.setOnClickListener(v -> {
            if (!isMorningTab) {
                isMorningTab = true;
                updateTabUI();
                populateSteps();
            }
        });
        binding.tabEvening.setOnClickListener(v -> {
            if (isMorningTab) {
                isMorningTab = false;
                updateTabUI();
                populateSteps();
            }
        });
        binding.btnApplyRoutine.setOnClickListener(v ->
                saveSelectedStepsToProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum));
        if (fromSkinProfile) {
            binding.tvRetakeQuizInline.setVisibility(View.GONE);
            return;
        }
        // Màn kết quả sau khi làm quiz — ẩn làm lại (cooldown 7 ngày)
        binding.tvRetakeQuizInline.setVisibility(View.GONE);
    }

    private void updateTabUI() {
        if (isMorningTab) {
            binding.tabMorning.setBackgroundResource(R.drawable.bg_btn_buy);
            binding.tabMorning.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            binding.tvTabMorning.setTextColor(Color.WHITE);
            binding.tabEvening.setBackgroundResource(android.R.color.transparent);
            binding.tabEvening.setBackgroundTintList(null);
            binding.tvTabEvening.setTextColor(Color.parseColor("#677559"));
        } else {
            binding.tabEvening.setBackgroundResource(R.drawable.bg_btn_buy);
            binding.tabEvening.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            binding.tvTabEvening.setTextColor(Color.WHITE);
            binding.tabMorning.setBackgroundResource(android.R.color.transparent);
            binding.tabMorning.setBackgroundTintList(null);
            binding.tvTabMorning.setTextColor(Color.parseColor("#677559"));
        }
    }

    private void loadAiRoutine(String skinType, int hydration, int sebum, int sensitivity, int elasticity, Set<String> flaggedSet) {
        if (binding == null || !isAdded()) return;
        try {
            showRoutineLoading(true);
            Context appContext = requireContext().getApplicationContext();
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                    BuildersKt.withContext(Dispatchers.getIO(), (s2, c2) -> {
                        SkinAiRoutineRecommender.RoutinePlan plan = null;
                        try {
                            plan = SkinAiRoutineRecommender.recommend(
                                    appContext, skinType, hydration, sebum, sensitivity, elasticity,
                                    flaggedSet, GEMINI_API_KEY);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        SkinAiRoutineRecommender.RoutinePlan finalPlan = plan;
                        BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                            if (!isAdded() || binding == null) return kotlin.Unit.INSTANCE;
                            try {
                                showRoutineLoading(false);
                                if (finalPlan != null) {
                                    applyRoutinePlan(finalPlan);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return kotlin.Unit.INSTANCE;
                        }, c2);
                        return kotlin.Unit.INSTANCE;
                    }, cont));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                showRoutineLoading(false);
            } catch (Exception ignored) {
            }
        }
    }

    private void showRoutineLoading(boolean loading) {
        if (binding == null) return;
        if (!loading) return;
        binding.llAiRoutineStepsContainer.removeAllViews();
        TextView loadingView = new TextView(requireContext());
        loadingView.setText("Rootie đang phân tích da và gợi ý routine phù hợp...");
        loadingView.setTextColor(Color.parseColor("#677559"));
        loadingView.setTextSize(14f);
        loadingView.setPadding(0, 24, 0, 24);
        binding.llAiRoutineStepsContainer.addView(loadingView);
    }

    private void applyRoutinePlan(SkinAiRoutineRecommender.RoutinePlan plan) {
        if (plan == null || binding == null) return;

        morningSteps.clear();
        for (int i = 0; i < plan.morningSteps.size(); i++) {
            SkinAiRoutineRecommender.RoutineStep step = plan.morningSteps.get(i);
            morningSteps.add(new AiSkincareStep(i, step.stepName, step.productName, step.reason, true));
        }

        eveningSteps.clear();
        for (int i = 0; i < plan.eveningSteps.size(); i++) {
            SkinAiRoutineRecommender.RoutineStep step = plan.eveningSteps.get(i);
            eveningSteps.add(new AiSkincareStep(i, step.stepName, step.productName, step.reason, true));
        }

        if (plan.assessment != null && !plan.assessment.trim().isEmpty() && isAdded() && binding != null) {
            binding.tvRecommendation.setText(plan.assessment);
            requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
                    .edit().putString("RECOMMENDATION", plan.assessment).apply();
        }

        populateSteps();
    }

    private void populateSteps() {
        if (binding == null) return;
        binding.llAiRoutineStepsContainer.removeAllViews();
        List<AiSkincareStep> stepsList = isMorningTab ? morningSteps : eveningSteps;

        for (AiSkincareStep step : stepsList) {
            View stepView = LayoutInflater.from(requireContext()).inflate(R.layout.quiz_item_ai_routine_step, binding.llAiRoutineStepsContainer, false);

            ImageView ivCheckbox = stepView.findViewById(R.id.iv_step_checkbox);
            TextView tvIndex = stepView.findViewById(R.id.tv_step_index);
            TextView tvName = stepView.findViewById(R.id.tv_step_name);
            TextView tvProduct = stepView.findViewById(R.id.tv_recommended_product);
            TextView tvReason = stepView.findViewById(R.id.tv_ai_reason);
            View layoutCard = stepView.findViewById(R.id.layout_step_card);

            tvIndex.setText(String.valueOf(step.getIndex() + 1));
            tvName.setText(step.getName());
            tvProduct.setText(step.getRecommendedProduct());
            tvReason.setText(step.getDescription());

            ivCheckbox.setImageResource(step.isChecked() ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);
            ivCheckbox.setImageTintList(step.isChecked()
                    ? ColorStateList.valueOf(Color.parseColor("#3E4D44"))
                    : ColorStateList.valueOf(Color.parseColor("#D9D9D9")));

            View.OnClickListener clickListener = v -> {
                step.setChecked(!step.isChecked());
                ivCheckbox.setImageResource(step.isChecked() ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);
                ivCheckbox.setImageTintList(step.isChecked()
                        ? ColorStateList.valueOf(Color.parseColor("#3E4D44"))
                        : ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            };

            ivCheckbox.setOnClickListener(clickListener);
            layoutCard.setOnClickListener(clickListener);

            binding.llAiRoutineStepsContainer.addView(stepView);
        }
    }

    private void persistSkinProfileData(
            SharedPreferences prefs,
            String skinType,
            String recommendation,
            Set<String> flaggedSet,
            int sensitivity,
            int hydration,
            int elasticity,
            int sebum,
            boolean appendHistory
    ) {
        String skinAreas = prefs.getString("SKIN_AREAS_DESC",
                "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da.");
        long currentTime = System.currentTimeMillis();

        prefs.edit()
                .putString("SAVED_USER_SKIN_TYPE", skinType)
                .putString("SAVED_RECOMMENDATION", recommendation)
                .putStringSet("SAVED_FLAGGED_GROUPS", flaggedSet)
                .putInt("SAVED_SENSITIVITY", sensitivity)
                .putInt("SAVED_HYDRATION", hydration)
                .putInt("SAVED_ELASTICITY", elasticity)
                .putInt("SAVED_SEBUM", sebum)
                .putString("SAVED_SKIN_AREAS", skinAreas)
                .putLong("KEY_LAST_SKIN_TEST_TIME", currentTime)
                .putBoolean("KEY_HIDE_QUIZ_REMINDER_WEEKLY", false)
                .commit();

        if (!appendHistory || historyLoggedThisOpen) {
            return;
        }
        historyLoggedThisOpen = true;

        try {
            String historyStr = prefs.getString("QUIZ_HISTORY_LIST", "[]");
            if (historyStr == null) historyStr = "[]";
            JSONArray historyArray = new JSONArray(historyStr);

            Date now = new Date();
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now);
            String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now);

            JSONObject newLog = new JSONObject();
            newLog.put("id", SkinHistoryIdHelper.generateId());
            newLog.put("date", dateStr);
            newLog.put("time", timeStr);
            newLog.put("skinType", skinType);
            newLog.put("recommendation", recommendation);
            newLog.put("sensitivity", sensitivity);
            newLog.put("hydration", hydration);
            newLog.put("elasticity", elasticity);
            newLog.put("sebum", sebum);
            newLog.put("score", SkinProfileMetricsHelper.computeOverallScore(newLog));
            newLog.put("scanType", "Test da");

            historyArray.put(newLog);
            String historyJson = historyArray.toString();
            prefs.edit().putString("QUIZ_HISTORY_LIST", historyJson).apply();

            Context appContext = requireContext().getApplicationContext();
            ProfileSession.setQuizHistoryList(appContext, historyJson);
            String email = ProfileSession.getEmail(appContext);
            String userId = ProfileSession.getUserId(appContext);
            // Luôn lưu Room khi đã đăng nhập — nguồn so sánh cũ/mới trên lịch sử da
            if ((email != null && !email.trim().isEmpty())
                    || (userId != null && !userId.trim().isEmpty())) {
                SkinHistoryLocalStore.save(
                        appContext,
                        newLog,
                        userId != null ? userId : "",
                        email != null ? email : ""
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSkinProfile(SharedPreferences prefs, String skinType, String recommendation, Set<String> flaggedSet, int sensitivity, int hydration, int elasticity, int sebum, @Nullable OnSkinProfileSavedListener listener) {
        ProfileSession.touchSession(requireContext());
        persistSkinProfileData(prefs, skinType, recommendation, flaggedSet,
                sensitivity, hydration, elasticity, sebum, false);

        if (pendingQuizReward && !quizRewardGrantedThisOpen) {
            // Chưa cộng lúc mở màn hình → cộng khi Lưu/Áp dụng
            grantQuizRewardIfPending(false, listener);
        } else if (listener != null && isAdded()) {
            // Đã cộng xu lúc mở kết quả, hoặc không đủ điều kiện
            listener.onSaved(false, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!quizRewardPendingUntilResume || quizRewardGrantedThisOpen) {
            return;
        }
        quizRewardPendingUntilResume = false;
        View root = getView();
        if (root == null) {
            grantQuizRewardIfPending(true, null);
            return;
        }
        // Đợi màn kết quả ổn định rồi mới cộng xu
        root.postDelayed(() -> {
            if (!isAdded() || quizRewardGrantedThisOpen) {
                return;
            }
            grantQuizRewardIfPending(true, null);
        }, 600);
    }

    /**
     * Cộng 100 xu quiz trên background thread (Room), rồi hiện popup full-screen.
     * Xu được ghi vào user_coin theo userId hiện tại — hồ sơ / đổi quà / giỏ hàng đọc cùng nguồn.
     */
    private void grantQuizRewardIfPending(boolean showDialogNow,
                                          @Nullable OnSkinProfileSavedListener listener) {
        if (!pendingQuizReward || quizRewardGrantedThisOpen || !isAdded()) {
            if (listener != null && isAdded()) {
                listener.onSaved(false, 0);
            }
            return;
        }
        quizRewardGrantedThisOpen = true;
        pendingQuizReward = false;

        final Context appContext = requireContext().getApplicationContext();
        ProfileSession.touchSession(appContext);
        final androidx.fragment.app.FragmentActivity hostActivity = getActivity();
        if (hostActivity == null || hostActivity.isFinishing()) {
            quizRewardGrantedThisOpen = false;
            pendingQuizReward = true;
            if (listener != null) {
                listener.onSaved(false, 0);
            }
            return;
        }

        new Thread(() -> {
            boolean success = false;
            int totalBalance = 0;
            try {
                ProfileSessionHelper.ensureCurrentUserInDatabase(appContext);
                String userId = ProfileSessionHelper.getEffectiveUserId(appContext);
                if (userId == null || userId.trim().isEmpty()) {
                    userId = ProfileSession.getUserId(appContext);
                }
                if (userId == null || userId.trim().isEmpty()) {
                    throw new IllegalStateException("Quiz reward skipped: user not logged in");
                }
                totalBalance = RewardPointsHelper.awardPoints(
                        appContext,
                        "SYSTEM_WEEKLY_QUIZ_" + System.currentTimeMillis(),
                        QUIZ_REWARD_POINTS,
                        "Cập nhật làn da định kỳ (+" + QUIZ_REWARD_POINTS + " xu)",
                        "từ quiz cập nhật chỉ số da",
                        false
                );
                int verified = RewardPointsHelper.getTotalPoints(appContext);
                if (verified >= totalBalance) {
                    totalBalance = verified;
                }
                ProfileSession.markQuizRewardGranted(appContext);
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            final boolean awarded = success;
            final int total = totalBalance;
            hostActivity.runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                if (!awarded) {
                    quizRewardGrantedThisOpen = false;
                    pendingQuizReward = true;
                    schedulePendingAiRoutineAfterReward();
                    if (listener != null) {
                        listener.onSaved(false, 0);
                    }
                    return;
                }
                if (showDialogNow) {
                    showSafeCoinRewardDialog(QUIZ_REWARD_POINTS, total, "từ quiz cập nhật chỉ số da", null);
                } else if (listener == null) {
                    // Không có dialog / listener → hoãn AI routine luôn
                    schedulePendingAiRoutineAfterReward();
                }
                if (listener != null) {
                    listener.onSaved(true, QUIZ_REWARD_POINTS);
                }
            });
        }).start();
    }

    private void onSaveProfileFinished(boolean rewardGranted, int rewardPoints) {
        if (!isAdded()) return;
        if (rewardGranted) {
            // Ở lại màn kết quả — không popBackStack / không out app
            showSafeCoinRewardDialog(rewardPoints,
                    com.veganbeauty.app.utils.RewardPointsHelper.getTotalPoints(requireContext()),
                    "từ quiz cập nhật chỉ số da",
                    null);
        } else {
            showSavedProfileDialog();
        }
    }

    /** Popup xu full-screen (giống Routine) — tránh AlertDialog nhỏ bị chạm nhầm bottom nav về Home. */
    private void showSafeCoinRewardDialog(int rewardPoints, int totalBalance,
                                          @Nullable String source, @Nullable Runnable onDismiss) {
        if (!isAdded() || rewardPoints <= 0) {
            schedulePendingAiRoutineAfterReward();
            if (onDismiss != null) onDismiss.run();
            return;
        }
        try {
            CoinRewardDialogHelper.showWithDismissCallback(
                    this,
                    rewardPoints,
                    totalBalance,
                    source,
                    () -> {
                        schedulePendingAiRoutineAfterReward();
                        if (onDismiss != null && isAdded()) {
                            onDismiss.run();
                        }
                    }
            );
        } catch (OutOfMemoryError | Exception e) {
            e.printStackTrace();
            try {
                android.widget.Toast.makeText(
                        requireContext(),
                        "Bạn nhận +" + rewardPoints + " xu!",
                        android.widget.Toast.LENGTH_LONG
                ).show();
            } catch (Exception ignored) {
            }
            schedulePendingAiRoutineAfterReward();
            if (onDismiss != null) onDismiss.run();
        }
    }

    /** Hoãn AI routine sau khi đóng popup — tránh OOM / trắng màn khi decode túi + load products cùng lúc. */
    private void schedulePendingAiRoutineAfterReward() {
        if (!aiRoutinePendingAfterReward || !isAdded()) {
            return;
        }
        View root = getView();
        if (root == null) {
            startPendingAiRoutineIfNeeded();
            return;
        }
        root.postDelayed(() -> {
            if (!isAdded()) return;
            startPendingAiRoutineIfNeeded();
        }, 700);
    }

    private void startPendingAiRoutineIfNeeded() {
        if (!aiRoutinePendingAfterReward || !isAdded()) {
            return;
        }
        aiRoutinePendingAfterReward = false;
        final String skinType = pendingAiSkinType;
        final int hydration = pendingAiHydration;
        final int sebum = pendingAiSebum;
        final int sensitivity = pendingAiSensitivity;
        final int elasticity = pendingAiElasticity;
        final Set<String> flagged = pendingAiFlagged != null ? pendingAiFlagged : new HashSet<>();
        View root = getView();
        if (root == null) {
            loadAiRoutine(skinType, hydration, sebum, sensitivity, elasticity, flagged);
            return;
        }
        root.post(() -> {
            if (!isAdded() || binding == null) return;
            loadAiRoutine(skinType, hydration, sebum, sensitivity, elasticity, flagged);
        });
    }

    private void showCoinRewardThen(Runnable onDismiss, int rewardPoints, String source) {
        if (!isAdded()) return;
        int total = 0;
        try {
            total = com.veganbeauty.app.utils.RewardPointsHelper.getTotalPoints(requireContext());
        } catch (Exception ignored) {
        }
        showSafeCoinRewardDialog(rewardPoints, total, source, onDismiss);
    }

    private void popBackStackIfAdded() {
        // Chỉ pop khi user chủ động (nút Back) — không tự out sau quiz
        if (isAdded()) {
            try {
                getParentFragmentManager().popBackStack();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showSavedProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
        View btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        TextView tvConfirmText = dialogView.findViewById(R.id.tv_dialog_confirm_text);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        tvTitle.setText("Đã lưu hồ sơ da!");
        tvMsg.setText("Chỉ số da đã được lưu vào lịch sử của bạn.");
        tvConfirmText.setText("ĐỒNG Ý");
        btnCancel.setVisibility(View.GONE);

        AlertDialog customDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Ở lại màn kết quả — không tự popBackStack (tránh cảm giác bị out)
        btnConfirm.setOnClickListener(v -> customDialog.dismiss());
        customDialog.show();
    }

    private void saveSelectedStepsToProfile(SharedPreferences prefs, String skinType, String recommendation, Set<String> flaggedSet, int sensitivity, int hydration, int elasticity, int sebum) {
        Set<String> morningSave = new HashSet<>();
        for (AiSkincareStep step : morningSteps) {
            if (step.isChecked()) morningSave.add(step.getIndex() + ":" + step.getName() + ":" + step.getRecommendedProduct() + ":true");
        }

        Set<String> eveningSave = new HashSet<>();
        for (AiSkincareStep step : eveningSteps) {
            if (step.isChecked()) eveningSave.add(step.getIndex() + ":" + step.getName() + ":" + step.getRecommendedProduct() + ":true");
        }

        ProfileSession.setMorningSteps(requireContext(), morningSave);
        ProfileSession.setEveningSteps(requireContext(), eveningSave);

        saveSkinProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum,
                (rewardGranted, rewardPoints) -> {
                    if (!isAdded()) return;
                    if (rewardGranted) {
                        showSafeCoinRewardDialog(rewardPoints,
                                com.veganbeauty.app.utils.RewardPointsHelper.getTotalPoints(requireContext()),
                                "từ quiz cập nhật chỉ số da",
                                this::showApplyRoutineReminderDialog);
                    } else {
                        showApplyRoutineReminderDialog();
                    }
                });
    }

    private void showApplyRoutineReminderDialog() {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
        View btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        TextView tvConfirmText = dialogView.findViewById(R.id.tv_dialog_confirm_text);
        TextView btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        tvTitle.setText("Áp dụng và lưu thành công!");
        tvMsg.setText("Đã lưu chỉ số da và áp dụng routine AI vào lịch trình hàng ngày.\n\nBạn có muốn thiết lập thời gian nhắc nhở routine không?");
        tvConfirmText.setText("CÀI ĐẶT NHẮC HẸN");
        btnCancel.setText("ĐỂ SAU");

        AlertDialog customDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(v -> {
            customDialog.dismiss();
            if (!isAdded()) return;
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        // ĐỂ SAU: chỉ đóng dialog, ở lại màn kết quả — không popBackStack
        btnCancel.setOnClickListener(v -> customDialog.dismiss());
        customDialog.show();
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class AiSkincareStep {
        private int index;
        private String name;
        private String recommendedProduct;
        private String description;
        private boolean isChecked;

        public AiSkincareStep(int index, String name, String recommendedProduct, String description, boolean isChecked) {
            this.index = index;
            this.name = name;
            this.recommendedProduct = recommendedProduct;
            this.description = description;
            this.isChecked = isChecked;
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getRecommendedProduct() { return recommendedProduct; }
        public String getDescription() { return description; }
        public boolean isChecked() { return isChecked; }
        public void setChecked(boolean checked) { isChecked = checked; }
    }
}
