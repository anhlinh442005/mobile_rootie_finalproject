package com.veganbeauty.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Trích chỉ số da từ payload quiz / quét AI để so sánh trước–sau. */
public final class SkinProfileMetricsHelper {

    private SkinProfileMetricsHelper() {
    }

    public static final class Snapshot {
        public final int hydration;
        public final int sebum;
        public final int sensitivity;
        public final int elasticity;
        @Nullable public final String skinType;
        @Nullable public final String dateLabel;
        public final boolean hasMetrics;

        Snapshot(int hydration, int sebum, int sensitivity, int elasticity,
                   @Nullable String skinType, @Nullable String dateLabel, boolean hasMetrics) {
            this.hydration = hydration;
            this.sebum = sebum;
            this.sensitivity = sensitivity;
            this.elasticity = elasticity;
            this.skinType = skinType;
            this.dateLabel = dateLabel;
            this.hasMetrics = hasMetrics;
        }

        static Snapshot invalid() {
            return new Snapshot(-1, -1, -1, -1, null, null, false);
        }
    }

  /** Lịch sử có chỉ số đo được, mới nhất trước. */
    @NonNull
    public static List<Snapshot> loadComparableSnapshots(@NonNull Context context,
                                                         @Nullable String userId,
                                                         @Nullable String email) {
        List<JSONObject> payloads = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        appendUniquePayloads(payloads, seenIds,
                SkinHistoryLocalStore.getHistory(context, userId, email));

        SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String quizHistoryStr = prefs.getString("QUIZ_HISTORY_LIST", "[]");
        try {
            appendUniquePayloads(payloads, seenIds, new JSONArray(quizHistoryStr != null ? quizHistoryStr : "[]"));
        } catch (Exception ignored) {
        }

        payloads.sort((a, b) -> extractSortKey(b).compareTo(extractSortKey(a)));

        List<Snapshot> snapshots = new ArrayList<>();
        for (JSONObject obj : payloads) {
            Snapshot snap = fromPayload(obj);
            if (snap.hasMetrics) {
                snapshots.add(snap);
            }
        }
        return snapshots;
    }

    public static boolean hasBeforeAfterData(@NonNull Context context,
                                             @Nullable String userId,
                                             @Nullable String email) {
        return loadComparableSnapshots(context, userId, email).size() >= 2;
    }

    @NonNull
    public static Snapshot fromPayload(@Nullable JSONObject obj) {
        if (obj == null) {
            return Snapshot.invalid();
        }

        String skinType = obj.optString("skinType", "");
        if (skinType.isEmpty()) {
            JSONObject skinCondition = obj.optJSONObject("skinCondition");
            if (skinCondition != null) {
                skinType = skinCondition.optString("skinType", "");
            }
        }

        String dateLabel = obj.optString("date", "").trim();
        if (dateLabel.isEmpty()) {
            dateLabel = obj.optString("time", "").trim();
        }

        if (obj.has("hydration") || obj.has("sebum") || obj.has("sensitivity")) {
            return new Snapshot(
                    clamp(obj.optInt("hydration", -1)),
                    clamp(obj.optInt("sebum", -1)),
                    clamp(obj.optInt("sensitivity", -1)),
                    clamp(obj.optInt("elasticity", 75)),
                    skinType.isEmpty() ? null : skinType,
                    dateLabel.isEmpty() ? null : dateLabel,
                    obj.optInt("hydration", -1) >= 0 || obj.optInt("sebum", -1) >= 0
            );
        }

        JSONObject detailed = obj.optJSONObject("detailedEvaluation");
        if (detailed != null) {
            int hydration = scoreFromEval(detailed, "moisture");
            int sebum = scoreFromEval(detailed, "oil");
            int sensitivity = scoreFromEval(detailed, "sensitivity");
            boolean has = hydration >= 0 || sebum >= 0;
            if (has) {
                return new Snapshot(
                        hydration >= 0 ? hydration : 50,
                        sebum >= 0 ? sebum : 50,
                        sensitivity >= 0 ? sensitivity : 50,
                        75,
                        skinType.isEmpty() ? null : skinType,
                        dateLabel.isEmpty() ? null : dateLabel,
                        true
                );
            }
        }

        return Snapshot.invalid();
    }

    private static void appendUniquePayloads(List<JSONObject> target, Set<String> seenIds, JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id", "");
                if (!id.isEmpty()) {
                    if (!seenIds.add(id)) {
                        continue;
                    }
                }
                target.add(obj);
            } catch (Exception ignored) {
            }
        }
    }

    private static String extractSortKey(JSONObject obj) {
        String date = obj.optString("date", "");
        String time = obj.optString("time", "");
        return (date + " " + time).trim();
    }

    private static int scoreFromEval(JSONObject detailed, String key) {
        JSONObject node = detailed.optJSONObject(key);
        if (node != null && node.has("score")) {
            return clamp(node.optInt("score", -1));
        }
        return -1;
    }

    private static int clamp(int value) {
        if (value < 0) {
            return -1;
        }
        return Math.max(0, Math.min(100, value));
    }
}
