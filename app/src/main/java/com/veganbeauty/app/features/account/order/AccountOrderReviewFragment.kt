package com.veganbeauty.app.features.account.order

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountOrderReviewFragmentBinding
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

class AccountOrderReviewFragment : RootieFragment() {

    private var _binding: AccountOrderReviewFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: OrderRepository
    private var orderId: String = ""
    private var order: OrderEntity? = null

    private var selectedStars = 0
    private var selectedImageUrl: String? = null
    private var isEditMode = false
    private var isSummaryMode = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUrl = uri.toString()
            binding.cardImg1.visibility = View.VISIBLE
            binding.ivImg1.load(uri) {
                crossfade(true)
            }
            Toast.makeText(context, "Đã thêm ảnh từ thư viện!", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val path = saveBitmapToCache(bitmap)
            if (path != null) {
                selectedImageUrl = path
                binding.cardImg1.visibility = View.VISIBLE
                binding.ivImg1.load(File(path)) {
                    crossfade(true)
                }
                Toast.makeText(context, "Đã chụp ảnh thành công!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val cacheDir = requireContext().cacheDir
            val file = File(cacheDir, "review_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountOrderReviewFragmentBinding.inflate(inflater, container, false)
        orderId = arguments?.getString(ARG_ORDER_ID) ?: ""
        setupRepository()
        return binding.root
    }

    private fun setupRepository() {
        val db = RootieDatabase.getDatabase(requireContext())
        repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Notification navigation
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup Star rating clicks
        val starsList = listOf(binding.ivStar1, binding.ivStar2, binding.ivStar3, binding.ivStar4, binding.ivStar5)
        for (i in starsList.indices) {
            starsList[i].setOnClickListener {
                if (!isSummaryMode || isEditMode) {
                    setStars(i + 1)
                }
            }
        }

        // Setup Word count watcher
        binding.etReviewText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                binding.tvWordCount.text = "${words.size}/200 từ"
                if (words.size > 200) {
                    binding.tvWordCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text))
                } else {
                    binding.tvWordCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup Image Picker
        binding.btnAddImage.setOnClickListener {
            if (!isSummaryMode || isEditMode) {
                showImageSelectionDialog()
            }
        }

        // Setup submission
        binding.btnSubmit.setOnClickListener {
            handleSubmitAction()
        }

        loadOrderData()
    }

    private fun loadOrderData() {
        lifecycleScope.launch {
            repository.getOrderById(orderId).collect { ord ->
                if (ord != null) {
                    order = ord
                    bindOrderDetails(ord)
                }
            }
        }
    }

    private fun bindOrderDetails(ord: OrderEntity) {
        val firstItem = ord.items.firstOrNull() ?: return
        binding.tvProductName.text = firstItem.productName
        
        val attribute = when {
            firstItem.productName.contains("50ml") -> "Dung tích: 50ml"
            firstItem.productName.contains("30ml") -> "Dung tích: 30ml"
            firstItem.productName.contains("70ml") -> "Dung tích: 70ml"
            firstItem.productName.contains("100ml") -> "Dung tích: 100ml"
            else -> "Phân loại: 50ml"
        }
        binding.tvProductVariant.text = attribute

        binding.ivProductImage.load(firstItem.productImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        // Configure layout based on whether order already has review
        if (ord.hasReview && !isEditMode) {
            enterSummaryMode(ord)
        } else if (!isEditMode) {
            enterCreateMode()
        }
    }

    private fun enterCreateMode() {
        isSummaryMode = false
        isEditMode = false
        binding.tvTitle.text = "Đánh giá"
        binding.btnSubmit.text = "Gửi đánh giá"
        setEditable(true)
    }

    private fun enterSummaryMode(ord: OrderEntity) {
        isSummaryMode = true
        isEditMode = false
        binding.tvTitle.text = "Đánh giá của bạn"
        binding.btnSubmit.text = "Chỉnh sửa"
        
        // Populate inputs
        setStars(ord.reviewStars)
        binding.etReviewText.setText(ord.reviewText)
        binding.cbAnonymous.isChecked = ord.isAnonymous
        binding.cbRecommend.isChecked = ord.recommendToFriends

        if (!ord.reviewImage.isNullOrEmpty()) {
            selectedImageUrl = ord.reviewImage
            binding.cardImg1.visibility = View.VISIBLE
            if (ord.reviewImage.startsWith("/") || ord.reviewImage.contains("/cache/")) {
                binding.ivImg1.load(File(ord.reviewImage))
            } else {
                binding.ivImg1.load(ord.reviewImage)
            }
        } else {
            binding.cardImg1.visibility = View.GONE
        }

        setEditable(false)
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.tvTitle.text = "Chỉnh sửa đánh giá"
        binding.btnSubmit.text = "Cập nhật đánh giá"
        setEditable(true)
    }

    private fun setEditable(editable: Boolean) {
        binding.etReviewText.isEnabled = editable
        binding.cbAnonymous.isEnabled = editable
        binding.cbRecommend.isEnabled = editable
        binding.btnAddImage.visibility = if (editable) View.VISIBLE else View.GONE
        
        // Star clicks are filtered by isSummaryMode/isEditMode state in setOnClickListener
    }

    private fun setStars(stars: Int) {
        selectedStars = stars
        val starsList = listOf(binding.ivStar1, binding.ivStar2, binding.ivStar3, binding.ivStar4, binding.ivStar5)
        for (i in starsList.indices) {
            if (i < stars) {
                starsList[i].imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondary))
            } else {
                starsList[i].imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light))
            }
        }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("Chụp ảnh camera", "Chọn từ thư viện")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thêm hình ảnh đánh giá")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhotoLauncher.launch(null)
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun handleSubmitAction() {
        if (isSummaryMode && !isEditMode) {
            enterEditMode()
            return
        }

        // Validate stars
        if (selectedStars == 0) {
            Toast.makeText(requireContext(), "Vui lòng chọn số sao đánh giá (bắt buộc)!", Toast.LENGTH_SHORT).show()
            return
        }

        val text = binding.etReviewText.text?.toString() ?: ""
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        if (words.size > 200) {
            Toast.makeText(requireContext(), "Nhận xét không được vượt quá 200 từ!", Toast.LENGTH_SHORT).show()
            return
        }

        // Submit review
        lifecycleScope.launch {
            val awardedPoints = repository.updateOrderReview(
                orderId = orderId,
                stars = selectedStars,
                text = text,
                image = selectedImageUrl,
                isAnonymous = binding.cbAnonymous.isChecked,
                recommend = binding.cbRecommend.isChecked
            )

            if (awardedPoints) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Thành công!")
                    .setMessage("Đánh giá của bạn đã được ghi nhận. Bạn nhận được +200 xu thưởng vào tài khoản!")
                    .setPositiveButton("Tuyệt vời") { _, _ ->
                        parentFragmentManager.popBackStack()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Đã lưu đánh giá thành công!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"

        fun newInstance(orderId: String): AccountOrderReviewFragment {
            val fragment = AccountOrderReviewFragment()
            val args = Bundle()
            args.putString(ARG_ORDER_ID, orderId)
            fragment.arguments = args
            return fragment
        }
    }
}

