package com.veganbeauty.app.features.shop.product.detail;

public class ProductReview {
    private final String reviewerName;
    private final int rating;
    private final String comment;

    public ProductReview(String reviewerName, int rating, String comment) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.comment = comment;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
