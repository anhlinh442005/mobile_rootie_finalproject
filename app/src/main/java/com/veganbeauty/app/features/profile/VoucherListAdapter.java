package com.veganbeauty.app.features.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ItemVoucherBinding;

import java.util.List;

public class VoucherListAdapter extends RecyclerView.Adapter<VoucherListAdapter.VoucherViewHolder> {

    public static class VoucherItem {
        private final String id;
        private final String title;
        private final String description;
        private final String code;
        private final String status;
        private final String hsd;
        private final String type;
        private final boolean fromGift;
        private final Integer quantity;
        private final int minOrderValue;
        private final String applicableProducts;
        private final String offerType;
        private final int discountValue;

        public VoucherItem(String id, String title, String description, String code, String status, String hsd, String type, boolean fromGift, Integer quantity, int minOrderValue, String applicableProducts, String offerType, int discountValue) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.code = code;
            this.status = status;
            this.hsd = hsd;
            this.type = type;
            this.fromGift = fromGift;
            this.quantity = quantity;
            this.minOrderValue = minOrderValue;
            this.applicableProducts = applicableProducts;
            this.offerType = offerType;
            this.discountValue = discountValue;
        }

        public VoucherItem(String id, String title, String description, String code, String status, String hsd, String type, boolean fromGift) {
            this(id, title, description, code, status, hsd, type, fromGift, null, 0, "Tất cả sản phẩm", "fixed_amount", 0);
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCode() { return code; }
        public String getStatus() { return status; }
        public String getHsd() { return hsd; }
        public String getType() { return type; }
        public boolean isFromGift() { return fromGift; }
        public Integer getQuantity() { return quantity; }
        public int getMinOrderValue() { return minOrderValue; }
        public String getApplicableProducts() { return applicableProducts; }
        public String getOfferType() { return offerType; }
        public int getDiscountValue() { return discountValue; }
    }

    public interface OnVoucherClickListener {
        void onClick(VoucherItem item);
    }

    private List<VoucherItem> vouchers;
    private OnVoucherClickListener onDeleteClickListener;
    private OnVoucherClickListener onItemClickListener;
    private OnVoucherClickListener onUseClickListener;

    public VoucherListAdapter(List<VoucherItem> vouchers) {
        this.vouchers = vouchers;
    }

    public void setOnDeleteClickListener(OnVoucherClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setOnItemClickListener(OnVoucherClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnUseClickListener(OnVoucherClickListener listener) {
        this.onUseClickListener = listener;
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        public final ItemVoucherBinding binding;

        public VoucherViewHolder(ItemVoucherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVoucherBinding binding = ItemVoucherBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VoucherViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        VoucherItem item = vouchers.get(position);
        ItemVoucherBinding binding = holder.binding;

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) onItemClickListener.onClick(item);
        });

        binding.tvVoucherTitle.setText(item.getTitle());
        binding.tvVoucherDesc.setText(item.getDescription());

        String displayHsd = item.getHsd().contains(" ") ? item.getHsd().split(" ")[0] : item.getHsd();
        binding.tvVoucherHsd.setText("HSD: " + displayHsd);
        binding.tvVoucherCodeLabel.setText("MÃ: " + item.getCode());

        String typeKey = item.getType().toLowerCase().replace(" ", "").replace("_", "");
        if ("freeship".equals(typeKey) || "voucherfreeship".equals(typeKey)) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_truck);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#556348"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green);
        } else if ("gift".equals(typeKey) || "productgift".equals(typeKey) || "product".equals(typeKey) || "product_gift".equals(typeKey)) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_gift);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#02542D"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green);
        } else if ("discount".equals(typeKey) || "voucherdiscount".equals(typeKey)) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_voucher);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#02542D"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green);
        } else {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_cancel);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#888888"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_grey);
        }

        if ("valid".equals(item.getStatus()) || "expiring".equals(item.getStatus())) {
            boolean isValid = "valid".equals(item.getStatus());
            binding.layoutCardContainer.setBackgroundResource(isValid ? R.drawable.bg_card_white : R.drawable.bg_card_expiring);

            boolean isExpiringToday = "expiring".equals(item.getStatus());
            boolean isLowQuantity = !item.isFromGift() && item.getQuantity() != null && item.getQuantity() < 10;

            if (isExpiringToday) {
                binding.tvVoucherExpiringBadge.setVisibility(View.VISIBLE);
                binding.tvVoucherExpiringBadge.setText("Sắp hết hạn");
            } else if (isLowQuantity) {
                binding.tvVoucherExpiringBadge.setVisibility(View.VISIBLE);
                binding.tvVoucherExpiringBadge.setText("Sắp hết");
            } else {
                binding.tvVoucherExpiringBadge.setVisibility(View.GONE);
            }

            binding.btnVoucherDelete.setVisibility(View.GONE);
            binding.btnVoucherDelete.setOnClickListener(null);

            binding.tvVoucherStatusTag.setText(isExpiringToday ? "Sắp hết" : "Còn hạn");
            binding.tvVoucherStatusTag.setBackgroundResource(isExpiringToday ? R.drawable.bg_tag_expiring : R.drawable.bg_tag_valid);
            binding.tvVoucherStatusTag.setTextColor(isExpiringToday ? Color.parseColor("#C62828") : Color.parseColor("#2E7D32"));

            binding.tvVoucherTitle.setPaintFlags(binding.tvVoucherTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvVoucherTitle.setTextColor(Color.parseColor("#3E4D44"));
            binding.tvVoucherDesc.setTextColor(Color.parseColor("#555555"));
            binding.tvVoucherHsd.setTextColor(isExpiringToday ? Color.parseColor("#C62828") : Color.parseColor("#888888"));
            binding.tvVoucherCodeLabel.setTextColor(Color.parseColor("#666666"));

            binding.btnVoucherAction.setText("Sử dụng ngay");
            binding.btnVoucherAction.setBackgroundResource(R.drawable.bg_button_copy);
            binding.btnVoucherAction.setTextColor(Color.WHITE);
            binding.btnVoucherAction.setEnabled(true);
            binding.btnVoucherAction.setOnClickListener(v -> {
                if (onUseClickListener != null) onUseClickListener.onClick(item);
            });
        } else if ("expired".equals(item.getStatus())) {
            binding.layoutCardContainer.setBackgroundResource(R.drawable.bg_card_expired);
            binding.tvVoucherExpiringBadge.setVisibility(View.GONE);
            binding.btnVoucherDelete.setVisibility(View.VISIBLE);
            binding.btnVoucherDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) onDeleteClickListener.onClick(item);
            });

            binding.tvVoucherStatusTag.setText("Hết hạn");
            binding.tvVoucherStatusTag.setBackgroundResource(R.drawable.bg_tag_expired);
            binding.tvVoucherStatusTag.setTextColor(Color.parseColor("#888888"));

            binding.tvVoucherTitle.setPaintFlags(binding.tvVoucherTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvVoucherTitle.setTextColor(Color.parseColor("#888888"));
            binding.tvVoucherDesc.setTextColor(Color.parseColor("#999999"));
            binding.tvVoucherHsd.setTextColor(Color.parseColor("#999999"));
            binding.tvVoucherCodeLabel.setTextColor(Color.parseColor("#999999"));

            binding.btnVoucherAction.setText("Đã hết hạn");
            binding.btnVoucherAction.setBackgroundResource(R.drawable.bg_button_disabled);
            binding.btnVoucherAction.setTextColor(Color.parseColor("#888888"));
            binding.btnVoucherAction.setEnabled(false);
            binding.btnVoucherAction.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    public void updateList(List<VoucherItem> newList) {
        vouchers = newList;
        notifyDataSetChanged();
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Voucher Code", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Đã sao chép mã: " + text, Toast.LENGTH_SHORT).show();
        }
    }
}
