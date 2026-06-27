package com.veganbeauty.app.features.community.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;

public class WithdrawalDetailPlaceholderFragment extends Fragment {

    private String withdrawId = "#WD20260615";
    private String amount = "500.000đ";
    private String date = "15/06/2026";
    private String status = "Thành công";

    public static WithdrawalDetailPlaceholderFragment newInstance(String withdrawId, String amount, String date, String status) {
        WithdrawalDetailPlaceholderFragment fragment = new WithdrawalDetailPlaceholderFragment();
        Bundle args = new Bundle();
        args.putString("WITHDRAW_ID", withdrawId);
        args.putString("AMOUNT", amount);
        args.putString("DATE", date);
        args.putString("STATUS", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            withdrawId = getArguments().getString("WITHDRAW_ID", "#WD20260615");
            amount = getArguments().getString("AMOUNT", "500.000đ");
            date = getArguments().getString("DATE", "15/06/2026");
            status = getArguments().getString("STATUS", "Thành công");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_withdrawal_detail_placeholder, container, false);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        TextView tvAmount = view.findViewById(R.id.tvAmount);
        if (tvAmount != null) tvAmount.setText(amount);

        TextView tvTransactionCode = view.findViewById(R.id.tvTransactionCode);
        if (tvTransactionCode != null) tvTransactionCode.setText(withdrawId);

        TextView tvRequestTime = view.findViewById(R.id.tvRequestTime);
        if (tvRequestTime != null) tvRequestTime.setText(date);

        TextView tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        if (tvStatusBadge != null) tvStatusBadge.setText(status);

        return view;
    }
}
