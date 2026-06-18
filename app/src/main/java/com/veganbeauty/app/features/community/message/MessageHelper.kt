package com.veganbeauty.app.features.community.message

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.veganbeauty.app.data.local.entities.ChatMessageEntity
import com.veganbeauty.app.data.local.entities.ConversationEntity
import com.veganbeauty.app.data.local.entities.MemberInfoEntity
import java.io.File
import java.util.UUID

object MessageHelper {
    private const val FILE_NAME = "community_message.json"
    
    private val conversationListeners = mutableMapOf<String, ListenerRegistration>()
    private val allConversationsListeners = mutableMapOf<String, ListenerRegistration>()
    
    private fun pushToFirebase(conv: ConversationEntity) {
        try {
            val db = FirebaseFirestore.getInstance()
            val jsonTree = Gson().toJsonTree(conv)
            val map = Gson().fromJson(jsonTree, Map::class.java) as Map<String, Any>
            db.collection("community_message").document(conv.id).set(map)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenToConversation(context: Context, conversationId: String, onUpdate: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("community_message").document(conversationId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val map = snapshot.data ?: return@addSnapshotListener
                        val jsonTree = Gson().toJsonTree(map)
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
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        conversationListeners[conversationId] = listener
    }

    fun removeConversationListener(conversationId: String) {
        conversationListeners.remove(conversationId)?.remove()
    }
    
    fun listenToAllConversations(context: Context, currentUserId: String, onUpdate: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val listener = db.collection("community_message")
            .whereArrayContains("members", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val data = readData(context)
                        var changed = false
                        for (doc in snapshot.documents) {
                            val map = doc.data ?: continue
                            val jsonTree = Gson().toJsonTree(map)
                            val conv = Gson().fromJson(jsonTree, ConversationEntity::class.java)
                            
                            val index = data.indexOfFirst { it.id == conv.id }
                            if (index != -1) {
                                data[index] = conv
                            } else {
                                data.add(conv)
                            }
                            changed = true
                        }
                        if (changed) {
                            writeData(context, data)
                            onUpdate()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        allConversationsListeners[currentUserId] = listener
    }
    
    fun removeAllConversationsListener(currentUserId: String) {
        allConversationsListeners.remove(currentUserId)?.remove()
    }

    fun forceResetFirebaseFromAssets(context: Context) {
        Thread {
            try {
                val db = FirebaseFirestore.getInstance()
                // Clear existing
                db.collection("community_message").get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                    
                    // Upload from assets
                    val jsonString = context.assets.open("community_message.json").bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<ConversationEntity>>() {}.type
                    val conversations = Gson().fromJson<List<ConversationEntity>>(jsonString, type)
                    
                    for (conv in conversations) {
                        val jsonTree = Gson().toJsonTree(conv)
                        val map = Gson().fromJson(jsonTree, Map::class.java) as Map<String, Any>
                        db.collection("community_message").document(conv.id).set(map)
                    }
                    
                    // Also overwrite local file
                    val file = File(context.filesDir, FILE_NAME)
                    file.writeText(jsonString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
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

    fun getConversations(context: Context, currentUserId: String): List<ConversationEntity> {
        return readData(context)
            .filter { (it.members ?: emptyList()).contains(currentUserId) }
            .sortedByDescending { it.updatedAt ?: "" }
    }

    fun getConversation(context: Context, conversationId: String): ConversationEntity? {
        return readData(context).find { it.id == conversationId }
    }

    fun getMessages(context: Context, conversationId: String): List<ChatMessageEntity> {
        return getConversation(context, conversationId)?.messages?.sortedBy { it.sentAt ?: "" } ?: emptyList()
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
            val unreadBy = conv.unreadBy?.toMutableList() ?: mutableListOf()
            unreadBy.remove(userId)
            
            val newMessages = (conv.messages ?: emptyList()).map {
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
            
            val unreadBy = conv.unreadBy?.toMutableList() ?: mutableListOf()
            if (!unreadBy.contains(receiverId)) {
                unreadBy.add(receiverId)
            }
            
            val updatedMessages = conv.messages?.toMutableList() ?: mutableListOf()
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
        val existing = data.find { (it.members ?: emptyList()).contains(currentUserId) && (it.members ?: emptyList()).contains(partnerId) }
        if (existing != null) return existing.id
        
        val currentUserName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context) ?: "Unknown"
        val currentUserAvatar = com.veganbeauty.app.data.local.ProfileSession.getAvatar(context) ?: ""
        
        val newConvId = "chat_${partnerId}_${currentUserId}"
        val timeStr = getCurrentTimeString()
        
        val newConv = ConversationEntity(
            id = newConvId,
            members = listOf(currentUserId, partnerId),
            memberInfo = mapOf(
                currentUserId to MemberInfoEntity(currentUserName, currentUserAvatar),
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
            val updatedMessages = (conv.messages ?: emptyList()).map {
                if (it.id == messageId) it.copy(text = newText) else it
            }
            
            var lastMsg = conv.lastMessage ?: ""
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
            val updatedMessages = (conv.messages ?: emptyList()).filter { it.id != messageId }
            
            var lastMsg = conv.lastMessage ?: ""
            var lastMsgAt = conv.lastMessageAt ?: ""
            if ((conv.messages ?: emptyList()).lastOrNull()?.id == messageId) {
                lastMsg = updatedMessages.lastOrNull()?.text ?: ""
                lastMsgAt = updatedMessages.lastOrNull()?.sentAt ?: conv.createdAt ?: ""
            }
            
            val updatedConv = conv.copy(messages = updatedMessages, lastMessage = lastMsg, lastMessageAt = lastMsgAt)
            data[index] = updatedConv
            writeData(context, data)
            pushToFirebase(updatedConv)
        }
    }
}
