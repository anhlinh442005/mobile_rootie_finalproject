package com.veganbeauty.app.features.shop.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopBottomSheetSuggestedProductsBinding;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class SuggestedProductsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SuggestedProductsBottomSheet";

    private ShopBottomSheetSuggestedProductsBinding binding;
    private ShopListAdapter productAdapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static SuggestedProductsBottomSheet newInstance() {
        return new SuggestedProductsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopBottomSheetSuggestedProductsBinding.inflate(inflater, container, false);
        
        productAdapter = new ShopListAdapter(
            product -> {
                navigateToDetail(product);
                dismiss();
            },
            this::showChooseQuantityBottomSheet
        );
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
        binding.rvProducts.setHasFixedSize(false);

        loadSuggestedProducts();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            View bottomSheet = ((BottomSheetDialog) getDialog()).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    private void loadSuggestedProducts() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        executor.execute(() -> {
            try {
                db.productDao().getAllProducts().collect(new FlowCollector<List<ProductEntity>>() {
                    @Override
                    public Object emit(List<ProductEntity> products, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        List<ProductEntity> suggested = new ArrayList<>();
                        if (products != null) {
                            for (ProductEntity product : products) {
                                if (product.isNew() || product.getPrice() >= 500000 || 
                                   (product.getCategory() != null && product.getCategory().toLowerCase().contains("combo"))) {
                                    suggested.add(product);
                                    if (suggested.size() >= 10) break;
                                }
                            }
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                productAdapter.submitList(suggested);
                            }
                        });
                        return kotlin.Unit.INSTANCE;
                    }
                }, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void navigateToDetail(ProductEntity product) {
        ProductDetailLauncher.open(this, product);
    }

    private void showChooseQuantityBottomSheet(ProductEntity product) {
        ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(
                product,
                new ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                    @Override
                    public void onAddToCartClick(ProductEntity p, int quantity) {
                        CartHelper.addToCart(requireContext(), androidx.lifecycle.LifecycleOwnerKt.getLifecycleScope(SuggestedProductsBottomSheet.this), p, quantity);
                    }

                    @Override
                    public void onBuyNowClick(ProductEntity p, int quantity) {
                        CartItemEntity checkoutItem = new CartItemEntity(
                                p.getId(), p.getName(), p.getMainImage(),
                                (long) p.getPrice(), quantity, true
                        );
                        ArrayList<CartItemEntity> items = new ArrayList<>();
                        items.add(checkoutItem);
                        ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.newInstance(items, "", 0L);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_container, checkoutFragment)
                                .addToBackStack(null)
                                .commit();
                        dismiss();
                    }
                }
        );
        bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
