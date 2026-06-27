package com.veganbeauty.app.data.local.entities;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "orders")
public class OrderEntity {

    @PrimaryKey
    @NotNull
    private String id;
    private String userId;
    private String status;
    private String orderDate;
    private String orderTime;
    private long totalAmount;
    private long subTotal;
    
    // We will use TypeConverters to handle List<OrderItem>
    // Assuming you have OrderItem class and appropriate converters
    private String items; // Keeping it as a string for JSON or you can keep it as List<OrderItem> if you have a TypeConverter

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

    // Constructors
    public OrderEntity(@NotNull String id) {
        this.id = id;
    }

    // Getters and setters
    @NotNull
    public String getId() { return id; }
    public void setId(@NotNull String id) { this.id = id; }

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

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

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

    public AffiliateInfo getAffiliate() { return affiliate; }
    public void setAffiliate(AffiliateInfo affiliate) { this.affiliate = affiliate; }

    public boolean isHasReview() { return hasReview; }
    public void setHasReview(boolean hasReview) { this.hasReview = hasReview; }

    public int getReviewStars() { return reviewStars; }
    public void setReviewStars(int reviewStars) { this.reviewStars = reviewStars; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getReviewImage() { return reviewImage; }
    public void setReviewImage(String reviewImage) { this.reviewImage = reviewImage; }

    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }

    public boolean isRecommendToFriends() { return recommendToFriends; }
    public void setRecommendToFriends(boolean recommendToFriends) { this.recommendToFriends = recommendToFriends; }

    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }

    public String getBillingPhone() { return billingPhone; }
    public void setBillingPhone(String billingPhone) { this.billingPhone = billingPhone; }

    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String billingEmail) { this.billingEmail = billingEmail; }

    public static class OrderItem {
        private String productId;
        private String productName;
        private String productImage;
        private int quantity;
        private long price;

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
        private boolean isAffiliateOrder;
        private String affiliate_id;
        private String affiliateCode;
        private String referrerUserId;
        private String referrerName;
        private String sourceType = "community_post";
        private String sourcePostId;
        private double commissionRate;
        private long commissionAmount;
        private String commissionStatus;

        public boolean isAffiliateOrder() { return isAffiliateOrder; }
        public void setAffiliateOrder(boolean affiliateOrder) { isAffiliateOrder = affiliateOrder; }
        public String getAffiliate_id() { return affiliate_id; }
        public void setAffiliate_id(String affiliate_id) { this.affiliate_id = affiliate_id; }
        public String getAffiliateCode() { return affiliateCode; }
        public void setAffiliateCode(String affiliateCode) { this.affiliateCode = affiliateCode; }
        public String getReferrerUserId() { return referrerUserId; }
        public void setReferrerUserId(String referrerUserId) { this.referrerUserId = referrerUserId; }
        public String getReferrerName() { return referrerName; }
        public void setReferrerName(String referrerName) { this.referrerName = referrerName; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getSourcePostId() { return sourcePostId; }
        public void setSourcePostId(String sourcePostId) { this.sourcePostId = sourcePostId; }
        public double getCommissionRate() { return commissionRate; }
        public void setCommissionRate(double commissionRate) { this.commissionRate = commissionRate; }
        public long getCommissionAmount() { return commissionAmount; }
        public void setCommissionAmount(long commissionAmount) { this.commissionAmount = commissionAmount; }
        public String getCommissionStatus() { return commissionStatus; }
        public void setCommissionStatus(String commissionStatus) { this.commissionStatus = commissionStatus; }
    }
}
