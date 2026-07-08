package com.veganbeauty.app.features.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Ảnh chụp toàn bộ dữ liệu user trong app — AI trả lời từ đây, không bịa. */
public final class SkinAiAppDataSnapshot {

    public final String userName;
    public final String email;
    public final String phone;
    public final boolean loggedIn;

    public final int totalCoins;
    public final int coinsEarnedToday;
    public final List<CoinEntry> todayCoins;

    public final boolean hasSkinProfile;
    public final String skinType;
    public final int hydration;
    public final int sebum;
    public final int sensitivity;
    public final int elasticity;
    public final String recommendation;
    public final String skinAreas;
    public final Set<String> flaggedGroups;

    public final boolean weatherOk;
    public final String weatherCity;
    public final double weatherTemp;
    public final int weatherHumidity;
    public final double weatherUv;
    public final double weatherPm25;
    public final boolean hasPm25;

    public final List<OrderEntry> orders;
    public final List<CartEntry> cartItems;
    public final List<VoucherEntry> userVouchers;
    public final List<VoucherEntry> systemVouchers;
    public final List<GiftEntry> gifts;
    public final List<BookingEntry> bookings;
    public final List<ProductEntry> productsInUse;
    public final String routineMorning;
    public final String routineEvening;
    public final List<StoreEntry> stores;
    public final int notificationUnread;
    public final List<String> notificationTitles;

    public SkinAiAppDataSnapshot(
            String userName, String email, String phone, boolean loggedIn,
            int totalCoins, int coinsEarnedToday, List<CoinEntry> todayCoins,
            boolean hasSkinProfile, String skinType, int hydration, int sebum,
            int sensitivity, int elasticity, String recommendation, String skinAreas,
            Set<String> flaggedGroups,
            boolean weatherOk, String weatherCity, double weatherTemp, int weatherHumidity,
            double weatherUv, double weatherPm25, boolean hasPm25,
            List<OrderEntry> orders, List<CartEntry> cartItems,
            List<VoucherEntry> userVouchers, List<VoucherEntry> systemVouchers,
            List<GiftEntry> gifts, List<BookingEntry> bookings,
            List<ProductEntry> productsInUse, String routineMorning, String routineEvening,
            List<StoreEntry> stores, int notificationUnread, List<String> notificationTitles) {
        this.userName = userName;
        this.email = email;
        this.phone = phone;
        this.loggedIn = loggedIn;
        this.totalCoins = totalCoins;
        this.coinsEarnedToday = coinsEarnedToday;
        this.todayCoins = todayCoins != null ? todayCoins : new ArrayList<>();
        this.hasSkinProfile = hasSkinProfile;
        this.skinType = skinType;
        this.hydration = hydration;
        this.sebum = sebum;
        this.sensitivity = sensitivity;
        this.elasticity = elasticity;
        this.recommendation = recommendation;
        this.skinAreas = skinAreas;
        this.flaggedGroups = flaggedGroups;
        this.weatherOk = weatherOk;
        this.weatherCity = weatherCity;
        this.weatherTemp = weatherTemp;
        this.weatherHumidity = weatherHumidity;
        this.weatherUv = weatherUv;
        this.weatherPm25 = weatherPm25;
        this.hasPm25 = hasPm25;
        this.orders = orders != null ? orders : new ArrayList<>();
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.userVouchers = userVouchers != null ? userVouchers : new ArrayList<>();
        this.systemVouchers = systemVouchers != null ? systemVouchers : new ArrayList<>();
        this.gifts = gifts != null ? gifts : new ArrayList<>();
        this.bookings = bookings != null ? bookings : new ArrayList<>();
        this.productsInUse = productsInUse != null ? productsInUse : new ArrayList<>();
        this.routineMorning = routineMorning;
        this.routineEvening = routineEvening;
        this.stores = stores != null ? stores : new ArrayList<>();
        this.notificationUnread = notificationUnread;
        this.notificationTitles = notificationTitles != null ? notificationTitles : new ArrayList<>();
    }

    public static final class CoinEntry {
        public final int points;
        public final String reason;

        public CoinEntry(int points, String reason) {
            this.points = points;
            this.reason = reason;
        }
    }

    public static final class OrderEntry {
        public final String id;
        public final String status;
        public final long totalAmount;
        public final String orderDate;

        public OrderEntry(String id, String status, long totalAmount, String orderDate) {
            this.id = id;
            this.status = status;
            this.totalAmount = totalAmount;
            this.orderDate = orderDate;
        }
    }

    public static final class CartEntry {
        public final String name;
        public final int quantity;

        public CartEntry(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }
    }

    public static final class VoucherEntry {
        public final String title;
        public final String code;
        public final String status;
        public final String expiry;
        public final String discountLabel;

        public VoucherEntry(String title, String code, String status, String expiry, String discountLabel) {
            this.title = title;
            this.code = code;
            this.status = status;
            this.expiry = expiry;
            this.discountLabel = discountLabel;
        }
    }

    public static final class GiftEntry {
        public final String title;
        public final String status;
        public final String type;

        public GiftEntry(String title, String status, String type) {
            this.title = title;
            this.status = status;
            this.type = type;
        }
    }

    public static final class BookingEntry {
        public final String service;
        public final String status;
        public final String store;
        public final String date;

        public BookingEntry(String service, String status, String store, String date) {
            this.service = service;
            this.status = status;
            this.store = store;
            this.date = date;
        }
    }

    public static final class ProductEntry {
        public final String name;
        public final String expiry;

        public ProductEntry(String name, String expiry) {
            this.name = name;
            this.expiry = expiry;
        }
    }

    public static final class StoreEntry {
        public final String name;
        public final String address;
        public final String hours;
        public final String phone;

        public StoreEntry(String name, String address, String hours, String phone) {
            this.name = name;
            this.address = address;
            this.hours = hours;
            this.phone = phone;
        }
    }
}
