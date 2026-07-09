package com.veganbeauty.app.features.shop.store;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.repository.StoreRepository;
import com.veganbeauty.app.databinding.ShopFragmentStoreSelectionBinding;
import com.veganbeauty.app.databinding.ShopItemSearchHistoryBinding;
import com.veganbeauty.app.databinding.ShopItemStoreBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;

public class ShopStoreSelectionFragment extends RootieFragment {

    public enum SearchState {
        NORMAL,
        SEARCH_HISTORY,
        SEARCH_RESULTS
    }

    private SearchState currentState = SearchState.NORMAL;

    private ShopFragmentStoreSelectionBinding binding;
    private RootieDatabase database;
    private StoreRepository repository;

    private boolean isSelectionMode = true;
    private String selectedStoreId = null;

    private List<StoreEntity> allStoresList = new ArrayList<>();
    private List<StoreEntity> filteredStoresList = new ArrayList<>();
    private StoreAdapter storeAdapter;
    private HistoryAdapter historyAdapter;

    private String selectedProvince = null;
    private String selectedDistrict = null;
    private boolean startInSearchMode = false;

    public static final String TAG = "ShopStoreSelectionFragment";
    public static final String REQUEST_KEY = "store_selection_request";
    public static final String RESULT_STORE_ID = "result_store_id";
    public static final String RESULT_STORE_NAME = "result_store_name";
    public static final String RESULT_STORE_ADDRESS = "result_store_address";

    private static final String ARG_IS_SELECTION_MODE = "is_selection_mode";
    private static final String ARG_SELECTED_STORE_ID = "selected_store_id";
    private static final String ARG_START_IN_SEARCH_MODE = "start_in_search_mode";
    private static final String ARG_INITIAL_PROVINCE = "initial_province";
    private static final String ARG_INITIAL_DISTRICT = "initial_district";

    public static ShopStoreSelectionFragment newInstance(boolean isSelectionMode, String selectedStoreId, boolean startInSearchMode, String initialProvince, String initialDistrict) {
        ShopStoreSelectionFragment fragment = new ShopStoreSelectionFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_SELECTION_MODE, isSelectionMode);
        if (selectedStoreId != null) args.putString(ARG_SELECTED_STORE_ID, selectedStoreId);
        args.putBoolean(ARG_START_IN_SEARCH_MODE, startInSearchMode);
        if (initialProvince != null) args.putString(ARG_INITIAL_PROVINCE, initialProvince);
        if (initialDistrict != null) args.putString(ARG_INITIAL_DISTRICT, initialDistrict);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = RootieDatabase.getDatabase(requireContext());
        repository = new StoreRepository(database.storeDao(), new LocalJsonReader(requireContext()));

        if (getArguments() != null) {
            isSelectionMode = getArguments().getBoolean(ARG_IS_SELECTION_MODE, true);
            selectedStoreId = getArguments().getString(ARG_SELECTED_STORE_ID);
            startInSearchMode = getArguments().getBoolean(ARG_START_IN_SEARCH_MODE, false);
            selectedProvince = getArguments().getString(ARG_INITIAL_PROVINCE);
            selectedDistrict = getArguments().getString(ARG_INITIAL_DISTRICT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopFragmentStoreSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        binding.btnBack.setOnClickListener(v -> {
            if (currentState != SearchState.NORMAL) updateUIState(SearchState.NORMAL);
            else getParentFragmentManager().popBackStack();
        });

        binding.btnSearch.setOnClickListener(v -> {
            updateUIState(SearchState.SEARCH_HISTORY);
            binding.etSearch.requestFocus();
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            updateUIState(SearchState.SEARCH_HISTORY);
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (currentState != SearchState.NORMAL && query.isEmpty()) {
                    updateUIState(SearchState.SEARCH_HISTORY);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    addToSearchHistory(query);
                    performSearch();
                }
                return true;
            }
            return false;
        });

        binding.btnClearAll.setOnClickListener(v -> {
            clearSearchHistory();
            refreshHistoryList();
        });

        binding.clRegionSelectorInner.setOnClickListener(v -> {
            ShopAddressSelectionFragment addressFragment = ShopAddressSelectionFragment.newInstance();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, addressFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getParentFragmentManager().setFragmentResultListener(ShopAddressSelectionFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String province = bundle.getString(ShopAddressSelectionFragment.RESULT_PROVINCE);
            String district = bundle.getString(ShopAddressSelectionFragment.RESULT_DISTRICT);
            if (province != null && !province.isEmpty() && district != null && !district.isEmpty()) {
                selectedProvince = province;
                selectedDistrict = district;
                binding.tvSelectedRegion.setText(province + ", " + district);
                filterStores();
            }
        });

        binding.btnSelect.setOnClickListener(v -> {
            if (selectedStoreId == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn một cửa hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            StoreEntity selectedStore = null;
            for (StoreEntity s : allStoresList) {
                if (s.getId().equals(selectedStoreId)) { selectedStore = s; break; }
            }
            if (selectedStore != null) {
                Bundle result = new Bundle();
                result.putString(RESULT_STORE_ID, selectedStore.getId());
                result.putString(RESULT_STORE_NAME, selectedStore.getTenCuaHang());
                result.putString(RESULT_STORE_ADDRESS, selectedStore.getDiaChiDayDu());
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                getParentFragmentManager().popBackStack();
            }
        });

        setupRecyclerView();
        setupHistoryRecyclerView();

        if (selectedProvince != null && selectedDistrict != null) {
            binding.tvSelectedRegion.setText(selectedProvince + ", " + selectedDistrict);
        }

        if (startInSearchMode) {
            updateUIState(SearchState.SEARCH_HISTORY);
            binding.etSearch.postDelayed(() -> {
                if (binding != null) {
                    binding.etSearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);
        } else {
            updateUIState(SearchState.NORMAL);
        }

        loadAndSyncStores();
    }

    @Override
    public void observeViewModel() {}

    private void setupRecyclerView() {
        storeAdapter = new StoreAdapter();
        binding.rvStores.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStores.setAdapter(storeAdapter);
    }

    private void loadAndSyncStores() {
        androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllStores())
            .observe(getViewLifecycleOwner(), stores -> {
                if (stores != null) {
                    allStoresList = stores;
                    filterStores();
                }
            });

        repository.refreshStores();
    }

    private void filterStores() {
        String query = binding.etSearch.getText() != null ? binding.etSearch.getText().toString().trim() : "";
        List<StoreEntity> list = new ArrayList<>(allStoresList);

        if (selectedProvince != null && !selectedProvince.isEmpty()) {
            List<StoreEntity> filtered = new ArrayList<>();
            for (StoreEntity s : list) {
                if (s.getTinhThanh().equalsIgnoreCase(selectedProvince)) filtered.add(s);
            }
            list = filtered;
        }

        if (selectedDistrict != null && !selectedDistrict.isEmpty()) {
            List<StoreEntity> filtered = new ArrayList<>();
            for (StoreEntity s : list) {
                if (s.getQuanHuyen().equalsIgnoreCase(selectedDistrict)) filtered.add(s);
            }
            list = filtered;
        }

        if (!query.isEmpty()) {
            List<StoreEntity> filtered = new ArrayList<>();
            for (StoreEntity s : list) {
                if (s.getTenCuaHang().toLowerCase().contains(query.toLowerCase()) ||
                        s.getDiaChiDayDu().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(s);
                }
            }
            list = filtered;
        }

        filteredStoresList = list;
        storeAdapter.notifyDataSetChanged();

        binding.tvStoreCount.setText("Tìm thấy " + filteredStoresList.size() + " cửa hàng gần bạn");
    }

    private void updateUIState(SearchState state) {
        currentState = state;
        if (state == SearchState.NORMAL) {
            binding.tvTitle.setVisibility(View.VISIBLE);
            binding.btnSearch.setVisibility(View.VISIBLE);
            binding.clSearchBar.setVisibility(View.GONE);
            binding.cvRegionSelector.setVisibility(View.VISIBLE);
            binding.tvStoreCount.setVisibility(View.VISIBLE);
            binding.rvStores.setVisibility(View.VISIBLE);
            binding.llSearchHistory.setVisibility(View.GONE);
            binding.flBottomButtonContainer.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);

            binding.etSearch.setText("");
            filterStores();
        } else if (state == SearchState.SEARCH_HISTORY) {
            binding.tvTitle.setVisibility(View.GONE);
            binding.btnSearch.setVisibility(View.GONE);
            binding.clSearchBar.setVisibility(View.VISIBLE);
            binding.cvRegionSelector.setVisibility(View.GONE);
            binding.tvStoreCount.setVisibility(View.GONE);
            binding.rvStores.setVisibility(View.GONE);
            binding.llSearchHistory.setVisibility(View.VISIBLE);
            binding.flBottomButtonContainer.setVisibility(View.GONE);

            refreshHistoryList();
        } else if (state == SearchState.SEARCH_RESULTS) {
            binding.tvTitle.setVisibility(View.GONE);
            binding.btnSearch.setVisibility(View.GONE);
            binding.clSearchBar.setVisibility(View.VISIBLE);
            binding.cvRegionSelector.setVisibility(View.GONE);
            binding.tvStoreCount.setVisibility(View.VISIBLE);
            binding.rvStores.setVisibility(View.VISIBLE);
            binding.llSearchHistory.setVisibility(View.GONE);
            binding.flBottomButtonContainer.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);

            filterStores();
        }
    }

    private void performSearch() {
        updateUIState(SearchState.SEARCH_RESULTS);
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
    }

    private List<String> getSearchHistory() {
        SharedPreferences prefs = requireContext().getSharedPreferences("StoreSearchPrefs", Context.MODE_PRIVATE);
        String historyString = prefs.getString("search_history", "");
        if (historyString == null || historyString.isEmpty()) return new ArrayList<>();
        return Arrays.asList(historyString.split(","));
    }

    private void saveSearchHistory(List<String> history) {
        SharedPreferences prefs = requireContext().getSharedPreferences("StoreSearchPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("search_history", String.join(",", history)).apply();
    }

    private void addToSearchHistory(String query) {
        if (query.isEmpty()) return;
        List<String> current = new ArrayList<>(getSearchHistory());
        current.remove(query);
        current.add(0, query);
        if (current.size() > 10) current.remove(current.size() - 1);
        saveSearchHistory(current);
    }

    private void removeFromSearchHistory(String query) {
        List<String> current = new ArrayList<>(getSearchHistory());
        current.remove(query);
        saveSearchHistory(current);
    }

    private void clearSearchHistory() {
        saveSearchHistory(new ArrayList<>());
    }

    private void setupHistoryRecyclerView() {
        historyAdapter = new HistoryAdapter(getSearchHistory(), query -> {
            binding.etSearch.setText(query);
            performSearch();
        }, query -> {
            removeFromSearchHistory(query);
            refreshHistoryList();
        });
        binding.rvSearchHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchHistory.setAdapter(historyAdapter);
    }

    private void refreshHistoryList() {
        if (historyAdapter != null) {
            historyAdapter.updateItems(getSearchHistory());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
        private List<String> items;
        private final OnItemClickListener onItemClick;
        private final OnItemClickListener onDeleteClick;

        public HistoryAdapter(List<String> items, OnItemClickListener onItemClick, OnItemClickListener onDeleteClick) {
            this.items = items;
            this.onItemClick = onItemClick;
            this.onDeleteClick = onDeleteClick;
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemSearchHistoryBinding binding = ShopItemSearchHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new HistoryViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
            String item = items.get(position);
            holder.binding.tvSearchTerm.setText(item);
            holder.binding.getRoot().setOnClickListener(v -> onItemClick.onClick(item));
            holder.binding.btnDelete.setOnClickListener(v -> onDeleteClick.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateItems(List<String> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        public class HistoryViewHolder extends RecyclerView.ViewHolder {
            public ShopItemSearchHistoryBinding binding;
            public HistoryViewHolder(ShopItemSearchHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public interface OnItemClickListener {
        void onClick(String item);
    }

    public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

        @NonNull
        @Override
        public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemStoreBinding binding = ShopItemStoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new StoreViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
            StoreEntity store = filteredStoresList.get(position);
            holder.binding.tvStoreName.setText(store.getTenCuaHang());
            holder.binding.tvStoreHours.setText("Mở cửa từ " + (store.getMoCua() != null ? store.getMoCua() : "") + " đến " + (store.getDongCua() != null ? store.getDongCua() : ""));
            holder.binding.tvStoreAddress.setText(store.getDiaChiDayDu());

            holder.binding.ivRadio.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
            if (isSelectionMode) {
                if (store.getId().equals(selectedStoreId)) {
                    holder.binding.ivRadio.setImageResource(R.drawable.ic_circle_checked);
                    holder.binding.ivRadio.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                } else {
                    holder.binding.ivRadio.setImageResource(R.drawable.ic_circle);
                    holder.binding.ivRadio.setImageTintList(ColorStateList.valueOf(Color.parseColor("#807F7F")));
                }

                holder.binding.getRoot().setOnClickListener(v -> {
                    selectedStoreId = store.getId();
                    notifyDataSetChanged();
                });
            } else {
                holder.binding.getRoot().setOnClickListener(v -> {
                    ShopStoreDetailFragment detailFragment = ShopStoreDetailFragment.newInstance(store);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, detailFragment)
                            .addToBackStack(null)
                            .commit();
                });
            }

            holder.binding.btnDirections.setOnClickListener(v -> {
                try {
                    Uri mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + store.getLat() + "," + store.getLng());
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                    holder.itemView.getContext().startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(holder.itemView.getContext(), "Không thể mở bản đồ", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredStoresList.size();
        }

        public class StoreViewHolder extends RecyclerView.ViewHolder {
            public ShopItemStoreBinding binding;
            public StoreViewHolder(ShopItemStoreBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
