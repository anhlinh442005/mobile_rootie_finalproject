package com.veganbeauty.app.features.shop.product.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.veganbeauty.app.databinding.ShopBottomSheetReviewsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.Pair;

public class ProductReviewsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ProductReviewsBottomSheet";
    
    private static final String ARG_PRODUCT_ID = "arg_product_id";
    private static final String ARG_PRODUCT_NAME = "arg_product_name";
    private static final String ARG_CATEGORY = "arg_category";

    private ShopBottomSheetReviewsBinding binding;
    private final List<ProductReview> allReviews = new ArrayList<>();
    private ShopReviewAdapter adapter;
    private int selectedStars = 0;

    public static ProductReviewsBottomSheet newInstance(String productId, String productName, String category) {
        ProductReviewsBottomSheet fragment = new ProductReviewsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_PRODUCT_NAME, productName);
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopBottomSheetReviewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String productId = "";
        String productName = "";
        String category = "";

        if (getArguments() != null) {
            productId = getArguments().getString(ARG_PRODUCT_ID, "");
            productName = getArguments().getString(ARG_PRODUCT_NAME, "");
            category = getArguments().getString(ARG_CATEGORY, "");
        }

        Pair<Double, Integer> stats = ProductReviewHelper.getRatingStats(productId);
        
        binding.tvAverageRating.setText(String.format(Locale.US, "%.1f", stats.getFirst()));
        binding.tvTotalRatingCount.setText(stats.getSecond() + " reviews");

        List<ProductReview> initialReviews = ProductReviewHelper.getReviews(productId, productName, category);
        allReviews.addAll(initialReviews);
        
        updateProgressBars();

        adapter = new ShopReviewAdapter(new ArrayList<>());
        binding.rvBottomSheetReviews.setAdapter(adapter);
        
        filterAndDisplayReviews();

        binding.tabLayoutStarFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: selectedStars = 0; break;
                    case 1: selectedStars = 5; break;
                    case 2: selectedStars = 4; break;
                    case 3: selectedStars = 3; break;
                    case 4: selectedStars = 2; break;
                    case 5: selectedStars = 1; break;
                    default: selectedStars = 0; break;
                }
                filterAndDisplayReviews();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        String finalProductName = productName;
        String finalCategory = category;
        binding.btnLoadMoreReviews.setOnClickListener(v -> {
            List<ProductReview> newReviews = ProductReviewHelper.getRandomReviews(finalProductName, finalCategory, 5);
            allReviews.addAll(newReviews);
            updateProgressBars();
            filterAndDisplayReviews();
        });
    }

    private void updateProgressBars() {
        int total = allReviews.size();
        if (total > 0) {
            int star5 = 0, star4 = 0, star3 = 0, star2 = 0, star1 = 0;
            for (ProductReview r : allReviews) {
                if (r.getRating() == 5) star5++;
                else if (r.getRating() == 4) star4++;
                else if (r.getRating() == 3) star3++;
                else if (r.getRating() == 2) star2++;
                else if (r.getRating() == 1) star1++;
            }

            binding.pbStar5.setProgress((star5 * 100) / total);
            binding.pbStar4.setProgress((star4 * 100) / total);
            binding.pbStar3.setProgress((star3 * 100) / total);
            binding.pbStar2.setProgress((star2 * 100) / total);
            binding.pbStar1.setProgress((star1 * 100) / total);
        }
    }

    private void filterAndDisplayReviews() {
        List<ProductReview> filtered = new ArrayList<>();
        if (selectedStars == 0) {
            filtered.addAll(allReviews);
        } else {
            for (ProductReview r : allReviews) {
                if (r.getRating() == selectedStars) {
                    filtered.add(r);
                }
            }
        }

        adapter.updateData(filtered);

        if (filtered.isEmpty()) {
            binding.rvBottomSheetReviews.setVisibility(View.GONE);
            binding.tvEmptyReviews.setVisibility(View.VISIBLE);
            if (selectedStars == 0) {
                binding.tvEmptyReviews.setText("Chưa có đánh giá nào cho sản phẩm này.");
            } else {
                binding.tvEmptyReviews.setText("Chưa có đánh giá " + selectedStars + " sao nào cho sản phẩm này.");
            }
            binding.btnLoadMoreReviews.setVisibility(View.GONE);
        } else {
            binding.rvBottomSheetReviews.setVisibility(View.VISIBLE);
            binding.tvEmptyReviews.setVisibility(View.GONE);
            if (filtered.size() >= 3) {
                binding.btnLoadMoreReviews.setVisibility(View.VISIBLE);
            } else {
                binding.btnLoadMoreReviews.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
