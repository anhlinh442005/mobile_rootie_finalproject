package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.Calendar;
import java.util.List;

public class BookingAdapters {

    public static class BookingService {
        private final String id;
        private final String name;
        private final String description;
        private final String duration;

        public BookingService(String id, String name, String description, String duration) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.duration = duration;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getDuration() { return duration; }
    }

    public static class BookingDate {
        private final String id;
        private final String dayOfWeek;
        private final String date;
        private final Calendar fullDate;

        public BookingDate(String id, String dayOfWeek, String date, Calendar fullDate) {
            this.id = id;
            this.dayOfWeek = dayOfWeek;
            this.date = date;
            this.fullDate = fullDate;
        }

        public String getId() { return id; }
        public String getDayOfWeek() { return dayOfWeek; }
        public String getDate() { return date; }
        public Calendar getFullDate() { return fullDate; }
    }

    public static class BookingTime {
        private final String id;
        private final String time;
        private final boolean isLocked;

        public BookingTime(String id, String time, boolean isLocked) {
            this.id = id;
            this.time = time;
            this.isLocked = isLocked;
        }

        public String getId() { return id; }
        public String getTime() { return time; }
        public boolean isLocked() { return isLocked; }
    }

    public interface OnServiceSelectedListener {
        void onSelected(BookingService service);
    }

    public interface OnDateSelectedListener {
        void onSelected(BookingDate date);
    }

    public interface OnTimeSelectedListener {
        void onSelected(BookingTime time);
    }

    public static class BookingServiceAdapter extends RecyclerView.Adapter<BookingServiceAdapter.ViewHolder> {
        private final List<BookingService> items;
        private final OnServiceSelectedListener onItemSelected;
        private int selectedIndex = -1;

        public BookingServiceAdapter(List<BookingService> items, OnServiceSelectedListener onItemSelected) {
            this.items = items;
            this.onItemSelected = onItemSelected;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final View container;
            public final TextView tvName;
            public final TextView tvDesc;
            public final ImageView ivCheck;

            public ViewHolder(View view) {
                super(view);
                container = view.findViewById(R.id.container_service);
                tvName = view.findViewById(R.id.tv_service_name);
                tvDesc = view.findViewById(R.id.tv_service_desc);
                ivCheck = view.findViewById(R.id.iv_service_check);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_booking_service, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingService item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvDesc.setText(item.getDescription());

            boolean isSelected = position == selectedIndex;
            holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            if (isSelected) {
                holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item);
            } else {
                holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
            }

            holder.container.setOnClickListener(v -> {
                int oldIndex = selectedIndex;
                selectedIndex = holder.getAdapterPosition();
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedIndex);
                if (onItemSelected != null) {
                    onItemSelected.onSelected(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    public static class BookingDateAdapter extends RecyclerView.Adapter<BookingDateAdapter.ViewHolder> {
        private final List<BookingDate> items;
        private final OnDateSelectedListener onItemSelected;
        private int selectedIndex = -1;

        public BookingDateAdapter(List<BookingDate> items, OnDateSelectedListener onItemSelected) {
            this.items = items;
            this.onItemSelected = onItemSelected;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final View container;
            public final TextView tvDay;
            public final TextView tvDate;

            public ViewHolder(View view) {
                super(view);
                container = view.findViewById(R.id.container_date);
                tvDay = view.findViewById(R.id.tv_date_day);
                tvDate = view.findViewById(R.id.tv_date_date);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_booking_date, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingDate item = items.get(position);
            holder.tvDay.setText(item.getDayOfWeek());
            holder.tvDate.setText(item.getDate());

            boolean isSelected = position == selectedIndex;
            if (isSelected) {
                holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item);
            } else {
                holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
            }

            holder.container.setOnClickListener(v -> {
                int oldIndex = selectedIndex;
                selectedIndex = holder.getAdapterPosition();
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedIndex);
                if (onItemSelected != null) {
                    onItemSelected.onSelected(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void setSelectedIndex(int index) {
            int oldIndex = selectedIndex;
            selectedIndex = index;
            notifyItemChanged(oldIndex);
            notifyItemChanged(selectedIndex);
        }
    }

    public static class BookingTimeAdapter extends RecyclerView.Adapter<BookingTimeAdapter.ViewHolder> {
        private List<BookingTime> items;
        private final OnTimeSelectedListener onItemSelected;
        private int selectedIndex = -1;

        public BookingTimeAdapter(List<BookingTime> items, OnTimeSelectedListener onItemSelected) {
            this.items = items;
            this.onItemSelected = onItemSelected;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final View container;
            public final TextView tvTime;

            public ViewHolder(View view) {
                super(view);
                container = view.findViewById(R.id.container_time);
                tvTime = view.findViewById(R.id.tv_time);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_booking_time, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingTime item = items.get(position);
            holder.tvTime.setText(item.getTime());

            boolean isSelected = position == selectedIndex;

            if (item.isLocked()) {
                holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
                holder.container.setAlpha(0.4f);
                holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.content));
            } else {
                holder.container.setAlpha(1.0f);
                if (isSelected) {
                    holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item);
                    holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                } else {
                    holder.container.setBackgroundResource(R.drawable.skin_bg_outline);
                    holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.content));
                }
            }

            holder.container.setOnClickListener(v -> {
                if (item.isLocked()) return;
                
                int oldIndex = selectedIndex;
                selectedIndex = holder.getAdapterPosition();
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedIndex);
                if (onItemSelected != null) {
                    onItemSelected.onSelected(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateData(List<BookingTime> newItems) {
            items = newItems;
            selectedIndex = -1;
            notifyDataSetChanged();
        }
    }
}
