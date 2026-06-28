package com.veganbeauty.app.features.profile;

import android.os.Parcel;
import android.os.Parcelable;

public class VoucherItem implements Parcelable {
    private String id;
    private String title;
    private String content;
    private String code;
    private String status;
    private String expiration;
    private String type;
    private boolean isUsed;
    private int quantity;
    private long minOrder;
    private String condition;
    private String offerType;
    private long discount;

    public VoucherItem(String id, String title, String content, String code, String status, String expiration, String type, boolean isUsed, int quantity, long minOrder, String condition, String offerType, long discount) {
        this.id = id; this.title = title; this.content = content; this.code = code; this.status = status; this.expiration = expiration; this.type = type; this.isUsed = isUsed; this.quantity = quantity; this.minOrder = minOrder; this.condition = condition; this.offerType = offerType; this.discount = discount;
    }

    protected VoucherItem(Parcel in) {
        id = in.readString();
        title = in.readString();
        content = in.readString();
        code = in.readString();
        status = in.readString();
        expiration = in.readString();
        type = in.readString();
        isUsed = in.readByte() != 0;
        quantity = in.readInt();
        minOrder = in.readLong();
        condition = in.readString();
        offerType = in.readString();
        discount = in.readLong();
    }

    public static final Creator<VoucherItem> CREATOR = new Creator<VoucherItem>() {
        @Override
        public VoucherItem createFromParcel(Parcel in) {
            return new VoucherItem(in);
        }

        @Override
        public VoucherItem[] newArray(int size) {
            return new VoucherItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(code);
        dest.writeString(status);
        dest.writeString(expiration);
        dest.writeString(type);
        dest.writeByte((byte) (isUsed ? 1 : 0));
        dest.writeInt(quantity);
        dest.writeLong(minOrder);
        dest.writeString(condition);
        dest.writeString(offerType);
        dest.writeLong(discount);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return content; }
    public String getCode() { return code; }
    public String getStatus() { return status; }
    public String getHsd() { return expiration; }
    public String getType() { return type; }
    public boolean isFromGift() { return isUsed; }
    public int getQuantity() { return quantity; }
    public int getMinOrderValue() { return (int) minOrder; }
    public String getApplicableProducts() { return condition; }
    public String getOfferType() { return offerType; }
    public int getDiscountValue() { return (int) discount; }
}
