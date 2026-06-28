package com.veganbeauty.app.features.shop.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.databinding.ShopFragmentStoresBottomSheetBinding;
import com.veganbeauty.app.databinding.ShopItemStoreBinding;

import java.util.ArrayList;
import java.util.List;

public class ShopStoresBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ShopStoresBottomSheetFragment";
    private static final String ARG_STORES_LIST = "stores_list";

    private ShopFragmentStoresBottomSheetBinding binding;
    private ArrayList<StoreEntity> storesList = new ArrayList<>();

    public static ShopStoresBottomSheetFragment newInstance(ArrayList<StoreEntity> stores) {
        ShopStoresBottomSheetFragment fragment = new ShopStoresBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STORES_LIST, stores);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storesList = (ArrayList<StoreEntity>) getArguments().getSerializable(ARG_STORES_LIST);
            if (storesList == null) storesList = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopFragmentStoresBottomSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvSheetTitle.setText("Tìm thấy " + storesList.size() + " cửa hàng gần bạn");

        binding.rvStoresList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStoresList.setAdapter(new StoresAdapter(storesList));
    }

    private void openGoogleMaps(double lat, double lng) {
        try {
            Uri mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            startActivity(mapIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không thể mở Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class StoresAdapter extends RecyclerView.Adapter<StoresAdapter.ViewHolder> {
        private final List<StoreEntity> items;

        StoresAdapter(List<StoreEntity> items) {
            this.items = items;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ShopItemStoreBinding itemBinding;

            ViewHolder(ShopItemStoreBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemStoreBinding binding = ShopItemStoreBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoreEntity store = items.get(position);
            ShopItemStoreBinding b = holder.itemBinding;

            b.tvStoreName.setText(store.getTenCuaHang());
            b.tvStoreHours.setText("Mở cửa từ " + (store.getMoCua() != null ? store.getMoCua() : "") + " đến " + (store.getDongCua() != null ? store.getDongCua() : ""));
            b.tvStoreAddress.setText(store.getDiaChiDayDu());
            b.ivRadio.setVisibility(View.GONE);

            b.btnDirections.setOnClickListener(v -> openGoogleMaps(store.getLat(), store.getLng()));

            b.getRoot().setOnClickListener(v -> {
                dismiss();
                ShopStoreDetailFragment detailFragment = ShopStoreDetailFragment.newInstance(store);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
