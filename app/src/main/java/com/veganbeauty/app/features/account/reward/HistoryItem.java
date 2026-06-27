package com.veganbeauty.app.features.account.reward;

import com.veganbeauty.app.data.local.entities.RewardPointEntity;

public abstract class HistoryItem {
    public static final class Header extends HistoryItem {
        private final String title;
        public Header(String title) { this.title = title; }
        public String getTitle() { return title; }
    }
    public static final class Transaction extends HistoryItem {
        private final RewardPointEntity data;
        public Transaction(RewardPointEntity data) { this.data = data; }
        public RewardPointEntity getData() { return data; }
    }
}
