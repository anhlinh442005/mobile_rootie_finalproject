package com.veganbeauty.app.features.account.expiry

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountProductExpiryViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // Baseline date for consistent calculations: June 4, 2026
    private val baselineDate: Date by lazy {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.parse("04/06/2026") ?: Date()
    }

    // Lọc tất cả sản phẩm theo tên tìm kiếm
    val allExpiryProducts: LiveData<List<ExpiryProductUiModel>> = _searchQuery.switchMap { query ->
        repository.allProducts.map { products ->
            products.map { mapToUiModel(it) }
                .filter { it.product.name.contains(query, ignoreCase = true) }
                .sortedBy { it.remainingDays }
        }.asLiveData()
    }

    // Các sản phẩm sắp hết hạn (sắp xếp tăng dần theo số ngày còn lại, lấy tối đa 5 sản phẩm có số ngày còn lại ít nhất)
    val soonExpiryProducts: LiveData<List<ExpiryProductUiModel>> = repository.allProducts.map { products ->
        products.map { mapToUiModel(it) }
            .filter { it.remainingDays in 1..90 } // Dưới 3 tháng
            .sortedBy { it.remainingDays }
            .take(5)
    }.asLiveData()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

        val isUrgent = diffDays in 0..30 // Cực kỳ gấp (dưới 1 tháng)
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
