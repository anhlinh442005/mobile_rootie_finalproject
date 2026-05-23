package com.veganbeauty.app.features.account.order

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.OrderItem
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountOrderDetailFragmentBinding
import com.veganbeauty.app.databinding.AccountOrderDetailProductItemBinding
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import kotlinx.coroutines.launch

class AccountOrderDetailFragment : RootieFragment() {

    private var _binding: AccountOrderDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OrderDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountOrderDetailFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = OrderRepository(db.orderDao(), LocalJsonReader(requireContext()))
        val orderId = arguments?.getString(ARG_ORDER_ID) ?: ""

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OrderDetailViewModel(repository, orderId) as T
            }
        })[OrderDetailViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Notification Navigation
        val navigateToNotification = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.layoutNotification.setOnClickListener(navigateToNotification)
        binding.btnNotification.setOnClickListener(navigateToNotification)
    }

    override fun observeViewModel() {
        // Observe single order details reactively
        viewModel.order.observe(viewLifecycleOwner) { order ->
            if (order != null) {
                bindOrderDetails(order)
            }
        }

        // Observe notification badge dot reactively
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationRepository.getInstance(requireContext()).unreadCount.collect { count ->
                _binding?.viewNotificationBadge?.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun bindOrderDetails(order: OrderEntity) {
        val context = context ?: return

        // 1. General Info Card
        binding.tvOrderCode.text = order.orderId
        binding.tvOrderDate.text = "${order.orderDate} - ${order.orderTime}"

        // Badge styling based on status
        val (badgeBgRes, badgeTextRes) = when (order.status) {
            "Chờ xác nhận" -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
            "Đang xử lý" -> Pair(R.color.status_processing_bg, R.color.status_processing_text)
            "Đang giao" -> Pair(R.color.status_delivering_bg, R.color.status_delivering_text)
            "Thành công" -> Pair(R.color.status_success_bg, R.color.status_success_text)
            "Đã hủy" -> Pair(R.color.status_cancelled_bg, R.color.status_cancelled_text)
            else -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
        }
        binding.tvStatusBadge.background.mutate().setTint(ContextCompat.getColor(context, badgeBgRes))
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, badgeTextRes))
        binding.tvStatusBadge.text = order.status.uppercase()

        // 2. Banner Status Card
        setupStatusBanner(order, badgeBgRes, badgeTextRes)

        // 3. Products List Card
        binding.layoutProductsContainer.removeAllViews()
        var subtotal = 0L
        for (item in order.items) {
            subtotal += item.price * item.quantity
            val productBinding = AccountOrderDetailProductItemBinding.inflate(
                LayoutInflater.from(context),
                binding.layoutProductsContainer,
                false
            )
            
            // Set details
            productBinding.tvProductName.text = item.productName
            productBinding.tvProductQuantity.text = "x${item.quantity}"
            productBinding.tvProductPrice.text = formatCurrency(item.price)

            // Deduce dynamic attribute variant
            val attribute = when {
                item.productName.contains("50ml") -> "Dung tích: 50ml"
                item.productName.contains("30ml") -> "Dung tích: 30ml"
                item.productName.contains("70ml") -> "Dung tích: 70ml"
                item.productName.contains("140ml") -> "Dung tích: 140ml"
                item.productName.contains("100ml") -> "Dung tích: 100ml"
                item.productId == "product_hair_grapefruit" -> "Dung tích: 140ml"
                item.productId == "product_rose_cream" -> "Dung tích: 50ml"
                item.productName.contains("Combo") -> "Phân loại: Bộ sản phẩm"
                else -> "Dung tích: 100ml"
            }
            productBinding.tvProductAttribute.text = attribute

            // Load product thumbnail
            productBinding.ivProductImage.load(item.productImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }

            binding.layoutProductsContainer.addView(productBinding.root)
        }

        // 4. Recipient Details Card
        binding.tvAddressName.text = order.shippingName
        binding.tvAddressPhone.text = order.shippingPhone
        binding.tvAddressFull.text = order.shippingAddress

        // 5. Invoice Payments Card
        binding.tvInvoiceSubtotalValue.text = formatCurrency(subtotal)
        binding.tvInvoiceShippingValue.text = formatCurrency(order.shippingCost)
        binding.tvInvoiceVoucherValue.text = "- ${formatCurrency(order.voucherDiscount)}"
        binding.tvInvoiceTotalValue.text = formatCurrency(order.totalAmount)
        binding.tvInvoicePayment.text = order.paymentMethod

        // 6. Docked Bottom Actions Bar
        setupBottomActions(order)
    }

    private fun setupStatusBanner(order: OrderEntity, bgResId: Int, textResId: Int) {
        val context = context ?: return
        
        when (order.status) {
            "Chờ xác nhận" -> {
                binding.tvBannerStatus.text = "● Chờ xác nhận"
                binding.tvBannerDesc.text = "Đơn hàng của bạn sẽ được nhân viên xác nhận và tiến hành đóng gói đóng gói"
                binding.btnBannerSubAction.visibility = View.GONE
            }
            "Đang xử lý" -> {
                binding.tvBannerStatus.text = "● Đang xử lý"
                binding.tvBannerDesc.text = "Nhân viên đang tiến hành đóng gói và sẽ giao cho đơn vị vận chuyển"
                binding.btnBannerSubAction.visibility = View.GONE
            }
            "Đang giao" -> {
                binding.tvBannerStatus.text = "● Đang giao hàng"
                val deliveryTime = order.expectedDeliveryTime ?: "Hôm nay 18:00 - 20:00"
                binding.tvBannerDesc.text = "Dự kiến giao: $deliveryTime"
                binding.btnBannerSubAction.visibility = View.VISIBLE
                
                // Style button dynamically
                binding.btnBannerSubAction.backgroundTintList = ContextCompat.getColorStateList(context, bgResId)
                
                // Set text inside TextView
                binding.tvBannerSubActionText.text = "Theo dõi đơn"
                binding.tvBannerSubActionText.setTextColor(ContextCompat.getColor(context, textResId))
                
                // Set icon in ImageView and tint it perfectly
                binding.ivBannerSubActionIcon.visibility = View.VISIBLE
                binding.ivBannerSubActionIcon.setImageResource(R.drawable.ic_truck)
                binding.ivBannerSubActionIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, textResId))

                binding.btnBannerSubAction.setOnClickListener {
                    Toast.makeText(context, "Tính năng Theo dõi đơn cho mã ${order.orderId} đang được tải...", Toast.LENGTH_SHORT).show()
                }
            }
            "Thành công" -> {
                binding.tvBannerStatus.text = "● Thành công"
                binding.tvBannerDesc.text = "Đơn hàng đã giao thành công"
                binding.btnBannerSubAction.visibility = View.VISIBLE
                
                // Style button dynamically
                binding.btnBannerSubAction.backgroundTintList = ContextCompat.getColorStateList(context, bgResId)
                
                // Set text inside TextView
                binding.tvBannerSubActionText.text = "Đánh giá đơn hàng"
                binding.tvBannerSubActionText.setTextColor(ContextCompat.getColor(context, textResId))
                
                // Set icon in ImageView and tint it perfectly
                binding.ivBannerSubActionIcon.visibility = View.VISIBLE
                binding.ivBannerSubActionIcon.setImageResource(R.drawable.ic_leaf)
                binding.ivBannerSubActionIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, textResId))

                binding.btnBannerSubAction.setOnClickListener {
                    Toast.makeText(context, "Đang mở giao diện đánh giá đơn hàng ${order.orderId}...", Toast.LENGTH_SHORT).show()
                }
            }
            "Đã hủy" -> {
                binding.tvBannerStatus.text = "● Đã hủy"
                binding.tvBannerDesc.text = "Đơn hàng của bạn đã hủy"
                binding.btnBannerSubAction.visibility = View.GONE
            }
            else -> {
                binding.tvBannerStatus.text = "● ${order.status}"
                binding.tvBannerDesc.text = ""
                binding.btnBannerSubAction.visibility = View.GONE
            }
        }
    }

    private fun setupBottomActions(order: OrderEntity) {
        val context = context ?: return

        when (order.status) {
            "Chờ xác nhận", "Đang xử lý" -> {
                binding.btnActionLeft.visibility = View.VISIBLE
                binding.btnActionLeft.text = "Liên hệ hỗ trợ"
                binding.btnActionLeft.setOnClickListener {
                    Toast.makeText(context, "Đang kết nối đến tư vấn viên để hỗ trợ đơn ${order.orderId}...", Toast.LENGTH_SHORT).show()
                }

                binding.btnActionRight.visibility = View.VISIBLE
                binding.btnActionRight.text = "Hủy đơn"
                binding.btnActionRight.setOnClickListener {
                    showCancelConfirmationDialog(order)
                }
            }
            "Đang giao" -> {
                binding.btnActionLeft.visibility = View.VISIBLE
                binding.btnActionLeft.text = "Liên hệ hỗ trợ"
                binding.btnActionLeft.setOnClickListener {
                    Toast.makeText(context, "Đang kết nối đến tư vấn viên để hỗ trợ đơn ${order.orderId}...", Toast.LENGTH_SHORT).show()
                }

                binding.btnActionRight.visibility = View.VISIBLE
                binding.btnActionRight.text = "Đã nhận hàng"
                binding.btnActionRight.setOnClickListener {
                    showConfirmReceivedDialog(order)
                }
            }
            "Thành công" -> {
                binding.btnActionLeft.visibility = View.VISIBLE
                binding.btnActionLeft.text = "Trả hàng/Hoàn tiền"
                binding.btnActionLeft.setOnClickListener {
                    Toast.makeText(context, "Gửi yêu cầu Trả hàng/Hoàn tiền cho đơn ${order.orderId}...", Toast.LENGTH_SHORT).show()
                }

                binding.btnActionRight.visibility = View.VISIBLE
                binding.btnActionRight.text = "Mua lại"
                binding.btnActionRight.setOnClickListener {
                    Toast.makeText(context, "Đã thêm toàn bộ sản phẩm của đơn ${order.orderId} vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            }
            "Đã hủy" -> {
                binding.btnActionLeft.visibility = View.VISIBLE
                binding.btnActionLeft.text = "Liên hệ hỗ trợ"
                binding.btnActionLeft.setOnClickListener {
                    Toast.makeText(context, "Đang kết nối đến tư vấn viên để hỗ trợ đơn ${order.orderId}...", Toast.LENGTH_SHORT).show()
                }

                binding.btnActionRight.visibility = View.VISIBLE
                binding.btnActionRight.text = "Mua lại"
                binding.btnActionRight.setOnClickListener {
                    Toast.makeText(context, "Đã thêm toàn bộ sản phẩm của đơn ${order.orderId} vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                binding.btnActionLeft.visibility = View.GONE
                binding.btnActionRight.visibility = View.GONE
            }
        }
    }

    private fun showCancelConfirmationDialog(order: OrderEntity) {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng ${order.orderId} không?")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.cancelOrder()
                Toast.makeText(
                    context,
                    "Đã hủy đơn hàng ${order.orderId} thành công!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }

    private fun showConfirmReceivedDialog(order: OrderEntity) {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setTitle("Đã nhận được hàng")
            .setMessage("Bạn xác nhận đã nhận đầy đủ và nguyên vẹn các sản phẩm trong đơn hàng ${order.orderId}?")
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.confirmReceived()
                Toast.makeText(
                    context,
                    "Cảm ơn bạn đã mua sắm tại Rootie!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }

    private fun formatCurrency(amount: Long): String {
        return String.format("%,dđ", amount).replace(',', '.')
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"

        fun newInstance(orderId: String): AccountOrderDetailFragment {
            val fragment = AccountOrderDetailFragment()
            val args = Bundle()
            args.putString(ARG_ORDER_ID, orderId)
            fragment.arguments = args
            return fragment
        }
    }
}
