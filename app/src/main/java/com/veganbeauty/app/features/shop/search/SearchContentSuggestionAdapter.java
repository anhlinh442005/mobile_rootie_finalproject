package com.veganbeauty.app.features.shop.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ShopSearchBarBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchContentSuggestionAdapter extends RecyclerView.Adapter<SearchContentSuggestionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ContentSuggestion item);
    }

    private final OnItemClickListener onItemClick;
    private final List<ContentSuggestion> items = new ArrayList<>();

    public SearchContentSuggestionAdapter(OnItemClickListener onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submitList(List<ContentSuggestion> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopSearchBarBinding binding = ShopSearchBarBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopSearchBarBinding binding;

        public ViewHolder(ShopSearchBarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ContentSuggestion item) {
            binding.tvText.setText(item.getLabel());
            int iconRes = R.drawable.ic_tag;
            if (item.getType() != null) {
                switch (item.getType()) {
                    case CATEGORY: iconRes = R.drawable.ic_tag; break;
                    case VIDEO: iconRes = R.drawable.ic_video_call; break;
                    case BLOG: iconRes = R.drawable.ic_article_outline; break;
                    case POST: iconRes = R.drawable.ic_community; break;
                }
            }
            binding.ivIcon.setImageResource(iconRes);
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(item));
        }
    }
}
