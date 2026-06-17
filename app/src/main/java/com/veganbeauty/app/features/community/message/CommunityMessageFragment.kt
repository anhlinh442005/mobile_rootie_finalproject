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
        messageAdapter = MessageAdapter(emptyList()) { conv ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, ChatDetailFragment.newInstance(conv.conversationId))
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
            
            val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getUsername(requireContext())
            val activeUsers = conversations.filter { it.isActive }
            val myFriendsIds = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getFriendsForUser(currentUserId)
            
            val sortedActiveUsers = activeUsers.sortedByDescending { myFriendsIds.contains(it.partnerId) }
            activeUserAdapter.updateData(sortedActiveUsers)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var conversationsListener: com.google.firebase.database.ValueEventListener? = null

    private fun listenForRealtimeConversations() {
        val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("conversations")
        conversationsListener = dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (context == null) return
                val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(requireContext())
                val usersMap = mutableMapOf<String, Pair<String, String>>()
                try {
                    val usersJsonString = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getRawUsersJson()
                    val usersArray = org.json.JSONArray(usersJsonString)
                    for (i in 0 until usersArray.length()) {
                        val u = usersArray.getJSONObject(i)
                        usersMap[u.getString("user_id")] = Pair(u.optString("username"), u.optString("avatar"))
                    }
                } catch (e: Exception) {}

                val list = mutableListOf<com.veganbeauty.app.data.local.entities.ConversationEntity>()
                
                // Read local ones first
                val localConvs = MessageHelper.getConversations(requireContext())
                val localMap = localConvs.associateBy { it.conversationId }.toMutableMap()

                for (child in snapshot.children) {
                    val convId = child.key ?: continue
                    
                    // Fallback to local if fields missing
                    val local = localMap[convId]

                    // We need to determine participants, partnerId, etc.
                    // This is complex, but we can update the last_message and unread_count for existing local conversations
                    if (local != null) {
                        val lastMsgSnapshot = child.child("last_message")
                        if (lastMsgSnapshot.exists()) {
                            val msgId = lastMsgSnapshot.child("message_id").getValue(String::class.java) ?: ""
                            val senderId = lastMsgSnapshot.child("sender_id").getValue(String::class.java) ?: ""
                            val text = lastMsgSnapshot.child("text").getValue(String::class.java) ?: ""
                            val timestamp = lastMsgSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            local.lastMessage = com.veganbeauty.app.data.local.entities.LastMessageEntity(msgId, senderId, text, timestamp)
                        }

                        val updatedAt = child.child("updated_at").getValue(Long::class.java) ?: local.updatedAt
                        local.updatedAt = updatedAt
                        
                        // We could also update unread_count by checking messages or relying on local count.
                        // For a simple real-time test, just update the last message is enough to show list changes.
                        localMap[convId] = local
                    }
                }
                
                val updatedList = localMap.values.sortedByDescending { it.updatedAt }
                messageAdapter.updateData(updatedList)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }
    
    override fun onResume() {
        super.onResume()
        loadData()
        listenForRealtimeConversations()
    }
    
    override fun onPause() {
        super.onPause()
        conversationsListener?.let {
            com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("conversations").removeEventListener(it)
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
        _binding = null
    }
}
