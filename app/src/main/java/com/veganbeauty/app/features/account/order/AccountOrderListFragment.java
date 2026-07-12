package com.veganbeauty.app.features.account.order;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LifecycleOwnerKt;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.features.ai.SkinChatFragment;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderListFragmentBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountOrderListFragment extends RootieFragment {

    private AccountOrderListFragmentBinding _binding;
    private OrderListViewModel viewModel;
    private OrderListAdapter orderAdapter;

    private static final String ARG_INITIAL_STATUS = "arg_initial_status";

    public static AccountOrderListFragment newInstance(String initialStatus) {
        AccountOrderListFragment fragment = new AccountOrderListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_STATUS, initialStatus);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountOrderListFragmentBinding.inflate(inflater, container, false);
        
        orderAdapter = new OrderListAdapter(
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
            },
            order -> {
                showCancelReasonBottomSheet(order);
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
            },
            order -> {
                ArrayList<CartItemEntity> checkoutItems = new ArrayList<>();
                List<OrderEntity.OrderItem> items = order.getItems();
                if (items != null) {
                    for (OrderEntity.OrderItem item : items) {
                        checkoutItems.add(new CartItemEntity(
                            item.getProductId(),
                            item.getProductName(),
                            item.getProductImage(),
                            item.getPrice(),
                            item.getQuantity(),
                            true
                        ));
                    }
                }
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, ShopCheckoutFragment.newInstance(checkoutItems, "", 0L))
                    .addToBackStack(null)
                    .commit();
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderTrackingFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new SkinChatFragment())
                    .addToBackStack(null)
                    .commit();
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderReviewFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
            }
        );

        setupViewModel();
        return _binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        OrderRepository repository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new OrderListViewModel(repository, requireContext().getApplicationContext());
            }
        }).get(OrderListViewModel.class);
    }

    @Override
    protected void setupUI(@NonNull View view) {
        String initialStatus = getArguments() != null ? getArguments().getString(ARG_INITIAL_STATUS, "Tất cả") : "Tất cả";
        viewModel.setFilter(initialStatus);

        _binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        _binding.rvOrders.setAdapter(orderAdapter);

        _binding.tabAll.setOnClickListener(v -> viewModel.setFilter("Tất cả"));
        _binding.tabPending.setOnClickListener(v -> viewModel.setFilter("Chờ xác nhận"));
        _binding.tabProcessing.setOnClickListener(v -> viewModel.setFilter("Đang xử lý"));
        _binding.tabDelivering.setOnClickListener(v -> viewModel.setFilter("Đang giao"));
        _binding.tabSuccess.setOnClickListener(v -> viewModel.setFilter("Hoàn tất"));
        _binding.tabCancelled.setOnClickListener(v -> viewModel.setFilter("Đã hủy"));
    }

    @Override
    protected void observeViewModel() {
        viewModel.filteredOrders.observe(getViewLifecycleOwner(), orders -> {
            orderAdapter.submitList(orders);
        });

        viewModel.orderStats.observe(getViewLifecycleOwner(), stats -> {
            _binding.tvOrderStats.setText(stats);
        });

        viewModel.selectedStatus.observe(getViewLifecycleOwner(), this::updateTabStyles);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.resumeOrderRealtimeSync();
        }
    }

    @Override
    public void onPause() {
        if (viewModel != null) {
            viewModel.pauseOrderRealtimeSync();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    private void updateTabStyles(String activeStatus) {
        if (_binding == null) return;
        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("Tất cả", _binding.tabAll);
        tabs.put("Chờ xác nhận", _binding.tabPending);
        tabs.put("Đang xử lý", _binding.tabProcessing);
        tabs.put("Đang giao", _binding.tabDelivering);
        tabs.put("Hoàn tất", _binding.tabSuccess);
        tabs.put("Đã hủy", _binding.tabCancelled);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            String status = entry.getKey();
            TextView textView = entry.getValue();
            if (status.equalsIgnoreCase(activeStatus)) {
                textView.setBackgroundResource(R.drawable.bg_btn_buy);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }
        }
    }

    private void showCancelReasonBottomSheet(OrderEntity order) {
        android.content.Context context = getContext();
        if (context == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);

        android.view.View view = android.view.LayoutInflater.from(context).inflate(R.layout.order_bottom_sheet_cancel_reason, null);
        bottomSheetDialog.setContentView(view);

        android.widget.RadioGroup rgReasons = view.findViewById(R.id.rgReasons);
        com.google.android.material.button.MaterialButton btnContinue = view.findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            int checkedId = rgReasons.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(context, "Vui lòng chọn lý do huỷ đơn", Toast.LENGTH_SHORT).show();
                return;
            }

            bottomSheetDialog.dismiss();
            viewModel.cancelOrder(order.getId());
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderCancelSuccessFragment.newInstance(order.getId(), order.getTotalAmount(), order.getPaymentMethod()))
                    .addToBackStack(null)
                    .commit();
        });

        bottomSheetDialog.show();
    }
}
