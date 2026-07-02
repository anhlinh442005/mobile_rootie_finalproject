package com.veganbeauty.app.features.community.beauty_hub;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.utils.BeVietnamProFontHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IngredientFragment extends Fragment {

    private RecyclerView rvIngredients;
    private EditText etSearch;
    private LinearLayout llFilterChips;
    private TextView tvResultCount;

    private List<IngredientEntity> allIngredients = new ArrayList<>();
    private String selectedType = null;
    private String searchQuery = "";

    private final List<String> allTypes = Arrays.asList("Tất cả", "Làm dịu", "Phục hồi", "Sạch sâu", "Da dầu mụn");
    private IngredientFullAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_ingredient, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BeVietnamProFontHelper.apply(view);

        rvIngredients = view.findViewById(R.id.rvIngredients);
        etSearch = view.findViewById(R.id.etSearch);
        llFilterChips = view.findViewById(R.id.llFilterChips);
        tvResultCount = view.findViewById(R.id.tvResultCount);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        allIngredients = new LocalJsonReader(requireContext()).getIngredients();

        setupFilterChips();

        adapter = new IngredientFullAdapter(allIngredients, ingredient -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, IngredientDetailFragment.newInstance(ingredient.getSlug()))
                    .addToBackStack(null)
                    .commit();
        });

        rvIngredients.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvIngredients.setAdapter(adapter);

        updateResultCount(allIngredients.size());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s != null ? s.toString() : "";
                filterAndUpdate();
            }
        });
    }

    private void setupFilterChips() {
        llFilterChips.removeAllViews();
        int dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        for (String type : allTypes) {
            TextView chip = new TextView(requireContext());
            chip.setText(type);
            chip.setTextSize(12f);
            chip.setPadding(12 * dp, 6 * dp, 12 * dp, 6 * dp);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(8 * dp);
            chip.setLayoutParams(params);

            chip.setClickable(true);
            chip.setFocusable(true);
            BeVietnamProFontHelper.applyToTextView(chip);

            chip.setOnClickListener(v -> {
                selectedType = "Tất cả".equals(type) ? null : type;
                refreshChipStates();
                filterAndUpdate();
            });

            llFilterChips.addView(chip);
        }
        refreshChipStates();
    }

    private void refreshChipStates() {
        for (int i = 0; i < llFilterChips.getChildCount(); i++) {
            View child = llFilterChips.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                String chipType = chip.getText().toString();
                boolean isSelected = ("Tất cả".equals(chipType) && selectedType == null) || chipType.equals(selectedType);

                if (isSelected) {
                    chip.setBackgroundResource(R.drawable.com_bg_chip_selected);
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                } else {
                    chip.setBackgroundResource(R.drawable.com_bg_chip_default);
                    chip.setTextColor(Color.parseColor("#888888"));
                }
            }
        }
    }

    private void filterAndUpdate() {
        List<IngredientEntity> filtered = new ArrayList<>();
        for (IngredientEntity item : allIngredients) {
            boolean matchesType = selectedType == null || (item.getTypes() != null && item.getTypes().contains(selectedType));
            boolean matchesSearch = true;

            if (!searchQuery.isEmpty()) {
                String sq = searchQuery.toLowerCase();
                String name = item.getName() != null ? item.getName().toLowerCase() : "";
                String scientific = item.getScientificName() != null ? item.getScientificName().toLowerCase() : "";
                String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                matchesSearch = name.contains(sq) || scientific.contains(sq) || desc.contains(sq);
            }

            if (matchesType && matchesSearch) {
                filtered.add(item);
            }
        }

        if (adapter != null) {
            adapter.updateData(filtered);
        }
        updateResultCount(filtered.size());
    }

    private void updateResultCount(int count) {
        if (tvResultCount != null) {
            tvResultCount.setText(count + " thành phần");
        }
    }

    public static class IngredientFullAdapter extends RecyclerView.Adapter<IngredientFullAdapter.ViewHolder> {

        private List<IngredientEntity> items;
        private final OnItemClickListener onClick;

        public interface OnItemClickListener {
            void onClick(IngredientEntity ingredient);
        }

        public IngredientFullAdapter(List<IngredientEntity> items, OnItemClickListener onClick) {
            this.items = items;
            this.onClick = onClick;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final ImageView ivIngredient;
            public final TextView tvName;
            public final TextView tvScientific;
            public final TextView tvDesc;

            public ViewHolder(View view) {
                super(view);
                ivIngredient = view.findViewById(R.id.ivIngredient);
                tvName = view.findViewById(R.id.tvIngredientName);
                tvScientific = view.findViewById(R.id.tvScientificName);
                tvDesc = view.findViewById(R.id.tvIngredientDesc);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_ingredient_full, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            IngredientEntity item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvScientific.setText(item.getScientificName());
            holder.tvDesc.setText(item.getDescription());

            if (item.getImage() != null && !item.getImage().isEmpty()) {
                com.bumptech.glide.Glide.with(holder.ivIngredient.getContext()).load(item.getImage()).error(R.drawable.img_placeholder).into(holder.ivIngredient);
            } else {
                holder.ivIngredient.setImageResource(R.drawable.img_placeholder);
            }

            holder.itemView.setOnClickListener(v -> {
                if (onClick != null) onClick.onClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateData(List<IngredientEntity> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }
    }
}
