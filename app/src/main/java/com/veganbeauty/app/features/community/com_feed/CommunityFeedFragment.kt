package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentFeedBinding

class CommunityFeedFragment : RootieFragment() {

    private var _binding: ComFragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private val storyAdapter = StoryAdapter(emptyList())
    private val postAdapter = PostAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentFeedBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CommunityViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.rvStories.adapter = storyAdapter
        binding.rvPosts.adapter = postAdapter
    }

    private fun updateFeedData() {
        val postsList = viewModel.posts.value ?: emptyList()
        val usersList = viewModel.users.value ?: emptyList()
        val reelsList = viewModel.reels.value ?: emptyList()
        postAdapter.updateData(postsList, usersList.take(10), reelsList)
    }

    override fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            val allStories = users.toMutableList()
            if (allStories.isNotEmpty()) {
                val myStory = allStories[0].copy(username = "Tin của bạn")
                allStories.add(0, myStory)
            }
            storyAdapter.updateData(allStories)
            updateFeedData()
        }

        viewModel.posts.observe(viewLifecycleOwner) { _ ->
            updateFeedData()
        }

        viewModel.reels.observe(viewLifecycleOwner) { _ ->
            updateFeedData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
