package com.veganbeauty.app.features.community.message

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ComFragmentChatDetailBinding
import kotlinx.coroutines.delay

class ChatDetailFragment : Fragment() {

    private var _binding: ComFragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private var conversationId: String? = null
    private lateinit var chatAdapter: ChatDetailAdapter
    private var partnerId: String = ""

    companion object {
        private const val ARG_CONVERSATION_ID = "conversation_id"

        fun newInstance(id: String) = ChatDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CONVERSATION_ID, id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationId = arguments?.getString(ARG_CONVERSATION_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        chatAdapter = ChatDetailAdapter(emptyList()) { msg ->
            showMessageOptionsDialog(msg)
        }
        binding.rvChat.adapter = chatAdapter
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.ivSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.ivSend.visibility = View.VISIBLE
                    binding.llRightIcons.visibility = View.GONE
                } else {
                    binding.ivSend.visibility = View.GONE
                    binding.llRightIcons.visibility = View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadData()
        listenForRealtimeMessages()
    }

    private var messagesListener: com.google.firebase.database.ValueEventListener? = null

    private fun listenForRealtimeMessages() {
        val convId = conversationId ?: return
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("conversations").child(convId).child("messages")

        messagesListener = dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val newMessages = mutableListOf<com.veganbeauty.app.data.local.entities.ChatMessageEntity>()
                for (child in snapshot.children) {
                    val msgId = child.child("message_id").getValue(String::class.java) ?: continue
                    val senderId = child.child("sender_id").getValue(String::class.java) ?: ""
                    val receiverId = child.child("receiver_id").getValue(String::class.java) ?: ""
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val timestamp = child.child("created_at").getValue(Long::class.java) ?: 0L
                    
                    val statusMap = mutableMapOf<String, String>()
                    val statusSnapshot = child.child("status")
                    for (status in statusSnapshot.children) {
                        statusMap[status.key ?: ""] = status.getValue(String::class.java) ?: "unread"
                    }

                    newMessages.add(com.veganbeauty.app.data.local.entities.ChatMessageEntity(
                        messageId = msgId,
                        conversationId = convId,
                        senderId = senderId,
                        receiverId = receiverId,
                        text = text,
                        type = child.child("type").getValue(String::class.java) ?: "text",
                        createdAt = timestamp,
                        status = statusMap,
                        readAt = emptyMap()
                    ))
                }

                if (newMessages.isNotEmpty()) {
                    newMessages.sortBy { it.createdAt }
                    chatAdapter.updateData(newMessages)
                    binding.rvChat.scrollToPosition(newMessages.size - 1)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun loadData() {
        conversationId?.let { convId ->
            val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
            // Mark conversation as read when opening
            MessageHelper.markAsRead(requireContext(), convId, currentUserId)

            val conv = MessageHelper.getConversation(requireContext(), convId)
            if (conv != null) {
                partnerId = conv.partnerId
                binding.tvName.text = conv.partnerName
                
                // Show verified tick for official Rootie VietNam account
                if (partnerId == "rootie_vn") {
                    binding.ivVerified.visibility = View.VISIBLE
                } else {
                    binding.ivVerified.visibility = View.GONE
                }
                
                val avatarUrl = conv.partnerAvatar
                if (avatarUrl.isNotEmpty()) {
                    binding.ivAvatar.load(avatarUrl) {
                        crossfade(true)
                        placeholder(R.color.gray_light)
                        if (partnerId == "rootie_vn") {
                            error(R.drawable.ic_logo_rootie)
                        } else {
                            error(R.drawable.img_avatar)
                        }
                        transformations(CircleCropTransformation())
                    }
                } else {
                    if (partnerId == "rootie_vn") {
                        binding.ivAvatar.setImageResource(R.drawable.ic_logo_rootie)
                    } else {
                        binding.ivAvatar.setImageResource(R.drawable.img_avatar)
                    }
                }
                binding.vActiveDot.visibility = if (conv.isActive) View.VISIBLE else View.GONE
                binding.tvStatus.text = if (conv.isActive) "Đang hoạt động" else "Hoạt động 15 phút trước"

                chatAdapter.setPartnerAvatar(avatarUrl)
            }

            // Load local initial data while waiting for Firebase
            val messages = MessageHelper.getMessages(requireContext(), convId)
            chatAdapter.updateData(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun refreshLocalMessages(convId: String) {
        val messages = MessageHelper.getMessages(requireContext(), convId)
        chatAdapter.updateData(messages)
        if (messages.isNotEmpty()) {
            binding.rvChat.scrollToPosition(messages.size - 1)
        }
    }

    private fun sendMessage(text: String) {
        conversationId?.let { convId ->
            val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
            MessageHelper.sendMessage(requireContext(), convId, currentUserId, partnerId, text)
            refreshLocalMessages(convId)
        }
    }

    private fun showMessageOptionsDialog(msg: com.veganbeauty.app.data.local.entities.ChatMessageEntity) {
        val options = arrayOf("Chỉnh sửa", "Thu hồi")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Edit
                        showEditDialog(msg)
                    }
                    1 -> { // Revoke
                        revokeMessage(msg)
                    }
                }
            }
            .show()
    }

    private fun showEditDialog(msg: com.veganbeauty.app.data.local.entities.ChatMessageEntity) {
        val input = android.widget.EditText(requireContext())
        input.setText(msg.text)
        input.setSelection(input.text.length)
        input.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 20, 50, 0)
        }

        val container = android.widget.FrameLayout(requireContext())
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh sửa tin nhắn")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty() && newText != msg.text) {
                    updateMessage(msg.messageId, newText)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateMessage(msgId: String, newText: String) {
        conversationId?.let { convId ->
            MessageHelper.updateMessage(requireContext(), convId, msgId, newText)
            refreshLocalMessages(convId)
        }
    }

    private fun revokeMessage(msg: com.veganbeauty.app.data.local.entities.ChatMessageEntity) {
        conversationId?.let { convId ->
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Thu hồi tin nhắn")
                .setMessage("Tin nhắn này sẽ bị xóa ở cả 2 phía. Bạn có chắc chắn muốn thu hồi?")
                .setPositiveButton("Thu hồi") { _, _ ->
                    MessageHelper.deleteMessage(requireContext(), convId, msg.messageId)
                    refreshLocalMessages(convId)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        conversationId?.let { convId ->
            messagesListener?.let {
                com.google.firebase.database.FirebaseDatabase.getInstance().reference
                    .child("conversations").child(convId).child("messages")
                    .removeEventListener(it)
            }
        }
        _binding = null
    }
}
