package com.veganbeauty.app.features.profile;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.databinding.FragmentSkinAllergyProfileBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.quiz.QuizTestResultFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import androidx.lifecycle.LifecycleOwnerKt;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.SkinProfileMetricsHelper;
import com.veganbeauty.app.features.ai.SkinComparisonAiHelper;

public class SkinAllergyProfileFragment extends RootieFragment {

    private FragmentSkinAllergyProfileBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSkinAllergyProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        Context ctx = requireContext();
        SkinWeatherProfileHelper.UserSkinProfile profile = SkinWeatherProfileHelper.load(ctx);
        String skinTypeRaw = profile.skinType;
        final String skinType = skinTypeRaw != null ? skinTypeRaw : "Da hỗn hợp thiên dầu";
        
        String recRaw = profile.recommendation;
        final String recommendation = recRaw != null && !recRaw.isEmpty() ? recRaw : "Hãy duy trì thói quen chăm sóc da lành tính hàng ngày...";
        
        Set<String> flaggedRaw = profile.flaggedGroups;
        final Set<String> flaggedSet = flaggedRaw != null ? flaggedRaw : new HashSet<>();

        binding.tvRecommendation.setText(recommendation);

        int sensitivity = profile.sensitivity;
        int hydration = profile.hydration;
        int elasticity = profile.elasticity;
        int sebum = profile.sebum;
        final String skinAreas = profile.skinAreas != null && !profile.skinAreas.isEmpty()
                ? profile.skinAreas
                : getDerivedSkinAreas(skinType);

        if (!profile.hasSavedProfile) {
            binding.tvSkinTypeResult.setText("Chưa có hồ sơ da");
            binding.tvSkinTypeDesc.setText("Làm bài quiz hoặc quét da bằng AI để xem loại da, chỉ số và hiện trạng vùng da.");
            binding.pbSensitivity.setProgress(0);
            binding.tvSensitivityVal.setText("—");
            binding.pbHydration.setProgress(0);
            binding.tvHydrationVal.setText("—");
            binding.pbElasticity.setProgress(0);
            binding.tvElasticityVal.setText("—");
            binding.pbSebum.setProgress(0);
            binding.tvSebumVal.setText("—");
            binding.tvSkinAreasDesc.setText("—");
        } else {
            binding.tvSkinTypeResult.setText(skinType);
            binding.tvSkinTypeDesc.setText(getSkinTypeDesc(skinType));
            binding.pbSensitivity.setProgress(sensitivity);
            binding.tvSensitivityVal.setText(sensitivity + "%");
            binding.pbHydration.setProgress(hydration);
            binding.tvHydrationVal.setText(hydration + "%");
            binding.pbElasticity.setProgress(elasticity);
            binding.tvElasticityVal.setText(elasticity + "%");
            binding.pbSebum.setProgress(sebum);
            binding.tvSebumVal.setText(sebum + "%");
            binding.tvSkinAreasDesc.setText(skinAreas);
        }

        setupSkinComparison();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        int finalSensitivity = sensitivity;
        int finalHydration = hydration;
        int finalElasticity = elasticity;
        int finalSebum = sebum;

        binding.btnViewSkinReport.setOnClickListener(v -> openPersonalSkinReport(
                ctx, profile.hasSavedProfile, skinType, recommendation, flaggedSet,
                finalSensitivity, finalHydration, finalElasticity, finalSebum, skinAreas));

        binding.btnSetupRoutine.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinReminderFragment())
                        .addToBackStack(null).commit());

        binding.btnRetakeQuiz.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new QuizTestIntroFragment())
                        .addToBackStack(null).commit());

        processIngredients(flaggedSet);
        checkInUseProducts(flaggedSet);

        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_account, tabId -> {
            BottomNavHelper.navigate(this, tabId);
        });

        setupScrollHideHeader();
    }

    private void openPersonalSkinReport(Context ctx, boolean hasSavedProfile, String skinType,
                                        String recommendation, Set<String> flaggedSet,
                                        int sensitivity, int hydration, int elasticity, int sebum,
                                        String skinAreas) {
        if (!hasSavedProfile) {
            Toast.makeText(ctx, "Hãy làm quiz hoặc quét da bằng AI trước để xem báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> flaggedCopy = flaggedSet != null ? new HashSet<>(flaggedSet) : new HashSet<>();
        ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("SKIN_TYPE_RESULT", skinType)
                .putString("RECOMMENDATION", recommendation)
                .putStringSet("FLAGGED_GROUPS", flaggedCopy)
                .putInt("SENSITIVITY_PERCENT", sensitivity)
                .putInt("HYDRATION_PERCENT", hydration)
                .putInt("ELASTICITY_PERCENT", elasticity)
                .putInt("SEBUM_PERCENT", sebum)
                .putString("SKIN_AREAS_DESC", skinAreas)
                .commit();

        Bundle args = new Bundle();
        args.putBoolean(QuizTestResultFragment.ARG_FROM_SKIN_PROFILE, true);
        args.putString(QuizTestResultFragment.ARG_SKIN_TYPE, skinType);
        args.putString(QuizTestResultFragment.ARG_RECOMMENDATION, recommendation);
        args.putStringArrayList(QuizTestResultFragment.ARG_FLAGGED_GROUPS, new ArrayList<>(flaggedCopy));
        args.putInt(QuizTestResultFragment.ARG_SENSITIVITY, sensitivity);
        args.putInt(QuizTestResultFragment.ARG_HYDRATION, hydration);
        args.putInt(QuizTestResultFragment.ARG_ELASTICITY, elasticity);
        args.putInt(QuizTestResultFragment.ARG_SEBUM, sebum);
        args.putString(QuizTestResultFragment.ARG_SKIN_AREAS, skinAreas);

        if (!isAdded()) {
            return;
        }

        QuizTestResultFragment fragment = new QuizTestResultFragment();
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, fragment)
                .addToBackStack("skin_personal_report")
                .commitAllowingStateLoss();
    }

    private void setupScrollHideHeader() {
        int bottomPadding = (int) requireContext().getResources().getDimension(R.dimen.home_nav_bar_height);
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.rlHeader,
                binding.skinAllergyScroll,
                bottomPadding
        );
        headerScrollHelper.attachToNestedScrollView(binding.skinAllergyScroll);
    }

    private void addPill(ViewGroup container, String text, int backgroundResId, String textColorStr) {
        TextView pillView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_pill, container, false);
        pillView.setText(text);
        pillView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f);
        pillView.setBackgroundResource(backgroundResId);
        pillView.setTextColor(Color.parseColor(textColorStr));
        container.addView(pillView);
    }

    private String getSkinTypeDesc(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("dầu") && lower.contains("nhạy cảm")) return "Làn da của bạn tiết nhiều dầu nhờn, lỗ chân lông to và rất dễ bị kích ứng, đỏ rát hoặc nổi mẩn khi gặp thành phần hoạt chất mạnh hoặc thay đổi thời tiết.";
        if (lower.contains("khô") && lower.contains("nhạy cảm")) return "Làn da thiếu ẩm, thường xuyên bong tróc, căng rát và có hàng rào bảo vệ da yếu, cực kỳ nhạy cảm với cồn khô và hương liệu.";
        if (lower.contains("dầu")) return "Làn da tiết nhiều bã nhờn toàn mặt hoặc vùng chữ T, dễ bít tắc lỗ chân lông gây mụn. Cần tập trung làm sạch sâu và cấp ẩm dạng gel mỏng nhẹ.";
        if (lower.contains("khô")) return "Làn da thiếu hụt dầu tự nhiên, bề mặt thô ráp, xuất hiện các nếp nhăn li ti. Cần được bổ sung độ ẩm và khóa ẩm bằng các loại kem dưỡng đậm đặc.";
        return "Làn da của bạn ở trạng thái tương đối cân bằng, tuy nhiên cần chăm sóc đều đặn để duy trì độ ẩm và bảo vệ da trước các tác nhân ô nhiễm môi trường.";
    }

    private String getDerivedSkinAreas(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("dầu") && lower.contains("nhạy cảm")) return "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu thừa, bóng nhờn; hai bên má nhạy cảm, dễ nổi mẩn đỏ, châm chích.";
        if (lower.contains("mụn")) return "Vùng trán và cằm dễ bị bít tắc gây mụn; lượng dầu phân bổ không đều làm bít tắc cổ nang lông.";
        if (lower.contains("mất nước")) return "Vùng chữ U (mũi và má) căng khô, thiếu nước trầm trọng; vùng chữ T có thể đổ dầu nhẹ do phản ứng bù ẩm.";
        if (lower.contains("nhạy cảm")) return "Toàn bộ bề mặt da có lớp màng bảo vệ yếu, mỏng và dễ phản ứng châm chích với mọi mỹ phẩm mới.";
        if (lower.contains("khô")) return "Khô ráp toàn mặt, vùng má căng chặt và có xu hướng bong tróc vảy da chết li ti.";
        if (lower.contains("dầu")) return "Lượng dầu hoạt động mạnh mẽ trên toàn bộ khuôn mặt, bóng loáng đặc biệt ở vùng chữ T và hai bên cánh mũi.";
        if (lower.contains("hỗn hợp")) return "Vùng chữ T đổ dầu nhiều và lỗ chân lông to; vùng chữ U (má) bình thường hoặc khô nhẹ.";
        if (lower.contains("thường")) return "Độ ẩm phân bổ đều đặn, vùng chữ T dầu nhẹ không đáng kể, vùng má mịn màng đàn hồi tốt.";
        return "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da.";
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

    private void checkInUseProducts(Set<String> flaggedGroups) {
        try {
            BufferedReader br1 = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("products.json")));
            StringBuilder sb1 = new StringBuilder(); String line1;
            while ((line1 = br1.readLine()) != null) sb1.append(line1);
            br1.close();
            JSONObject root = new JSONObject(sb1.toString());
            JSONArray jsonArray = root.getJSONArray("products");

            BufferedReader br2 = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("quiz_thanhphan.json")));
            StringBuilder sb2 = new StringBuilder(); String line2;
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
                    if ("avoid".equals(risk)) avoidChemicals.add(name.toLowerCase());
                    else if ("caution".equals(risk)) cautionChemicals.add(name.toLowerCase());
                }
            }

            String userId = ProfileSession.getUserId(requireContext());
            Set<String> combinedIds = new HashSet<>();
            
            List<OrderEntity> orderList = new LocalJsonReader(requireContext()).getAllOrders();
            if (orderList != null) {
                for (OrderEntity order : orderList) {
                    if (userId != null && userId.equals(order.getUserId())) {
                        if (order.getItems() != null) {
                            for (OrderItem item : order.getItems()) {
                                combinedIds.add(item.getProductId());
                            }
                        }
                    }
                }
            }
            
            renderProductsInUse(jsonArray, combinedIds, avoidChemicals, cautionChemicals);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderProductsInUse(JSONArray jsonArray, Set<String> inUseIds, Set<String> avoidChemicals, Set<String> cautionChemicals) {
        if (binding == null) return;
        binding.llProductsInUseContainer.removeAllViews();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject prodObj = jsonArray.getJSONObject(i);
                String id = prodObj.getString("id");
                if (!inUseIds.contains(id)) continue;

                String prodName = prodObj.getString("name");
                String mainImage = prodObj.optString("mainImage", "");

                List<String> detailedIngredientsList = new ArrayList<>();
                if (prodObj.has("detailedIngredients")) {
                    JSONArray detailedArray = prodObj.getJSONArray("detailedIngredients");
                    for (int j = 0; j < detailedArray.length(); j++) {
                        detailedIngredientsList.add(detailedArray.getString(j).toLowerCase());
                    }
                }

                List<String> triggeredAvoids = new ArrayList<>();
                List<String> triggeredCautions = new ArrayList<>();

                for (String chem : avoidChemicals) {
                    for (String ing : detailedIngredientsList) {
                        if (ing.contains(chem)) triggeredAvoids.add(getViName(chem));
                    }
                }
                for (String chem : cautionChemicals) {
                    for (String ing : detailedIngredientsList) {
                        if (ing.contains(chem)) triggeredCautions.add(getViName(chem));
                    }
                }

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.skin_item_product_in_use, binding.llProductsInUseContainer, false);
                TextView tvProdName = itemView.findViewById(R.id.tv_product_name);
                TextView tvProdDesc = itemView.findViewById(R.id.tv_product_desc);
                TextView tvBadge = itemView.findViewById(R.id.tv_compatibility_badge);
                ImageView ivBadgeIcon = itemView.findViewById(R.id.iv_badge_icon);
                ImageView ivImage = itemView.findViewById(R.id.iv_product_image);

                tvProdName.setText(prodName);

                if (!mainImage.isEmpty()) {
                    com.bumptech.glide.Glide.with(ivImage.getContext()).load(mainImage).placeholder(R.drawable.myphamxanh).into(ivImage);
                } else {
                    ivImage.setImageResource(R.drawable.myphamxanh);
                }

                if (!triggeredAvoids.isEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.ic_warning_triangle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F04758")));
                    tvBadge.setText("Nguy cơ kích ứng cao");
                    tvBadge.setTextColor(Color.parseColor("#F04758"));
                    Set<String> uniqueAvoids = new HashSet<>(triggeredAvoids);
                    String avoidStr = String.join(", ", uniqueAvoids);
                    tvProdDesc.setText("Chứa " + avoidStr + " — không phù hợp da nhạy cảm.");
                } else if (!triggeredCautions.isEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.ic_warning_triangle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EAAA08")));
                    tvBadge.setText("Cần lưu ý khi dùng");
                    tvBadge.setTextColor(Color.parseColor("#EAAA08"));
                    Set<String> uniqueCautions = new HashSet<>(triggeredCautions);
                    String cautionStr = String.join(", ", uniqueCautions);
                    tvProdDesc.setText("Chứa " + cautionStr + " — cần theo dõi khi dùng.");
                } else {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_wavy_check);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#12B76A")));
                    tvBadge.setText("Phù hợp");
                    tvBadge.setTextColor(Color.parseColor("#12B76A"));
                    tvProdDesc.setText("Thành phần lành tính, phù hợp nền da hiện tại.");
                }

                binding.llProductsInUseContainer.addView(itemView);
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

    private static final int COMPARE_BG_IMPROVED = Color.parseColor("#F2FBF6");
    private static final int COMPARE_BG_DEFAULT = Color.parseColor("#F7F9FA");

    private void setCompareCellBackground(View cell, boolean improved) {
        if (cell == null) return;
        cell.setBackgroundTintList(ColorStateList.valueOf(improved ? COMPARE_BG_IMPROVED : COMPARE_BG_DEFAULT));
    }

    private void setupSkinComparison() {
        if (binding == null || getContext() == null) return;
        binding.skinComparisonSection.setVisibility(View.GONE);

        Context appContext = requireContext().getApplicationContext();
        final String geminiKey = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY;
        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
            BuildersKt.withContext(Dispatchers.getIO(), (s2, c2) -> {
                String userId = ProfileSession.getUserId(appContext);
                String email = ProfileSession.getEmail(appContext);
                List<SkinProfileMetricsHelper.Snapshot> snapshots =
                        SkinProfileMetricsHelper.loadComparableSnapshots(appContext, userId, email);

                if (snapshots.size() < 2) {
                    return BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                        if (binding != null) {
                            binding.skinComparisonSection.setVisibility(View.GONE);
                        }
                        return kotlin.Unit.INSTANCE;
                    }, c2);
                }

                final SkinProfileMetricsHelper.Snapshot newer = snapshots.get(0);
                final SkinProfileMetricsHelper.Snapshot older = snapshots.get(1);
                final String localAnalysis = buildEfficacyAnalysis(older, newer);

                // Hiện UI + text tạm trên Main, rồi gọi Gemini trên IO
                BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                    if (binding == null) return kotlin.Unit.INSTANCE;
                    try {
                        binding.skinComparisonSection.setVisibility(View.VISIBLE);
                        bindHydrationComparison(older.hydration, newer.hydration);
                        bindSebumComparison(older.sebum, newer.sebum);
                        bindSensitivityComparison(older.sensitivity, newer.sensitivity);
                        bindElasticityComparison(older.elasticity, newer.elasticity);
                        binding.skinTvProductEfficacyAnalysis.setText(
                                "Rootie AI đang phân tích thay đổi da của bạn...\n\n" + localAnalysis);
                    } catch (Exception e) {
                        e.printStackTrace();
                        binding.skinComparisonSection.setVisibility(View.GONE);
                    }
                    return kotlin.Unit.INSTANCE;
                }, c2);

                String aiText = SkinComparisonAiHelper.analyzeWithGemini(geminiKey, older, newer);
                final String display = (aiText != null && !aiText.trim().isEmpty())
                        ? aiText.trim()
                        : localAnalysis;

                return BuildersKt.withContext(Dispatchers.getMain(), (s4, c4) -> {
                    if (binding != null) {
                        binding.skinTvProductEfficacyAnalysis.setText(display);
                    }
                    return kotlin.Unit.INSTANCE;
                }, c2);
            }, cont));
    }

    private void bindHydrationComparison(int oldVal, int newVal) {
        binding.skinCompareHydrationOld.setText(oldVal + "%");
        binding.skinCompareHydrationNew.setText(newVal + "%");
        int diff = newVal - oldVal;
        if (diff > 0) {
            binding.skinCompareHydrationDiff.setText("+" + diff + "% (Cải thiện \uD83D\uDCC8)");
            binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#12B76A"));
        } else if (diff < 0) {
            binding.skinCompareHydrationDiff.setText(diff + "% (Kém đi \uD83D\uDCC9)");
            binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#F04758"));
        } else {
            binding.skinCompareHydrationDiff.setText("0% (Duy trì)");
            binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#7E8A83"));
        }
        setCompareCellBackground(binding.skinCompareHydrationCell, diff > 0);
    }

    private void bindSebumComparison(int oldVal, int newVal) {
        binding.skinCompareSebumOld.setText(oldVal + "%");
        binding.skinCompareSebumNew.setText(newVal + "%");
        int diff = newVal - oldVal;
        if (diff < 0) {
            binding.skinCompareSebumDiff.setText(diff + "% (Cải thiện \uD83D\uDCC9)");
            binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#12B76A"));
        } else if (diff > 0) {
            binding.skinCompareSebumDiff.setText("+" + diff + "% (Tăng dầu \uD83D\uDCC8)");
            binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#F04758"));
        } else {
            binding.skinCompareSebumDiff.setText("0% (Duy trì)");
            binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#7E8A83"));
        }
        setCompareCellBackground(binding.skinCompareSebumCell, diff < 0);
    }

    private void bindSensitivityComparison(int oldVal, int newVal) {
        binding.skinCompareSensitivityOld.setText(oldVal + "%");
        binding.skinCompareSensitivityNew.setText(newVal + "%");
        int diff = newVal - oldVal;
        if (diff < 0) {
            binding.skinCompareSensitivityDiff.setText(diff + "% (Cải thiện \uD83D\uDCC9)");
            binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#12B76A"));
        } else if (diff > 0) {
            binding.skinCompareSensitivityDiff.setText("+" + diff + "% (Tăng nhạy cảm \uD83D\uDCC8)");
            binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#F04758"));
        } else {
            binding.skinCompareSensitivityDiff.setText("0% (Duy trì)");
            binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#7E8A83"));
        }
        setCompareCellBackground(binding.skinCompareSensitivityCell, diff < 0);
    }

    private void bindElasticityComparison(int oldVal, int newVal) {
        binding.skinCompareElasticityOld.setText(oldVal + "%");
        binding.skinCompareElasticityNew.setText(newVal + "%");
        int diff = newVal - oldVal;
        if (diff > 0) {
            binding.skinCompareElasticityDiff.setText("+" + diff + "% (Cải thiện \uD83D\uDCC8)");
            binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#12B76A"));
        } else if (diff < 0) {
            binding.skinCompareElasticityDiff.setText(diff + "% (Giảm đàn hồi \uD83D\uDCC9)");
            binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#F04758"));
        } else {
            binding.skinCompareElasticityDiff.setText("0% (Duy trì)");
            binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#7E8A83"));
        }
        setCompareCellBackground(binding.skinCompareElasticityCell, diff > 0);
    }

    @NonNull
    private String buildEfficacyAnalysis(SkinProfileMetricsHelper.Snapshot older,
                                         SkinProfileMetricsHelper.Snapshot newer) {
        int hydDiff = newer.hydration - older.hydration;
        int sebDiff = newer.sebum - older.sebum;
        int sensDiff = newer.sensitivity - older.sensitivity;
        int elastDiff = newer.elasticity - older.elasticity;

        StringBuilder sb = new StringBuilder();
        if (older.dateLabel != null && newer.dateLabel != null) {
            sb.append("So sánh từ ").append(older.dateLabel)
                    .append(" → ").append(newer.dateLabel).append(":\n\n");
        }

        if (older.skinType != null && newer.skinType != null
                && !older.skinType.equalsIgnoreCase(newer.skinType)) {
            sb.append("• Loại da: ").append(older.skinType)
                    .append(" → ").append(newer.skinType).append("\n");
        } else if (newer.skinType != null && !newer.skinType.isEmpty()) {
            sb.append("• Loại da hiện tại: ").append(newer.skinType).append("\n");
        }

        sb.append("• Chỉ số trước → sau:\n");
        sb.append("  - Ẩm: ").append(older.hydration).append("% → ").append(newer.hydration).append("%\n");
        sb.append("  - Bã nhờn: ").append(older.sebum).append("% → ").append(newer.sebum).append("%\n");
        sb.append("  - Nhạy cảm: ").append(older.sensitivity).append("% → ").append(newer.sensitivity).append("%\n");
        sb.append("  - Đàn hồi: ").append(older.elasticity).append("% → ").append(newer.elasticity).append("%\n\n");

        int changeCount = 0;
        if (hydDiff > 0) { sb.append("• Cấp ẩm tốt hơn (+").append(hydDiff).append("%)\n"); changeCount++; }
        if (sebDiff < 0) { sb.append("• Kiểm soát bã nhờn ổn định (").append(sebDiff).append("%)\n"); changeCount++; }
        if (sensDiff < 0) { sb.append("• Giảm nhạy cảm kích ứng (").append(sensDiff).append("%)\n"); changeCount++; }
        if (elastDiff > 0) { sb.append("• Tăng độ đàn hồi săn chắc (+").append(elastDiff).append("%)\n"); changeCount++; }
        if (hydDiff < 0) { sb.append("• Độ ẩm giảm (").append(hydDiff).append("%)\n"); changeCount++; }
        if (sebDiff > 0) { sb.append("• Bã nhờn tăng (+").append(sebDiff).append("%)\n"); changeCount++; }
        if (sensDiff > 0) { sb.append("• Nhạy cảm tăng (+").append(sensDiff).append("%)\n"); changeCount++; }
        if (elastDiff < 0) { sb.append("• Đàn hồi giảm (").append(elastDiff).append("%)\n"); changeCount++; }
        if (changeCount == 0) {
            sb.append("• Chỉ số da duy trì ở mức ổn định theo dữ liệu đo của bạn.\n");
        }
        sb.append("\n=> Phân tích dựa trên lịch sử test/quét da đã lưu của bạn. Tiếp tục theo dõi routine để đánh giá hiệu quả lâu dài.");
        return sb.toString();
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
