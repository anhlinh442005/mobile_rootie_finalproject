package com.veganbeauty.app.features.account.order;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.databinding.AccountOrderItemBinding;

import java.util.List;

public class OrderListAdapter extends ListAdapter<OrderEntity, OrderListAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onClick(OrderEntity order);
    }

    private final OnOrderClickListener onItemClick;
    private final OnOrderClickListener onCancelClick;
    private final OnOrderClickListener onDetailClick;
    private final OnOrderClickListener onReorderClick;
    private final OnOrderClickListener onTrackClick;
    private final OnOrderClickListener onContactClick;
    private final OnOrderClickListener onReviewClick;

    public OrderListAdapter(
            OnOrderClickListener onItemClick,
            OnOrderClickListener onCancelClick,
            OnOrderClickListener onDetailClick,
            OnOrderClickListener onReorderClick,
            OnOrderClickListener onTrackClick,
            OnOrderClickListener onContactClick,
            OnOrderClickListener onReviewClick
    ) {
        super(new OrderDiffCallback());
        this.onItemClick = onItemClick != null ? onItemClick : order -> {};
        this.onCancelClick = onCancelClick != null ? onCancelClick : order -> {};
        this.onDetailClick = onDetailClick != null ? onDetailClick : order -> {};
        this.onReorderClick = onReorderClick != null ? onReorderClick : order -> {};
        this.onTrackClick = onTrackClick != null ? onTrackClick : order -> {};
        this.onContactClick = onContactClick != null ? onContactClick : order -> {};
        this.onReviewClick = onReviewClick != null ? onReviewClick : order -> {};
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AccountOrderItemBinding binding = AccountOrderItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(
                getItem(position),
                onItemClick,
                onCancelClick,
                onDetailClick,
                onReorderClick,
                onTrackClick,
                onContactClick,
                onReviewClick
        );
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {

        private final AccountOrderItemBinding binding;
        private boolean isExpanded = false;

        public OrderViewHolder(@NonNull AccountOrderItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                OrderEntity order,
                OnOrderClickListener onItemClick,
                OnOrderClickListener onCancelClick,
                OnOrderClickListener onDetailClick,
                OnOrderClickListener onReorderClick,
                OnOrderClickListener onTrackClick,
                OnOrderClickListener onContactClick,
                OnOrderClickListener onReviewClick
        ) {
            Context context = binding.getRoot().getContext();

            // Khôi phục feedback đã lưu theo đơn — nút hiện "Xem đánh giá" đúng
            com.veganbeauty.app.data.local.OrderReviewLocalStore.applyTo(context, order);

            binding.getRoot().setOnClickListener(v -> onItemClick.onClick(order));

            binding.tvOrderDateTime.setText(order.getOrderDate() + " • " + order.getOrderTime());
            binding.tvOrderId.setText(order.getId());

            binding.tvOrderStatus.setText(order.getStatus());
            int bgResId;
            int textResId;

            switch (order.getStatus()) {
                case "Chờ xác nhận":
                    bgResId = R.color.status_pending_bg;
                    textResId = R.color.status_pending_text;
                    break;
                case "Đang xử lý":
                    bgResId = R.color.status_processing_bg;
                    textResId = R.color.status_processing_text;
                    break;
                case "Đang giao":
                    bgResId = R.color.status_delivering_bg;
                    textResId = R.color.status_delivering_text;
                    break;
                case "Hoàn tất":
                    bgResId = R.color.status_success_bg;
                    textResId = R.color.status_success_text;
                    break;
                case "Đã hủy":
                    bgResId = R.color.status_cancelled_bg;
                    textResId = R.color.status_cancelled_text;
                    break;
                default:
                    bgResId = R.color.status_pending_bg;
                    textResId = R.color.status_pending_text;
                    break;
            }

            binding.tvOrderStatus.getBackground().mutate().setTint(ContextCompat.getColor(context, bgResId));
            binding.tvOrderStatus.setTextColor(ContextCompat.getColor(context, textResId));

            binding.tvProductCountLabel.setText("Sản phẩm (" + order.getItems().size() + ")");

            isExpanded = false;

            updateProductsVisibility(order, context);

            binding.llToggleProducts.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                updateProductsVisibility(order, context);
            });

            String formattedAmount = String.format("%,dđ", order.getTotalAmount()).replace(',', '.');
            binding.tvTotalAmount.setText(formattedAmount);

            setupActionButtons(
                    order,
                    onCancelClick,
                    onDetailClick,
                    onReorderClick,
                    onTrackClick,
                    onContactClick,
                    onReviewClick
            );
        }

        private void updateProductsVisibility(OrderEntity order, Context context) {
            binding.llProductsContainer.removeAllViews();
            
            List<OrderItem> itemsToShow;
            if (isExpanded) {
                itemsToShow = order.getItems();
            } else {
                itemsToShow = order.getItems().size() > 2 ? order.getItems().subList(0, 2) : order.getItems();
            }

            for (OrderItem item : itemsToShow) {
                View rowView = LayoutInflater.from(context).inflate(R.layout.item_account_order_product_row, binding.llProductsContainer, false);

                TextView tvName = rowView.findViewById(R.id.tvProductName);
                TextView tvQuantity = rowView.findViewById(R.id.tvProductQuantity);
                TextView tvPrice = rowView.findViewById(R.id.tvProductPrice);
                ImageView ivImage = rowView.findViewById(R.id.ivProductImage);

                tvName.setText(item.getProductName());
                tvQuantity.setText("x" + item.getQuantity());

                String itemPriceFormatted = String.format("%,dđ", item.getPrice()).replace(',', '.');
                tvPrice.setText(itemPriceFormatted);

                com.bumptech.glide.Glide.with(ivImage.getContext()).load(item.getProductImage()).placeholder(android.R.color.darker_gray).error(android.R.color.darker_gray).into(ivImage);

                binding.llProductsContainer.addView(rowView);
            }

            if (order.getItems().size() > 2) {
                binding.llToggleProducts.setVisibility(View.VISIBLE);
                if (isExpanded) {
                    binding.tvToggleText.setText("Thu gọn");
                    binding.ivToggleIcon.setImageResource(R.drawable.ic_arrow_up);
                } else {
                    binding.tvToggleText.setText("Xem thêm");
                    binding.ivToggleIcon.setImageResource(R.drawable.ic_arrow_down);
                }
            } else {
                binding.llToggleProducts.setVisibility(View.GONE);
            }
        }

        private void setupActionButtons(
                OrderEntity order,
                OnOrderClickListener onCancelClick,
                OnOrderClickListener onDetailClick,
                OnOrderClickListener onReorderClick,
                OnOrderClickListener onTrackClick,
                OnOrderClickListener onContactClick,
                OnOrderClickListener onReviewClick
        ) {
            android.content.Context context = binding.getRoot().getContext();
            binding.btnActionOutlined.setTextColor(ContextCompat.getColor(context, R.color.black));
            binding.btnActionOutlined.setStrokeColorResource(R.color.black);

            switch (order.getStatus()) {
                case "Chờ xác nhận":
                    binding.btnActionOutlined.setVisibility(View.VISIBLE);
                    binding.btnActionOutlined.setText("Hủy đơn");
                    binding.btnActionOutlined.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    binding.btnActionOutlined.setStrokeColorResource(android.R.color.holo_red_dark);
                    binding.btnActionOutlined.setOnClickListener(v -> onCancelClick.onClick(order));

                    binding.btnActionFilled.setVisibility(View.VISIBLE);
                    binding.btnActionFilled.setText("Liên hệ hỗ trợ");
                    binding.btnActionFilled.setOnClickListener(v -> onContactClick.onClick(order));
                    break;
                case "Đang xử lý":
                    binding.btnActionOutlined.setVisibility(View.VISIBLE);
                    binding.btnActionOutlined.setText("Liên hệ");
                    binding.btnActionOutlined.setOnClickListener(v -> onContactClick.onClick(order));
                    binding.btnActionFilled.setVisibility(View.GONE);
                    break;
                case "Đang giao":
                    binding.btnActionOutlined.setVisibility(View.VISIBLE);
                    binding.btnActionOutlined.setText("Liên hệ");
                    binding.btnActionOutlined.setOnClickListener(v -> onContactClick.onClick(order));
                    binding.btnActionFilled.setVisibility(View.VISIBLE);
                    binding.btnActionFilled.setText("Theo dõi");
                    binding.btnActionFilled.setOnClickListener(v -> onTrackClick.onClick(order));
                    break;
                case "Hoàn tất":
                    binding.btnActionOutlined.setVisibility(View.VISIBLE);
                    binding.btnActionOutlined.setText(order.isHasReview() ? "Xem đánh giá" : "Đánh giá");
                    binding.btnActionOutlined.setOnClickListener(v -> onReviewClick.onClick(order));
                    binding.btnActionFilled.setVisibility(View.VISIBLE);
                    binding.btnActionFilled.setText("Mua lại");
                    binding.btnActionFilled.setOnClickListener(v -> onReorderClick.onClick(order));
                    break;
                case "Đã hủy":
                    binding.btnActionOutlined.setVisibility(View.GONE);
                    binding.btnActionFilled.setVisibility(View.VISIBLE);
                    binding.btnActionFilled.setText("Mua lại");
                    binding.btnActionFilled.setOnClickListener(v -> onReorderClick.onClick(order));
                    break;
                default:
                    binding.btnActionOutlined.setVisibility(View.GONE);
                    binding.btnActionFilled.setVisibility(View.GONE);
                    break;
            }
        }
    }

    public static class OrderDiffCallback extends DiffUtil.ItemCallback<OrderEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderEntity oldItem, @NonNull OrderEntity newItem) {
            return oldItem.equals(newItem)
                    && oldItem.isHasReview() == newItem.isHasReview()
                    && oldItem.getReviewStars() == newItem.getReviewStars()
                    && java.util.Objects.equals(oldItem.getReviewText(), newItem.getReviewText());
        }
    }
}
