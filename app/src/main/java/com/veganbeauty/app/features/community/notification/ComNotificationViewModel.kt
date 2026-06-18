package com.veganbeauty.app.features.community.notification

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ComNotificationItem(
    val id: String,
    val userId: String?,
    val userName: String,
    val userAvatar: String?,
    val type: String, // "POST" (Bài viết), "INTERACTION" (Tương tác), "ORDER" (Đơn hàng)
    val actionType: String, // "COMMENT", "REPLY", "LIKE", "SHARE", "REPOST", "ORDER_PLACED", "ORDER_COMPLETED", "WITHDRAW"
    val content: String,
    val time: String,
    val date: String, // dd/MM/yyyy
    var isRead: Boolean = false,
    val postId: String? = null,
    val commentId: String? = null,
    var section: String = "" // "Hôm nay", "Hôm qua", "Cũ hơn"
)

sealed class ComNotificationListItem {
    data class Header(val title: String) : ComNotificationListItem()
    data class Notification(val item: ComNotificationItem) : ComNotificationListItem()
}

class ComNotificationViewModel : ViewModel() {

    private val _notifications = MutableLiveData<List<ComNotificationItem>>()
    
    private val _filteredNotifications = MutableLiveData<List<ComNotificationListItem>>()
    val filteredNotifications: LiveData<List<ComNotificationListItem>> get() = _filteredNotifications

    private val _activeTab = MutableLiveData<String>("POST") // "POST", "INTERACTION", "ORDER"
    val activeTab: LiveData<String> get() = _activeTab

    private val _searchQuery = MutableLiveData<String>("")

    fun initData(context: Context) {
        viewModelScope.launch {
            val loadedList = withContext(Dispatchers.IO) {
                var list = loadNotificationsFromLocal(context)
                val needsRegen = list != null && list.any { (it.type == "POST" || it.type == "INTERACTION") && it.postId.isNullOrEmpty() }
                if (list.isNullOrEmpty() || needsRegen) {
                    val jsonReader = LocalJsonReader(context)
                    val users = jsonReader.getUsers()
                    val usersMap = users.associateBy { it.user_id }
                    list = generateNotifications(context, usersMap)
                    saveNotificationsToLocal(context, list)
                }
                list
            }
            _notifications.value = loadedList
            applyFilter()
        }
    }

    fun getSectionName(dateStr: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return "Cũ hơn"
            
            val todayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val itemCal = java.util.Calendar.getInstance().apply {
                time = date
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val todayYear = todayCal.get(java.util.Calendar.YEAR)
            val todayDayOfYear = todayCal.get(java.util.Calendar.DAY_OF_YEAR)
            
            val itemYear = itemCal.get(java.util.Calendar.YEAR)
            val itemDayOfYear = itemCal.get(java.util.Calendar.DAY_OF_YEAR)
            
            if (todayYear == itemYear && todayDayOfYear == itemDayOfYear) {
                "Hôm nay"
            } else {
                val yesterdayCal = (todayCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -1)
                }
                val yesterdayYear = yesterdayCal.get(java.util.Calendar.YEAR)
                val yesterdayDayOfYear = yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR)
                
                if (yesterdayYear == itemYear && yesterdayDayOfYear == itemDayOfYear) {
                    "Hôm qua"
                } else {
                    "Cũ hơn"
                }
            }
        } catch (e: Exception) {
            "Cũ hơn"
        }
    }

    private fun saveNotificationsToLocal(context: Context, items: List<ComNotificationItem>) {
        try {
            val jsonArray = org.json.JSONArray()
            for (item in items) {
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId ?: "")
                    put("userName", item.userName)
                    put("userAvatar", item.userAvatar ?: "")
                    put("type", item.type)
                    put("actionType", item.actionType)
                    put("content", item.content)
                    put("time", item.time)
                    put("date", item.date)
                    put("isRead", item.isRead)
                    put("postId", item.postId ?: "")
                    put("commentId", item.commentId ?: "")
                }
                jsonArray.put(obj)
            }
            val file = java.io.File(context.filesDir, "local_notifications.json")
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadNotificationsFromLocal(context: Context): List<ComNotificationItem>? {
        try {
            val file = java.io.File(context.filesDir, "local_notifications.json")
            if (!file.exists()) return null
            val jsonString = file.readText()
            val jsonArray = org.json.JSONArray(jsonString)
            val list = mutableListOf<ComNotificationItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val uId = obj.optString("userId").takeIf { it.isNotEmpty() && it != "null" }
                val avatar = obj.optString("userAvatar").takeIf { it.isNotEmpty() && it != "null" }
                val pId = obj.optString("postId").takeIf { it.isNotEmpty() && it != "null" }
                val cId = obj.optString("commentId").takeIf { it.isNotEmpty() && it != "null" }
                val itemDate = obj.getString("date")
                list.add(
                    ComNotificationItem(
                        id = obj.getString("id"),
                        userId = uId,
                        userName = obj.getString("userName"),
                        userAvatar = avatar,
                        type = obj.getString("type"),
                        actionType = obj.getString("actionType"),
                        content = obj.getString("content"),
                        time = obj.getString("time"),
                        date = itemDate,
                        isRead = obj.getBoolean("isRead"),
                        postId = pId,
                        commentId = cId,
                        section = getSectionName(itemDate)
                    )
                )
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun generateNotifications(
        context: Context,
        usersMap: Map<String, UserEntity>
    ): List<ComNotificationItem> {
        val list = mutableListOf<ComNotificationItem>()
        try {
            val jsonString = context.assets.open("notification_com.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val userId = obj.optString("userId").takeIf { it.isNotEmpty() && it != "null" }
                val type = obj.getString("type")
                val actionType = obj.getString("actionType")
                val content = obj.getString("content")
                val time = obj.getString("time")
                val date = obj.getString("date")
                val isRead = obj.getBoolean("isRead")
                val id = obj.getString("id")
                val postId = obj.optString("postId").takeIf { it.isNotEmpty() && it != "null" }
                val commentId = obj.optString("commentId").takeIf { it.isNotEmpty() && it != "null" }

                val user = if (userId != null) usersMap[userId] else null
                var finalName = user?.full_name ?: user?.username ?: obj.optString("userName").takeIf { it.isNotEmpty() && it != "null" } ?: "Hệ thống"
                var finalAvatar = user?.avatar ?: obj.optString("userAvatar").takeIf { it.isNotEmpty() && it != "null" } ?: ""

                if (userId == null) {
                    if (actionType == "WITHDRAW") {
                        finalName = "Hệ thống Rootie"
                        finalAvatar = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
                    } else {
                        finalName = "Hệ thống"
                        finalAvatar = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
                    }
                } else if (finalAvatar.isEmpty()) {
                    finalAvatar = when (userId) {
                        "75675216" -> "https://i.pinimg.com/736x/b1/f4/f1/b1f4f17046008cee09ece025370ebae3.jpg"
                        "42788949" -> "https://i.pinimg.com/736x/32/fa/81/32fa81622bd6f125026938d2f6fb39f6.jpg"
                        "85924906" -> "https://i.pinimg.com/736x/19/67/e2/1967e25a3aac9452bace230397d15d1a.jpg"
                        "34260569" -> "https://i.pinimg.com/474x/de/ed/45/deed45e4e0bbaa78991e1779dc87d417.jpg"
                        "49058200" -> "https://i.pinimg.com/736x/12/96/30/1296309708221c882e57eefae42bf46b.jpg"
                        "39692528" -> "https://i.pinimg.com/736x/e1/23/49/e12349977ea0127c7481887fafcb23f5.jpg"
                        "51884402" -> "https://i.pinimg.com/736x/ca/36/57/ca36573c75ab03bb970f9cc9afc3a2ef.jpg"
                        "xuannk_001" -> "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg"
                        "50899342" -> "https://i.pinimg.com/736x/52/f9/a0/52f9a02638cbaa0610a0a7afb61f4db0.jpg"
                        else -> "https://i.pinimg.com/736x/b1/f4/f1/b1f4f17046008cee09ece025370ebae3.jpg"
                    }
                }

                list.add(
                    ComNotificationItem(
                        id = id,
                        userId = userId,
                        userName = finalName,
                        userAvatar = finalAvatar,
                        type = type,
                        actionType = actionType,
                        content = content,
                        time = time,
                        date = date,
                        isRead = isRead,
                        postId = postId,
                        commentId = commentId,
                        section = getSectionName(date)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun selectTab(tab: String) {
        _activeTab.value = tab
        applyFilter()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun markAllRead(context: Context) {
        val current = _notifications.value ?: return
        current.forEach { it.isRead = true }
        _notifications.value = current
        saveNotificationsToLocal(context, current)
        applyFilter()
    }

    fun markAsRead(context: Context, id: String) {
        val current = _notifications.value ?: return
        current.find { it.id == id }?.isRead = true
        _notifications.value = current
        saveNotificationsToLocal(context, current)
        // Note: Do not call applyFilter() here so that the unread dot is not hidden immediately on screen,
        // but it persists the read state so it will show up as read when returning to the notification page.
    }

    fun deleteNotification(context: Context, id: String) {
        val current = _notifications.value ?: return
        val updated = current.filter { it.id != id }
        _notifications.value = updated
        saveNotificationsToLocal(context, updated)
        applyFilter()
    }

    fun deleteAllNotifications(context: Context) {
        val current = _notifications.value ?: return
        val active = _activeTab.value ?: "POST"
        val updated = current.filter { it.type != active }
        _notifications.value = updated
        saveNotificationsToLocal(context, updated)
        applyFilter()
    }

    private fun applyFilter() {
        val allNotis = _notifications.value ?: emptyList()
        val tab = _activeTab.value ?: "POST"
        val query = _searchQuery.value ?: ""

        val tabFiltered = allNotis.filter { it.type == tab }
        val searchFiltered = if (query.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter {
                it.userName.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }

        // Recalculate sections dynamically before grouping
        searchFiltered.forEach {
            it.section = getSectionName(it.date)
        }

        // Group by section and construct flat list with headers
        val flatList = mutableListOf<ComNotificationListItem>()
        val grouped = searchFiltered.groupBy { it.section }

        val sectionsOrder = listOf("Hôm nay", "Hôm qua", "Cũ hơn")
        for (sectionName in sectionsOrder) {
            val items = grouped[sectionName] ?: emptyList()
            if (items.isNotEmpty()) {
                flatList.add(ComNotificationListItem.Header(sectionName))
                flatList.addAll(items.map { ComNotificationListItem.Notification(it) })
            }
        }

        _filteredNotifications.value = flatList
    }
}
