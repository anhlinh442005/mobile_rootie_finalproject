package com.veganbeauty.app.features.community.profile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityAffiliateOrdersFragment extends Fragment {

    private final List<OrderEntity> allAffiliateOrders = new ArrayList<>();
    private final Map<String, String> userEmailById = new HashMap<>();
    private AffiliateOrderAdapter adapter;
    private String currentFilter = "Tất cả";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_fragment_affiliate_orders, container, false);

        LinearLayout navOverview = view.findViewById(R.id.navOverview);
        if (navOverview != null) {
            navOverview.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        LinearLayout navOrders = view.findViewById(R.id.navOrders);
        if (navOrders != null) {
            navOrders.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateOrdersFragment()).commit());
        }

        LinearLayout navProducts = view.findViewById(R.id.navProducts);
        if (navProducts != null) {
            navProducts.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateProductsFragment()).commit());
        }

        LinearLayout navWithdraw = view.findViewById(R.id.navWithdraw);
        if (navWithdraw != null) {
            navWithdraw.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_container, new CommunityAffiliateWithdrawFragment()).commit());
        }

        setupFilterTabs(view);
        setupSearch(view);
        loadOrdersData(view);

        return view;
    }

    private void setupFilterTabs(View view) {
        TextView tabAll = view.findViewById(R.id.tabAll);
        TextView tabSuccess = view.findViewById(R.id.tabSuccess);
        TextView tabProcessing = view.findViewById(R.id.tabProcessing);
        TextView tabDelivering = view.findViewById(R.id.tabDelivering);

        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("Tất cả", tabAll);
        tabs.put("Thành công", tabSuccess);
        tabs.put("Đang xử lý", tabProcessing);
        tabs.put("Đang giao", tabDelivering);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            TextView tab = entry.getValue();
            if (tab == null) {
                continue;
            }
            tab.setOnClickListener(v -> {
                currentFilter = entry.getKey();
                updateTabStyles(tabs, currentFilter);
                applyFilters();
            });
        }
        updateTabStyles(tabs, currentFilter);
    }

    private void updateTabStyles(Map<String, TextView> tabs, String activeFilter) {
        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            TextView tab = entry.getValue();
            if (tab == null) {
                continue;
            }
            if (entry.getKey().equals(activeFilter)) {
                tab.setBackgroundResource(R.drawable.tab_active_bg);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                tab.setBackgroundResource(R.drawable.tab_inactive_bg);
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }
        }
    }

    private void setupSearch(View view) {
        EditText etSearch = view.findViewById(R.id.etSearch);
        if (etSearch == null) {
            return;
        }
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadOrdersData(View view) {
        try {
            loadUserEmails();

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###đ", symbols);

            List<OrderEntity> orders = new LocalJsonReader(requireContext()).getAllOrders();
            String currentUserId = ProfileSessionHelper.getEffectiveUserId(requireContext());

            allAffiliateOrders.clear();
            for (OrderEntity order : orders) {
                if (!"Đã hủy".equals(order.getStatus()) && order.getAffiliate() != null
                        && currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    allAffiliateOrders.add(order);
                }
            }

            RecyclerView rvOrders = view.findViewById(R.id.rvOrders);
            rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new AffiliateOrderAdapter(format, userEmailById);
            rvOrders.setAdapter(adapter);
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserEmails() {
        userEmailById.clear();
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(requireContext().getAssets().open("users.json"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray usersArr = new JSONArray(sb.toString());
            for (int i = 0; i < usersArr.length(); i++) {
                JSONObject obj = usersArr.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String userId = obj.optString("user_id", "");
                String email = obj.optString("email", "");
                if (!userId.isEmpty() && !email.isEmpty()) {
                    userEmailById.put(userId, email);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        if (adapter == null) {
            return;
        }
        List<OrderEntity> filtered = new ArrayList<>();
        for (OrderEntity order : allAffiliateOrders) {
            if (!matchesFilter(order, currentFilter)) {
                continue;
            }
            if (!matchesSearch(order)) {
                continue;
            }
            filtered.add(order);
        }
        adapter.submitList(filtered);
    }

    private boolean matchesFilter(OrderEntity order, String filter) {
        if ("Tất cả".equals(filter)) {
            return true;
        }
        return filter.equals(getDisplayStatus(order));
    }

    static String getDisplayStatus(OrderEntity order) {
        String status = order.getStatus() != null ? order.getStatus() : "";
        if ("Hoàn tất".equals(status) || "Đã duyệt".equals(status) || "Thành công".equals(status)) {
            return "Thành công";
        }
        if ("Đang giao".equals(status)) {
            return "Đang giao";
        }
        if (order.getAffiliate() != null && "confirmed".equals(order.getAffiliate().getCommissionStatus())
                && !"Đang xử lý".equals(status) && !"Chờ xử lý".equals(status) && !"Đang giao".equals(status)) {
            return "Thành công";
        }
        return "Đang xử lý";
    }

    private boolean matchesSearch(OrderEntity order) {
        if (searchQuery.isEmpty()) {
            return true;
        }
        String affiliateId = order.getAffiliate() != null && order.getAffiliate().getAffiliate_id() != null
                ? order.getAffiliate().getAffiliate_id().toLowerCase(Locale.ROOT)
                : "";
        String orderId = order.getId() != null ? order.getId().toLowerCase(Locale.ROOT) : "";
        String customer = "";
        String buyerEmail = userEmailById.get(order.getUserId());
        if (buyerEmail != null) {
            customer = maskEmail(buyerEmail);
        } else if (order.getShippingName() != null) {
            customer = order.getShippingName();
        }
        String shippingName = order.getShippingName() != null ? order.getShippingName().toLowerCase(Locale.ROOT) : "";
        customer = customer.toLowerCase(Locale.ROOT);
        return orderId.contains(searchQuery) || affiliateId.contains(searchQuery)
                || customer.contains(searchQuery) || shippingName.contains(searchQuery);
    }

    static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "Khách hàng";
        }
        if (!email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 3 ? local.charAt(0) + "***" : local.substring(0, Math.min(5, local.length())) + "***";
        return maskedLocal + "@" + parts[1];
    }

    public static class AffiliateOrderAdapter extends RecyclerView.Adapter<AffiliateOrderAdapter.ViewHolder> {

        private List<OrderEntity> items = new ArrayList<>();
        private final DecimalFormat format;
        private final Map<String, String> userEmailById;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView tvOrderId;
            public final TextView tvStatus;
            public final TextView tvOrderDate;
            public final TextView tvOrderValue;
            public final TextView tvTotalValue;
            public final TextView tvItemCount;
            public final TextView tvCustomer;
            public final TextView tvCommission;
            public final ShapeableImageView ivProduct1;
            public final ShapeableImageView ivProduct2;
            public final ShapeableImageView ivProduct3;
            public final TextView tvMoreImages;

            public ViewHolder(View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
                tvOrderValue = itemView.findViewById(R.id.tvOrderValue);
                tvTotalValue = itemView.findViewById(R.id.tvTotalValue);
                tvItemCount = itemView.findViewById(R.id.tvItemCount);
                tvCustomer = itemView.findViewById(R.id.tvCustomer);
                tvCommission = itemView.findViewById(R.id.tvCommission);
                ivProduct1 = itemView.findViewById(R.id.ivProduct1);
                ivProduct2 = itemView.findViewById(R.id.ivProduct2);
                ivProduct3 = itemView.findViewById(R.id.ivProduct3);
                tvMoreImages = itemView.findViewById(R.id.tvMoreImages);
            }
        }

        public AffiliateOrderAdapter(DecimalFormat format, Map<String, String> userEmailById) {
            this.format = format;
            this.userEmailById = userEmailById;
        }

        public void submitList(List<OrderEntity> newItems) {
            items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_affiliate_order_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrderEntity order = items.get(position);
            String displayStatus = getDisplayStatus(order);
            long orderValue = order.getTotalAmount();
            long commission = order.getAffiliate() != null ? order.getAffiliate().getCommissionAmount() : 0L;

            holder.tvOrderId.setText(order.getId());
            holder.tvTotalValue.setText(format.format(orderValue));
            holder.tvOrderValue.setText(format.format(orderValue));
            holder.tvCommission.setText(format.format(commission));

            String dateText = order.getOrderDate() != null ? order.getOrderDate() : "";
            if (order.getOrderTime() != null && !order.getOrderTime().isEmpty()) {
                dateText = dateText + " " + order.getOrderTime();
            }
            holder.tvOrderDate.setText(dateText);

            String buyerEmail = userEmailById.get(order.getUserId());
            if (buyerEmail != null && !buyerEmail.isEmpty()) {
                holder.tvCustomer.setText(maskEmail(buyerEmail));
            } else if (order.getShippingName() != null && !order.getShippingName().isEmpty()) {
                holder.tvCustomer.setText(order.getShippingName());
            } else {
                holder.tvCustomer.setText("Khách hàng");
            }

            bindStatus(holder.tvStatus, displayStatus);
            bindProductImages(holder, order);
        }

        private void bindStatus(TextView tvStatus, String displayStatus) {
            tvStatus.setText(displayStatus);
            tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            switch (displayStatus) {
                case "Thành công":
                    tvStatus.setTextColor(Color.parseColor("#56694E"));
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
        }

        private void bindProductImages(ViewHolder holder, OrderEntity order) {
            ShapeableImageView[] imageViews = {holder.ivProduct1, holder.ivProduct2, holder.ivProduct3};
            for (ShapeableImageView imageView : imageViews) {
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
            }
            holder.tvMoreImages.setVisibility(View.GONE);

            List<OrderEntity.OrderItem> orderItems = order.getItems();
            if (orderItems == null || orderItems.isEmpty()) {
                holder.ivProduct1.setVisibility(View.VISIBLE);
                holder.tvItemCount.setText("0 sản phẩm");
                return;
            }

            int totalQty = 0;
            for (OrderEntity.OrderItem item : orderItems) {
                totalQty += Math.max(item.getQuantity(), 1);
            }
            holder.tvItemCount.setText(totalQty + " sản phẩm");

            int visibleCount = Math.min(orderItems.size(), 3);
            for (int i = 0; i < visibleCount; i++) {
                OrderEntity.OrderItem item = orderItems.get(i);
                imageViews[i].setVisibility(View.VISIBLE);
                if (item.getProductImage() != null && !item.getProductImage().isEmpty()) {
                    com.bumptech.glide.Glide.with(imageViews[i].getContext())
                            .load(item.getProductImage())
                            .centerCrop()
                            .into(imageViews[i]);
                }
            }

            if (orderItems.size() > 3) {
                holder.tvMoreImages.setVisibility(View.VISIBLE);
                holder.tvMoreImages.setText("+" + (orderItems.size() - 3));
            }
        }
    }
}
