package com.veganbeauty.app.features.shop.product.list;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopCategoryBinding;
import com.veganbeauty.app.features.shop.ShopViewModel;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.features.shop.product.ShopVoucherFragment;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.search.ShopSearchFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;

public class ShopListFragment extends RootieFragment {

    private ShopCategoryBinding _binding;
    private ShopViewModel viewModel;
    private SubcategoryAdapter subcategoryAdapter;
    private ShopListAdapter productAdapter;

    private String cartVoucherCode = null;
    private long cartVoucherDiscount = 0L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopCategoryBinding.inflate(inflater, container, false);
        setupViewModel();
        return _binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ShopViewModel(repository);
            }
        }).get(ShopViewModel.class);
    }

    private void navigateToDetail(ProductEntity product) {
        ProductDetailLauncher.open(this, product);
    }

    @Override
    protected void setupUI(@NonNull View view) {
        productAdapter = new ShopListAdapter(
                this::navigateToDetail,
                product -> {
                    ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(
                            product,
                            new ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                                @Override
                                public void onAddToCartClick(ProductEntity p, int quantity) {
                                    CartHelper.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), p, quantity);
                                }
                                @Override
                                public void onBuyNowClick(ProductEntity p, int quantity) {
                                    CartItemEntity checkoutItem = new CartItemEntity(
                                            p.getId(), p.getName(), p.getMainImage(), p.getPrice(), quantity, true
                                    );
                                    ArrayList<CartItemEntity> list = new ArrayList<>();
                                    list.add(checkoutItem);
                                    ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.newInstance(list, "", 0L);
                                    getParentFragmentManager().beginTransaction()
                                            .replace(R.id.main_container, checkoutFragment)
                                            .addToBackStack(null)
                                            .commit();
                                }
                            }
                    );
                    bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
                }
        );

        _binding.rvProducts.setAdapter(productAdapter);

        subcategoryAdapter = new SubcategoryAdapter(subcategory -> {
            subcategoryAdapter.setSelectedSubcategory(subcategory);
            viewModel.setSubcategoryFilter(subcategory);
        });
        _binding.rvSubcategories.setAdapter(subcategoryAdapter);

        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        _binding.btnSearch.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new ShopSearchFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        _binding.btnFilterAdvanced.setOnClickListener(v -> {
            AdvancedFilterBottomSheet filterSheet = new AdvancedFilterBottomSheet();
            filterSheet.show(getChildFragmentManager(), AdvancedFilterBottomSheet.TAG);
        });

        _binding.btnFilterSkinType.setOnClickListener(v -> {
            SkinTypeFilterBottomSheet skinTypeSheet = new SkinTypeFilterBottomSheet();
            skinTypeSheet.show(getChildFragmentManager(), SkinTypeFilterBottomSheet.TAG);
        });

        _binding.btnFilterPrice.setOnClickListener(v -> {
            PriceFilterBottomSheet priceSheet = new PriceFilterBottomSheet();
            priceSheet.show(getChildFragmentManager(), PriceFilterBottomSheet.TAG);
        });

        _binding.btnCart.setOnClickListener(v -> {
            com.veganbeauty.app.features.shop.product.CartBottomSheetFragment cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                    cartVoucherCode, cartVoucherDiscount
            );
            cartSheet.show(getParentFragmentManager(), com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG);
        });

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            cartVoucherCode = result.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            cartVoucherDiscount = result.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            com.veganbeauty.app.features.shop.product.CartBottomSheetFragment cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                    cartVoucherCode, cartVoucherDiscount
            );
            cartSheet.show(getParentFragmentManager(), com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG);
        });

        _binding.btnSortToggle.setOnClickListener(v -> {
            if (_binding.layoutSortOptions.getVisibility() == View.VISIBLE) {
                _binding.layoutSortOptions.setVisibility(View.GONE);
            } else {
                _binding.layoutSortOptions.setVisibility(View.VISIBLE);
            }
        });

        _binding.btnSortBestSelling.setOnClickListener(v -> selectSortOption(_binding.btnSortBestSelling, "BEST_SELLING"));
        _binding.btnSortNewest.setOnClickListener(v -> selectSortOption(_binding.btnSortNewest, "NEWEST"));
        _binding.btnSortPriceLow.setOnClickListener(v -> selectSortOption(_binding.btnSortPriceLow, "PRICE_LOW"));
        _binding.btnSortPriceHigh.setOnClickListener(v -> selectSortOption(_binding.btnSortPriceHigh, "PRICE_HIGH"));

        Bundle args = getArguments();
        String categoryName = args != null ? args.getString("CATEGORY_NAME") : null;
        String subcategoryName = args != null ? args.getString("SUBCATEGORY_NAME") : null;

        if (categoryName != null) {
            viewModel.setCategoryFilter(categoryName);
            _binding.tvTitle.setText(categoryName);
            if (subcategoryName != null && !subcategoryName.isEmpty()) {
                viewModel.setSubcategoryFilter(subcategoryName);
            }
        } else {
            viewModel.setCategoryFilter("Tất cả");
            _binding.tvTitle.setText("Tất cả sản phẩm");
        }
    }

    private void selectSortOption(TextView selectedView, String sortOrder) {
        Typeface medTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium);
        Typeface regTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular);

        List<kotlin.Pair<TextView, String>> options = Arrays.asList(
                new kotlin.Pair<>(_binding.btnSortBestSelling, "BEST_SELLING"),
                new kotlin.Pair<>(_binding.btnSortNewest, "NEWEST"),
                new kotlin.Pair<>(_binding.btnSortPriceLow, "PRICE_LOW"),
                new kotlin.Pair<>(_binding.btnSortPriceHigh, "PRICE_HIGH")
        );

        for (kotlin.Pair<TextView, String> option : options) {
            TextView view = option.getFirst();
            if (view == selectedView) {
                view.setTextColor(Color.parseColor("#3E4D44"));
                view.setTypeface(medTypeface);
            } else {
                view.setTextColor(Color.parseColor("#888888"));
                view.setTypeface(regTypeface);
            }
        }
        viewModel.setSortOrder(sortOrder);
    }

    @Override
    protected void observeViewModel() {
        viewModel.products.observe(getViewLifecycleOwner(), products -> {
            productAdapter.submitList(products);
            _binding.rvProducts.scrollToPosition(0);
        });

        viewModel.subcategories.observe(getViewLifecycleOwner(), subcategories -> {
            subcategoryAdapter.submitList(subcategories);
            Bundle args = getArguments();
            String subcategoryName = args != null ? args.getString("SUBCATEGORY_NAME") : null;

            if (subcategoryName != null && !subcategoryName.isEmpty() && subcategories.contains(subcategoryName)) {
                subcategoryAdapter.setSelectedSubcategory(subcategoryName);
            } else {
                subcategoryAdapter.setSelectedSubcategory("Tất cả");
            }
        });

        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            RootieDatabase db = RootieDatabase.getDatabase(requireContext());
            db.cartDao().getAllCartItems().collect(new FlowCollector<List<CartItemEntity>>() {
                @Nullable
                @Override
                public Object emit(List<CartItemEntity> items, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    int totalQty = 0;
                    for (CartItemEntity item : items) {
                        totalQty += item.getQuantity();
                    }
                    if (totalQty > 0) {
                        _binding.tvCartBadge.setVisibility(View.VISIBLE);
                        _binding.tvCartBadge.setText(String.valueOf(totalQty));
                    } else {
                        _binding.tvCartBadge.setVisibility(View.GONE);
                    }
                    return kotlin.Unit.INSTANCE;
                }
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
