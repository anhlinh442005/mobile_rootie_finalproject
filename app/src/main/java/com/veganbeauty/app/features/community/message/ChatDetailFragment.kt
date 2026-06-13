package com.veganbeauty.app.features.community.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import org.json.JSONArray
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.ChatItemEntity
import com.veganbeauty.app.data.local.entities.MessageEntity
import com.veganbeauty.app.databinding.ComFragmentChatDetailBinding
import androidx.lifecycle.lifecycleScope

class ChatDetailFragment : Fragment() {

    private var _binding: ComFragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private var conversationId: String? = null
    private lateinit var chatAdapter: ChatDetailAdapter

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
        
        chatAdapter = ChatDetailAdapter(emptyList())
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

        loadData()
    }

    private fun loadData() {
        try {
            val jsonString = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getMessagesData()
            
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == conversationId) {
                    binding.tvName.text = obj.optString("partner_name")
                    val avatarUrl = obj.optString("partner_avatar")
                    if (avatarUrl.isNotEmpty()) {
                        binding.ivAvatar.load(avatarUrl) {
                            crossfade(true)
                            placeholder(R.color.gray_light)
                            transformations(CircleCropTransformation())
                        }
                    }
                    val isActive = obj.optBoolean("is_active")
                    binding.vActiveDot.visibility = if (isActive) View.VISIBLE else View.GONE
                    binding.tvStatus.text = if (isActive) "Đang hoạt động" else "Ngoại tuyến"
                    
                    val chatItems = mutableListOf<ChatItemEntity>()
                    val messagesArray = obj.optJSONArray("messages")
                    if (messagesArray != null) {
                        for (j in 0 until messagesArray.length()) {
                            val msgObj = messagesArray.getJSONObject(j)
                            chatItems.add(
                                ChatItemEntity(
                                    id = msgObj.optString("id"),
                                    senderId = msgObj.optString("sender_id"),
                                    text = msgObj.optString("text"),
                                    timestamp = msgObj.optString("timestamp"),
                                    isMine = msgObj.optBoolean("is_mine"),
                                    status = msgObj.optString("status")
                                )
                            )
                        }
                    }
                    
                    chatAdapter.setPartnerAvatar(avatarUrl)
                    chatAdapter.updateData(chatItems)
                    binding.rvChat.scrollToPosition(chatItems.size - 1)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMessage(text: String) {
        val reader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
        val jsonString = reader.getMessagesData()
        val jsonArray = org.json.JSONArray(jsonString)
        var partnerId = ""
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.optString("id") == conversationId) {
                partnerId = obj.optString("partner_id")
                val messagesArray = obj.optJSONArray("messages") ?: org.json.JSONArray()
                
                val newMsg = org.json.JSONObject()
                newMsg.put("id", "m" + System.currentTimeMillis())
                newMsg.put("sender_id", "test_001")
                newMsg.put("text", text)
                newMsg.put("timestamp", "Vừa xong")
                newMsg.put("is_mine", true)
                newMsg.put("status", "Đã gửi")
                
                messagesArray.put(newMsg)
                obj.put("messages", messagesArray)
                break
            }
        }
        
        reader.saveMessagesData(jsonArray.toString())
        loadData() // Reload UI
        
        // Simulate partner replying
        if (partnerId.isNotEmpty()) {
            simulateReply(partnerId)
        }
    }
    
    private fun simulateReply(partnerId: String) {
        lifecycleScope.launchWhenStarted {
            kotlinx.coroutines.delay(2000)
            val reader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
            val jsonString = reader.getMessagesData()
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.optString("id") == conversationId) {
                    val messagesArray = obj.optJSONArray("messages") ?: org.json.JSONArray()
                    
                    val replyMsg = org.json.JSONObject()
                    replyMsg.put("id", "m" + System.currentTimeMillis())
                    replyMsg.put("sender_id", partnerId)
                    replyMsg.put("text", "Dạ mình nhận được tin nhắn của bạn rồi nhé. Mình sẽ phản hồi bạn trong chốc lát ạ!")
                    replyMsg.put("timestamp", "Vừa xong")
                    replyMsg.put("is_mine", false)
                    replyMsg.put("status", "")
                    
                    messagesArray.put(replyMsg)
                    obj.put("messages", messagesArray)
                    break
                }
            }
            reader.saveMessagesData(jsonArray.toString())
            if (isAdded) {
                loadData() // Reload UI
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
