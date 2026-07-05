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
import com.veganbeauty.app.data.local.entities.UserGiftEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyGiftsAdapter extends RecyclerView.Adapter<MyGiftsAdapter.ViewHolder> {

    public interface OnGiftClick { void onClick(UserGiftEntity gift); }

    private final List<UserGiftEntity> items = new ArrayList<>();
    private final OnGiftClick onItemClick;
    private final OnGiftClick onActionClick;

    public MyGiftsAdapter(OnGiftClick onItemClick, OnGiftClick onActionClick) {
        this.onItemClick = onItemClick;
        this.onActionClick = onActionClick;
    }

    public void submitList(List<UserGiftEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward_gift, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), onItemClick, onActionClick);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, subtitle, value, badge, actionBtn;
        final ImageView img;
        final View imgCircle;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tvGiftTitle);
            subtitle = view.findViewById(R.id.tvGiftSubtitle);
            value = view.findViewById(R.id.tvGiftValue);
            badge = view.findViewById(R.id.tvGiftStatusBadge);
            img = view.findViewById(R.id.ivGiftIcon);
            imgCircle = view.findViewById(R.id.layoutIconCircle);
            actionBtn = view.findViewById(R.id.btnAction);
        }

        void bind(UserGiftEntity gift, OnGiftClick onClick, OnGiftClick onAction) {
            title.setText(gift.getTitle());
            String displayHsd = gift.getExpiryDate().contains(" ") ? gift.getExpiryDate().split(" ")[0] : gift.getExpiryDate();
            String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            boolean expiringToday = gift.getExpiryDate().startsWith(todayStr);
            subtitle.setText(gift.getDescription() + "\nHSD: " + (expiringToday ? "Hôm nay" : displayHsd));
            value.setVisibility(View.GONE);
            badge.setVisibility(View.VISIBLE);

            boolean isExpired;
            try {
                Date exp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(gift.getExpiryDate());
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
                isExpired = exp != null && exp.before(today.getTime());
            } catch (Exception e) { isExpired = false; }

            String computedStatus = isExpired ? "Hết hạn" : (expiringToday ? "Hôm nay" : "Còn hạn");
            badge.setText(computedStatus);

            switch (computedStatus) {
                case "Hôm nay":
                    badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF3E0")));
                    badge.setTextColor(Color.parseColor("#D97706"));
                    actionBtn.setVisibility(View.VISIBLE); actionBtn.setText("Sử dụng");
                    actionBtn.setTextColor(Color.WHITE);
                    actionBtn.setBackgroundResource(R.drawable.bg_btn_buy);
                    actionBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D3A34")));
                    break;
                case "Hết hạn":
                    badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECEFF1")));
                    badge.setTextColor(Color.parseColor("#7E8A83"));
                    actionBtn.setVisibility(View.GONE);
                    break;
                default:
                    badge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                    badge.setTextColor(Color.parseColor("#2E7D32"));
                    actionBtn.setVisibility(View.VISIBLE); actionBtn.setText("Sử dụng");
                    actionBtn.setTextColor(Color.WHITE);
                    actionBtn.setBackgroundResource(R.drawable.bg_btn_buy);
                    actionBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D3A34")));
                    break;
            }

            int iconRes;
            switch (gift.getGiftType()) {
                case "voucher_discount": case "voucher": iconRes = R.drawable.ic_voucher; break;
                case "voucher_freeship": case "freeship": iconRes = R.drawable.ic_shipping; break;
                case "gift": iconRes = R.drawable.ic_gift; break;
                case "product": iconRes = R.drawable.ic_logo_rootie; break;
                default: iconRes = R.drawable.ic_gift; break;
            }
            img.setImageResource(iconRes);
            img.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.primary)));
            imgCircle.setBackgroundResource(R.drawable.bg_circle_light_green);

            itemView.setOnClickListener(v -> onClick.onClick(gift));
            actionBtn.setOnClickListener(v -> onAction.onClick(gift));
        }
    }
}
