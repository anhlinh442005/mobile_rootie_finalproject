package com.veganbeauty.app.features.community.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.ChatItemEntity
import com.veganbeauty.app.data.local.entities.MessageEntity
import com.veganbeauty.app.databinding.ComFragmentMessageBinding
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

class CommunityMessageFragment : Fragment() {

    private var _binding: ComFragmentMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var activeUserAdapter: ActiveUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters()
        loadData()
        setupBottomNavigation()
        setupInteractions()
    }

    private fun setupAdapters() {
        messageAdapter = MessageAdapter(emptyList()) { message ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .add(R.id.main_container, ChatDetailFragment.newInstance(message.id))
                .addToBackStack(null)
                .commit()
        }
        activeUserAdapter = ActiveUserAdapter(emptyList())

        binding.rvMessages.adapter = messageAdapter
        binding.rvActiveUsers.adapter = activeUserAdapter
    }

    private fun loadData() {
        try {
            val inputStream = requireContext().assets.open("community_message.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            
            // Upload to Firestore in background
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                com.veganbeauty.app.data.remote.FirestoreService().uploadAllCommunityMessages(jsonString)
            }
            
            val jsonArray = JSONArray(jsonString)
            val messages = mutableListOf<MessageEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
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
                
                messages.add(
                    MessageEntity(
                        id = obj.optString("id"),
                        partnerId = obj.optString("partner_id"),
                        partnerName = obj.optString("partner_name"),
                        partnerAvatar = obj.optString("partner_avatar"),
                        isActive = obj.optBoolean("is_active"),
                        isUnread = obj.optBoolean("is_unread"),
                        isTyping = obj.optBoolean("is_typing"),
                        messages = chatItems
                    )
                )
            }
            
            messageAdapter.updateData(messages)
            
            // For active users, filter only active ones or just take all for UI purpose
            val activeUsers = messages.filter { it.isActive }
            activeUserAdapter.updateData(activeUsers)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupInteractions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupBottomNavigation() {
        binding.comBottomNav.navComFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.comBottomNav.navComHub.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityBeautyHubFragment())
                .commit()
        }

        binding.comBottomNav.navComExplore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityExploreFragment())
                .commit()
        }
        
        binding.comBottomNav.navComProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.profile.AccountProfileFragment())
                .commit()
        }

        // Keep Tin nhắn active visually
        val chatIcon = binding.comBottomNav.navComChat.getChildAt(0) as? ImageView
        chatIcon?.setColorFilter(resources.getColor(R.color.primary, null))
        val chatText = binding.comBottomNav.navComChat.getChildAt(1) as? TextView
        chatText?.setTextColor(resources.getColor(R.color.primary, null))
        chatText?.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
