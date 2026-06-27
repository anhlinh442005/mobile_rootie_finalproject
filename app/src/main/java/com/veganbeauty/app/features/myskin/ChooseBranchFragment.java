package com.veganbeauty.app.features.myskin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.ArrayList;
import java.util.List;

public class ChooseBranchFragment extends RootieFragment {

    private RecyclerView rvBranches;
    private EditText etSearch;
    private TextView filterAll;
    private TextView filterHcm;
    private TextView filterHn;
    private TextView filterDn;
    private TextView btnContinue;
    private ImageView btnBack;

    private BranchAdapter branchAdapter;
    private List<StoreEntity> allStores = new ArrayList<>();
    private String currentFilter = "ALL";
    private String currentQuery = "";
    private StoreEntity selectedStore = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_fragment_choose_branch, container, false);
    }

    @Override
    public void setupUI(@NonNull View view) {
        rvBranches = view.findViewById(R.id.rv_branches);
        etSearch = view.findViewById(R.id.et_search);
        filterAll = view.findViewById(R.id.filter_all);
        filterHcm = view.findViewById(R.id.filter_hcm);
        filterHn = view.findViewById(R.id.filter_hn);
        filterDn = view.findViewById(R.id.filter_dn);
        btnContinue = view.findViewById(R.id.btn_continue);
        btnBack = view.findViewById(R.id.btn_back);

        LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
        List<StoreEntity> loadedStores = jsonReader.getAllStores();
        if (loadedStores != null) {
            allStores = loadedStores;
        }

        rvBranches.setLayoutManager(new LinearLayoutManager(getContext()));
        branchAdapter = new BranchAdapter(allStores, store -> {
            selectedStore = store;
        });
        rvBranches.setAdapter(branchAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filterAll.setOnClickListener(v -> setFilter("ALL", filterAll));
        filterHcm.setOnClickListener(v -> setFilter("Hồ Chí Minh", filterHcm));
        filterHn.setOnClickListener(v -> setFilter("Hà Nội", filterHn));
        filterDn.setOnClickListener(v -> setFilter("Đà Nẵng", filterDn));

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnContinue.setOnClickListener(v -> {
            if (selectedStore != null) {
                BookingFragment bookingFragment = BookingFragment.newInstance(
                        selectedStore.getTenCuaHang(),
                        selectedStore.getDiaChiDayDu(),
                        ""
                );
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.slide_in_left,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.slide_out_right
                        )
                        .replace(R.id.main_container, bookingFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Vui lòng chọn một chi nhánh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    private void setFilter(String province, TextView selectedTextView) {
        currentFilter = province;

        int unselectedBg = R.drawable.skin_bg_store_card;
        int unselectedColor = ContextCompat.getColor(requireContext(), R.color.primary);

        filterAll.setBackgroundResource(unselectedBg);
        filterAll.setTextColor(unselectedColor);

        filterHcm.setBackgroundResource(unselectedBg);
        filterHcm.setTextColor(unselectedColor);

        filterHn.setBackgroundResource(unselectedBg);
        filterHn.setTextColor(unselectedColor);

        filterDn.setBackgroundResource(unselectedBg);
        filterDn.setTextColor(unselectedColor);

        selectedTextView.setBackgroundResource(R.drawable.skin_bg_btn_book);
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral));

        applyFilters();
    }

    private void applyFilters() {
        List<StoreEntity> filteredList = new ArrayList<>();

        for (StoreEntity store : allStores) {
            boolean matchesProvince = currentFilter.equals("ALL") || 
                    store.getTinhThanh().toLowerCase().contains(currentFilter.toLowerCase()) || 
                    store.getDiaChiDayDu().toLowerCase().contains(currentFilter.toLowerCase());

            boolean matchesQuery = currentQuery.isEmpty() || 
                    store.getTenCuaHang().toLowerCase().contains(currentQuery.toLowerCase()) || 
                    store.getDiaChiDayDu().toLowerCase().contains(currentQuery.toLowerCase());

            if (matchesProvince && matchesQuery) {
                filteredList.add(store);
            }
        }

        branchAdapter.updateData(filteredList);
    }
}
