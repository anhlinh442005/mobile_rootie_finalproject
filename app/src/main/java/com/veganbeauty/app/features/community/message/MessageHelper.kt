package com.veganbeauty.app.features.community.message

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.veganbeauty.app.data.local.entities.ChatMessageEntity
import com.veganbeauty.app.data.local.entities.ConversationEntity
import com.veganbeauty.app.data.local.entities.MemberInfoEntity
import java.io.File
import java.util.UUID

object MessageHelper {
    private const val FILE_NAME = "community_messages_v4.json"
    
    private val conversationListeners = mutableMapOf<String, ValueEventListener>()
    private val allConversationsListeners = mutableMapOf<String, ValueEventListener>()
    
    private fun pushToFirebase(conv: ConversationEntity) {
        try {
            val ref = FirebaseDatabase.getInstance().getReference("conversations").child(conv.id)
            val jsonTree = Gson().toJsonTree(conv)
            val map = Gson().fromJson(jsonTree, Map::class.java)
            ref.setValue(map)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenToConversation(context: Context, conversationId: String, onUpdate: () -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("conversations").child(conversationId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val jsonTree = Gson().toJsonTree(snapshot.value)
                        val conv = Gson().fromJson(jsonTree, ConversationEntity::class.java)
                        
                        val data = readData(context)
                        val index = data.indexOfFirst { it.id == conversationId }
                        if (index != -1) {
                            data[index] = conv
                        } else {
                            data.add(conv)
                        }
                        writeData(context, data)
                        onUpdate()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        conversationListeners[conversationId] = listener
    }

    fun removeConversationListener(conversationId: String) {
        val listener = conversationListeners.remove(conversationId)
        if (listener != null) {
            FirebaseDatabase.getInstance().getReference("conversations").child(conversationId).removeEventListener(listener)
        }
    }
    
    fun listenToAllConversations(context: Context, currentUserId: String, onUpdate: () -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("conversations")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val data = readData(context)
                        var changed = false
                        for (child in snapshot.children) {
                            val jsonTree = Gson().toJsonTree(child.value)
                            val conv = Gson().fromJson(jsonTree, ConversationEntity::class.java)
                            if (conv.members.contains(currentUserId)) {
                                val index = data.indexOfFirst { it.id == conv.id }
                                if (index != -1) {
                                    data[index] = conv
                                } else {
                                    data.add(conv)
                                }
                                changed = true
                            }
                        }
                        if (changed) {
                            writeData(context, data)
                            onUpdate()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        allConversationsListeners[currentUserId] = listener
    }
    
    fun removeAllConversationsListener(currentUserId: String) {
        val listener = allConversationsListeners.remove(currentUserId)
        if (listener != null) {
            FirebaseDatabase.getInstance().getReference("conversations").removeEventListener(listener)
        }
    }

    fun initDataIfNeed(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            try {
                val jsonString = context.assets.open("community_message.json").bufferedReader().use { it.readText() }
                file.writeText(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun readData(context: Context): MutableList<ConversationEntity> {
        initDataIfNeed(context)
        val file = File(context.filesDir, FILE_NAME)
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<ConversationEntity>>() {}.type
            Gson().fromJson<List<ConversationEntity>>(json, type).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeData(context: Context, data: List<ConversationEntity>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val json = Gson().toJson(data)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getConversations(context: Context): List<ConversationEntity> {
        return readData(context).sortedByDescending { it.updatedAt }
    }

    fun getConversation(context: Context, conversationId: String): ConversationEntity? {
        return readData(context).find { it.id == conversationId }
    }

    fun getMessages(context: Context, conversationId: String): List<ChatMessageEntity> {
        return getConversation(context, conversationId)?.messages?.sortedBy { it.sentAt } ?: emptyList()
    }

    private fun getCurrentTimeString(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(java.util.Date())
    }

    fun markAsRead(context: Context, conversationId: String, userId: String) {
        val data = readData(context)
        val index = data.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val conv = data[index]
            val unreadBy = conv.unreadBy.toMutableList()
            unreadBy.remove(userId)
            
            val newMessages = conv.messages.map {
                if (it.senderId != userId && it.seenAt == null) {
                    it.copy(seenAt = getCurrentTimeString())
                } else {
                    it
                }
            }
            
            val updatedConv = conv.copy(unreadBy = unreadBy, messages = newMessages)
            data[index] = updatedConv
            writeData(context, data)
            pushToFirebase(updatedConv)
        }
    }

    fun sendMessage(context: Context, conversationId: String, senderId: String, receiverId: String, text: String) {
        val data = readData(context)
        val index = data.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val conv = data[index]
            val msgId = "m_" + UUID.randomUUID().toString().take(8)
            val timeStr = getCurrentTimeString()
            
            val newMsg = ChatMessageEntity(
                id = msgId,
                senderId = senderId,
                text = text,
                sentAt = timeStr,
                deliveredAt = timeStr,
                seenAt = null
            )
            
            val unreadBy = conv.unreadBy.toMutableList()
            if (!unreadBy.contains(receiverId)) {
                unreadBy.add(receiverId)
            }
            
            val updatedMessages = conv.messages.toMutableList()
            updatedMessages.add(newMsg)
            
            val updatedConv = conv.copy(
                lastMessage = text,
                lastMessageAt = timeStr,
                updatedAt = timeStr,
                unreadBy = unreadBy,
                messages = updatedMessages
            )
            data[index] = updatedConv
            writeData(context, data)
            pushToFirebase(updatedConv)
        }
    }

    fun getOrCreateConversation(context: Context, currentUserId: String, partnerId: String, partnerName: String, partnerAvatar: String): String {
        val data = readData(context)
        val existing = data.find { it.members.contains(currentUserId) && it.members.contains(partnerId) }
        if (existing != null) return existing.id
        
        val newConvId = "chat_${partnerId}_${currentUserId}"
        val timeStr = getCurrentTimeString()
        
        val newConv = ConversationEntity(
            id = newConvId,
            members = listOf(currentUserId, partnerId),
            memberInfo = mapOf(
                currentUserId to MemberInfoEntity("You", ""),
                partnerId to MemberInfoEntity(partnerName, partnerAvatar)
            ),
            activeBy = listOf(),
            typingBy = listOf(),
            unreadBy = listOf(),
            createdAt = timeStr,
            updatedAt = timeStr,
            lastMessage = "",
            lastMessageAt = timeStr,
            chatType = "private",
            messages = listOf()
        )
        
        data.add(newConv)
        writeData(context, data)
        pushToFirebase(newConv)
        return newConvId
    }

    fun updateMessage(context: Context, conversationId: String, messageId: String, newText: String) {
        val data = readData(context)
        val index = data.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val conv = data[index]
            val updatedMessages = conv.messages.map {
                if (it.id == messageId) it.copy(text = newText) else it
            }
            
            var lastMsg = conv.lastMessage
            if (updatedMessages.lastOrNull()?.id == messageId) {
                lastMsg = newText
            }
            
            val updatedConv = conv.copy(messages = updatedMessages, lastMessage = lastMsg)
            data[index] = updatedConv
            writeData(context, data)
            pushToFirebase(updatedConv)
        }
    }

    fun deleteMessage(context: Context, conversationId: String, messageId: String) {
        val data = readData(context)
        val index = data.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val conv = data[index]
            val updatedMessages = conv.messages.filter { it.id != messageId }
            
            var lastMsg = conv.lastMessage
            var lastMsgAt = conv.lastMessageAt
            if (conv.messages.lastOrNull()?.id == messageId) {
                lastMsg = updatedMessages.lastOrNull()?.text ?: ""
                lastMsgAt = updatedMessages.lastOrNull()?.sentAt ?: conv.createdAt
            }
            
            val updatedConv = conv.copy(messages = updatedMessages, lastMessage = lastMsg, lastMessageAt = lastMsgAt)
            data[index] = updatedConv
            writeData(context, data)
            pushToFirebase(updatedConv)
        }
    }
}
