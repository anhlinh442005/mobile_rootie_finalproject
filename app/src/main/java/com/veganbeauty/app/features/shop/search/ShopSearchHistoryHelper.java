package com.veganbeauty.app.features.shop.search;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopSearchHistoryHelper {
    private static final String PREFS_NAME = "ShopSearchPrefs";
    private static final String KEY_HISTORY = "search_history";
    private static final int MAX_ITEMS = 10;

    public static List<String> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String historyString = prefs.getString(KEY_HISTORY, "");
        if (historyString == null || historyString.isEmpty()) {
            return new ArrayList<>();
        }
        String[] split = historyString.split("\\|\\|\\|");
        List<String> list = new ArrayList<>();
        for (String s : split) {
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list;
    }

    public static void add(Context context, String query) {
        if (query == null || query.trim().isEmpty()) return;
        List<String> current = new ArrayList<>(getHistory(context));
        current.remove(query);
        current.add(0, query);
        if (current.size() > MAX_ITEMS) {
            current.remove(current.size() - 1);
        }
        save(context, current);
    }

    public static void remove(Context context, String query) {
        List<String> current = new ArrayList<>(getHistory(context));
        current.remove(query);
        save(context, current);
    }

    public static void clear(Context context) {
        save(context, new ArrayList<>());
    }

    private static void save(Context context, List<String> history) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append("|||");
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, sb.toString())
                .apply();
    }
}
