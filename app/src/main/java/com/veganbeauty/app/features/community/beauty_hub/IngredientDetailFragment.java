package com.veganbeauty.app.features.community.beauty_hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IngredientDetailFragment extends Fragment {

    private static final String ARG_SLUG = "slug";
    private String ingredientSlug;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static IngredientDetailFragment newInstance(String slug) {
        IngredientDetailFragment fragment = new IngredientDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SLUG, slug);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ingredientSlug = getArguments().getString(ARG_SLUG);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_ingredient_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        executor.execute(() -> {
            if (getContext() == null) return;
            LocalJsonReader jsonReader = new LocalJsonReader(getContext());
            List<IngredientEntity> ingredients = jsonReader.getIngredients();
            
            IngredientEntity foundIngredient = null;
            if (ingredients != null) {
                for (IngredientEntity ing : ingredients) {
                    if (ing.getSlug().equals(ingredientSlug)) {
                        foundIngredient = ing;
                        break;
                    }
                }
            }
            
            final IngredientEntity ingredient = foundIngredient;
            if (ingredient == null) return;

            List<ProductEntity> allProducts = jsonReader.getAllProducts();
            List<ProductEntity> matchingProducts = new ArrayList<>();
            
            if (allProducts != null) {
                for (ProductEntity p : allProducts) {
                    boolean match = false;
                    if (p.getMainIngredientsSummary() != null && p.getMainIngredientsSummary().toLowerCase().contains(ingredient.getName().toLowerCase())) match = true;
                    if (!match && p.getDetailedIngredients() != null) {
                        for (String di : p.getDetailedIngredients()) {
                            if (di.toLowerCase().contains(ingredient.getName().toLowerCase())) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (!match && p.getName() != null && p.getName().toLowerCase().contains(ingredient.getName().toLowerCase())) match = true;
                    if (!match && p.getDescription() != null && p.getDescription().toLowerCase().contains(ingredient.getName().toLowerCase())) match = true;
                    if (!match && p.getKeyIngredients() != null) {
                        for (ProductEntity.KeyIngredient ki : p.getKeyIngredients()) {
                            if (ki.getName().toLowerCase().contains(ingredient.getName().toLowerCase())) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (match) matchingProducts.add(p);
                }
            }

            List<ProductEntity> finalProducts = new ArrayList<>();
            if (matchingProducts.isEmpty() && allProducts != null) {
                List<ProductEntity> shuffled = new ArrayList<>(allProducts);
                Collections.shuffle(shuffled);
                for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
                    finalProducts.add(shuffled.get(i));
                }
            } else {
                Collections.shuffle(matchingProducts);
                for (int i = 0; i < Math.min(6, matchingProducts.size()); i++) {
                    finalProducts.add(matchingProducts.get(i));
                }
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                ImageView ivHeader = view.findViewById(R.id.ivHeader);
                if (ivHeader != null && ingredient.getImage() != null) {
                    ImageRequest request = new ImageRequest.Builder(requireContext())
                            .data(ingredient.getImage())
                            .target(ivHeader)
                            .crossfade(true)
                            .build();
                    Coil.imageLoader(requireContext()).enqueue(request);
                }

                TextView tvName = view.findViewById(R.id.tvName);
                if (tvName != null) tvName.setText(ingredient.getName());

                TextView tvScientificName = view.findViewById(R.id.tvScientificName);
                if (tvScientificName != null) tvScientificName.setText(ingredient.getScientificName());

                TextView tvOrigin = view.findViewById(R.id.tvOrigin);
                if (tvOrigin != null) tvOrigin.setText(ingredient.getOrigin());

                TextView tabIntro = view.findViewById(R.id.tabIntro);
                TextView tabUses = view.findViewById(R.id.tabUses);
                TextView tvContent = view.findViewById(R.id.tvContent);

                if (tvContent != null) tvContent.setText(ingredient.getDescription());

                int colorActive = ContextCompat.getColor(requireContext(), R.color.white);
                int colorInactive = ContextCompat.getColor(requireContext(), R.color.primary);

                if (tabIntro != null && tabUses != null && tvContent != null) {
                    tabIntro.setOnClickListener(v -> {
                        tabIntro.setTextColor(colorActive);
                        tabIntro.setBackgroundResource(R.drawable.com_bg_tab_active);
                        tabUses.setTextColor(colorInactive);
                        tabUses.setBackgroundResource(android.R.color.transparent);
                        tvContent.setText(ingredient.getDescription());
                    });

                    tabUses.setOnClickListener(v -> {
                        tabUses.setTextColor(colorActive);
                        tabUses.setBackgroundResource(R.drawable.com_bg_tab_active);
                        tabIntro.setTextColor(colorInactive);
                        tabIntro.setBackgroundResource(android.R.color.transparent);
                        tvContent.setText(ingredient.getUses());
                    });
                }

                TextView tvProductsTitle = view.findViewById(R.id.tvProductsTitle);
                if (tvProductsTitle != null) {
                    tvProductsTitle.setText("Sản phẩm chứa " + ingredient.getName().toLowerCase());
                }

                LinearLayout llTypes = view.findViewById(R.id.llTypes);
                if (llTypes != null) {
                    llTypes.removeAllViews();
                    int dp = (int) getResources().getDisplayMetrics().density;
                    if (ingredient.getTypes() != null) {
                        for (String type : ingredient.getTypes()) {
                            TextView chip = new TextView(requireContext());
                            chip.setText(type);
                            chip.setTextSize(12f);
                            chip.setPadding(12 * dp, 6 * dp, 12 * dp, 6 * dp);
                            chip.setBackgroundResource(R.drawable.com_bg_chip_type);
                            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                            LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            marginParams.setMarginEnd(8 * dp);
                            chip.setLayoutParams(marginParams);
                            llTypes.addView(chip);
                        }
                    }
                }

                RecyclerView rvProducts = view.findViewById(R.id.rvProducts);
                if (rvProducts != null) {
                    ShopListAdapter adapter = new ShopListAdapter(p -> {}, p -> {});
                    adapter.submitList(finalProducts);
                    rvProducts.setAdapter(adapter);
                }
            });
        });
    }
}
