package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ItemSkinHistoryBinding;

import org.json.JSONArray;
import org.json.JSONObject;

public class SkinHistoryAdapter extends RecyclerView.Adapter<SkinHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(JSONObject item);
    }

    private JSONArray data;
    private final OnItemClickListener onItemClick;

    public SkinHistoryAdapter(JSONArray data, OnItemClickListener onItemClick) {
        this.data = data;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSkinHistoryBinding binding = ItemSkinHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject item = data.getJSONObject(position);
            holder.bind(item);

            if (position == data.length() - 1) {
                holder.binding.itemSkinHistoryDivider.setVisibility(View.GONE);
            } else {
                holder.binding.itemSkinHistoryDivider.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.length() : 0;
    }

    public void updateData(JSONArray newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemSkinHistoryBinding binding;

        public ViewHolder(ItemSkinHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(JSONObject item) {
            binding.itemSkinHistoryDate.setText(item.optString("date", ""));
            binding.itemSkinHistoryTime.setText(item.optString("time", ""));
            binding.itemSkinHistoryType.setText(item.optString("scanType", "Quét AI"));
            int score = item.optInt("score", -1);
            if (score < 0) {
                score = com.veganbeauty.app.data.local.SkinProfileMetricsHelper.computeOverallScore(item);
            }
            binding.itemSkinHistoryScore.setText(String.valueOf(score));

            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(item));
        }
    }
}
