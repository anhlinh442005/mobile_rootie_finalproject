package com.veganbeauty.app.utils;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.veganbeauty.app.R;

public final class BeVietnamProFontHelper {

    private BeVietnamProFontHelper() {
    }

    public static void apply(@NonNull View root) {
        Typeface typeface = ResourcesCompat.getFont(root.getContext(), R.font.be_vietnam_pro);
        if (typeface == null) {
            return;
        }
        apply(root, typeface);
    }

    private static void apply(@NonNull View view, @NonNull Typeface base) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int style = Typeface.NORMAL;
            Typeface current = textView.getTypeface();
            if (current != null) {
                style = current.getStyle();
            }
            textView.setTypeface(base, style);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), base);
            }
        }
    }

    public static void applyToTextView(@NonNull TextView textView) {
        Typeface typeface = ResourcesCompat.getFont(textView.getContext(), R.font.be_vietnam_pro);
        if (typeface == null) {
            return;
        }
        int style = Typeface.NORMAL;
        Typeface current = textView.getTypeface();
        if (current != null) {
            style = current.getStyle();
        }
        textView.setTypeface(typeface, style);
    }
}
