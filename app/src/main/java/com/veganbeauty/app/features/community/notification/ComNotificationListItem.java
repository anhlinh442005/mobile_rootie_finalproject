package com.veganbeauty.app.features.community.notification;

public abstract class ComNotificationListItem {

    public static final class Header extends ComNotificationListItem {
        private final String title;

        public Header(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    public static final class Notification extends ComNotificationListItem {
        private final ComNotificationItem item;

        public Notification(ComNotificationItem item) {
            this.item = item;
        }

        public ComNotificationItem getItem() {
            return item;
        }
    }
}
