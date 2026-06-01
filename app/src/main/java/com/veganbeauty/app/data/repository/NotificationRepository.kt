package com.veganbeauty.app.data.repository

import android.content.Context
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class NotificationRepository private constructor(context: Context) {

    private val localJsonReader = LocalJsonReader(context.applicationContext)
    
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val allNotifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Reactively compute unread count
    val unreadCount = _notifications.map { list ->
        list.count { !it.isRead }
    }

    init {
        refreshNotifications()
    }

    // Refresh and populate notifications from JSON
    fun refreshNotifications() {
        val mockList = localJsonReader.getAllNotifications()
        _notifications.value = mockList
    }

    // Thread-safe update of isRead status
    fun markAsRead(id: String) {
        val currentList = _notifications.value
        val updatedList = currentList.map { item ->
            if (item.id == id) {
                item.copy(isRead = true)
            } else {
                item
            }
        }
        _notifications.value = updatedList
    }

    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
