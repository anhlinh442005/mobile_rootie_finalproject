package com.veganbeauty.app.features.community.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ComFragmentMessageBinding
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment

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
        
        val currentRealId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
        MessageHelper.listenToAllConversations(requireContext(), currentRealId) {
            if (isAdded) {
                loadData()
            }
        }
    }

    private fun setupAdapters() {
        messageAdapter = MessageAdapter(emptyList()) { conv ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, ChatDetailFragment.newInstance(conv.id))
                .addToBackStack(null)
                .commit()
        }
        activeUserAdapter = ActiveUserAdapter(emptyList())

        binding.rvMessages.adapter = messageAdapter
        binding.rvActiveUsers.adapter = activeUserAdapter
    }

    private fun loadData() {
        try {
            val conversations = MessageHelper.getConversations(requireContext())
            messageAdapter.updateData(conversations)
            
            val currentUsername = com.veganbeauty.app.data.local.ProfileSession.getUsername(requireContext())
            val currentRealId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
            
            // Only consider users active if they are in the activeBy list
            val activeUsers = conversations.filter { conv ->
                val partnerId = conv.members.firstOrNull { it != currentRealId } ?: ""
                conv.activeBy.contains(partnerId)
            }
            
            val myFriendsIds = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getFriendsForUser(currentUsername)
            
            // Just use the conversation entities for the active user adapter. 
            // Note: If ActiveUserAdapter still expects the old properties, it might need to be updated.
            // Assuming it accepts ConversationEntity.
            val sortedActiveUsers = activeUsers.sortedByDescending { conv ->
                val partnerId = conv.members.firstOrNull { it != currentRealId } ?: ""
                myFriendsIds.contains(partnerId)
            }
            
            activeUserAdapter.updateData(sortedActiveUsers)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.ivNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.notification.CommunityNotificationFragment())
                .addToBackStack(null)
                .commit()
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
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
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
        val currentRealId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
        MessageHelper.removeAllConversationsListener(currentRealId)
        _binding = null
    }
}
