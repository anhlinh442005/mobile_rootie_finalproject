package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.List;

public class BookingServiceAdapter extends RecyclerView.Adapter<BookingServiceAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onClick(BookingService service);
    }

    private final List<BookingService> list;
    private final OnItemClickListener listener;
    private int selectedIndex = -1;

    public BookingServiceAdapter(List<BookingService> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View container;
        final TextView tvName;
        final TextView tvDesc;
        final ImageView ivCheck;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container_service);
            tvName = itemView.findViewById(R.id.tv_service_name);
            tvDesc = itemView.findViewById(R.id.tv_service_desc);
            ivCheck = itemView.findViewById(R.id.iv_service_check);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skin_item_booking_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingService item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getPriceInfo());

        boolean isSelected = position == selectedIndex;
        holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.container.setBackgroundResource(
                isSelected ? R.drawable.skin_bg_selected_item : R.drawable.skin_bg_outline
        );

        holder.container.setOnClickListener(v -> {
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
