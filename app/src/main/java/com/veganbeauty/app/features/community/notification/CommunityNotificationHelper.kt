package com.veganbeauty.app.features.community.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunityNotificationHelper {

    @JvmStatic
    fun addCommunityNotification(
        context: Context,
        id: String,
        userId: String?,
        userName: String,
        userAvatar: String?,
        type: String, // "POST", "INTERACTION", "ORDER"
        actionType: String,
        content: String,
        postId: String? = null,
        commentId: String? = null
    ) {
        val file = File(context.filesDir, "local_notifications.json")
        val list = mutableListOf<ComNotificationItem>()
        
        // Load existing notifications
        if (file.exists()) {
            try {
                val jsonArray = JSONArray(file.readText())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ComNotificationItem(
                            id = obj.getString("id"),
                            userId = obj.optString("userId").takeIf { it.isNotEmpty() && it != "null" },
                            userName = obj.getString("userName"),
                            userAvatar = obj.optString("userAvatar").takeIf { it.isNotEmpty() && it != "null" },
                            type = obj.getString("type"),
                            actionType = obj.getString("actionType"),
                            content = obj.getString("content"),
                            time = obj.getString("time"),
                            date = obj.getString("date"),
                            isRead = obj.optBoolean("isRead", false),
                            postId = obj.optString("postId").takeIf { it.isNotEmpty() && it != "null" },
                            commentId = obj.optString("commentId").takeIf { it.isNotEmpty() && it != "null" }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Load initial from assets if local doesn't exist yet
            try {
                val jsonString = context.assets.open("notification_com.json").bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ComNotificationItem(
                            id = obj.getString("id"),
                            userId = obj.optString("userId").takeIf { it.isNotEmpty() && it != "null" },
                            userName = obj.optString("userName", "Hệ thống"),
                            userAvatar = obj.optString("userAvatar", ""),
                            type = obj.getString("type"),
                            actionType = obj.getString("actionType"),
                            content = obj.getString("content"),
                            time = obj.getString("time"),
                            date = obj.getString("date"),
                            isRead = obj.optBoolean("isRead", false),
                            postId = obj.optString("postId").takeIf { it.isNotEmpty() && it != "null" },
                            commentId = obj.optString("commentId").takeIf { it.isNotEmpty() && it != "null" }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Create new item
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val newItem = ComNotificationItem(
            id = id,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar ?: "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png",
            type = type,
            actionType = actionType,
            content = content,
            time = currentTime,
            date = currentDate,
            isRead = false,
            postId = postId,
            commentId = commentId
        )

        // Prevent duplicate
        list.removeAll { it.id == id }
        list.add(0, newItem)

        // Save back
        try {
            val newArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
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
                newArray.put(obj)
            }
            file.writeText(newArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
