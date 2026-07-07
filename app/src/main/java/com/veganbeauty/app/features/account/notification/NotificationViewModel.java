package com.veganbeauty.app.features.account.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.FlowLiveDataConversions;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationViewModel extends RootieViewModel {

    private final NotificationRepository repository;

    private final MutableLiveData<String> _selectedTab = new MutableLiveData<>("Tất cả");
    public final LiveData<String> selectedTab = _selectedTab;

    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    public final LiveData<String> searchQuery = _searchQuery;

    private final MediatorLiveData<List<NotificationListItem>> _notificationItems = new MediatorLiveData<>();
    public final LiveData<List<NotificationListItem>> notificationItems = _notificationItems;

    private List<NotificationItem> currentAllNotifications = new ArrayList<>();

    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;

        LiveData<List<NotificationItem>> allNotifications = FlowLiveDataConversions.asLiveData(repository.notifications);
        _notificationItems.addSource(allNotifications, value -> {
            if (value != null) {
                currentAllNotifications = value;
                updateItems(value, _selectedTab.getValue(), _searchQuery.getValue());
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
        } else if ("Khác".equalsIgnoreCase(tab)) {
            for (NotificationItem item : allNotifications) {
                String cat = item.getCategory() != null ? item.getCategory().trim() : "";
                if (!"Khuyến mãi".equalsIgnoreCase(cat) && !"Đơn hàng".equalsIgnoreCase(cat)) {
                    filteredByCategory.add(item);
                }
            }
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

        filteredByQuery.sort((a, b) -> Long.compare(
                NotificationDateHelper.getTimeMillis(b.getTime()),
                NotificationDateHelper.getTimeMillis(a.getTime())
        ));

        List<NotificationListItem> flatList = new ArrayList<>();
        Map<String, List<NotificationItem>> grouped = new LinkedHashMap<>();

        for (NotificationItem item : filteredByQuery) {
            String section = NotificationDateHelper.getSectionFromTime(item.getTime());
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
        repository.markAsRead(id);
    }

    public void markAllAsRead() {
        repository.markAllAsRead();
    }

    public void deleteNotification(String id) {
        repository.deleteNotification(id);
    }

    public void deleteAllNotifications() {
        repository.deleteAllNotifications();
    }
}
