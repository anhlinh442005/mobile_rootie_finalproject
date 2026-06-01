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
import com.veganbeauty.app.R

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

        // Set click listener for the top home icon
        binding.ivHome.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.home.HomeFragment())
                .commit()
        }

        // Set click listeners for the specialized community navbar
        binding.comBottomNav.navComFeed.setOnClickListener {
            // Scroll back to top immediately
            binding.nsvFeed.smoothScrollTo(0, 0)
            
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Shuffle lists to create a randomized feed
                val shuffledPosts = viewModel.posts.value?.shuffled() ?: emptyList()
                val shuffledUsers = viewModel.users.value?.shuffled() ?: emptyList()
                val shuffledReels = viewModel.reels.value?.shuffled() ?: emptyList()
                val productsList = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getProducts()
                
                postAdapter.updateData(shuffledPosts, shuffledUsers.take(10), shuffledReels, productsList)
            }, 800)
        }

        binding.comBottomNav.navComProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.home.HomeFragment())
                .commit()
        }

        binding.comBottomNav.navComHub.setOnClickListener {
            android.widget.Toast.makeText(context, "Beauty Hub đang được phát triển!", android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.comBottomNav.navComExplore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityExploreFragment())
                .commit()
        }

        binding.comBottomNav.navComChat.setOnClickListener {
            android.widget.Toast.makeText(context, "Mục Tin nhắn đang được phát triển!", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Hide/Show bottom navbar on scroll
        binding.nsvFeed.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 15) {
                hideBottomNavigation()
            } else if (dy < -15) {
                showBottomNavigation()
            }
        })

        // Initialize category filters
        setupFilters()

        // Set pull to refresh spinner color
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary)

        // Pull to refresh logic
        binding.swipeRefreshLayout.setOnRefreshListener {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val shuffledPosts = viewModel.posts.value?.shuffled() ?: emptyList()
                val shuffledUsers = viewModel.users.value?.shuffled() ?: emptyList()
                val shuffledReels = viewModel.reels.value?.shuffled() ?: emptyList()
                val productsList = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getProducts()
                
                postAdapter.updateData(shuffledPosts, shuffledUsers.take(10), shuffledReels, productsList)
                binding.swipeRefreshLayout.isRefreshing = false
            }, 800)
        }
    }

    private var isNavVisible = true

    private fun hideBottomNavigation() {
        if (!isNavVisible) return
        isNavVisible = false
        binding.comBottomNav.root.animate()
            .translationY(binding.comBottomNav.root.height.toFloat())
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start()
    }

    private fun showBottomNavigation() {
        if (isNavVisible) return
        isNavVisible = true
        binding.comBottomNav.root.animate()
            .translationY(0f)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start()
    }

    private var currentFilter = "Tất cả"

    private fun setupFilters() {
        val filters = listOf(
            binding.tvFilterAll, binding.tvFilterRoutine, binding.tvFilterReview, binding.tvFilterReels,
            binding.tvFilterKienThuc, binding.tvFilterHoiDap, binding.tvFilterDaDau, binding.tvFilterMun,
            binding.tvFilterDaKho, binding.tvFilterThamMun, binding.tvFilterDiUng
        )
        filters.forEach { textView ->
            textView.setOnClickListener {
                // Update backgrounds
                filters.forEach { tv ->
                    tv.background = requireContext().getDrawable(R.drawable.com_bg_filter_normal)
                }
                textView.background = requireContext().getDrawable(R.drawable.com_bg_filter_selected)
                
                currentFilter = textView.text.toString()
                updateFeedData()
            }
        }
    }

    private fun updateFeedData() {
        var postsList = viewModel.posts.value ?: emptyList()
        val usersList = viewModel.users.value ?: emptyList()
        val reelsList = viewModel.reels.value ?: emptyList()

        if (currentFilter == "Reels") {
            binding.rvPosts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3)
            val dp1 = (1 * resources.displayMetrics.density).toInt()
            binding.rvPosts.setPadding(dp1, dp1, dp1, dp1)
            binding.rvPosts.clipToPadding = false
            val reelAdapter = ReelAdapter(reelsList, isGrid = true)
            binding.rvPosts.adapter = reelAdapter
        } else {
            binding.rvPosts.setPadding(0, 0, 0, 0)
            binding.rvPosts.clipToPadding = true
            if (binding.rvPosts.adapter != postAdapter) {
                binding.rvPosts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                binding.rvPosts.adapter = postAdapter
            }

            if (currentFilter != "Tất cả") {
                postsList = postsList.filter { 
                    it.type.equals(currentFilter, ignoreCase = true) ||
                    it.skinType.equals(currentFilter, ignoreCase = true) ||
                    it.concern.equals(currentFilter, ignoreCase = true)
                }
            }
            
            val productsList = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getProducts()
            postAdapter.updateData(postsList, usersList.take(10), reelsList, productsList)
        }
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
