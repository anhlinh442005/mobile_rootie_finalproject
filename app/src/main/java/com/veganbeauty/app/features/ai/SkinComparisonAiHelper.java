package com.veganbeauty.app.features.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.SkinProfileMetricsHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * So sánh trước–sau bằng Gemini Flash (free tier) trên đúng 2 snapshot da của khách.
 * Lỗi / hết quota / chưa cấu hình key → trả null để UI dùng phân tích diff local.
 */
public final class SkinComparisonAiHelper {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private SkinComparisonAiHelper() {
    }

    @Nullable
    public static String analyzeWithGemini(
            @Nullable String apiKey,
            @NonNull SkinProfileMetricsHelper.Snapshot older,
            @NonNull SkinProfileMetricsHelper.Snapshot newer) {
        if (!SkinAiAssistantHelper.isGeminiConfigured(apiKey)) {
            return null;
        }
        try {
            String prompt = buildPrompt(older, newer);
            String reply = callGemini(apiKey.trim(), prompt);
            if (reply == null || reply.trim().isEmpty()) {
                return null;
            }
            String cleaned = SkinAiTextHelper.sanitize(reply);
            if (cleaned.length() > 1200) {
                cleaned = cleaned.substring(0, 1197) + "...";
            }
            return cleaned;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    private static String buildPrompt(
            @NonNull SkinProfileMetricsHelper.Snapshot older,
            @NonNull SkinProfileMetricsHelper.Snapshot newer) {
        return "Bạn là Rootie AI — chuyên gia da thuần chay. So sánh 2 lần đo da CỦA CÙNG MỘT KHÁCH.\n"
                + "CHỈ dựa vào số liệu dưới đây, không bịa thêm chỉ số.\n\n"
                + "LẦN CŨ (" + label(older) + "):\n"
                + "- Loại da: " + safe(older.skinType) + "\n"
                + "- Cấp ẩm: " + older.hydration + "%\n"
                + "- Bã nhờn: " + older.sebum + "%\n"
                + "- Nhạy cảm: " + older.sensitivity + "%\n"
                + "- Đàn hồi: " + older.elasticity + "%\n\n"
                + "LẦN MỚI (" + label(newer) + "):\n"
                + "- Loại da: " + safe(newer.skinType) + "\n"
                + "- Cấp ẩm: " + newer.hydration + "%\n"
                + "- Bã nhờn: " + newer.sebum + "%\n"
                + "- Nhạy cảm: " + newer.sensitivity + "%\n"
                + "- Đàn hồi: " + newer.elasticity + "%\n\n"
                + "Quy ước đánh giá:\n"
                + "- Cấp ẩm / đàn hồi: tăng = cải thiện, giảm = kém đi\n"
                + "- Bã nhờn / nhạy cảm: giảm = cải thiện, tăng = kém đi\n\n"
                + "Viết tiếng Việt, ngắn gọn (4–7 bullet), gồm:\n"
                + "1) Tóm tắt thay đổi nổi bật (nhắc đúng số % trước → sau)\n"
                + "2) Điểm cải thiện / điểm cần chú ý\n"
                + "3) 1–2 gợi ý chăm da thuần chay phù hợp xu hướng chỉ số\n"
                + "Không markdown nặng, không bảng, không nói bạn là model.";
    }

    @Nullable
    private static String callGemini(@NonNull String apiKey, @NonNull String prompt) throws Exception {
        URL url = new URL(GEMINI_URL + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(12000);
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
                "Bạn là Rootie AI. Chỉ nhận xét dựa trên số liệu da được cung cấp. Tiếng Việt, rõ ràng, trung thực.")));
        requestJson.put("systemInstruction", systemInstruction);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 700);
        requestJson.put("generationConfig", generationConfig);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return null;
        }

        Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        connection.disconnect();

        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return null;
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) return null;
        JSONArray outParts = content.optJSONArray("parts");
        if (outParts == null || outParts.length() == 0) return null;
        return outParts.getJSONObject(0).optString("text", "").trim();
    }

    @NonNull
    private static String label(@NonNull SkinProfileMetricsHelper.Snapshot snap) {
        return snap.dateLabel != null && !snap.dateLabel.isEmpty() ? snap.dateLabel : "không rõ ngày";
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "chưa ghi nhận" : value.trim();
    }
}
