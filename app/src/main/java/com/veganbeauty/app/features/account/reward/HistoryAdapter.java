package com.veganbeauty.app.features.account.reward;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private final List<HistoryItem> items = new ArrayList<>();

    public void submitList(List<HistoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof HistoryItem.Header ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            TextView tv = new TextView(parent.getContext());
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setLayoutParams(lp);
            tv.setPadding(16, 20, 16, 8);
            tv.setTextSize(16f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
            return new HeaderViewHolder(tv);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward_history, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        if (item instanceof HistoryItem.Header) {
            ((HeaderViewHolder) holder).bind(((HistoryItem.Header) item).getTitle());
        } else if (item instanceof HistoryItem.Transaction) {
            boolean isFirst = position == 0 || items.get(position - 1) instanceof HistoryItem.Header;
            boolean isLast = position == getItemCount() - 1 || items.get(position + 1) instanceof HistoryItem.Header;
            ((ItemViewHolder) holder).bind(((HistoryItem.Transaction) item).getData(), isFirst, isLast, position == 1);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView tv;
        HeaderViewHolder(TextView tv) { super(tv); this.tv = tv; }
        void bind(String title) { tv.setText(title); }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        final TextView title, date, value, status;
        final View dot, lineTop, lineBottom;

        ItemViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.tvHistoryTitle);
            date = view.findViewById(R.id.tvHistoryDate);
            value = view.findViewById(R.id.tvHistoryPoints);
            status = view.findViewById(R.id.tvHistoryStatus);
            dot = view.findViewById(R.id.timelineDot);
            lineTop = view.findViewById(R.id.timelineLineTop);
            lineBottom = view.findViewById(R.id.timelineLineBottom);
        }

        void bind(RewardPointEntity item, boolean isFirst, boolean isLast, boolean isMostRecent) {
            title.setText(item.getReason().replace("Đổi quà: ", ""));
            date.setText(new SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault()).format(new Date(item.getTimestamp())));
            String fmt = String.format("%,d", item.getPoints()).replace(',', '.');
            if (item.getPoints() > 0) {
                value.setText("+ " + fmt);
                value.setTextColor(Color.parseColor("#38A169"));
                status.setText("Nhận xu thành công");
                status.setTextColor(Color.parseColor("#2E7D32"));
                status.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            } else {
                value.setText(fmt);
                value.setTextColor(Color.parseColor("#E53E3E"));
                String reason = item.getReason() != null ? item.getReason() : "";
                if (reason.startsWith("Đổi quà")) {
                    status.setText("Đổi quà thành công");
                } else {
                    status.setText("Đã dùng xu");
                }
                status.setTextColor(Color.parseColor("#C53030"));
                status.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FED7D7")));
            }
            dot.setBackgroundResource(isMostRecent ? R.drawable.bg_dialog_btn_confirm : R.drawable.bg_circle_grey);
            dot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isMostRecent ? "#3E4D44" : "#A0AEC0")));
            lineTop.setVisibility(isFirst ? View.INVISIBLE : View.VISIBLE);
            lineBottom.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
