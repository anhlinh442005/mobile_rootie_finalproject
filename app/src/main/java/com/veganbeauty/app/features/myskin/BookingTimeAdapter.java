package com.veganbeauty.app.features.myskin;

import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import java.util.List;

public class BookingTimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnItemClickListener { void onClick(BookingTime time); }
    public BookingTimeAdapter(List<BookingTime> list, OnItemClickListener listener) {}
    public void updateData(List<BookingTime> list) {}
    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { return null; }
    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
    @Override public int getItemCount() { return 0; }
}
