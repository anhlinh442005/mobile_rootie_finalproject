package com.veganbeauty.app.features.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.SkinChatBinding
import com.veganbeauty.app.databinding.ItemSkinChatLeftBinding
import com.veganbeauty.app.databinding.ItemSkinChatRightBinding
import com.veganbeauty.app.databinding.ItemSkinChatTimeBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SkinChatFragment : DialogFragment() {

    private var _binding: SkinChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: SkinChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    private var firestoreListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    enum class Sender {
        AGENT, USER, TIME
    }

    data class ChatMessage(
        val sender: Sender,
        val text: String,
        val time: String,
        val timestamp: Long
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
    }

    override fun onStart() {
        super.onStart()
        if (showsDialog) {
            dialog?.window?.let { window ->
                val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
                val height = (resources.displayMetrics.heightPixels * 0.85).toInt()
                window.setLayout(width, height)
                window.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }

    private fun setupUI(view: View) {
        if (showsDialog) {
            view.setBackgroundResource(R.drawable.bg_chat_dialog)
            view.clipToOutline = true
            view.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }

        setupRecyclerView()
        setupListeners()

        // 1. Load history from local JSON file
        val history = loadLocalHistory()
        messagesList.clear()
        if (history.isNotEmpty()) {
            messagesList.add(ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", System.currentTimeMillis()))
            messagesList.addAll(history)
        }
        chatAdapter.notifyDataSetChanged()
        binding.rvChatList.scrollToPosition(messagesList.size - 1)

        // 2. Start Firebase real-time listener for admin replies
        startFirestoreListener()
    }

    private fun setupRecyclerView() {
        chatAdapter = SkinChatAdapter(messagesList)
        binding.rvChatList.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatList.adapter = chatAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.btnPlus.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đính kèm tệp sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentTimeStr(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    // JSON persistence helpers
    private fun getLocalHistoryFile(): File {
        return File(requireContext().filesDir, "chat_message.json")
    }

    private fun loadLocalHistory(): List<ChatMessage> {
        val file = getLocalHistoryFile()
        if (!file.exists()) {
            try {
                val jsonString = requireContext().assets.open("chat_message.json").bufferedReader().use { it.readText() }
                file.writeText(jsonString)
            } catch (e: Exception) {
                try {
                    file.writeText("[]")
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }

        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChatMessage(
                        sender = Sender.valueOf(obj.getString("sender")),
                        text = obj.getString("text"),
                        time = obj.getString("time"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveLocalHistory(list: List<ChatMessage>) {
        val file = getLocalHistoryFile()
        try {
            val jsonArray = JSONArray()
            for (msg in list) {
                if (msg.sender == Sender.TIME) continue
                val obj = JSONObject()
                obj.put("sender", msg.sender.name)
                obj.put("text", msg.text)
                obj.put("time", msg.time)
                obj.put("timestamp", msg.timestamp)
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Firestore Synchronization
    private fun getCurrentUserId(): String {
        return ProfileSession.getCurrentUserId(requireContext()) ?: "guest_user"
    }

    private fun getCurrentUsername(): String {
        return ProfileSession.getUsername(requireContext()) ?: "Khách"
    }

    @Suppress("UNCHECKED_CAST")
    private fun startFirestoreListener() {
        val currentUserId = getCurrentUserId()
        
        firestoreListener = db.collection("chat_messages").document(currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val messagesRaw = snapshot.get("messages") as? List<Map<String, Any>>
                    if (messagesRaw != null) {
                        val remoteList = messagesRaw.map { map ->
                            val senderStr = map["sender"] as? String ?: "AGENT"
                            ChatMessage(
                                sender = Sender.valueOf(senderStr),
                                text = map["text"] as? String ?: "",
                                time = map["time"] as? String ?: "",
                                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                        }
                        
                        val localList = loadLocalHistory()
                        if (isDifferent(localList, remoteList)) {
                            // Save locally
                            saveLocalHistory(remoteList)
                            
                            // Update display list
                            messagesList.clear()
                            messagesList.add(ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", System.currentTimeMillis()))
                            messagesList.addAll(remoteList)
                            chatAdapter.notifyDataSetChanged()
                            binding.rvChatList.scrollToPosition(messagesList.size - 1)
                        }
                    }
                }
            }
    }

    private fun isDifferent(local: List<ChatMessage>, remote: List<ChatMessage>): Boolean {
        if (local.size != remote.size) return true
        for (i in local.indices) {
            if (local[i].sender != remote[i].sender ||
                local[i].text != remote[i].text ||
                local[i].time != remote[i].time) {
                return true
            }
        }
        return false
    }

    private fun uploadChatToFirestore(list: List<ChatMessage>) {
        val currentUserId = getCurrentUserId()
        val currentUsername = getCurrentUsername()
        
        val actualMessages = list.filter { it.sender != Sender.TIME }
        val lastMsg = actualMessages.lastOrNull()
        
        val dataMap = hashMapOf(
            "userId" to currentUserId,
            "username" to currentUsername,
            "lastMessage" to (lastMsg?.text ?: ""),
            "lastMessageAt" to (lastMsg?.timestamp ?: System.currentTimeMillis()),
            "messages" to actualMessages.map { msg ->
                hashMapOf(
                    "sender" to msg.sender.name,
                    "text" to msg.text,
                    "time" to msg.time,
                    "timestamp" to msg.timestamp
                )
            }
        )
        
        db.collection("chat_messages").document(currentUserId)
            .set(dataMap)
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun sendMessage() {
        val text = binding.etMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        val now = System.currentTimeMillis()
        val timeStr = getCurrentTimeStr()

        // 1. Get history and check if session is active (diff <= 1 hour)
        val history = loadLocalHistory()
        val lastMsg = history.lastOrNull { it.sender != Sender.TIME }
        val isNewSession = if (lastMsg == null) {
            true
        } else {
            val diffMs = now - lastMsg.timestamp
            diffMs > 3600000 // 1 hour threshold
        }

        // 2. Add user message
        val userMsg = ChatMessage(Sender.USER, text, timeStr, now)
        val updatedHistory = history.toMutableList()
        updatedHistory.add(userMsg)

        // Save locally and upload
        saveLocalHistory(updatedHistory)
        
        // Update local display list
        messagesList.clear()
        messagesList.add(ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", now))
        messagesList.addAll(updatedHistory)
        chatAdapter.notifyDataSetChanged()
        binding.rvChatList.scrollToPosition(messagesList.size - 1)
        binding.etMessageInput.setText("")

        // Sync user message to Firestore
        uploadChatToFirestore(updatedHistory)

        // 3. Trigger consultant greeting message only if it is a new session
        if (isNewSession) {
            handler.postDelayed({
                if (isAdded) {
                    val greetingTimeStr = getCurrentTimeStr()
                    val greetingTimeMs = System.currentTimeMillis()
                    
                    val greetingMsg = ChatMessage(
                        Sender.AGENT,
                        "Chào bạn, tôi là chuyên gia tư vấn Rootie. Tôi có thể giúp gì cho bạn hôm nay?",
                        greetingTimeStr,
                        greetingTimeMs
                    )
                    
                    val historyWithGreeting = loadLocalHistory().toMutableList()
                    historyWithGreeting.add(greetingMsg)
                    
                    // Save and update UI
                    saveLocalHistory(historyWithGreeting)
                    
                    messagesList.clear()
                    messagesList.add(ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", greetingTimeMs))
                    messagesList.addAll(historyWithGreeting)
                    chatAdapter.notifyDataSetChanged()
                    binding.rvChatList.scrollToPosition(messagesList.size - 1)
                    
                    // Sync greeting to Firestore
                    uploadChatToFirestore(historyWithGreeting)
                }
            }, 1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    inner class SkinChatAdapter(private val list: List<ChatMessage>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_LEFT = 0
        private val TYPE_RIGHT = 1
        private val TYPE_TIME = 2

        override fun getItemViewType(position: Int): Int {
            return when (list[position].sender) {
                Sender.AGENT -> TYPE_LEFT
                Sender.USER -> TYPE_RIGHT
                Sender.TIME -> TYPE_TIME
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_LEFT -> {
                    val b = ItemSkinChatLeftBinding.inflate(inflater, parent, false)
                    LeftViewHolder(b)
                }
                TYPE_RIGHT -> {
                    val b = ItemSkinChatRightBinding.inflate(inflater, parent, false)
                    RightViewHolder(b)
                }
                else -> {
                    val b = ItemSkinChatTimeBinding.inflate(inflater, parent, false)
                    TimeViewHolder(b)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = list[position]
            when (holder) {
                is LeftViewHolder -> {
                    holder.binding.tvMessage.text = item.text
                    holder.binding.tvTime.text = item.time
                }
                is RightViewHolder -> {
                    holder.binding.tvMessage.text = item.text
                    holder.binding.tvTime.text = item.time
                }
                is TimeViewHolder -> {
                    holder.binding.tvTimeText.text = item.text
                }
            }
        }

        override fun getItemCount(): Int = list.size

        inner class LeftViewHolder(val binding: ItemSkinChatLeftBinding) :
            RecyclerView.ViewHolder(binding.root)

        inner class RightViewHolder(val binding: ItemSkinChatRightBinding) :
            RecyclerView.ViewHolder(binding.root)

        inner class TimeViewHolder(val binding: ItemSkinChatTimeBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
