package com.veganbeauty.app.features.shop.search;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.flexbox.FlexboxLayout;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.ShopSearchBinding;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopSearchFragment extends RootieFragment {

    private ShopSearchBinding _binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopSearchBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        _binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() > 0) {
                    _binding.btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    _binding.btnClearSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        _binding.btnClearSearch.setOnClickListener(v -> {
            _binding.etSearch.setText("");
            _binding.etSearch.requestFocus();
            showKeyboard();
        });

        _binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = _binding.etSearch.getText() != null ? _binding.etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        _binding.tvSearchBtn.setOnClickListener(v -> {
            String query = _binding.etSearch.getText() != null ? _binding.etSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });

        _binding.tvClearHistory.setOnClickListener(v -> clearSearchHistory());

        setupCategoryCards();

        populateSearchHistory();
        populateTrendingSearches();

        _binding.etSearch.requestFocus();
        showKeyboard();
    }

    private void performSearch(String query) {
        hideKeyboard();
        saveSearchQuery(query);

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

    private void navigateToCategory(String categoryName) {
        hideKeyboard();

        ShopListFragment fragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("CATEGORY_NAME", categoryName);
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupCategoryCards() {
        _binding.cardCategoryCleanser.setOnClickListener(v -> navigateToCategory("Làm sạch"));
        _binding.cardCategoryToner.setOnClickListener(v -> navigateToCategory("Nước cân bằng"));
        _binding.cardCategorySerum.setOnClickListener(v -> navigateToCategory("Tinh chất"));
        _binding.cardCategoryMoisturizer.setOnClickListener(v -> navigateToCategory("Dưỡng ẩm"));
        _binding.cardCategorySunscreen.setOnClickListener(v -> navigateToCategory("Chống nắng"));
        _binding.cardCategoryMask.setOnClickListener(v -> navigateToCategory("Mặt nạ"));
    }

    private void populateSearchHistory() {
        List<String> history = getSearchHistory();
        if (history.isEmpty()) {
            _binding.llSearchHistory.setVisibility(View.GONE);
        } else {
            _binding.llSearchHistory.setVisibility(View.VISIBLE);
            _binding.flexSearchHistory.removeAllViews();
            for (String query : history) {
                View tagView = createTagView(query);
                tagView.setOnClickListener(v -> {
                    _binding.etSearch.setText(query);
                    _binding.etSearch.setSelection(query.length());
                    performSearch(query);
                });
                _binding.flexSearchHistory.addView(tagView);
            }
        }
    }

    private void populateTrendingSearches() {
        List<String> trending = Arrays.asList(
                "Kem chống nắng vật lý",
                "Serum vitamin C",
                "Nước tẩy trang",
                "Kem dưỡng ẩm B5",
                "Sữa rửa mặt rau má"
        );

        _binding.flexTrendingSearches.removeAllViews();
        for (String query : trending) {
            View tagView = createTagView(query);
            tagView.setOnClickListener(v -> {
                _binding.etSearch.setText(query);
                _binding.etSearch.setSelection(query.length());
                performSearch(query);
            });
            _binding.flexTrendingSearches.addView(tagView);
        }
    }

    private View createTagView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#3E4D44"));
        tv.setTextSize(14f);
        tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium));
        tv.setBackgroundResource(R.drawable.bg_search_tag);

        int paddingHorizontal = (int) (16 * getResources().getDisplayMetrics().density);
        int paddingVertical = (int) (8 * getResources().getDisplayMetrics().density);
        tv.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        lp.setMargins(0, 0, margin, margin);
        tv.setLayoutParams(lp);

        return tv;
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(_binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private List<String> getSearchHistory() {
        String historyString = requireContext().getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE)
                .getString("search_history", "");
        if (historyString.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(historyString.split(",")));
    }

    private void saveSearchQuery(String query) {
        List<String> history = getSearchHistory();
        history.remove(query);
        history.add(0, query);
        if (history.size() > 10) {
            history = history.subList(0, 10);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append(",");
            }
        }

        requireContext().getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("search_history", sb.toString())
                .apply();
    }

    private void clearSearchHistory() {
        requireContext().getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("search_history")
                .apply();
        _binding.llSearchHistory.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
