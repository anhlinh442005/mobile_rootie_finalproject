package com.veganbeauty.app.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SampleAvatarCatalog {

    public static final String AVATAR_REF_PREFIX = "rootie_sample:";

    public static final class Item {
        public final String id;
        public final int drawableRes;
        public final String label;

        public Item(@NonNull String id, int drawableRes, @NonNull String label) {
            this.id = id;
            this.drawableRes = drawableRes;
            this.label = label;
        }
    }

    private static final List<Item> ITEMS = Collections.unmodifiableList(Arrays.asList(
            new Item("smile", R.drawable.avt_rootie_smile, "Vui vẻ"),
            new Item("chill", R.drawable.avt_rootie_chill, "Thư giãn"),
            new Item("nangdong", R.drawable.avt_rootie_nangdong, "Năng động"),
            new Item("deadline", R.drawable.avt_rootie_deadline, "Làm việc"),
            new Item("ngu", R.drawable.avt_rootie_ngu, "Ngủ ngon"),
            new Item("aodai", R.drawable.avt_rootie_aodai, "Áo dài"),
            new Item("cauvang", R.drawable.avt_rootie_cauvang, "Cầu Vàng"),
            new Item("caurong", R.drawable.avt_rootie_caurong, "Cầu Rồng"),
            new Item("landmark81", R.drawable.avt_rootie_landmark81, "Landmark 81"),
            new Item("hanoi", R.drawable.avt_rootie_hanoi, "Hà Nội"),
            new Item("uel", R.drawable.avt_rootie_uel, "UEL")
    ));

    private SampleAvatarCatalog() {
    }

    @NonNull
    public static List<Item> getAll() {
        return ITEMS;
    }

    @NonNull
    public static String toAvatarRef(@NonNull String sampleId) {
        return AVATAR_REF_PREFIX + sampleId;
    }

    @NonNull
    public static String toAvatarRef(@NonNull Item item) {
        return toAvatarRef(item.id);
    }

    public static boolean isSampleAvatarRef(@Nullable String avatarRef) {
        return avatarRef != null && avatarRef.trim().startsWith(AVATAR_REF_PREFIX);
    }

    public static int resolveDrawableRes(@Nullable String avatarRef) {
        if (!isSampleAvatarRef(avatarRef)) {
            return 0;
        }
        String sampleId = avatarRef.trim().substring(AVATAR_REF_PREFIX.length());
        for (Item item : ITEMS) {
            if (item.id.equals(sampleId)) {
                return item.drawableRes;
            }
        }
        return 0;
    }
}
