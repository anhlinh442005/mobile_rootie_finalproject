package com.veganbeauty.app.features.shop.product.detail;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ExploreVideoEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopProductDetailBinding;
import com.veganbeauty.app.features.community.beauty_hub.NotebookVideoAdapter;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.shop.product.CartBottomSheetFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.features.shop.product.ShopVoucherFragment;
import com.veganbeauty.app.features.shop.search.ShopSearchFragment;
import com.veganbeauty.app.features.shop.store.ShopStoreSystemFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import coil.Coil;
import coil.request.ImageRequest;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;

public class ShopDetailFragment extends RootieFragment {

    private ShopProductDetailBinding binding;

    private ProductEntity product = null;
    private String cartVoucherCode = null;
    private long cartVoucherDiscount = 0L;

    private NotebookVideoAdapter handbookAdapter;
    private ShopReviewAdapter reviewAdapter;
    private ShopHorizontalProductAdapter relatedAdapter;
    private ShopHorizontalProductAdapter recentlyViewedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        handbookAdapter = new NotebookVideoAdapter(new ArrayList<>());
        binding.rvHandbook.setAdapter(handbookAdapter);

        reviewAdapter = new ShopReviewAdapter(new ArrayList<>());
        binding.rvReviews.setAdapter(reviewAdapter);

        relatedAdapter = new ShopHorizontalProductAdapter(
                new ArrayList<>(),
                prod -> {
                    navigateToProduct(prod);
                    return kotlin.Unit.INSTANCE;
                },
                prod -> {
                    addToCart(prod);
                    return kotlin.Unit.INSTANCE;
                }
        );
        binding.rvRelatedProducts.setAdapter(relatedAdapter);

        recentlyViewedAdapter = new ShopHorizontalProductAdapter(
                new ArrayList<>(),
                prod -> {
                    navigateToProduct(prod);
                    return kotlin.Unit.INSTANCE;
                },
                prod -> {
                    addToCart(prod);
                    return kotlin.Unit.INSTANCE;
                }
        );
        binding.rvRecentlyViewed.setAdapter(recentlyViewedAdapter);

        binding.btnAllReviews.setOnClickListener(v -> {
            if (product != null) {
                ProductReviewsBottomSheet bottomSheet = ProductReviewsBottomSheet.Companion.newInstance(product.getId(), product.getName(), product.getCategory());
                bottomSheet.show(getParentFragmentManager(), ProductReviewsBottomSheet.TAG);
            }
        });

        binding.llRatingHeader.setOnClickListener(v ->
                binding.nsvDetail.post(() -> binding.nsvDetail.smoothScrollTo(0, binding.rlReviewsHeader.getTop()))
        );

        binding.btnSearch.setOnClickListener(v -> {
            ShopSearchFragment searchFragment = new ShopSearchFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, searchFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnCart.setOnClickListener(v -> {
            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.Companion.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            cartVoucherCode = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            cartVoucherDiscount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.Companion.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        binding.btnBuyOnline.setOnClickListener(v -> {
            if (product != null) {
                if (product.getStock() <= 0) {
                    Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
                    return;
                }
                ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(product, (prod, quantity) -> {
                    CartHelper.INSTANCE.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), prod, quantity);
                    return kotlin.Unit.INSTANCE;
                }, (prod, quantity) -> {
                    CartItemEntity checkoutItem = new CartItemEntity(prod.getId(), prod.getName(), prod.getMainImage(), prod.getPrice(), quantity, true);
                    ArrayList<CartItemEntity> list = new ArrayList<>();
                    list.add(checkoutItem);
                    ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.Companion.newInstance(list);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, checkoutFragment)
                            .addToBackStack(null)
                            .commit();
                    return kotlin.Unit.INSTANCE;
                });
                bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
            }
        });

        binding.btnFindStore.setOnClickListener(v -> {
            if (product != null && product.getStock() <= 0) {
                Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            ShopStoreSystemFragment storeFragment = new ShopStoreSystemFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, storeFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab != null) {
                    binding.llTabInfo.setVisibility(View.GONE);
                    binding.llTabIngredients.setVisibility(View.GONE);
                    binding.llTabStory.setVisibility(View.GONE);
                    binding.llTabBenefits.setVisibility(View.GONE);
                    binding.llTabUsage.setVisibility(View.GONE);

                    switch (tab.getPosition()) {
                        case 0: binding.llTabInfo.setVisibility(View.VISIBLE); break;
                        case 1: binding.llTabIngredients.setVisibility(View.VISIBLE); break;
                        case 2: binding.llTabStory.setVisibility(View.VISIBLE); break;
                        case 3: binding.llTabBenefits.setVisibility(View.VISIBLE); break;
                        case 4: binding.llTabUsage.setVisibility(View.VISIBLE); break;
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (product != null) {
            displayProduct(product);
        }
    }

    @Override
    public void observeViewModel() {
        super.observeViewModel();
        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    RootieDatabase db = RootieDatabase.getDatabase(requireContext());
                    db.cartDao().getAllCartItems().collect(new FlowCollector<List<CartItemEntity>>() {
                        @Nullable
                        @Override
                        public Object emit(List<CartItemEntity> items, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            int totalQty = 0;
                            for (CartItemEntity it : items) totalQty += it.getQuantity();
                            if (totalQty > 0) {
                                binding.tvCartBadge.setVisibility(View.VISIBLE);
                                binding.tvCartBadge.setText(String.valueOf(totalQty));
                            } else {
                                binding.tvCartBadge.setVisibility(View.GONE);
                            }
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
        if (binding != null) {
            displayProduct(product);
        }
    }

    private void displayProduct(ProductEntity product) {
        binding.tvProductName.setText(product.getName());

        if (product.getStock() <= 0) {
            binding.tvStockStatus.setText("Hết hàng");
            binding.tvStockStatus.setTextColor(Color.parseColor("#888888"));
            binding.viewStockIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        } else {
            binding.tvStockStatus.setText("Còn hàng");
            binding.tvStockStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.content));
            binding.viewStockIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvPrice.setText(formatter.format(product.getPrice()));

        long originalPrice = (long) (product.getPrice() * 1.2);
        binding.tvOriginalPrice.setText(formatter.format(originalPrice));
        binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        List<String> albumList = !product.getAlbum().isEmpty() ? product.getAlbum() : Arrays.asList(product.getMainImage());
        binding.vpProductImage.setAdapter(new ProductImageAdapter(albumList));
        setupIndicators(albumList.size());

        TabLayout.Tab firstTab = binding.tabLayout.getTabAt(0);
        if (firstTab != null) binding.tabLayout.selectTab(firstTab);
        binding.llTabInfo.setVisibility(View.VISIBLE);
        binding.llTabIngredients.setVisibility(View.GONE);
        binding.llTabStory.setVisibility(View.GONE);
        binding.llTabBenefits.setVisibility(View.GONE);
        binding.llTabUsage.setVisibility(View.GONE);

        binding.tvDescription.setText(product.getDescription());
        binding.tvSuitableFor.setText(product.getSuitableFor());
        binding.tvMainIngredientsSummary.setText(product.getMainIngredientsSummary());

        if (!product.getAllergyInformation().isEmpty()) {
            binding.cvAllergy.setVisibility(View.VISIBLE);
            binding.tvAllergyInformation.setText(product.getAllergyInformation());
        } else {
            binding.cvAllergy.setVisibility(View.VISIBLE);
            binding.tvAllergyInformation.setText("Sản phẩm chứa thành phần tự nhiên lành tính. Thử trên vùng da nhỏ trước khi sử dụng nếu bạn có cơ địa nhạy cảm.");
        }

        binding.llKeyIngredientsContainer.removeAllViews();
        if (product.getKeyIngredients().isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("Chưa cập nhật thông tin thành phần nổi bật.");
            emptyTv.setTextColor(Color.parseColor("#888888"));
            emptyTv.setTextSize(14f);
            emptyTv.setPadding(0, 8, 0, 8);
            binding.llKeyIngredientsContainer.addView(emptyTv);
        } else {
            for (IngredientEntity ingredient : product.getKeyIngredients()) {
                TextView titleTv = new TextView(requireContext());
                titleTv.setText("• " + ingredient.getName());
                titleTv.setTextColor(Color.parseColor("#333333"));
                titleTv.setTypeface(null, Typeface.BOLD);
                titleTv.setTextSize(14f);
                titleTv.setPadding(0, 8, 0, 4);

                TextView descTv = new TextView(requireContext());
                descTv.setText(ingredient.getDescription());
                descTv.setTextColor(Color.parseColor("#666666"));
                descTv.setTextSize(13f);
                descTv.setPadding(16, 0, 0, 8);

                binding.llKeyIngredientsContainer.addView(titleTv);
                binding.llKeyIngredientsContainer.addView(descTv);
            }
        }

        if (!product.getDetailedIngredients().isEmpty()) {
            binding.tvDetailedIngredients.setText(String.join(", ", product.getDetailedIngredients()));
        } else {
            binding.tvDetailedIngredients.setText("Chưa cập nhật bảng thành phần đầy đủ.");
        }

        if (!product.getStoryDescription().isEmpty()) {
            binding.tvStoryDescription.setText(product.getStoryDescription());
            if (!product.getStoryImage().isEmpty()) {
                binding.cvStoryImage.setVisibility(View.VISIBLE);
                ImageRequest req = new ImageRequest.Builder(requireContext())
                        .data(product.getStoryImage())
                        .crossfade(true)
                        .placeholder(android.R.color.darker_gray)
                        .target(binding.ivStoryImage)
                        .build();
                Coil.imageLoader(requireContext()).enqueue(req);
            } else {
                binding.cvStoryImage.setVisibility(View.GONE);
            }
        } else {
            binding.tvStoryDescription.setText("Câu chuyện về sản phẩm đang được cập nhật.");
            binding.cvStoryImage.setVisibility(View.GONE);
        }

        binding.llIdealForContainer.removeAllViews();
        if (product.getIdealFor().isEmpty()) {
            binding.tvIdealForHeader.setVisibility(View.GONE);
        } else {
            binding.tvIdealForHeader.setVisibility(View.VISIBLE);
            for (String ideal : product.getIdealFor()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);
                row.setGravity(Gravity.CENTER_VERTICAL);

                ImageView icon = new ImageView(requireContext());
                int sizePx = (int) (16 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                icon.setLayoutParams(lp);
                icon.setImageResource(R.drawable.ic_check);
                icon.setColorFilter(Color.parseColor("#455B49"));

                TextView text = new TextView(requireContext());
                text.setText(ideal);
                text.setTextColor(Color.parseColor("#444444"));
                text.setTextSize(14f);

                row.addView(icon);
                row.addView(text);
                binding.llIdealForContainer.addView(row);
            }
        }

        binding.llBenefitsContainer.removeAllViews();
        if (product.getBenefits().isEmpty()) {
            binding.tvBenefitsHeader.setVisibility(View.GONE);
        } else {
            binding.tvBenefitsHeader.setVisibility(View.VISIBLE);
            for (String benefit : product.getBenefits()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);
                row.setGravity(Gravity.CENTER_VERTICAL);

                ImageView icon = new ImageView(requireContext());
                int sizePx = (int) (16 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                icon.setLayoutParams(lp);
                icon.setImageResource(R.drawable.ic_check);
                icon.setColorFilter(Color.parseColor("#455B49"));

                TextView text = new TextView(requireContext());
                text.setText(benefit);
                text.setTextColor(Color.parseColor("#444444"));
                text.setTextSize(14f);

                row.addView(icon);
                row.addView(text);
                binding.llBenefitsContainer.addView(row);
            }
        }

        if (!product.getUsage().isEmpty()) {
            binding.tvUsage.setText(product.getUsage());
        } else {
            binding.tvUsage.setText("Chưa cập nhật hướng dẫn sử dụng sản phẩm.");
        }

        if (!product.getUsageAmount().isEmpty()) {
            binding.cvUsageAmount.setVisibility(View.VISIBLE);
            binding.tvUsageAmount.setText(product.getUsageAmount());
        } else {
            binding.cvUsageAmount.setVisibility(View.GONE);
        }

        if (!product.getScent().isEmpty()) {
            binding.cvScent.setVisibility(View.VISIBLE);
            binding.tvScent.setText(product.getScent());
        } else {
            binding.cvScent.setVisibility(View.GONE);
        }

        if (!product.getNotes().isEmpty()) {
            binding.cvNotes.setVisibility(View.VISIBLE);
            binding.tvNotes.setText(product.getNotes());
        } else {
            binding.cvNotes.setVisibility(View.GONE);
        }

        kotlin.Pair<Double, Integer> ratingStats = ProductReviewHelper.INSTANCE.getRatingStats(product.getId());
        double averageRating = ratingStats.getFirst();
        int totalReviews = ratingStats.getSecond();
        binding.tvRatingValue.setText(String.format(Locale.US, "%.1f", averageRating));
        binding.tvReviewsCount.setText("(" + totalReviews + " reviews)");

        List<ProductReviewHelper.ReviewData> reviews = ProductReviewHelper.INSTANCE.getReviews(product.getId(), product.getName(), product.getCategory());
        reviewAdapter.updateData(reviews.subList(0, Math.min(3, reviews.size())));

        SharedPreferences sharedPrefs = requireContext().getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE);
        String recentlyViewedStr = sharedPrefs.getString("recently_viewed_ids", "");
        if (recentlyViewedStr == null) recentlyViewedStr = "";
        List<String> idList = new ArrayList<>(Arrays.asList(recentlyViewedStr.split(",")));
        idList.remove("");
        idList.remove(product.getId());
        idList.add(0, product.getId());
        List<String> limitedList = idList.subList(0, Math.min(10, idList.size()));
        sharedPrefs.edit().putString("recently_viewed_ids", String.join(",", limitedList)).apply();

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    RootieDatabase db = RootieDatabase.getDatabase(requireContext());
                    db.productDao().getAllProducts().collect(new FlowCollector<List<ProductEntity>>() {
                        @Nullable
                        @Override
                        public Object emit(List<ProductEntity> allProducts, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            List<ProductEntity> otherProducts = new ArrayList<>();
                            for (ProductEntity p : allProducts) {
                                if (!p.getId().equals(product.getId())) otherProducts.add(p);
                            }

                            List<String> currentSubcategories = new ArrayList<>();
                            for (String s : product.getCategoryIds().split(",")) {
                                if (!s.isEmpty()) currentSubcategories.add(s);
                            }

                            List<String> subcategoryIds = Arrays.asList(
                                    "f5877af6a55f88bcf57c17b4", "389971929086b2ce7fba9dd0", "36cbf3f5c4b7a299ce2a2d0c",
                                    "4e20d6bbc1203015ee2ecd48", "b1b6cd208332d4f1e015a26c", "7667d982515426a9d88b787b",
                                    "bb88a3306cf95af20d073594", "9882d5fa14c74dd053e17f33", "c211afa24702f5d1ff86fe42",
                                    "7c70e845e829b374e57ee7b1", "b703bb813e660aa88076ee5a", "8fce5340c618672aa1ae7fb3",
                                    "24a75aa9d541feed638b1970", "755731e01d8c579c633ae4d2", "ded17e0716783c133b1a5b9a"
                            );

                            List<String> productSubcategories = new ArrayList<>();
                            List<String> productParentCategories = new ArrayList<>();
                            for (String c : currentSubcategories) {
                                if (subcategoryIds.contains(c)) productSubcategories.add(c);
                                else productParentCategories.add(c);
                            }

                            List<ProductEntity> subcategoryMatches = new ArrayList<>();
                            for (ProductEntity other : otherProducts) {
                                boolean match = false;
                                for (String id : other.getCategoryIds().split(",")) {
                                    if (productSubcategories.contains(id)) { match = true; break; }
                                }
                                if (match) subcategoryMatches.add(other);
                            }

                            List<ProductEntity> parentMatches = new ArrayList<>();
                            for (ProductEntity other : otherProducts) {
                                if (subcategoryMatches.contains(other)) continue;
                                boolean match = false;
                                for (String id : other.getCategoryIds().split(",")) {
                                    if (productParentCategories.contains(id)) { match = true; break; }
                                }
                                if (match) parentMatches.add(other);
                            }

                            List<ProductEntity> generalCategoryMatches = new ArrayList<>();
                            for (ProductEntity other : otherProducts) {
                                if (subcategoryMatches.contains(other) || parentMatches.contains(other)) continue;
                                if (other.getCategory().equalsIgnoreCase(product.getCategory())) {
                                    generalCategoryMatches.add(other);
                                }
                            }

                            List<ProductEntity> finalRelated = new ArrayList<>(subcategoryMatches);
                            finalRelated.addAll(parentMatches);
                            finalRelated.addAll(generalCategoryMatches);
                            relatedAdapter.updateData(finalRelated.subList(0, Math.min(8, finalRelated.size())));

                            String currentViewedStr = sharedPrefs.getString("recently_viewed_ids", "");
                            if (currentViewedStr == null) currentViewedStr = "";
                            List<String> currentIds = new ArrayList<>();
                            for (String s : currentViewedStr.split(",")) {
                                if (!s.isEmpty()) currentIds.add(s);
                            }

                            List<ProductEntity> viewedProducts = new ArrayList<>();
                            for (String id : currentIds) {
                                if (id.equals(product.getId())) continue;
                                for (ProductEntity p : allProducts) {
                                    if (p.getId().equals(id)) { viewedProducts.add(p); break; }
                                }
                            }

                            recentlyViewedAdapter.updateData(viewedProducts);
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    RootieDatabase db = RootieDatabase.getDatabase(requireContext());
                    db.communityDao().getAllExploreVideos().collect(new FlowCollector<List<ExploreVideoEntity>>() {
                        @Nullable
                        @Override
                        public Object emit(List<ExploreVideoEntity> allVideos, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            List<ExploreVideoEntity> videos = allVideos;
                            if (videos.isEmpty()) {
                                LocalJsonReader localReader = new LocalJsonReader(requireContext());
                                List<ExploreVideoEntity> localVideos = localReader.getExploreVideos();
                                if (!localVideos.isEmpty()) {
                                    LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((s3, c3) -> {
                                        db.communityDao().insertExploreVideos(localVideos);
                                        return kotlin.Unit.INSTANCE;
                                    });
                                }
                                videos = localVideos;
                            }

                            List<ExploreVideoEntity> filteredVideos = new ArrayList<>();
                            for (ExploreVideoEntity video : videos) {
                                if (video.getType().toLowerCase().contains("notebook")) filteredVideos.add(video);
                            }

                            List<Pair<ExploreVideoEntity, Integer>> rankedVideos = new ArrayList<>();
                            for (ExploreVideoEntity video : filteredVideos) {
                                int score = 0;
                                String textToSearch = (video.getTitle() + " " + video.getDescription()).toLowerCase();
                                List<String> ingredients = Arrays.asList("bí đao", "nghệ", "cà phê", "bưởi", "hoa hồng", "dừa", "tràm trà", "sen", "rau má", "bồ kết");
                                for (String ing : ingredients) {
                                    if (product.getName().toLowerCase().contains(ing)) {
                                        if (textToSearch.contains(ing)) score += 10;
                                        if (ing.equals("bí đao") && (textToSearch.contains("mụn") || textToSearch.contains("thâm"))) score += 5;
                                        if (ing.equals("nghệ") && (textToSearch.contains("sáng da") || textToSearch.contains("thâm") || textToSearch.contains("curcumin"))) score += 5;
                                        if (ing.equals("bưởi") && (textToSearch.contains("tóc") || textToSearch.contains("rụng") || textToSearch.contains("gội"))) score += 5;
                                        if (ing.equals("cà phê") && (textToSearch.contains("tẩy tế bào chết") || textToSearch.contains("body") || textToSearch.contains("scrub"))) score += 5;
                                    }
                                }

                                if (product.getCategory().toLowerCase().contains("da") || product.getCategoryIds().contains("7176b5e7966be88daf95cfd4")) {
                                    if (textToSearch.contains("skincare") || textToSearch.contains("da") || textToSearch.contains("mặt")) {
                                        score += 2;
                                    }
                                }
                                rankedVideos.add(new Pair<>(video, score));
                            }

                            Collections.sort(rankedVideos, (o1, o2) -> Integer.compare(o2.second, o1.second));

                            List<ExploreVideoEntity> finalVideos = new ArrayList<>();
                            for (int i = 0; i < Math.min(4, rankedVideos.size()); i++) {
                                finalVideos.add(rankedVideos.get(i).first);
                            }
                            handbookAdapter.updateData(finalVideos);
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));

        checkProductCompatibility(product);
    }

    private void checkProductCompatibility(ProductEntity product) {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        boolean hasQuiz = prefs.contains("SAVED_USER_SKIN_TYPE");

        if (!hasQuiz) {
            binding.cvSkinCompatibility.setVisibility(View.VISIBLE);
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#FEFBF4"));
            binding.cvSkinCompatibility.setStrokeColor(Color.parseColor("#DDDFC4"));
            binding.ivCompatibilityIcon.setImageResource(R.drawable.quiz_ic_sparkles);
            binding.ivCompatibilityIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            binding.tvCompatibilityTitle.setText("Kiểm tra độ phù hợp làn da");
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#677559"));
            binding.tvCompatibilitySubtitle.setText("Làm quiz test da để nhận phân tích chi tiết độ phù hợp của sản phẩm này.");
            binding.flIrritatingPills.setVisibility(View.GONE);
            binding.vCompatibilityDivider.setVisibility(View.GONE);
            binding.llCompatibilityReasons.setVisibility(View.GONE);
            binding.cvSkinCompatibility.setOnClickListener(v ->
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, new QuizTestIntroFragment())
                            .addToBackStack(null)
                            .commit()
            );
            return;
        }

        binding.cvSkinCompatibility.setOnClickListener(null);
        Set<String> flaggedSet = prefs.getStringSet("SAVED_FLAGGED_GROUPS", new HashSet<>());

        Set<String> avoidChemicals = new HashSet<>();
        Set<String> cautionChemicals = new HashSet<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getAssets().open("quiz_thanhphan.json")));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject tpObject = new JSONObject(sb.toString());
            JSONArray tpArray = tpObject.getJSONArray("ingredients");

            for (int i = 0; i < tpArray.length(); i++) {
                JSONObject ing = tpArray.getJSONObject(i);
                String name = ing.getString("name");
                String category = ing.getString("category");
                String risk = ing.getString("risk");

                if (flaggedSet.contains(category)) {
                    if ("avoid".equals(risk)) avoidChemicals.add(name.toLowerCase());
                    else if ("caution".equals(risk)) cautionChemicals.add(name.toLowerCase());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        List<String> detailedIngredientsList = new ArrayList<>();
        for (String s : product.getDetailedIngredients()) detailedIngredientsList.add(s.toLowerCase());

        List<String> triggeredAvoids = new ArrayList<>();
        List<String> triggeredCautions = new ArrayList<>();

        for (String chem : avoidChemicals) {
            for (String ing : detailedIngredientsList) {
                if (ing.contains(chem)) triggeredAvoids.add(getViName(chem));
            }
        }

        for (String chem : cautionChemicals) {
            for (String ing : detailedIngredientsList) {
                if (ing.contains(chem)) triggeredCautions.add(getViName(chem));
            }
        }

        binding.cvSkinCompatibility.setVisibility(View.VISIBLE);

        if (!triggeredAvoids.isEmpty() || !triggeredCautions.isEmpty()) {
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#FFF2DF"));
            binding.cvSkinCompatibility.setStrokeColor(Color.parseColor("#D2945D"));
            binding.ivCompatibilityIcon.setImageResource(R.drawable.quiz_ic_warning_triangle);
            binding.ivCompatibilityIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FE851A")));

            binding.tvCompatibilityTitle.setText("Cảnh báo thành phần không phù hợp");
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#FE851A"));

            Set<String> uniqueAvoidsAndCautions = new HashSet<>(triggeredAvoids);
            uniqueAvoidsAndCautions.addAll(triggeredCautions);
            int totalCount = uniqueAvoidsAndCautions.size();
            binding.tvCompatibilitySubtitle.setText("Sản phẩm chứa " + totalCount + " thành phần gây kích ứng cho làn da của bạn.");

            binding.flIrritatingPills.setVisibility(View.VISIBLE);
            binding.flIrritatingPills.removeAllViews();

            for (String name : uniqueAvoidsAndCautions) {
                TextView pillView = (TextView) LayoutInflater.from(ctx).inflate(R.layout.quiz_item_pill, binding.flIrritatingPills, false);
                pillView.setText(name);
                pillView.setBackgroundResource(R.drawable.quiz_bg_pill_avoid);
                pillView.setTextColor(Color.parseColor("#EB862D"));
                binding.flIrritatingPills.addView(pillView);
            }

            binding.vCompatibilityDivider.setVisibility(View.VISIBLE);
            binding.llCompatibilityReasons.setVisibility(View.VISIBLE);

            Set<String> uniqueAvoids = new HashSet<>(triggeredAvoids);
            int avoidCount = uniqueAvoids.size();
            if (avoidCount > 0) {
                binding.llReason1.setVisibility(View.VISIBLE);
                binding.tvReason1Title.setText(avoidCount + " thành phần có nguy cơ kích ứng cao");
                binding.tvReason1Desc.setText("Dựa trên hồ sơ dị ứng và biểu hiện da của bạn.");
            } else {
                binding.llReason1.setVisibility(View.GONE);
            }

            String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "");
            if (skinType != null && skinType.toLowerCase().contains("nhạy cảm")) {
                binding.llReason2.setVisibility(View.VISIBLE);
                binding.tvReason2Title.setText("Làn da đang nhạy cảm");
                binding.tvReason2Desc.setText("Hồ sơ da của bạn cho thấy hàng rào bảo vệ da đang yếu.");
            } else {
                binding.llReason2.setVisibility(View.GONE);
            }
        } else {
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#EDF3ED"));
            binding.cvSkinCompatibility.setStrokeColor(Color.parseColor("#A2B5A2"));
            binding.ivCompatibilityIcon.setImageResource(R.drawable.quiz_ic_wavy_check);
            binding.ivCompatibilityIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#12B76A")));

            binding.tvCompatibilityTitle.setText("Sản phẩm phù hợp với da của bạn");
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#67814D"));
            binding.tvCompatibilitySubtitle.setText("Bảng thành phần cực kỳ lành tính và hoàn toàn phù hợp với nền da hiện tại.");

            binding.flIrritatingPills.setVisibility(View.GONE);
            binding.vCompatibilityDivider.setVisibility(View.GONE);
            binding.llCompatibilityReasons.setVisibility(View.GONE);
        }
    }

    private String getViName(String chemicalName) {
        String lower = chemicalName.toLowerCase();
        switch (lower) {
            case "alcohol denat": case "ethanol": return "Alcohol";
            case "fragrance": return "Fragrance";
            case "parfum": return "Parfum";
            case "essential oil": return "Tinh dầu";
            case "sodium lauryl sulfate": return "SLS";
            case "sodium laureth sulfate": return "Sulfate";
            case "cocamidopropyl betaine": return "Cocamidopropyl";
            case "retinol": return "Retinol";
            case "salicylic acid": return "BHA";
            case "glycolic acid": return "AHA";
            case "phenoxyethanol": case "paraben": return "Paraben";
            case "propylene glycol": return "Propylene Glycol";
            case "glycerin": return "Glycerin";
            case "hyaluronic acid": return "HA";
            case "centella asiatica": return "Rau má";
            case "green tea": return "Trà xanh";
            case "aloe vera": return "Nha đam";
            case "silicone": return "Silicone";
            case "petrolatum": return "Petrolatum";
            default:
                if (!chemicalName.isEmpty()) {
                    return chemicalName.substring(0, 1).toUpperCase() + chemicalName.substring(1);
                }
                return chemicalName;
        }
    }

    private void setupIndicators(int count) {
        binding.llIndicatorContainer.removeAllViews();
        if (count <= 1) return;
        List<ImageView> indicators = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int size = (i == 0) ? 8 : 6;
            int sizePx = (int) (size * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(4, 0, 4, 0);
            ImageView imageView = new ImageView(requireContext());
            imageView.setImageResource((i == 0) ? R.drawable.bg_circle_green : R.drawable.bg_circle_grey);
            indicators.add(imageView);
            binding.llIndicatorContainer.addView(imageView, lp);
        }

        binding.vpProductImage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < count; i++) {
                    int size = (i == position) ? 8 : 6;
                    int sizePx = (int) (size * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMargins(4, 0, 4, 0);
                    indicators.get(i).setLayoutParams(lp);
                    indicators.get(i).setImageResource((i == position) ? R.drawable.bg_circle_green : R.drawable.bg_circle_grey);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.ViewHolder> {
        private final List<String> images;

        public ProductImageAdapter(List<String> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext()).inflate(R.layout.shop_item_product_image, parent, false);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageRequest req = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(images.get(position))
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .target(holder.imageView)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(req);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public ViewHolder(@NonNull ImageView itemView) {
                super(itemView);
                this.imageView = itemView;
            }
        }
    }

    private void navigateToProduct(ProductEntity prod) {
        ShopDetailFragment detailFragment = new ShopDetailFragment();
        detailFragment.setProduct(prod);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void addToCart(ProductEntity prod) {
        CartHelper.INSTANCE.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), prod, 1);
    }
}
