package com.veganbeauty.app.features.shop.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.repository.StoreRepository;
import com.veganbeauty.app.databinding.ShopFragmentStoreDetailBinding;
import com.veganbeauty.app.databinding.ShopItemStoreBinding;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.flow.FlowCollector;

public class ShopStoreDetailFragment extends RootieFragment {

    public static final String TAG = "ShopStoreDetailFragment";
    private static final String ARG_STORE = "arg_store";

    private ShopFragmentStoreDetailBinding binding;
    private RootieDatabase database;
    private StoreRepository repository;

    private StoreEntity currentStore;
    private List<StoreEntity> nearbyStoresList = new ArrayList<>();
    private NearbyStoresAdapter nearbyAdapter;

    public static ShopStoreDetailFragment newInstance(StoreEntity store) {
        ShopStoreDetailFragment fragment = new ShopStoreDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STORE, store);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = RootieDatabase.getDatabase(requireContext());
        repository = new StoreRepository(database.storeDao(), new LocalJsonReader(requireContext()));

        if (getArguments() != null) {
            currentStore = (StoreEntity) getArguments().getSerializable(ARG_STORE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopFragmentStoreDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (currentStore != null) {
            binding.tvStoreNameDetail.setText(currentStore.getTenCuaHang());
            binding.tvStoreHoursDetail.setText("Mở cửa từ " + (currentStore.getMoCua() != null ? currentStore.getMoCua() : "") + " đến " + (currentStore.getDongCua() != null ? currentStore.getDongCua() : ""));
            binding.tvStoreAddressDetail.setText(currentStore.getDiaChiDayDu());
            populateAmenities();
        }

        nearbyAdapter = new NearbyStoresAdapter();
        binding.rvNearbyStores.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNearbyStores.setAdapter(nearbyAdapter);

        binding.btnXemChiDuong.setOnClickListener(v -> {
            if (currentStore != null) {
                openGoogleMaps(currentStore.getLat(), currentStore.getLng());
            }
        });

        binding.btnLienHe.setOnClickListener(v -> {
            if (currentStore != null) {
                dialPhoneNumber(currentStore.getSoDienThoai());
            }
        });

        loadNearbyStores();
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    private void populateAmenities() {
        binding.cgAmenitiesDetail.removeAllViews();
        if (currentStore != null && currentStore.getTienNghi() != null && !currentStore.getTienNghi().isEmpty()) {
            String[] items = currentStore.getTienNghi().split(",");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    TextView tv = new TextView(requireContext());
                    tv.setText(trimmed);
                    tv.setTextColor(getResources().getColor(R.color.primary, null));
                    tv.setBackgroundResource(R.drawable.bg_amenity_tag);
                    tv.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
                    tv.setTextSize(13f);
                    try {
                        tv.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    binding.cgAmenitiesDetail.addView(tv);
                }
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    private void loadNearbyStores() {
        LifecycleCoroutineScope scope = LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());


        // Use traditional coroutine launch via flow collection
        kotlinx.coroutines.BuildersKt.launch(scope, null, null, (coroutineScope, continuation) -> {
            repository.getAllStores().collect(new FlowCollector<List<StoreEntity>>() {
                @Nullable
                @Override
                public Object emit(List<StoreEntity> stores, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                    if (currentStore == null) return kotlin.Unit.INSTANCE;
                    String currentDistrict = currentStore.getQuanHuyen() != null ? currentStore.getQuanHuyen().trim() : "";
                    List<StoreEntity> filtered = new ArrayList<>();
                    for (StoreEntity s : stores) {
                        if (!s.getId().equals(currentStore.getId()) && currentDistrict.equalsIgnoreCase(s.getQuanHuyen())) {
                            filtered.add(s);
                        }
                    }
                    requireActivity().runOnUiThread(() -> {
                        nearbyStoresList = filtered;
                        nearbyAdapter.notifyDataSetChanged();
                        boolean hasNearby = !filtered.isEmpty();
                        binding.rvNearbyStores.setVisibility(hasNearby ? View.VISIBLE : View.GONE);
                        binding.tvNoNearbyStores.setVisibility(!hasNearby ? View.VISIBLE : View.GONE);
                    });
                    return kotlin.Unit.INSTANCE;
                }
            }, continuation);
            return kotlin.Unit.INSTANCE;
        });
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

    private void dialPhoneNumber(String phones) {
        if (phones == null || phones.isEmpty()) {
            Toast.makeText(requireContext(), "Cửa hàng chưa có số điện thoại liên hệ", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String[] parts = phones.split(",");
            String firstNumber = parts[0].trim();
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + firstNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public class NearbyStoresAdapter extends RecyclerView.Adapter<NearbyStoresAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final ShopItemStoreBinding itemBinding;

            public ViewHolder(ShopItemStoreBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemStoreBinding binding = ShopItemStoreBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoreEntity store = nearbyStoresList.get(position);
            ShopItemStoreBinding b = holder.itemBinding;

            b.tvStoreName.setText(store.getTenCuaHang());
            b.tvStoreHours.setText("Mở cửa từ " + (store.getMoCua() != null ? store.getMoCua() : "") + " đến " + (store.getDongCua() != null ? store.getDongCua() : ""));
            b.tvStoreAddress.setText(store.getDiaChiDayDu());
            b.ivRadio.setVisibility(View.GONE);

            b.getRoot().setOnClickListener(v -> {
                ShopStoreDetailFragment detailFragment = newInstance(store);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });

            b.btnDirections.setOnClickListener(v -> openGoogleMaps(store.getLat(), store.getLng()));
        }

        @Override
        public int getItemCount() {
            return nearbyStoresList.size();
        }
    }
}
