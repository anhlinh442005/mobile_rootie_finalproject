package com.veganbeauty.app.data.repository

import android.content.Context
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NotificationRepository private constructor(private val context: Context) {

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

    // Refresh and populate notifications
    fun refreshNotifications() {
        val assetList = localJsonReader.getAllNotifications()
        val localList = loadNotificationsFromLocal()

        if (localList.isEmpty()) {
            _notifications.value = assetList
            saveNotificationsToLocal(assetList)
            return
        }

        // Merge assets into local list
        val assetMap = assetList.associateBy { it.id }
        val updatedList = mutableListOf<NotificationItem>()

        // 1. Process local items
        for (localItem in localList) {
            val assetItem = assetMap[localItem.id]
            if (assetItem != null) {
                // If it exists in assets, update fields but preserve isRead status
                updatedList.add(assetItem.copy(isRead = localItem.isRead))
            } else {
                // It was added locally (e.g., push notification), keep it
                updatedList.add(localItem)
            }
        }

        // 2. Add new items from assets that are not in the local list
        val localIds = localList.map { it.id }.toSet()
        for (assetItem in assetList) {
            if (assetItem.id !in localIds) {
                updatedList.add(assetItem)
            }
        }

        _notifications.value = updatedList
        saveNotificationsToLocal(updatedList)
    }

    private fun loadNotificationsFromLocal(): List<NotificationItem> {
        val file = File(context.filesDir, "local_account_notifications.json")
        if (!file.exists()) return emptyList()
        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<NotificationItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NotificationItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        time = obj.getString("time"),
                        category = obj.getString("category"),
                        tag = obj.optString("tag", "").takeIf { it.isNotEmpty() },
                        voucherCode = obj.optString("voucherCode", "").takeIf { it.isNotEmpty() },
                        actionText = obj.optString("actionText", "").takeIf { it.isNotEmpty() },
                        isRead = obj.optBoolean("isRead", false),
                        section = obj.optString("section", "Hôm nay"),
                        iconResName = obj.getString("iconResName"),
                        notificationType = obj.optString("notificationType", "").takeIf { it.isNotEmpty() },
                        orderId = obj.optString("orderId", "").takeIf { it.isNotEmpty() },
                        scheduleId = obj.optString("scheduleId", "").takeIf { it.isNotEmpty() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveNotificationsToLocal(list: List<NotificationItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("content", item.content)
                    put("time", item.time)
                    put("category", item.category)
                    put("tag", item.tag ?: "")
                    put("voucherCode", item.voucherCode ?: "")
                    put("actionText", item.actionText ?: "")
                    put("isRead", item.isRead)
                    put("section", item.section)
                    put("iconResName", item.iconResName)
                    put("notificationType", item.notificationType ?: "")
                    put("orderId", item.orderId ?: "")
                    put("scheduleId", item.scheduleId ?: "")
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, "local_account_notifications.json")
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        saveNotificationsToLocal(updatedList)
    }

    // Mark all notifications as read
    fun markAllAsRead() {
        val currentList = _notifications.value
        val updatedList = currentList.map { it.copy(isRead = true) }
        _notifications.value = updatedList
        saveNotificationsToLocal(updatedList)
    }

    // Delete a specific notification by ID
    fun deleteNotification(id: String) {
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.id == id }
        _notifications.value = currentList
        saveNotificationsToLocal(currentList)
    }

    // Delete all notifications
    fun deleteAllNotifications() {
        _notifications.value = emptyList()
        saveNotificationsToLocal(emptyList())
    }

    // Add notification (e.g. from push notifications)
    fun addNotification(item: NotificationItem) {
        val currentList = _notifications.value.toMutableList()
        // Prevent duplicate IDs
        currentList.removeAll { it.id == item.id }
        currentList.add(0, item) // Add at start (newest)
        _notifications.value = currentList
        saveNotificationsToLocal(currentList)
    }

    companion object {
        @Volatile
        private var INSTANCE: NotificationRepository? = null

        @JvmStatic
        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
