package com.veganbeauty.app.features.shop.search;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopSearchDetailBinding;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;

public class ShopSearchDetailFragment extends RootieFragment {

    private ShopSearchDetailBinding binding;

    private ShopSearchViewModel searchViewModel;
    private SearchSuggestionAdapter keywordSuggestionAdapter;
    private SearchContentSuggestionAdapter contentSuggestionAdapter;
    private HotDealsAdapter previewProductsAdapter;

    private static final String ARG_KEYWORD = "arg_keyword";

    public static ShopSearchDetailFragment newInstance(String keyword) {
        ShopSearchDetailFragment fragment = new ShopSearchDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEYWORD, keyword);
        fragment.setArguments(args);
        return fragment;
    }

    public static ShopSearchDetailFragment newInstance() {
        return newInstance("");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopSearchDetailBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));
        searchViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ShopSearchViewModel(repository, db.communityDao());
            }
        }).get(ShopSearchViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnClear.setOnClickListener(v -> binding.etSearch.getText().clear());

        keywordSuggestionAdapter = new SearchSuggestionAdapter(R.drawable.ic_search, name -> {
            binding.etSearch.setText(name);
            binding.etSearch.setSelection(name.length());
            performSearch();
        });
        binding.rvKeywordSuggestions.setAdapter(keywordSuggestionAdapter);
        binding.rvKeywordSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));

        contentSuggestionAdapter = new SearchContentSuggestionAdapter(this::handleContentSuggestionClick);
        binding.rvCategorySuggestions.setAdapter(contentSuggestionAdapter);
        binding.rvCategorySuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));

        previewProductsAdapter = new HotDealsAdapter(
                this::navigateToDetail,
                product -> {
                    com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet bottomSheet = new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(product, new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                        @Override
                        public void onAddToCartClick(com.veganbeauty.app.data.local.entities.ProductEntity p, int quantity) {
                            com.veganbeauty.app.features.shop.product.CartHelper.addToCart(requireContext(), androidx.lifecycle.LifecycleOwnerKt.getLifecycleScope(ShopSearchDetailFragment.this), p, quantity);
                        }
                        @Override
                        public void onBuyNowClick(com.veganbeauty.app.data.local.entities.ProductEntity p, int quantity) {}
                    });
                    bottomSheet.show(getParentFragmentManager(), "ChooseQuantity");
                }
        );
        binding.rvSearchResults.setAdapter(previewProductsAdapter);
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        String initialKeyword = "";
        if (getArguments() != null) {
            initialKeyword = getArguments().getString(ARG_KEYWORD, "");
        }
        if (!initialKeyword.isEmpty()) {
            binding.etSearch.setText(initialKeyword);
            binding.etSearch.setSelection(initialKeyword.length());
            searchViewModel.updateSuggestions(initialKeyword);
        }

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s != null ? s.toString().trim() : "";
                binding.btnClear.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                searchViewModel.updateSuggestions(keyword);
            }
        });
    }

    @Override
    protected void observeViewModel() {
        searchViewModel.suggestions.observe(getViewLifecycleOwner(), suggestions -> {
            keywordSuggestionAdapter.submitList(suggestions.getProductNames());
            contentSuggestionAdapter.submitList(suggestions.getContentItems());
            previewProductsAdapter.submitList(suggestions.getPreviewProducts());

            binding.rvKeywordSuggestions.setVisibility(
                    suggestions.getProductNames().isEmpty() ? View.GONE : View.VISIBLE);
            binding.dividerContent.setVisibility(
                    (suggestions.getProductNames().isEmpty() || suggestions.getContentItems().isEmpty()) ? View.GONE : View.VISIBLE);
            binding.rvCategorySuggestions.setVisibility(
                    suggestions.getContentItems().isEmpty() ? View.GONE : View.VISIBLE);
            binding.dividerProducts.setVisibility(
                    (suggestions.getContentItems().isEmpty() && suggestions.getProductNames().isEmpty()) ? View.GONE
                            : (suggestions.getPreviewProducts().isEmpty() ? View.GONE : View.VISIBLE));
            binding.rvSearchResults.setVisibility(
                    suggestions.getPreviewProducts().isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void handleContentSuggestionClick(ContentSuggestion item) {
        if (item.getType() == ContentSuggestionType.CATEGORY) {
            String parentCategory = item.getParentCategory();
            String category = item.getCategoryName();
            if (parentCategory == null && category == null) return;
            if (parentCategory != null) {
                navigateToCategoryList(parentCategory, category);
            } else {
                navigateToCategoryList(category, null);
            }
        } else if (item.getType() == ContentSuggestionType.VIDEO) {
            if (item.getVideoUrl() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getVideoUrl())));
            }
        } else if (item.getType() == ContentSuggestionType.BLOG || item.getType() == ContentSuggestionType.POST) {
            performSearch();
        }
    }

    private void performSearch() {
        Editable text = binding.etSearch.getText();
        String keyword = text != null ? text.toString().trim() : "";
        if (keyword.isEmpty()) return;
        ShopSearchHistoryHelper.add(requireContext(), keyword);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, ShopSearchResultsFragment.newInstance(keyword))
                .addToBackStack(null)
                .commit();
    }

    private void navigateToCategoryList(String category, String subcategory) {
        ShopListFragment listFragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("CATEGORY_NAME", category);
        if (subcategory != null) {
            args.putString("SUBCATEGORY_NAME", subcategory);
        }
        listFragment.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, listFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToDetail(ProductEntity product) {
        ProductDetailLauncher.open(this, product);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
