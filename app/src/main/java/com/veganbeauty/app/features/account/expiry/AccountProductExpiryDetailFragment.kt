package com.veganbeauty.app.features.account.expiry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import coil.load
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.AccountProductExpiryDetailFragmentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountProductExpiryDetailFragment : RootieFragment() {

    private var _binding: AccountProductExpiryDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: ProductRepository
    private var productId: String? = null

    // Baseline date: June 4, 2026
    private val baselineDate: Date by lazy {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.parse("04/06/2026") ?: Date()
    }

    companion object {
        private const val ARG_PRODUCT_ID = "PRODUCT_ID"

        fun newInstance(productId: String): AccountProductExpiryDetailFragment {
            val fragment = AccountProductExpiryDetailFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_PRODUCT_ID, productId)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = arguments?.getString(ARG_PRODUCT_ID)
        val db = RootieDatabase.getDatabase(requireContext())
        repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProductExpiryDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        productId?.let { id ->
            lifecycleScope.launch {
                val db = RootieDatabase.getDatabase(requireContext())
                val product = db.productDao().getProductById(id)
                if (product != null) {
                    bindProductDetails(product)
                }
            }
        }

        // Configure Switch listeners
        binding.switchWeek1.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "Bật" else "Tắt"
            Toast.makeText(context, "$status nhắc nhở trước 1 tuần", Toast.LENGTH_SHORT).show()
        }

        binding.switchWeek2.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "Bật" else "Tắt"
            Toast.makeText(context, "$status nhắc nhở trước 2 tuần", Toast.LENGTH_SHORT).show()
        }

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "Bật" else "Tắt"
            Toast.makeText(context, "$status nhận thông báo hạn sử dụng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindProductDetails(product: ProductEntity) {
        binding.tvProductName.text = product.name
        binding.ivProductImage.load(product.mainImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        // Dynamic date calculations
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val expiry: Date? = try {
            sdf.parse(product.expiryDate)
        } catch (e: Exception) {
            null
        }

        if (expiry == null) {
            binding.tvPurchaseDate.text = "Mua hàng: Không xác định"
            binding.tvTotalShelfLife.text = "Không xác định"
            binding.tvRemainingValue.text = "--"
            binding.tvRemainingUnit.text = ""
            binding.circularProgress.progress = 0f
            return
        }

        // Calculate purchase date as 18 months before expiry date
        val cal = Calendar.getInstance()
        cal.time = expiry
        cal.add(Calendar.MONTH, -18)
        val purchaseDate = cal.time

        binding.tvPurchaseDate.text = "Mua hàng: ${sdf.format(purchaseDate)}"
        binding.tvTotalShelfLife.text = "18 tháng"

        // Calculate remaining time
        val diffMs = expiry.time - baselineDate.time
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()

        val valueText: String
        val unitText: String
        val ratio: Float

        if (diffDays <= 0) {
            valueText = "0"
            unitText = "ngày"
            ratio = 0.0f
        } else if (diffDays < 30) {
            val weeks = diffDays / 7
            if (weeks > 0) {
                valueText = String.format(Locale.getDefault(), "%02d", weeks)
                unitText = "tuần"
            } else {
                valueText = String.format(Locale.getDefault(), "%02d", diffDays)
                unitText = "ngày"
            }
            // Small remaining progress ratio
            ratio = diffDays.toFloat() / (18 * 30).toFloat()
        } else {
            val months = diffDays / 30
            valueText = String.format(Locale.getDefault(), "%02d", months)
            unitText = "tháng"
            ratio = diffDays.toFloat() / (18 * 30).toFloat()
        }

        binding.tvRemainingValue.text = valueText
        binding.tvRemainingUnit.text = unitText
        binding.circularProgress.progress = ratio
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
