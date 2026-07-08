package com.veganbeauty.app.features.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.utils.SampleAvatarCatalog;

import java.util.List;

public class SampleAvatarPickerAdapter extends RecyclerView.Adapter<SampleAvatarPickerAdapter.ViewHolder> {

    public interface OnSampleSelectedListener {
        void onSampleSelected(@NonNull SampleAvatarCatalog.Item item);
    }

    private final List<SampleAvatarCatalog.Item> items;
    private final OnSampleSelectedListener listener;
    @Nullable
    private String selectedId;

    public SampleAvatarPickerAdapter(
            @NonNull List<SampleAvatarCatalog.Item> items,
            @NonNull OnSampleSelectedListener listener
    ) {
        this.items = items;
        this.listener = listener;
        if (!items.isEmpty()) {
            selectedId = items.get(0).id;
        }
    }

    public void setSelectedId(@Nullable String selectedId) {
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    @Nullable
    public SampleAvatarCatalog.Item getSelectedItem() {
        if (selectedId == null) {
            return null;
        }
        for (SampleAvatarCatalog.Item item : items) {
            if (selectedId.equals(item.id)) {
                return item;
            }
        }
        return items.isEmpty() ? null : items.get(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_sample, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SampleAvatarCatalog.Item item = items.get(position);
        holder.avatarView.setImageResource(item.drawableRes);
        holder.labelView.setText(item.label);
        boolean selected = item.id.equals(selectedId);
        holder.selectionRing.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.labelView.setTextColor(holder.itemView.getContext().getColor(
                selected ? R.color.primary : R.color.gray_dark
        ));
        holder.itemView.setOnClickListener(v -> {
            selectedId = item.id;
            notifyDataSetChanged();
            listener.onSampleSelected(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatarView;
        final TextView labelView;
        final View selectionRing;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.ivSampleAvatar);
            labelView = itemView.findViewById(R.id.tvSampleLabel);
            selectionRing = itemView.findViewById(R.id.viewSelectionRing);
        }
    }
}
