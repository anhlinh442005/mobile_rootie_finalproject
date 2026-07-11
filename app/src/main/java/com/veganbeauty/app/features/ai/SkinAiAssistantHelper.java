package com.veganbeauty.app.features.ai;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.utils.RewardPointsHelper;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.features.ai.RootieChatAdapter.RootieChatItem;
import com.veganbeauty.app.features.weather.SkinWeatherProductMatcher;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.features.weather.SkinWeatherSnapshotManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Gom context hồ sơ da + thời tiết và gọi Gemini cho Skin AI Chat. */
public final class SkinAiAssistantHelper {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private SkinAiAssistantHelper() {
    }

    public static final class WeatherContext {
        public final double temp;
        public final int humidity;
        public final double uv;
        public final double pm25;
        public final boolean hasPm25;
        public final String city;
        public final boolean available;

        WeatherContext(double temp, int humidity, double uv, double pm25,
                       boolean hasPm25, String city, boolean available) {
            this.temp = temp;
            this.humidity = humidity;
            this.uv = uv;
            this.pm25 = pm25;
            this.hasPm25 = hasPm25;
            this.city = city != null ? city : "";
            this.available = available;
        }
    }

    public static boolean isGeminiConfigured(String apiKey) {
        return apiKey != null
                && !apiKey.trim().isEmpty()
                && !"YOUR_GEMINI_API_KEY_HERE".equals(apiKey.trim());
    }

    public static WeatherContext loadWeatherContext(Context context) {
        SkinWeatherSnapshotManager.Snapshot snapshot = SkinWeatherSnapshotManager.loadLocal(context);
        if (snapshot != null && snapshot.weatherSuccess) {
            return new WeatherContext(
                    snapshot.temp,
                    snapshot.humidity,
                    snapshot.uv,
                    snapshot.pm25,
                    snapshot.hasPm25,
                    snapshot.city,
                    true
            );
        }
        return new WeatherContext(0, 0, 0, -1, false, "", false);
    }

    public static SkinWeatherProfileHelper.TodaySkinMetrics computeTodayMetrics(
            SkinWeatherProfileHelper.UserSkinProfile profile, WeatherContext weather) {
        if (!weather.available) {
            return SkinWeatherProfileHelper.metricsFromProfile(profile);
        }
        return SkinWeatherProfileHelper.computeTodayMetrics(
                profile,
                weather.temp,
                weather.humidity,
                weather.uv,
                (int) Math.round(weather.pm25),
                weather.hasPm25
        );
    }

    public static RootieChatItem.DiagnosticData buildDiagnostic(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            WeatherContext weather) {

        SkinWeatherProfileHelper.TodaySkinMetrics metrics = computeTodayMetrics(profile, weather);

        List<String> productIds = new ArrayList<>();
        List<String> phases = new ArrayList<>();
        List<String> subcats = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        if (weather.available) {
            Map<String, SkinWeatherProductMatcher.ProductMatch> matches =
                    SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(
                            context, weather.temp, weather.humidity, profile.skinType, profile.flaggedGroups);
            appendProductMatches(matches, productIds, phases, subcats, reasons, profile.skinType);
        } else if (profile.hasSavedProfile) {
            Map<String, SkinWeatherProductMatcher.ProductMatch> matches =
                    SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(
                            context, 30, 65, profile.skinType, profile.flaggedGroups);
            appendProductMatches(matches, productIds, phases, subcats, reasons, profile.skinType);
        }

        if (productIds.isEmpty()) {
            // Không hardcode SP — UI sẽ ẩn block sản phẩm khi không match catalog
        }

        String barrier;
        if (metrics.sensitivityPercent >= 65) {
            barrier = "Cần phục hồi gấp";
        } else if (metrics.sensitivityPercent >= 40) {
            barrier = "Cần phục hồi";
        } else {
            barrier = "Ổn định";
        }

        String sensitivityLabel = metrics.sensitivityPercent + "%"
                + (metrics.sensitivityPercent >= 60 ? " (cao)" :
                metrics.sensitivityPercent >= 35 ? " (trung bình)" : " (thấp)");

        String detail;
        if (profile.hasSavedProfile) {
            detail = "Theo hồ sơ quiz: cấp ẩm " + profile.hydration + "%, bã nhờn "
                    + profile.sebum + "%, nhạy cảm " + profile.sensitivity + "%.";
            if (!profile.recommendation.isEmpty()) {
                detail += " " + SkinAiTextHelper.sanitize(profile.recommendation);
            }
        } else {
            detail = "Bạn chưa lưu hồ sơ da — hãy làm bài test da để Rootie phân tích chính xác hơn.";
        }

        String why;
        if (weather.available) {
            why = String.format(Locale.US,
                    "Hôm nay tại %s: %.0f°C, độ ẩm %d%%, UV %.1f%s. Chỉ số ước tính: bã nhờn %d%%, cấp ẩm %d%%, nhạy cảm %d%%.",
                    weather.city.isEmpty() ? "khu vực của bạn" : weather.city,
                    weather.temp, weather.humidity, weather.uv,
                    weather.hasPm25 ? String.format(Locale.US, ", PM2.5 %.0f", weather.pm25) : "",
                    metrics.oilyPercent, metrics.hydrationPercent, metrics.sensitivityPercent);
        } else {
            why = "Mở mục Da × Thời tiết để Rootie cập nhật thời tiết và gợi ý routine theo ngày.";
        }

        return new RootieChatItem.DiagnosticData(
                "Tình trạng da: " + profile.skinType,
                detail,
                metrics.hydrationPercent + "%",
                sensitivityLabel,
                barrier,
                why,
                productIds,
                phases,
                subcats,
                reasons
        );
    }

    private static void appendProductMatches(
            Map<String, SkinWeatherProductMatcher.ProductMatch> matches,
            List<String> productIds,
            List<String> phases,
            List<String> subcats,
            List<String> reasons,
            String skinType) {
        if (matches == null || matches.isEmpty()) return;
        String[] keys = {"Cleanser", "Serum", "Moisturizer", "Sunscreen"};
        String[] phaseLabels = {"Làm sạch", "Tinh chất", "Dưỡng ẩm", "Chống nắng"};
        for (int i = 0; i < keys.length; i++) {
            SkinWeatherProductMatcher.ProductMatch match = matches.get(keys[i]);
            if (match == null) continue;
            productIds.add(match.getId());
            phases.add(phaseLabels[i]);
            subcats.add(match.getSubcategory() != null ? match.getSubcategory() : keys[i]);
            String note = match.getNotes();
            reasons.add(note != null && !note.isEmpty() ? note : "Phù hợp " + skinType);
        }
    }

    public static String buildChatPrompt(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather,
            String userMessage,
            List<RootieChatItem> recentMessages) {

        StringBuilder sb = new StringBuilder();
        sb.append(SkinAiUserContextBuilder.build(context));
        sb.append("\n---\n");

        if (weather.available) {
            SkinWeatherProfileHelper.TodaySkinMetrics today = computeTodayMetrics(profile, weather);
            sb.append("Chỉ số da ước tính hôm nay (đã tính thời tiết): bã nhờn ")
                    .append(today.oilyPercent).append("%, cấp ẩm ")
                    .append(today.hydrationPercent).append("%, nhạy cảm ")
                    .append(today.sensitivityPercent).append("%\n");
        }

        if (recentMessages != null && !recentMessages.isEmpty()) {
            sb.append("\n(Lịch sử chat gần đây được gửi riêng qua multi-turn API.)\n");
        }

        SkinAiIntentHelper.Intent intent = SkinAiIntentHelper.detect(userMessage);
        sb.append("\n[Ý ĐỊNH CÂU HỎI: ").append(intent.name()).append("]\n");
        sb.append(SkinAiIntentHelper.intentHint(intent)).append('\n');

        sb.append("\nCâu hỏi mới: ").append(userMessage).append('\n');
        sb.append("\nTrả lời dựa trên TOÀN BỘ ngữ cảnh trên. Tiếng Việt chuẩn, 2-5 câu.");
        return sb.toString();
    }

    public static String buildChatUserTurn(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather,
            String userMessage) {
        return buildChatPrompt(context, profile, metrics, weather, userMessage, null);
    }

    /**
     * Trả lời chat: ưu tiên nhận diện ý định → dữ liệu app (nhanh, chính xác).
     * Chỉ gọi Gemini khi không xác định được ý định.
     */
    public static String replyForChat(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather,
            String userMessage,
            String apiKey,
            List<RootieChatItem> recentMessages) {

        SkinAiAppDataSnapshot data = SkinAiAppDataLoader.load(context);
        SkinAiIntentHelper.Intent intent = SkinAiIntentHelper.detect(userMessage);
        if (SkinAiIntentHelper.usesAppData(intent)) {
            return SkinAiDataResponder.respond(data, intent, context, profile, metrics, weather, userMessage);
        }

        if (isGeminiConfigured(apiKey)) {
            try {
                StringBuilder prompt = new StringBuilder();
                prompt.append(SkinAiFieldCatalog.describe()).append('\n');
                prompt.append(SkinAiSnapshotFormatter.toCompact(data));
                prompt.append("\nCâu hỏi: ").append(userMessage);
                String gemini = requestChatReply(apiKey, null, prompt.toString());
                if (gemini != null && !gemini.trim().isEmpty()) {
                    return SkinAiTextHelper.sanitizeAiReply(gemini);
                }
            } catch (Exception ignored) {
            }
        }

        return SkinAiDataResponder.respond(data, SkinAiIntentHelper.Intent.UNKNOWN,
                context, profile, metrics, weather, userMessage);
    }

    /** @deprecated dùng {@link #buildChatPrompt(Context, ...)} */
    public static String buildChatPrompt(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather,
            String userMessage,
            List<RootieChatItem> recentMessages) {
        StringBuilder sb = new StringBuilder();
        if (profile.hasSavedProfile) {
            sb.append("- Loại da: ").append(profile.skinType).append('\n');
        }
        sb.append("\nCâu hỏi: ").append(userMessage);
        return sb.toString();
    }

    public static String requestChatReply(String apiKey, String prompt) throws Exception {
        return requestChatReply(apiKey, null, prompt);
    }

    public static String requestChatReply(
            String apiKey,
            List<RootieChatItem> recentMessages,
            String userTurnText) throws Exception {
        URL url = new URL(GEMINI_URL + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setDoOutput(true);

        JSONObject requestJson = new JSONObject();
        JSONArray contentsArray = new JSONArray();
        appendChatHistory(contentsArray, recentMessages);
        JSONArray partsArray = new JSONArray();
        partsArray.put(new JSONObject().put("text", userTurnText));
        contentsArray.put(new JSONObject()
                .put("role", "user")
                .put("parts", partsArray));
        requestJson.put("contents", contentsArray);

        JSONObject systemInstruction = new JSONObject();
        JSONArray systemParts = new JSONArray();
        systemParts.put(new JSONObject().put("text", SkinAiKnowledgeBase.getSystemInstruction()));
        systemInstruction.put("parts", systemParts);
        requestJson.put("systemInstruction", systemInstruction);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.42);
        generationConfig.put("maxOutputTokens", 750);
        requestJson.put("generationConfig", generationConfig);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return null;
        }

        InputStream inputStream = connection.getInputStream();
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        inputStream.close();
        connection.disconnect();

        JSONObject json = new JSONObject(response);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return null;

        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        if (parts.length() == 0) return null;

        return SkinAiTextHelper.sanitizeAiReply(parts.getJSONObject(0).getString("text").trim());
    }

    private static void appendChatHistory(JSONArray contentsArray, List<RootieChatItem> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) return;

        List<RootieChatItem> textOnly = new ArrayList<>();
        for (RootieChatItem item : recentMessages) {
            if (item.getType() != RootieChatItem.ItemType.TEXT) continue;
            String text = item.getMessageText();
            if (text == null || text.trim().isEmpty()) continue;
            if ("Đang phân tích câu hỏi của bạn...".equals(text.trim())) continue;
            textOnly.add(item);
        }

        int start = Math.max(0, textOnly.size() - 8);
        for (int i = start; i < textOnly.size(); i++) {
            RootieChatItem item = textOnly.get(i);
            String role = item.getSender() == RootieChatItem.Sender.USER ? "user" : "model";
            try {
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", item.getMessageText().trim()));
                contentsArray.put(new JSONObject().put("role", role).put("parts", parts));
            } catch (JSONException ignored) {
            }
        }
    }

    public static String buildRuleBasedReply(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            SkinAiAssistantHelper.WeatherContext weather,
            String userMessage) {

        SkinAiAppDataSnapshot data = SkinAiAppDataLoader.load(context);
        SkinAiIntentHelper.Intent intent = SkinAiIntentHelper.detect(userMessage);
        return SkinAiDataResponder.respond(data, intent, context, profile, metrics, weather, userMessage);
    }

    /** Có nên kèm card phác đồ chẩn đoán sau câu trả lời text. */
    public static boolean shouldAttachDiagnosticCard(String userMessage) {
        return SkinAiIntentHelper.detect(userMessage) == SkinAiIntentHelper.Intent.DIAGNOSTIC;
    }

    private static String buildGreetingReply(Context context, SkinWeatherProfileHelper.UserSkinProfile profile) {
        String name = ProfileSession.getFullName(context);
        String who = name.isEmpty() ? "bạn" : name;
        if (!profile.hasSavedProfile) {
            return "Chào " + who + "! Mình là Rootie AI. Bạn chưa làm bài test da — làm quiz để mình tư vấn đúng loại da và thành phần cần tránh nhé.";
        }
        return "Chào " + who + "! Hồ sơ da: " + profile.skinType
                + " (ẩm " + profile.hydration + "%, nhạy cảm " + profile.sensitivity + "%). "
                + "Hỏi mình: xu, voucher, đơn hàng, routine, sản phẩm, thành phần tránh, phác đồ da, cửa hàng, thông báo.";
    }

    private static String buildProfileReply(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics) {
        if (!profile.hasSavedProfile) {
            return "Bạn chưa làm bài test da. Vào My Skin → Bài test da để Rootie phân tích loại da và chỉ số cho bạn nhé.";
        }
        return profile.skinType + " — cấp ẩm " + profile.hydration + "%, bã nhờn "
                + profile.sebum + "%, nhạy cảm " + profile.sensitivity + "%, đàn hồi "
                + profile.elasticity + "%. Ước tính hôm nay: ẩm " + metrics.hydrationPercent
                + "%, dầu " + metrics.oilyPercent + "%, nhạy cảm " + metrics.sensitivityPercent + "%.";
    }

    private static String buildIngredientReply(SkinWeatherProfileHelper.UserSkinProfile profile) {
        if (!profile.hasSavedProfile) {
            return "Làm quiz da trước — Rootie sẽ liệt kê thành phần nên tránh theo kết quả của bạn.";
        }
        String avoid = profile.flaggedGroups.isEmpty()
                ? "cồn, hương liệu, sulfate (nếu da nhạy cảm)"
                : String.join(", ", profile.flaggedGroups);
        return "Với " + profile.skinType + ", nên tránh/thận trọng: " + avoid
                + ". Ưu tiên sản phẩm thuần chay, dịu, không hương liệu nếu nhạy cảm "
                + profile.sensitivity + "%.";
    }

    private static String buildGiftReply(Context context) {
        try {
            String userId = ProfileSessionHelper.getEffectiveUserId(context);
            if (userId == null) userId = "";
            List<com.veganbeauty.app.data.local.entities.UserGiftEntity> gifts =
                    RootieDatabase.getDatabase(context).userGiftDao().getAllUserGiftsSync(userId);
            if (gifts == null || gifts.isEmpty()) {
                return "Bạn chưa đổi quà nào. Vào Tài khoản → Đổi quà — dùng "
                        + formatPoints(RewardPointsHelper.getTotalPoints(context))
                        + " xu hiện có để đổi nhé.";
            }
            StringBuilder sb = new StringBuilder("Quà của bạn:\n");
            int limit = Math.min(4, gifts.size());
            for (int i = 0; i < limit; i++) {
                com.veganbeauty.app.data.local.entities.UserGiftEntity g = gifts.get(i);
                sb.append("• ").append(g.getTitle())
                        .append(" (").append(g.getStatus()).append(")\n");
            }
            sb.append("Xem thêm tại Tài khoản → Đổi quà / Voucher.");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Xem quà tại Tài khoản → Đổi quà.";
        }
    }

    private static String buildStoreReply(Context context) {
        try {
            List<com.veganbeauty.app.data.local.entities.StoreEntity> stores =
                    new LocalJsonReader(context).getAllStores();
            if (stores == null || stores.isEmpty()) {
                return "Xem hệ thống cửa hàng tại Cửa hàng → Hệ thống cửa hàng.";
            }
            StringBuilder sb = new StringBuilder("Cửa hàng Rootie:\n");
            int limit = Math.min(3, stores.size());
            for (int i = 0; i < limit; i++) {
                com.veganbeauty.app.data.local.entities.StoreEntity s = stores.get(i);
                sb.append("• ").append(s.getStoreName())
                        .append(" — ").append(s.getAddress())
                        .append(" (").append(s.getOpenHours()).append(")\n");
            }
            sb.append("Đặt lịch soi da: My Skin → Đặt lịch.");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Xem cửa hàng tại Cửa hàng → Hệ thống cửa hàng.";
        }
    }

    private static String buildNotificationReply(Context context) {
        try {
            List<com.veganbeauty.app.data.local.entities.NotificationItem> items =
                    new LocalJsonReader(context).getAllNotifications();
            if (items == null || items.isEmpty()) {
                return "Chưa có thông báo. Bấm chuông góc phải để xem khi có tin mới.";
            }
            int unread = 0;
            for (com.veganbeauty.app.data.local.entities.NotificationItem item : items) {
                if (!item.isRead()) unread++;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Bạn có ").append(unread).append(" thông báo chưa đọc.\n");
            int limit = Math.min(3, items.size());
            for (int i = 0; i < limit; i++) {
                sb.append("• ").append(items.get(i).getTitle())
                        .append(items.get(i).isRead() ? "" : " (mới)").append('\n');
            }
            sb.append("Xem đầy đủ: chuông thông báo → Tài khoản.");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Xem thông báo tại biểu tượng chuông góc phải màn hình.";
        }
    }

    private static String buildHowToReply(Context context, String userMessage) {
        String lower = userMessage.toLowerCase(Locale.getDefault());
        if (lower.contains("quiz") || lower.contains("test da") || lower.contains("bài test")) {
            return "Làm bài test da: My Skin → Bài test da → làm quiz → xem loại da và routine gợi ý AI.";
        }
        if (lower.contains("đổi quà") || lower.contains("xu")) {
            return "Đổi quà: Tài khoản → Đổi quà. Số dư: "
                    + formatPoints(RewardPointsHelper.getTotalPoints(context)) + " xu.";
        }
        if (lower.contains("voucher")) {
            return "Xem voucher: Tài khoản → Voucher hoặc Cửa hàng → Voucher khi thanh toán.";
        }
        if (lower.contains("routine") || lower.contains("nhắc")) {
            return "Thiết lập routine: sau quiz bấm Áp dụng routine, hoặc My Skin → Routine → cài nhắc sáng/tối.";
        }
        if (lower.contains("thời tiết")) {
            return "Cập nhật thời tiết: mở mục Da × Thời tiết trên thanh điều hướng.";
        }
        return SkinAiTrainingExamples.getAppNavigationMap().replace('\n', ' ').trim();
    }

    private static String buildVoucherReply(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            String userId = ProfileSessionHelper.getEffectiveUserId(context);
            if (userId == null) userId = "";
            List<com.veganbeauty.app.data.local.entities.UserGiftEntity> gifts =
                    RootieDatabase.getDatabase(context).userGiftDao().getAllUserGiftsSync(userId);
            List<com.veganbeauty.app.data.local.entities.VoucherEntity> system =
                    RootieDatabase.getDatabase(context).voucherDao().getActiveVouchers();
            if (system == null || system.isEmpty()) {
                system = new LocalJsonReader(context).getVouchers();
            }

            int activeUserCount = 0;
            if (gifts != null) {
                for (com.veganbeauty.app.data.local.entities.UserGiftEntity gift : gifts) {
                    if (!"voucher_discount".equals(gift.getGiftType())
                            && !"voucher_freeship".equals(gift.getGiftType())) {
                        continue;
                    }
                    String status = gift.getStatus();
                    if (status != null && status.toLowerCase(Locale.getDefault()).contains("hết hạn")) {
                        continue;
                    }
                    if (activeUserCount == 0) {
                        sb.append("Voucher trong ví của bạn:\n");
                    }
                    activeUserCount++;
                    sb.append("• ").append(gift.getTitle())
                            .append(" — mã ").append(gift.getCode())
                            .append(" (").append(gift.getStatus())
                            .append(", HSD ").append(gift.getExpiryDate()).append(")\n");
                    if (activeUserCount >= 5) break;
                }
            }

            if (activeUserCount == 0) {
                sb.append("Bạn chưa có voucher còn hạn trong ví. ");
            }

            if (system != null && !system.isEmpty()) {
                sb.append("Khuyến mãi hệ thống đang có: ");
                int limit = Math.min(4, system.size());
                for (int i = 0; i < limit; i++) {
                    com.veganbeauty.app.data.local.entities.VoucherEntity v = system.get(i);
                    if (i > 0) sb.append("; ");
                    sb.append(v.getTitle()).append(" (mã ").append(v.getCode()).append(")");
                }
                sb.append(". ");
            }

            sb.append("Xem chi tiết tại Tài khoản → Voucher hoặc Cửa hàng → Voucher.");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Xem voucher tại Tài khoản → Voucher hoặc Cửa hàng → Voucher.";
        }
    }

    private static String buildHelpfulFallback() {
        return "Mình chưa chắc bạn đang hỏi về chủ đề gì. Thử hỏi cụ thể: "
                + "xu hôm nay, voucher đang có, loại da của tôi, thành phần nên tránh, "
                + "routine sáng/tối, sản phẩm phù hợp, đơn hàng, giỏ hàng, quà đã đổi, "
                + "cửa hàng Rootie, thông báo, thời tiết & da, phác đồ da, lịch soi da.";
    }

    private static String buildCoinsReply(Context context) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();

            int todayEarned = RewardPointsHelper.getPointsEarnedSince(context, startOfDay);
            int total = RewardPointsHelper.getTotalPoints(context);

            if (todayEarned <= 0) {
                return "Hôm nay bạn chưa nhận thêm xu nào. Tổng số dư hiện tại: "
                        + formatPoints(total) + " xu. "
                        + "Làm quiz da, check-in hoặc hoàn thành routine để tích thêm xu nhé!";
            }

            List<com.veganbeauty.app.data.local.entities.RewardPointEntity> todayList =
                    RewardPointsHelper.getHistorySince(context, startOfDay);

            StringBuilder sb = new StringBuilder();
            sb.append("Hôm nay bạn đã nhận ").append(formatPoints(todayEarned)).append(" xu. Tổng số dư: ")
                    .append(formatPoints(total)).append(" xu.\n");
            int limit = Math.min(3, todayList.size());
            for (int i = 0; i < limit; i++) {
                com.veganbeauty.app.data.local.entities.RewardPointEntity item = todayList.get(i);
                sb.append("• +").append(item.getPoints()).append(" xu — ")
                        .append(SkinAiTextHelper.sanitize(item.getReason())).append('\n');
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Tổng xu hiện tại của bạn: "
                    + formatPoints(RewardPointsHelper.getTotalPoints(context)) + " xu.";
        }
    }

    private static String buildDiagnosticTextReply(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics) {
        if (!profile.hasSavedProfile) {
            return "Bạn chưa có hồ sơ da. Làm bài test da trước — sau đó mình sẽ gửi phác đồ chi tiết.";
        }
        return profile.skinType + " — cấp ẩm " + metrics.hydrationPercent + "%, bã nhờn "
                + metrics.oilyPercent + "%, nhạy cảm " + metrics.sensitivityPercent + "%. "
                + "Mình gửi phác đồ phân tích chi tiết ngay bên dưới nhé.";
    }

    private static String buildWeatherRoutineReply(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather) {
        if (!profile.hasSavedProfile) {
            return "Làm bài test da trước để mình tư vấn routine và thời tiết chính xác cho bạn nhé.";
        }
        if (weather.available) {
            SkinWeatherProfileHelper.PersonalizedAdvice advice = SkinWeatherProfileHelper.buildRuleBasedAdvice(
                    profile, metrics,
                    weather.temp, weather.humidity, weather.uv,
                    (int) Math.round(weather.pm25), weather.hasPm25,
                    weather.city.isEmpty() ? "khu vực của bạn" : weather.city);
            return SkinAiTextHelper.sanitize(advice.headline) + " "
                    + SkinAiTextHelper.sanitize(advice.subtext);
        }
        return "Mình chưa có dữ liệu thời tiết mới. Mở mục Da × Thời tiết rồi hỏi lại — mình sẽ tư vấn theo "
                + profile.skinType + " và UV/độ ẩm thực tế.";
    }

    private static String buildProductReply(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            WeatherContext weather) {
        if (!profile.hasSavedProfile) {
            return "Làm quiz da trước để mình gợi ý sản phẩm phù hợp nhé.";
        }
        if (weather.available) {
            Map<String, SkinWeatherProductMatcher.ProductMatch> matches =
                    SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(
                            context, weather.temp, weather.humidity, profile.skinType, profile.flaggedGroups);
            if (!matches.isEmpty()) {
                StringBuilder sb = new StringBuilder("Với " + profile.skinType + " và thời tiết hôm nay, Rootie gợi ý: ");
                boolean first = true;
                for (SkinWeatherProductMatcher.ProductMatch match : matches.values()) {
                    if (!first) sb.append("; ");
                    sb.append(match.getName());
                    first = false;
                }
                sb.append(". Tránh: ")
                        .append(profile.flaggedGroups.isEmpty() ? "cồn, hương liệu" : String.join(", ", profile.flaggedGroups))
                        .append('.');
                return sb.toString();
            }
        }
        return "Với " + profile.skinType + " (ẩm " + metrics.hydrationPercent + "%, nhạy cảm "
                + metrics.sensitivityPercent + "%), ưu tiên làm sạch dịu, cấp ẩm và chống nắng.";
    }

    private static String buildBookingReply(Context context) {
        try {
            String userId = ProfileSession.getUserId(context);
            String email = ProfileSession.getEmail(context);
            String key = email != null && !email.isEmpty() ? email : userId;
            List<com.veganbeauty.app.data.local.entities.BookingHistoryEntity> bookings =
                    new LocalJsonReader(context).getUserBookingHistory(key);
            if (bookings == null || bookings.isEmpty()) {
                return "Bạn chưa có lịch đặt soi da. Vào My Skin → Đặt lịch để book spa Rootie nhé.";
            }
            com.veganbeauty.app.data.local.entities.BookingHistoryEntity next = bookings.get(0);
            return "Lịch gần nhất: " + next.getServiceName() + " — " + next.getStatus()
                    + " tại " + next.getStoreName() + " (" + next.getDateDisplay() + "). "
                    + "Xem thêm tại My Skin → Lịch sử đặt lịch.";
        } catch (Exception e) {
            return "Xem lịch soi da tại My Skin → Lịch sử đặt lịch.";
        }
    }

    private static String buildOrderCartReply(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            List<com.veganbeauty.app.data.local.entities.CartItemEntity> cartItems =
                    RootieDatabase.getDatabase(context).cartDao().getCartItemsSync();
            if (cartItems != null && !cartItems.isEmpty()) {
                sb.append("Giỏ hàng: ");
                for (int i = 0; i < Math.min(3, cartItems.size()); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(cartItems.get(i).getName());
                }
                if (cartItems.size() > 3) sb.append("... (+").append(cartItems.size() - 3).append(" SP)");
                sb.append(". ");
            } else {
                sb.append("Giỏ hàng đang trống. ");
            }

            String userId = ProfileSession.getUserId(context);
            String phone = ProfileSession.getPhone(context);
            List<com.veganbeauty.app.data.local.entities.OrderEntity> orders =
                    RootieDatabase.getDatabase(context).orderDao()
                            .getOrdersForBuyerIdentitySync(userId, phone != null ? phone : "");
            if (orders != null && !orders.isEmpty()) {
                com.veganbeauty.app.data.local.entities.OrderEntity latest = orders.get(0);
                sb.append("Đơn gần nhất: ").append(latest.getStatus())
                        .append(" — ").append(String.format(Locale.getDefault(), "%,dđ", latest.getTotalAmount()).replace(',', '.'))
                        .append(" (").append(latest.getOrderDate()).append("). ");
            }
            sb.append("Xem chi tiết tại Cửa hàng → Đơn hàng của tôi.");
            return sb.toString();
        } catch (Exception e) {
            return "Xem đơn hàng tại Cửa hàng → Đơn hàng của tôi.";
        }
    }

    private static String buildRoutineReply(Context context, SkinWeatherProfileHelper.UserSkinProfile profile) {
        try {
            int hydration = profile.hasSavedProfile ? profile.hydration : 50;
            int sebum = profile.hasSavedProfile ? profile.sebum : 50;
            int sensitivity = profile.hasSavedProfile ? profile.sensitivity : 50;
            int elasticity = profile.hasSavedProfile ? profile.elasticity : 75;
            String skinType = profile.hasSavedProfile ? profile.skinType : "Da thường";

            SkinAiRoutineRecommender.RoutinePlan plan = SkinAiRoutineRecommender.recommend(
                    context, skinType, hydration, sebum, sensitivity, elasticity,
                    profile.flaggedGroups, "");

            return "Routine sáng: " + SkinAiRoutineRecommender.formatRoutineForChat(plan, true)
                    + "\nRoutine tối: " + SkinAiRoutineRecommender.formatRoutineForChat(plan, false)
                    + "\n" + plan.assessment;
        } catch (Exception e) {
            Set<String> morning = ProfileSession.getMorningSteps(context);
            Set<String> evening = ProfileSession.getEveningSteps(context);
            StringBuilder sb = new StringBuilder();
            if (!profile.hasSavedProfile) {
                sb.append("Làm quiz da để routine phù hợp hơn. ");
            }
            sb.append("Routine sáng: ").append(formatRoutineSteps(morning))
                    .append(". Routine tối: ").append(formatRoutineSteps(evening)).append('.');
            return sb.toString();
        }
    }

    private static String formatRoutineSteps(Set<String> steps) {
        StringBuilder line = new StringBuilder();
        for (com.veganbeauty.app.features.routine.SkincareStep step :
                com.veganbeauty.app.features.routine.SkincareStep.parseList(steps)) {
            if (!step.isChecked()) continue;
            if (line.length() > 0) line.append(" → ");
            line.append(step.getName());
        }
        return line.length() > 0 ? line.toString() : "chưa bật bước nào";
    }

    private static String buildGeneralSkinReply(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics) {
        return "Với " + profile.skinType + ", ưu tiên "
                + (metrics.hydrationPercent < 45 ? "cấp ẩm và khóa ẩm" :
                metrics.oilyPercent >= 65 ? "làm sạch kiềm dầu nhẹ và chống nắng mỏng" :
                        "duy trì routine cân bằng")
                + ". Nhạy cảm ~" + metrics.sensitivityPercent + "%. "
                + "Bạn có thể hỏi cụ thể: xu hôm nay, voucher, thời tiết, sản phẩm, phác đồ da, lịch soi da.";
    }

    private static String formatPoints(int points) {
        return String.format(Locale.getDefault(), "%,d", points).replace(',', '.');
    }

    public static String buildAdviceCacheKey(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            double temp, int humidity, double uv, int pm25, String city) {
        return profile.skinType + "|" + profile.hydration + "|" + profile.sebum + "|" + profile.sensitivity
                + "|" + profile.elasticity + "|" + profile.flaggedGroups.hashCode()
                + "|" + temp + "|" + humidity + "|" + uv + "|" + pm25 + "|" + city;
    }
}
