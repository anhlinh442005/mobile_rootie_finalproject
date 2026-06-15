package com.veganbeauty.app.features.account.notification

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment
import com.veganbeauty.app.features.myskin.BookingDetailUpcomingFragment
import com.veganbeauty.app.features.profile.AccountVoucherDetailFragment
import com.veganbeauty.app.features.profile.VoucherItem
import com.veganbeauty.app.features.routine.SkinReminderFragment

object NotificationIntentHandler {

    @JvmStatic
    fun handleIntent(activity: AppCompatActivity, intent: Intent?) {
        if (intent == null) return
        val action = intent.getStringExtra("extra_notification_action") ?: return
        
        // Remove the extras so they don't trigger again on config change
        intent.removeExtra("extra_notification_action")

        val supportFragmentManager = activity.supportFragmentManager

        if (action == "open_notification_list") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
            return
        }

        if (action == "open_detail") {
            val type = intent.getStringExtra("extra_notification_type") ?: ""
            when (type.lowercase()) {
                "voucher" -> {
                    val code = intent.getStringExtra("extra_voucher_code") ?: "RT50KDEC"
                    val voucher = findVoucherByCode(activity, code) ?: VoucherItem(
                        id = "noti_voucher_1",
                        title = "Voucher giảm 50K sống xanh!",
                        description = "Nhận ngay mã GREEN50 giảm 50.000đ cho đơn hàng mỹ phẩm thuần chay tiếp theo tại ví voucher của bạn.",
                        code = code,
                        status = "valid",
                        hsd = "2026-12-31 23:59:59",
                        type = "discount",
                        fromGift = false,
                        quantity = 1,
                        minOrderValue = 300000,
                        applicableProducts = "Tất cả sản phẩm",
                        offerType = "fixed_amount",
                        discountValue = 50000
                    )
                    val fragment = AccountVoucherDetailFragment.newInstance(voucher)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                "order" -> {
                    val orderId = intent.getStringExtra("extra_order_id") ?: "ORDER_PLACEHOLDER"
                    val fragment = AccountOrderDetailFragment.newInstance(orderId)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                "skin care" -> {
                    val fragment = SkinReminderFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                "checkin" -> {
                    val fragment = AccountCheckinFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                "schedule date" -> {
                    val scheduleId = intent.getStringExtra("extra_schedule_id") ?: "BK_NOTI_101"
                    val mockBooking = BookingHistoryEntity(
                        id = scheduleId,
                        serviceName = "Chăm sóc da chuyên sâu Acne Free",
                        dateDisplay = "15 Tháng 6, 2026",
                        dayOfWeek = "Thứ Hai",
                        time = "14:00 - 15:30",
                        duration = "90 phút",
                        storeName = "Rootie Quận 1",
                        storeAddress = "123 Nguyễn Thị Minh Khai, Quận 1, TP. HCM",
                        status = "Sắp diễn ra"
                    )
                    val fragment = BookingDetailUpcomingFragment.newInstance(mockBooking)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    private fun findVoucherByCode(context: Context, code: String): VoucherItem? {
        return try {
            val jsonString = context.assets.open("vouchers.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("code", "").equals(code, ignoreCase = true)) {
                    val hsdStr = obj.optString("hsd", "")
                    return VoucherItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        code = obj.optString("code", ""),
                        status = "valid",
                        hsd = hsdStr,
                        type = obj.optString("type", "discount"),
                        fromGift = obj.optBoolean("from-gift", false),
                        quantity = if (obj.has("quantity")) obj.getInt("quantity") else null,
                        minOrderValue = obj.optInt("minOrderValue", 0),
                        applicableProducts = obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        offerType = obj.optString("offerType", "fixed_amount"),
                        discountValue = obj.optInt("discountValue", 0)
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
