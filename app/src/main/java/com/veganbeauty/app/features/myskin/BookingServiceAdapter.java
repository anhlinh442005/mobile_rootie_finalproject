package com.veganbeauty.app.features.myskin;

import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import java.util.List;

public class BookingServiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnItemClickListener { void onClick(BookingService service); }
    public BookingServiceAdapter(List<BookingService> list, OnItemClickListener listener) {}
    @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { return null; }
    @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
    @Override public int getItemCount() { return 0; }
}
