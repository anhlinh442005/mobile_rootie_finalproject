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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommunityBeautyHubFragment : RootieFragment() {

    private var _binding: ComFragmentBeautyHubBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private val ingredientAdapter = IngredientAdapter(emptyList())
    private val blogAdapter = LatestKnowledgeAdapter(emptyList())
    private val notebookAdapter = NotebookVideoAdapter(emptyList())

    private var isNavVisible = true
    
    // Lazy blog loading state
    private val blogsPerPage = 10
    private var blogCurrentOffset = 0
    private var isLoadingMoreBlogs = false
    private var hasMoreBlogs = true
    private val loadedBlogs = mutableListOf<com.veganbeauty.app.data.local.entities.CommunityBlogEntity>()

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
        binding.llShortcutNews.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityNewsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.llShortcutBlog.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.blog.CommunityBlogFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.llShortcutIngredient.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, IngredientFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.root.findViewById<android.view.View>(R.id.llShortcutHandbook)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, HandbookFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnExploreFeed.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary)
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Reset lazy load state và reload từ đầu
            loadedBlogs.clear()
            blogCurrentOffset = 0
            hasMoreBlogs = true
            isLoadingMoreBlogs = false
            viewModel.refreshData()
            loadNextBlogPage()
            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 800)
        }

        binding.nsvHub.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 15) {
                hideBottomNavigation()
            } else if (dy < -15) {
                showBottomNavigation()
            }
            
            // Lazy load blog khi scroll gần cuối
            if (!v.canScrollVertically(1) && hasMoreBlogs && !isLoadingMoreBlogs) {
                loadNextBlogPage()
            }
        })

        setupBottomNavigation()
        
        // Load blog page đầu tiên
        loadNextBlogPage()
    }
    
    private fun loadNextBlogPage() {
        if (isLoadingMoreBlogs || !hasMoreBlogs) return
        isLoadingMoreBlogs = true
        val ctx = context ?: return
        
        lifecycleScope.launch {
            val newBlogs = withContext(Dispatchers.IO) {
                try {
                    LocalJsonReader(ctx).getCommunityBlogs(limit = blogsPerPage, offset = blogCurrentOffset)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            
            if (newBlogs.isEmpty()) {
                hasMoreBlogs = false
            } else {
                blogCurrentOffset += newBlogs.size
                loadedBlogs.addAll(newBlogs)
                
                if (loadedBlogs.isNotEmpty()) {
                    val featuredBlog = loadedBlogs[0]
                    val remainingBlogs = loadedBlogs.drop(1)
                    
                    binding.layoutFeaturedBlog.visibility = View.VISIBLE
                    binding.tvFeaturedBlogTitle.text = featuredBlog.title
                    binding.tvFeaturedBlogDate.text = "${featuredBlog.publishedAt} • ${featuredBlog.shortDescription}"
                    binding.ivFeaturedBlog.load(featuredBlog.imageUrl) {
                        crossfade(true)
                    }
                    blogAdapter.updateData(remainingBlogs)
                }
                
                // Nếu trả về ít hơn limit, tức là đã hết dữ liệu
                if (newBlogs.size < blogsPerPage) {
                    hasMoreBlogs = false
                }
            }
            isLoadingMoreBlogs = false
        }
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
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
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

        // Blog không dùng ViewModel nữa, load trực tiếp qua loadNextBlogPage()
        
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


