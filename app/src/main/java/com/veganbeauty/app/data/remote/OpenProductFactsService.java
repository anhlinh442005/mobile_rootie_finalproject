package com.veganbeauty.app.data.remote;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.entities.KeyIngredient;
import com.veganbeauty.app.data.local.entities.ProductEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tra cứu barcode công khai qua Open Beauty Facts (ưu tiên) rồi Open Food Facts.
 * Không cần API key. User-Agent bắt buộc theo policy của Open*Facts.
 */
public final class OpenProductFactsService {

    public static final String EXTERNAL_ID_PREFIX = "ext_";

    private static final String USER_AGENT = "ROOTIE - Android - 1.0 - https://github.com/rootie";
    private static final int TIMEOUT_MS = 12_000;

    private static final String[] ENDPOINTS = {
            "https://world.openbeautyfacts.org/api/v0/product/%s.json",
            "https://world.openfoodfacts.org/api/v0/product/%s.json"
    };

    private OpenProductFactsService() {
    }

    public static boolean isExternalProductId(@Nullable String productId) {
        return productId != null && productId.startsWith(EXTERNAL_ID_PREFIX);
    }

    @Nullable
    public static ProductEntity fetchByBarcode(@Nullable String barcode) {
        String code = barcode != null ? barcode.trim() : "";
        if (code.isEmpty()) return null;

        // QR đôi khi chứa URL — cố lấy đoạn số barcode ở cuối path
        code = extractBarcodeCandidate(code);
        if (code.isEmpty()) return null;

        for (String template : ENDPOINTS) {
            try {
                String url = String.format(Locale.US, template, code);
                JSONObject root = getJson(url);
                if (root == null) continue;
                if (root.optInt("status", 0) != 1) continue;
                JSONObject product = root.optJSONObject("product");
                if (product == null) continue;
                ProductEntity mapped = mapProduct(code, product, template.contains("beauty") ? "Open Beauty Facts" : "Open Food Facts");
                if (mapped != null && mapped.getName() != null && !mapped.getName().trim().isEmpty()) {
                    return mapped;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String extractBarcodeCandidate(String raw) {
        String trimmed = raw.trim();
        if (trimmed.matches("\\d{8,14}")) return trimmed;
        // URL dạng .../product/8938501234567 hoặc .../8938501234567
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < trimmed.length() - 1) {
            String tail = trimmed.substring(lastSlash + 1).split("[?#]")[0];
            if (tail.matches("\\d{8,14}")) return tail;
        }
        // Lấy chuỗi số dài nhất trong raw
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{8,14}").matcher(trimmed);
        String best = "";
        while (m.find()) {
            if (m.group().length() > best.length()) best = m.group();
        }
        return best;
    }

    @Nullable
    private static ProductEntity mapProduct(String barcode, JSONObject product, String source) {
        String name = firstNonEmpty(
                product.optString("product_name_vi"),
                product.optString("product_name_en"),
                product.optString("product_name"),
                product.optString("generic_name_vi"),
                product.optString("generic_name_en"),
                product.optString("generic_name")
        );
        if (name.isEmpty()) return null;

        String brand = firstNonEmpty(product.optString("brands"), product.optString("brand"));
        String category = firstNonEmpty(
                product.optString("categories"),
                joinTags(product.optJSONArray("categories_tags"))
        );
        if (category.length() > 80) {
            category = category.substring(0, 80) + "…";
        }

        String image = firstNonEmpty(
                product.optString("image_front_url"),
                product.optString("image_url"),
                product.optString("image_front_small_url"),
                product.optString("image_small_url")
        );

        List<String> album = new ArrayList<>();
        if (!image.isEmpty()) album.add(image);
        addIfValid(album, product.optString("image_ingredients_url"));
        addIfValid(album, product.optString("image_nutrition_url"));

        String ingredients = firstNonEmpty(
                product.optString("ingredients_text_vi"),
                product.optString("ingredients_text_en"),
                product.optString("ingredients_text")
        );

        List<String> detailedIngredients = splitIngredients(ingredients);

        String allergens = firstNonEmpty(
                product.optString("allergens_from_ingredients"),
                product.optString("allergens"),
                joinTags(product.optJSONArray("allergens_tags"))
        );

        String origin = firstNonEmpty(
                product.optString("origins"),
                product.optString("countries"),
                joinTags(product.optJSONArray("countries_tags"))
        );

        String quantity = product.optString("quantity", "");
        String packaging = product.optString("packaging", "");

        StringBuilder description = new StringBuilder();
        String generic = firstNonEmpty(
                product.optString("generic_name_vi"),
                product.optString("generic_name_en"),
                product.optString("generic_name")
        );
        if (!generic.isEmpty() && !generic.equalsIgnoreCase(name)) {
            description.append(generic);
        }
        if (!quantity.isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("Dung tích/khối lượng: ").append(quantity);
        }
        if (!packaging.isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("Bao bì: ").append(packaging);
        }
        if (description.length() == 0) {
            description.append("Thông tin tham khảo từ ").append(source).append(".");
        }

        String notes = "Nguồn: " + source + " • Chỉ tham khảo, chưa bán trên Rootie";

        List<String> idealFor = new ArrayList<>();
        String labels = product.optString("labels", "");
        if (!labels.isEmpty()) {
            for (String part : labels.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) idealFor.add(t);
            }
        }

        return new ProductEntity(
                EXTERNAL_ID_PREFIX + barcode,
                name,
                barcode,
                barcode,
                0L,
                null,
                category.isEmpty() ? "Tham khảo online" : category,
                brand.isEmpty() ? "—" : brand,
                0,
                description.toString(),
                image,
                "",
                origin,
                "",
                false,
                "",
                album,
                ingredients,
                allergens,
                Collections.<KeyIngredient>emptyList(),
                detailedIngredients,
                "Dữ liệu cộng đồng từ " + source + ". Có thể chưa đầy đủ hoặc chưa được xác minh.",
                image,
                idealFor,
                Collections.<String>emptyList(),
                "",
                "",
                "",
                notes,
                0f,
                0
        );
    }

    private static List<String> splitIngredients(String ingredients) {
        if (ingredients == null || ingredients.trim().isEmpty()) return Collections.emptyList();
        String[] parts = ingredients.split("[,;•]");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
            if (out.size() >= 40) break;
        }
        return out;
    }

    private static void addIfValid(List<String> album, String url) {
        if (url != null && !url.trim().isEmpty() && !album.contains(url.trim())) {
            album.add(url.trim());
        }
    }

    private static String joinTags(@Nullable JSONArray tags) {
        if (tags == null || tags.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(tags.length(), 5);
        for (int i = 0; i < limit; i++) {
            String tag = tags.optString(i, "");
            if (tag.startsWith("en:") || tag.startsWith("fr:") || tag.startsWith("vi:")) {
                tag = tag.substring(3).replace('-', ' ');
            }
            if (tag.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(tag);
        }
        return sb.toString();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty() && !"null".equalsIgnoreCase(t)) return t;
            }
        }
        return "";
    }

    @Nullable
    private static JSONObject getJson(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (stream == null) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            if (sb.length() == 0) return null;
            return new JSONObject(sb.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
