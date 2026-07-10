package com.veganbeauty.app.features.ai;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;
import com.veganbeauty.app.data.local.entities.VoucherEntity;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.features.weather.SkinWeatherSnapshotManager;
import com.veganbeauty.app.utils.RewardPointsHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Đọc một lần toàn bộ trường dữ liệu user từ DB / prefs / JSON. */
public final class SkinAiAppDataLoader {

    private SkinAiAppDataLoader() {
    }

    public static SkinAiAppDataSnapshot load(Context context) {
        String userId = ProfileSession.getUserId(context);
        String email = ProfileSession.getEmail(context);
        String phone = ProfileSession.getPhone(context);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        RootieDatabase db = RootieDatabase.getDatabase(context);
        int totalCoins = RewardPointsHelper.getTotalPoints(context);
        int coinsToday = 0;
        List<SkinAiAppDataSnapshot.CoinEntry> todayCoins = new ArrayList<>();
        try {
            coinsToday = RewardPointsHelper.getPointsEarnedSince(context, startOfDay);
            List<RewardPointEntity> history = RewardPointsHelper.getHistorySince(context, startOfDay);
            if (history != null) {
                for (RewardPointEntity e : history) {
                    todayCoins.add(new SkinAiAppDataSnapshot.CoinEntry(e.getPoints(), e.getReason()));
                }
            }
        } catch (Exception ignored) {
        }

        SkinWeatherProfileHelper.UserSkinProfile profile = SkinWeatherProfileHelper.load(context);
        SkinWeatherSnapshotManager.Snapshot w = SkinWeatherSnapshotManager.loadLocal(context);
        boolean weatherOk = w != null && w.weatherSuccess;

        List<SkinAiAppDataSnapshot.OrderEntry> orders = new ArrayList<>();
        try {
            List<OrderEntity> list = db.orderDao()
                    .getOrdersForBuyerIdentitySync(userId, phone != null ? phone : "");
            if (list != null) {
                int limit = Math.min(5, list.size());
                for (int i = 0; i < limit; i++) {
                    OrderEntity o = list.get(i);
                    orders.add(new SkinAiAppDataSnapshot.OrderEntry(
                            o.getId(), o.getStatus(), o.getTotalAmount(), o.getOrderDate()));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.CartEntry> cart = new ArrayList<>();
        try {
            List<CartItemEntity> items = db.cartDao().getCartItemsSync();
            if (items != null) {
                for (CartItemEntity c : items) {
                    cart.add(new SkinAiAppDataSnapshot.CartEntry(c.getName(), c.getQuantity()));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.VoucherEntry> userVouchers = new ArrayList<>();
        try {
            String giftUserId = userId != null ? userId : "";
            List<UserGiftEntity> gifts = db.userGiftDao().getAllUserGiftsSync(giftUserId);
            if (gifts != null) {
                for (UserGiftEntity g : gifts) {
                    if (!"voucher_discount".equals(g.getGiftType())
                            && !"voucher_freeship".equals(g.getGiftType())) {
                        continue;
                    }
                    if (g.getStatus() != null && g.getStatus().toLowerCase(Locale.getDefault()).contains("hết hạn")) {
                        continue;
                    }
                    userVouchers.add(new SkinAiAppDataSnapshot.VoucherEntry(
                            g.getTitle(), g.getCode(), g.getStatus(), g.getExpiryDate(),
                            formatGiftDiscount(g)));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.VoucherEntry> systemVouchers = new ArrayList<>();
        try {
            List<VoucherEntity> sys = db.voucherDao().getActiveVouchers();
            if (sys == null || sys.isEmpty()) {
                sys = new LocalJsonReader(context).getVouchers();
            }
            if (sys != null) {
                int limit = Math.min(6, sys.size());
                for (int i = 0; i < limit; i++) {
                    VoucherEntity v = sys.get(i);
                    systemVouchers.add(new SkinAiAppDataSnapshot.VoucherEntry(
                            v.getTitle(), v.getCode(), v.isActive() ? "đang active" : "off",
                            v.getExpiryDate(), formatSystemDiscount(v)));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.GiftEntry> giftList = new ArrayList<>();
        try {
            String giftUserId = userId != null ? userId : "";
            List<UserGiftEntity> gifts = db.userGiftDao().getAllUserGiftsSync(giftUserId);
            if (gifts != null) {
                for (UserGiftEntity g : gifts) {
                    giftList.add(new SkinAiAppDataSnapshot.GiftEntry(
                            g.getTitle(), g.getStatus(), g.getGiftType()));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.BookingEntry> bookings = new ArrayList<>();
        try {
            String key = email != null && !email.isEmpty() ? email : userId;
            List<BookingHistoryEntity> list = new LocalJsonReader(context).getUserBookingHistory(key);
            if (list != null) {
                int limit = Math.min(3, list.size());
                for (int i = 0; i < limit; i++) {
                    BookingHistoryEntity b = list.get(i);
                    bookings.add(new SkinAiAppDataSnapshot.BookingEntry(
                            b.getServiceName(), b.getStatus(), b.getStoreName(), b.getDateDisplay()));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.ProductEntry> inUse = new ArrayList<>();
        try {
            List<UserProductExpiryEntity> products = db.userProductExpiryDao().getProductsByUserId(userId);
            if (products != null) {
                int limit = Math.min(6, products.size());
                for (int i = 0; i < limit; i++) {
                    UserProductExpiryEntity p = products.get(i);
                    inUse.add(new SkinAiAppDataSnapshot.ProductEntry(p.getName(), p.getExpiryDate()));
                }
            }
        } catch (Exception ignored) {
        }

        List<SkinAiAppDataSnapshot.StoreEntry> stores = new ArrayList<>();
        try {
            List<StoreEntity> list = new LocalJsonReader(context).getAllStores();
            if (list != null) {
                int limit = Math.min(4, list.size());
                for (int i = 0; i < limit; i++) {
                    StoreEntity s = list.get(i);
                    stores.add(new SkinAiAppDataSnapshot.StoreEntry(
                            s.getStoreName(), s.getAddress(), s.getOpenHours(), s.getSoDienThoai()));
                }
            }
        } catch (Exception ignored) {
        }

        int unread = 0;
        List<String> notifTitles = new ArrayList<>();
        try {
            List<NotificationItem> notifs = new LocalJsonReader(context).getAllNotifications();
            if (notifs != null) {
                for (NotificationItem n : notifs) {
                    if (!n.isRead()) unread++;
                }
                int limit = Math.min(3, notifs.size());
                for (int i = 0; i < limit; i++) {
                    notifTitles.add(notifs.get(i).getTitle());
                }
            }
        } catch (Exception ignored) {
        }

        return new SkinAiAppDataSnapshot(
                ProfileSession.getFullName(context),
                email,
                phone,
                ProfileSession.isLoggedIn(context),
                totalCoins,
                coinsToday,
                todayCoins,
                profile.hasSavedProfile,
                profile.skinType,
                profile.hydration,
                profile.sebum,
                profile.sensitivity,
                profile.elasticity,
                profile.recommendation,
                profile.skinAreas,
                profile.flaggedGroups,
                weatherOk,
                weatherOk && w != null ? w.city : "",
                weatherOk && w != null ? w.temp : 0,
                weatherOk && w != null ? w.humidity : 0,
                weatherOk && w != null ? w.uv : 0,
                weatherOk && w != null ? w.pm25 : 0,
                weatherOk && w != null && w.hasPm25,
                orders,
                cart,
                userVouchers,
                systemVouchers,
                giftList,
                bookings,
                inUse,
                formatRoutineSteps(ProfileSession.getMorningSteps(context)),
                formatRoutineSteps(ProfileSession.getEveningSteps(context)),
                stores,
                unread,
                notifTitles
        );
    }

    private static String formatRoutineSteps(Set<String> steps) {
        StringBuilder line = new StringBuilder();
        for (String step : steps) {
            String[] parts = step.split(":", 4);
            if (parts.length < 3) continue;
            boolean enabled = parts.length < 4 || "true".equalsIgnoreCase(parts[3]);
            if (!enabled) continue;
            if (line.length() > 0) line.append(" → ");
            line.append(parts[2]);
        }
        return line.length() > 0 ? line.toString() : "chưa bật bước nào";
    }

    private static String formatGiftDiscount(UserGiftEntity gift) {
        if ("voucher_freeship".equals(gift.getGiftType())) return "Freeship";
        if ("percentage".equals(gift.getOfferType())) return "Giảm " + gift.getDiscountValue() + "%";
        return "Giảm " + formatVnd(gift.getDiscountValue());
    }

    private static String formatSystemDiscount(VoucherEntity v) {
        if ("percentage".equals(v.getOfferType())) return "Giảm " + v.getDiscountValue() + "%";
        if ("free_ship".equals(v.getType()) || "freeship".equals(v.getType())) return "Freeship";
        return "Giảm " + formatVnd(v.getDiscountValue());
    }

    private static String formatVnd(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount).replace(',', '.');
    }
}
