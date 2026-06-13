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
    }

    private fun loadData() {
        conversationId?.let { convId ->
            // Mark conversation as read when opening
            MessageHelper.markAsRead(requireContext(), convId, "test_001")

            val conv = MessageHelper.getConversation(requireContext(), convId)
            if (conv != null) {
                partnerId = conv.partnerId
                binding.tvName.text = conv.partnerName
                val avatarUrl = conv.partnerAvatar
                if (avatarUrl.isNotEmpty()) {
                    binding.ivAvatar.load(avatarUrl) {
                        crossfade(true)
                        placeholder(R.color.gray_light)
                        transformations(CircleCropTransformation())
                    }
                }
                binding.vActiveDot.visibility = if (conv.isActive) View.VISIBLE else View.GONE
                binding.tvStatus.text = if (conv.isActive) "Đang hoạt động" else "Hoạt động 15 phút trước"

                chatAdapter.setPartnerAvatar(avatarUrl)
            }

            val messages = MessageHelper.getMessages(requireContext(), convId)
            chatAdapter.updateData(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage(text: String) {
        conversationId?.let { convId ->
            MessageHelper.sendMessage(requireContext(), convId, "test_001", partnerId, text)
            loadData() // Reload UI
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
