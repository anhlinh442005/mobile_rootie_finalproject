package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class BookingTimeAdapter extends RecyclerView.Adapter<BookingTimeAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onClick(BookingTime time);
    }

    private List<BookingTime> list;
    private final OnItemClickListener listener;
    private final BooleanSupplier loginGate;
    private int selectedIndex = -1;

    public BookingTimeAdapter(List<BookingTime> list, OnItemClickListener listener) {
        this(list, listener, () -> true);
    }

    public BookingTimeAdapter(List<BookingTime> list, OnItemClickListener listener, BooleanSupplier loginGate) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
        this.loginGate = loginGate != null ? loginGate : () -> true;
    }

    public void updateData(List<BookingTime> list) {
        this.list = list != null ? list : new ArrayList<>();
        selectedIndex = -1;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View container;
        final TextView tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container_time);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skin_item_booking_time, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingTime item = list.get(position);
        holder.tvTime.setText(item.getTime());

        boolean isSelected = position == selectedIndex;
        if (item.isLocked()) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
            holder.container.setAlpha(0.4f);
            holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.content));
        } else {
            holder.container.setAlpha(1f);
            if (isSelected) {
                holder.container.setBackgroundResource(R.drawable.skin_bg_booking_slot_selected);
                holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_level_blue));
            } else {
                holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
                holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.content));
            }
        }

        holder.container.setOnClickListener(v -> {
            if (!loginGate.getAsBoolean()) {
                return;
            }
            if (item.isLocked()) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Giờ này đã được bạn đặt hoặc đã qua, vui lòng chọn giờ khác.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            int oldIndex = selectedIndex;
            selectedIndex = holder.getBindingAdapterPosition();
            if (oldIndex >= 0) notifyItemChanged(oldIndex);
            if (selectedIndex >= 0) notifyItemChanged(selectedIndex);
            if (listener != null && selectedIndex >= 0 && selectedIndex < list.size()) {
                listener.onClick(list.get(selectedIndex));
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }
}
