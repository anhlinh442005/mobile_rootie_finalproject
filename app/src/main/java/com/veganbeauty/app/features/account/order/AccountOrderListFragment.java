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
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.OrderItemEntity;
import com.veganbeauty.app.features.ai.SkinAiChatFragment;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderListFragmentBinding;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

import java.util.ArrayList;
import java.util.HashMap;
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
                return null;
            },
            order -> {
                showCancelConfirmationDialog(order);
                return null;
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
                return null;
            },
            order -> {
                ArrayList<CartItemEntity> checkoutItems = new ArrayList<>();
                for (OrderItemEntity item : order.getItems()) {
                    checkoutItems.add(new CartItemEntity(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductImage(),
                        item.getPrice(),
                        item.getQuantity(),
                        true
                    ));
                }
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, ShopCheckoutFragment.newInstance(checkoutItems))
                    .addToBackStack(null)
                    .commit();
                return null;
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderTrackingFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
                return null;
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new SkinAiChatFragment())
                    .addToBackStack(null)
                    .commit();
                return null;
            },
            order -> {
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderReviewFragment.newInstance(order.getId()))
                    .addToBackStack(null)
                    .commit();
                return null;
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
                getActivity().onBackPressedDispatcher().onBackPressed();
            }
        });

        View.OnClickListener navigateToNotification = v -> {
            getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit();
        };
        _binding.layoutNotification.setOnClickListener(navigateToNotification);
        _binding.btnNotification.setOnClickListener(navigateToNotification);

        _binding.rvOrders.setAdapter(orderAdapter);

        _binding.tabAll.setOnClickListener(v -> viewModel.setFilter("Tất cả"));
        _binding.tabPending.setOnClickListener(v -> viewModel.setFilter("Chờ xử lý"));
        _binding.tabProcessing.setOnClickListener(v -> viewModel.setFilter("Đang xử lý"));
        _binding.tabDelivering.setOnClickListener(v -> viewModel.setFilter("Đang giao"));
        _binding.tabSuccess.setOnClickListener(v -> viewModel.setFilter("Hoàn tất"));
        _binding.tabCancelled.setOnClickListener(v -> viewModel.setFilter("Đã hủy"));
    }

    @Override
    protected void observeViewModel() {
        viewModel.getFilteredOrders().observe(getViewLifecycleOwner(), orders -> {
            orderAdapter.submitList(orders);
        });

        viewModel.getOrderStats().observe(getViewLifecycleOwner(), stats -> {
            _binding.tvOrderStats.setText(stats);
        });

        viewModel.getSelectedStatus().observe(getViewLifecycleOwner(), this::updateTabStyles);

        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            NotificationRepository.getInstance(requireContext()).getUnreadCount().collect(new FlowCollector<Integer>() {
                @Nullable
                @Override
                public Object emit(Integer count, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    if (_binding != null) {
                        _binding.viewNotificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    }
                    return kotlin.Unit.INSTANCE;
                }
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });
    }

    private void updateTabStyles(String activeStatus) {
        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("Tất cả", _binding.tabAll);
        tabs.put("Chờ xử lý", _binding.tabPending);
        tabs.put("Đang xử lý", _binding.tabProcessing);
        tabs.put("Đang giao", _binding.tabDelivering);
        tabs.put("Hoàn tất", _binding.tabSuccess);
        tabs.put("Đã hủy", _binding.tabCancelled);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            String status = entry.getKey();
            TextView textView = entry.getValue();
            if (status.equalsIgnoreCase(activeStatus)) {
                textView.setBackgroundResource(R.drawable.tab_active_bg);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }
        }
    }

    private void showCancelConfirmationDialog(OrderEntity order) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng " + order.getId() + " không?")
            .setPositiveButton("Xác nhận", (dialog, which) -> {
                viewModel.cancelOrder(order.getId());
                Toast.makeText(requireContext(), "Đã hủy đơn hàng " + order.getId() + " thành công!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Quay lại", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
