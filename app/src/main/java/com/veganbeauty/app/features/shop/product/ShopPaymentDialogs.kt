package com.veganbeauty.app.features.shop.product

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.utils.VietQRHelper
import java.text.NumberFormat
import java.util.Locale

/**
 * Helpers for the three checkout-payment flows:
 * - COD: handled directly in [ShopCheckoutFragment] (no dialog).
 * - ATM / VNPay: [showAtmVnpayQrDialog] shows a dialog with a dynamic VietQR
 *   code, then on "Kiểm tra thanh toán" hands the user off to the success screen.
 * - MoMo: [showMomoRedirectDialog] shows a dialog with a rotating MoMo
 *   gradient ring, then after ~2s unlocks a "Xác nhận" button that goes to
 *   the success screen.
 *
 * The "payment" is fully mocked: the success screen is the same one used
 * after a real successful payment so the rest of the app behaves identically.
 */
object ShopPaymentDialogs {

    // ----- Mock bank configuration (display only; real integration is out of scope) -----
    private const val MOCK_BANK_CODE = "VCB" // Vietcombank
    private const val MOCK_ACCOUNT_NUMBER = "9960123456"

    // ----- ATM / VNPay: dialog with dynamic QR code -----

    fun showAtmVnpayQrDialog(
        fragment: Fragment,
        orderCode: String,
        totalAmount: Long,
        isVnpay: Boolean
    ) {
        val ctx = fragment.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.shop_dialog_atm_vnpay_qr, null, false)
        val dialog = AlertDialog.Builder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val ivProviderLogo = view.findViewById<ImageView>(R.id.ivProviderLogo)
        val tvProviderName = view.findViewById<android.widget.TextView>(R.id.tvProviderName)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val tvAmount = view.findViewById<android.widget.TextView>(R.id.tvAmount)
        val tvOrderCode = view.findViewById<android.widget.TextView>(R.id.tvOrderCode)
        val btnCopy = view.findViewById<View>(R.id.btnCopy)
        val btnClose = view.findViewById<View>(R.id.btnClose)
        val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)

        // Provider visuals
        if (isVnpay) {
            ivProviderLogo.setImageResource(R.drawable.ic_vnpay)
            ivProviderLogo.imageTintList = null
            tvProviderName.text = "Cổng thanh toán VNPay"
        } else {
            ivProviderLogo.setImageResource(R.drawable.ic_credit_card)
            ivProviderLogo.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#3E4D44")
            )
            tvProviderName.text = "Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)"
        }

        // Money + order code
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        tvAmount.text = formatter.format(totalAmount)
        tvOrderCode.text = orderCode

        // Build dynamic VietQR URL
        val qrUrl = VietQRHelper.buildImageUrl(
            bankCode = MOCK_BANK_CODE,
            accountNumber = MOCK_ACCOUNT_NUMBER,
            amount = totalAmount,
            addInfo = orderCode,
            template = "qr_only"
        )
        ivQrCode.load(qrUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_qr_placeholder)
            error(R.drawable.ic_qr_placeholder)
        }

        btnCopy.setOnClickListener {
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Order code", orderCode))
            Toast.makeText(ctx, "Đã sao chép mã đơn hàng", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            // The checkout fragment handles order persistence and notification display
            navigateToOrderSuccess(
                fragment = fragment,
                orderCode = orderCode,
                totalAmount = totalAmount,
                paymentMethod = if (isVnpay) "Thanh toán trực tuyến VNPay" else "Thẻ ATM nội địa/Internet Banking"
            )
        }

        // If the user dismisses without confirming, do nothing — they can press
        // "Thanh toán" again to retry.
        dialog.show()
    }

    // ----- MoMo: rotating ring + delayed confirm button -----

    fun showMomoRedirectDialog(fragment: Fragment, orderCode: String, totalAmount: Long) {
        val ctx = fragment.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.shop_dialog_momo_redirect, null, false)
        val dialog = AlertDialog.Builder(ctx)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val tvOrderCode = view.findViewById<android.widget.TextView>(R.id.tvOrderCode)
        val btnClose = view.findViewById<View>(R.id.btnClose)
        val btnConfirm = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPayment)

        tvOrderCode.text = "Mã đơn: $orderCode"

        btnClose.setOnClickListener { dialog.dismiss() }

        // Simulate the 2-second redirect window: keep the button disabled with
        // a "Đang chờ mở MoMo..." label, then unlock it.
        btnConfirm.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            // Fragment may have moved on; only mutate the view if the dialog
            // is still showing.
            if (dialog.isShowing) {
                btnConfirm.isEnabled = true
                btnConfirm.text = "Xác nhận đã thanh toán"
            }
        }, 2_000L)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            // The checkout fragment handles order persistence and notification display
            navigateToOrderSuccess(
                fragment = fragment,
                orderCode = orderCode,
                totalAmount = totalAmount,
                paymentMethod = "Thanh toán trực tuyến MoMo"
            )
        }

        dialog.show()
    }

    // ----- Shared success-screen navigation -----

    private fun navigateToOrderSuccess(
        fragment: Fragment,
        orderCode: String,
        totalAmount: Long,
        paymentMethod: String
    ) {
        // The checkout fragment owns the [OrderEntity] build (it has
        // the form values), so ask it to persist the order before we
        // navigate. The [isGuest] flag is derived from the same
        // [ProfileSession.isLoggedIn] check the checkout fragment used
        // when it built the form, so the order row written to Room
        // matches the success-screen banner.
        val checkoutFragment = fragment as? ShopCheckoutFragment
        val isGuest = !com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(fragment.requireContext())

        // Determine notification type for guest: email if provided, otherwise SMS
        val isEmailNotification = if (isGuest && checkoutFragment != null) {
            checkoutFragment.hasGuestEmail()
        } else {
            false
        }

        val notificationType = if (isEmailNotification) "email" else "sms"

        if (checkoutFragment != null) {
            checkoutFragment.persistOrderFromDialog(
                orderCode = orderCode,
                totalAmount = totalAmount,
                method = paymentMethod,
                isGuest = isGuest,
                isEmailNotification = isEmailNotification
            )
            return
        }

        val isStorePickup = checkoutFragment?.isStorePickup() ?: false
        val storeName = checkoutFragment?.getSelectedStoreName()
        val storeAddress = checkoutFragment?.getSelectedStoreAddress()

        val fm: FragmentManager = fragment.parentFragmentManager
        val successFragment = ShopOrderSuccessFragment.newInstance(
            orderCode = orderCode,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            isGuest = isGuest,
            notificationType = notificationType,
            isStorePickup = isStorePickup,
            storeName = storeName,
            storeAddress = storeAddress
        )
        try {
            fm.popBackStack()
        } catch (_: Exception) {}
        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, successFragment)
            .commit()
    }
}
