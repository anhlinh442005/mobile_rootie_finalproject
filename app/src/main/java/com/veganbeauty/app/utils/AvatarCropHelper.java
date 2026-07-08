package com.veganbeauty.app.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.ImageView;

import androidx.annotation.NonNull;

/**
 * Giới hạn pan/zoom avatar trong vùng ảnh và cắt đúng khung tròn overlay.
 */
public final class AvatarCropHelper {

    public static final float CROP_RADIUS_RATIO = 0.9f;

    private AvatarCropHelper() {
    }

    public static float getCropRadius(float viewWidth, float viewHeight) {
        return (Math.min(viewWidth, viewHeight) / 2f) * CROP_RADIUS_RATIO;
    }

    public static float getMinScale(float imageWidth, float imageHeight, float cropDiameter) {
        return Math.max(cropDiameter / imageWidth, cropDiameter / imageHeight);
    }

    public static void initializeCropState(
            @NonNull float[] cropState,
            float imageWidth,
            float imageHeight,
            float viewWidth,
            float viewHeight
    ) {
        float cropRadius = getCropRadius(viewWidth, viewHeight);
        float minScale = getMinScale(imageWidth, imageHeight, cropRadius * 2f);
        float coverScale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight);
        float initialScale = Math.max(minScale, coverScale);

        cropState[0] = initialScale;
        cropState[1] = (viewWidth - imageWidth * initialScale) / 2f;
        cropState[2] = (viewHeight - imageHeight * initialScale) / 2f;
        clampTranslation(cropState, imageWidth, imageHeight, viewWidth, viewHeight);
    }

    public static void clampTranslation(
            @NonNull float[] cropState,
            float imageWidth,
            float imageHeight,
            float viewWidth,
            float viewHeight
    ) {
        float cropRadius = getCropRadius(viewWidth, viewHeight);
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        float cropLeft = centerX - cropRadius;
        float cropTop = centerY - cropRadius;
        float cropRight = centerX + cropRadius;
        float cropBottom = centerY + cropRadius;

        float minScale = getMinScale(imageWidth, imageHeight, cropRadius * 2f);
        cropState[0] = Math.max(cropState[0], minScale);

        float scaledWidth = imageWidth * cropState[0];
        float scaledHeight = imageHeight * cropState[0];

        float minTransX = cropRight - scaledWidth;
        float maxTransX = cropLeft;
        float minTransY = cropBottom - scaledHeight;
        float maxTransY = cropTop;

        cropState[1] = Math.max(minTransX, Math.min(maxTransX, cropState[1]));
        cropState[2] = Math.max(minTransY, Math.min(maxTransY, cropState[2]));
    }

    public static void applyMatrix(@NonNull ImageView imageView, @NonNull float[] cropState) {
        Matrix matrix = new Matrix();
        matrix.postScale(cropState[0], cropState[0]);
        matrix.postTranslate(cropState[1], cropState[2]);
        imageView.setImageMatrix(matrix);
    }

    public static void zoomAroundCenter(
            @NonNull float[] cropState,
            float baseScale,
            float baseTransX,
            float baseTransY,
            float zoomFactor,
            float imageWidth,
            float imageHeight,
            float viewWidth,
            float viewHeight
    ) {
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        float bitmapX = (centerX - baseTransX) / baseScale;
        float bitmapY = (centerY - baseTransY) / baseScale;

        float newScale = baseScale * zoomFactor;
        cropState[0] = newScale;
        cropState[1] = centerX - newScale * bitmapX;
        cropState[2] = centerY - newScale * bitmapY;
        clampTranslation(cropState, imageWidth, imageHeight, viewWidth, viewHeight);
    }

    @NonNull
    public static Bitmap cropCircularBitmap(
            @NonNull Bitmap source,
            @NonNull float[] cropState,
            float viewWidth,
            float viewHeight,
            int outputSize
    ) {
        float cropRadius = getCropRadius(viewWidth, viewHeight);
        float cropCenterX = viewWidth / 2f;
        float cropCenterY = viewHeight / 2f;
        float viewToOutput = outputSize / (cropRadius * 2f);

        Bitmap output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Path clipPath = new Path();
        clipPath.addCircle(outputSize / 2f, outputSize / 2f, outputSize / 2f, Path.Direction.CW);
        canvas.clipPath(clipPath);

        Matrix drawMatrix = new Matrix();
        drawMatrix.postScale(cropState[0], cropState[0]);
        drawMatrix.postTranslate(cropState[1], cropState[2]);
        drawMatrix.postTranslate(-(cropCenterX - cropRadius), -(cropCenterY - cropRadius));
        drawMatrix.postScale(viewToOutput, viewToOutput);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, drawMatrix, paint);
        return output;
    }
}
