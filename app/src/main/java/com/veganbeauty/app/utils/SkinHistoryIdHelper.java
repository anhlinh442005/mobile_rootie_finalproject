package com.veganbeauty.app.utils;

public final class SkinHistoryIdHelper {

    private static final String PREFIX = "sh_";

    private SkinHistoryIdHelper() {
    }

    public static String generateId() {
        return fromTimestampMillis(System.currentTimeMillis());
    }

    public static String fromTimestampMillis(long timestampMillis) {
        return PREFIX + timestampMillis;
    }
}
