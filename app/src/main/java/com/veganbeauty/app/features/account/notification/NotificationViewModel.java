package com.veganbeauty.app.features.account.notification;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class NotificationViewModel extends RootieViewModel {

    private final NotificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> _selectedTab = new MutableLiveData<>("Tất cả");
    public final LiveData<String> selectedTab = _selectedTab;

    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    public final LiveData<String> searchQuery = _searchQuery;

    private final MutableLiveData<List<NotificationListItem>> _notificationItems = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<NotificationListItem>> notificationItems = _notificationItems;

    private List<NotificationItem> currentAllNotifications = new ArrayList<>();

    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;

        Flow<List<NotificationItem>> allNotifications = repository.getAllNotifications();
        
        executor.execute(() -> {
            try {
                allNotifications.collect(new FlowCollector<List<NotificationItem>>() {
                    @Override
                    public Object emit(List<NotificationItem> value, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        currentAllNotifications = value;
                        updateItems(value, _selectedTab.getValue(), _searchQuery.getValue());
                        return kotlin.Unit.INSTANCE;
                    }
                }, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateItems(List<NotificationItem> allNotifications, String tab, String query) {
        if (allNotifications == null) allNotifications = new ArrayList<>();
        if (tab == null) tab = "Tất cả";
        if (query == null) query = "";

        List<NotificationItem> filteredByCategory = new ArrayList<>();
        if ("Tất cả".equalsIgnoreCase(tab)) {
            filteredByCategory.addAll(allNotifications);
        } else {
            for (NotificationItem item : allNotifications) {
                if (tab.equalsIgnoreCase(item.getCategory())) {
                    filteredByCategory.add(item);
                }
            }
        }

        List<NotificationItem> filteredByQuery = new ArrayList<>();
        if (query.trim().isEmpty()) {
            filteredByQuery.addAll(filteredByCategory);
        } else {
            for (NotificationItem item : filteredByCategory) {
                if (item.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    item.getContent().toLowerCase().contains(query.toLowerCase())) {
                    filteredByQuery.add(item);
                }
            }
        }

        List<NotificationListItem> flatList = new ArrayList<>();
        Map<String, List<NotificationItem>> grouped = new LinkedHashMap<>();
        
        for (NotificationItem item : filteredByQuery) {
            String section = item.getSection();
            if (!grouped.containsKey(section)) {
                grouped.put(section, new ArrayList<>());
            }
            grouped.get(section).add(item);
        }

        String[] orderedSections = {"Hôm nay", "Hôm qua"};

        for (String sectionName : orderedSections) {
            if (grouped.containsKey(sectionName) && !grouped.get(sectionName).isEmpty()) {
                flatList.add(new NotificationListItem.Header(sectionName.toUpperCase()));
                for (NotificationItem item : grouped.get(sectionName)) {
                    flatList.add(new NotificationListItem.Notification(item));
                }
            }
        }

        for (Map.Entry<String, List<NotificationItem>> entry : grouped.entrySet()) {
            String sectionName = entry.getKey();
            if (!sectionName.equals("Hôm nay") && !sectionName.equals("Hôm qua")) {
                if (!entry.getValue().isEmpty()) {
                    flatList.add(new NotificationListItem.Header(sectionName.toUpperCase()));
                    for (NotificationItem item : entry.getValue()) {
                        flatList.add(new NotificationListItem.Notification(item));
                    }
                }
            }
        }

        _notificationItems.postValue(flatList);
    }

    public void selectTab(String tab) {
        _selectedTab.setValue(tab);
        updateItems(currentAllNotifications, tab, _searchQuery.getValue());
    }

    public void setSearchQuery(String query) {
        _searchQuery.setValue(query);
        updateItems(currentAllNotifications, _selectedTab.getValue(), query);
    }

    public void markAsRead(String id) {
        executor.execute(() -> {
            try {
                repository.markAsRead(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void markAllAsRead() {
        executor.execute(() -> {
            try {
                repository.markAllAsRead();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void deleteNotification(String id) {
        executor.execute(() -> {
            try {
                repository.deleteNotification(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void deleteAllNotifications() {
        executor.execute(() -> {
            try {
                repository.deleteAllNotifications();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static abstract class NotificationListItem {
        public static class Header extends NotificationListItem {
            private final String title;
            public Header(String title) { this.title = title; }
            public String getTitle() { return title; }
        }
        public static class Notification extends NotificationListItem {
            private final NotificationItem item;
            public Notification(NotificationItem item) { this.item = item; }
            public NotificationItem getItem() { return item; }
        }
    }
}
