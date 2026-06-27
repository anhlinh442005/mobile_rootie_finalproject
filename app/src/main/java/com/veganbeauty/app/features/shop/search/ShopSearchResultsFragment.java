package com.veganbeauty.app.features.shop.search;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopSearchResultsBinding;
import com.veganbeauty.app.features.shop.ShopViewModel;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;
import com.veganbeauty.app.features.shop.product.list.AdvancedFilterBottomSheet;
import com.veganbeauty.app.features.shop.product.list.PriceFilterBottomSheet;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;
import com.veganbeauty.app.features.shop.product.list.SkinTypeFilterBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class ShopSearchResultsFragment extends RootieFragment {

    private static final String ARG_KEYWORD = "arg_keyword";

    private ShopSearchResultsBinding binding;

    private ShopSearchViewModel searchViewModel;
    private ShopViewModel shopViewModel;

    private String keyword = "";
    private boolean showingProducts = true;
    private boolean isSuggestionMode = false;

    private SearchSuggestionAdapter keywordSuggestionAdapter;
    private SearchContentSuggestionAdapter contentSuggestionAdapter;
    private HotDealsAdapter previewProductsAdapter;
    private SearchVideoAdapter videoAdapter;

    private ShopListAdapter productAdapter;

    public static ShopSearchResultsFragment newInstance(String keyword) {
        ShopSearchResultsFragment fragment = new ShopSearchResultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEYWORD, keyword);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopSearchResultsBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            keyword = getArguments().getString(ARG_KEYWORD, "");
        }
        setupViewModels();
        return binding.getRoot();
    }

    private void setupViewModels() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(ShopSearchViewModel.class)) {
                    return (T) new ShopSearchViewModel(repository, db.communityDao());
                } else if (modelClass.isAssignableFrom(ShopViewModel.class)) {
                    return (T) new ShopViewModel(repository);
                }
                throw new IllegalArgumentException("Unknown ViewModel");
            }
        };

        searchViewModel = new ViewModelProvider(this, factory).get(ShopSearchViewModel.class);
        shopViewModel = new ViewModelProvider(this, factory).get(ShopViewModel.class);
    }

    @Override
    public void setupUI(View view) {
        productAdapter = new ShopListAdapter(
                product -> { navigateToDetail(product); return kotlin.Unit.INSTANCE; },
                product -> {
                    ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(
                            product,
                            (p, quantity) -> {
                                CartHelper.INSTANCE.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), p, quantity);
                                return kotlin.Unit.INSTANCE;
                            },
                            (p, quantity) -> {
                                CartItemEntity checkoutItem = new CartItemEntity(p.getId(), p.getName(), p.getMainImage(), p.getPrice(), quantity, true);
                                ArrayList<CartItemEntity> list = new ArrayList<>();
                                list.add(checkoutItem);
                                ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.Companion.newInstance(list, null, 0L);
                                getParentFragmentManager().beginTransaction()
                                        .replace(R.id.main_container, checkoutFragment)
                                        .addToBackStack(null)
                                        .commit();
                                return kotlin.Unit.INSTANCE;
                            }
                    );
                    bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
                    return kotlin.Unit.INSTANCE;
                }
        );

        binding.etSearch.setText(keyword);
        binding.btnBack.setOnClickListener(v -> handleBack());
        binding.btnClear.setOnClickListener(v -> binding.etSearch.getText().clear());

        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);

        videoAdapter = new SearchVideoAdapter();
        binding.rvVideos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVideos.setAdapter(videoAdapter);

        setupSuggestionAdapters();
        setupSearchInput();
        setupFilters();
        setupSortButtons();
        setupTabs();

        shopViewModel.setCategoryFilter("Tất cả");
        selectTab(true);
    }

    private void setupSuggestionAdapters() {
        keywordSuggestionAdapter = new SearchSuggestionAdapter(R.drawable.ic_search, name -> {
            binding.etSearch.setText(name);
            binding.etSearch.setSelection(name.length());
            performSearch();
            return kotlin.Unit.INSTANCE;
        });
        binding.rvKeywordSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvKeywordSuggestions.setAdapter(keywordSuggestionAdapter);

        contentSuggestionAdapter = new SearchContentSuggestionAdapter(item -> {
            handleContentSuggestionClick(item);
            return kotlin.Unit.INSTANCE;
        });
        binding.rvCategorySuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategorySuggestions.setAdapter(contentSuggestionAdapter);

        previewProductsAdapter = new HotDealsAdapter(product -> { navigateToDetail(product); return kotlin.Unit.INSTANCE; });
        binding.rvPreviewProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPreviewProducts.setAdapter(previewProductsAdapter);
    }

    private void setupSearchInput() {
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            isSuggestionMode = hasFocus;
            updateSuggestionUi();
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                binding.btnClear.setVisibility(s == null || s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
                if (isSuggestionMode) {
                    searchViewModel.updateSuggestions(s != null ? s.toString().trim() : "");
                }
            }
        });
    }

    private void setupFilters() {
        binding.btnFilterAdvanced.setOnClickListener(v -> new AdvancedFilterBottomSheet().show(getChildFragmentManager(), AdvancedFilterBottomSheet.TAG));
        binding.btnFilterSkinType.setOnClickListener(v -> new SkinTypeFilterBottomSheet().show(getChildFragmentManager(), SkinTypeFilterBottomSheet.TAG));
        binding.btnFilterPrice.setOnClickListener(v -> new PriceFilterBottomSheet().show(getChildFragmentManager(), PriceFilterBottomSheet.TAG));
        binding.btnSortToggle.setOnClickListener(v -> binding.layoutSortOptions.setVisibility(binding.layoutSortOptions.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    private void setupSortButtons() {
        binding.btnSortBestSelling.setOnClickListener(v -> selectSort(v, "BEST_SELLING"));
        binding.btnSortNewest.setOnClickListener(v -> selectSort(v, "NEWEST"));
        binding.btnSortPriceLow.setOnClickListener(v -> selectSort(v, "PRICE_LOW"));
        binding.btnSortPriceHigh.setOnClickListener(v -> selectSort(v, "PRICE_HIGH"));
    }

    private void selectSort(View selected, String order) {
        android.graphics.Typeface med = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium);
        android.graphics.Typeface reg = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular);

        TextView[] btns = {binding.btnSortBestSelling, binding.btnSortNewest, binding.btnSortPriceLow, binding.btnSortPriceHigh};

        for (TextView btn : btns) {
            if (btn == selected) {
                btn.setTextColor(getResources().getColor(R.color.primary, null));
                btn.setTypeface(med);
            } else {
                btn.setTextColor(getResources().getColor(R.color.gray_dark, null));
                btn.setTypeface(reg);
            }
        }
        shopViewModel.setSortOrder(order);
    }

    private void setupTabs() {
        binding.tabProducts.setOnClickListener(v -> selectTab(true));
        binding.tabHandbook.setOnClickListener(v -> selectTab(false));
    }

    private void selectTab(boolean products) {
        showingProducts = products;
        android.graphics.Typeface bold = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold);
        android.graphics.Typeface regular = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular);

        if (products) {
            binding.tabProducts.setTextColor(getResources().getColor(R.color.primary, null));
            binding.tabProducts.setTypeface(bold);
            binding.tabHandbook.setTextColor(getResources().getColor(R.color.gray_dark, null));
            binding.tabHandbook.setTypeface(regular);
            binding.layoutProductFilters.setVisibility(View.VISIBLE);
            binding.rvProducts.setVisibility(View.VISIBLE);
            binding.rvVideos.setVisibility(View.GONE);
        } else {
            binding.tabHandbook.setTextColor(getResources().getColor(R.color.primary, null));
            binding.tabHandbook.setTypeface(bold);
            binding.tabProducts.setTextColor(getResources().getColor(R.color.gray_dark, null));
            binding.tabProducts.setTypeface(regular);
            binding.layoutProductFilters.setVisibility(View.GONE);
            binding.rvProducts.setVisibility(View.GONE);
            binding.rvVideos.setVisibility(View.VISIBLE);
            refreshVideos();
        }
        refreshProducts();
    }

    private void updateSuggestionUi() {
        if (isSuggestionMode) {
            binding.llSuggestions.setVisibility(View.VISIBLE);
            binding.layoutMainContent.setVisibility(View.GONE);
            String kw = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
            searchViewModel.updateSuggestions(kw);
        } else {
            binding.llSuggestions.setVisibility(View.GONE);
            binding.layoutMainContent.setVisibility(View.VISIBLE);
            binding.etSearch.clearFocus();
        }
    }

    private void handleBack() {
        if (isSuggestionMode) {
            exitSuggestionMode();
        } else {
            navigateToSearchRoot();
        }
    }

    private void exitSuggestionMode() {
        isSuggestionMode = false;
        binding.etSearch.clearFocus();
        hideKeyboard();
        updateSuggestionUi();
    }

    private void navigateToSearchRoot() {
        getParentFragmentManager().popBackStack();
    }

    private void performSearch() {
        keyword = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
        if (keyword.isEmpty()) return;
        ShopSearchHistoryHelper.INSTANCE.add(requireContext(), keyword);
        exitSuggestionMode();
        refreshProducts();
        refreshVideos();
    }

    private void handleContentSuggestionClick(ContentSuggestion item) {
        if (item.getType() == ContentSuggestionType.CATEGORY) {
            if (item.getCategoryName() != null) {
                navigateToCategoryList(item.getCategoryName(), item.getSubcategoryName());
            }
        } else if (item.getType() == ContentSuggestionType.VIDEO) {
            if (item.getVideoUrl() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getVideoUrl())));
            }
        } else {
            performSearch();
        }
    }

    private void navigateToCategoryList(String category, String subcategory) {
        ShopListFragment listFragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("CATEGORY_NAME", category);
        if (subcategory != null) args.putString("SUBCATEGORY_NAME", subcategory);
        listFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, listFragment)
                .addToBackStack(null)
                .commit();
    }

    private void refreshProducts() {
        if (!showingProducts || keyword.trim().isEmpty()) return;
        List<ProductEntity> allProducts = shopViewModel.getProducts().getValue();
        List<ProductEntity> filtered = new ArrayList<>();
        if (allProducts != null) {
            for (ProductEntity p : allProducts) {
                if (searchViewModel.matchesKeyword(p, keyword)) {
                    filtered.add(p);
                }
            }
        }
        productAdapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshVideos() {
        if (showingProducts || keyword.trim().isEmpty()) return;
        List<SearchVideoModel> videos = searchViewModel.searchVideos(keyword);
        videoAdapter.submitList(videos);
        binding.tvEmpty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void navigateToDetail(ProductEntity product) {
        ShopDetailFragment detailFragment = new ShopDetailFragment();
        detailFragment.setProduct(product);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void observeViewModel() {
        shopViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (showingProducts) refreshProducts();
        });

        searchViewModel.getDataReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                refreshProducts();
                refreshVideos();
            }
        });

        searchViewModel.getSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            if (!isSuggestionMode) return;
            keywordSuggestionAdapter.submitList(suggestions.getProductNames());
            contentSuggestionAdapter.submitList(suggestions.getContentItems());
            previewProductsAdapter.submitList(suggestions.getPreviewProducts());

            binding.rvKeywordSuggestions.setVisibility(suggestions.getProductNames().isEmpty() ? View.GONE : View.VISIBLE);
            binding.dividerContent.setVisibility(suggestions.getProductNames().isEmpty() || suggestions.getContentItems().isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvCategorySuggestions.setVisibility(suggestions.getContentItems().isEmpty() ? View.GONE : View.VISIBLE);
            binding.dividerProducts.setVisibility(suggestions.getPreviewProducts().isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvPreviewProducts.setVisibility(suggestions.getPreviewProducts().isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
