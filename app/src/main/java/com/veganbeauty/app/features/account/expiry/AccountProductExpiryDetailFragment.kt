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
    private var isWeek1Checked = false
    private var isWeek2Checked = false
    private var isNotificationChecked = false

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Vui lòng cấp quyền thông báo trong cài đặt để nhận nhắc nhở!", Toast.LENGTH_LONG).show()
        }
    }

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
        repository = ProductRepository(
            productDao = db.productDao(),
            localJsonReader = LocalJsonReader(requireContext()),
            userProductExpiryDao = db.userProductExpiryDao()
        )
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
                val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(requireContext())
                val product = repository.getExpiryProductById(userId, id)
                if (product != null) {
                    bindProductDetails(product)
                }
            }
        }

        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(requireContext())
        val pId = productId ?: ""

        // Load initial product-specific switches state
        val productNotiEnabled = com.veganbeauty.app.data.local.ProfileSession.getProductNotiEnabled(requireContext(), userId, pId)
        val productWeek1Enabled = com.veganbeauty.app.data.local.ProfileSession.getProductWeek1Enabled(requireContext(), userId, pId)
        val productWeek2Enabled = com.veganbeauty.app.data.local.ProfileSession.getProductWeek2Enabled(requireContext(), userId, pId)

        isNotificationChecked = productNotiEnabled
        isWeek1Checked = productWeek1Enabled
        isWeek2Checked = productWeek2Enabled

        updateSwitchUI(binding.switchNotification, binding.switchNotificationThumb, isNotificationChecked)
        updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, isWeek1Checked)
        updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, isWeek2Checked)
        
        binding.switchWeek1.isEnabled = isNotificationChecked
        binding.switchWeek1.alpha = if (isNotificationChecked) 1.0f else 0.5f
        binding.switchWeek2.isEnabled = isNotificationChecked
        binding.switchWeek2.alpha = if (isNotificationChecked) 1.0f else 0.5f

        // Configure Switch listeners
        binding.switchWeek1.setOnClickListener {
            if (!isNotificationChecked) return@setOnClickListener
            isWeek1Checked = !isWeek1Checked
            updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, isWeek1Checked)
            com.veganbeauty.app.data.local.ProfileSession.setProductWeek1Enabled(requireContext(), userId, pId, isWeek1Checked)
            if (isWeek1Checked) {
                checkAndRequestNotiPermission {
                    val productName = binding.tvProductName.text.toString()
                    triggerCustomExpiryNotification(1, productName)
                }
            } else {
                Toast.makeText(context, "Tắt nhắc nhở trước 1 tuần", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchWeek2.setOnClickListener {
            if (!isNotificationChecked) return@setOnClickListener
            isWeek2Checked = !isWeek2Checked
            updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, isWeek2Checked)
            com.veganbeauty.app.data.local.ProfileSession.setProductWeek2Enabled(requireContext(), userId, pId, isWeek2Checked)
            if (isWeek2Checked) {
                checkAndRequestNotiPermission {
                    val productName = binding.tvProductName.text.toString()
                    triggerCustomExpiryNotification(2, productName)
                }
            } else {
                Toast.makeText(context, "Tắt nhắc nhở trước 2 tuần", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchNotification.setOnClickListener {
            isNotificationChecked = !isNotificationChecked
            updateSwitchUI(binding.switchNotification, binding.switchNotificationThumb, isNotificationChecked)
            com.veganbeauty.app.data.local.ProfileSession.setProductNotiEnabled(requireContext(), userId, pId, isNotificationChecked)
            
            binding.switchWeek1.isEnabled = isNotificationChecked
            binding.switchWeek1.alpha = if (isNotificationChecked) 1.0f else 0.5f
            binding.switchWeek2.isEnabled = isNotificationChecked
            binding.switchWeek2.alpha = if (isNotificationChecked) 1.0f else 0.5f
            
            if (!isNotificationChecked) {
                isWeek1Checked = false
                isWeek2Checked = false
                updateSwitchUI(binding.switchWeek1, binding.switchWeek1Thumb, false)
                updateSwitchUI(binding.switchWeek2, binding.switchWeek2Thumb, false)
                com.veganbeauty.app.data.local.ProfileSession.setProductWeek1Enabled(requireContext(), userId, pId, false)
                com.veganbeauty.app.data.local.ProfileSession.setProductWeek2Enabled(requireContext(), userId, pId, false)
            }
            val status = if (isNotificationChecked) "Bật" else "Tắt"
            Toast.makeText(context, "$status nhận thông báo hạn sử dụng", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestNotiPermission(onGranted: () -> Unit) {
        val context = requireContext()
        val systemNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!systemNotificationsEnabled) {
            Toast.makeText(context, "Thông báo hiện đang bị tắt. Vui lòng bật trong Cài đặt hệ thống.", Toast.LENGTH_LONG).show()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                onGranted()
            } else {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onGranted()
        }
    }

    private fun triggerCustomExpiryNotification(weeks: Int, productName: String) {
        val context = requireContext()
        val titleText = "Chỉ còn $weeks tuần!"
        val messageText = "$productName của bạn sắp hết hạn. Kiểm tra ngay!"

        // 1. Show In-App Notification slide-down (Geofencing banner style)
        _binding?.let { b ->
            b.tvNotiTitle.text = titleText
            b.tvNotiMessage.text = messageText
            b.cvNotificationBanner.visibility = View.VISIBLE
            b.cvNotificationBanner.translationY = -300f
            
            b.cvNotificationBanner.animate()
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        // Automatically hide in-app banner after 5 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _binding?.let { b ->
                b.cvNotificationBanner.animate()
                    .translationY(-300f)
                    .setDuration(500)
                    .withEndAction {
                        b.cvNotificationBanner.visibility = View.GONE
                    }
                    .start()
            }
        }, 5000)

        // 2. Trigger System Notification
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "rootie_expiry_channel"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Nhắc nhở hết hạn sản phẩm"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = "Thông báo nhắc nhở hạn sử dụng mỹ phẩm"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.veganbeauty.app.R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(weeks * 1000 + 99, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
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

        // Dynamic circular progress ring color based on status (Expired/Urgent/Normal)
        val progressColor = when {
            diffDays <= 0 -> android.graphics.Color.parseColor("#8E8E8E") // Grey for expired
            diffDays <= 14 -> android.graphics.Color.parseColor("#C62828") // Red for urgent (<= 2 weeks)
            else -> android.graphics.Color.parseColor("#3E4D44") // Dark green for normal
        }
        binding.circularProgress.progressColor = progressColor
    }

    private fun updateSwitchUI(container: android.widget.FrameLayout, thumb: android.widget.ImageView, enabled: Boolean) {
        if (enabled) {
            container.setBackgroundResource(com.veganbeauty.app.R.drawable.ic_switch_track_on)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            lp.marginStart = 0
            lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        } else {
            container.setBackgroundResource(com.veganbeauty.app.R.drawable.ic_switch_track_off)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            lp.marginEnd = 0
            lp.marginStart = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
