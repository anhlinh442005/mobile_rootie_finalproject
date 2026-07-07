package com.veganbeauty.app.features.ai;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.utils.RewardPointsHelper;
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
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
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
        generationConfig.put("temperature", 0.55);
        generationConfig.put("maxOutputTokens", 600);
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
            WeatherContext weather,
            String userMessage) {

        String lower = userMessage.toLowerCase(Locale.getDefault()).trim();

        if (isGreeting(lower)) {
            return buildGreetingReply(context, profile);
        }

        if (isCoinsQuestion(lower)) {
            return buildCoinsReply(context);
        }

        if (isDiagnosticRequest(lower)) {
            return buildDiagnosticTextReply(profile, metrics);
        }

        if (isBookingQuestion(lower)) {
            return buildBookingReply(context);
        }

        if (isOrderOrCartQuestion(lower)) {
            return buildOrderCartReply(context);
        }

        if (isRoutineQuestion(lower)) {
            return buildRoutineReply(context, profile);
        }

        if (isProductQuestion(lower)) {
            return buildProductReply(context, profile, metrics, weather);
        }

        if (isWeatherOrRoutineQuestion(lower)) {
            return buildWeatherRoutineReply(profile, metrics, weather);
        }

        if (!profile.hasSavedProfile) {
            return "Bạn chưa có hồ sơ da. Hãy làm bài test da trong app trước — Rootie sẽ tư vấn chính xác theo loại da và thành phần cần tránh của bạn.";
        }

        return buildGeneralSkinReply(profile, metrics);
    }

    /** Có nên kèm card phác đồ chẩn đoán sau câu trả lời text. */
    public static boolean shouldAttachDiagnosticCard(String userMessage) {
        return isDiagnosticRequest(userMessage.toLowerCase(Locale.getDefault()).trim());
    }

    private static String buildGreetingReply(Context context, SkinWeatherProfileHelper.UserSkinProfile profile) {
        String name = ProfileSession.getFullName(context);
        String who = name.isEmpty() ? "bạn" : name;
        if (!profile.hasSavedProfile) {
            return "Chào " + who + "! Mình là Rootie AI. Bạn chưa làm bài test da — làm quiz để mình tư vấn đúng loại da và thành phần cần tránh nhé.";
        }
        return "Chào " + who + "! Hồ sơ da: " + profile.skinType
                + " (ẩm " + profile.hydration + "%, nhạy cảm " + profile.sensitivity + "%). "
                + "Bạn có thể hỏi: xu hôm nay, thời tiết & da, sản phẩm phù hợp, phác đồ da, lịch soi da.";
    }

    private static String buildCoinsReply(Context context) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();

            int todayEarned = RootieDatabase.getDatabase(context)
                    .rewardPointDao()
                    .getPointsEarnedSince(startOfDay);
            int total = RewardPointsHelper.getTotalPoints(context);

            if (todayEarned <= 0) {
                return "Hôm nay bạn chưa nhận thêm xu nào. Tổng số dư hiện tại: "
                        + formatPoints(total) + " xu. "
                        + "Làm quiz da, check-in hoặc hoàn thành routine để tích thêm xu nhé!";
            }

            List<com.veganbeauty.app.data.local.entities.RewardPointEntity> todayList =
                    RootieDatabase.getDatabase(context).rewardPointDao().getHistorySince(startOfDay);

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

    private static String formatRoutineSteps(Set<String> steps) {
        StringBuilder line = new StringBuilder();
        for (String step : steps) {
            String[] parts = step.split(":", 4);
            if (parts.length < 3) continue;
            boolean enabled = parts.length < 4 || "true".equalsIgnoreCase(parts[3]);
            if (!enabled) continue;
            if (line.length() > 0) line.append(" → ");
            line.append(parts[2]);
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
                + "Bạn có thể hỏi cụ thể: xu hôm nay, thời tiết, sản phẩm, phác đồ da, lịch soi da.";
    }

    private static String formatPoints(int points) {
        return String.format(Locale.getDefault(), "%,d", points).replace(',', '.');
    }

    private static boolean isCoinsQuestion(String lower) {
        return lower.contains("xu") || lower.contains("coin") || lower.contains("điểm thưởng")
                || lower.contains("tích điểm") || lower.contains("thưởng")
                || (lower.contains("cộng") && lower.contains("xu"));
    }

    private static boolean isDiagnosticRequest(String lower) {
        return lower.contains("phác đồ") || lower.contains("chẩn đoán")
                || lower.contains("phân tích da") || lower.contains("bản phân tích");
    }

    private static boolean isWeatherOrRoutineQuestion(String lower) {
        if (isCoinsQuestion(lower) || isDiagnosticRequest(lower)) return false;
        return lower.contains("thời tiết") || lower.contains("routine")
                || lower.contains("chu trình") || lower.contains(" uv")
                || lower.startsWith("uv") || lower.contains("nắng")
                || lower.contains("độ ẩm không khí")
                || (lower.contains("hôm nay") && (lower.contains("da") || lower.contains("dưỡng")
                || lower.contains("thời tiết") || lower.contains("routine") || lower.contains("nắng")))
                || lower.contains("thời tiết & da") || lower.contains("thời tiết và da");
    }

    private static boolean isProductQuestion(String lower) {
        return lower.contains("sản phẩm") || lower.contains("phù hợp") || lower.contains("mua gì")
                || lower.contains("dùng gì") || lower.contains("kem") && lower.contains("nào");
    }

    private static boolean isRoutineQuestion(String lower) {
        return lower.contains("routine") || lower.contains("chu trình")
                || (lower.contains("sáng") && (lower.contains("bước") || lower.contains("làm gì")))
                || (lower.contains("tối") && (lower.contains("bước") || lower.contains("làm gì")));
    }

    private static boolean isOrderOrCartQuestion(String lower) {
        return lower.contains("đơn hàng") || lower.contains("giỏ hàng") || lower.contains("giỏ ");
    }

    private static boolean isBookingQuestion(String lower) {
        return lower.contains("đặt lịch") || lower.contains("booking")
                || lower.contains("lịch soi") || lower.contains("spa")
                || (lower.contains("lịch") && !lower.contains("lịch sử quiz"));
    }

    private static boolean isGreeting(String lower) {
        return lower.equals("hello") || lower.equals("hi") || lower.equals("chào")
                || lower.startsWith("chào ") || lower.contains("xin chào");
    }

    public static String buildAdviceCacheKey(
            SkinWeatherProfileHelper.UserSkinProfile profile,
            double temp, int humidity, double uv, int pm25, String city) {
        return profile.skinType + "|" + profile.hydration + "|" + profile.sebum + "|" + profile.sensitivity
                + "|" + profile.elasticity + "|" + profile.flaggedGroups.hashCode()
                + "|" + temp + "|" + humidity + "|" + uv + "|" + pm25 + "|" + city;
    }
}
