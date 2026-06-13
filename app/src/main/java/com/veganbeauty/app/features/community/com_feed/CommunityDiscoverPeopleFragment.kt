package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentDiscoverPeopleBinding
import com.veganbeauty.app.R
import kotlinx.coroutines.launch

class CommunityDiscoverPeopleFragment : RootieFragment() {

    private var _binding: ComFragmentDiscoverPeopleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private lateinit var communityDao: com.veganbeauty.app.data.local.dao.CommunityDao
    private val firestoreService = FirestoreService()

    private fun handleUserAction(user: com.veganbeauty.app.data.local.entities.UserEntity, action: String) {
        val content = if (action == "FOLLOW") "Bạn đã bắt đầu theo dõi ${user.username}" else "Bạn đã chấp nhận yêu cầu theo dõi của ${user.username}"
        val memory = com.veganbeauty.app.data.local.entities.UserMemoryEntity(
            actionType = action,
            targetUserId = user.user_id,
            targetUsername = user.username,
            targetAvatar = user.avatar ?: "",
            content = content
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            // Save to local SQLite
            communityDao.insertUserMemory(memory)
            // Upload to Firebase
            firestoreService.uploadUserMemory(memory)
        }
    }

    private val suggestAdapter = DiscoverPeopleAdapter(emptyList(), DiscoverPeopleAdapter.ActionType.SUGGEST) { user, act -> handleUserAction(user, act) }
    private val requestAdapter = DiscoverPeopleAdapter(emptyList(), DiscoverPeopleAdapter.ActionType.REQUEST) { user, act -> handleUserAction(user, act) }
    private val followBackAdapter = DiscoverPeopleAdapter(emptyList(), DiscoverPeopleAdapter.ActionType.FOLLOW_BACK) { user, act -> handleUserAction(user, act) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentDiscoverPeopleBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        communityDao = db.communityDao()
        val repository = CommunityRepository(communityDao, LocalJsonReader(requireContext()), FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CommunityViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.rvSuggestions.adapter = suggestAdapter
        binding.rvRequests.adapter = requestAdapter
        binding.rvFollowBacks.adapter = followBackAdapter
    }

    override fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            if (users.isNotEmpty()) {
                val jsonReader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
                val mySocialData = jsonReader.getSocialDataForUser("test_001")
                val myFriends = (mySocialData["friends"] ?: emptyList()).toSet()
                
                val suggestIds = mySocialData["suggested"] ?: emptyList()
                val requestIds = mySocialData["friend_requests"] ?: emptyList()
                // For "Follow Back" (Theo dõi lại), these are people in "followers" but NOT in "following" and NOT "friends"
                val followers = (mySocialData["followers"] ?: emptyList()).toSet()
                val following = (mySocialData["following"] ?: emptyList()).toSet()
                val followBackIds = followers.subtract(following).subtract(myFriends).toList()
                
                val suggestions = users.filter { it.user_id in suggestIds && it.user_id != "test_001" }
                val requests = users.filter { it.user_id in requestIds && it.user_id != "test_001" }
                val followBacks = users.filter { it.user_id in followBackIds && it.user_id != "test_001" }
                
                // Calculate mutual friends
                val allTargetIds = suggestions.map { it.user_id } + requests.map { it.user_id } + followBacks.map { it.user_id }
                val mutualMap = jsonReader.getMutualFriendsForUsers(myFriends, allTargetIds)
                
                val applyMutualData = { list: List<com.veganbeauty.app.data.local.entities.UserEntity> ->
                    list.forEach { user ->
                        val mutualIds = mutualMap[user.user_id] ?: emptyList()
                        user.mutualCount = mutualIds.size
                        if (mutualIds.isNotEmpty()) {
                            val mutualUsers = users.filter { it.user_id in mutualIds }
                            if (mutualUsers.isNotEmpty()) {
                                user.firstMutualFriendName = mutualUsers[0].full_name.ifBlank { mutualUsers[0].username }
                                user.mutualFriendAvatars = mutualUsers.mapNotNull { it.avatar }.take(3)
                            }
                        }
                    }
                }
                
                applyMutualData(suggestions)
                applyMutualData(requests)
                applyMutualData(followBacks)
                
                suggestAdapter.updateData(suggestions)
                requestAdapter.updateData(requests)
                followBackAdapter.updateData(followBacks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

