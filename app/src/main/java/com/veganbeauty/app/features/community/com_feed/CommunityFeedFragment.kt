package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentFeedBinding
import com.veganbeauty.app.R
import coil.load
import androidx.lifecycle.lifecycleScope

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
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[CommunityViewModel::class.java]
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

        // Open Side Menu
        binding.ivMenu.setOnClickListener {
            view.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        // Close Side Menu
        view.findViewById<View>(R.id.ivCloseMenu)?.setOnClickListener {
            view.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout)?.closeDrawer(androidx.core.view.GravityCompat.START)
        }

        // Set click listeners for the specialized community navbar
        binding.comBottomNav.navComFeed.setOnClickListener {
            // Scroll back to top immediately
            binding.nsvFeed.smoothScrollTo(0, 0)
            
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                updateFeedData()
            }
        }

        binding.comBottomNav.navComProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.comBottomNav.navComHub.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment())
                .commit()
        }

        binding.comBottomNav.navComExplore.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityExploreFragment())
                .commit()
        }

        binding.comBottomNav.navComChat.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.message.CommunityMessageFragment())
                .commit()
        }

        // Hide/Show bottom navbar on scroll
        binding.nsvFeed.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 15) {
                hideBottomNavigation()
            } else if (dy < -15) {
                showBottomNavigation()
            }
            
            if (!v.canScrollVertically(1) && currentFilter != "Reels") {
                if (!isLoadingMore && currentPage * postsPerPage < allFilteredPosts.size) {
                    isLoadingMore = true
                    currentPage++
                    updateFeedData(resetData = false)
                }
            }
        })

        // Initialize category filters
        setupFilters()

        // Set pull to refresh spinner color
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary)

        // Pull to refresh logic
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                kotlinx.coroutines.delay(800)
                updateFeedData()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        binding.ivPlus.setOnClickListener {
            val bottomSheet = ComCreatePostBottomSheet()
            bottomSheet.show(parentFragmentManager, ComCreatePostBottomSheet.TAG)
        }
        
        binding.ivHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            com.veganbeauty.app.features.home.BottomNavHelper.navigate(this, R.id.nav_home)
        }
        
        // Initialize Side Menu User Info
        updateSideMenuUserInfo(null)
    }

    private fun updateSideMenuUserInfo(user: com.veganbeauty.app.data.local.entities.UserEntity?) {
        val navView = view?.findViewById<com.google.android.material.navigation.NavigationView>(R.id.navView)
        val ivAvatar = navView?.findViewById<android.widget.ImageView>(R.id.ivSideMenuAvatar)
        val tvDisplayName = navView?.findViewById<android.widget.TextView>(R.id.tvSideMenuDisplayName)
        val tvUsername = navView?.findViewById<android.widget.TextView>(R.id.tvSideMenuUsername)

        if (user != null) {
            tvDisplayName?.text = user.username // Replace with displayName if added to UserEntity later
            tvUsername?.text = "@${user.username.lowercase().replace(" ", "_")}"
            
            if (!user.avatar.isNullOrEmpty() && ivAvatar != null) {
                ivAvatar.visibility = View.VISIBLE
                ivAvatar.load(user.avatar) {
                    crossfade(true)
                    transformations(coil.transform.CircleCropTransformation())
                    placeholder(R.drawable.img_avatar) // fallback
                }
            }
        } else {
            // Default placeholder if not logged in
            tvDisplayName?.text = "Ánh Linh"
            tvUsername?.text = "@eng_lyns"
            if (ivAvatar != null) {
                ivAvatar.load("https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg") {
                    crossfade(true)
                    transformations(coil.transform.CircleCropTransformation())
                    placeholder(R.drawable.img_avatar)
                }
            }
        }
    }

    private var isNavVisible = true
    private var currentPage = 1
    private val postsPerPage = 5
    private var isLoadingMore = false
    private var allFilteredPosts: List<com.veganbeauty.app.data.local.entities.CommunityPostEntity> = emptyList()

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
                // Update backgrounds and text colors
                filters.forEach { tv ->
                    tv.background = requireContext().getDrawable(R.drawable.com_bg_filter_normal)
                    tv.setTextColor(requireContext().getColor(R.color.primary))
                }
                textView.background = requireContext().getDrawable(R.drawable.com_bg_filter_selected)
                textView.setTextColor(android.graphics.Color.WHITE)
                
                currentFilter = if (textView.text.toString() == "Dành cho bạn") "Tất cả" else textView.text.toString()
                updateFeedData(resetData = true)
            }
        }
    }

    private fun updateFeedData(resetData: Boolean = true) {
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

            val ctx = context ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val productsList = FeedDataCache.productsList ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.veganbeauty.app.data.local.LocalJsonReader(ctx).getProducts() }.also { FeedDataCache.productsList = it }
                val newsList = FeedDataCache.newsList ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.veganbeauty.app.data.local.LocalJsonReader(ctx).getCommunityNews() }.also { FeedDataCache.newsList = it }
                
                if (resetData) {
                    allFilteredPosts = if (currentFilter != "Tất cả") {
                        postsList.filter { 
                            it.type.equals(currentFilter, ignoreCase = true) ||
                            it.skinType.equals(currentFilter, ignoreCase = true) ||
                            it.concern.equals(currentFilter, ignoreCase = true)
                        }.distinctBy { it.postId }.shuffled()  // deduplicate by postId
                    } else {
                        postsList.distinctBy { it.postId }.shuffled()  // deduplicate by postId
                    }
                    
                    if (newsList.isNotEmpty() && currentFilter == "Tất cả") {
                        val randomNews = newsList.random()
                        allFilteredPosts = listOf(randomNews) + allFilteredPosts
                    } else if (newsList.isNotEmpty()) {
                        val filteredNews = newsList.filter {
                            it.type.equals(currentFilter, ignoreCase = true) ||
                            it.skinType.equals(currentFilter, ignoreCase = true) ||
                            it.concern.equals(currentFilter, ignoreCase = true)
                        }
                        if (filteredNews.isNotEmpty()) {
                            val randomNews = filteredNews.random()
                            allFilteredPosts = listOf(randomNews) + allFilteredPosts
                        }
                    }
                    currentPage = 1
                }
                
                val pagedPostsList = allFilteredPosts.take(currentPage * postsPerPage)

                
                val mySocialData = FeedDataCache.mySocialData ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.veganbeauty.app.data.local.LocalJsonReader(ctx).getSocialDataForUser("test_001") }.also { FeedDataCache.mySocialData = it }
                val suggestedIds = mySocialData["suggested"] ?: emptyList()
                val myFriends = (mySocialData["friends"] ?: emptyList()).toSet()
                
                val suggestedUsers = usersList.filter { it.user_id in suggestedIds && it.user_id != "test_001" }.toMutableList()
                if (suggestedUsers.size < 5) {
                    suggestedUsers.addAll(usersList.filter { it.user_id != "test_001" && !myFriends.contains(it.user_id) && !suggestedUsers.contains(it) }.shuffled().take(10 - suggestedUsers.size))
                }
                
                val mutualMap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.veganbeauty.app.data.local.LocalJsonReader(ctx).getMutualFriendsForUsers(myFriends, suggestedUsers.map { it.user_id }) }
                suggestedUsers.forEach { user -> 
                    val mutualIds = mutualMap[user.user_id] ?: emptyList()
                    user.mutualCount = mutualIds.size
                    if (mutualIds.isNotEmpty()) {
                        val mutualUsers = usersList.filter { it.user_id in mutualIds }
                        if (mutualUsers.isNotEmpty()) {
                            user.firstMutualFriendName = mutualUsers[0].full_name.ifBlank { mutualUsers[0].username }
                            user.mutualFriendAvatars = mutualUsers.mapNotNull { it.avatar }.take(3)
                        }
                    }
                }
                
                postAdapter.updateData(pagedPostsList, suggestedUsers, reelsList, productsList)
                isLoadingMore = false
            }
        }
    }

    override fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            viewLifecycleOwner.lifecycleScope.launch {
                val ctx = context ?: return@launch
                val myFriendsIds = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.veganbeauty.app.data.local.LocalJsonReader(ctx).getFriendsForUser("test_001")
                }
                
                var allStories = users.toMutableList()
                // Sort stories so that friends appear first
                allStories = allStories.sortedByDescending { myFriendsIds.contains(it.user_id) }.toMutableList()
                
                if (allStories.isNotEmpty()) {
                    val myStory = allStories[0].copy(username = "Tin của bạn", user_id = "test_001", avatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg")
                    allStories.add(0, myStory)
                }
                storyAdapter.updateData(allStories)
                updateFeedData()
            }
        }

        viewModel.posts.observe(viewLifecycleOwner) { _ -> }

        viewModel.reels.observe(viewLifecycleOwner) { _ -> }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

