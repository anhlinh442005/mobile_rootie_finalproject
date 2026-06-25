package com.veganbeauty.app.features.shop.product

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.ShopOrderSuccessBinding
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment
import com.veganbeauty.app.features.account.order.AccountOrderListFragment
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity
import com.veganbeauty.app.features.shop.home.ShopHomeFragment
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShopOrderSuccessFragment : RootieFragment() {

    private var _binding: ShopOrderSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopOrderSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val orderCode = arguments?.getString(ARG_ORDER_CODE)
            ?: generateMockOrderCode()
        val totalAmount = arguments?.getLong(ARG_TOTAL_AMOUNT, 0L) ?: 0L
        val paymentMethod = arguments?.getString(ARG_PAYMENT_METHOD)
            ?: "Thanh toán tiền mặt khi nhận hàng"
        val isGuest = arguments?.getBoolean(ARG_IS_GUEST, false) ?: false
        val notificationType = arguments?.getString(ARG_NOTIFICATION_TYPE, "sms") ?: "sms"
        val isStorePickup = arguments?.getBoolean(ARG_IS_STORE_PICKUP, false) ?: false
        val storeName = arguments?.getString(ARG_STORE_NAME)

        binding.tvOrderCode.text = "#$orderCode"
        binding.tvTotalAmount.text = formatCurrency(totalAmount)
        binding.tvPaymentMethod.text = paymentMethod

        if (isStorePickup) {
            binding.tvHeaderTitle.text = "Đăng ký nhận tại cửa hàng"
            binding.tvSuccessTitle.text = "Đăng ký nhận tại cửa hàng thành công!"
            
            val baseMsg = "Cảm ơn bạn đã mua sắm tại Rootie. Đơn hàng của bạn đang được chuẩn bị. Vui lòng đến nhận hàng tại cửa hàng đã chọn khi nhận được thông báo."
            if (isGuest) {
                val notificationMsg = when (notificationType) {
                    "email" -> "Xác nhận đăng ký nhận tại cửa hàng đã được gửi qua email của bạn."
                    else -> "Xác nhận đăng ký nhận tại cửa hàng đã được gửi qua SMS đến số điện thoại của bạn."
                }
                binding.tvSuccessSubtitle.text = "$baseMsg $notificationMsg"
            } else {
                binding.tvSuccessSubtitle.text = baseMsg
            }
            
            binding.tvEstimatedDeliveryLabel.text = "Cửa hàng nhận"
            binding.tvEstimatedDelivery.text = storeName ?: "Cửa hàng Rootie"
            binding.tvTimelineStep3.text = "Sẵn sàng nhận hàng"
        } else {
            binding.tvHeaderTitle.text = "Đặt hàng thành công"
            binding.tvSuccessTitle.text = "Đặt hàng thành công!"
            
            val baseMsg = "Cảm ơn bạn đã mua sắm tại Rootie. Đơn hàng của bạn đang được xử lý."
            if (isGuest) {
                val notificationMsg = when (notificationType) {
                    "email" -> "Xác nhận đặt hàng đã được gửi qua email của bạn."
                    else -> "Xác nhận đặt hàng đã được gửi qua SMS đến số điện thoại của bạn."
                }
                binding.tvSuccessSubtitle.text = "$baseMsg $notificationMsg"
            } else {
                binding.tvSuccessSubtitle.text = baseMsg
            }
            
            binding.tvEstimatedDeliveryLabel.text = "Dự kiến giao hàng"
            binding.tvEstimatedDelivery.text = calculateEstimatedDelivery()
            binding.tvTimelineStep3.text = "Đang giao hàng"
        }

        // The guest onboarding banner is only rendered for guest checkouts.
        // Tapping "Đăng ký ngay" sends the user back to the welcome flow
        // (which already prefills the email/phone once we wire it in).
        if (isGuest) {
            binding.llGuestOnboarding.visibility = View.VISIBLE
            binding.btnGuestCreateAccount.setOnClickListener {
                val intent = Intent(requireContext(), HomeWelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            binding.tvGuestSkip.setOnClickListener {
                binding.llGuestOnboarding.visibility = View.GONE
            }
        } else {
            binding.llGuestOnboarding.visibility = View.GONE
        }

        if (isGuest) {
            binding.btnViewOrderStatus.visibility = View.GONE
            binding.btnBackToShop.text = "Quay về trang chủ"
            val params = binding.btnBackToShop.layoutParams as? android.widget.LinearLayout.LayoutParams
            params?.topMargin = 0
            binding.btnBackToShop.layoutParams = params
        } else {
            binding.btnViewOrderStatus.visibility = View.VISIBLE
            binding.btnBackToShop.text = "Quay về cửa hàng"
        }

        binding.btnViewOrderStatus.setOnClickListener {
            navigateToOrderStatus()
        }

        binding.btnBackToShop.setOnClickListener {
            navigateToShopHome()
        }

        // Show Slide-down push-notification in-app banner for guest checkouts
        if (isGuest) {
            val recipientName = arguments?.getString(ARG_RECIPIENT_NAME)
            val greeting = if (!recipientName.isNullOrBlank()) "$recipientName ơi, " else ""
            
            val titleText: String
            val messageText: String

            if (notificationType == "email") {
                if (isStorePickup) {
                    titleText = "Xác nhận nhận tại cửa hàng qua Email"
                    messageText = "${greeting}chúng tôi đã nhận được đơn hàng #$orderCode. Xác nhận nhận tại cửa hàng đã được gửi qua email của bạn."
                } else {
                    titleText = "Xác nhận đặt hàng qua Email"
                    messageText = "${greeting}chúng tôi đã nhận được đơn hàng #$orderCode. Xác nhận đã được gửi qua email của bạn."
                }
            } else {
                if (isStorePickup) {
                    titleText = "Xác nhận nhận tại cửa hàng qua SMS"
                    messageText = "${greeting}cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #$orderCode. Vui lòng đến cửa hàng đã chọn để nhận hàng."
                } else {
                    titleText = "Xác nhận đặt hàng qua SMS"
                    messageText = "${greeting}cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #$orderCode. Đơn hàng đang được chuẩn bị."
                }
            }

            binding.tvNotiTitle.text = titleText
            binding.tvNotiMessage.text = messageText

            binding.cvNotificationBanner.visibility = View.VISIBLE
            binding.cvNotificationBanner.translationY = -300f

            binding.cvNotificationBanner.animate()
                .translationY(0f)
                .setDuration(1200) // Slow slide-in
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        _binding?.let { b ->
                            b.cvNotificationBanner.animate()
                                .translationY(-300f)
                                .setDuration(800)
                                .withEndAction {
                                    b.cvNotificationBanner.visibility = View.GONE
                                }
                                .start()
                        }
                    }, 2500)
                }
                .start()
        }
    }

    private fun navigateToOrderStatus() {
        val orderCode = arguments?.getString(ARG_ORDER_CODE) ?: ""
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, AccountOrderDetailFragment.newInstance(orderCode, fromSuccess = true))
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToShopHome() {
        // Clear the entire back stack and replace with ShopHomeFragment so the user
        // is taken back to the shop landing page instead of the previous checkout flow.
        parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, ShopHomeFragment())
            .commit()
    }

    private fun generateMockOrderCode(): String {
        // Fallback ORD code
        val format = SimpleDateFormat("HHmmss", Locale("vi", "VN"))
        return "ORD-1" + format.format(Date())
    }

    private fun calculateEstimatedDelivery(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 2)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 2)
        val endDate = calendar.time

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        return "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
    }

    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_CODE = "arg_order_code"
        private const val ARG_TOTAL_AMOUNT = "arg_total_amount"
        private const val ARG_PAYMENT_METHOD = "arg_payment_method"
        private const val ARG_IS_GUEST = "arg_is_guest"
        private const val ARG_NOTIFICATION_TYPE = "arg_notification_type" // "email" or "sms"
        private const val ARG_IS_STORE_PICKUP = "arg_is_store_pickup"
        private const val ARG_STORE_NAME = "arg_store_name"
        private const val ARG_STORE_ADDRESS = "arg_store_address"
        private const val ARG_RECIPIENT_NAME = "arg_recipient_name"

        fun newInstance(
            orderCode: String? = null,
            totalAmount: Long = 0L,
            paymentMethod: String? = null,
            isGuest: Boolean = false,
            notificationType: String = "sms",
            isStorePickup: Boolean = false,
            storeName: String? = null,
            storeAddress: String? = null,
            recipientName: String? = null
        ): ShopOrderSuccessFragment {
            return ShopOrderSuccessFragment().apply {
                arguments = Bundle().apply {
                    if (!orderCode.isNullOrEmpty()) {
                        putString(ARG_ORDER_CODE, orderCode)
                    }
                    putLong(ARG_TOTAL_AMOUNT, totalAmount)
                    if (!paymentMethod.isNullOrEmpty()) {
                        putString(ARG_PAYMENT_METHOD, paymentMethod)
                    }
                    putBoolean(ARG_IS_GUEST, isGuest)
                    putString(ARG_NOTIFICATION_TYPE, notificationType)
                    putBoolean(ARG_IS_STORE_PICKUP, isStorePickup)
                    if (!storeName.isNullOrEmpty()) {
                        putString(ARG_STORE_NAME, storeName)
                    }
                    if (!storeAddress.isNullOrEmpty()) {
                        putString(ARG_STORE_ADDRESS, storeAddress)
                    }
                    if (!recipientName.isNullOrEmpty()) {
                        putString(ARG_RECIPIENT_NAME, recipientName)
                    }
                }
            }
        }
    }
}
