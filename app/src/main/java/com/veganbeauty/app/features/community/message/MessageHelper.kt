package com.veganbeauty.app.features.community.message

import android.content.Context
import com.veganbeauty.app.data.local.entities.ChatMessageEntity
import com.veganbeauty.app.data.local.entities.ConversationEntity
import com.veganbeauty.app.data.local.entities.LastMessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object MessageHelper {
    private const val CONVERSATIONS_FILE = "conversations_local_v2.json"
    private const val MESSAGES_FILE = "messages_local_v2.json"

    fun initDataIfNeed(context: Context) {
        val convFile = File(context.filesDir, CONVERSATIONS_FILE)
        val msgFile = File(context.filesDir, MESSAGES_FILE)
        
        var needsInit = false
        if (!convFile.exists() || !msgFile.exists()) {
            needsInit = true
        } else {
            try {
                if (JSONArray(convFile.readText()).length() <= 1) {
                    needsInit = true
                }
            } catch (e: Exception) {
                needsInit = true
            }
        }
        
        if (needsInit) {
            try {
                val jsonString = com.veganbeauty.app.data.local.LocalJsonReader(context).getMessagesData()
                val oldArray = JSONArray(jsonString)
                
                val newConvArray = JSONArray()
                val newMsgArray = JSONArray()
                
                for (i in 0 until oldArray.length()) {
                    val oldObj = oldArray.getJSONObject(i)
                    val convId = oldObj.optString("id")
                    val currentUserId = oldObj.optString("user_id")
                    val partnerId = oldObj.optString("partner_id")
                    
                    val isUnread = oldObj.optBoolean("is_unread")
                    val messages = oldObj.optJSONArray("messages") ?: JSONArray()
                    
                    var lastMessageEntity: JSONObject? = null
                    val timestamp = System.currentTimeMillis() - i * 100000 // Mock sorted timestamps
                    
                    for (j in 0 until messages.length()) {
                        val oldMsg = messages.getJSONObject(j)
                        val msgId = oldMsg.optString("id")
                        val senderId = oldMsg.optString("sender_id")
                        val isMine = oldMsg.optBoolean("is_mine")
                        val receiverId = if (isMine) partnerId else currentUserId
                        val text = oldMsg.optString("text")
                        
                        val newMsg = JSONObject().apply {
                            put("message_id", msgId)
                            put("conversation_id", convId)
                            put("sender_id", senderId)
                            put("receiver_id", receiverId)
                            put("text", text)
                            put("type", "text")
                            put("created_at", timestamp + j * 1000)
                            put("status", JSONObject().apply {
                                put(currentUserId, if (isUnread && j == messages.length() - 1 && !isMine) "unread" else "read")
                                put(partnerId, "read")
                            })
                        }
                        newMsgArray.put(newMsg)
                        
                        if (j == messages.length() - 1) {
                            lastMessageEntity = JSONObject().apply {
                                put("message_id", msgId)
                                put("sender_id", senderId)
                                put("text", text)
                                put("timestamp", timestamp + j * 1000)
                            }
                        }
                    }
                    
                    val newConv = JSONObject().apply {
                        put("conversationId", convId)
                        put("participants", JSONArray(listOf(currentUserId, partnerId)))
                        put("partner_id", partnerId)
                        put("partner_name", oldObj.optString("partner_name"))
                        put("partner_avatar", oldObj.optString("partner_avatar"))
                        put("is_active", oldObj.optBoolean("is_active"))
                        put("is_typing", oldObj.optBoolean("is_typing"))
                        put("last_message", lastMessageEntity)
                        put("unread_count", JSONObject().apply {
                            put(currentUserId, if (isUnread) 1 else 0)
                            put(partnerId, 0)
                        })
                        put("updated_at", timestamp + messages.length() * 1000)
                    }
                    newConvArray.put(newConv)
                }
                
                convFile.writeText(newConvArray.toString())
                msgFile.writeText(newMsgArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun readConversationsArray(context: Context): JSONArray {
        initDataIfNeed(context)
        val file = File(context.filesDir, CONVERSATIONS_FILE)
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun writeConversationsArray(context: Context, array: JSONArray) {
        File(context.filesDir, CONVERSATIONS_FILE).writeText(array.toString())
    }

    private fun readMessagesArray(context: Context): JSONArray {
        initDataIfNeed(context)
        val file = File(context.filesDir, MESSAGES_FILE)
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun writeMessagesArray(context: Context, array: JSONArray) {
        File(context.filesDir, MESSAGES_FILE).writeText(array.toString())
    }

    fun getConversations(context: Context): List<ConversationEntity> {
        val array = readConversationsArray(context)
        
        // Merge latest user avatars and names from users.json
        val usersJsonString = com.veganbeauty.app.data.local.LocalJsonReader(context).getRawUsersJson()
        val usersMap = mutableMapOf<String, Pair<String, String>>()
        try {
            val usersArray = JSONArray(usersJsonString)
            for (i in 0 until usersArray.length()) {
                val u = usersArray.getJSONObject(i)
                usersMap[u.getString("user_id")] = Pair(u.optString("username"), u.optString("avatar"))
            }
        } catch (e: Exception) {}

        val list = mutableListOf<ConversationEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            val participantsArr = obj.optJSONArray("participants") ?: JSONArray()
            val participants = mutableListOf<String>()
            for (p in 0 until participantsArr.length()) {
                participants.add(participantsArr.getString(p))
            }
            
            val lastMsgObj = obj.optJSONObject("last_message")
            val lastMessage = lastMsgObj?.let {
                LastMessageEntity(
                    it.optString("message_id"),
                    it.optString("sender_id"),
                    it.optString("text"),
                    it.optLong("timestamp")
                )
            }
            
            val unreadObj = obj.optJSONObject("unread_count") ?: JSONObject()
            val unreadCount = mutableMapOf<String, Int>()
            val keys = unreadObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                unreadCount[key] = unreadObj.optInt(key, 0)
            }

            val partnerId = obj.optString("partner_id")
            val cachedName = obj.optString("partner_name")
            val cachedAvatar = obj.optString("partner_avatar")
            
            val freshestName = usersMap[partnerId]?.first?.takeIf { it.isNotEmpty() } ?: cachedName
            val freshestAvatar = usersMap[partnerId]?.second?.takeIf { it.isNotEmpty() } ?: cachedAvatar

            list.add(ConversationEntity(
                conversationId = obj.optString("conversationId"),
                participants = participants,
                partnerId = partnerId,
                partnerName = freshestName,
                partnerAvatar = freshestAvatar,
                isActive = obj.optBoolean("is_active"),
                isTyping = obj.optBoolean("is_typing"),
                lastMessage = lastMessage,
                unreadCount = unreadCount,
                updatedAt = obj.optLong("updated_at")
            ))
        }
        return list.sortedByDescending { it.updatedAt }
    }

    fun getMessages(context: Context, conversationId: String): List<ChatMessageEntity> {
        val array = readMessagesArray(context)
        val list = mutableListOf<ChatMessageEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.optString("conversation_id") == conversationId) {
                val statusObj = obj.optJSONObject("status") ?: JSONObject()
                val statusMap = mutableMapOf<String, String>()
                val keys = statusObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    statusMap[key] = statusObj.optString(key, "unread")
                }
                
                list.add(ChatMessageEntity(
                    messageId = obj.optString("message_id"),
                    conversationId = conversationId,
                    senderId = obj.optString("sender_id"),
                    receiverId = obj.optString("receiver_id"),
                    text = obj.optString("text"),
                    type = obj.optString("type", "text"),
                    createdAt = obj.optLong("created_at"),
                    status = statusMap,
                    readAt = emptyMap()
                ))
            }
        }
        return list.sortedBy { it.createdAt }
    }

    fun markAsRead(context: Context, conversationId: String, userId: String) {
        val convArray = readConversationsArray(context)
        for (i in 0 until convArray.length()) {
            val obj = convArray.getJSONObject(i)
            if (obj.optString("conversationId") == conversationId) {
                val unreadObj = obj.optJSONObject("unread_count") ?: JSONObject()
                unreadObj.put(userId, 0)
                obj.put("unread_count", unreadObj)
                break
            }
        }
        writeConversationsArray(context, convArray)

        val msgArray = readMessagesArray(context)
        var changed = false
        for (i in 0 until msgArray.length()) {
            val obj = msgArray.getJSONObject(i)
            if (obj.optString("conversation_id") == conversationId) {
                val statusObj = obj.optJSONObject("status") ?: JSONObject()
                if (statusObj.optString(userId) != "read") {
                    statusObj.put(userId, "read")
                    obj.put("status", statusObj)
                    changed = true
                }
            }
        }
        if (changed) writeMessagesArray(context, msgArray)
    }

    fun sendMessage(context: Context, conversationId: String, senderId: String, receiverId: String, text: String) {
        val msgId = "m_" + UUID.randomUUID().toString().take(8)
        val timestamp = System.currentTimeMillis()

        val msgArray = readMessagesArray(context)
        val newMsg = JSONObject().apply {
            put("message_id", msgId)
            put("conversation_id", conversationId)
            put("sender_id", senderId)
            put("receiver_id", receiverId)
            put("text", text)
            put("type", "text")
            put("created_at", timestamp)
            put("status", JSONObject().apply {
                put(senderId, "read")
                put(receiverId, "unread")
            })
        }
        msgArray.put(newMsg)
        writeMessagesArray(context, msgArray)

        val convArray = readConversationsArray(context)
        for (i in 0 until convArray.length()) {
            val obj = convArray.getJSONObject(i)
            if (obj.optString("conversationId") == conversationId) {
                obj.put("last_message", JSONObject().apply {
                    put("message_id", msgId)
                    put("sender_id", senderId)
                    put("text", text)
                    put("timestamp", timestamp)
                })
                obj.put("updated_at", timestamp)

                val unreadObj = obj.optJSONObject("unread_count") ?: JSONObject()
                val currentUnread = unreadObj.optInt(receiverId, 0)
                unreadObj.put(receiverId, currentUnread + 1)
                obj.put("unread_count", unreadObj)
                break
            }
        }
        writeConversationsArray(context, convArray)

        // Push to Firebase Realtime Database
        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            val msgMap = mapOf(
                "message_id" to msgId,
                "conversation_id" to conversationId,
                "sender_id" to senderId,
                "receiver_id" to receiverId,
                "text" to text,
                "type" to "text",
                "created_at" to timestamp,
                "status" to mapOf(
                    senderId to "read",
                    receiverId to "unread"
                )
            )
            database.child("conversations").child(conversationId).child("messages").child(msgId).setValue(msgMap)
            
            val lastMsgMap = mapOf(
                "message_id" to msgId,
                "sender_id" to senderId,
                "text" to text,
                "timestamp" to timestamp
            )
            database.child("conversations").child(conversationId).child("last_message").setValue(lastMsgMap)
            database.child("conversations").child(conversationId).child("updated_at").setValue(timestamp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getConversation(context: Context, conversationId: String): ConversationEntity? {
        return getConversations(context).find { it.conversationId == conversationId }
    }

    fun getOrCreateConversation(context: Context, currentUserId: String, partnerId: String, partnerName: String, partnerAvatar: String): String {
        val convArray = readConversationsArray(context)
        for (i in 0 until convArray.length()) {
            val obj = convArray.getJSONObject(i)
            val participants = obj.optJSONArray("participants")
            if (participants != null && participants.length() == 2) {
                val p1 = participants.getString(0)
                val p2 = participants.getString(1)
                if ((p1 == currentUserId && p2 == partnerId) || (p1 == partnerId && p2 == currentUserId)) {
                    return obj.optString("conversationId")
                }
            }
        }
        
        // Not found, create new
        val newConvId = "c_" + UUID.randomUUID().toString().take(8)
        val timestamp = System.currentTimeMillis()
        val newConv = JSONObject().apply {
            put("conversationId", newConvId)
            put("participants", JSONArray(listOf(currentUserId, partnerId)))
            put("partner_id", partnerId)
            put("partner_name", partnerName)
            put("partner_avatar", partnerAvatar)
            put("is_active", true)
            put("is_typing", false)
            put("last_message", JSONObject.NULL)
            put("unread_count", JSONObject().apply {
                put(currentUserId, 0)
                put(partnerId, 0)
            })
            put("updated_at", timestamp)
        }
        convArray.put(newConv)
        writeConversationsArray(context, convArray)
        return newConvId
    }

    fun updateMessage(context: Context, conversationId: String, messageId: String, newText: String) {
        val msgArray = readMessagesArray(context)
        for (i in 0 until msgArray.length()) {
            val obj = msgArray.getJSONObject(i)
            if (obj.optString("message_id") == messageId) {
                obj.put("text", newText)
                break
            }
        }
        writeMessagesArray(context, msgArray)

        // Also update last_message if it's the last message
        val convArray = readConversationsArray(context)
        for (i in 0 until convArray.length()) {
            val obj = convArray.getJSONObject(i)
            if (obj.optString("conversationId") == conversationId) {
                val lastMsg = obj.optJSONObject("last_message")
                if (lastMsg != null && lastMsg.optString("message_id") == messageId) {
                    lastMsg.put("text", newText)
                    obj.put("last_message", lastMsg)
                }
                break
            }
        }
        writeConversationsArray(context, convArray)

        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("conversations").child(conversationId).child("messages").child(messageId).child("text").setValue(newText)
            
            // Check if last message
            database.child("conversations").child(conversationId).child("last_message").get().addOnSuccessListener { snapshot ->
                val lastId = snapshot.child("message_id").getValue(String::class.java)
                if (lastId == messageId) {
                    database.child("conversations").child(conversationId).child("last_message").child("text").setValue(newText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteMessage(context: Context, conversationId: String, messageId: String) {
        val msgArray = readMessagesArray(context)
        val newMsgArray = JSONArray()
        for (i in 0 until msgArray.length()) {
            val obj = msgArray.getJSONObject(i)
            if (obj.optString("message_id") != messageId) {
                newMsgArray.put(obj)
            }
        }
        writeMessagesArray(context, newMsgArray)

        try {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            database.child("conversations").child(conversationId).child("messages").child(messageId).removeValue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
