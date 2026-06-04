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

        loadData()
    }

    private fun loadData() {
        try {
            val inputStream = requireContext().assets.open("community_message.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
