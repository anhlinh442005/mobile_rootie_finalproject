package com.veganbeauty.app.features.community.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class CommunityRevenueFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_revenue, container, false);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        ImageView ivNotification = view.findViewById(R.id.ivNotification);
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityNotificationFragment())
                    .addToBackStack(null)
                    .commit());
        }

        LinearLayout navOrders = view.findViewById(R.id.navOrders);
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateOrdersFragment())
                    .addToBackStack(null)
                    .commit());
        }

        LinearLayout navProducts = view.findViewById(R.id.navProducts);
        if (navProducts != null) {
            navProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateProductsFragment())
                    .addToBackStack(null)
                    .commit());
        }

        LinearLayout navWithdraw = view.findViewById(R.id.navWithdraw);
        if (navWithdraw != null) {
            navWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateWithdrawFragment())
                    .addToBackStack(null)
                    .commit());
        }

        TextView tvViewAllProducts = view.findViewById(R.id.tvViewAllProducts);
        if (tvViewAllProducts != null) {
            tvViewAllProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateProductsFragment())
                    .addToBackStack(null)
                    .commit());
        }

        TextView tvViewAllOrders = view.findViewById(R.id.tvViewAllOrders);
        if (tvViewAllOrders != null) {
            tvViewAllOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateOrdersFragment())
                    .addToBackStack(null)
                    .commit());
        }

        TextView tvViewAllWithdrawals = view.findViewById(R.id.tvViewAllWithdrawals);
        if (tvViewAllWithdrawals != null) {
            tvViewAllWithdrawals.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateWithdrawFragment())
                    .addToBackStack(null)
                    .commit());
        }

        TextView btnWithdraw = view.findViewById(R.id.btnWithdraw);
        if (btnWithdraw != null) {
            btnWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new CommunityAffiliateWithdrawFragment())
                    .addToBackStack(null)
                    .commit());
        }

        loadAffiliateData(view);

        return view;
    }

    private void loadAffiliateData(View view) {
        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###đ", symbols);

            LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
            List<OrderEntity> allOrders = jsonReader.getAllOrders();
            String currentUserId = ProfileSessionHelper.getEffectiveUserId(requireContext());

            long totalRevenue = 0L;
            long totalCommission = 0L;
            long availableBalance = 0L;
            long pendingBalance = 0L;
            int successfulOrdersCount = 0;
            int pendingOrdersCount = 0;
            Set<String> newCustomerIds = new HashSet<>();

            for (OrderEntity order : allOrders) {
                if (order.isAffiliate() && order.getAffiliate() != null && currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    totalRevenue += order.getTotalAmount();
                    long commission = order.getAffiliate().getCommissionAmount();
                    totalCommission += commission;

                    if (order.getUserId() != null) {
                        newCustomerIds.add(order.getUserId());
                    }

                    if ("confirmed".equals(order.getAffiliate().getCommissionStatus())) {
                        availableBalance += commission;
                        successfulOrdersCount++;
                    } else if ("pending".equals(order.getAffiliate().getCommissionStatus())) {
                        pendingBalance += commission;
                        pendingOrdersCount++;
                    }
                }
            }

            TextView tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
            if (tvTotalRevenue != null) tvTotalRevenue.setText(format.format(totalRevenue));

            TextView tvPendingCommission = view.findViewById(R.id.tvPendingCommission);
            if (tvPendingCommission != null) tvPendingCommission.setText(format.format(pendingBalance));

            TextView tvTotalOrders = view.findViewById(R.id.tvTotalOrders);
            if (tvTotalOrders != null) tvTotalOrders.setText(String.valueOf(successfulOrdersCount));

            TextView tvNewCustomers = view.findViewById(R.id.tvNewCustomers);
            if (tvNewCustomers != null) tvNewCustomers.setText(String.valueOf(newCustomerIds.size()));

            TextView tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
            if (tvAvailableBalance != null) tvAvailableBalance.setText(format.format(availableBalance));

            TextView tvWithdrawBalance = view.findViewById(R.id.tvWithdrawBalance);
            if (tvWithdrawBalance != null) tvWithdrawBalance.setText(format.format(availableBalance));

            List<UserEntity> allUsers = jsonReader.getUsers();
            UserEntity currentUser = null;
            for (UserEntity u : allUsers) {
                if (currentUserId.equals(u.getUser_id())) {
                    currentUser = u;
                    break;
                }
            }
            if (currentUser != null) {
                TextView tvUserName = view.findViewById(R.id.tvUserName);
                if (tvUserName != null) tvUserName.setText("Xin chào, " + currentUser.getFull_name() + " \ud83c\udf3f");

                ImageView ivAvatar = view.findViewById(R.id.ivAvatar);
                if (ivAvatar != null) {
                    String avatarUrl = currentUser != null ? currentUser.getAvatar() : null;
                    com.bumptech.glide.Glide.with(ivAvatar.getContext())
                            .load(avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : R.drawable.img_avatar)
                            .circleCrop()
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .into(ivAvatar);
                }
            }

            LinearLayout llProductsSoldContainer = view.findViewById(R.id.llProductsSoldContainer);
            if (llProductsSoldContainer != null) llProductsSoldContainer.removeAllViews();

            LinearLayout llOrdersContainer = view.findViewById(R.id.llOrdersContainer);
            if (llOrdersContainer != null) llOrdersContainer.removeAllViews();

            class ProductStats {
                int count = 0;
                long commission = 0L;
                String image = "";
                String name = "";
            }

            Map<String, ProductStats> productMap = new HashMap<>();

            for (OrderEntity order : allOrders) {
                if (order.isAffiliate() && order.getAffiliate() != null && currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    if ("Đã hủy".equals(order.getStatus())) continue;

                    String affiliateId = order.getAffiliate().getAffiliate_id() != null ? order.getAffiliate().getAffiliate_id() : order.getId();
                    String orderDate = order.getOrderDate();
                    long orderValue = order.getTotalAmount();
                    long commission = order.getAffiliate().getCommissionAmount();

                    View rowView = LayoutInflater.from(requireContext()).inflate(R.layout.com_item_revenue_order, llOrdersContainer, false);

                    TextView tvOrderId = rowView.findViewById(R.id.tvOrderId);
                    tvOrderId.setText(affiliateId);

                    TextView tvOrderDate = rowView.findViewById(R.id.tvOrderDate);
                    tvOrderDate.setText(orderDate.split(" ")[0]);

                    TextView tvOrderValue = rowView.findViewById(R.id.tvOrderValue);
                    tvOrderValue.setText(format.format(orderValue));

                    TextView tvStatus = rowView.findViewById(R.id.tvStatus);
                    String displayStatus = CommunityAffiliateOrdersFragment.getDisplayStatus(order);
                    tvStatus.setText(displayStatus);
                    tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    switch (displayStatus) {
                        case "Thành công":
                            tvStatus.setTextColor(Color.parseColor("#6E846A"));
                            tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAF1E7")));
                            break;
                        case "Đang giao":
                            tvStatus.setTextColor(Color.parseColor("#1976D2"));
                            tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                            break;
                        default:
                            tvStatus.setTextColor(Color.parseColor("#FF9800"));
                            tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                            break;
                    }

                    ImageView ivProductImage = rowView.findViewById(R.id.ivProductImage);
                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        OrderItem firstItem = order.getItems().get(0);
                        if (firstItem.getProductImage() != null && !firstItem.getProductImage().isEmpty()) {
                            com.bumptech.glide.Glide.with(ivProductImage.getContext())
                                    .load(firstItem.getProductImage())
                                    .centerCrop()
                                    .into(ivProductImage);
                        }
                    }

                    if (llOrdersContainer != null) {
                        llOrdersContainer.addView(rowView);
                    }

                    if (order.getItems() != null) {
                        for (OrderItem item : order.getItems()) {
                            String productId = item.getProductId();
                            String name = item.getProductName();
                            String image = item.getProductImage();

                            long itemComm = !order.getItems().isEmpty() ? commission / order.getItems().size() : 0L;

                            ProductStats stats = productMap.get(productId);
                            if (stats == null) {
                                stats = new ProductStats();
                                stats.image = image;
                                stats.name = name;
                                productMap.put(productId, stats);
                            }
                            stats.count += item.getQuantity();
                            stats.commission += itemComm;
                        }
                    }
                }
            }

            for (Map.Entry<String, ProductStats> entry : productMap.entrySet()) {
                ProductStats stats = entry.getValue();
                View prodView = LayoutInflater.from(requireContext()).inflate(R.layout.com_item_revenue_product, llProductsSoldContainer, false);
                
                TextView tvProductName = prodView.findViewById(R.id.tvProductName);
                tvProductName.setText(stats.name != null ? stats.name : "Sản phẩm");

                TextView tvProductSold = prodView.findViewById(R.id.tvProductSold);
                tvProductSold.setText("Đã bán: " + stats.count);

                TextView tvProductCommission = prodView.findViewById(R.id.tvProductCommission);
                tvProductCommission.setText("Hoa hồng: " + format.format(stats.commission));

                ImageView ivProduct = prodView.findViewById(R.id.ivProduct);
                if (stats.image != null && !stats.image.isEmpty()) {
                    com.bumptech.glide.Glide.with(ivProduct.getContext())
                            .load(stats.image)
                            .centerCrop()
                            .into(ivProduct);
                }

                if (llProductsSoldContainer != null) {
                    llProductsSoldContainer.addView(prodView);
                }
            }

            JSONArray affiliateWithdrawals = new JSONArray();
            LinearLayout llWithdrawalsContainer = view.findViewById(R.id.llWithdrawalsContainer);
            if (llWithdrawalsContainer != null) llWithdrawalsContainer.removeAllViews();

            for (int i = 0; i < affiliateWithdrawals.length(); i++) {
                JSONObject wd = affiliateWithdrawals.optJSONObject(i);
                if (wd == null) continue;

                String wdDate = wd.optString("date");
                long wdAmount = wd.optLong("amount");
                String wdStatus = wd.optString("status");

                View wdView = LayoutInflater.from(requireContext()).inflate(R.layout.com_item_revenue_withdrawal, llWithdrawalsContainer, false);

                TextView tvWithdrawDate = wdView.findViewById(R.id.tvWithdrawDate);
                tvWithdrawDate.setText(wdDate);

                TextView tvWithdrawAmount = wdView.findViewById(R.id.tvWithdrawAmount);
                tvWithdrawAmount.setText(format.format(wdAmount));

                TextView tvStatus = wdView.findViewById(R.id.tvWithdrawStatus);
                if ("Đã chuyển".equals(wdStatus)) {
                    tvStatus.setText("Đã chuyển");
                    tvStatus.setTextColor(Color.parseColor("#6E846A"));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAF1E7")));
                } else {
                    tvStatus.setText(wdStatus);
                    tvStatus.setTextColor(Color.parseColor("#FF9800"));
                    tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                }

                if (llWithdrawalsContainer != null) {
                    llWithdrawalsContainer.addView(wdView);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
