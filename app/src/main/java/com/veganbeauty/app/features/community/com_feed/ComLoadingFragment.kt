package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ComLoadingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ivMascotLoading = view.findViewById<android.widget.ImageView>(R.id.ivMascotLoading)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressBar)

        view.post {
            val animator = android.animation.ValueAnimator.ofInt(0, 100)
            animator.duration = 1500
            animator.addUpdateListener { animation ->
                val progress = animation.animatedValue as Int
                progressBar.progress = progress
                
                val width = progressBar.width - ivMascotLoading.width
                if (width > 0) {
                    ivMascotLoading.translationX = (width * progress / 100f)
                }
            }
            animator.start()
        }

        val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(requireContext())
        val repository = com.veganbeauty.app.data.repository.CommunityRepository(
            db.communityDao(),
            com.veganbeauty.app.data.local.LocalJsonReader(requireContext()),
            com.veganbeauty.app.data.remote.FirestoreService()
        )
        val factory = com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory(repository)
        val viewModel = androidx.lifecycle.ViewModelProvider(requireActivity(), factory)[com.veganbeauty.app.features.community.com_feed.CommunityViewModel::class.java]

        var isDataLoaded = false
        
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            if (!posts.isNullOrEmpty()) {
                isDataLoaded = true
            }
        }

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val maxWaitMs = 8000L // Timeout 8 giây để tránh treo vô hạn
            
            // Chờ dữ liệu DB load xong, nhưng không quá maxWaitMs
            while (!isDataLoaded && (System.currentTimeMillis() - startTime) < maxWaitMs) {
                kotlinx.coroutines.delay(100)
            }
            
            // Preload heavy JSON data trên IO thread
            val ctx = context
            if (ctx != null) {
                try {
                    val reader = com.veganbeauty.app.data.local.LocalJsonReader(ctx)
                    withContext(Dispatchers.IO) {
                        FeedDataCache.productsList = reader.getProducts()
                        FeedDataCache.newsList = reader.getCommunityNews()
                        FeedDataCache.mySocialData = reader.getSocialDataForUser("test_001")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Đảm bảo animation chạy đủ 2.5 giây
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2500) {
                kotlinx.coroutines.delay(2500 - elapsed)
            }
            
            if (isAdded && !isDetached) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, CommunityFeedFragment())
                    .commitAllowingStateLoss()
            }
        }
    }
}
