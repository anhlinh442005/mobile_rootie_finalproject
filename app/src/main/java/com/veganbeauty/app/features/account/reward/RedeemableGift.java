package com.veganbeauty.app.features.account.reward;

public class RedeemableGift {
    private final String giftId;
    private final String title;
    private final String description;
    private final int cost;
    private final String expiryDate;
    private final String code;
    private final String giftType;
    private String status;
    private final String productId;
    private final int minOrderValue;
    private final String applicableProducts;
    private final String offerType;
    private final int discountValue;
    private final String rankRequired;

    public RedeemableGift(String giftId, String title, String description, int cost,
                           String expiryDate, String code, String giftType, String status,
                           String productId, int minOrderValue, String applicableProducts,
                           String offerType, int discountValue, String rankRequired) {
        this.giftId = giftId; this.title = title; this.description = description;
        this.cost = cost; this.expiryDate = expiryDate; this.code = code;
        this.giftType = giftType; this.status = status; this.productId = productId;
        this.minOrderValue = minOrderValue;
        this.applicableProducts = applicableProducts != null ? applicableProducts : "Tất cả sản phẩm";
        this.offerType = offerType != null ? offerType : "fixed_amount";
        this.discountValue = discountValue;
        this.rankRequired = rankRequired != null ? rankRequired : "Đồng";
    }

    public String getGiftId() { return giftId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
    public String getExpiryDate() { return expiryDate; }
    public String getCode() { return code; }
    public String getGiftType() { return giftType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProductId() { return productId; }
    public int getMinOrderValue() { return minOrderValue; }
    public String getApplicableProducts() { return applicableProducts; }
    public String getOfferType() { return offerType; }
    public int getDiscountValue() { return discountValue; }
    public String getRankRequired() { return rankRequired; }
}
