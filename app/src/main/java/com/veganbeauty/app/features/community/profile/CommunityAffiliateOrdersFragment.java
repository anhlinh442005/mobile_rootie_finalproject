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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityAffiliateOrdersFragment extends Fragment {

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

        loadOrdersData(view);

        return view;
    }

    private void loadOrdersData(View view) {
        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
            symbols.setGroupingSeparator('.');
            DecimalFormat format = new DecimalFormat("#,###đ", symbols);

            List<ProductEntity> allProducts = new LocalJsonReader(requireContext()).getAllProducts();
            Map<String, String> productImagesMap = new HashMap<>();
            for (ProductEntity p : allProducts) {
                productImagesMap.put(p.getId(), p.getMainImage());
            }

            List<OrderEntity> allOrders = new LocalJsonReader(requireContext()).getAllOrders();

            RecyclerView rvOrders = view.findViewById(R.id.rvOrders);
            rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

            String currentUserId = "test_001";
            List<OrderEntity> orderList = new ArrayList<>();
            for (OrderEntity order : allOrders) {
                if (!"Đã hủy".equals(order.getStatus()) && order.getAffiliate() != null &&
                        currentUserId.equals(order.getAffiliate().getReferrerUserId())) {
                    orderList.add(order);
                }
            }

            rvOrders.setAdapter(new AffiliateOrderAdapter(orderList, productImagesMap, format));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class AffiliateOrderAdapter extends RecyclerView.Adapter<AffiliateOrderAdapter.ViewHolder> {

        private final List<OrderEntity> items;
        private final Map<String, String> productImagesMap;
        private final DecimalFormat format;

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView tvOrderId;
            public final TextView tvStatus;
            public final ImageView ivProduct1;
            public final ImageView ivProduct2;
            public final ImageView ivProduct3;
            public final TextView tvMoreImages;
            public final TextView tvTotalValue;
            public final TextView tvItemCount;
            public final TextView tvCustomer;
            public final TextView tvOrderDate;
            public final TextView tvOrderValue;
            public final TextView tvCommission;

            public ViewHolder(View itemView) {
                super(itemView);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                ivProduct1 = itemView.findViewById(R.id.ivProduct1);
                ivProduct2 = itemView.findViewById(R.id.ivProduct2);
                ivProduct3 = itemView.findViewById(R.id.ivProduct3);
                tvMoreImages = itemView.findViewById(R.id.tvMoreImages);
                tvTotalValue = itemView.findViewById(R.id.tvTotalValue);
                tvItemCount = itemView.findViewById(R.id.tvItemCount);
                tvCustomer = itemView.findViewById(R.id.tvCustomer);
                tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
                tvOrderValue = itemView.findViewById(R.id.tvOrderValue);
                tvCommission = itemView.findViewById(R.id.tvCommission);
            }
        }

        public AffiliateOrderAdapter(List<OrderEntity> items, Map<String, String> productImagesMap, DecimalFormat format) {
            this.items = items;
            this.productImagesMap = productImagesMap;
            this.format = format;
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
            String orderId = order.getId();
            String orderDate = order.getOrderDate();
            String customer = order.getShippingName();
            long orderValue = order.getTotalAmount();
            long commission = order.getAffiliate() != null ? order.getAffiliate().getCommissionAmount() : 0L;
            String commissionStatus = order.getAffiliate() != null && order.getAffiliate().getCommissionStatus() != null ? order.getAffiliate().getCommissionStatus() : "";
            String status = order.getStatus();
            String affiliateId = order.getAffiliate() != null && order.getAffiliate().getAffiliate_id() != null ? order.getAffiliate().getAffiliate_id() : orderId;

            holder.tvOrderId.setText(affiliateId);

            if ("Hoàn tất".equals(status) || "Đã duyệt".equals(status) || "Thành công".equals(status)) {
                holder.tvStatus.setText("Thành công");
                holder.tvStatus.setTextColor(Color.parseColor("#56694E"));
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EAF1E7")));
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.tvStatus.setCompoundDrawablePadding(0);
            } else {
                holder.tvStatus.setText("Đang xử lý");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800"));
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.tvStatus.setCompoundDrawablePadding(0);
            }

            int itemCount = 0;
            holder.ivProduct1.setVisibility(View.GONE);
            holder.ivProduct2.setVisibility(View.GONE);
            holder.ivProduct3.setVisibility(View.GONE);
            holder.tvMoreImages.setVisibility(View.GONE);

            for (int i = 0; i < order.getItems().size(); i++) {
                OrderEntity.OrderItem item = order.getItems().get(i);
                String pImg = productImagesMap.get(item.getProductId());
                if (pImg == null || pImg.isEmpty()) {
                    pImg = item.getProductImage() != null ? item.getProductImage() : "";
                }
                itemCount += item.getQuantity();

                if (i == 0) {
                    holder.ivProduct1.setVisibility(View.VISIBLE);
                    if (!pImg.isEmpty()) {
                        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                                .data(pImg)
                                .target(holder.ivProduct1)
                                .build();
                        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                    }
                } else if (i == 1) {
                    holder.ivProduct2.setVisibility(View.VISIBLE);
                    if (!pImg.isEmpty()) {
                        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                                .data(pImg)
                                .target(holder.ivProduct2)
                                .build();
                        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                    }
                } else if (i == 2) {
                    holder.ivProduct3.setVisibility(View.VISIBLE);
                    if (!pImg.isEmpty()) {
                        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                                .data(pImg)
                                .target(holder.ivProduct3)
                                .build();
                        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                    }
                }
            }
            if (order.getItems().size() > 3) {
                holder.tvMoreImages.setVisibility(View.VISIBLE);
                holder.tvMoreImages.setText("+" + (order.getItems().size() - 3));
            }

            holder.tvItemCount.setText(itemCount + " sản phẩm");
            holder.tvTotalValue.setText(format.format(orderValue));
            holder.tvCustomer.setText(customer);
            holder.tvOrderDate.setText(orderDate.contains(":") ? orderDate : orderDate + " 10:30");
            holder.tvOrderValue.setText(format.format(orderValue));

            String commStatusText = "confirmed".equals(commissionStatus) ? "(Đã duyệt)" : ("pending".equals(commissionStatus) ? "(Chờ duyệt)" : "");
            holder.tvCommission.setText(format.format(commission) + " " + commStatusText);
        }
    }
}
