package com.veganbeauty.app.features.account.order;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderDetailFragmentBinding;
import com.veganbeauty.app.databinding.AccountOrderDetailProductItemBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.ai.SkinAiChatFragment;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.FlowLiveDataConversions;

public class AccountOrderDetailFragment extends RootieFragment {

    private AccountOrderDetailFragmentBinding binding;
    private OrderDetailViewModel viewModel;

    private static final String ARG_ORDER_ID = "arg_order_id";
    private static final String ARG_FROM_SUCCESS = "arg_from_success";

    public static AccountOrderDetailFragment newInstance(String orderId) {
        return newInstance(orderId, false);
    }

    public static AccountOrderDetailFragment newInstance(String orderId, boolean fromSuccess) {
        AccountOrderDetailFragment fragment = new AccountOrderDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        args.putBoolean(ARG_FROM_SUCCESS, fromSuccess);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountOrderDetailFragmentBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        OrderRepository repository = new OrderRepository(
                db.orderDao(),
                db.rewardPointDao(),
                db.userGiftDao(),
                new LocalJsonReader(requireContext())
        );
        String orderId = getArguments() != null ? getArguments().getString(ARG_ORDER_ID) : "";

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new OrderDetailViewModel(repository, orderId);
            }
        }).get(OrderDetailViewModel.class);
    }

    @Override
    protected void setupUI(@NonNull View view) {
        boolean fromSuccess = getArguments() != null && getArguments().getBoolean(ARG_FROM_SUCCESS, false);

        binding.btnBack.setOnClickListener(v -> {
            if (fromSuccess) {
                navigateToShopHome();
            } else {
                if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (fromSuccess) {
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    navigateToShopHome();
                }
            };
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
            }
        }

        View.OnClickListener navigateToNotification = v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit();

        binding.layoutNotification.setOnClickListener(navigateToNotification);
        binding.btnNotification.setOnClickListener(navigateToNotification);
    }

    private void navigateToShopHome() {
        getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.main_container, new ShopHomeFragment())
                .commit();
    }

    @Override
    protected void observeViewModel() {
        viewModel.order.observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                bindOrderDetails(order);
            }
        });

        FlowLiveDataConversions.asLiveData(
                NotificationRepository.getInstance(requireContext()).getUnreadCount()
        ).observe(getViewLifecycleOwner(), count -> {
            if (binding == null) return;
            binding.viewNotificationBadge.setVisibility(count != null && count > 0 ? View.VISIBLE : View.GONE);
        });
    }

    private void bindOrderDetails(OrderEntity order) {
        Context context = getContext();
        if (context == null) return;

        binding.tvOrderCode.setText(order.getId());
        binding.tvOrderDate.setText(order.getOrderDate() + " - " + order.getOrderTime());

        int badgeBgRes;
        int badgeTextRes;
        switch (order.getStatus()) {
            case "Chờ xử lý":
                badgeBgRes = R.color.status_pending_bg;
                badgeTextRes = R.color.status_pending_text;
                break;
            case "Đang xử lý":
                badgeBgRes = R.color.status_processing_bg;
                badgeTextRes = R.color.status_processing_text;
                break;
            case "Đang giao":
                badgeBgRes = R.color.status_delivering_bg;
                badgeTextRes = R.color.status_delivering_text;
                break;
            case "Hoàn tất":
                badgeBgRes = R.color.status_success_bg;
                badgeTextRes = R.color.status_success_text;
                break;
            case "Đã hủy":
                badgeBgRes = R.color.status_cancelled_bg;
                badgeTextRes = R.color.status_cancelled_text;
                break;
            default:
                badgeBgRes = R.color.status_pending_bg;
                badgeTextRes = R.color.status_pending_text;
                break;
        }

        if (binding.tvStatusBadge.getBackground() != null) {
            binding.tvStatusBadge.getBackground().mutate().setTint(ContextCompat.getColor(context, badgeBgRes));
        }
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, badgeTextRes));
        binding.tvStatusBadge.setText(order.getStatus().toUpperCase());

        setupStatusBanner(order, badgeBgRes, badgeTextRes);

        binding.layoutProductsContainer.removeAllViews();
        long subtotal = 0L;
        List<OrderEntity.OrderItem> items = order.getItems();
        if (items != null) {
            for (OrderEntity.OrderItem item : items) {
                        subtotal += item.getPrice() * item.getQuantity();
                        AccountOrderDetailProductItemBinding productBinding = AccountOrderDetailProductItemBinding.inflate(
                                LayoutInflater.from(context),
                                binding.layoutProductsContainer,
                                false
                        );

                        productBinding.tvProductName.setText(item.getProductName());
                        productBinding.tvProductQuantity.setText("x" + item.getQuantity());
                        productBinding.tvProductPrice.setText(formatCurrency(item.getPrice()));

                        String attribute;
                        if (item.getProductName().contains("50ml")) {
                            attribute = "Dung tích: 50ml";
                        } else if (item.getProductName().contains("30ml")) {
                            attribute = "Dung tích: 30ml";
                        } else if (item.getProductName().contains("70ml")) {
                            attribute = "Dung tích: 70ml";
                        } else if (item.getProductName().contains("140ml")) {
                            attribute = "Dung tích: 140ml";
                        } else if (item.getProductName().contains("100ml")) {
                            attribute = "Dung tích: 100ml";
                        } else if ("product_hair_grapefruit".equals(item.getProductId())) {
                            attribute = "Dung tích: 140ml";
                        } else if ("product_rose_cream".equals(item.getProductId())) {
                            attribute = "Dung tích: 50ml";
                        } else if (item.getProductName().contains("Combo")) {
                            attribute = "Phân loại: Bộ sản phẩm";
                        } else {
                            attribute = "Dung tích: 100ml";
                        }
                        productBinding.tvProductAttribute.setText(attribute);

                        com.bumptech.glide.Glide.with(productBinding.ivProductImage.getContext()).load(item.getProductImage()).placeholder(android.R.color.darker_gray).error(android.R.color.darker_gray).into(productBinding.ivProductImage);

                        binding.layoutProductsContainer.addView(productBinding.getRoot());
            }
        }

        binding.tvAddressName.setText(order.getShippingName());
        binding.tvAddressPhone.setText(order.getShippingPhone());
        binding.tvAddressFull.setText(order.getShippingAddress());

        binding.tvInvoiceSubtotalValue.setText(formatCurrency(subtotal));
        binding.tvInvoiceShippingValue.setText(formatCurrency(order.getShippingCost()));
        binding.tvInvoiceVoucherValue.setText("- " + formatCurrency(order.getVoucherDiscount()));
        binding.tvInvoiceTotalValue.setText(formatCurrency(order.getTotalAmount()));
        binding.tvInvoicePayment.setText(order.getPaymentMethod());

        setupBottomActions(order);
    }

    private void setupStatusBanner(OrderEntity order, int bgResId, int textResId) {
        Context context = getContext();
        if (context == null) return;

        switch (order.getStatus()) {
            case "Chờ xử lý":
                binding.tvBannerStatus.setText("● Chờ xử lý");
                binding.tvBannerDesc.setText("Đơn hàng của bạn sẽ được nhân viên xác nhận và tiến hành đóng gói đóng gói");
                binding.btnBannerSubAction.setVisibility(View.GONE);
                break;
            case "Đang xử lý":
                binding.tvBannerStatus.setText("● Đang xử lý");
                binding.tvBannerDesc.setText("Nhân viên đang tiến hành đóng gói và sẽ giao cho đơn vị vận chuyển");
                binding.btnBannerSubAction.setVisibility(View.GONE);
                break;
            case "Đang giao":
                binding.tvBannerStatus.setText("● Đang giao hàng");
                String deliveryTime = order.getExpectedDeliveryTime() != null ? order.getExpectedDeliveryTime() : "Hôm nay 18:00 - 20:00";
                binding.tvBannerDesc.setText("Dự kiến giao: " + deliveryTime);
                binding.btnBannerSubAction.setVisibility(View.VISIBLE);

                binding.btnBannerSubAction.setBackgroundTintList(ContextCompat.getColorStateList(context, bgResId));
                binding.tvBannerSubActionText.setText("Theo dõi đơn");
                binding.tvBannerSubActionText.setTextColor(ContextCompat.getColor(context, textResId));

                binding.ivBannerSubActionIcon.setVisibility(View.VISIBLE);
                binding.ivBannerSubActionIcon.setImageResource(R.drawable.ic_truck);
                binding.ivBannerSubActionIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, textResId)));

                binding.btnBannerSubAction.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, AccountOrderTrackingFragment.newInstance(order.getId()))
                        .addToBackStack(null)
                        .commit());
                break;
            case "Hoàn tất":
                binding.tvBannerStatus.setText("● Hoàn tất");
                binding.tvBannerDesc.setText("Đơn hàng đã giao thành công");
                binding.btnBannerSubAction.setVisibility(View.VISIBLE);

                binding.btnBannerSubAction.setBackgroundTintList(ContextCompat.getColorStateList(context, bgResId));
                binding.tvBannerSubActionText.setText(order.isHasReview() ? "Xem đánh giá" : "Đánh giá đơn hàng");
                binding.tvBannerSubActionText.setTextColor(ContextCompat.getColor(context, textResId));

                binding.ivBannerSubActionIcon.setVisibility(View.VISIBLE);
                binding.ivBannerSubActionIcon.setImageResource(R.drawable.ic_logo_ol);
                binding.ivBannerSubActionIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, textResId)));

                binding.btnBannerSubAction.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, AccountOrderReviewFragment.newInstance(order.getId()))
                        .addToBackStack(null)
                        .commit());
                break;
            case "Đã hủy":
                binding.tvBannerStatus.setText("● Đã hủy");
                binding.tvBannerDesc.setText("Đơn hàng của bạn đã hủy");
                binding.btnBannerSubAction.setVisibility(View.GONE);
                break;
            default:
                binding.tvBannerStatus.setText("● " + order.getStatus());
                binding.tvBannerDesc.setText("");
                binding.btnBannerSubAction.setVisibility(View.GONE);
                break;
        }
    }

    private void setupBottomActions(OrderEntity order) {
        Context context = getContext();
        if (context == null) return;

        switch (order.getStatus()) {
            case "Chờ xử lý":
            case "Đang xử lý":
                binding.btnActionLeft.setVisibility(View.VISIBLE);
                binding.btnActionLeft.setText("Liên hệ hỗ trợ");
                binding.btnActionLeft.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinAiChatFragment())
                        .addToBackStack(null)
                        .commit());

                binding.btnActionRight.setVisibility(View.VISIBLE);
                binding.btnActionRight.setText("Hủy đơn");
                binding.btnActionRight.setOnClickListener(v -> showCancelConfirmationDialog(order));
                break;
            case "Đang giao":
                binding.btnActionLeft.setVisibility(View.VISIBLE);
                binding.btnActionLeft.setText("Liên hệ hỗ trợ");
                binding.btnActionLeft.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinAiChatFragment())
                        .addToBackStack(null)
                        .commit());

                binding.btnActionRight.setVisibility(View.VISIBLE);
                binding.btnActionRight.setText("Đã nhận hàng");
                binding.btnActionRight.setOnClickListener(v -> showConfirmReceivedDialog(order));
                break;
            case "Hoàn tất":
                binding.btnActionLeft.setVisibility(View.VISIBLE);
                binding.btnActionLeft.setText("Trả hàng/Hoàn tiền");
                binding.btnActionLeft.setOnClickListener(v -> Toast.makeText(context, "Gửi yêu cầu Trả hàng/Hoàn tiền cho đơn " + order.getId() + "...", Toast.LENGTH_SHORT).show());

                binding.btnActionRight.setVisibility(View.VISIBLE);
                binding.btnActionRight.setText("Mua lại");
                binding.btnActionRight.setOnClickListener(v -> {
                    ArrayList<CartItemEntity> checkoutItems = new ArrayList<>();
                    List<OrderEntity.OrderItem> items = order.getItems();
                    if (items != null) {
                        try {
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, ShopCheckoutFragment.newInstance(checkoutItems, "", 0L))
                            .addToBackStack(null)
                            .commit();
                });
                break;
            case "Đã hủy":
                binding.btnActionLeft.setVisibility(View.VISIBLE);
                binding.btnActionLeft.setText("Liên hệ hỗ trợ");
                binding.btnActionLeft.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinAiChatFragment())
                        .addToBackStack(null)
                        .commit());

                binding.btnActionRight.setVisibility(View.VISIBLE);
                binding.btnActionRight.setText("Mua lại");
                binding.btnActionRight.setOnClickListener(v -> {
                    ArrayList<CartItemEntity> checkoutItems = new ArrayList<>();
                    List<OrderEntity.OrderItem> items = order.getItems();
                    if (items != null) {
                        try {
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, ShopCheckoutFragment.newInstance(checkoutItems, "", 0L))
                            .addToBackStack(null)
                            .commit();
                });
                break;
            default:
                binding.btnActionLeft.setVisibility(View.GONE);
                binding.btnActionRight.setVisibility(View.GONE);
                break;
        }
    }

    private void showCancelConfirmationDialog(OrderEntity order) {
        Context context = getContext();
        if (context == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng " + order.getId() + " không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    viewModel.cancelOrder();
                    Toast.makeText(context, "Đã hủy đơn hàng " + order.getId() + " thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void showConfirmReceivedDialog(OrderEntity order) {
        Context context = getContext();
        if (context == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Đã nhận được hàng")
                .setMessage("Bạn xác nhận đã nhận đầy đủ và nguyên vẹn các sản phẩm trong đơn hàng " + order.getId() + "?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    viewModel.confirmReceived();
                    Toast.makeText(context, "Cảm ơn bạn đã mua sắm tại Rootie!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, AccountOrderReviewFragment.newInstance(order.getId()))
                            .addToBackStack(null)
                            .commit();
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private String formatCurrency(long amount) {
        return String.format("%,dđ", amount).replace(',', '.');
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
