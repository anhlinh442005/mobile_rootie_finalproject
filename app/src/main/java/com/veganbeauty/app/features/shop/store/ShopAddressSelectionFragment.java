package com.veganbeauty.app.features.shop.store;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.repository.StoreRepository;
import com.veganbeauty.app.databinding.ShopFragmentAddressSelectionBinding;
import com.veganbeauty.app.databinding.ShopItemAddressRowBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class ShopAddressSelectionFragment extends RootieFragment {

    public enum SelectionStage {
        PROVINCE,
        DISTRICT
    }

    private SelectionStage currentStage = SelectionStage.PROVINCE;

    private ShopFragmentAddressSelectionBinding _binding;

    private RootieDatabase database;
    private StoreRepository repository;

    private List<StoreEntity> allStoresList = new ArrayList<>();
    private String selectedProvince = null;
    private String selectedDistrict = null;

    private List<String> displayedItems = new ArrayList<>();
    private AddressAdapter addressAdapter;

    private final List<String> vietnamProvinces = Arrays.asList(
            "Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng", "Huế",
            "Tỉnh An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
            "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận",
            "Cà Mau", "Cao Bằng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai",
            "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh", "Hải Dương",
            "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum",
            "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
            "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình",
            "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng", "Sơn La",
            "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế",
            "Tiền Giang", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = RootieDatabase.getDatabase(requireContext());
        repository = new StoreRepository(database.storeDao(), new LocalJsonReader(requireContext()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopFragmentAddressSelectionBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> {
            if (currentStage == SelectionStage.DISTRICT) {
                switchToProvinceStage();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        _binding.tvStep1Change.setOnClickListener(v -> switchToProvinceStage());

        _binding.btnClearSearch.setOnClickListener(v -> _binding.etSearch.setText(""));

        _binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                _binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                filterDisplayedList(query);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupRecyclerView();
        loadStores();
    }

    private void setupRecyclerView() {
        addressAdapter = new AddressAdapter();
        _binding.rvAddressItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvAddressItems.setAdapter(addressAdapter);
    }

    private void loadStores() {
        FlowLiveDataConversions.asLiveData(repository.getAllStores())
            .observe(getViewLifecycleOwner(), stores -> {
                if (stores != null) {
                    allStoresList = stores;
                    updateStageUI();
                }
            });
    }

    private void updateStageUI() {
        String query = _binding.etSearch.getText().toString().trim();
        if (currentStage == SelectionStage.PROVINCE) {
            _binding.clStep1.setBackgroundResource(R.drawable.bg_btn_outlined);
            _binding.tvStep1Text.setText(selectedProvince != null ? selectedProvince : "Chọn Tỉnh/Thành phố");
            _binding.tvStep1Change.setVisibility(View.GONE);
            _binding.viewStep1Dot.setBackgroundResource(R.drawable.bg_dialog_btn_confirm);

            _binding.viewConnectingLine.setVisibility(View.GONE);
            _binding.clStep2.setVisibility(View.GONE);

            _binding.etSearch.setHint("Nhập tìm Tỉnh/Thành phố");

            List<String> storeProvinces = new ArrayList<>();
            for (StoreEntity store : allStoresList) {
                String p = store.getTinhThanh();
                if (p != null && !p.isEmpty() && !storeProvinces.contains(p)) {
                    storeProvinces.add(p);
                }
            }

            List<String> combined = new ArrayList<>(storeProvinces);
            for (String vp : vietnamProvinces) {
                if (!combined.contains(vp)) {
                    combined.add(vp);
                }
            }
            displayedItems = combined;
            filterDisplayedList(query);
        } else {
            _binding.clStep1.setBackgroundResource(0);
            _binding.tvStep1Text.setText(selectedProvince);
            _binding.tvStep1Change.setVisibility(View.VISIBLE);
            _binding.viewStep1Dot.setBackgroundResource(R.drawable.bg_circle_grey);

            _binding.viewConnectingLine.setVisibility(View.VISIBLE);
            _binding.clStep2.setVisibility(View.VISIBLE);
            _binding.clStep2.setBackgroundResource(R.drawable.bg_btn_outlined);
            _binding.tvStep2Text.setText(selectedDistrict != null ? selectedDistrict : "Chọn Quận/Huyện");
            _binding.viewStep2Dot.setBackgroundResource(R.drawable.bg_dialog_btn_confirm);

            _binding.etSearch.setHint("Nhập tìm Quận/Huyện");

            String prov = selectedProvince != null ? selectedProvince : "";
            List<String> dbDistricts = new ArrayList<>();
            for (StoreEntity store : allStoresList) {
                if (prov.equalsIgnoreCase(store.getTinhThanh())) {
                    String d = store.getQuanHuyen();
                    if (d != null && !d.isEmpty() && !dbDistricts.contains(d)) {
                        dbDistricts.add(d);
                    }
                }
            }

            displayedItems = dbDistricts;
            filterDisplayedList(query);
        }
    }

    private void filterDisplayedList(String query) {
        List<String> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(displayedItems);
        } else {
            for (String item : displayedItems) {
                if (item.toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(item);
                }
            }
        }
        addressAdapter.submitList(filtered);
    }

    private void switchToProvinceStage() {
        currentStage = SelectionStage.PROVINCE;
        selectedProvince = null;
        selectedDistrict = null;
        _binding.etSearch.setText("");
        updateStageUI();
    }

    private void switchToDistrictStage(String province) {
        selectedProvince = province;
        selectedDistrict = null;
        currentStage = SelectionStage.DISTRICT;
        _binding.etSearch.setText("");
        updateStageUI();
    }

    private void selectDistrict(String district) {
        selectedDistrict = district;
        _binding.tvStep2Text.setText(district);
        
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(_binding.etSearch.getWindowToken(), 0);
        }

        String province = selectedProvince;
        if (province != null && !province.isEmpty() && !district.isEmpty()) {
            Bundle result = new Bundle();
            result.putString(RESULT_PROVINCE, province);
            result.putString(RESULT_DISTRICT, district);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

        private List<String> items = new ArrayList<>();

        public void submitList(List<String> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        class AddressViewHolder extends RecyclerView.ViewHolder {
            ShopItemAddressRowBinding itemBinding;

            public AddressViewHolder(ShopItemAddressRowBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }

        @NonNull
        @Override
        public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemAddressRowBinding binding = ShopItemAddressRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AddressViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
            String item = items.get(position);
            holder.itemBinding.tvAddressName.setText(item);
            holder.itemBinding.tvAddressName.setOnClickListener(v -> {
                if (currentStage == SelectionStage.PROVINCE) {
                    switchToDistrictStage(item);
                } else {
                    selectDistrict(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    public static final String TAG = "ShopAddressSelectionFragment";
    public static final String REQUEST_KEY = "address_selection_request";
    public static final String RESULT_PROVINCE = "result_province";
    public static final String RESULT_DISTRICT = "result_district";

    public static ShopAddressSelectionFragment newInstance() {
        return new ShopAddressSelectionFragment();
    }
}
