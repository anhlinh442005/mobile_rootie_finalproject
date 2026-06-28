package com.veganbeauty.app.features.account.expiry;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.AccountProductExpiryFragmentBinding;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.data.local.entities.ProductEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AccountProductExpiryFragment extends RootieFragment {

    private AccountProductExpiryFragmentBinding binding;
    private AccountProductExpiryViewModel viewModel;
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final AccountProductExpiryAdapter soonAdapter = new AccountProductExpiryAdapter(
            AccountProductExpiryAdapter.ExpiryLayoutMode.HORIZONTAL,
            this::navigateToDetail,
            this::showActionBottomSheet
    );

    private final AccountProductExpiryAdapter allAdapter = new AccountProductExpiryAdapter(
            AccountProductExpiryAdapter.ExpiryLayoutMode.GRID,
            this::navigateToDetail,
            this::showActionBottomSheet
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProductExpiryFragmentBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(
                db.productDao(),
                new LocalJsonReader(requireContext()),
                null,
                db.userProductExpiryDao()
        );
        String userId = ProfileSession.getUserId(requireContext());

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AccountProductExpiryViewModel(repository, userId);
            }
        }).get(AccountProductExpiryViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.rvSoonProducts.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvSoonProducts.setAdapter(soonAdapter);

        binding.rvAllProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvAllProducts.setAdapter(allAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s != null ? s.toString() : "");
            }
        });

        binding.btnFilter.setOnClickListener(v -> {
            ExpiryFilterBottomSheet filterBottomSheet = ExpiryFilterBottomSheet.newInstance();
            filterBottomSheet.show(getChildFragmentManager(), ExpiryFilterBottomSheet.TAG);
        });

        binding.btnViewAllSoon.setOnClickListener(v -> {
            SoonExpiryBottomSheet bottomSheet = SoonExpiryBottomSheet.newInstance();
            bottomSheet.show(getChildFragmentManager(), SoonExpiryBottomSheet.TAG);
        });
    }

    private void showActionBottomSheet(ExpiryProductUiModel uiModel) {
        ExpiryActionBottomSheet actionBottomSheet = ExpiryActionBottomSheet.newInstance(
                uiModel.getProduct().getName(),
                uiModel.getProduct().getExpiryDate(),
                uiModel.getProduct().getMainImage()
        );
        actionBottomSheet.setActionListener(new ExpiryActionBottomSheet.OnActionClickListener() {
            @Override
            public void onBuyAgain() {
                String productId = uiModel.getProduct().getId();
                requireActivity().runOnUiThread(() -> ProductDetailLauncher.open(AccountProductExpiryFragment.this, productId));
            }

            @Override
            public void onDelete() {
                viewModel.deleteExpiryProduct(uiModel.getProduct().getId());
                Toast.makeText(requireContext(), "Đã xoá " + uiModel.getProduct().getName() + " khỏi kệ", Toast.LENGTH_SHORT).show();
            }
        });
        actionBottomSheet.show(getChildFragmentManager(), ExpiryActionBottomSheet.TAG);
    }

    @Override
    protected void observeViewModel() {
        viewModel.soonExpiryProducts.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                soonAdapter.submitList(products.size() > 5 ? products.subList(0, 5) : products);
                if (products.isEmpty()) {
                    binding.rvSoonProducts.setVisibility(View.GONE);
                    binding.tvSoonTitle.setVisibility(View.GONE);
                    binding.btnViewAllSoon.setVisibility(View.GONE);
                } else {
                    binding.rvSoonProducts.setVisibility(View.VISIBLE);
                    binding.tvSoonTitle.setVisibility(View.VISIBLE);
                    binding.btnViewAllSoon.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.allExpiryProducts.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                allAdapter.submitList(products);
            }
        });
    }

    public void navigateToDetail(ExpiryProductUiModel uiModel) {
        AccountProductExpiryDetailFragment detailFragment = AccountProductExpiryDetailFragment.newInstance(uiModel.getProduct().getId());

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executorService.shutdown();
    }
}
