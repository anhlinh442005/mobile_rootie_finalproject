package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

public class BookingHistoryEntity {
    @NonNull private String id;
    @NonNull private String userId;
    @NonNull private String userName;
    @NonNull private String userPhone;
    @NonNull private String userEmail;
    @NonNull private String serviceName;
    @NonNull private String dateDisplay;
    @NonNull private String monthDisplay;
    @NonNull private String dayOfWeek;
    @NonNull private String time;
    @NonNull private String duration;
    @NonNull private String storeName;
    @NonNull private String storeAddress;
    @NonNull private String storePhone;
    @NonNull private String storeImage;
    @NonNull private String note;
    @NonNull private String status;
    @NonNull private String policy;
    @NonNull private String createdAt;
    @NonNull private String completedAt;
    @NonNull private List<String> skinResults;
    @NonNull private String consultantName;
    @NonNull private String consultantAvatar;
    private float consultantRating;
    private float userRating;
    @NonNull private String userReview;
    @NonNull private String reviewDate;
    @NonNull private String beforeImage;
    @NonNull private String afterImage;
    private int earnedPoints;
    private int totalPoints;
    @NonNull private String nextAppointmentDate;
    @NonNull private String nextAppointmentText;
    @NonNull private String cancelledAt;
    @NonNull private String cancelReason;
    @NonNull private List<String> userFeedbackImages;

    public BookingHistoryEntity(@NonNull String id, @NonNull String userId, @NonNull String userName, @NonNull String userPhone, @NonNull String userEmail, @NonNull String serviceName, @NonNull String dateDisplay, @NonNull String dayOfWeek, @NonNull String time, @NonNull String duration, @NonNull String storeName, @NonNull String storeAddress, @NonNull String status) {
        this(id, userId, userName, userPhone, userEmail, serviceName, dateDisplay, "", dayOfWeek, time, duration, storeName, storeAddress, "", "", "", status, "", "", "", Collections.emptyList(), "", "", 0f, 0f, "", "", "", "", 0, 0, "", "", "", "", Collections.emptyList());
    }

    public BookingHistoryEntity(@NonNull String id, @NonNull String userId, @NonNull String userName, @NonNull String userPhone, @NonNull String userEmail, @NonNull String serviceName, @NonNull String dateDisplay, @NonNull String monthDisplay, @NonNull String dayOfWeek, @NonNull String time, @NonNull String duration, @NonNull String storeName, @NonNull String storeAddress, @NonNull String storePhone, @NonNull String storeImage, @NonNull String note, @NonNull String status, @NonNull String policy, @NonNull String createdAt, @NonNull String completedAt, @NonNull List<String> skinResults, @NonNull String consultantName, @NonNull String consultantAvatar, float consultantRating, float userRating, @NonNull String userReview, @NonNull String reviewDate, @NonNull String beforeImage, @NonNull String afterImage, int earnedPoints, int totalPoints, @NonNull String nextAppointmentDate, @NonNull String nextAppointmentText, @NonNull String cancelledAt, @NonNull String cancelReason, @NonNull List<String> userFeedbackImages) {
        this.id = id; this.userId = userId; this.userName = userName; this.userPhone = userPhone; this.userEmail = userEmail; this.serviceName = serviceName; this.dateDisplay = dateDisplay; this.monthDisplay = monthDisplay; this.dayOfWeek = dayOfWeek; this.time = time; this.duration = duration; this.storeName = storeName; this.storeAddress = storeAddress; this.storePhone = storePhone; this.storeImage = storeImage; this.note = note; this.status = status; this.policy = policy; this.createdAt = createdAt; this.completedAt = completedAt; this.skinResults = skinResults != null ? skinResults : Collections.emptyList(); this.consultantName = consultantName; this.consultantAvatar = consultantAvatar; this.consultantRating = consultantRating; this.userRating = userRating; this.userReview = userReview; this.reviewDate = reviewDate; this.beforeImage = beforeImage; this.afterImage = afterImage; this.earnedPoints = earnedPoints; this.totalPoints = totalPoints; this.nextAppointmentDate = nextAppointmentDate; this.nextAppointmentText = nextAppointmentText; this.cancelledAt = cancelledAt; this.cancelReason = cancelReason; this.userFeedbackImages = userFeedbackImages != null ? userFeedbackImages : Collections.emptyList();
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getUserId() { return userId; }
    @NonNull public String getUserName() { return userName; }
    @NonNull public String getUserPhone() { return userPhone; }
    @NonNull public String getUserEmail() { return userEmail; }
    @NonNull public String getServiceName() { return serviceName; }
    @NonNull public String getDateDisplay() { return dateDisplay; }
    @NonNull public String getMonthDisplay() { return monthDisplay; }
    @NonNull public String getDayOfWeek() { return dayOfWeek; }
    @NonNull public String getTime() { return time; }
    @NonNull public String getDuration() { return duration; }
    @NonNull public String getStoreName() { return storeName; }
    @NonNull public String getStoreAddress() { return storeAddress; }
    @NonNull public String getStorePhone() { return storePhone; }
    @NonNull public String getStoreImage() { return storeImage; }
    @NonNull public String getNote() { return note; }
    @NonNull public String getStatus() { return status; }
    @NonNull public String getPolicy() { return policy; }
    @NonNull public String getCreatedAt() { return createdAt; }
    @NonNull public String getCompletedAt() { return completedAt; }
    @NonNull public List<String> getSkinResults() { return skinResults; }
    @NonNull public String getConsultantName() { return consultantName; }
    @NonNull public String getConsultantAvatar() { return consultantAvatar; }
    public float getConsultantRating() { return consultantRating; }
    public float getUserRating() { return userRating; }
    @NonNull public String getUserReview() { return userReview; }
    @NonNull public String getReviewDate() { return reviewDate; }
    @NonNull public String getBeforeImage() { return beforeImage; }
    @NonNull public String getAfterImage() { return afterImage; }
    public int getEarnedPoints() { return earnedPoints; }
    public int getTotalPoints() { return totalPoints; }
    @NonNull public String getNextAppointmentDate() { return nextAppointmentDate; }
    @NonNull public String getNextAppointmentText() { return nextAppointmentText; }
    @NonNull public String getCancelledAt() { return cancelledAt; }
    @NonNull public String getCancelReason() { return cancelReason; }
    @NonNull public List<String> getUserFeedbackImages() { return userFeedbackImages; }

    public void setStatus(@NonNull String status) { this.status = status; }
    public void setMonthDisplay(@NonNull String monthDisplay) { this.monthDisplay = monthDisplay; }
    public void setStorePhone(@NonNull String storePhone) { this.storePhone = storePhone; }
    public void setStoreImage(@NonNull String storeImage) { this.storeImage = storeImage; }
    public void setCreatedAt(@NonNull String createdAt) { this.createdAt = createdAt; }
    public void setConsultantName(@NonNull String consultantName) { this.consultantName = consultantName; }
    public void setConsultantAvatar(@NonNull String consultantAvatar) { this.consultantAvatar = consultantAvatar; }
    public void setConsultantRating(float consultantRating) { this.consultantRating = consultantRating; }
    public void setCancelReason(@NonNull String cancelReason) { this.cancelReason = cancelReason; }
    public void setCancelledAt(@NonNull String cancelledAt) { this.cancelledAt = cancelledAt; }
    @NonNull public List<String> getFeedbackTags() { return Collections.emptyList(); }
    @NonNull public String getStoreId() { return ""; }
    @NonNull public String getServiceId() { return ""; }
    @NonNull public String getServiceImage() { return ""; }
    @NonNull public String getCheckInCode() { return ""; }
    public float getFeedbackRating() { return 0f; }
    @NonNull public String getFeedbackComment() { return ""; }
}
