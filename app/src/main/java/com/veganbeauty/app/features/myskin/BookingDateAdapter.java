package com.veganbeauty.app.features.myskin;



import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import android.widget.TextView;



import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.RecyclerView;



import com.veganbeauty.app.R;



import java.util.List;
import java.util.function.BooleanSupplier;



public class BookingDateAdapter extends RecyclerView.Adapter<BookingDateAdapter.ViewHolder> {

    public interface OnItemClickListener {

        void onClick(BookingDate date);

    }



    private final List<BookingDate> list;

    private final OnItemClickListener listener;

    private final BooleanSupplier loginGate;

    private int selectedIndex = -1;



    public BookingDateAdapter(List<BookingDate> list, OnItemClickListener listener) {

        this(list, listener, () -> true);

    }



    public BookingDateAdapter(List<BookingDate> list, OnItemClickListener listener, BooleanSupplier loginGate) {

        this.list = list;

        this.listener = listener;

        this.loginGate = loginGate != null ? loginGate : () -> true;

    }



    static class ViewHolder extends RecyclerView.ViewHolder {

        final View container;

        final TextView tvDay;

        final TextView tvDate;



        ViewHolder(View itemView) {

            super(itemView);

            container = itemView.findViewById(R.id.container_date);

            tvDay = itemView.findViewById(R.id.tv_date_day);

            tvDate = itemView.findViewById(R.id.tv_date_date);

        }

    }



    @NonNull

    @Override

    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())

                .inflate(R.layout.skin_item_booking_date, parent, false);

        return new ViewHolder(view);

    }



    @Override

    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        BookingDate item = list.get(position);

        holder.tvDay.setText(item.getDayOfWeek());

        holder.tvDate.setText(item.getDate());



        boolean isSelected = position == selectedIndex;

        if (isSelected) {

            holder.container.setBackgroundResource(R.drawable.skin_bg_booking_slot_selected);

            int blueText = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_level_blue);

            holder.tvDay.setTextColor(blueText);

            holder.tvDate.setTextColor(blueText);

        } else {

            holder.container.setBackgroundResource(R.drawable.skin_bg_outline);

            int primary = ContextCompat.getColor(holder.itemView.getContext(), R.color.primary);

            int tertiary = ContextCompat.getColor(holder.itemView.getContext(), R.color.tertiary);

            holder.tvDay.setTextColor(primary);

            holder.tvDate.setTextColor(tertiary);

        }



        holder.container.setOnClickListener(v -> {

            if (!loginGate.getAsBoolean()) {

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

