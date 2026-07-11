package com.veganbeauty.app.features.routine;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;

public final class RoutineSetupDialogHelper {

    private RoutineSetupDialogHelper() {}

    public static void show(
            Fragment fragment,
            Runnable onSetupNow,
            @Nullable Runnable onLater
    ) {
        if (fragment == null || !fragment.isAdded()) return;
        Context ctx = fragment.requireContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_routine_not_configured, null);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        view.findViewById(R.id.btnRoutineSetupNow).setOnClickListener(v -> {
            dialog.dismiss();
            if (onSetupNow != null) onSetupNow.run();
        });

        view.findViewById(R.id.btnRoutineSetupLater).setOnClickListener(v -> {
            dialog.dismiss();
            if (onLater != null) onLater.run();
        });

        dialog.show();
    }
}
