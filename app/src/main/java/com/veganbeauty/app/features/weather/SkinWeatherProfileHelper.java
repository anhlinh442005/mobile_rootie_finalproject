package com.veganbeauty.app.features.weather;

import android.content.Context;
import android.content.SharedPreferences;

import com.veganbeauty.app.data.local.ProfileSession;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Đọc hồ sơ da đã lưu (quiz / Firebase sync) và tính chỉ số + lời khuyên theo thời tiết. */
public final class SkinWeatherProfileHelper {

    private SkinWeatherProfileHelper() {
    }

    public static final class UserSkinProfile {
        public final String skinType;
        public final String recommendation;
        public final String skinAreas;
        public final int sensitivity;
        public final int hydration;
        public final int elasticity;
        public final int sebum;
        public final Set<String> flaggedGroups;
        public final boolean hasSavedProfile;

        UserSkinProfile(String skinType, String recommendation, String skinAreas,
                        int sensitivity, int hydration, int elasticity, int sebum,
                        Set<String> flaggedGroups, boolean hasSavedProfile) {
            this.skinType = skinType;
            this.recommendation = recommendation;
            this.skinAreas = skinAreas;
            this.sensitivity = sensitivity;
            this.hydration = hydration;
            this.elasticity = elasticity;
            this.sebum = sebum;
            this.flaggedGroups = flaggedGroups;
            this.hasSavedProfile = hasSavedProfile;
        }
    }

    public static final class TodaySkinMetrics {
        public final int oilyPercent;
        public final int hydrationPercent;
        public final int sensitivityPercent;

        TodaySkinMetrics(int oilyPercent, int hydrationPercent, int sensitivityPercent) {
            this.oilyPercent = oilyPercent;
            this.hydrationPercent = hydrationPercent;
            this.sensitivityPercent = sensitivityPercent;
        }
    }

    public static final class PersonalizedAdvice {
        public final String headline;
        public final String subtext;
        public final String insight;

        PersonalizedAdvice(String headline, String subtext, String insight) {
            this.headline = headline;
            this.subtext = subtext;
            this.insight = insight;
        }
    }

    public static UserSkinProfile load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        boolean hasSavedProfile = ProfileSession.getLastSkinTestTime(context) > 0;

        String skinType = ProfileSession.getSavedUserSkinType(context);
        if (skinType == null || skinType.trim().isEmpty()) {
            skinType = prefs.getString("SKIN_TYPE_RESULT", "Da hỗn hợp thiên dầu");
        }
        if (skinType == null || skinType.trim().isEmpty()) {
            skinType = "Da hỗn hợp thiên dầu";
        }

        String recommendation = ProfileSession.getSavedRecommendation(context);
        if (recommendation == null) recommendation = "";

        String skinAreas = ProfileSession.getSavedSkinAreas(context);
        if (skinAreas == null || skinAreas.trim().isEmpty()) {
            skinAreas = prefs.getString("SAVED_SKIN_AREAS", "");
        }

        Set<String> flagged = prefs.getStringSet("SAVED_FLAGGED_GROUPS", null);
        if (flagged == null) flagged = new HashSet<>();

        return new UserSkinProfile(
                skinType,
                recommendation,
                skinAreas,
                ProfileSession.getSavedSensitivity(context),
                ProfileSession.getSavedHydration(context),
                ProfileSession.getSavedElasticity(context),
                ProfileSession.getSavedSebum(context),
                flagged,
                hasSavedProfile
        );
    }

    /** Chỉ số hôm nay = hồ sơ da đã lưu + điều chỉnh theo thời tiết thực tế. */
    public static TodaySkinMetrics computeTodayMetrics(UserSkinProfile profile,
                                                        double temp, int humidity, double uv,
                                                        int pm25, boolean hasPm25) {
        int oily = profile.sebum;
        oily += temp >= 33 ? 12 : temp >= 28 ? 6 : temp < 22 ? -4 : 0;
        if (humidity > 75 && profile.sebum >= 55) oily += 4;

        int hydration = profile.hydration;
        hydration += humidity < 40 ? -12 : humidity < 55 ? -4 : humidity > 75 ? 6 : 0;
        if (uv >= 8.0) hydration -= 6;
        else if (uv >= 5.0) hydration -= 3;
        if (temp >= 33) hydration -= 4;

        int sensitivity = profile.sensitivity;
        sensitivity += uv >= 8.0 ? 15 : uv >= 5.0 ? 8 : 0;
        if (temp >= 33) sensitivity += 8;
        if (hasPm25) {
            if (pm25 > 55) sensitivity += 12;
            else if (pm25 > 35) sensitivity += 5;
        }

        return new TodaySkinMetrics(
                clamp(oily, 5, 98),
                clamp(hydration, 5, 98),
                clamp(sensitivity, 5, 98)
        );
    }

    public static String buildSkinStatusSectionTitle(UserSkinProfile profile) {
        return "TÌNH TRẠNG DA HÔM NAY";
    }

    public static String buildSkinStatusSectionSubtitle(UserSkinProfile profile) {
        if (profile.hasSavedProfile) {
            return "Theo hồ sơ: " + profile.skinType;
        }
        return "Chưa có hồ sơ da — làm bài test da để Rootie tư vấn chính xác hơn";
    }

    public static PersonalizedAdvice buildRuleBasedAdvice(UserSkinProfile profile, TodaySkinMetrics metrics,
                                                          double temp, int humidity, double uv,
                                                          int pm25, boolean hasPm25, String cityName) {
        String skin = profile.skinType;
        String headline;
        String subtext;

        if (uv >= 8) {
            headline = metrics.sensitivityPercent >= 60
                    ? skin + " hôm nay rất dễ kích ứng — UV đang ở mức rất cao."
                    : "UV rất cao — " + skin + " cần chống nắng và che chắn kỹ.";
            subtext = "Chỉ số UV " + String.format(Locale.US, "%.1f", uv) + " tại " + cityName
                    + ". Với độ nhạy cảm " + metrics.sensitivityPercent + "% (từ hồ sơ da của bạn), hãy ưu tiên KCN phổ rộng và làm dịu da.";
        } else if (metrics.hydrationPercent <= 40 && humidity < 45) {
            headline = skin + " đang thiếu ẩm — hôm nay cần cấp nước và khóa ẩm.";
            subtext = "Độ ẩm không khí " + humidity + "% làm mức cấp nước da bạn (~" + metrics.hydrationPercent
                    + "%) dễ giảm thêm. " + pickRecommendationHint(profile);
        } else if (metrics.oilyPercent >= 70 && temp >= 30) {
            headline = "Nhiệt độ " + (int) Math.round(temp) + "°C có thể khiến " + skin + " bóng nhờn hơn.";
            subtext = "Mức bã nhờn ước tính hôm nay ~" + metrics.oilyPercent + "% (từ hồ sơ bã nhờn "
                    + profile.sebum + "%). Dùng sản phẩm mỏng nhẹ, tránh bít tắc lỗ chân lông.";
        } else if (hasPm25 && pm25 > 55) {
            headline = "Không khí kém — " + skin + " cần làm sạch kỹ khi về nhà.";
            subtext = "PM2.5 ~" + pm25 + " µg/m³. Với độ nhạy cảm " + metrics.sensitivityPercent
                    + "%, hạn chế tiếp xúc bụi và rửa mặt dịu nhẹ ngay sau khi ra ngoài.";
        } else if (uv >= 5) {
            headline = skin + " cần chống nắng và phục hồi da hôm nay.";
            subtext = "UV " + String.format(Locale.US, "%.1f", uv) + ", nhiệt độ " + (int) Math.round(temp)
                    + "°C. " + pickRecommendationHint(profile);
        } else {
            headline = "Thời tiết ổn — duy trì routine phù hợp với " + skin + ".";
            subtext = pickRecommendationHint(profile);
            if (subtext.isEmpty()) {
                subtext = "Rootie đang theo dõi hồ sơ da của bạn để điều chỉnh chu trình mỗi ngày.";
            }
        }

        String insight = buildInsightBody(profile, metrics, temp, humidity, uv, pm25, hasPm25);
        return new PersonalizedAdvice(headline, subtext, "“" + insight + "”");
    }

    public static String buildGeminiPrompt(UserSkinProfile profile, TodaySkinMetrics metrics,
                                           double temp, int humidity, double uv,
                                           int pm25, boolean hasPm25, String cityName) {
        String pm25Text = hasPm25 ? pm25 + " µg/m³" : "không có dữ liệu";
        String flagged = profile.flaggedGroups.isEmpty() ? "không" : String.join(", ", profile.flaggedGroups);
        return "Hồ sơ da đã lưu của người dùng:\n"
                + "- Loại da: " + profile.skinType + "\n"
                + "- Khuyến nghị trước đó: " + (profile.recommendation.isEmpty() ? "chưa có" : profile.recommendation) + "\n"
                + "- Vùng da / mô tả: " + (profile.skinAreas == null || profile.skinAreas.isEmpty() ? "chưa có" : profile.skinAreas) + "\n"
                + "- Chỉ số nhạy cảm (quiz): " + profile.sensitivity + "%\n"
                + "- Chỉ số cấp nước (quiz): " + profile.hydration + "%\n"
                + "- Chỉ số bã nhờn (quiz): " + profile.sebum + "%\n"
                + "- Chỉ số đàn hồi (quiz): " + profile.elasticity + "%\n"
                + "- Thành phần cần tránh: " + flagged + "\n"
                + "- Chỉ số ước tính hôm nay (hồ sơ + thời tiết): bã nhờn " + metrics.oilyPercent
                + "%, cấp nước " + metrics.hydrationPercent + "%, nhạy cảm " + metrics.sensitivityPercent + "%\n\n"
                + "Thời tiết hiện tại tại " + cityName + ":\n"
                + "- Nhiệt độ: " + temp + "°C\n"
                + "- Độ ẩm không khí: " + humidity + "%\n"
                + "- UV hiện tại: " + String.format(Locale.US, "%.1f", uv) + "\n"
                + "- PM2.5: " + pm25Text + "\n\n"
                + "Hãy trả lời ĐÚNG theo format sau (tiếng Việt, thuần chay, ngắn gọn):\n"
                + "[TIEU_DE] 1 câu tiêu đề ngắn, cá nhân hóa theo loại da và thời tiết\n"
                + "[MO_TA] 1-2 câu giải thích dựa trên hồ sơ da đã lưu\n"
                + "[LOI_KHUYEN] 2-3 câu lời khuyên thực tế, bọc trong dấu ngoặc kép “ ”";
    }

    public static PersonalizedAdvice parseGeminiAdvice(String raw, UserSkinProfile profile,
                                                     TodaySkinMetrics metrics, double temp, int humidity,
                                                     double uv, int pm25, boolean hasPm25, String cityName) {
        if (raw == null || raw.trim().isEmpty()) {
            return buildRuleBasedAdvice(profile, metrics, temp, humidity, uv, pm25, hasPm25, cityName);
        }
        String headline = extractTag(raw, "TIEU_DE");
        String subtext = extractTag(raw, "MO_TA");
        String insight = extractTag(raw, "LOI_KHUYEN");
        if (headline.isEmpty() && subtext.isEmpty() && insight.isEmpty()) {
            insight = raw.trim();
            if (!insight.startsWith("“")) insight = "“" + insight;
            if (!insight.endsWith("”")) insight = insight + "”";
            PersonalizedAdvice fallback = buildRuleBasedAdvice(profile, metrics, temp, humidity, uv, pm25, hasPm25, cityName);
            return new PersonalizedAdvice(fallback.headline, fallback.subtext, insight);
        }
        if (headline.isEmpty() || subtext.isEmpty()) {
            PersonalizedAdvice fallback = buildRuleBasedAdvice(profile, metrics, temp, humidity, uv, pm25, hasPm25, cityName);
            if (headline.isEmpty()) headline = fallback.headline;
            if (subtext.isEmpty()) subtext = fallback.subtext;
        }
        if (insight.isEmpty()) {
            insight = "“" + buildInsightBody(profile, metrics, temp, humidity, uv, pm25, hasPm25) + "”";
        } else if (!insight.startsWith("“")) {
            insight = "“" + insight;
            if (!insight.endsWith("”")) insight = insight + "”";
        }
        return new PersonalizedAdvice(headline, subtext, insight);
    }

    private static String buildInsightBody(UserSkinProfile profile, TodaySkinMetrics metrics,
                                           double temp, int humidity, double uv,
                                           int pm25, boolean hasPm25) {
        String skin = profile.skinType;
        if (uv >= 8) {
            return "Với " + skin + " (nhạy cảm " + metrics.sensitivityPercent + "%), UV "
                    + String.format(Locale.US, "%.1f", uv) + " rất nguy hiểm. Thoa KCN phổ rộng SPF50+, che chắn và dùng sản phẩm phục hồi dịu nhẹ tối nay.";
        }
        if (metrics.hydrationPercent <= 40) {
            return skin + " đang cần cấp ẩm (chỉ số " + metrics.hydrationPercent + "%). Dùng serum HA và kem khóa ẩm, đặc biệt khi độ ẩm không khí chỉ " + humidity + "%.";
        }
        if (metrics.oilyPercent >= 70) {
            return "Bã nhờn hôm nay có thể tăng (~" + metrics.oilyPercent + "%). Với " + skin + ", chọn sữa rửa mặt kiềm dầu dịu và gel dưỡng mỏng nhẹ, tránh sản phẩm dày gây bí da.";
        }
        if (hasPm25 && pm25 > 55) {
            return "PM2.5 cao (" + pm25 + " µg/m³) dễ kích thích " + skin + ". Làm sạch nhẹ khi về nhà và tăng cường hàng rào bảo vệ da.";
        }
        if (!profile.recommendation.isEmpty()) {
            return profile.recommendation + " Hôm nay tiếp tục duy trì routine này và bôi kem chống nắng nếu ra ngoài.";
        }
        return "Dựa trên hồ sơ " + skin + ", hãy giữ routine lành tính, cấp ẩm vừa đủ và chống nắng khi UV từ " + String.format(Locale.US, "%.1f", uv) + ".";
    }

    private static String pickRecommendationHint(UserSkinProfile profile) {
        if (profile.recommendation != null && !profile.recommendation.trim().isEmpty()) {
            return profile.recommendation;
        }
        if (profile.skinAreas != null && !profile.skinAreas.trim().isEmpty()) {
            return profile.skinAreas;
        }
        return "";
    }

    private static String extractTag(String raw, String tag) {
        String open = "[" + tag + "]";
        int start = raw.indexOf(open);
        if (start < 0) return "";
        start += open.length();
        int next = raw.indexOf("[", start);
        String value = next >= 0 ? raw.substring(start, next) : raw.substring(start);
        return value.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
