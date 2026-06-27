package com.veganbeauty.app.features.community.beauty_hub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.IngredientEntity;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {

    private List<IngredientEntity> ingredients;

    public IngredientAdapter(List<IngredientEntity> ingredients) {
        this.ingredients = ingredients;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivIngredient;
        public final TextView tvName;
        public final TextView tvDesc;

        public ViewHolder(View view) {
            super(view);
            ivIngredient = view.findViewById(R.id.ivIngredient);
            tvName = view.findViewById(R.id.tvIngredientName);
            tvDesc = view.findViewById(R.id.tvIngredientDesc);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IngredientEntity item = ingredients.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());

        if (item.getImage() != null && !item.getImage().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(item.getImage())
                    .target(holder.ivIngredient)
                    .crossfade(true)
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.ivIngredient.setImageResource(R.drawable.img_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    public void updateData(List<IngredientEntity> newIngredients) {
        this.ingredients = newIngredients;
        notifyDataSetChanged();
    }
}
