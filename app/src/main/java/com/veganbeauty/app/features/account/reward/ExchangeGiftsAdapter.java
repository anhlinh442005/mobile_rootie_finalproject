package com.veganbeauty.app.features.account.reward;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.ArrayList;
import java.util.List;

public class ExchangeGiftsAdapter extends RecyclerView.Adapter<ExchangeGiftsAdapter.ViewHolder> {

    public interface OnItemClick { void onClick(RedeemableGift gift); }

    private final List<RedeemableGift> items = new ArrayList<>();
    private int userPoints = 8500;
    private final OnItemClick onItemClick;
    private final OnItemClick onActionClick;

    public ExchangeGiftsAdapter(OnItemClick onItemClick, OnItemClick onActionClick) {
        this.onItemClick = onItemClick;
        this.onActionClick = onActionClick;
    }

    public void submitList(List<RedeemableGift> newItems, int points) {
        items.clear();
        items.addAll(newItems);
        userPoints = points;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward_gift, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), userPoints, onItemClick, onActionClick);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, subtitle, value, actionBtn, badge;
        final ImageView img;
        final View imgCircle;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tvGiftTitle);
            subtitle = view.findViewById(R.id.tvGiftSubtitle);
            value = view.findViewById(R.id.tvGiftValue);
            img = view.findViewById(R.id.ivGiftIcon);
            imgCircle = view.findViewById(R.id.layoutIconCircle);
            actionBtn = view.findViewById(R.id.btnAction);
            badge = view.findViewById(R.id.tvGiftStatusBadge);
        }

        void bind(RedeemableGift gift, int userPoints, OnItemClick onClick, OnItemClick onActionClick) {
            title.setText(gift.getTitle());
            boolean expiringToday = gift.getExpiryDate().startsWith("2026-06-11");
            String datePart = gift.getExpiryDate().contains(" ") ? gift.getExpiryDate().split(" ")[0] : gift.getExpiryDate();
            subtitle.setText(expiringToday ? "Hôm nay" : "HSD: " + datePart);
            value.setText(String.format("%,d xu", gift.getCost()).replace(',', '.'));

            boolean isLocked = (gift.getRankRequired().equals("Vàng") && userPoints < 10000)
                    || ((gift.getRankRequired().equals("VIP") || gift.getRankRequired().equals("Kim Cương")) && userPoints < 20000);
            boolean notEnough = userPoints < gift.getCost();

            itemView.setOnClickListener(v -> onClick.onClick(gift));

            if (expiringToday) {
                badge.setVisibility(View.VISIBLE);
                badge.setText("Sắp hết hạn");
                badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                badge.setTextColor(Color.parseColor("#D97706"));
            } else {
                badge.setVisibility(View.GONE);
            }

            int iconRes = getIconRes(gift.getGiftType());

            if (isLocked) {
                actionBtn.setText("Chưa đủ ĐK"); actionBtn.setEnabled(false);
                actionBtn.setTextColor(Color.parseColor("#A0AEC0"));
                actionBtn.setBackgroundResource(R.drawable.com_bg_post);
                actionBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECEFF0")));
                img.setImageResource(R.drawable.ic_lock);
                img.setImageTintList(ColorStateList.valueOf(Color.parseColor("#A0AEC0")));
                imgCircle.setBackgroundResource(R.drawable.bg_circle_grey);
                actionBtn.setOnClickListener(null);
            } else if (notEnough) {
                actionBtn.setText("Đổi quà"); actionBtn.setEnabled(false);
                actionBtn.setTextColor(Color.parseColor("#A0AEC0"));
                actionBtn.setBackgroundResource(R.drawable.com_bg_post);
                actionBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECEFF0")));
                img.setImageResource(iconRes);
                img.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary)));
                imgCircle.setBackgroundResource(R.drawable.bg_circle_grey);
                actionBtn.setOnClickListener(null);
            } else {
                actionBtn.setText("Đổi quà"); actionBtn.setEnabled(true);
                actionBtn.setTextColor(Color.WHITE);
                actionBtn.setBackgroundResource(R.drawable.bg_btn_buy);
                actionBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D3A34")));
                img.setImageResource(iconRes);
                img.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary)));
                imgCircle.setBackgroundResource(R.drawable.bg_circle_grey);
                actionBtn.setOnClickListener(v -> onActionClick.onClick(gift));
            }
        }

        private int getIconRes(String type) {
            switch (type) {
                case "voucher_discount": case "voucher": return R.drawable.ic_voucher;
                case "voucher_freeship": case "freeship": return R.drawable.ic_shipping;
                case "gift": return R.drawable.ic_gift;
                case "product": return R.drawable.ic_logo_rootie;
                default: return R.drawable.ic_gift;
            }
        }
    }
}
