package com.veganbeauty.app.features.quiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.databinding.QuizTestResultBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.RoundedCornersTransformation;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class QuizTestResultFragment extends RootieFragment {

    private QuizTestResultBinding binding;
    private final String GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY;
    private final List<AiSkincareStep> morningSteps = new ArrayList<>();
    private final List<AiSkincareStep> eveningSteps = new ArrayList<>();
    private boolean isMorningTab = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinType = prefs.getString("SKIN_TYPE_RESULT", "Da thường");
        String recommendation = prefs.getString("RECOMMENDATION", "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày.");
        Set<String> flaggedSet = prefs.getStringSet("FLAGGED_GROUPS", new HashSet<>());
        int sensitivity = prefs.getInt("SENSITIVITY_PERCENT", 50);
        int hydration = prefs.getInt("HYDRATION_PERCENT", 50);
        int elasticity = prefs.getInt("ELASTICITY_PERCENT", 75);
        int sebum = prefs.getInt("SEBUM_PERCENT", 50);

        binding.tvSkinTypeResult.setText(skinType);
        binding.tvRecommendation.setText(recommendation);

        String skinDesc;
        switch (skinType) {
            case "Da dầu nhạy cảm": skinDesc = "Làn da của bạn có xu hướng tiết nhiều dầu ở vùng chữ T nhưng dễ dàng bị kích ứng, đỏ rát bởi các tác nhân môi trường hoặc mỹ phẩm mạnh."; break;
            case "Da mụn": skinDesc = "Nền da dễ bị bít tắc lỗ chân lông, sinh nhân mụn đầu đen, mụn ẩn hoặc mụn viêm. Cần chú trọng làm sạch dịu nhẹ và kháng khuẩn."; break;
            case "Da nhạy cảm": skinDesc = "Làn da có hàng rào bảo vệ mỏng yếu, dễ đỏ rát, châm chích khi thay đổi thời tiết hoặc sử dụng sản phẩm có cồn/hương liệu."; break;
            case "Da khô": skinDesc = "Làn da thiếu hụt độ ẩm tự nhiên, thường xuyên có cảm giác căng chặt, bong tróc vảy nhỏ và dễ hình thành nếp nhăn sớm."; break;
            case "Da dầu": skinDesc = "Lượng bã nhờn hoạt động quá mức gây bóng loáng toàn mặt, lỗ chân lông to và dễ bám bụi bẩn hình thành mụn."; break;
            case "Da hỗn hợp": skinDesc = "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu nhờn, trong khi vùng chữ U (hai bên má) lại khô hoặc bình thường."; break;
            case "Da thường": skinDesc = "Làn da lý tưởng với độ ẩm cân bằng, lỗ chân lông nhỏ, da mịn màng khỏe mạnh và ít khi gặp các vấn đề kích ứng."; break;
            case "Da lão hóa": skinDesc = "Làn da bắt đầu xuất hiện các nếp nhăn nông sâu, độ đàn hồi kém, có thể có sạm nám và cần bổ sung chất chống oxy hóa mạnh."; break;
            case "Da mất nước": skinDesc = "Da có thể tiết dầu nhưng bề mặt vẫn căng khô, thô ráp do thiếu hụt lượng nước trong tế bào biểu bì."; break;
            case "Da dễ kích ứng": skinDesc = "Hàng rào bảo vệ da bị tổn thương nghiêm trọng, phản ứng tức thì với hầu hết các thành phần hoạt tính mạnh."; break;
            default: skinDesc = "Làn da cần được chăm sóc cân bằng và bảo vệ hàng ngày với các sản phẩm phù hợp."; break;
        }
        binding.tvSkinTypeDesc.setText(skinDesc);

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnDone.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new QuizTestIntroFragment())
                        .commit());

        String finalSkinType = skinType;
        String finalRecommendation = recommendation;
        Set<String> finalFlaggedSet = flaggedSet;
        binding.btnSaveProfile.setOnClickListener(v -> saveSkinProfile(prefs, finalSkinType, finalRecommendation, finalFlaggedSet, sensitivity, hydration, elasticity, sebum, false));

        boolean isFirstTest = getArguments() != null && getArguments().getBoolean("IS_FIRST_TEST", false);
        if (isFirstTest) {
            binding.llAiRoutineSection.setVisibility(View.VISIBLE);
            binding.llFooterButtons.setVisibility(View.GONE);
            setupAiRoutineListeners(prefs, finalSkinType, finalRecommendation, finalFlaggedSet, sensitivity, hydration, elasticity, sebum);
            loadAiRoutine(finalSkinType, hydration, sebum, sensitivity, elasticity);
        } else {
            binding.llAiRoutineSection.setVisibility(View.GONE);
            binding.llFooterButtons.setVisibility(View.VISIBLE);
            binding.btnSaveProfile.setVisibility(View.GONE);
            binding.btnSuggestRoutine.setVisibility(View.GONE);
        }

        binding.cardAiAdvice.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.ai.SkinAiChatFragment())
                        .addToBackStack(null).commit());

        processIngredients(finalFlaggedSet);
        recommendProducts(finalFlaggedSet);

        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_myskin, tabId -> {
            BottomNavHelper.navigate(this, tabId);
            return null;
        });
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

    private void recommendProducts(Set<String> flaggedGroups) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("products.json")));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            JSONObject root = new JSONObject(sb.toString());
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

            binding.llProductsContainer.removeAllViews();
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
                        detailedIngredientsList.add(detailedArray.getString(j).toLowerCase());
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

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.quiz_item_product_recommendation, binding.llProductsContainer, false);
                TextView tvProdName = itemView.findViewById(R.id.tv_product_name);
                TextView tvProdDesc = itemView.findViewById(R.id.tv_product_desc);
                TextView tvBadge = itemView.findViewById(R.id.tv_compatibility_badge);
                ImageView ivBadgeIcon = itemView.findViewById(R.id.iv_badge_icon);
                ImageView ivImage = itemView.findViewById(R.id.iv_product_image);

                tvProdName.setText(prodName);
                if (!mainImage.isEmpty()) {
                    ImageRequest req = new ImageRequest.Builder(requireContext())
                            .data(mainImage)
                            .crossfade(true)
                            .transformations(new RoundedCornersTransformation(10f))
                            .placeholder(R.drawable.myphamxanh)
                            .target(ivImage)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(req);
                } else {
                    ivImage.setImageResource(R.drawable.myphamxanh);
                }

                if (!triggeredAvoids.isEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F04758")));
                    tvBadge.setText("Nguy cơ kích ứng cao");
                    tvBadge.setTextColor(Color.parseColor("#F04758"));
                    Set<String> uniqueNames = new HashSet<>();
                    for (String c : triggeredAvoids) uniqueNames.add(getViName(c));
                    String namesStr = String.join(", ", uniqueNames);
                    tvProdDesc.setText("Sản phẩm chứa " + namesStr + " không phù hợp với da nhạy cảm của bạn.");
                } else if (!triggeredCautions.isEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_circle);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E29400")));
                    tvBadge.setText("Cần thận trọng khi dùng");
                    tvBadge.setTextColor(Color.parseColor("#E29400"));
                    Set<String> uniqueNames = new HashSet<>();
                    for (String c : triggeredCautions) uniqueNames.add(getViName(c));
                    String namesStr = String.join(", ", uniqueNames);
                    tvProdDesc.setText("Sản phẩm chứa " + namesStr + " cần lưu ý theo dõi khi sử dụng.");
                } else {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_selected);
                    ivBadgeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#375633")));
                    tvBadge.setText("Lành tính - Khuyên dùng");
                    tvBadge.setTextColor(Color.parseColor("#375633"));
                    tvProdDesc.setText("Công thức tối giản, lành tính giúp củng cố lớp màng ẩm tự nhiên mà không gây bí da.");
                }

                binding.llProductsContainer.addView(itemView);
                count++;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupAiRoutineListeners(SharedPreferences prefs, String skinType, String recommendation, Set<String> flaggedSet, int sensitivity, int hydration, int elasticity, int sebum) {
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
        binding.btnApplyRoutine.setOnClickListener(v -> saveSelectedStepsToProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum));
        binding.tvRetakeQuizInline.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new QuizTestIntroFragment()).commit());
    }

    private void updateTabUI() {
        if (isMorningTab) {
            binding.tabMorning.setBackgroundResource(R.drawable.bg_btn_solid);
            binding.tabMorning.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            binding.tvTabMorning.setTextColor(Color.WHITE);
            binding.tabEvening.setBackgroundResource(android.R.color.transparent);
            binding.tabEvening.setBackgroundTintList(null);
            binding.tvTabEvening.setTextColor(Color.parseColor("#677559"));
        } else {
            binding.tabEvening.setBackgroundResource(R.drawable.bg_btn_solid);
            binding.tabEvening.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            binding.tvTabEvening.setTextColor(Color.WHITE);
            binding.tabMorning.setBackgroundResource(android.R.color.transparent);
            binding.tabMorning.setBackgroundTintList(null);
            binding.tvTabMorning.setTextColor(Color.parseColor("#677559"));
        }
    }

    private void loadAiRoutine(String skinType, int hydration, int sebum, int sensitivity, int elasticity) {
        if (GEMINI_API_KEY.trim().isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity);
        } else {
            fetchRoutineFromAi(skinType, hydration, sebum, sensitivity, elasticity);
        }
    }

    private void fetchRoutineFromAi(String skinType, int hydration, int sebum, int sensitivity, int elasticity) {
        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getIO(), (s2, c2) -> {
                    try {
                        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setConnectTimeout(12000);
                        connection.setReadTimeout(12000);
                        connection.setDoOutput(true);

                        String prompt = "Bạn là trợ lý Rootie AI chuyên thiết kế chu trình dưỡng da thuần chay (skincare routine) phù hợp nhất với các chỉ số da người dùng.\n" +
                                "Hãy phân tích chi tiết làn da người dùng có các chỉ số sau:\n" +
                                "- Loại da: " + skinType + "\n" +
                                "- Độ ẩm da: " + hydration + "%\n" +
                                "- Chỉ số bã nhờn: " + sebum + "%\n" +
                                "- Độ nhạy cảm da: " + sensitivity + "%\n" +
                                "- Độ đàn hồi: " + elasticity + "%\n" +
                                "\n" +
                                "Hãy đề xuất một routine buổi sáng và routine buổi tối phù hợp nhất sử dụng các sản phẩm thuần chay (Ví dụ: Gel rửa mặt bí đao, Tinh chất bí đao, Thạch bí đao, Thạch hoa hồng dưỡng ẩm, Sữa rửa mặt nghệ Hưng Yên, Nước hoa hồng Cao Bằng, Nước tẩy trang sen Hậu Giang, Sữa chống nắng bí đao...).\n" +
                                "Lưu ý: Giải thích lý do vì sao mỗi sản phẩm/bước dưỡng lại hoàn hảo cho các chỉ số cụ thể của làn da này.\n" +
                                "\n" +
                                "Trả về cấu trúc JSON chính xác như sau và KHÔNG kèm bất cứ ký tự nào khác ngoài JSON:\n" +
                                "{\n" +
                                "  \"assessment\": \"Một đoạn phân tích chi tiết, khoa học và ấm áp về tình trạng da của người dùng dựa trên độ ẩm, dầu nhờn và độ nhạy cảm.\",\n" +
                                "  \"morning_steps\": [\n" +
                                "    {\n" +
                                "      \"name\": \"Tên bước (ví dụ: Sữa rửa mặt)\",\n" +
                                "      \"product\": \"Tên sản phẩm thuần chay (ví dụ: Gel rửa mặt bí đao)\",\n" +
                                "      \"reason\": \"Giải thích chi tiết tại sao phù hợp (ví dụ: Giúp làm sạch sâu bã nhờn ở mức " + sebum + "% nhưng giữ ẩm nhẹ nhàng cho da nhạy cảm)\"\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"evening_steps\": [\n" +
                                "    {\n" +
                                "      \"name\": \"Tên bước\",\n" +
                                "      \"product\": \"Tên sản phẩm\",\n" +
                                "      \"reason\": \"Giải thích chi tiết tại sao phù hợp\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}";

                        JSONObject requestJson = new JSONObject();
                        JSONArray partsArray = new JSONArray();
                        partsArray.put(new JSONObject().put("text", prompt));
                        JSONArray contentsArray = new JSONArray();
                        contentsArray.put(new JSONObject().put("parts", partsArray));
                        requestJson.put("contents", contentsArray);

                        JSONObject systemInstruction = new JSONObject();
                        JSONArray systemParts = new JSONArray();
                        systemParts.put(new JSONObject().put("text", "Bạn chỉ trả về duy nhất chuỗi JSON hợp lệ theo cấu trúc được yêu cầu."));
                        systemInstruction.put("parts", systemParts);
                        requestJson.put("systemInstruction", systemInstruction);

                        JSONObject generationConfig = new JSONObject();
                        generationConfig.put("temperature", 0.3);
                        generationConfig.put("maxOutputTokens", 2000);
                        requestJson.put("generationConfig", generationConfig);

                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = requestJson.toString().getBytes("UTF-8");
                            os.write(input, 0, input.length);
                        }

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder responseStr = new StringBuilder(); String line;
                            while ((line = br.readLine()) != null) responseStr.append(line);
                            br.close();

                            JSONObject json = new JSONObject(responseStr.toString());
                            JSONArray candidates = json.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                                JSONArray parts = content.getJSONArray("parts");
                                if (parts.length() > 0) {
                                    String textResult = parts.getJSONObject(0).getString("text").trim();

                                    String cleanJson = textResult;
                                    if (textResult.startsWith("```json")) cleanJson = textResult.substring(7, textResult.lastIndexOf("```")).trim();
                                    else if (textResult.startsWith("```")) cleanJson = textResult.substring(3, textResult.lastIndexOf("```")).trim();

                                    JSONObject routineObj = new JSONObject(cleanJson);
                                    JSONArray morningJson = routineObj.getJSONArray("morning_steps");
                                    JSONArray eveningJson = routineObj.getJSONArray("evening_steps");

                                    BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                                        morningSteps.clear();
                                        for (int i = 0; i < morningJson.length(); i++) {
                                            JSONObject step = morningJson.getJSONObject(i);
                                            morningSteps.add(new AiSkincareStep(i, step.getString("name"), step.getString("product"), step.getString("reason"), true));
                                        }

                                        eveningSteps.clear();
                                        for (int i = 0; i < eveningJson.length(); i++) {
                                            JSONObject step = eveningJson.getJSONObject(i);
                                            eveningSteps.add(new AiSkincareStep(i, step.getString("name"), step.getString("product"), step.getString("reason"), true));
                                        }

                                        populateSteps();
                                        return kotlin.Unit.INSTANCE;
                                    }, c2);
                                    return kotlin.Unit.INSTANCE;
                                }
                            }
                        }

                        BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                            generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity);
                            return kotlin.Unit.INSTANCE;
                        }, c2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        BuildersKt.withContext(Dispatchers.getMain(), (s3, c3) -> {
                            generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity);
                            return kotlin.Unit.INSTANCE;
                        }, c2);
                    }
                    return kotlin.Unit.INSTANCE;
                }, cont));
    }

    private void generateRuleBasedRoutine(String skinType, int hydration, int sebum, int sensitivity, int elasticity) {
        String lowerSkinType = skinType.toLowerCase();
        morningSteps.clear();
        eveningSteps.clear();

        if (lowerSkinType.contains("dầu") || lowerSkinType.contains("mụn") || lowerSkinType.contains("hỗn hợp thiên dầu")) {
            morningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Sữa rửa mặt", "Gel rửa mặt Bí đao", "Loại bỏ dầu thừa (" + sebum + "%) nhẹ nhàng mà không làm khô căng da.", true),
                    new AiSkincareStep(1, "Nước cân bằng", "Nước bí đao cân bằng da", "Làm sạch sâu bã nhờn vùng chữ T và kháng viêm ngừa mụn.", true),
                    new AiSkincareStep(2, "Tinh chất", "Tinh chất bí đao", "Chứa 7% Niacinamide giúp kiềm dầu tối đa và thu nhỏ lỗ chân lông.", true),
                    new AiSkincareStep(3, "Gel dưỡng ẩm", "Thạch bí đao dưỡng ẩm", "Cấp ẩm mỏng nhẹ dạng gel-cream, không gây bít tắc nang lông.", true),
                    new AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ tối ưu khỏi tia UV với màng lọc kiềm dầu thoáng nhẹ.", true)
            ));

            eveningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang bí đao", "Hòa tan dầu thừa, bụi bẩn PM2.5 và kem chống nắng sâu trong lỗ chân lông.", true),
                    new AiSkincareStep(1, "Sữa rửa mặt", "Gel rửa mặt bí đao", "Làm sạch sâu da mặt để chuẩn bị cho các bước dưỡng tiếp theo.", true),
                    new AiSkincareStep(2, "Nước cân bằng", "Nước bí đao cân bằng da", "Cân bằng lại pH da và làm dịu nhanh các nốt mụn sưng đỏ.", true),
                    new AiSkincareStep(3, "Tinh chất", "Tinh chất bí đao", "Tập trung điều trị mụn ẩn và làm mờ thâm mụn ban đêm.", true),
                    new AiSkincareStep(4, "Dưỡng ẩm khóa nước", "Thạch bí đao dưỡng ẩm", "Giữ nước khóa ẩm dịu nhẹ giúp da phục hồi lúc ngủ.", true)
            ));
        } else if (lowerSkinType.contains("khô")) {
            morningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Sữa rửa mặt", "Sữa rửa mặt nghệ Hưng Yên", "Làm sạch dịu nhẹ không bọt, bổ sung beta-carotene dưỡng ẩm.", true),
                    new AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cấp nước bù ẩm tức thì cho lớp sừng khô ráp.", true),
                    new AiSkincareStep(2, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Cung cấp vitamin C và curcumin chống oxy hóa, sáng da mặt.", true),
                    new AiSkincareStep(3, "Kem dưỡng ẩm", "Thạch hoa hồng dưỡng ẩm", "Nuôi dưỡng làn da căng mướt suốt 24 giờ liên tục.", true),
                    new AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ màng ẩm của da khô khỏi ánh nắng trực tiếp.", true)
            ));

            eveningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang hoa hồng Cao Bằng", "Tẩy trang dịu nhẹ đồng thời cấp ẩm sâu cho da không bị khô ráp.", true),
                    new AiSkincareStep(1, "Sữa rửa mặt", "Sữa rửa mặt nghệ Hưng Yên", "Làm sạch sâu bụi bẩn mà vẫn giữ lại độ ẩm tự nhiên cho da.", true),
                    new AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cân bằng độ pH và bù nước tức thì sau khi rửa mặt.", true),
                    new AiSkincareStep(3, "Tinh chất phục hồi", "Tinh chất hoa hồng Cao Bằng", "Bổ sung acid amin nuôi dưỡng sâu các tế bào da mất nước.", true),
                    new AiSkincareStep(4, "Kem dưỡng đêm", "Thạch hoa hồng dưỡng ẩm", "Khóa dưỡng chất ban đêm giúp da mướt mịn căng tràn vào sáng hôm sau.", true)
            ));
        } else if (lowerSkinType.contains("nhạy cảm") || lowerSkinType.contains("dễ kích ứng")) {
            morningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Sữa rửa mặt", "Sữa rửa mặt sen Hậu Giang", "Bảo vệ màng lipid nhạy cảm (" + sensitivity + "%), làm sạch không sulfate.", true),
                    new AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Làm dịu nhanh cảm giác châm chích và đỏ rát.", true),
                    new AiSkincareStep(2, "Tinh chất phục hồi", "Tinh chất rau má", "Kích thích sinh collagen, phục hồi hàng rào bảo vệ bị suy yếu.", true),
                    new AiSkincareStep(3, "Dưỡng ẩm làm dịu", "Thạch bí đao dưỡng ẩm", "Làm mát và dưỡng ẩm dịu nhẹ cho da nhạy cảm cực kỳ an toàn.", true),
                    new AiSkincareStep(4, "Chống nắng vật lý", "Sữa chống nắng bí đao", "Bảo vệ da dịu nhẹ nhất khỏi tia UV mà không gây bí hay ngứa.", true)
            ));

            eveningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang sen Hậu Giang", "Công thức Micellar làm sạch nhẹ nhàng không gây rát da.", true),
                    new AiSkincareStep(1, "Sữa rửa mặt", "Sữa rửa mặt sen Hậu Giang", "Sạch sâu dịu nhẹ bảo vệ da khỏi kích ứng.", true),
                    new AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Bù ẩm và cân bằng lại trạng thái ổn định cho da.", true),
                    new AiSkincareStep(3, "Tinh chất", "Tinh chất rau má", "Hỗ trợ làm lành nhanh các tổn thương của biểu bì nhạy cảm ban đêm.", true),
                    new AiSkincareStep(4, "Khóa ẩm dịu mát", "Thạch bí đao dưỡng ẩm", "Khóa ẩm dịu nhẹ không cồn, không hương liệu cho da nhạy cảm.", true)
            ));
        } else {
            morningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Sữa rửa mặt", "Gel rửa mặt cà phê", "Rửa mặt sảng khoái với hạt cà phê siêu mịn khơi dậy năng lượng da.", true),
                    new AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cấp ẩm dị mọc và cân bằng độ pH lý tưởng.", true),
                    new AiSkincareStep(2, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Làm đều màu da, mờ thâm mụn, chống oxy hóa.", true),
                    new AiSkincareStep(3, "Dưỡng ẩm", "Thạch hoa hồng dưỡng ẩm", "Dưỡng da căng mướt mịn màng tự nhiên.", true),
                    new AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ da tối ưu dưới tia UV gay gắt.", true)
            ));

            eveningSteps.addAll(Arrays.asList(
                    new AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang sen Hậu Giang", "Sạch thoáng bụi bẩn bã nhờn sau ngày dài.", true),
                    new AiSkincareStep(1, "Sữa rửa mặt", "Gel rửa mặt cà phê", "Làm sạch sâu lỗ chân lông nhẹ nhàng.", true),
                    new AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Khôi phục lại màng ẩm sau khi rửa mặt.", true),
                    new AiSkincareStep(3, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Làm mờ thâm mụn sạm màu hiệu quả ban đêm.", true),
                    new AiSkincareStep(4, "Dưỡng ẩm ban đêm", "Thạch hoa hồng dưỡng ẩm", "Khóa ẩm giúp da mềm mịn và rạng rỡ vào sáng hôm sau.", true)
            ));
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

            ivCheckbox.setImageResource(step.isChecked() ? R.drawable.skin_ic_checkbox_checked : R.drawable.skin_ic_checkbox_unchecked);

            View.OnClickListener clickListener = v -> {
                step.setChecked(!step.isChecked());
                ivCheckbox.setImageResource(step.isChecked() ? R.drawable.skin_ic_checkbox_checked : R.drawable.skin_ic_checkbox_unchecked);
            };

            ivCheckbox.setOnClickListener(clickListener);
            layoutCard.setOnClickListener(clickListener);

            binding.llAiRoutineStepsContainer.addView(stepView);
        }
    }

    private void saveSkinProfile(SharedPreferences prefs, String skinType, String recommendation, Set<String> flaggedSet, int sensitivity, int hydration, int elasticity, int sebum, boolean silent) {
        String skinAreas = prefs.getString("SKIN_AREAS_DESC", "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da.");

        long lastTestTime = ProfileSession.getLastSkinTestTime(requireContext());
        long currentTime = System.currentTimeMillis();
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        boolean isEligibleForReward = lastTestTime == 0L || (currentTime - lastTestTime >= sevenDaysMs);

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
                .apply();

        try {
            String historyStr = prefs.getString("QUIZ_HISTORY_LIST", "[]");
            if (historyStr == null) historyStr = "[]";
            JSONArray historyArray = new JSONArray(historyStr);

            JSONObject newLog = new JSONObject();
            newLog.put("date", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            newLog.put("skinType", skinType);
            newLog.put("recommendation", recommendation);
            newLog.put("sensitivity", sensitivity);
            newLog.put("hydration", hydration);
            newLog.put("elasticity", elasticity);
            newLog.put("sebum", sebum);

            historyArray.put(newLog);
            prefs.edit().putString("QUIZ_HISTORY_LIST", historyArray.toString()).apply();
        } catch (Exception e) { e.printStackTrace(); }

        if (isEligibleForReward) {
            RootieDatabase db = RootieDatabase.getDatabase(requireContext());
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                    BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                        try {
                            db.rewardPointDao().insertRewardPoints(new RewardPointEntity(
                                    0, "SYSTEM_WEEKLY_QUIZ", 100, "Cập nhật làn da định kỳ hàng tuần (+100 xu)", currentTime
                            ));
                            com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(requireContext());
                        } catch (Exception e) { e.printStackTrace(); }

                        if (!silent) {
                            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null);
                            TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
                            TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
                            View llCoinBadge = dialogView.findViewById(R.id.ll_dialog_coin_badge);
                            TextView tvCoinText = dialogView.findViewById(R.id.tv_dialog_coin_text);
                            View btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
                            TextView tvConfirmText = dialogView.findViewById(R.id.tv_dialog_confirm_text);
                            View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

                            tvTitle.setText("Nhận 100 xu thành công!");
                            tvMsg.setText("Cảm ơn bạn đã cập nhật chỉ số da định kỳ. Bạn được cộng +100 xu vào ví thành viên!");
                            llCoinBadge.setVisibility(View.VISIBLE);
                            tvCoinText.setText("Tặng +100 Xu thành viên");
                            tvConfirmText.setText("TUYỆT VỜI");
                            btnCancel.setVisibility(View.GONE);

                            AlertDialog customDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
                            if (customDialog.getWindow() != null) customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                            btnConfirm.setOnClickListener(v -> {
                                customDialog.dismiss();
                                getParentFragmentManager().popBackStack();
                            });
                            customDialog.setOnDismissListener(dialog -> getParentFragmentManager().popBackStack());
                            customDialog.show();
                        }
                        return kotlin.Unit.INSTANCE;
                    }, cont));
        } else {
            if (!silent) {
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null);
                TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
                TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
                View btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
                TextView tvConfirmText = dialogView.findViewById(R.id.tv_dialog_confirm_text);
                View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

                tvTitle.setText("Đã lưu hồ sơ da!");
                tvMsg.setText("Chỉ số da và loại da " + skinType + " đã được lưu vào lịch sử của bạn.");
                tvConfirmText.setText("ĐỒNG Ý");
                btnCancel.setVisibility(View.GONE);

                AlertDialog customDialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
                if (customDialog.getWindow() != null) customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                btnConfirm.setOnClickListener(v -> {
                    customDialog.dismiss();
                    getParentFragmentManager().popBackStack();
                });
                customDialog.setOnDismissListener(dialog -> getParentFragmentManager().popBackStack());
                customDialog.show();
            }
        }
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

        saveSkinProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum, true);

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
        if (customDialog.getWindow() != null) customDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        final boolean[] isNavigatingToReminder = {false};
        btnConfirm.setOnClickListener(v -> {
            isNavigatingToReminder[0] = true;
            customDialog.dismiss();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnCancel.setOnClickListener(v -> {
            customDialog.dismiss();
            getParentFragmentManager().popBackStack();
        });

        customDialog.setOnDismissListener(dialog -> {
            if (!isNavigatingToReminder[0]) getParentFragmentManager().popBackStack();
        });

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
