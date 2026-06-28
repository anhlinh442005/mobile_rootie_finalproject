package com.veganbeauty.app.features.account.notification;

import com.veganbeauty.app.data.local.entities.NotificationItem;

import java.util.Objects;

public abstract class NotificationListItem {
    public static class Header extends NotificationListItem {
        private final String title;
        public Header(String title) { this.title = title; }
        public String getTitle() { return title; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Header header = (Header) o;
            return Objects.equals(title, header.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title);
        }
    }

    public static class Notification extends NotificationListItem {
        private final NotificationItem item;
        public Notification(NotificationItem item) { this.item = item; }
        public NotificationItem getItem() { return item; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Notification that = (Notification) o;
            return Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }
}
