package com.veganbeauty.app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;

/**
 * Decode túi đỏ reward ở kích thước nhỏ — tránh OOM khi PNG lớn
 * bị Android scale theo density nếu load bằng setImageResource.
 */
public final class MoneyBagBitmapHelper {

    private MoneyBagBitmapHelper() {
    }

    public static void bind(@Nullable ImageView imageView) {
        bind(imageView, R.drawable.img_coin_reward_moneybag, 220);
    }

    public static void bind(@Nullable ImageView imageView, @DrawableRes int resId, int targetDp) {
        if (imageView == null) {
            return;
        }
        Context context = imageView.getContext();
        if (context == null) {
            return;
        }
        try {
            Bitmap bitmap = decodeSampled(context.getResources(), resId, targetDp);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_coin);
            }
        } catch (OutOfMemoryError | Exception e) {
            try {
                imageView.setImageResource(R.drawable.ic_coin);
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    private static Bitmap decodeSampled(@NonNull Resources res, @DrawableRes int resId, int targetDp) {
        int targetPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, targetDp, res.getDisplayMetrics());
        targetPx = Math.max(96, targetPx);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetPx, targetPx);
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeResource(res, resId, opts);
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
