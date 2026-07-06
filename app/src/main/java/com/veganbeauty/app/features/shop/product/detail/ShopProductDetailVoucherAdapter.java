package com.veganbeauty.app.features.shop.product.detail;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.databinding.ShopItemVoucherHorizontalBinding;

import java.util.List;

public class ShopProductDetailVoucherAdapter extends RecyclerView.Adapter<ShopProductDetailVoucherAdapter.VoucherViewHolder> {

    private List<UserGiftEntity> vouchers;

    public ShopProductDetailVoucherAdapter(List<UserGiftEntity> vouchers) {
        this.vouchers = vouchers;
    }

    public void updateList(List<UserGiftEntity> newList) {
        this.vouchers = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemVoucherHorizontalBinding binding = ShopItemVoucherHorizontalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new VoucherViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        UserGiftEntity item = vouchers.get(position);
        ShopItemVoucherHorizontalBinding binding = holder.binding;

        binding.tvVoucherTitle.setText(item.getTitle());
        binding.tvVoucherDesc.setText(item.getDescription());

        String hsd = item.getExpiryDate();
        String displayHsd = hsd.contains(" ") ? hsd.split(" ")[0] : hsd;
        binding.tvVoucherHsd.setText("HSD: " + displayHsd);
        binding.tvVoucherCodeLabel.setText(item.getCode());

        String typeKey = item.getGiftType().toLowerCase().replace(" ", "").replace("_", "");
        if ("freeship".equals(typeKey) || "voucherfreeship".equals(typeKey)) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_shipping);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#556348"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green);
        } else {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_voucher);
            binding.ivVoucherIcon.setColorFilter(Color.parseColor("#02542D"));
            binding.ivVoucherIconBg.setBackgroundResource(R.drawable.bg_icon_rounded_green);
        }

        View.OnClickListener copyListener = v -> {
            Context context = v.getContext();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Voucher Code", item.getCode());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Đã sao chép mã: " + item.getCode(), Toast.LENGTH_SHORT).show();
            }
        };

        binding.btnVoucherAction.setOnClickListener(copyListener);
        binding.layoutCodeDisplay.setOnClickListener(copyListener);
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    public static class VoucherViewHolder extends RecyclerView.ViewHolder {
        public final ShopItemVoucherHorizontalBinding binding;

        public VoucherViewHolder(ShopItemVoucherHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
