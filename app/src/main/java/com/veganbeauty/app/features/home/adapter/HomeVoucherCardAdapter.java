package com.veganbeauty.app.features.home.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.VoucherEntity;
import com.veganbeauty.app.databinding.HomeItemVoucherCardBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeVoucherCardAdapter extends ListAdapter<VoucherEntity, HomeVoucherCardAdapter.ViewHolder> {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeVoucherCardAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemVoucherCardBinding binding = HomeItemVoucherCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemVoucherCardBinding binding;

        ViewHolder(HomeItemVoucherCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VoucherEntity voucher) {
            binding.tvVoucherTitle.setText(voucher.getTitle());
            binding.tvVoucherDesc.setText(buildDescription(voucher));
            binding.tvVoucherCode.setText(voucher.getCode());

            if (voucher.getBadge() != null && !voucher.getBadge().trim().isEmpty()) {
                binding.tvVoucherBadge.setVisibility(View.VISIBLE);
                binding.tvVoucherBadge.setText(voucher.getBadge());
            } else {
                binding.tvVoucherBadge.setVisibility(View.GONE);
            }

            int iconRes = resolveIcon(voucher);
            binding.ivVoucherIcon.setImageResource(iconRes);

            View.OnClickListener copyAction = v -> copyCode(v.getContext(), voucher.getCode());
            binding.btnVoucherCopy.setOnClickListener(copyAction);
            binding.layoutVoucherCode.setOnClickListener(copyAction);

            binding.ivVoucherInfo.setOnClickListener(v -> showInfoDialog(v.getContext(), voucher));
        }

        private String buildDescription(VoucherEntity voucher) {
            if (voucher.getDescription() != null && !voucher.getDescription().trim().isEmpty()) {
                return voucher.getDescription();
            }
            String minText = voucher.getMinOrderValue() > 0
                    ? " từ đơn " + formatShortCurrency(voucher.getMinOrderValue())
                    : "";
            if ("percentage".equalsIgnoreCase(voucher.getOfferType())) {
                return "Giảm " + voucher.getDiscountValue() + "% tối đa "
                        + formatShortCurrency(voucher.getDiscountValue()) + minText;
            }
            if ("free ship".equalsIgnoreCase(voucher.getType())
                    || "freeship".equalsIgnoreCase(voucher.getType())
                    || "voucher_freeship".equalsIgnoreCase(voucher.getType())) {
                return "Miễn phí vận chuyển" + minText;
            }
            return "Giảm " + formatShortCurrency(voucher.getDiscountValue()) + minText;
        }

        private String formatShortCurrency(long amount) {
            if (amount >= 1_000_000) {
                return (amount / 1_000_000) + " triệu";
            }
            if (amount >= 1_000) {
                return (amount / 1_000) + "k";
            }
            return currencyFormat.format(amount);
        }

        private int resolveIcon(VoucherEntity voucher) {
            String type = voucher.getType() != null ? voucher.getType().toLowerCase(Locale.ROOT) : "";
            String category = voucher.getCategory() != null ? voucher.getCategory().toLowerCase(Locale.ROOT) : "";
            if (type.contains("free") || category.contains("freeship")) {
                return R.drawable.ic_shop;
            }
            if (type.contains("flash") || category.contains("flash")) {
                return R.drawable.ic_skin_fire;
            }
            if (category.contains("combo") || category.contains("quà")) {
                return R.drawable.ic_gift;
            }
            return R.drawable.ic_voucher;
        }

        private void copyCode(Context context, String code) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("voucher_code", code));
            }
            Toast.makeText(context, "Đã sao chép mã " + code, Toast.LENGTH_SHORT).show();
        }

        private void showInfoDialog(Context context, VoucherEntity voucher) {
            StringBuilder message = new StringBuilder();
            if (voucher.getDescription() != null && !voucher.getDescription().trim().isEmpty()) {
                message.append(voucher.getDescription());
            } else {
                message.append(buildDescription(voucher));
            }
            if (voucher.getExpiryDate() != null && !voucher.getExpiryDate().trim().isEmpty()) {
                message.append("\n\nHSD: ").append(voucher.getExpiryDate());
            }
            new MaterialAlertDialogBuilder(context)
                    .setTitle(voucher.getTitle())
                    .setMessage(message.toString())
                    .setPositiveButton("Đóng", null)
                    .show();
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<VoucherEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull VoucherEntity oldItem, @NonNull VoucherEntity newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull VoucherEntity oldItem, @NonNull VoucherEntity newItem) {
            return oldItem.getCode().equals(newItem.getCode())
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getCategory().equals(newItem.getCategory());
        }
    }
}
