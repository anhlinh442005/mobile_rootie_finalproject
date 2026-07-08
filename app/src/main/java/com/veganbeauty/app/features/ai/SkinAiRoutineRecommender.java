package com.veganbeauty.app.features.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/** Gợi ý routine sáng/tối theo loại da + chỉ số quiz, dùng sản phẩm thật trong catalog. */
public final class SkinAiRoutineRecommender {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private SkinAiRoutineRecommender() {
    }

    public static final class RoutineStep {
        public final String stepName;
        public final String productName;
        public final String reason;

        public RoutineStep(String stepName, String productName, String reason) {
            this.stepName = stepName;
            this.productName = productName;
            this.reason = reason;
        }
    }

    public static final class RoutinePlan {
        public final String assessment;
        public final List<RoutineStep> morningSteps;
        public final List<RoutineStep> eveningSteps;

        public RoutinePlan(String assessment, List<RoutineStep> morningSteps, List<RoutineStep> eveningSteps) {
            this.assessment = assessment;
            this.morningSteps = morningSteps;
            this.eveningSteps = eveningSteps;
        }
    }

    public static RoutinePlan recommend(
            Context context,
            String skinType,
            int hydration,
            int sebum,
            int sensitivity,
            int elasticity,
            Set<String> flaggedGroups,
            String geminiApiKey) {

        RoutinePlan rulePlan = buildRuleBasedPlan(context, skinType, hydration, sebum, sensitivity, elasticity, flaggedGroups);
        if (!SkinAiAssistantHelper.isGeminiConfigured(geminiApiKey)) {
            return rulePlan;
        }
        RoutinePlan geminiPlan = tryGeminiPlan(context, skinType, hydration, sebum, sensitivity, elasticity,
                flaggedGroups, geminiApiKey, rulePlan);
        return geminiPlan != null ? geminiPlan : rulePlan;
    }

    public static String formatRoutineForChat(RoutinePlan plan, boolean morning) {
        if (plan == null) return "";
        List<RoutineStep> steps = morning ? plan.morningSteps : plan.eveningSteps;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            RoutineStep s = steps.get(i);
            if (i > 0) sb.append(" → ");
            sb.append(s.stepName).append(": ").append(s.productName);
        }
        return sb.toString();
    }

    private static RoutinePlan buildRuleBasedPlan(
            Context context,
            String skinType,
            int hydration,
            int sebum,
            int sensitivity,
            int elasticity,
            Set<String> flaggedGroups) {

        try {
            JSONArray products = loadProductsArray(context);
            Set<String> avoid = buildAvoidChemicals(context, flaggedGroups);

            JSONObject remover = pickBest(products, avoid, skinType, ProductBucket.MAKEUP_REMOVER);
            JSONObject cleanser = pickBest(products, avoid, skinType, ProductBucket.CLEANSER);
            JSONObject toner = pickBest(products, avoid, skinType, ProductBucket.TONER);
            JSONObject serum = pickBest(products, avoid, skinType, ProductBucket.SERUM);
            JSONObject moisturizer = pickBest(products, avoid, skinType, ProductBucket.MOISTURIZER);
            JSONObject sunscreen = pickBest(products, avoid, skinType, ProductBucket.SUNSCREEN);

            String st = skinType.toLowerCase(Locale.getDefault());
            List<RoutineStep> morning = new ArrayList<>();
            List<RoutineStep> evening = new ArrayList<>();

            morning.add(step("Sữa rửa mặt", cleanser, reasonCleanser(st, sebum, sensitivity)));
            morning.add(step("Nước cân bằng", toner, reasonToner(hydration, sensitivity)));
            morning.add(step("Tinh chất", serum, reasonSerum(st, sebum, hydration, sensitivity)));
            morning.add(step("Dưỡng ẩm", moisturizer, reasonMoisturizer(st, hydration, sebum)));
            morning.add(step("Chống nắng", sunscreen, reasonSunscreen(st, sensitivity)));

            evening.add(step("Tẩy trang", remover != null ? remover : cleanser, reasonRemover(sebum)));
            evening.add(step("Sữa rửa mặt", cleanser, "Làm sạch sâu bụi bẩn và bã nhờn (" + sebum + "%) sau một ngày dài."));
            evening.add(step("Nước cân bằng", toner, reasonToner(hydration, sensitivity)));
            evening.add(step("Tinh chất", serum, reasonSerumNight(st, sensitivity, elasticity)));
            evening.add(step("Khóa ẩm", moisturizer, reasonMoisturizerNight(hydration, sensitivity)));

            String assessment = buildAssessment(skinType, hydration, sebum, sensitivity, elasticity, flaggedGroups);
            return new RoutinePlan(assessment, morning, evening);
        } catch (Exception e) {
            return fallbackPlan(skinType, hydration, sebum, sensitivity);
        }
    }

    private static RoutinePlan fallbackPlan(String skinType, int hydration, int sebum, int sensitivity) {
        List<RoutineStep> morning = new ArrayList<>();
        morning.add(new RoutineStep("Sữa rửa mặt", "Gel rửa mặt dịu nhẹ", "Làm sạch phù hợp " + skinType + "."));
        morning.add(new RoutineStep("Tinh chất", "Serum cấp ẩm", "Bù ẩm khi da chỉ đạt " + hydration + "%."));
        morning.add(new RoutineStep("Dưỡng ẩm", "Kem dưỡng khóa ẩm", "Giữ nước cho da nhạy cảm ~" + sensitivity + "%."));
        morning.add(new RoutineStep("Chống nắng", "Kem chống nắng SPF50+", "Bảo vệ da hàng ngày."));

        List<RoutineStep> evening = new ArrayList<>(morning);
        String assessment = "Với " + skinType + ", ưu tiên làm sạch dịu, cấp ẩm và chống nắng mỗi ngày.";
        return new RoutinePlan(assessment, morning, evening);
    }

    private static RoutinePlan tryGeminiPlan(
            Context context,
            String skinType,
            int hydration,
            int sebum,
            int sensitivity,
            int elasticity,
            Set<String> flaggedGroups,
            String apiKey,
            RoutinePlan fallback) {

        try {
            String catalog = buildProductCatalogSnippet(context, skinType, flaggedGroups);
            String prompt = "Thiết kế routine thuần chay cho:\n"
                    + "- Loại da: " + skinType + "\n"
                    + "- Cấp ẩm: " + hydration + "%, bã nhờn: " + sebum + "%, nhạy cảm: " + sensitivity
                    + "%, đàn hồi: " + elasticity + "%\n"
                    + "- Thành phần cần tránh: "
                    + (flaggedGroups == null || flaggedGroups.isEmpty() ? "không ghi nhận" : String.join(", ", flaggedGroups)) + "\n\n"
                    + "CHỈ dùng sản phẩm trong danh sách sau:\n" + catalog + "\n\n"
                    + "Trả về JSON duy nhất:\n"
                    + "{\"assessment\":\"...\",\"morning_steps\":[{\"name\":\"...\",\"product\":\"...\",\"reason\":\"...\"}],"
                    + "\"evening_steps\":[{\"name\":\"...\",\"product\":\"...\",\"reason\":\"...\"}]}\n"
                    + "Mỗi routine 4-5 bước. Tiếng Việt. Lý do phải nhắc chỉ số da cụ thể.";

            String jsonText = callGeminiJson(apiKey, prompt);
            if (jsonText == null) return null;

            JSONObject obj = new JSONObject(jsonText);
            String assessment = obj.optString("assessment", fallback.assessment);
            List<RoutineStep> morning = parseSteps(obj.optJSONArray("morning_steps"));
            List<RoutineStep> evening = parseSteps(obj.optJSONArray("evening_steps"));
            if (morning.isEmpty() || evening.isEmpty()) return null;
            return new RoutinePlan(assessment, morning, evening);
        } catch (Exception e) {
            return null;
        }
    }

    private static String callGeminiJson(String apiKey, String prompt) throws Exception {
        URL url = new URL(GEMINI_URL + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);

        JSONObject requestJson = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        JSONArray contents = new JSONArray();
        contents.put(new JSONObject().put("role", "user").put("parts", parts));
        requestJson.put("contents", contents);

        JSONObject systemInstruction = new JSONObject();
        systemInstruction.put("parts", new JSONArray().put(new JSONObject().put("text",
                "Bạn là Rootie AI. Chỉ trả về JSON hợp lệ, không markdown.")));
        requestJson.put("systemInstruction", systemInstruction);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.35);
        generationConfig.put("maxOutputTokens", 1800);
        requestJson.put("generationConfig", generationConfig);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;

        Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        connection.disconnect();

        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return null;
        String text = candidates.getJSONObject(0).getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0).getString("text").trim();
        return extractJson(text);
    }

    private static String extractJson(String text) {
        String clean = text.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7, clean.lastIndexOf("```")).trim();
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3, clean.lastIndexOf("```")).trim();
        }
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) return clean.substring(start, end + 1);
        return clean;
    }

    private static List<RoutineStep> parseSteps(JSONArray arr) throws Exception {
        List<RoutineStep> steps = new ArrayList<>();
        if (arr == null) return steps;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            steps.add(new RoutineStep(
                    o.getString("name"),
                    o.getString("product"),
                    o.optString("reason", "")));
        }
        return steps;
    }

    private static String buildProductCatalogSnippet(Context context, String skinType, Set<String> flaggedGroups) {
        try {
            JSONArray products = loadProductsArray(context);
            Set<String> avoid = buildAvoidChemicals(context, flaggedGroups);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (int i = 0; i < products.length() && count < 20; i++) {
                JSONObject p = products.getJSONObject(i);
                if (!isRoutineCandidate(p) || containsAvoidedIngredient(p, avoid)) continue;
                if (skinScore(p, skinType) < 10) continue;
                sb.append("- ").append(p.getString("name"))
                        .append(" (").append(p.optString("suitableFor", "")).append(")\n");
                count++;
            }
            return sb.length() > 0 ? sb.toString() : "- Gel rửa mặt, nước cân bằng, tinh chất, kem dưỡng, chống nắng thuần chay";
        } catch (Exception e) {
            return "";
        }
    }

    private enum ProductBucket {
        MAKEUP_REMOVER, CLEANSER, TONER, SERUM, MOISTURIZER, SUNSCREEN
    }

    private static JSONObject pickBest(JSONArray products, Set<String> avoid, String skinType, ProductBucket bucket) {
        JSONObject best = null;
        int bestScore = -1;
        for (int i = 0; i < products.length(); i++) {
            JSONObject p = products.optJSONObject(i);
            if (p == null || !isRoutineCandidate(p) || containsAvoidedIngredient(p, avoid)) continue;
            if (!matchesBucket(p, bucket)) continue;
            int score = skinScore(p, skinType);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private static boolean matchesBucket(JSONObject product, ProductBucket bucket) {
        String name = product.optString("name", "").toLowerCase(Locale.ROOT);
        List<String> subs = subcategories(product);
        switch (bucket) {
            case MAKEUP_REMOVER:
                return name.contains("tẩy trang") || name.contains("micellar");
            case CLEANSER:
                return (name.contains("rửa mặt") || name.contains("sữa rửa") || name.contains("gel rửa"))
                        && !name.contains("tẩy trang");
            case TONER:
                return name.contains("nước hoa hồng") || name.contains("nước bí đao")
                        || name.contains("nước sen") || name.contains("nước cân bằng")
                        || hasSub(subs, "nước cân bằng");
            case SERUM:
                return (name.contains("tinh chất") && !name.contains("tắm") && !name.contains("gội"))
                        || hasSub(subs, "tinh chất");
            case MOISTURIZER:
                return name.contains("thạch") || name.contains("kem dưỡng") || name.contains("gel dưỡng")
                        || hasSub(subs, "kem dưỡng");
            case SUNSCREEN:
                return name.contains("chống nắng") || hasSub(subs, "chống nắng");
            default:
                return false;
        }
    }

    private static boolean isRoutineCandidate(JSONObject p) {
        String cat = p.optString("category", "").toLowerCase(Locale.ROOT);
        String name = p.optString("name", "").toLowerCase(Locale.ROOT);
        return !cat.contains("combo") && !name.contains("giftbox") && !name.contains("tặng ")
                && !name.contains(" + ");
    }

    private static List<String> subcategories(JSONObject product) {
        List<String> subs = new ArrayList<>();
        Object raw = product.opt("subcategory");
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            for (int i = 0; i < arr.length(); i++) subs.add(arr.optString(i, "").toLowerCase(Locale.ROOT));
        } else if (raw instanceof String) {
            subs.add(((String) raw).toLowerCase(Locale.ROOT));
        }
        return subs;
    }

    private static boolean hasSub(List<String> subs, String needle) {
        for (String s : subs) if (s.contains(needle)) return true;
        return false;
    }

    private static int skinScore(JSONObject product, String skinType) {
        String suitable = product.optString("suitableFor", "").toLowerCase(Locale.ROOT);
        String desc = product.optString("description", "").toLowerCase(Locale.ROOT);
        String st = skinType.toLowerCase(Locale.ROOT);
        int score = 0;
        if (st.contains("dầu") || st.contains("mụn")) {
            if (suitable.contains("dầu") || suitable.contains("hỗn hợp")) score += 20;
            if (desc.contains("kiềm dầu") || desc.contains("bã nhờn")) score += 15;
        }
        if (st.contains("khô") || st.contains("mất nước")) {
            if (suitable.contains("khô")) score += 20;
            if (desc.contains("cấp ẩm") || desc.contains("khóa ẩm")) score += 15;
        }
        if (st.contains("nhạy cảm") || st.contains("kích ứng")) {
            if (suitable.contains("nhạy cảm") || desc.contains("dịu nhẹ") || desc.contains("lành tính")) score += 20;
        }
        if (suitable.contains("mọi loại da") || suitable.contains("mọi người")) score += 8;
        return score;
    }

    private static JSONArray loadProductsArray(Context context) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("products.json")))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return new JSONObject(sb.toString()).getJSONArray("products");
    }

    private static Set<String> buildAvoidChemicals(Context context, Set<String> flaggedGroups) {
        Set<String> avoid = new HashSet<>();
        if (flaggedGroups == null || flaggedGroups.isEmpty()) return avoid;
        try {
            StringBuilder jsonStr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("quiz_thanhphan.json")))) {
                String line;
                while ((line = reader.readLine()) != null) jsonStr.append(line);
            }
            JSONArray ingredients = new JSONObject(jsonStr.toString()).getJSONArray("ingredients");
            for (int i = 0; i < ingredients.length(); i++) {
                JSONObject ing = ingredients.getJSONObject(i);
                if (!flaggedGroups.contains(ing.getString("category"))) continue;
                if ("avoid".equals(ing.optString("risk", ""))) {
                    avoid.add(ing.getString("name").toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ignored) {
        }
        return avoid;
    }

    private static boolean containsAvoidedIngredient(JSONObject product, Set<String> avoid) {
        if (avoid.isEmpty()) return false;
        try {
            if (product.has("detailedIngredients")) {
                JSONArray detailed = product.getJSONArray("detailedIngredients");
                for (int i = 0; i < detailed.length(); i++) {
                    String ing = detailed.getString(i).toLowerCase(Locale.ROOT);
                    for (String chem : avoid) {
                        if (ing.contains(chem)) return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static RoutineStep step(String stepName, JSONObject product, String fallbackReason) {
        String productName = product != null ? product.optString("name", "Sản phẩm phù hợp") : "Sản phẩm phù hợp";
        return new RoutineStep(stepName, productName, fallbackReason);
    }

    private static String buildAssessment(String skinType, int hydration, int sebum, int sensitivity,
                                          int elasticity, Set<String> flaggedGroups) {
        StringBuilder sb = new StringBuilder();
        sb.append("Theo kết quả quiz, bạn là ").append(skinType).append(" — cấp ẩm ")
                .append(hydration).append("%, bã nhờn ").append(sebum).append("%, nhạy cảm ")
                .append(sensitivity).append("%.");
        if (hydration < 45) sb.append(" Da cần ưu tiên cấp ẩm và khóa ẩm.");
        if (sebum >= 60) sb.append(" Nên kiểm soát dầu nhẹ, tránh làm khô căng.");
        if (sensitivity >= 55) sb.append(" Chọn sản phẩm dịu, tránh kích ứng.");
        if (flaggedGroups != null && !flaggedGroups.isEmpty()) {
            sb.append(" Tránh thành phần: ").append(String.join(", ", flaggedGroups)).append('.');
        }
        return sb.toString();
    }

    private static String reasonCleanser(String st, int sebum, int sensitivity) {
        if (st.contains("dầu") || st.contains("mụn")) {
            return "Kiểm soát bã nhờn " + sebum + "% mà không làm khô căng.";
        }
        if (st.contains("nhạy cảm")) {
            return "Làm sạch dịu cho da nhạy cảm " + sensitivity + "%, không sulfate gắt.";
        }
        return "Làm sạch nhẹ nhàng, phù hợp " + st + ".";
    }

    private static String reasonToner(int hydration, int sensitivity) {
        return "Cân bằng pH và bù ẩm (da " + hydration + "%), làm dịu nhạy cảm ~" + sensitivity + "%.";
    }

    private static String reasonSerum(String st, int sebum, int hydration, int sensitivity) {
        if (st.contains("dầu") || st.contains("mụn")) return "Hỗ trợ kiềm dầu và làm dịu mụn viêm.";
        if (hydration < 50) return "Cấp ẩm sâu khi da chỉ đạt " + hydration + "%.";
        if (sensitivity >= 55) return "Phục hồi hàng rào bảo vệ da nhạy cảm.";
        return "Dưỡng sáng và chống oxy hóa cho da khỏe.";
    }

    private static String reasonSerumNight(String st, int sensitivity, int elasticity) {
        if (st.contains("lão hóa") || elasticity < 60) return "Hỗ trợ chống lão hóa, cải thiện đàn hồi " + elasticity + "%.";
        if (sensitivity >= 55) return "Phục hồi và làm dịu da ban đêm.";
        return "Tập trung phục hồi da trong giấc ngủ.";
    }

    private static String reasonMoisturizer(String st, int hydration, int sebum) {
        if (st.contains("dầu")) return "Dưỡng ẩm dạng gel/mỏng, không bí lỗ chân lông.";
        if (hydration < 45) return "Khóa ẩm mạnh cho da thiếu nước (" + hydration + "%).";
        return "Duy trì độ ẩm cân bằng, bã nhờn ~" + sebum + "%.";
    }

    private static String reasonMoisturizerNight(int hydration, int sensitivity) {
        return "Khóa ẩm ban đêm — cấp ẩm " + hydration + "%, dịu da nhạy cảm " + sensitivity + "%.";
    }

    private static String reasonSunscreen(String st, int sensitivity) {
        if (sensitivity >= 55) return "Chống nắng dịu, không gây kích ứng.";
        if (st.contains("dầu")) return "Màng lọc mỏng, kiềm dầu, chống UV hàng ngày.";
        return "Bảo vệ da khỏi tia UV — bước bắt buộc mỗi sáng.";
    }

    private static String reasonRemover(int sebum) {
        return "Tẩy sạch makeup và kem chống nắng, hỗ trợ kiểm soát dầu " + sebum + "%.";
    }
}
