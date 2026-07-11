package com.veganbeauty.app.utils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.veganbeauty.app.R;

public class CoinRewardDialogFragment extends DialogFragment {

    private static final String ARG_COINS = "arg_coins";
    private static final String ARG_TOTAL_BALANCE = "arg_total_balance";
    private static final String ARG_SOURCE = "arg_source";

    public static CoinRewardDialogFragment newInstance(int coins, int totalBalance, @Nullable String source) {
        CoinRewardDialogFragment fragment = new CoinRewardDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COINS, coins);
        args.putInt(ARG_TOTAL_BALANCE, totalBalance);
        args.putString(ARG_SOURCE, source);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar);
        setCancelable(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0f;
            window.setAttributes(params);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_coin_reward, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int coins = getArguments() != null ? getArguments().getInt(ARG_COINS, 0) : 0;
        int totalBalance = getArguments() != null ? getArguments().getInt(ARG_TOTAL_BALANCE, 0) : 0;
        String source = getArguments() != null ? getArguments().getString(ARG_SOURCE) : null;

        TextView tvCoinAmount = view.findViewById(R.id.tvCoinAmount);
        TextView tvCoinBadge = view.findViewById(R.id.tvCoinBadge);
        TextView tvRewardSource = view.findViewById(R.id.tvRewardSource);
        View layoutRewardContent = view.findViewById(R.id.layoutRewardContent);
        android.widget.ImageView ivMoneyBag = view.findViewById(R.id.ivMoneyBag);
        MoneyBagBitmapHelper.bind(ivMoneyBag);

        tvCoinAmount.setText(CoinRewardDialogHelper.formatAmountLabel(coins));
        tvCoinBadge.setText(CoinRewardDialogHelper.formatBadgeLabel(coins));

        StringBuilder subtitle = new StringBuilder();
        if (source != null && !source.trim().isEmpty()) {
            subtitle.append(source.trim());
        }
        if (totalBalance > 0) {
            if (subtitle.length() > 0) {
                subtitle.append("\n");
            }
            subtitle.append(CoinRewardDialogHelper.formatBalanceSubtitle(totalBalance));
        }
        if (subtitle.length() > 0) {
            tvRewardSource.setText(subtitle.toString());
            tvRewardSource.setVisibility(View.VISIBLE);
        } else {
            tvRewardSource.setVisibility(View.GONE);
        }

        layoutRewardContent.setScaleX(0.92f);
        layoutRewardContent.setScaleY(0.92f);
        layoutRewardContent.setAlpha(0f);
        layoutRewardContent.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(280)
                .start();

        View.OnClickListener dismissListener = v -> dismiss();
        view.findViewById(R.id.rootOverlay).setOnClickListener(dismissListener);
        view.findViewById(R.id.tvTapToClose).setOnClickListener(dismissListener);
        layoutRewardContent.setClickable(true);
        layoutRewardContent.setOnClickListener(v -> {
            // Prevent tap-through to the overlay dismiss handler.
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            if (fm != null) {
                fm.setFragmentResult(CoinRewardDialogHelper.RESULT_DISMISSED, new Bundle());
            }
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.getSupportFragmentManager()
                        .setFragmentResult(CoinRewardDialogHelper.RESULT_DISMISSED, new Bundle());
            }
        } catch (Exception ignored) {
        }
    }
}
