package com.veganbeauty.app.features.community.notification;

public class ComNotificationItem {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String type;       // "POST", "INTERACTION", "ORDER"
    private String actionType; // "COMMENT", "REPLY", "LIKE", "SHARE", "REPOST", "ORDER_PLACED", "ORDER_COMPLETED", "WITHDRAW"
    private String content;
    private String time;
    private String date;       // dd/MM/yyyy
    private boolean isRead;
    private String postId;
    private String commentId;
    private String section;    // "Hôm nay", "Hôm qua", "Cũ hơn"

    public ComNotificationItem(String id, String userId, String userName, String userAvatar,
                                String type, String actionType, String content, String time,
                                String date, boolean isRead, String postId, String commentId, String section) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.type = type;
        this.actionType = actionType;
        this.content = content;
        this.time = time;
        this.date = date;
        this.isRead = isRead;
        this.postId = postId;
        this.commentId = commentId;
        this.section = section;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getType() { return type; }
    public String getActionType() { return actionType; }
    public String getContent() { return content; }
    public String getTime() { return time; }
    public String getDate() { return date; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { this.isRead = read; }
    public String getPostId() { return postId; }
    public String getCommentId() { return commentId; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
