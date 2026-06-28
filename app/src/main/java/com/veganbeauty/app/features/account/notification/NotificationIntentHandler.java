package com.veganbeauty.app.features.account.notification;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment;
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment;
import com.veganbeauty.app.features.myskin.BookingDetailUpcomingFragment;
import com.veganbeauty.app.features.myskin.BookingDetailCompletedFragment;
import com.veganbeauty.app.features.myskin.BookingDetailCancelledFragment;
import com.veganbeauty.app.features.profile.AccountVoucherDetailFragment;
import com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem;
import com.veganbeauty.app.features.routine.SkinReminderFragment;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.message.CommunityMessageFragment;
import com.veganbeauty.app.features.ai.SkinChatFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NotificationIntentHandler {

    public static void handleIntent(AppCompatActivity activity, Intent intent) {
        if (intent == null) return;
        String action = intent.getStringExtra("extra_notification_action");
        if (action == null) return;
        
        // Remove the extras so they don't trigger again on config change
        intent.removeExtra("extra_notification_action");

        FragmentManager supportFragmentManager = activity.getSupportFragmentManager();

        if (action.equals("open_notification_list")) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit();
            return;
        }

        if (action.equals("open_community_notification_list")) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, new CommunityNotificationFragment())
                .addToBackStack(null)
                .commit();
            return;
        }

        if (action.equals("open_skin_chat")) {
            SkinChatFragment dialog = new SkinChatFragment();
            dialog.show(supportFragmentManager, "SkinChatDialog");
            return;
        }

        if (action.equals("open_community_message_list")) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_container, new CommunityMessageFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss();
            return;
        }

        if (action.equals("open_detail")) {
            String typeRaw = intent.getStringExtra("extra_notification_type");
            String type = typeRaw != null ? typeRaw.toLowerCase() : "";
            String voucherCode = intent.getStringExtra("extra_voucher_code");
            String orderId = intent.getStringExtra("extra_order_id");
            String scheduleId = intent.getStringExtra("extra_schedule_id");

            if (type.equals("voucher") || (voucherCode != null && !voucherCode.isEmpty())) {
                String code = voucherCode != null ? voucherCode : "RT50KDEC";
                VoucherItem voucher = findVoucherByCode(activity, code);
                if (voucher == null) {
                    voucher = new VoucherItem(
                        "noti_voucher_1",
                        "Voucher giảm 50K sống xanh!",
                        "Nhận ngay mã GREEN50 giảm 50.000đ cho đơn hàng mỹ phẩm thuần chay tiếp theo tại ví voucher của bạn.",
                        code,
                        "valid",
                        "2026-12-31 23:59:59",
                        "discount",
                        false,
                        1,
                        300000,
                        "Tất cả sản phẩm",
                        "fixed_amount",
                        50000
                    );
                }
                AccountVoucherDetailFragment fragment = AccountVoucherDetailFragment.newInstance(voucher);
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
            } else if (type.equals("order") || (orderId != null && !orderId.isEmpty())) {
                String actualOrderId = orderId != null ? orderId : "RT8829";
                AccountOrderDetailFragment fragment = AccountOrderDetailFragment.newInstance(actualOrderId);
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
            } else if (type.equals("skin care")) {
                SkinReminderFragment fragment = new SkinReminderFragment();
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
            } else if (type.equals("checkin")) {
                AccountCheckinFragment fragment = new AccountCheckinFragment();
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
            } else if (type.equals("schedule date") || (scheduleId != null && !scheduleId.isEmpty())) {
                String actualScheduleId = scheduleId != null ? scheduleId : "BK_NOTI_101";
                String userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(activity);
                List<BookingHistoryEntity> bookings = new LocalJsonReader(activity).getUserBookingHistory(userEmail);
                BookingHistoryEntity realBooking = null;
                for (BookingHistoryEntity b : bookings) {
                    if (b.getId().equals(actualScheduleId)) {
                        realBooking = b;
                        break;
                    }
                }
                androidx.fragment.app.Fragment fragment;
                if (realBooking != null) {
                    String status = realBooking.getStatus();
                    if (status.equals("Đã hoàn thành")) {
                        fragment = BookingDetailCompletedFragment.newInstance(realBooking);
                    } else if (status.equals("Đã huỷ")) {
                        fragment = BookingDetailCancelledFragment.newInstance(realBooking);
                    } else {
                        fragment = BookingDetailUpcomingFragment.newInstance(realBooking);
                    }
                } else {
                    BookingHistoryEntity mockBooking = new BookingHistoryEntity(
                        actualScheduleId,
                        "user_1",
                        "Nguyễn Khánh Xuân",
                        "0901234567",
                        userEmail,
                        "Chăm sóc da chuyên sâu Acne Free",
                        "15 Tháng 6, 2026",
                        "Thứ Hai",
                        "14:00 - 15:30",
                        "90 phút",
                        "Rootie Quận 1",
                        "123 Nguyễn Thị Minh Khai, Quận 1, TP. HCM",
                        "Sắp diễn ra"
                    );
                    fragment = BookingDetailUpcomingFragment.newInstance(mockBooking);
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
            }
        }
    }

    public static VoucherItem findVoucherByCode(Context context, String code) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("vouchers.json"), StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            JSONArray jsonArray = new JSONArray(builder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.optString("code", "").equalsIgnoreCase(code)) {
                    String hsdStr = obj.optString("hsd", "");
                    return new VoucherItem(
                        obj.optString("id", ""),
                        obj.optString("title", ""),
                        obj.optString("description", ""),
                        obj.optString("code", ""),
                        "valid",
                        hsdStr,
                        obj.optString("type", "discount"),
                        obj.optBoolean("from-gift", false),
                        obj.has("quantity") ? obj.getInt("quantity") : null,
                        obj.optInt("minOrderValue", 0),
                        obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        obj.optString("offerType", "fixed_amount"),
                        obj.optInt("discountValue", 0)
                    );
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
