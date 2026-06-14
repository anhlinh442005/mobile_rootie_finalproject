package com.veganbeauty.app.features.account.expiry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpiryFilterState {
    ALL,
    EXPIRED,
    SOON,
    VALID
}

class AccountProductExpiryViewModel(
    private val repository: ProductRepository,
    private val userId: String
) : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedFilter = MutableLiveData<ExpiryFilterState>(ExpiryFilterState.ALL)
    val selectedFilter: LiveData<ExpiryFilterState> = _selectedFilter

    // Combined LiveData to trigger updates when either search query or filter changes
    private val filterAndQuery = MediatorLiveData<Pair<String, ExpiryFilterState>>().apply {
        addSource(_searchQuery) { query ->
            value = Pair(query.orEmpty(), _selectedFilter.value ?: ExpiryFilterState.ALL)
        }
        addSource(_selectedFilter) { filter ->
            value = Pair(_searchQuery.value.orEmpty(), filter ?: ExpiryFilterState.ALL)
        }
        value = Pair("", ExpiryFilterState.ALL)
    }

    // Baseline date for consistent calculations: June 4, 2026
    private val baselineDate: Date by lazy {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.parse("04/06/2026") ?: Date()
    }

    init {
        viewModelScope.launch {
            repository.seedExpiryProductsIfEmpty(userId)
        }
    }

    // Lọc tất cả sản phẩm theo tên tìm kiếm và trạng thái lọc
    val allExpiryProducts: LiveData<List<ExpiryProductUiModel>> = filterAndQuery.switchMap { (query, filter) ->
        repository.getExpiryProductsForUser(userId).map { products ->
            products.map { mapToUiModel(it) }
                .filter { uiModel ->
                    val matchesQuery = uiModel.product.name.contains(query, ignoreCase = true)
                    val matchesFilter = when (filter) {
                        ExpiryFilterState.ALL -> true
                        ExpiryFilterState.EXPIRED -> uiModel.remainingDays <= 0
                        ExpiryFilterState.SOON -> uiModel.isUrgent // diffDays in 1..14 (red warning)
                        ExpiryFilterState.VALID -> uiModel.remainingDays > 14
                    }
                    matchesQuery && matchesFilter
                }
                .sortedBy { it.remainingDays }
        }.asLiveData()
    }

    // Các sản phẩm sắp hết hạn (sắp xếp tăng dần theo số ngày còn lại, lọc dưới 2 tuần / báo đỏ)
    val soonExpiryProducts: LiveData<List<ExpiryProductUiModel>> = repository.getExpiryProductsForUser(userId).map { products ->
        products.map { mapToUiModel(it) }
            .filter { it.isUrgent } // diffDays in 1..14 (Báo đỏ / 2 tuần trở xuống)
            .sortedBy { it.remainingDays }
    }.asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: ExpiryFilterState) {
        _selectedFilter.value = filter
    }

    fun deleteExpiryProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteExpiryProduct(userId, productId)
        }
    }

    private fun mapToUiModel(product: ProductEntity): ExpiryProductUiModel {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expiry: Date? = try {
            sdf.parse(product.expiryDate)
        } catch (e: Exception) {
            null
        }

        if (expiry == null) {
            return ExpiryProductUiModel(product, 365, "HSD Không xác định", 10, false)
        }

        val diffMs = expiry.time - baselineDate.time
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        val isUrgent = diffDays in 1..14 // Cực kỳ gấp (dưới 2 tuần)
        val text: String
        val progress: Int

        if (diffDays <= 0) {
            text = "Hết hạn"
            progress = 100
        } else if (diffDays < 30) {
            val weeks = diffDays / 7
            text = if (weeks > 0) "Còn $weeks tuần" else "Còn $diffDays ngày"
            // High progress (red status)
            progress = 80 + (30 - diffDays) / 2
        } else {
            val months = diffDays / 30
            text = "Còn $months tháng"
            // Proportional progress (clamped)
            progress = ((730 - diffDays).coerceAtLeast(0) * 100 / 730).coerceIn(10, 60)
        }

        return ExpiryProductUiModel(product, diffDays, text, progress, isUrgent)
    }
}

data class ExpiryProductUiModel(
    val product: ProductEntity,
    val remainingDays: Int,
    val durationText: String,
    val progressPercent: Int,
    val isUrgent: Boolean
)
