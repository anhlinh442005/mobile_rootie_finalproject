package com.veganbeauty.app.features.ai;

import android.content.Context;

import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.features.weather.SkinWeatherSnapshotManager;

/** Xuất ngữ cảnh từ {@link SkinAiAppDataSnapshot} — một nguồn dữ liệu duy nhất. */
public final class SkinAiUserContextBuilder {

    private SkinAiUserContextBuilder() {
    }

    public static String build(Context context) {
        return SkinAiSnapshotFormatter.toFullContext(SkinAiAppDataLoader.load(context));
    }

    public static String buildCompact(
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherSnapshotManager.Snapshot weather) {
        return SkinAiSnapshotFormatter.toCompact(SkinAiAppDataLoader.load(context));
    }
}
