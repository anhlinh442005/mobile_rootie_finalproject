package com.veganbeauty.app.features.account.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.entities.NotificationItem
import com.veganbeauty.app.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed class NotificationListItem {
    data class Header(val title: String) : NotificationListItem()
    data class Notification(val item: NotificationItem) : NotificationListItem()
}

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow("Tất cả")
    val selectedTab = _selectedTab.asLiveData(viewModelScope.coroutineContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    val notificationItems = combine(
        repository.allNotifications,
        _selectedTab,
        _searchQuery
    ) { allNotifications, tab, query ->
        // First filter by selected tab
        val filteredByCategory = if (tab.equals("Tất cả", ignoreCase = true)) {
            allNotifications
        } else {
            allNotifications.filter { it.category.equals(tab, ignoreCase = true) }
        }

        // Then filter by search query if non-empty
        val filteredByQuery = if (query.isBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }

        // Create the flat list representation
        val flatList = mutableListOf<NotificationListItem>()

        // Group by section ("Hôm nay", "Hôm qua", etc.) for all category tabs
        val grouped = filteredByQuery.groupBy { it.section }
        
        // Order the groups: "Hôm nay" first, "Hôm qua" second, then others
        val orderedSections = listOf("Hôm nay", "Hôm qua")
        
        for (sectionName in orderedSections) {
            grouped[sectionName]?.let { items ->
                if (items.isNotEmpty()) {
                    flatList.add(NotificationListItem.Header(sectionName.uppercase()))
                    flatList.addAll(items.map { NotificationListItem.Notification(it) })
                }
            }
        }
        
        // Any other sections not covered
        grouped.keys.filter { it !in orderedSections }.forEach { sectionName ->
            grouped[sectionName]?.let { items ->
                if (items.isNotEmpty()) {
                    flatList.add(NotificationListItem.Header(sectionName.uppercase()))
                    flatList.addAll(items.map { NotificationListItem.Notification(it) })
                }
            }
        }

        flatList
    }.asLiveData(viewModelScope.coroutineContext)

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAllNotifications()
        }
    }
}
