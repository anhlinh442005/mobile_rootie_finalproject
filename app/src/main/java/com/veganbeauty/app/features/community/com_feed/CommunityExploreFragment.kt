package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.viewpager2.widget.ViewPager2
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentExploreBinding
import com.veganbeauty.app.features.home.HomeFragment

class CommunityExploreFragment : RootieFragment() {

    private var _binding: ComFragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private val exploreAdapter = ExploreVideoAdapter(emptyList())

    private var isNavVisible = true

    private fun hideBottomNavigation() {
        if (!isNavVisible) return
        isNavVisible = false
        val height = binding.comBottomNav.root.height.toFloat()
        binding.comBottomNav.root.animate()
            .translationY(height)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start()
        
        updateVideoContentTranslation(height)
    }

    private fun showBottomNavigation() {
        if (isNavVisible) return
        isNavVisible = true
        binding.comBottomNav.root.animate()
            .translationY(0f)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start()
            
        updateVideoContentTranslation(0f)
    }

    private fun updateVideoContentTranslation(translationY: Float) {
        exploreAdapter.contentTranslationY = translationY
        val rv = binding.viewPagerExplore.getChildAt(0) as? RecyclerView
        val layoutManager = rv?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        layoutManager?.let {
            val first = it.findFirstVisibleItemPosition()
            val last = it.findLastVisibleItemPosition()
            if (first != -1 && last != -1) {
                for (i in first..last) {
                    val holder = rv.findViewHolderForAdapterPosition(i) as? ExploreVideoAdapter.ExploreVideoViewHolder
                    holder?.animateContent(translationY)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentExploreBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CommunityViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Setup ViewPager2
        binding.viewPagerExplore.adapter = exploreAdapter
        binding.viewPagerExplore.offscreenPageLimit = 1

        binding.viewPagerExplore.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var currentPlayingHolder: ExploreVideoAdapter.ExploreVideoViewHolder? = null
            private var lastPosition = 0

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // Handle nav bar visibility based on scroll direction
                if (position > lastPosition) {
                    hideBottomNavigation()
                } else if (position < lastPosition) {
                    showBottomNavigation()
                }
                lastPosition = position
                
                // Stop previous
                currentPlayingHolder?.stopVideo()
                
                // Play new
                val rv = binding.viewPagerExplore.getChildAt(0) as? RecyclerView
                val holder = rv?.findViewHolderForAdapterPosition(position) as? ExploreVideoAdapter.ExploreVideoViewHolder
                if (holder != null) {
                    holder.playVideo()
                    currentPlayingHolder = holder
                } else {
                    // If holder is not ready yet, delay a bit
                    Handler(Looper.getMainLooper()).postDelayed({
                        val delayedHolder = rv?.findViewHolderForAdapterPosition(position) as? ExploreVideoAdapter.ExploreVideoViewHolder
                        delayedHolder?.playVideo()
                        currentPlayingHolder = delayedHolder
                    }, 200)
                }
            }
        })

        // Setup UI Actions
        binding.ivBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.ivSearch.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, ExploreSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup Bottom Nav Navigation
        binding.comBottomNav.navComFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.comBottomNav.navComProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
                .commit()
        }

        binding.comBottomNav.navComHub.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment())
                .commit()
        }

        binding.comBottomNav.navComChat.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.message.CommunityMessageFragment())
                .commit()
        }

        // Keep Khám phá active visually
        val exploreIcon = binding.comBottomNav.navComExplore.getChildAt(0) as? android.widget.ImageView
        exploreIcon?.setColorFilter(resources.getColor(R.color.primary, null))
        val exploreText = binding.comBottomNav.navComExplore.getChildAt(1) as? android.widget.TextView
        exploreText?.setTextColor(resources.getColor(R.color.primary, null))
        exploreText?.setTypeface(null, android.graphics.Typeface.BOLD)

        val feedIcon = binding.comBottomNav.navComFeed.getChildAt(0) as? android.widget.ImageView
        feedIcon?.setColorFilter(resources.getColor(R.color.tertiary, null))
        val feedText = binding.comBottomNav.navComFeed.getChildAt(1) as? android.widget.TextView
        feedText?.setTextColor(resources.getColor(R.color.tertiary, null))
        feedText?.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    override fun observeViewModel() {
        viewModel.exploreVideos.observe(viewLifecycleOwner) { videos ->
            var finalVideos = videos.shuffled()
            
            val targetId = arguments?.getString("target_video_id")
            if (targetId != null) {
                val targetVideo = finalVideos.find { it.id == targetId }
                if (targetVideo != null) {
                    val listWithoutTarget = finalVideos.filter { it.id != targetId }.toMutableList()
                    listWithoutTarget.add(0, targetVideo)
                    finalVideos = listWithoutTarget
                }
            }

            exploreAdapter.updateData(finalVideos)
            
            // Start playing first video if possible
            if (finalVideos.isNotEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val rv = binding.viewPagerExplore.getChildAt(0) as? RecyclerView
                    val holder = rv?.findViewHolderForAdapterPosition(0) as? ExploreVideoAdapter.ExploreVideoViewHolder
                    holder?.playVideo()
                }, 500)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

