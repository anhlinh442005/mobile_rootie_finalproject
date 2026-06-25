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
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.SkinChatBinding
import com.veganbeauty.app.databinding.ItemSkinChatLeftBinding
import com.veganbeauty.app.databinding.ItemSkinChatRightBinding
import com.veganbeauty.app.databinding.ItemSkinChatTimeBinding
import com.veganbeauty.app.features.community.message.MessageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SkinChatFragment : DialogFragment() {

    private var _binding: SkinChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: SkinChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()
    private var conversationId: String = ""

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

        val currentUserId = getCurrentUserId()
        conversationId = MessageHelper.getOrCreateConversation(
            requireContext(),
            currentUserId,
            "rootie_vn",
            "Rootie VietNam",
            "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
        )

        loadConversationData()
        
        MessageHelper.listenToConversation(requireContext(), conversationId) {
            if (isAdded) {
                loadConversationData()
            }
        }
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

    private fun loadConversationData() {
        val currentUserId = getCurrentUserId()
        MessageHelper.markAsRead(requireContext(), conversationId, currentUserId)
        
        val rawMessages = MessageHelper.getMessages(requireContext(), conversationId)
        messagesList.clear()
        messagesList.add(ChatMessage(Sender.TIME, "Lịch sử cuộc trò chuyện", "", System.currentTimeMillis()))
        for (msg in rawMessages) {
            val isAgent = msg.senderId == "rootie_vn"
            val timestamp = parseIsoString(msg.sentAt)
            val timeStr = formatTime(timestamp)
            messagesList.add(
                ChatMessage(
                    sender = if (isAgent) Sender.AGENT else Sender.USER,
                    text = msg.text,
                    time = timeStr,
                    timestamp = timestamp
                )
            )
        }
        chatAdapter.notifyDataSetChanged()
        if (messagesList.isNotEmpty()) {
            binding.rvChatList.scrollToPosition(messagesList.size - 1)
        }
    }

    private fun parseIsoString(isoStr: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    private fun getCurrentUserId(): String {
        return ProfileSession.getCurrentUserId(requireContext()) ?: "guest_user"
    }

    private fun sendMessage() {
        val text = binding.etMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        val currentUserId = getCurrentUserId()
        
        val rawMessages = MessageHelper.getMessages(requireContext(), conversationId)
        val now = System.currentTimeMillis()
        val isNewSession = if (rawMessages.isEmpty()) {
            true
        } else {
            val lastMsgTime = parseIsoString(rawMessages.last().sentAt)
            (now - lastMsgTime) > 3600000
        }

        MessageHelper.sendMessage(requireContext(), conversationId, currentUserId, "rootie_vn", text)
        binding.etMessageInput.setText("")
        loadConversationData()

        if (isNewSession) {
            binding.rvChatList.postDelayed({
                if (isAdded) {
                    MessageHelper.sendMessage(
                        requireContext(), 
                        conversationId, 
                        "rootie_vn", 
                        currentUserId, 
                        "Chào bạn, tôi là chuyên gia tư vấn Rootie. Tôi có thể giúp gì cho bạn hôm nay?"
                    )
                    loadConversationData()
                }
            }, 1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MessageHelper.removeConversationListener(conversationId)
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
