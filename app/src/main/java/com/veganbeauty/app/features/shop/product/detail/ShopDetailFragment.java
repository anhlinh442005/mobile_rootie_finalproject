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
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
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
import com.veganbeauty.app.utils.ProductImageCache;
import com.veganbeauty.app.utils.ProductImageHelper;

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

public class ShopDetailFragment extends RootieFragment {

    private static final String ARG_PRODUCT_ID = "PRODUCT_ID";

    private ShopProductDetailBinding binding;

    private ProductEntity product = null;
    private String pendingProductId = null;
    private List<ProductEntity> cachedAllProducts = null;
    private String cartVoucherCode = null;
    private long cartVoucherDiscount = 0L;

    private NotebookVideoAdapter handbookAdapter;
    private ShopReviewAdapter reviewAdapter;
    private ShopHorizontalProductAdapter relatedAdapter;
    private ShopHorizontalProductAdapter recentlyViewedAdapter;
    private ShopProductDetailVoucherAdapter voucherAdapter;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    private boolean isHeaderVisible = true;

    public static ShopDetailFragment newInstance(String productId) {
        ShopDetailFragment fragment = new ShopDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pendingProductId = args.getString(ARG_PRODUCT_ID);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh product stock from Firestore whenever fragment resumes (e.g., after checkout)
        if (pendingProductId != null && !pendingProductId.isEmpty()) {
            refreshProductStockFromFirestore(pendingProductId);
        }
    }

    private void refreshProductStockFromFirestore(String productId) {
        new Thread(() -> {
            ProductEntity refreshed = null;
            try {
                refreshed = new com.veganbeauty.app.data.remote.FirestoreService().fetchProductById(productId);
                android.content.Context ctx = getContext();
                if (refreshed != null && ctx != null) {
                    RootieDatabase.getDatabase(ctx.getApplicationContext()).productDao().insertProducts(
                        java.util.Collections.singletonList(refreshed)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final ProductEntity finalRefreshed = refreshed;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) return;
                if (finalRefreshed != null) {
                    product = finalRefreshed;
                    bindProductSafely(finalRefreshed);
                }
            });
        }).start();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = ShopProductDetailBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            e.printStackTrace();
            Context ctx = getContext();
            if (ctx == null) ctx = inflater.getContext();
            android.widget.FrameLayout fallback = new android.widget.FrameLayout(ctx);
            android.widget.TextView message = new android.widget.TextView(ctx);
            message.setText("Không thể tải trang sản phẩm");
            message.setPadding(48, 48, 48, 48);
            fallback.addView(message);
            return fallback;
        }
    }

    @Override
    public void setupUI(View view) {
        try {
            setupDetailUi(view);
        } catch (Exception e) {
            e.printStackTrace();
            Context ctx = getContext();
            if (ctx != null) {
                Toast.makeText(ctx, "Không thể mở trang sản phẩm", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupDetailUi(View view) {
        if (binding == null) return;

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        voucherAdapter = new ShopProductDetailVoucherAdapter(new ArrayList<>());
        binding.rvVouchersProductDetail.setAdapter(voucherAdapter);

        handbookAdapter = new NotebookVideoAdapter(new ArrayList<>());
        binding.rvHandbook.setAdapter(handbookAdapter);

        reviewAdapter = new ShopReviewAdapter(new ArrayList<>());
        binding.rvReviews.setAdapter(reviewAdapter);

        relatedAdapter = new ShopHorizontalProductAdapter(
                new ArrayList<>(),
                prod -> {
                    navigateToProduct(prod);
                },
                prod -> {
                    showChooseQuantityBottomSheet(prod);
                }
        );
        binding.rvRelatedProducts.setAdapter(relatedAdapter);

        recentlyViewedAdapter = new ShopHorizontalProductAdapter(
                new ArrayList<>(),
                prod -> {
                    navigateToProduct(prod);
                },
                prod -> {
                    showChooseQuantityBottomSheet(prod);
                }
        );
        binding.rvRecentlyViewed.setAdapter(recentlyViewedAdapter);

        binding.btnAllReviews.setOnClickListener(v -> {
            if (product != null) {
                ProductReviewsBottomSheet bottomSheet = ProductReviewsBottomSheet.newInstance(product.getId(), product.getName(), product.getCategory());
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
            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            cartVoucherCode = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            cartVoucherDiscount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        binding.btnBuyOnline.setOnClickListener(v -> {
            if (product != null) {
                showChooseQuantityBottomSheet(product);
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

        binding.nsvDetail.setOnScrollChangeListener(new androidx.core.widget.NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int dy = scrollY - oldScrollY;

                if (scrollY <= 0) {
                    showHeader();
                } else if (dy > 10 && isHeaderVisible) {
                    hideHeader();
                } else if (dy < -10 && !isHeaderVisible) {
                    showHeader();
                }
            }
        });

        loadProductAsync();
    }

    private void hideHeader() {
        if (binding == null || !isHeaderVisible) return;
        isHeaderVisible = false;
        float translationY = -binding.clHeader.getHeight();
        if (translationY == 0) {
            float density = getResources().getDisplayMetrics().density;
            translationY = -52 * density;
        }
        binding.clHeader.animate()
                .translationY(translationY)
                .setDuration(200)
                .start();
    }

    private void showHeader() {
        if (binding == null || isHeaderVisible) return;
        isHeaderVisible = true;
        binding.clHeader.animate()
                .translationY(0)
                .setDuration(200)
                .start();
    }

    private void loadProductAsync() {
        if (product != null) {
            // Even if product is not null, let's refresh it from DB to get the latest stock in case it changed
            if (product.getId() != null) {
                pendingProductId = product.getId();
            }
        }
        if (pendingProductId == null || pendingProductId.isEmpty()) {
            if (product != null) {
                bindProductSafely(product);
            } else {
                showProductNotFound();
            }
            return;
        }

        binding.tvProductName.setText("Đang tải sản phẩm...");

        new Thread(() -> {
            ProductEntity resolved = null;
            try {
                Context ctx = getContext();
                if (ctx != null) {
                    ProductImageCache.preload(ctx.getApplicationContext());
                    resolved = new com.veganbeauty.app.data.remote.FirestoreService().fetchProductById(pendingProductId);
                    if (resolved != null) {
                        RootieDatabase.getDatabase(ctx.getApplicationContext()).productDao().insertProducts(java.util.Collections.singletonList(resolved));
                    } else {
                        resolved = RootieDatabase.getDatabase(ctx.getApplicationContext()).productDao().getProductById(pendingProductId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            ProductEntity finalProduct = resolved;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || binding == null) return;
                if (finalProduct != null) {
                    product = finalProduct;
                    bindProductSafely(finalProduct);
                } else {
                    showProductNotFound();
                }
            });
        }).start();
    }

    private void bindProductSafely(ProductEntity loadedProduct) {
        try {
            displayProduct(loadedProduct);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                displayMinimalProduct(loadedProduct);
            } catch (Exception inner) {
                inner.printStackTrace();
                Context ctx = getContext();
                if (ctx != null) {
                    Toast.makeText(ctx, "Không thể hiển thị đầy đủ thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void displayMinimalProduct(ProductEntity product) {
        if (binding == null || product == null) return;
        binding.tvProductName.setText(safeStr(product.getName()));
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvPrice.setText(formatter.format(product.getPrice()));
        List<String> albumList = new ArrayList<>();
        String mainImage = safeStr(product.getMainImage());
        if (!ProductImageCache.isValidImageUrl(mainImage) && getContext() != null) {
            mainImage = ProductImageCache.getImageUrl(getContext(), product.getId(), product.getSku());
        }
        if (ProductImageCache.isValidImageUrl(mainImage)) {
            albumList.add(mainImage);
        }
        if (albumList.isEmpty()) {
            albumList.add("");
        }
        binding.vpProductImage.setAdapter(new ProductImageAdapter(albumList));
    }

    private void showProductNotFound() {
        if (binding == null) return;
        binding.tvProductName.setText("Không tìm thấy sản phẩm");
        Context ctx = getContext();
        if (ctx != null) {
            Toast.makeText(ctx, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void observeViewModel() {
        super.observeViewModel();
        Context ctx = getContext();
        if (ctx == null || binding == null) return;

        try {
            FlowLiveDataConversions.asLiveData(
                    RootieDatabase.getDatabase(ctx).cartDao().getAllCartItems()
            ).observe(getViewLifecycleOwner(), this::updateCartBadge);

            FlowLiveDataConversions.asLiveData(
                    RootieDatabase.getDatabase(ctx).productDao().getAllProducts()
            ).observe(getViewLifecycleOwner(), allProducts -> {
                try {
                    cachedAllProducts = allProducts;
                    if (product != null && binding != null && allProducts != null) {
                        updateRelatedProducts(allProducts, product);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            FlowLiveDataConversions.asLiveData(
                    RootieDatabase.getDatabase(ctx).userGiftDao().getAllUserGiftsFlow()
            ).observe(getViewLifecycleOwner(), dbGifts -> {
                try {
                    List<com.veganbeauty.app.data.local.entities.UserGiftEntity> activeVouchers = new ArrayList<>();
                    if (dbGifts != null) {
                        for (com.veganbeauty.app.data.local.entities.UserGiftEntity gift : dbGifts) {
                            if ("voucher_discount".equals(gift.getGiftType()) || "voucher_freeship".equals(gift.getGiftType())) {
                                String status = gift.getStatus();
                                if ("Còn hạn".equalsIgnoreCase(status) || "Hôm nay".equalsIgnoreCase(status) ||
                                        "valid".equalsIgnoreCase(status) || "expiring".equalsIgnoreCase(status)) {
                                    activeVouchers.add(gift);
                                }
                            }
                        }
                    }
                    if (activeVouchers.isEmpty()) {
                        activeVouchers.add(new com.veganbeauty.app.data.local.entities.UserGiftEntity(
                                0, "voucher_50k", "Voucher Giảm 50K", "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.",
                                500, "2026-12-30 23:59:59", "Còn hạn", "voucher_discount", "SAVE50K",
                                300000, "Chăm Sóc Da Mặt", "fixed_amount", null, 50000, 1775831400000L
                        ));
                        activeVouchers.add(new com.veganbeauty.app.data.local.entities.UserGiftEntity(
                                0, "gift_freeship", "Freeship Đơn 150K", "Miễn phí vận chuyển toàn quốc cho đơn hàng từ 150K.",
                                200, "2026-12-31 23:59:59", "Còn hạn", "voucher_freeship", "FREESHIP",
                                150000, "Tất cả sản phẩm", "percentage", null, 100, 1772698500000L
                        ));
                        activeVouchers.add(new com.veganbeauty.app.data.local.entities.UserGiftEntity(
                                0, "voucher_10_percent", "Giảm 10% Cho Bạn Mới", "Áp dụng cho tất cả các sản phẩm trên Rootie.",
                                300, "2026-12-31 23:59:59", "Còn hạn", "voucher_discount", "ROOTIE10",
                                0, "Tất cả sản phẩm", "percentage", null, 10, 1767261600000L
                        ));
                    }
                    if (binding != null) {
                        binding.rlVoucherHeader.setVisibility(View.VISIBLE);
                        binding.rvVouchersProductDetail.setVisibility(View.VISIBLE);
                        voucherAdapter.updateList(activeVouchers);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCartBadge(List<CartItemEntity> items) {
        if (binding == null) return;
        int totalQty = 0;
        if (items != null) {
            for (CartItemEntity it : items) totalQty += it.getQuantity();
        }
        if (totalQty > 0) {
            binding.tvCartBadge.setVisibility(View.VISIBLE);
            binding.tvCartBadge.setText(String.valueOf(totalQty));
        } else {
            binding.tvCartBadge.setVisibility(View.GONE);
        }
    }

    private static String safeStr(String value) {
        return value != null ? value : "";
    }

    private static List<String> safeList(List<String> list) {
        return list != null ? list : Collections.emptyList();
    }

    private static List<com.veganbeauty.app.data.local.entities.KeyIngredient> safeKeyIngredients(
            List<com.veganbeauty.app.data.local.entities.KeyIngredient> list) {
        return list != null ? list : Collections.emptyList();
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
        if (product != null && product.getId() != null) {
            this.pendingProductId = product.getId();
        }
        if (binding != null && product != null) {
            bindProductSafely(product);
        }
    }

    private void displayProduct(ProductEntity product) {
        if (binding == null || product == null || !isAdded()) return;
        Context ctx = getContext();
        if (ctx == null) return;

        binding.tvProductName.setText(safeStr(product.getName()));

        if (product.getStock() <= 0) {
            binding.tvStockStatus.setText("Hết hàng");
            binding.tvStockStatus.setTextColor(Color.parseColor("#888888"));
            binding.viewStockIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#888888")));
        } else {
            binding.tvStockStatus.setText("Còn hàng (Tồn kho: " + product.getStock() + ")");
            binding.tvStockStatus.setTextColor(ContextCompat.getColor(ctx, R.color.content));
            binding.viewStockIndicator.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        binding.tvPrice.setText(formatter.format(product.getPrice()));

        long originalPrice = (long) (product.getPrice() * 1.2);
        binding.tvOriginalPrice.setText(formatter.format(originalPrice));
        binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        List<String> album = safeList(product.getAlbum());
        String mainImage = safeStr(product.getMainImage());
        if (!ProductImageCache.isValidImageUrl(mainImage) && getContext() != null) {
            mainImage = ProductImageCache.getImageUrl(getContext(), product.getId(), product.getSku());
        }
        List<String> albumList = new ArrayList<>();
        for (String url : album) {
            if (ProductImageCache.isValidImageUrl(url)) {
                albumList.add(url.trim());
            }
        }
        if (albumList.isEmpty() && ProductImageCache.isValidImageUrl(mainImage)) {
            albumList.add(mainImage);
        }
        if (albumList.isEmpty()) {
            albumList.add("");
        }
        binding.vpProductImage.setAdapter(new ProductImageAdapter(albumList));
        setupIndicators(albumList.size());

        TabLayout.Tab firstTab = binding.tabLayout.getTabAt(0);
        if (firstTab != null) binding.tabLayout.selectTab(firstTab);
        binding.llTabInfo.setVisibility(View.VISIBLE);
        binding.llTabIngredients.setVisibility(View.GONE);
        binding.llTabStory.setVisibility(View.GONE);
        binding.llTabBenefits.setVisibility(View.GONE);
        binding.llTabUsage.setVisibility(View.GONE);

        binding.tvDescription.setText(safeStr(product.getDescription()));
        binding.tvSuitableFor.setText(safeStr(product.getSuitableFor()));
        binding.tvMainIngredientsSummary.setText(safeStr(product.getMainIngredientsSummary()));

        String allergyInfo = safeStr(product.getAllergyInformation());
        if (!allergyInfo.isEmpty()) {
            binding.cvAllergy.setVisibility(View.VISIBLE);
            binding.tvAllergyInformation.setText(allergyInfo);
        } else {
            binding.cvAllergy.setVisibility(View.VISIBLE);
            binding.tvAllergyInformation.setText("Sản phẩm chứa thành phần tự nhiên lành tính. Thử trên vùng da nhỏ trước khi sử dụng nếu bạn có cơ địa nhạy cảm.");
        }

        binding.llKeyIngredientsContainer.removeAllViews();
        List<com.veganbeauty.app.data.local.entities.KeyIngredient> keyIngredients = safeKeyIngredients(product.getKeyIngredients());
        if (keyIngredients.isEmpty()) {
            TextView emptyTv = new TextView(ctx);
            emptyTv.setText("Chưa cập nhật thông tin thành phần nổi bật.");
            emptyTv.setTextColor(Color.parseColor("#888888"));
            emptyTv.setTextSize(14f);
            emptyTv.setPadding(0, 8, 0, 8);
            binding.llKeyIngredientsContainer.addView(emptyTv);
        } else {
            for (com.veganbeauty.app.data.local.entities.KeyIngredient ingredient : keyIngredients) {
                TextView titleTv = new TextView(ctx);
                titleTv.setText("• " + safeStr(ingredient.getName()));
                titleTv.setTextColor(Color.parseColor("#333333"));
                titleTv.setTypeface(null, Typeface.BOLD);
                titleTv.setTextSize(14f);
                titleTv.setPadding(0, 8, 0, 4);

                TextView descTv = new TextView(ctx);
                descTv.setText(safeStr(ingredient.getDescription()));
                descTv.setTextColor(Color.parseColor("#666666"));
                descTv.setTextSize(13f);
                descTv.setPadding(16, 0, 0, 8);

                binding.llKeyIngredientsContainer.addView(titleTv);
                binding.llKeyIngredientsContainer.addView(descTv);
            }
        }

        List<String> detailedIngredients = safeList(product.getDetailedIngredients());
        if (!detailedIngredients.isEmpty()) {
            binding.tvDetailedIngredients.setText(String.join(", ", detailedIngredients));
        } else {
            binding.tvDetailedIngredients.setText("Chưa cập nhật bảng thành phần đầy đủ.");
        }

        String storyDescription = safeStr(product.getStoryDescription());
        if (!storyDescription.isEmpty()) {
            binding.tvStoryDescription.setText(storyDescription);
            String storyImage = safeStr(product.getStoryImage());
            if (!storyImage.isEmpty()) {
                binding.cvStoryImage.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(binding.ivStoryImage.getContext()).load(storyImage).placeholder(android.R.color.darker_gray).into(binding.ivStoryImage);
            } else {
                binding.cvStoryImage.setVisibility(View.GONE);
            }
        } else {
            binding.tvStoryDescription.setText("Câu chuyện về sản phẩm đang được cập nhật.");
            binding.cvStoryImage.setVisibility(View.GONE);
        }

        binding.llIdealForContainer.removeAllViews();
        List<String> idealFor = safeList(product.getIdealFor());
        if (idealFor.isEmpty()) {
            binding.tvIdealForHeader.setVisibility(View.GONE);
        } else {
            binding.tvIdealForHeader.setVisibility(View.VISIBLE);
            for (String ideal : idealFor) {
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);
                row.setGravity(Gravity.CENTER_VERTICAL);

                ImageView icon = new ImageView(ctx);
                int sizePx = (int) (16 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                icon.setLayoutParams(lp);
                icon.setImageResource(R.drawable.ic_check);
                icon.setColorFilter(Color.parseColor("#455B49"));

                TextView text = new TextView(ctx);
                text.setText(ideal);
                text.setTextColor(Color.parseColor("#444444"));
                text.setTextSize(14f);

                row.addView(icon);
                row.addView(text);
                binding.llIdealForContainer.addView(row);
            }
        }

        binding.llBenefitsContainer.removeAllViews();
        List<String> benefits = safeList(product.getBenefits());
        if (benefits.isEmpty()) {
            binding.tvBenefitsHeader.setVisibility(View.GONE);
        } else {
            binding.tvBenefitsHeader.setVisibility(View.VISIBLE);
            for (String benefit : benefits) {
                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);
                row.setGravity(Gravity.CENTER_VERTICAL);

                ImageView icon = new ImageView(ctx);
                int sizePx = (int) (16 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                icon.setLayoutParams(lp);
                icon.setImageResource(R.drawable.ic_check);
                icon.setColorFilter(Color.parseColor("#455B49"));

                TextView text = new TextView(ctx);
                text.setText(benefit);
                text.setTextColor(Color.parseColor("#444444"));
                text.setTextSize(14f);

                row.addView(icon);
                row.addView(text);
                binding.llBenefitsContainer.addView(row);
            }
        }

        String usage = safeStr(product.getUsage());
        if (!usage.isEmpty()) {
            binding.tvUsage.setText(usage);
        } else {
            binding.tvUsage.setText("Chưa cập nhật hướng dẫn sử dụng sản phẩm.");
        }

        String usageAmount = safeStr(product.getUsageAmount());
        if (!usageAmount.isEmpty()) {
            binding.cvUsageAmount.setVisibility(View.VISIBLE);
            binding.tvUsageAmount.setText(usageAmount);
        } else {
            binding.cvUsageAmount.setVisibility(View.GONE);
        }

        String scent = safeStr(product.getScent());
        if (!scent.isEmpty()) {
            binding.cvScent.setVisibility(View.VISIBLE);
            binding.tvScent.setText(scent);
        } else {
            binding.cvScent.setVisibility(View.GONE);
        }

        String notes = safeStr(product.getNotes());
        if (!notes.isEmpty()) {
            binding.cvNotes.setVisibility(View.VISIBLE);
            binding.tvNotes.setText(notes);
        } else {
            binding.cvNotes.setVisibility(View.GONE);
        }

        ProductReviewHelper.RatingStats ratingStats = ProductReviewHelper.getRatingStats(safeStr(product.getId()));
        double averageRating = ratingStats.rating;
        int totalReviews = ratingStats.reviewCount;
        binding.tvRatingValue.setText(String.format(Locale.US, "%.1f", averageRating));
        binding.tvReviewsCount.setText("(" + totalReviews + " reviews)");

        List<ProductReview> reviewItems = ProductReviewHelper.getReviews(
                safeStr(product.getId()), safeStr(product.getName()), safeStr(product.getCategory()));
        if (!reviewItems.isEmpty()) {
            reviewAdapter.updateData(new ArrayList<>(reviewItems.subList(0, Math.min(3, reviewItems.size()))));
        } else {
            reviewAdapter.updateData(Collections.emptyList());
        }

        SharedPreferences sharedPrefs = ctx.getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE);
        String recentlyViewedStr = sharedPrefs.getString("recently_viewed_ids", "");
        if (recentlyViewedStr == null) recentlyViewedStr = "";
        List<String> idList = new ArrayList<>(Arrays.asList(recentlyViewedStr.split(",")));
        idList.remove("");
        String productId = product.getId();
        if (productId != null && !productId.isEmpty()) {
            idList.remove(productId);
            idList.add(0, productId);
        }
        List<String> limitedList = idList.subList(0, Math.min(10, idList.size()));
        sharedPrefs.edit().putString("recently_viewed_ids", String.join(",", limitedList)).apply();

        if (cachedAllProducts != null) {
            try {
                updateRelatedProducts(cachedAllProducts, product);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        checkProductCompatibilitySafe(product);
        loadRelatedHandbooks(product);
    }

    private void loadRelatedHandbooks(ProductEntity product) {
        if (product == null || handbookAdapter == null) return;

        new Thread(() -> {
            List<YtVideoEntity> allVideos = new LocalJsonReader(requireContext().getApplicationContext()).getExploreVideos();
            List<YtVideoEntity> filteredVideos = new ArrayList<>();
            for (YtVideoEntity video : allVideos) {
                if (LocalJsonReader.isNotebookVideo(video)) {
                    filteredVideos.add(video);
                }
            }

            List<String> ingredients = Arrays.asList(
                    "bí đao", "nghệ", "cà phê", "bưởi", "hoa hồng", "dừa",
                    "tràm trà", "sen", "rau má", "bồ kết"
            );
            String productNameLower = safeStr(product.getName()).toLowerCase();
            String productCategoryLower = safeStr(product.getCategory()).toLowerCase();
            String productCategoryIds = safeStr(product.getCategoryIds());

            List<Pair<YtVideoEntity, Integer>> rankedVideos = new ArrayList<>();
            for (YtVideoEntity video : filteredVideos) {
                int score = 0;
                String textToSearch = (safeStr(video.getTitle()) + " " + safeStr(video.getDescription())).toLowerCase();

                for (String ing : ingredients) {
                    if (productNameLower.contains(ing)) {
                        if (textToSearch.contains(ing)) score += 10;
                        if ("bí đao".equals(ing) && (textToSearch.contains("mụn") || textToSearch.contains("thâm"))) score += 5;
                        if ("nghệ".equals(ing) && (textToSearch.contains("sáng da") || textToSearch.contains("thâm") || textToSearch.contains("curcumin"))) score += 5;
                        if ("bưởi".equals(ing) && (textToSearch.contains("tóc") || textToSearch.contains("rụng") || textToSearch.contains("gội"))) score += 5;
                        if ("cà phê".equals(ing) && (textToSearch.contains("tẩy tế bào chết") || textToSearch.contains("body") || textToSearch.contains("scrub"))) score += 5;
                    }
                }

                if (productCategoryLower.contains("da") || productCategoryIds.contains("7176b5e7966be88daf95cfd4")) {
                    if (textToSearch.contains("skincare") || textToSearch.contains("da") || textToSearch.contains("mặt")) {
                        score += 2;
                    }
                }

                rankedVideos.add(new Pair<>(video, score));
            }

            rankedVideos.sort((a, b) -> Integer.compare(b.second, a.second));
            List<YtVideoEntity> finalVideos = new ArrayList<>();
            for (Pair<YtVideoEntity, Integer> entry : rankedVideos) {
                finalVideos.add(entry.first);
                if (finalVideos.size() >= 4) break;
            }

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || handbookAdapter == null) return;
                handbookAdapter.updateData(finalVideos);
            });
        }).start();
    }

    private void updateRelatedProducts(List<ProductEntity> allProducts, ProductEntity product) {
        if (binding == null || product == null || allProducts == null || relatedAdapter == null || recentlyViewedAdapter == null) return;
        String currentProductId = product.getId();

        List<ProductEntity> otherProducts = new ArrayList<>();
        for (ProductEntity p : allProducts) {
            if (p == null || p.getId() == null) continue;
            if (currentProductId == null || !currentProductId.equals(p.getId())) otherProducts.add(p);
        }

        List<String> currentSubcategories = new ArrayList<>();
        String categoryIdsStr = safeStr(product.getCategoryIds());
        for (String s : categoryIdsStr.split(",")) {
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
            String otherCategoryIds = safeStr(other.getCategoryIds());
            for (String id : otherCategoryIds.split(",")) {
                if (productSubcategories.contains(id)) { match = true; break; }
            }
            if (match) subcategoryMatches.add(other);
        }

        List<ProductEntity> parentMatches = new ArrayList<>();
        for (ProductEntity other : otherProducts) {
            if (subcategoryMatches.contains(other)) continue;
            boolean match = false;
            String otherCategoryIds = safeStr(other.getCategoryIds());
            for (String id : otherCategoryIds.split(",")) {
                if (productParentCategories.contains(id)) { match = true; break; }
            }
            if (match) parentMatches.add(other);
        }

        String productCategory = safeStr(product.getCategory());
        List<ProductEntity> generalCategoryMatches = new ArrayList<>();
        for (ProductEntity other : otherProducts) {
            if (subcategoryMatches.contains(other) || parentMatches.contains(other)) continue;
            String otherCategory = safeStr(other.getCategory());
            if (!otherCategory.isEmpty() && otherCategory.equalsIgnoreCase(productCategory)) {
                generalCategoryMatches.add(other);
            }
        }

        List<ProductEntity> finalRelated = new ArrayList<>(subcategoryMatches);
        finalRelated.addAll(parentMatches);
        finalRelated.addAll(generalCategoryMatches);
        if (!finalRelated.isEmpty()) {
            relatedAdapter.updateData(finalRelated.subList(0, Math.min(8, finalRelated.size())));
        } else {
            relatedAdapter.updateData(Collections.emptyList());
        }

        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences sharedPrefs = ctx.getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE);
        String currentViewedStr = sharedPrefs.getString("recently_viewed_ids", "");
        if (currentViewedStr == null) currentViewedStr = "";
        List<String> currentIds = new ArrayList<>();
        for (String s : currentViewedStr.split(",")) {
            if (!s.isEmpty()) currentIds.add(s);
        }

        List<ProductEntity> viewedProducts = new ArrayList<>();
        for (String id : currentIds) {
            if (id == null || id.isEmpty()) continue;
            if (currentProductId != null && id.equals(currentProductId)) continue;
            for (ProductEntity p : allProducts) {
                if (p == null || p.getId() == null) continue;
                if (p.getId().equals(id)) {
                    viewedProducts.add(p);
                    break;
                }
            }
        }
        recentlyViewedAdapter.updateData(viewedProducts);
    }

    private void checkProductCompatibilitySafe(ProductEntity product) {
        try {
            checkProductCompatibility(product);
        } catch (Exception e) {
            e.printStackTrace();
            if (binding != null) {
                binding.cvSkinCompatibility.setVisibility(View.GONE);
            }
        }
    }

    private void checkProductCompatibility(ProductEntity product) {
        Context ctx = getContext();
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        boolean hasQuiz = ProfileSession.hasSavedSkinProfile(ctx);

        if (!hasQuiz) {
            binding.cvSkinCompatibility.setVisibility(View.VISIBLE);
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#FEFBF4"));
            binding.cvSkinCompatibility.setStrokeColor(Color.parseColor("#DDDFC4"));
            binding.ivCompatibilityIcon.setImageResource(R.drawable.ic_ai_outline);
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
        for (String s : safeList(product.getDetailedIngredients())) detailedIngredientsList.add(s.toLowerCase());

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
            binding.ivCompatibilityIcon.setImageResource(R.drawable.ic_warning_triangle);
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
        if (pageChangeCallback != null) {
            binding.vpProductImage.unregisterOnPageChangeCallback(pageChangeCallback);
            pageChangeCallback = null;
        }
        if (count <= 1) return;

        Context ctx = getContext();
        if (ctx == null) return;

        List<ImageView> indicators = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int size = (i == 0) ? 8 : 6;
            int sizePx = (int) (size * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(4, 0, 4, 0);
            ImageView imageView = new ImageView(ctx);
            imageView.setImageResource((i == 0) ? R.drawable.bg_dialog_btn_confirm : R.drawable.bg_circle_grey);
            indicators.add(imageView);
            binding.llIndicatorContainer.addView(imageView, lp);
        }

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (binding == null) return;
                for (int i = 0; i < count; i++) {
                    int size = (i == position) ? 8 : 6;
                    int sizePx = (int) (size * getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                    lp.setMargins(4, 0, 4, 0);
                    indicators.get(i).setLayoutParams(lp);
                    indicators.get(i).setImageResource((i == position) ? R.drawable.bg_dialog_btn_confirm : R.drawable.bg_circle_grey);
                }
            }
        };
        binding.vpProductImage.registerOnPageChangeCallback(pageChangeCallback);
    }

    @Override
    public void onDestroyView() {
        if (binding != null && pageChangeCallback != null) {
            binding.vpProductImage.unregisterOnPageChangeCallback(pageChangeCallback);
            pageChangeCallback = null;
        }
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
            String imageUrl = images.get(position);
            ProductImageHelper.clearToWhitePlaceholder(holder.imageView);
            if (!ProductImageCache.isValidImageUrl(imageUrl)) {
                return;
            }
            com.bumptech.glide.Glide.with(holder.imageView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.color.white)
                    .error(android.R.color.white)
                    .into(holder.imageView);
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
        ProductDetailLauncher.open(this, prod);
    }

    private void addToCart(ProductEntity prod) {
        CartHelper.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), prod, 1);
    }

    private void showChooseQuantityBottomSheet(ProductEntity prod) {
        if (prod == null) return;
        if (prod.getStock() <= 0) {
            Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(prod, new ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
            @Override
            public void onAddToCartClick(ProductEntity p, int quantity) {
                CartHelper.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), p, quantity);
            }
            @Override
            public void onBuyNowClick(ProductEntity p, int quantity) {
                CartItemEntity checkoutItem = new CartItemEntity(p.getId(), p.getName(), p.getMainImage(), p.getPrice(), quantity, true);
                ArrayList<CartItemEntity> list = new ArrayList<>();
                list.add(checkoutItem);
                ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.newInstance(list, cartVoucherCode, cartVoucherDiscount);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, checkoutFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
    }
}
