package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.YtVideoEntity

class ExploreSearchFragment : RootieFragment() {

    private lateinit var rvGrid: RecyclerView
    private lateinit var ivBack: ImageView
    private lateinit var etSearch: android.widget.EditText
    private val videos = mutableListOf<YtVideoEntity>()
    private lateinit var adapter: ExploreGridAdapter
    private lateinit var llResults: View
    private lateinit var svSuggestions: View
    private lateinit var tvSearchBtn: TextView
    private lateinit var llTrendingKeywords: android.widget.LinearLayout
    private var allExploreVideos: List<YtVideoEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_explore_search, container, false)
        rvGrid = view.findViewById(R.id.rvExploreGrid)
        ivBack = view.findViewById(R.id.ivBack)
        etSearch = view.findViewById(R.id.etSearch)
        llResults = view.findViewById(R.id.llResults)
        svSuggestions = view.findViewById(R.id.svSuggestions)
        tvSearchBtn = view.findViewById(R.id.tvSearchBtn)
        llTrendingKeywords = view.findViewById(R.id.llTrendingKeywords)
        return view
    }

    override fun setupUI(view: View) {
        ivBack.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, CommunityExploreFragment())
                    .commit()
            }
        }

        // Load Data
        val reader = LocalJsonReader(requireContext())
        allExploreVideos = reader.getExploreVideos().filter { 
            val t = it.type.lowercase()
            !t.contains("notebook") && !t.contains("cẩm nang")
        }

        // Setup Grid
        adapter = ExploreGridAdapter(videos) { video ->
            val fragment = CommunityExploreFragment()
            val args = Bundle()
            args.putString("target_video_id", video.id)
            fragment.arguments = args
            
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .commit()
        }
        rvGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvGrid.adapter = adapter

        // Setup Trending Keywords
        setupTrendingKeywords()

        // Search Action
        tvSearchBtn.setOnClickListener {
            performSearch(etSearch.text.toString())
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.text.toString())
                true
            } else false
        }

        // Setup Bottom Nav
        val bottomNav = view.findViewById<View>(R.id.comBottomNav)
        setupBottomNav(bottomNav)
    }

    private fun setupTrendingKeywords() {
        val keywordsMap = mutableMapOf<String, Int>()
        allExploreVideos.forEach { video ->
            val tags = video.keywords.split(",") + video.hashtags.split(Regex("\\s+|,|#"))
            tags.forEach { kw ->
                val cleaned = kw.replace("#", "").trim().lowercase()
                if (cleaned.isNotBlank()) {
                    keywordsMap[cleaned] = keywordsMap.getOrDefault(cleaned, 0) + 1
                }
            }
        }
        val topKeywords = keywordsMap.entries.sortedByDescending { it.value }.take(10).map { it.key }
        
        llTrendingKeywords.removeAllViews()
        topKeywords.forEach { kw ->
            val tv = TextView(requireContext())
            tv.text = "• $kw"
            tv.textSize = 14f
            tv.setTextColor(android.graphics.Color.parseColor("#3E4D44"))
            tv.setPadding(0, 24, 0, 24)
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.setOnClickListener {
                etSearch.setText(kw)
                etSearch.setSelection(kw.length)
                performSearch(kw)
            }
            llTrendingKeywords.addView(tv)
        }
    }

    private fun performSearch(query: String) {
        val q = query.lowercase().trim()
        if (q.isBlank()) return
        
        // Hide keyboard
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

        // Filter
        val filtered = allExploreVideos.filter {
            it.title.lowercase().contains(q) || 
            it.keywords.lowercase().contains(q) ||
            it.hashtags.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }

        videos.clear()
        videos.addAll(filtered)
        adapter.notifyDataSetChanged()

        svSuggestions.visibility = View.GONE
        llResults.visibility = View.VISIBLE
    }

    private fun setupBottomNav(bottomNav: View) {
        bottomNav.findViewById<View>(R.id.nav_com_feed).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityFeedFragment())
                .commit()
        }
        bottomNav.findViewById<View>(R.id.nav_com_profile).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
                .commit()
        }
        bottomNav.findViewById<View>(R.id.nav_com_hub).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment())
                .commit()
        }
        bottomNav.findViewById<View>(R.id.nav_com_chat).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.community.message.CommunityMessageFragment())
                .commit()
        }
        
        // Highlight Explore
        val exploreNav = bottomNav.findViewById<android.widget.LinearLayout>(R.id.nav_com_explore)
        val exploreIcon = exploreNav.getChildAt(0) as? ImageView
        exploreIcon?.setColorFilter(resources.getColor(R.color.primary, null))
        val exploreText = exploreNav.getChildAt(1) as? TextView
        exploreText?.setTextColor(resources.getColor(R.color.primary, null))
        exploreText?.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    override fun observeViewModel() {}

    inner class ExploreGridAdapter(
        private val list: List<YtVideoEntity>,
        private val onClick: (YtVideoEntity) -> Unit
    ) : RecyclerView.Adapter<ExploreGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            val tvUsername: TextView = view.findViewById(R.id.tvUsername)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvLikes: TextView = view.findViewById(R.id.tvLikes)

            init {
                view.setOnClickListener { onClick(list[adapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.com_item_explore_grid_video, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val video = list[position]
            holder.tvTitle.text = video.title
            holder.tvUsername.text = if (video.username.isNotBlank()) video.username else "Cộng đồng Rootie"
            holder.tvDate.text = "18 tháng 3 2025" // Mock
            holder.tvLikes.text = "${video.likesCount / 1000}.${(video.likesCount % 1000) / 100}k"
            
            // Thumbnail
            val videoId = extractYouTubeVideoId(video.url)
            val thumbUrl = if (video.url.contains("cloudinary", ignoreCase = true) && video.url.endsWith(".mp4", ignoreCase = true)) {
                video.url.replace(".mp4", ".jpg", ignoreCase = true)
            } else if (videoId != null) {
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            } else {
                video.url
            }
            
            holder.ivThumbnail.load(thumbUrl) {
                crossfade(true)
            }
                
            // Avatar
            if (video.avatarUrl.isNullOrBlank()) {
                val avatarRes = listOf<Int>(
                    R.drawable.img_avatar,
                    R.drawable.ic_user_outline,
                    R.drawable.img_avatar,
                    R.drawable.ic_user_outline
                ).random()
                holder.ivAvatar.load(avatarRes) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                holder.ivAvatar.load(video.avatarUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }

        override fun getItemCount() = list.size
        
        private fun extractYouTubeVideoId(url: String): String? {
            val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
            val compiledPattern = java.util.regex.Pattern.compile(pattern)
            val matcher = compiledPattern.matcher(url)
            return if (matcher.find()) matcher.group() else null
        }
    }
}

