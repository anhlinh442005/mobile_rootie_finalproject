package com.veganbeauty.app.features.shop.barcode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;

public class BarcodeScanViewModel extends RootieViewModel {

    private final ProductRepository productRepository;
    private final MutableLiveData<BarcodeScanState> _scanState = new MutableLiveData<>(BarcodeScanState.Idle.INSTANCE);
    public final LiveData<BarcodeScanState> scanState = _scanState;

    private boolean isSearching = false;

    public BarcodeScanViewModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void lookupBarcode(String barcode) {
        if (isSearching) return;
        isSearching = true;
        _scanState.setValue(BarcodeScanState.Loading.INSTANCE);

        new Thread(() -> {
            try {
                ProductEntity product = productRepository.getProductByBarcode(barcode);
                isSearching = false;
                if (product != null) {
                    _scanState.postValue(new BarcodeScanState.Found(product));
                } else {
                    _scanState.postValue(new BarcodeScanState.NotFound(barcode));
                }
            } catch (Exception e) {
                isSearching = false;
                _scanState.postValue(new BarcodeScanState.NotFound(barcode));
            }
        }).start();
    }

    public void resetToScanning() {
        isSearching = false;
        _scanState.setValue(BarcodeScanState.Idle.INSTANCE);
    }
}
