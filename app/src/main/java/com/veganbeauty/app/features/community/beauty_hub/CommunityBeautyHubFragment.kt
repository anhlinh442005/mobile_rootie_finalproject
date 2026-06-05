package com.veganbeauty.app.features.community.beauty_hub

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentBeautyHubBinding
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory
import com.veganbeauty.app.features.home.HomeFragment

class CommunityBeautyHubFragment : RootieFragment() {

    private var _binding: ComFragmentBeautyHubBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private val ingredientAdapter = IngredientAdapter(emptyList())
    private val blogAdapter = LatestKnowledgeAdapter(emptyList())
    private val notebookAdapter = NotebookVideoAdapter(emptyList())

    private var isNavVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentBeautyHubBinding.inflate(inflater, container, false)
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
        binding.rvIngredients.adapter = ingredientAdapter
        binding.rvBlogs.adapter = blogAdapter
        binding.rvNotebooks.adapter = notebookAdapter



        binding.btnExploreFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary)
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 800)
        }

        binding.nsvHub.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 15) {
                hideBottomNavigation()
            } else if (dy < -15) {
                showBottomNavigation()
            }
        })

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.comBottomNav.navComFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.comBottomNav.navComProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.profile.AccountProfileFragment())
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
        
        binding.comBottomNav.navComHub.setOnClickListener {
            binding.nsvHub.smoothScrollTo(0, 0)
        }

        // Keep Beauty Hub active visually
        val hubIcon = binding.comBottomNav.navComHub.getChildAt(0) as? ImageView
        hubIcon?.setColorFilter(resources.getColor(R.color.primary, null))
        val hubText = binding.comBottomNav.navComHub.getChildAt(1) as? TextView
        hubText?.setTextColor(resources.getColor(R.color.primary, null))
        hubText?.setTypeface(null, android.graphics.Typeface.BOLD)

        val feedIcon = binding.comBottomNav.navComFeed.getChildAt(0) as? ImageView
        feedIcon?.setColorFilter(resources.getColor(R.color.tertiary, null))
        val feedText = binding.comBottomNav.navComFeed.getChildAt(1) as? TextView
        feedText?.setTextColor(resources.getColor(R.color.tertiary, null))
        feedText?.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

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

    override fun observeViewModel() {
        viewModel.ingredients.observe(viewLifecycleOwner) { items ->
            if (items != null) {
                ingredientAdapter.updateData(items.take(10))
            }
        }

        viewModel.blogs.observe(viewLifecycleOwner) { items ->
            if (!items.isNullOrEmpty()) {
                val featuredBlog = items[0]
                val remainingBlogs = if (items.size > 1) items.drop(1) else emptyList()
                
                binding.layoutFeaturedBlog.visibility = View.VISIBLE
                binding.tvFeaturedBlogTitle.text = featuredBlog.title
                binding.tvFeaturedBlogDate.text = "${featuredBlog.publishedAt} • ${featuredBlog.shortDescription}"
                binding.ivFeaturedBlog.load(featuredBlog.imageUrl) {
                    crossfade(true)
                }
                
                blogAdapter.updateData(remainingBlogs)
            } else {
                binding.layoutFeaturedBlog.visibility = View.GONE
                blogAdapter.updateData(emptyList())
            }
        }
        
        viewModel.exploreVideos.observe(viewLifecycleOwner) { videos ->
            if (videos != null) {
                val notebooks = videos.filter { it.type.contains("notebook", ignoreCase = true) }
                notebookAdapter.updateData(notebooks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

