package com.veganbeauty.app.features.shop.search;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopSearchBinding;
import com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;

import java.util.List;

public class ShopSearchFragment extends RootieFragment {

    private ShopSearchBinding binding;
    private ShopSearchViewModel searchViewModel;
    private SearchChipAdapter topSearchAdapter;
    private ShopSearchHistoryAdapter historyAdapter;
    private HotDealsAdapter hotDealsAdapter;
    private SearchSuggestionAdapter keywordSuggestionAdapter;
    private SearchContentSuggestionAdapter contentSuggestionAdapter;
    private HotDealsAdapter previewProductsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopSearchBinding.inflate(inflater, container, false);
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
    protected void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnScanQr.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new BarcodeScanFragment())
                .addToBackStack(null)
                .commit());

        setupAdapters();
        populateSearchHistory();
        searchViewModel.ensureProductsLoaded();

        binding.btnClear.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.etSearch.requestFocus();
            showKeyboard();
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s != null ? s.toString().trim() : "";
                searchViewModel.updateSuggestions(keyword);
                updateSuggestionVisibility(keyword);
            }
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        binding.btnClearHistory.setOnClickListener(v -> {
            ShopSearchHistoryHelper.clear(requireContext());
            populateSearchHistory();
        });

        binding.etSearch.requestFocus();
        binding.etSearch.post(() -> showKeyboard());
    }

    private void setupAdapters() {
        binding.rvTopSearch.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        topSearchAdapter = new SearchChipAdapter(term -> {
            kotlin.Pair<String, String> nav = searchViewModel.getNavigationForTerm(term);
            if (nav != null) {
                navigateToCategory(nav.getFirst(), nav.getSecond());
            }
        });
        binding.rvTopSearch.setAdapter(topSearchAdapter);

        historyAdapter = new ShopSearchHistoryAdapter(
                this::performSearch,
                query -> {
                    ShopSearchHistoryHelper.remove(requireContext(), query);
                    populateSearchHistory();
                }
        );
        binding.rvSearchHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchHistory.setAdapter(historyAdapter);

        hotDealsAdapter = new HotDealsAdapter(
                product -> ProductDetailLauncher.open(this, product),
                product -> {}
        );
        binding.rvHotDeals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotDeals.setAdapter(hotDealsAdapter);

        keywordSuggestionAdapter = new SearchSuggestionAdapter(R.drawable.ic_search, name -> {
            binding.etSearch.setText(name);
            binding.etSearch.setSelection(name.length());
            performSearch(name);
        });
        binding.rvKeywordSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvKeywordSuggestions.setAdapter(keywordSuggestionAdapter);

        contentSuggestionAdapter = new SearchContentSuggestionAdapter(this::handleContentSuggestionClick);
        binding.rvCategorySuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategorySuggestions.setAdapter(contentSuggestionAdapter);

        previewProductsAdapter = new HotDealsAdapter(
                product -> ProductDetailLauncher.open(this, product),
                product -> {}
        );
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(previewProductsAdapter);
    }

    @Override
    protected void observeViewModel() {
        searchViewModel.topSearchTerms.observe(getViewLifecycleOwner(), terms -> {
            if (terms != null) {
                topSearchAdapter.submitList(terms);
            }
        });

        searchViewModel.hotDeals.observe(getViewLifecycleOwner(), deals -> {
            if (deals != null) {
                hotDealsAdapter.submitList(deals);
            }
        });

        searchViewModel.suggestions.observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions == null) {
                return;
            }
            keywordSuggestionAdapter.submitList(suggestions.getProductNames());
            contentSuggestionAdapter.submitList(suggestions.getContentItems());
            previewProductsAdapter.submitList(suggestions.getPreviewProducts());

            String keyword = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
            if (keyword.isEmpty()) {
                return;
            }

            binding.rvKeywordSuggestions.setVisibility(
                    suggestions.getProductNames().isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvCategorySuggestions.setVisibility(
                    suggestions.getContentItems().isEmpty() ? View.GONE : View.VISIBLE);
            binding.rvSearchResults.setVisibility(
                    suggestions.getPreviewProducts().isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void updateSuggestionVisibility(String keyword) {
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        binding.llDefaultContent.setVisibility(hasKeyword ? View.GONE : View.VISIBLE);
        binding.llSuggestions.setVisibility(hasKeyword ? View.VISIBLE : View.GONE);
        if (!hasKeyword) {
            binding.rvSearchResults.setVisibility(View.GONE);
        }
    }

    private void populateSearchHistory() {
        List<String> history = ShopSearchHistoryHelper.getHistory(requireContext());
        if (history.isEmpty()) {
            binding.llSearchHistory.setVisibility(View.GONE);
            historyAdapter.submitList(history);
            return;
        }
        binding.llSearchHistory.setVisibility(View.VISIBLE);
        historyAdapter.submitList(history);
    }

    private void performSearch(String query) {
        hideKeyboard();
        ShopSearchHistoryHelper.add(requireContext(), query);
        populateSearchHistory();

        ShopListFragment fragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("SEARCH_QUERY", query);
        args.putString("CATEGORY_NAME", "Tất cả");
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToCategory(String categoryName, @Nullable String subcategoryName) {
        hideKeyboard();

        ShopListFragment fragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("CATEGORY_NAME", categoryName);
        if (subcategoryName != null) {
            args.putString("SUBCATEGORY_NAME", subcategoryName);
        }
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void handleContentSuggestionClick(ContentSuggestion item) {
        if (item.getType() == ContentSuggestionType.CATEGORY) {
            String parentCategory = item.getParentCategory();
            String category = item.getCategoryName();
            if (parentCategory == null && category == null) {
                return;
            }
            if (parentCategory != null) {
                navigateToCategory(parentCategory, category);
            } else {
                navigateToCategory(category, null);
            }
        } else if (item.getType() == ContentSuggestionType.VIDEO) {
            if (item.getVideoUrl() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getVideoUrl())));
            }
        } else if (item.getType() == ContentSuggestionType.BLOG || item.getType() == ContentSuggestionType.POST) {
            String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                performSearch(query);
            }
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
