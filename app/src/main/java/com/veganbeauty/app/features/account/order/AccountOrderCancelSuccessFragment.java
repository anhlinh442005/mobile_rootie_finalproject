package com.veganbeauty.app.features.account.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.AccountOrderCancelSuccessFragmentBinding;
import com.veganbeauty.app.features.home.HomeFragment;

public class AccountOrderCancelSuccessFragment extends RootieFragment {

    private AccountOrderCancelSuccessFragmentBinding binding;
    private String orderId;
    private long refundAmount;
    private String refundMethod;

    public static AccountOrderCancelSuccessFragment newInstance(String orderId, long refundAmount, String refundMethod) {
        AccountOrderCancelSuccessFragment fragment = new AccountOrderCancelSuccessFragment();
        Bundle args = new Bundle();
        args.putString("orderId", orderId);
        args.putLong("refundAmount", refundAmount);
        args.putString("refundMethod", refundMethod);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            orderId = getArguments().getString("orderId", "");
            refundAmount = getArguments().getLong("refundAmount", 0);
            refundMethod = getArguments().getString("refundMethod", "Ví RootiePay");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountOrderCancelSuccessFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        binding.tvSuccessDesc.setText("Yêu cầu huỷ đơn hàng #" + orderId + " của bạn đã được gửi thành công. Rootie sẽ xử lý yêu cầu trong thời gian sớm nhất.");
        
        binding.tvRefundAmount.setText(formatCurrency(refundAmount));
        binding.tvRefundMethod.setText(refundMethod);

        binding.btnTrackOrder.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(); // go back to order details or order list
        });

        binding.btnGoHome.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new HomeFragment())
                    .commit();
        });
    }

    private String formatCurrency(long amount) {
        return String.format("%,dđ", amount).replace(',', '.');
    }

    @Override
    protected void observeViewModel() {
        // No ViewModel needed
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
