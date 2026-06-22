package com.veganbeauty.app.features.account.notification

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment
import com.veganbeauty.app.features.myskin.BookingDetailUpcomingFragment
import com.veganbeauty.app.features.myskin.BookingDetailCompletedFragment
import com.veganbeauty.app.features.myskin.BookingDetailCancelledFragment
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

        if (action == "open_community_notification_list") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.community.notification.CommunityNotificationFragment())
                .addToBackStack(null)
                .commit()
            return
        }

        if (action == "open_skin_chat") {
            val dialog = com.veganbeauty.app.features.ai.SkinChatFragment()
            dialog.show(supportFragmentManager, "SkinChatDialog")
            return
        }

        if (action == "open_community_message_list") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.community.message.CommunityMessageFragment())
                .addToBackStack(null)
                .commit()
            return
        }

        if (action == "open_detail") {
            val type = (intent.getStringExtra("extra_notification_type") ?: "").lowercase()
            val voucherCode = intent.getStringExtra("extra_voucher_code")
            val orderId = intent.getStringExtra("extra_order_id")
            val scheduleId = intent.getStringExtra("extra_schedule_id")

            when {
                type == "voucher" || !voucherCode.isNullOrEmpty() -> {
                    val code = voucherCode ?: "RT50KDEC"
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
                type == "order" || !orderId.isNullOrEmpty() -> {
                    val actualOrderId = orderId ?: "RT8829"
                    val fragment = AccountOrderDetailFragment.newInstance(actualOrderId)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                type == "skin care" -> {
                    val fragment = SkinReminderFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                type == "checkin" -> {
                    val fragment = AccountCheckinFragment()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                type == "schedule date" || !scheduleId.isNullOrEmpty() -> {
                    val actualScheduleId = scheduleId ?: "BK_NOTI_101"
                    val userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(activity)
                    val bookings = LocalJsonReader(activity).getUserBookingHistory(userEmail)
                    val realBooking = bookings.find { it.id == actualScheduleId }
                    val fragment = if (realBooking != null) {
                        when (realBooking.status) {
                            "Đã hoàn thành" -> BookingDetailCompletedFragment.newInstance(realBooking)
                            "Đã huỷ" -> BookingDetailCancelledFragment.newInstance(realBooking)
                            else -> BookingDetailUpcomingFragment.newInstance(realBooking)
                        }
                    } else {
                        val mockBooking = BookingHistoryEntity(
                            id = actualScheduleId,
                            userId = "user_1",
                            userName = "Nguyễn Khánh Xuân",
                            userPhone = "0901234567",
                            userEmail = userEmail,
                            serviceName = "Chăm sóc da chuyên sâu Acne Free",
                            dateDisplay = "15 Tháng 6, 2026",
                            dayOfWeek = "Thứ Hai",
                            time = "14:00 - 15:30",
                            duration = "90 phút",
                            storeName = "Rootie Quận 1",
                            storeAddress = "123 Nguyễn Thị Minh Khai, Quận 1, TP. HCM",
                            status = "Sắp diễn ra"
                        )
                        BookingDetailUpcomingFragment.newInstance(mockBooking)
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    fun findVoucherByCode(context: Context, code: String): VoucherItem? {
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
