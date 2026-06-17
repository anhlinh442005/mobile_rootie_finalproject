package com.veganbeauty.app.features.shop.barcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.launch

class BarcodeScanViewModel(
    private val productRepository: ProductRepository
) : RootieViewModel() {

    private val _scanState = MutableLiveData<BarcodeScanState>(BarcodeScanState.Idle)
    val scanState: LiveData<BarcodeScanState> = _scanState

    private var isSearching = false

    fun lookupBarcode(barcode: String) {
        if (isSearching) return
        isSearching = true
        _scanState.value = BarcodeScanState.Loading

        viewModelScope.launch {
            val product = productRepository.getProductByBarcode(barcode)
            isSearching = false
            _scanState.value = if (product != null) {
                BarcodeScanState.Found(product)
            } else {
                BarcodeScanState.NotFound(barcode)
            }
        }
    }

    fun resetToScanning() {
        isSearching = false
        _scanState.value = BarcodeScanState.Idle
    }
}

sealed class BarcodeScanState {
    data object Idle : BarcodeScanState()
    data object Loading : BarcodeScanState()
    data class Found(val product: ProductEntity) : BarcodeScanState()
    data class NotFound(val barcode: String) : BarcodeScanState()
}
