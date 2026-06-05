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
                val shuffled = users.shuffled()
                suggestAdapter.updateData(shuffled.take(5))
                requestAdapter.updateData(shuffled.drop(5).take(3))
                followBackAdapter.updateData(shuffled.drop(8).take(3))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

