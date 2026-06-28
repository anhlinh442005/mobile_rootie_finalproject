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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.databinding.FragmentSkinAllergyProfileBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.quiz.QuizTestResultFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SkinAllergyProfileFragment extends RootieFragment {

    private FragmentSkinAllergyProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSkinAllergyProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        Context ctx = requireContext();
        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinTypeRaw = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu");
        final String skinType = skinTypeRaw != null ? skinTypeRaw : "Da hỗn hợp thiên dầu";
        
        String recRaw = prefs.getString("SAVED_RECOMMENDATION", "Hãy duy trì thói quen chăm sóc da lành tính hàng ngày...");
        final String recommendation = recRaw != null ? recRaw : "Hãy duy trì thói quen chăm sóc da lành tính hàng ngày...";
        
        Set<String> flaggedRaw = prefs.getStringSet("SAVED_FLAGGED_GROUPS", new HashSet<>());
        final Set<String> flaggedSet = flaggedRaw != null ? flaggedRaw : new HashSet<>();

        binding.tvSkinTypeResult.setText(skinType);
        binding.tvRecommendation.setText(recommendation);
        binding.tvSkinTypeDesc.setText(getSkinTypeDesc(skinType));

        int savedSens = prefs.getInt("SAVED_SENSITIVITY", -1);
        int sensitivity = savedSens != -1 ? savedSens : getDerivedSensitivity(skinType);
        
        int savedHydr = prefs.getInt("SAVED_HYDRATION", -1);
        int hydration = savedHydr != -1 ? savedHydr : getDerivedHydration(skinType);
        
        int savedElas = prefs.getInt("SAVED_ELASTICITY", -1);
        int elasticity = savedElas != -1 ? savedElas : getDerivedElasticity(skinType);
        
        int savedSebum = prefs.getInt("SAVED_SEBUM", -1);
        int sebum = savedSebum != -1 ? savedSebum : getDerivedSebum(skinType);
        
        String skinAreasRaw = prefs.getString("SAVED_SKIN_AREAS", null);
        final String skinAreas = skinAreasRaw != null ? skinAreasRaw : getDerivedSkinAreas(skinType);

        binding.pbSensitivity.setProgress(sensitivity); binding.tvSensitivityVal.setText(sensitivity + "%");
        binding.pbHydration.setProgress(hydration); binding.tvHydrationVal.setText(hydration + "%");
        binding.pbElasticity.setProgress(elasticity); binding.tvElasticityVal.setText(elasticity + "%");
        binding.pbSebum.setProgress(sebum); binding.tvSebumVal.setText(sebum + "%");
        binding.tvSkinAreasDesc.setText(skinAreas);

        setupSkinComparison(prefs, hydration, sebum, sensitivity, elasticity);

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnNotification.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null).commit());

        int finalSensitivity = sensitivity;
        int finalHydration = hydration;
        int finalElasticity = elasticity;
        int finalSebum = sebum;

        binding.btnViewSkinReport.setOnClickListener(v -> {
            prefs.edit()
                    .putString("SKIN_TYPE_RESULT", skinType)
                    .putString("RECOMMENDATION", recommendation)
                    .putStringSet("FLAGGED_GROUPS", flaggedSet)
                    .putInt("SENSITIVITY_PERCENT", finalSensitivity)
                    .putInt("HYDRATION_PERCENT", finalHydration)
                    .putInt("ELASTICITY_PERCENT", finalElasticity)
                    .putInt("SEBUM_PERCENT", finalSebum)
                    .putString("SKIN_AREAS_DESC", skinAreas)
                    .apply();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new QuizTestResultFragment())
                    .addToBackStack(null).commit();
        });

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
    }

    private void addPill(ViewGroup container, String text, int backgroundResId, String textColorStr) {
        TextView pillView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_pill, container, false);
        pillView.setText(text);
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

    private int getDerivedSensitivity(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("nhạy cảm") || lower.contains("kích ứng")) return 85;
        if (lower.contains("mụn")) return 65;
        if (lower.contains("dầu") || lower.contains("khô")) return 45;
        return 15;
    }

    private int getDerivedHydration(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("mất nước")) return 25;
        if (lower.contains("khô")) return 35;
        if (lower.contains("dầu")) return 60;
        if (lower.contains("thường")) return 85;
        return 55;
    }

    private int getDerivedElasticity(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("lão hóa")) return 40;
        if (lower.contains("thường")) return 92;
        return 75;
    }

    private int getDerivedSebum(String skinType) {
        String lower = skinType.toLowerCase();
        if (lower.contains("dầu")) return 90;
        if (lower.contains("hỗn hợp")) return 70;
        if (lower.contains("khô") || lower.contains("mất nước")) return 20;
        return 50;
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

            Set<String> inUseIds = new HashSet<>(Arrays.asList(
                    "5f29c9fa19873eb44aedced4", "1ba0d34dab3627b376a567c7",
                    "1e499ed75a31e4a02af2d962", "50c50369a5552d24a4c319b6"
            ));

            List<OrderEntity> orderList = new LocalJsonReader(requireContext()).getAllOrders();
            Set<String> combinedIds = new HashSet<>(inUseIds);
            if (orderList != null) {
                for (OrderEntity order : orderList) {
                    if (order.getItems() != null) {
                        for (OrderItem item : order.getItems()) {
                            combinedIds.add(item.getProductId());
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

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_product_recommendation, binding.llProductsInUseContainer, false);
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
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F04758")));
                    tvBadge.setText("Nguy cơ kích ứng cao");
                    tvBadge.setTextColor(Color.parseColor("#F04758"));
                    Set<String> uniqueAvoids = new HashSet<>(triggeredAvoids);
                    String avoidStr = String.join(", ", uniqueAvoids);
                    tvProdDesc.setText("Sản phẩm có chứa " + avoidStr + " không phù hợp với loại da nhạy cảm của bạn, dễ gây dị ứng hoặc kích ứng đỏ rát.");
                } else if (!triggeredCautions.isEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#EAAA08")));
                    tvBadge.setText("Cần lưu ý khi dùng");
                    tvBadge.setTextColor(Color.parseColor("#EAAA08"));
                    Set<String> uniqueCautions = new HashSet<>(triggeredCautions);
                    String cautionStr = String.join(", ", uniqueCautions);
                    tvProdDesc.setText("Sản phẩm chứa " + cautionStr + ". Cần theo dõi sát sao biểu hiện của da khi sử dụng sản phẩm này.");
                } else {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_wavy_check);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#12B76A")));
                    tvBadge.setText("Phù hợp");
                    tvBadge.setTextColor(Color.parseColor("#12B76A"));
                    tvProdDesc.setText("Bảng thành phần cực kỳ lành tính và hoàn toàn phù hợp với nền da hiện tại của bạn.");
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

    private void setupSkinComparison(SharedPreferences prefs, int currentHydration, int currentSebum, int currentSensitivity, int currentElasticity) {
        String historyStr = prefs.getString("QUIZ_HISTORY_LIST", "[]");
        if (historyStr == null) historyStr = "[]";
        try {
            JSONArray historyArray = new JSONArray(historyStr);

            int oldHydration, oldSebum, oldSensitivity, oldElasticity;
            boolean isComparedToStartProfile;

            if (historyArray.length() >= 2) {
                JSONObject oldRecord = historyArray.getJSONObject(historyArray.length() - 2);
                String oldSkinType = oldRecord.optString("skinType", "Da hỗn hợp");
                oldHydration = oldRecord.optInt("hydration", getDerivedHydration(oldSkinType));
                oldSebum = oldRecord.optInt("sebum", getDerivedSebum(oldSkinType));
                oldSensitivity = oldRecord.optInt("sensitivity", getDerivedSensitivity(oldSkinType));
                oldElasticity = oldRecord.optInt("elasticity", getDerivedElasticity(oldSkinType));
                isComparedToStartProfile = false;
            } else {
                oldHydration = Math.max(0, Math.min(100, currentHydration - 10));
                oldSebum = Math.max(0, Math.min(100, currentSebum + 12));
                oldSensitivity = Math.max(0, Math.min(100, currentSensitivity + 8));
                oldElasticity = Math.max(0, Math.min(100, currentElasticity - 5));
                isComparedToStartProfile = true;
            }

            binding.skinCompareHydrationOld.setText(oldHydration + "%");
            binding.skinCompareHydrationNew.setText(currentHydration + "%");
            binding.skinCompareHydrationProgressOld.setProgress(oldHydration);
            binding.skinCompareHydrationProgress.setProgress(currentHydration);
            int hydDiff = currentHydration - oldHydration;
            if (hydDiff > 0) {
                binding.skinCompareHydrationDiff.setText("+" + hydDiff + "% (Cải thiện \uD83D\uDCC8)");
                binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#12B76A"));
            } else if (hydDiff < 0) {
                binding.skinCompareHydrationDiff.setText(hydDiff + "% (Kém đi \uD83D\uDCC9)");
                binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#F04758"));
            } else {
                binding.skinCompareHydrationDiff.setText("0% (Duy trì)");
                binding.skinCompareHydrationDiff.setTextColor(Color.parseColor("#7E8A83"));
            }

            binding.skinCompareSebumOld.setText(oldSebum + "%");
            binding.skinCompareSebumNew.setText(currentSebum + "%");
            binding.skinCompareSebumProgressOld.setProgress(oldSebum);
            binding.skinCompareSebumProgress.setProgress(currentSebum);
            int sebDiff = currentSebum - oldSebum;
            if (sebDiff < 0) {
                binding.skinCompareSebumDiff.setText(sebDiff + "% (Cải thiện \uD83D\uDCC9)");
                binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#12B76A"));
            } else if (sebDiff > 0) {
                binding.skinCompareSebumDiff.setText("+" + sebDiff + "% (Tăng dầu \uD83D\uDCC8)");
                binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#F04758"));
            } else {
                binding.skinCompareSebumDiff.setText("0% (Duy trì)");
                binding.skinCompareSebumDiff.setTextColor(Color.parseColor("#7E8A83"));
            }

            binding.skinCompareSensitivityOld.setText(oldSensitivity + "%");
            binding.skinCompareSensitivityNew.setText(currentSensitivity + "%");
            binding.skinCompareSensitivityProgressOld.setProgress(oldSensitivity);
            binding.skinCompareSensitivityProgress.setProgress(currentSensitivity);
            int sensDiff = currentSensitivity - oldSensitivity;
            if (sensDiff < 0) {
                binding.skinCompareSensitivityDiff.setText(sensDiff + "% (Cải thiện \uD83D\uDCC9)");
                binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#12B76A"));
            } else if (sensDiff > 0) {
                binding.skinCompareSensitivityDiff.setText("+" + sensDiff + "% (Tăng nhạy cảm \uD83D\uDCC8)");
                binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#F04758"));
            } else {
                binding.skinCompareSensitivityDiff.setText("0% (Duy trì)");
                binding.skinCompareSensitivityDiff.setTextColor(Color.parseColor("#7E8A83"));
            }

            binding.skinCompareElasticityOld.setText(oldElasticity + "%");
            binding.skinCompareElasticityNew.setText(currentElasticity + "%");
            binding.skinCompareElasticityProgressOld.setProgress(oldElasticity);
            binding.skinCompareElasticityProgress.setProgress(currentElasticity);
            int elastDiff = currentElasticity - oldElasticity;
            if (elastDiff > 0) {
                binding.skinCompareElasticityDiff.setText("+" + elastDiff + "% (Cải thiện \uD83D\uDCC8)");
                binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#12B76A"));
            } else if (elastDiff < 0) {
                binding.skinCompareElasticityDiff.setText(elastDiff + "% (Giảm đàn hồi \uD83D\uDCC9)");
                binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#F04758"));
            } else {
                binding.skinCompareElasticityDiff.setText("0% (Duy trì)");
                binding.skinCompareElasticityDiff.setTextColor(Color.parseColor("#7E8A83"));
            }

            float temp = prefs.getFloat("SAVED_WEATHER_TEMP", -100f);
            int humidityVal = prefs.getInt("SAVED_WEATHER_HUMIDITY", -1);
            float uv = prefs.getFloat("SAVED_WEATHER_UV", -1f);
            int pm25 = prefs.getInt("SAVED_WEATHER_PM25", -1);
            String city = prefs.getString("SAVED_WEATHER_CITY", "");
            String weatherCondition = prefs.getString("SAVED_WEATHER_CONDITION", "");

            StringBuilder sb = new StringBuilder();
            if (isComparedToStartProfile) sb.append("So với hồ sơ da khởi điểm, làn da của bạn đang có những tín hiệu phục hồi tích cực:\n");
            else sb.append("So với kết quả kiểm tra tuần trước, làn da tuần này có sự thay đổi rõ rệt:\n");

            boolean improvedAny = false;
            if (hydDiff > 0 || sebDiff < 0 || sensDiff < 0 || elastDiff > 0) {
                improvedAny = true;
                sb.append("- ");
                List<String> list = new ArrayList<>();
                if (hydDiff > 0) list.add("cấp ẩm tốt hơn (+" + hydDiff + "%)");
                if (sebDiff < 0) list.add("kiểm soát bã nhờn ổn định hơn (" + sebDiff + "%)");
                if (sensDiff < 0) list.add("giảm độ nhạy cảm kích ứng (" + sensDiff + "%)");
                if (elastDiff > 0) list.add("tăng cường độ săn chắc đàn hồi (+" + elastDiff + "%)");
                sb.append(String.join(", ", list)).append(".\n");
                sb.append("=> Cho thấy các sản phẩm dưỡng da lành tính (như thạch/gel Bí Đao, tinh chất Rau Má) đang hoạt động tối ưu và tương tương thích tốt trên nền da của bạn.\n");
            }

            if (!improvedAny) sb.append("- Chỉ số da duy trì ở mức ổn định. Hãy theo dõi thêm và duy trì routine đều đặn để đạt hiệu quả cao nhất.\n");

            if (temp != -100f && city != null && !city.trim().isEmpty()) {
                sb.append("\n⛅ **Khuyên dùng theo thời tiết tại ").append(city).append(" (").append(temp).append("°C, độ ẩm ").append(humidityVal).append("%):**\n");
                if (uv >= 6.0) {
                    sb.append("- Hôm nay chỉ số UV ở mức cao (").append(String.format(Locale.US, "%.1f", uv)).append("). Hãy chú ý thoa Sữa chống nắng Bí Đao phổ rộng trước khi ra ngoài và che chắn kỹ để bảo vệ nền da nhạy cảm (").append(currentSensitivity).append("%).\n");
                } else if (temp >= 30.0 && currentSebum >= 50) {
                    sb.append("- Trời nắng nóng gay gắt (").append(temp).append("°C), bã nhờn của bạn là ").append(currentSebum).append("%. Hãy ưu tiên Gel rửa mặt Bí Đao và Tinh chất Bí Đao dạng gel mỏng nhẹ để kiềm dầu thừa, tránh bít tắc lỗ chân lông.\n");
                } else if (humidityVal < 50 && currentHydration < 50) {
                    sb.append("- Độ ẩm không khí ngoài trời thấp (").append(humidityVal).append("%), kết hợp với da bạn đang thiếu ẩm (").append(currentHydration).append("%). Hãy bổ sung ngay Hyaluronic Acid Serum và Thạch Hoa Hồng hữu cơ khóa ẩm để ngăn mất nước.\n");
                } else if (pm25 >= 50) {
                    sb.append("- Chỉ số bụi mịn PM2.5 ở mức kém (").append(pm25).append(" μg/m³). Hãy làm sạch da kỹ lưỡng với nước tẩy trang Hoa Hồng/Sen thuần chay ngay khi về nhà để tránh bít tắc sinh mụn.\n");
                } else {
                    sb.append("- Thời tiết hôm nay mát mẻ, dễ chịu (").append(weatherCondition).append("). Hãy duy trì cấp ẩm dịu nhẹ bằng toner và serum phục hồi B5 để giữ vững hàng rào bảo vệ da khỏe mạnh.\n");
                }
            } else {
                sb.append("\n=> Khuyên dùng: Tiếp tục sử dụng routine chăm sóc da hiện tại, uống đủ nước và hạn chế sản phẩm chứa cồn khô/hương liệu nhân tạo.");
            }

            binding.skinTvProductEfficacyAnalysis.setText(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            binding.skinTvProductEfficacyAnalysis.setText("Đã có lỗi xảy ra khi xử lý biểu đồ so sánh.");
        }
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
