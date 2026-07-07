package com.veganbeauty.app.features.weather;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SkinWeatherProductMatcher {

    public static class ProductMatch {
        private final String id;
        private final String name;
        private final int suitabilityScore;
        private final String notes;
        private final String subcategory;
        private final String mainImage;

        public ProductMatch(String id, String name, int suitabilityScore, String notes, String subcategory, String mainImage) {
            this.id = id;
            this.name = name;
            this.suitabilityScore = suitabilityScore;
            this.notes = notes;
            this.subcategory = subcategory;
            this.mainImage = mainImage;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getSuitabilityScore() { return suitabilityScore; }
        public String getNotes() { return notes; }
        public String getSubcategory() { return subcategory; }
        public String getMainImage() { return mainImage; }
    }

    private static class ScoreNotesPair {
        final int score;
        final String notes;

        ScoreNotesPair(int score, String notes) {
            this.score = score;
            this.notes = notes;
        }
    }

    public static Map<String, ProductMatch> matchProductsForWeatherAndSkin(Context context, double temp, int humidity, String skinType) {
        return matchProductsForWeatherAndSkin(context, temp, humidity, skinType, new HashSet<>());
    }

    public static Map<String, ProductMatch> matchProductsForWeatherAndSkin(
            Context context, double temp, int humidity, String skinType, Set<String> flaggedGroups) {
        Map<String, ProductMatch> result = new HashMap<>();

        try {
            StringBuilder weathersJsonStr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("weathers.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    weathersJsonStr.append(line);
                }
            }
            JSONArray weathersArray = new JSONObject(weathersJsonStr.toString()).getJSONArray("weathers");
            String matchedWeatherId = "weather_008";
            double minDistance = Double.MAX_VALUE;

            for (int i = 0; i < weathersArray.length(); i++) {
                JSONObject item = weathersArray.getJSONObject(i);
                if (!item.optBoolean("is_active", true)) continue;

                JSONObject tempRange = item.getJSONObject("temperature_range");
                double tMin = tempRange.getDouble("min");
                double tMax = tempRange.getDouble("max");
                double tMid = (tMin + tMax) / 2.0;

                JSONObject humRange = item.getJSONObject("humidity_range");
                double hMin = humRange.getDouble("min");
                double hMax = humRange.getDouble("max");
                double hMid = (hMin + hMax) / 2.0;

                double distance = Math.abs(temp - tMid) + Math.abs((double) humidity - hMid);
                if (distance < minDistance) {
                    minDistance = distance;
                    matchedWeatherId = item.getString("id");
                }
            }

            StringBuilder mappingsJsonStr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("product_weather.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    mappingsJsonStr.append(line);
                }
            }
            JSONArray mappingsArray = new JSONObject(mappingsJsonStr.toString()).getJSONArray("mappings");
            Map<String, ScoreNotesPair> productScoreMap = new HashMap<>();

            for (int i = 0; i < mappingsArray.length(); i++) {
                JSONObject mapping = mappingsArray.getJSONObject(i);
                if (mapping.getString("weather_id").equals(matchedWeatherId)) {
                    String pId = mapping.getString("product_id");
                    int score = mapping.optInt("suitability_score", 0);
                    String notes = mapping.optString("notes", "");
                    productScoreMap.put(pId, new ScoreNotesPair(score, notes));
                }
            }

            StringBuilder productsJsonStr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("products.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    productsJsonStr.append(line);
                }
            }
            JSONArray productsArray = new JSONObject(productsJsonStr.toString()).getJSONArray("products");

            Set<String> avoidChemicals = buildAvoidChemicals(context, flaggedGroups);

            List<JSONObject> matchedProducts = new ArrayList<>();
            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject product = productsArray.getJSONObject(i);
                String pId = product.getString("id");
                if (!productScoreMap.containsKey(pId)) continue;
                if (containsAvoidedIngredient(product, avoidChemicals)) continue;
                matchedProducts.add(product);
            }

            List<JSONObject> cleansers = new ArrayList<>();
            List<JSONObject> serums = new ArrayList<>();
            List<JSONObject> moisturizers = new ArrayList<>();
            List<JSONObject> sunscreens = new ArrayList<>();

            for (JSONObject product : matchedProducts) {
                String name = product.getString("name").toLowerCase();

                List<String> subcategories = new ArrayList<>();
                Object subcatRaw = product.opt("subcategory");
                if (subcatRaw instanceof JSONArray) {
                    JSONArray arr = (JSONArray) subcatRaw;
                    for (int j = 0; j < arr.length(); j++) {
                        subcategories.add(arr.getString(j).toLowerCase());
                    }
                } else if (subcatRaw instanceof String) {
                    subcategories.add(((String) subcatRaw).toLowerCase());
                }

                boolean isCleanser = name.contains("rửa mặt") || name.contains("tẩy trang") || name.contains("làm sạch");
                if (!isCleanser) {
                    for (String s : subcategories) {
                        if (s.contains("rửa mặt") || s.contains("tẩy trang") || s.contains("làm sạch")) {
                            isCleanser = true;
                            break;
                        }
                    }
                }

                boolean isSerum = (name.contains("tinh chất") && !name.contains("tắm") && !name.contains("gội")) || name.contains("serum") || name.contains("essence") || name.contains("ampoule");
                if (!isSerum) {
                    for (String s : subcategories) {
                        if (s.contains("tinh chất") || s.contains("serum") || s.contains("essence")) {
                            isSerum = true;
                            break;
                        }
                    }
                }

                boolean isMoisturizer = name.contains("kem dưỡng") || name.contains("thạch dưỡng") || name.contains("thạch bí đao") || name.contains("gel dưỡng") || name.contains("lotion dưỡng") || name.contains("cream");
                if (!isMoisturizer) {
                    for (String s : subcategories) {
                        if (s.contains("dưỡng ẩm") || s.contains("kem dưỡng") || s.contains("thạch")) {
                            isMoisturizer = true;
                            break;
                        }
                    }
                }

                boolean isSunscreen = name.contains("chống nắng") || name.contains("sunscreen");
                if (!isSunscreen) {
                    for (String s : subcategories) {
                        if (s.contains("chống nắng") || s.contains("sunscreen")) {
                            isSunscreen = true;
                            break;
                        }
                    }
                }

                if (isCleanser) cleansers.add(product);
                else if (isSerum) serums.add(product);
                else if (isMoisturizer) moisturizers.add(product);
                else if (isSunscreen) sunscreens.add(product);
            }

            ProductMatch cleanserMatch = getBestProduct(cleansers, productScoreMap, skinType);
            if (cleanserMatch != null) result.put("Cleanser", cleanserMatch);

            ProductMatch serumMatch = getBestProduct(serums, productScoreMap, skinType);
            if (serumMatch != null) result.put("Serum", serumMatch);

            ProductMatch moisturizerMatch = getBestProduct(moisturizers, productScoreMap, skinType);
            if (moisturizerMatch != null) result.put("Moisturizer", moisturizerMatch);

            ProductMatch sunscreenMatch = getBestProduct(sunscreens, productScoreMap, skinType);
            if (sunscreenMatch != null) result.put("Sunscreen", sunscreenMatch);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static int getSkinCompatibilityScore(JSONObject product, String skinType) {
        String suitableFor = product.optString("suitableFor", "").toLowerCase();
        String skinConcerns = product.optString("skinConcerns", "").toLowerCase();
        String description = product.optString("description", "").toLowerCase();

        int score = 0;
        String st = skinType.toLowerCase();

        if (st.contains("dầu")) {
            if (suitableFor.contains("dầu") || suitableFor.contains("hỗn hợp")) score += 20;
            if (skinConcerns.contains("dầu") || skinConcerns.contains("mụn")) score += 20;
            if (description.contains("kiềm dầu") || description.contains("bã nhờn")) score += 10;
        }
        if (st.contains("khô")) {
            if (suitableFor.contains("khô")) score += 20;
            if (suitableFor.contains("mọi loại da") || suitableFor.contains("mọi người")) score += 10;
            if (description.contains("cấp ẩm") || description.contains("khóa ẩm") || description.contains("khô căng")) score += 15;
        }
        if (st.contains("nhạy cảm")) {
            if (suitableFor.contains("nhạy cảm") || suitableFor.contains("dịu nhẹ")) score += 20;
            if (description.contains("nhạy cảm") || description.contains("lành tính") || description.contains("dịu nhẹ")) score += 10;
        }
        if (st.contains("mụn")) {
            if (suitableFor.contains("mụn")) score += 20;
            if (skinConcerns.contains("mụn")) score += 20;
            if (description.contains("giảm mụn") || description.contains("kháng viêm")) score += 10;
        }
        if (suitableFor.contains("mọi loại da") || suitableFor.contains("mọi người") || suitableFor.trim().isEmpty()) {
            score += 5;
        }
        return score;
    }

    private static ProductMatch getBestProduct(List<JSONObject> list, Map<String, ScoreNotesPair> productScoreMap, String skinType) throws Exception {
        if (list == null || list.isEmpty()) return null;

        JSONObject bestProduct = null;
        int bestTotalScore = -1;
        int bestWeatherScore = 0;
        String bestNotes = "";

        for (JSONObject prod : list) {
            String pId = prod.getString("id");
            ScoreNotesPair pair = productScoreMap.get(pId);
            if (pair == null) continue;

            int weatherScore = pair.score;
            String notes = pair.notes;

            int skinScore = getSkinCompatibilityScore(prod, skinType);
            int totalScore = weatherScore + skinScore;

            if (totalScore > bestTotalScore) {
                bestTotalScore = totalScore;
                bestProduct = prod;
                bestWeatherScore = weatherScore;
                bestNotes = notes;
            }
        }

        if (bestProduct != null) {
            Object subcatRaw = bestProduct.opt("subcategory");
            String subcatStr = "";
            if (subcatRaw instanceof JSONArray) {
                JSONArray arr = (JSONArray) subcatRaw;
                if (arr.length() > 0) subcatStr = arr.getString(0);
            } else if (subcatRaw instanceof String) {
                subcatStr = (String) subcatRaw;
            }

            return new ProductMatch(
                    bestProduct.getString("id"),
                    bestProduct.getString("name"),
                    bestWeatherScore,
                    bestNotes,
                    subcatStr,
                    bestProduct.optString("mainImage", "")
            );
        }
        return null;
    }

    private static Set<String> buildAvoidChemicals(Context context, Set<String> flaggedGroups) {
        Set<String> avoidChemicals = new HashSet<>();
        if (flaggedGroups == null || flaggedGroups.isEmpty()) {
            return avoidChemicals;
        }
        try {
            StringBuilder jsonStr = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("quiz_thanhphan.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonStr.append(line);
                }
            }
            JSONArray ingredients = new JSONObject(jsonStr.toString()).getJSONArray("ingredients");
            for (int i = 0; i < ingredients.length(); i++) {
                JSONObject ing = ingredients.getJSONObject(i);
                if (!flaggedGroups.contains(ing.getString("category"))) continue;
                if ("avoid".equals(ing.optString("risk", ""))) {
                    avoidChemicals.add(ing.getString("name").toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return avoidChemicals;
    }

    private static boolean containsAvoidedIngredient(JSONObject product, Set<String> avoidChemicals) {
        if (avoidChemicals.isEmpty()) return false;
        try {
            if (product.has("detailedIngredients")) {
                JSONArray detailed = product.getJSONArray("detailedIngredients");
                for (int i = 0; i < detailed.length(); i++) {
                    String ing = detailed.getString(i).toLowerCase(Locale.ROOT);
                    for (String chem : avoidChemicals) {
                        if (ing.contains(chem)) return true;
                    }
                }
            }
            String allergyInfo = product.optString("allergyInformation", "").toLowerCase(Locale.ROOT);
            for (String chem : avoidChemicals) {
                if (allergyInfo.contains(chem)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
