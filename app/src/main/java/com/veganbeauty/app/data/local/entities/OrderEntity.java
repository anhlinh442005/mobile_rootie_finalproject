package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "orders")
@TypeConverters({OrderEntity.OrderConverters.class})
public class OrderEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String status;
    private String orderDate;
    private String orderTime;
    private long totalAmount;
    private long subTotal;
    
    private List<OrderItem> items;

    private boolean isGuest;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private long shippingCost;
    private long voucherDiscount;
    private String paymentMethod;
    private String expectedDeliveryTime;
    private String deliveryDate;
    
    private boolean isAffiliate;
    @Nullable
    @Embedded(prefix = "aff_")
    private AffiliateInfo affiliate;
    
    private boolean hasReview;
    private int reviewStars;
    private String reviewText;
    private String reviewImage;
    private boolean isAnonymous;
    private boolean recommendToFriends;
    
    private String billingName;
    private String billingPhone;
    private String billingEmail;
    private String orderNote;

    public OrderEntity(@NonNull String id) {
        this.id = id;
    }

    @androidx.room.Ignore
    public OrderEntity(
            @NonNull String id, String orderDate, String orderTime, String status, long totalAmount, long subTotal, List<OrderItem> items,
            String userId, boolean isGuest, String shippingName, String shippingPhone, String shippingAddress, long shippingCost, long voucherDiscount, String paymentMethod, String expectedDeliveryTime, boolean isAffiliate, int reviewStars, String reviewText, String reviewImage, boolean isAnonymous, boolean recommendToFriends, String billingName, String billingPhone, String billingEmail) {
        this.id = id;
        this.orderDate = orderDate;
        this.orderTime = orderTime;
        this.status = status;
        this.totalAmount = totalAmount;
        this.subTotal = subTotal;
        this.items = items;
        this.userId = userId;
        this.isGuest = isGuest;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingCost = shippingCost;
        this.voucherDiscount = voucherDiscount;
        this.paymentMethod = paymentMethod;
        this.expectedDeliveryTime = expectedDeliveryTime;
        this.isAffiliate = isAffiliate;
        this.reviewStars = reviewStars;
        this.reviewText = reviewText;
        this.reviewImage = reviewImage;
        this.isAnonymous = isAnonymous;
        this.recommendToFriends = recommendToFriends;
        this.billingName = billingName;
        this.billingPhone = billingPhone;
        this.billingEmail = billingEmail;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public String getOrderTime() { return orderTime; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }

    public long getSubTotal() { return subTotal; }
    public void setSubTotal(long subTotal) { this.subTotal = subTotal; }

    public List<OrderItem> getItems() { return items != null ? items : new ArrayList<>(); }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public long getShippingCost() { return shippingCost; }
    public void setShippingCost(long shippingCost) { this.shippingCost = shippingCost; }

    public long getVoucherDiscount() { return voucherDiscount; }
    public void setVoucherDiscount(long voucherDiscount) { this.voucherDiscount = voucherDiscount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getExpectedDeliveryTime() { return expectedDeliveryTime; }
    public void setExpectedDeliveryTime(String expectedDeliveryTime) { this.expectedDeliveryTime = expectedDeliveryTime; }

    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    public boolean isAffiliate() { return isAffiliate; }
    public void setAffiliate(boolean affiliate) { isAffiliate = affiliate; }

    @Nullable
    public AffiliateInfo getAffiliate() { return affiliate; }
    public void setAffiliate(@Nullable AffiliateInfo affiliate) { this.affiliate = affiliate; }

    public boolean isHasReview() { return hasReview; }
    public void setHasReview(boolean hasReview) { this.hasReview = hasReview; }

    public int getReviewStars() { return reviewStars; }
    public void setReviewStars(int reviewStars) { this.reviewStars = reviewStars; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getReviewImage() { return reviewImage; }
    public void setReviewImage(String reviewImage) { this.reviewImage = reviewImage; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { this.isAnonymous = anonymous; }

    public boolean isRecommendToFriends() { return recommendToFriends; }
    public void setRecommendToFriends(boolean recommendToFriends) { this.recommendToFriends = recommendToFriends; }

    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }

    public String getBillingPhone() { return billingPhone; }
    public void setBillingPhone(String billingPhone) { this.billingPhone = billingPhone; }

    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }

    public String getOrderNote() { return orderNote; }
    public void setOrderNote(String orderNote) { this.orderNote = orderNote; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderEntity that = (OrderEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status);
    }

    public static class OrderItem {
        private String productId;
        private String productName;
        private String productImage;
        private int quantity;
        private long price;

        public OrderItem() {}

        public OrderItem(String productId, String productName, String productImage, int quantity, long price) {
            this.productId = productId;
            this.productName = productName;
            this.productImage = productImage;
            this.quantity = quantity;
            this.price = price;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getProductImage() { return productImage; }
        public void setProductImage(String productImage) { this.productImage = productImage; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public long getPrice() { return price; }
        public void setPrice(long price) { this.price = price; }
    }

    public static class AffiliateInfo {
        private String affiliate_id;
        private String referrerUserId;
        private long commissionAmount;
        private String commissionStatus;

        public String getAffiliate_id() { return affiliate_id; }
        public void setAffiliate_id(String affiliate_id) { this.affiliate_id = affiliate_id; }
        public String getReferrerUserId() { return referrerUserId; }
        public void setReferrerUserId(String referrerUserId) { this.referrerUserId = referrerUserId; }
        public long getCommissionAmount() { return commissionAmount; }
        public void setCommissionAmount(long commissionAmount) { this.commissionAmount = commissionAmount; }
        public String getCommissionStatus() { return commissionStatus; }
        public void setCommissionStatus(String commissionStatus) { this.commissionStatus = commissionStatus; }
    }

    public static class OrderConverters {
        @TypeConverter
        public static List<OrderItem> fromString(String value) {
            Type listType = new TypeToken<List<OrderItem>>() {}.getType();
            return new Gson().fromJson(value, listType);
        }

        @TypeConverter
        public static String fromList(List<OrderItem> list) {
            return new Gson().toJson(list);
        }
    }
}
