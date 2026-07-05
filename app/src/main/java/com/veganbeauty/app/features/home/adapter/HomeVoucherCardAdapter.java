package com.veganbeauty.app.features.home.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.VoucherEntity;
import com.veganbeauty.app.databinding.HomeItemVoucherCardBinding;

import java.util.Locale;

public class HomeVoucherCardAdapter extends ListAdapter<VoucherEntity, HomeVoucherCardAdapter.ViewHolder> {

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
            boolean freeship = isFreeship(voucher);
            applyTheme(freeship);

            binding.tvVoucherDesc.setText(buildDescription(voucher));
            binding.tvVoucherCode.setText(voucher.getCode());
            binding.tvExpiry.setText(formatExpiry(voucher.getExpiryDate()));
            binding.tvScopeBadge.setText(buildScopeText(voucher, freeship));

            if (freeship) {
                binding.layoutDiscountValue.setVisibility(View.GONE);
                binding.layoutFreeshipValue.setVisibility(View.VISIBLE);
            } else {
                binding.layoutDiscountValue.setVisibility(View.VISIBLE);
                binding.layoutFreeshipValue.setVisibility(View.GONE);
                bindDiscountValue(voucher);
            }

            if (voucher.getBadge() != null && !voucher.getBadge().trim().isEmpty()) {
                binding.tvVoucherBadge.setVisibility(View.VISIBLE);
                binding.tvVoucherBadge.setText(voucher.getBadge());
            } else {
                binding.tvVoucherBadge.setVisibility(View.GONE);
            }

            View.OnClickListener copyAction = v -> copyCode(v.getContext(), voucher.getCode());
            binding.btnVoucherCopy.setOnClickListener(copyAction);
            binding.layoutVoucherBottom.setOnClickListener(copyAction);
        }

        private void applyTheme(boolean freeship) {
            Context context = binding.getRoot().getContext();
            @ColorInt int accent = ContextCompat.getColor(context,
                    freeship ? R.color.home_voucher_orange_dark : R.color.home_voucher_green_dark);
            @ColorInt int accentLight = ContextCompat.getColor(context,
                    freeship ? R.color.home_voucher_orange : R.color.home_voucher_green);

            binding.layoutVoucherLeft.setBackgroundResource(
                    freeship ? R.drawable.bg_home_voucher_left_freeship : R.drawable.bg_home_voucher_left_discount);
            binding.tvVoucherBadge.setBackgroundResource(
                    freeship ? R.drawable.bg_home_voucher_ribbon_orange : R.drawable.bg_home_voucher_ribbon_green);
            binding.layoutScopeBadge.setBackgroundResource(
                    freeship ? R.drawable.bg_home_voucher_badge_orange : R.drawable.bg_home_voucher_badge_green);
            binding.layoutExpiry.setBackgroundResource(
                    freeship ? R.drawable.bg_home_voucher_badge_orange : R.drawable.bg_home_voucher_badge_green);
            binding.btnVoucherCopy.setBackgroundResource(
                    freeship ? R.drawable.bg_home_voucher_btn_orange : R.drawable.bg_home_voucher_btn_green);

            ImageViewCompat.setImageTintList(binding.ivScopeIcon,
                    android.content.res.ColorStateList.valueOf(accentLight));
            ImageViewCompat.setImageTintList(binding.ivExpiryIcon,
                    android.content.res.ColorStateList.valueOf(accentLight));
            ImageViewCompat.setImageTintList(binding.ivWatermark,
                    android.content.res.ColorStateList.valueOf(accentLight));

            binding.tvScopeBadge.setTextColor(accent);
            binding.tvExpiry.setTextColor(accent);
        }

        private void bindDiscountValue(VoucherEntity voucher) {
            if ("percentage".equalsIgnoreCase(voucher.getOfferType())) {
                binding.tvValueAmount.setText(voucher.getDiscountValue() + "%");
            } else {
                binding.tvValueAmount.setText(formatShortCurrency(voucher.getDiscountValue()));
            }

            if (voucher.getMinOrderValue() > 0) {
                binding.tvMinOrder.setVisibility(View.VISIBLE);
                binding.tvMinOrder.setText("Đơn từ " + formatShortCurrency(voucher.getMinOrderValue()));
            } else {
                binding.tvMinOrder.setVisibility(View.GONE);
            }
        }

        private String buildScopeText(VoucherEntity voucher, boolean freeship) {
            if (freeship) {
                return "Cho mọi đơn hàng";
            }
            String category = voucher.getCategory() != null ? voucher.getCategory().trim() : "";
            if (category.isEmpty() || "tất cả".equalsIgnoreCase(category)) {
                return "Cho tất cả sản phẩm";
            }
            return "Cho " + category.toLowerCase(Locale.ROOT);
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
            if (isFreeship(voucher)) {
                return "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng" + minText + ".";
            }
            return "Giảm " + formatShortCurrency(voucher.getDiscountValue()) + minText + ".";
        }

        private String formatExpiry(String expiryDate) {
            if (expiryDate == null || expiryDate.trim().isEmpty()) {
                return "HSD: --";
            }
            String trimmed = expiryDate.trim();
            return trimmed.toUpperCase(Locale.ROOT).startsWith("HSD")
                    ? trimmed
                    : "HSD: " + trimmed;
        }

        private String formatShortCurrency(long amount) {
            if (amount >= 1_000_000) {
                return (amount / 1_000_000) + "M";
            }
            if (amount >= 1_000) {
                return (amount / 1_000) + "K";
            }
            return String.valueOf(amount);
        }

        private boolean isFreeship(VoucherEntity voucher) {
            String type = voucher.getType() != null ? voucher.getType().toLowerCase(Locale.ROOT) : "";
            String category = voucher.getCategory() != null ? voucher.getCategory().toLowerCase(Locale.ROOT) : "";
            return type.contains("free") || category.contains("freeship");
        }

        private void copyCode(Context context, String code) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("voucher_code", code));
            }
            Toast.makeText(context, "Đã sao chép mã " + code, Toast.LENGTH_SHORT).show();
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
                    && oldItem.getCategory().equals(newItem.getCategory())
                    && oldItem.getType().equals(newItem.getType());
        }
    }
}
