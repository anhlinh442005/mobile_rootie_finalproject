package com.veganbeauty.app.features.community.com_feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.databinding.ComItemStoryBinding
import com.veganbeauty.app.databinding.ComItemPostBinding
import com.veganbeauty.app.databinding.ComItemSuggestionBinding
import com.veganbeauty.app.databinding.ComItemReelBinding
import com.veganbeauty.app.databinding.ComItemSuggestedUsersFeedBinding
import com.veganbeauty.app.databinding.ComItemSuggestedReelsFeedBinding

import com.google.android.material.tabs.TabLayoutMediator
import android.widget.ImageView
import coil.decode.SvgDecoder
import kotlinx.coroutines.launch

sealed class CommunityFeedItem {
    data class Post(val post: CommunityPostEntity) : CommunityFeedItem()
    data class SuggestedUsers(val users: List<UserEntity>) : CommunityFeedItem()
    data class SuggestedReels(val reels: List<ReelEntity>) : CommunityFeedItem()
}

class ImageSliderAdapter(var imageUrls: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
        }
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = imageUrls[position]
        try {
            if (url.startsWith("content://") || url.startsWith("file://")) {
                // Local URI - use Coil to handle gracefully and avoid SecurityException on expired URIs
                holder.imageView.load(android.net.Uri.parse(url)) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                    error(android.R.color.darker_gray)
                }
            } else {
                holder.imageView.load(url) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                    error(android.R.color.darker_gray)
                }
            }
        } catch (e: Exception) {
            holder.imageView.setBackgroundColor(android.graphics.Color.DKGRAY)
        }
    }

    override fun getItemCount() = imageUrls.size

    fun updateData(newUrls: List<String>) {
        imageUrls = newUrls
        notifyDataSetChanged()
    }
}

class StoryAdapter(private var stories: List<UserEntity>) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(val binding: ComItemStoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ComItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.binding.tvUsername.text = story.username
        
        if (position == 0) {
            holder.binding.ivAdd.visibility = View.VISIBLE
        } else {
            holder.binding.ivAdd.visibility = View.GONE
        }

        // Load avatar using Coil
        if (!story.avatar.isNullOrEmpty()) {
            holder.binding.ivAvatar.load(story.avatar) {
                decoderFactory(SvgDecoder.Factory())
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(R.drawable.logo)
            }
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray)
        }
    }

    override fun getItemCount() = stories.size

    fun updateData(newStories: List<UserEntity>) {
        stories = newStories
        notifyDataSetChanged()
    }
}

class PostAdapter(
    private var items: List<CommunityFeedItem> = emptyList(),
    private var globalProducts: List<com.veganbeauty.app.data.local.entities.CommunityProduct> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_SUGGESTED_USERS = 1
        private const val VIEW_TYPE_SUGGESTED_REELS = 2
    }

    class PostViewHolder(val binding: ComItemPostBinding) : RecyclerView.ViewHolder(binding.root)
    class SuggestedUsersViewHolder(val binding: ComItemSuggestedUsersFeedBinding) : RecyclerView.ViewHolder(binding.root)
    class SuggestedReelsViewHolder(val binding: ComItemSuggestedReelsFeedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CommunityFeedItem.Post -> VIEW_TYPE_POST
            is CommunityFeedItem.SuggestedUsers -> VIEW_TYPE_SUGGESTED_USERS
            is CommunityFeedItem.SuggestedReels -> VIEW_TYPE_SUGGESTED_REELS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SUGGESTED_USERS -> {
                val binding = ComItemSuggestedUsersFeedBinding.inflate(inflater, parent, false)
                SuggestedUsersViewHolder(binding)
            }
            VIEW_TYPE_SUGGESTED_REELS -> {
                val binding = ComItemSuggestedReelsFeedBinding.inflate(inflater, parent, false)
                SuggestedReelsViewHolder(binding)
            }
            else -> {
                val binding = ComItemPostBinding.inflate(inflater, parent, false)
                PostViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
                is CommunityFeedItem.Post -> {
                val postHolder = holder as PostViewHolder
                val post = item.post
                postHolder.binding.tvAuthorName.text = post.authorDisplayName.ifEmpty { post.authorUsername }
                
                // Add verified icon for rootie
                if (post.authorId == "rootie_official" || post.authorId == "rootie_vn") {
                    val verifiedIcon = androidx.core.content.ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_verified)
                    verifiedIcon?.setBounds(0, 0, 36, 36) // Resize to match text
                    postHolder.binding.tvAuthorName.setCompoundDrawables(null, null, verifiedIcon, null)
                    postHolder.binding.tvAuthorName.compoundDrawablePadding = 8
                    
                    // Format date for official post
                    val timestamp = post.createdAt.toLongOrNull() ?: 0L
                    var dateStr = "11 thg 3"
                    if (timestamp > 0) {
                        val sdf = java.text.SimpleDateFormat("d 'thg' M", java.util.Locale("vi"))
                        dateStr = sdf.format(java.util.Date(timestamp * 1000))
                    }
                    val text = "$dateStr •  "
                    val spannable = android.text.SpannableStringBuilder(text)
                    val publicIcon = androidx.core.content.ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_public)
                    publicIcon?.setBounds(0, 0, 36, 36)
                    if (publicIcon != null) {
                        val imageSpan = android.text.style.ImageSpan(publicIcon, android.text.style.ImageSpan.ALIGN_BASELINE)
                        spannable.setSpan(imageSpan, text.length - 1, text.length, android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                    }
                    postHolder.binding.tvSkinType.text = spannable
                    postHolder.binding.tvCreatedAt.visibility = View.GONE
                } else {
                    postHolder.binding.tvAuthorName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    val tagList = mutableListOf<String>()
                    post.skinType?.takeIf { it.isNotBlank() && it != "Không xác định" }?.let { tagList.add(it) }
                    post.concern?.takeIf { it.isNotBlank() && it != "Khác" && it != "Chung" }?.let { tagList.add(it) }
                    post.type?.takeIf { it.isNotBlank() }?.let { tagList.add(it) }
                    postHolder.binding.tvSkinType.text = tagList.joinToString(" • ")
                    
                    postHolder.binding.tvCreatedAt.visibility = View.VISIBLE
                    postHolder.binding.tvCreatedAt.text = com.veganbeauty.app.utils.TimeFormatter.getTimeAgo(post.createdAt)
                }
                
                postHolder.binding.tvLikes.text = post.likesCount.toString()
                postHolder.binding.tvComments.text = post.commentsCount.toString()
                postHolder.binding.tvContent.text = post.content
                
                // Load author avatar using Coil
                if (!post.authorAvatarUrl.isNullOrEmpty()) {
                    postHolder.binding.ivAuthorAvatar.load(post.authorAvatarUrl) {
                        decoderFactory(SvgDecoder.Factory())
                        crossfade(true)
                        placeholder(android.R.color.darker_gray)
                        error(R.drawable.img_avatar)
                    }
                } else {
                    if (post.authorId == "rootie_official" || post.authorId == "rootie_vn") {
                        postHolder.binding.ivAuthorAvatar.setImageResource(R.drawable.imv_logo)
                    } else {
                        postHolder.binding.ivAuthorAvatar.setImageResource(android.R.color.darker_gray)
                    }
                }

                // Linked Products
                val linkedIds = post.linkedProductIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                if (linkedIds.isNotEmpty() && globalProducts.isNotEmpty()) {
                    val matchingProducts = globalProducts.filter { linkedIds.contains(it.id) }
                    if (matchingProducts.isNotEmpty()) {
                        postHolder.binding.llUsedProducts.visibility = View.VISIBLE
                        postHolder.binding.rvLinkedProducts.adapter = PostLinkedProductAdapter(matchingProducts, post.postId, post.authorId)
                    } else {
                        postHolder.binding.llUsedProducts.visibility = View.GONE
                    }
                } else {
                    postHolder.binding.llUsedProducts.visibility = View.GONE
                }

                // Load post images slider (ViewPager2)
                val urls = post.mediaUrlsString.split(",").filter { it.isNotBlank() }
                val rootLayout = postHolder.binding.root
                val contentContainer = postHolder.binding.llContentContainer
                val currentIdx = rootLayout.indexOfChild(contentContainer)

                if (urls.isNotEmpty()) {
                    var sliderAdapter = postHolder.binding.vpPostImages.adapter as? ImageSliderAdapter
                    if (sliderAdapter == null) {
                        sliderAdapter = ImageSliderAdapter(urls)
                        postHolder.binding.vpPostImages.adapter = sliderAdapter
                        TabLayoutMediator(postHolder.binding.tabIndicator, postHolder.binding.vpPostImages) { _, _ -> }.attach()
                    } else {
                        if (sliderAdapter.imageUrls != urls) {
                            sliderAdapter.updateData(urls)
                        }
                    }
                    
                    // If only 1 image, hide dots indicator to look clean
                    if (urls.size <= 1) {
                        postHolder.binding.tabIndicator.visibility = View.GONE
                    } else {
                        postHolder.binding.tabIndicator.visibility = View.VISIBLE
                    }
                    postHolder.binding.flPostImagesContainer.visibility = View.VISIBLE
                    
                    val expectedIdx = rootLayout.childCount - 1
                    if (currentIdx != expectedIdx) {
                        rootLayout.removeView(contentContainer)
                        rootLayout.addView(contentContainer)
                    }
                } else {
                    postHolder.binding.flPostImagesContainer.visibility = View.GONE
                    postHolder.binding.tabIndicator.visibility = View.GONE
                    if (currentIdx != 1) {
                        rootLayout.removeView(contentContainer)
                        rootLayout.addView(contentContainer, 1)
                    }
                }

                // Like toggle micro-interaction
                val sharedPrefs = postHolder.itemView.context.getSharedPreferences("rootie_prefs", android.content.Context.MODE_PRIVATE)
                var isLiked = sharedPrefs.getBoolean("liked_${post.postId}", false)
                var currentLikesCount = post.likesCount
                
                if (isLiked) {
                    postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                } else {
                    postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                }

                postHolder.binding.ivLike.setOnClickListener {
                    isLiked = !isLiked
                    if (isLiked) {
                        postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                        currentLikesCount++
                        postHolder.binding.tvLikes.text = currentLikesCount.toString()
                        sharedPrefs.edit().putBoolean("liked_${post.postId}", true).apply()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            com.veganbeauty.app.data.local.RootieDatabase.getDatabase(holder.itemView.context).communityDao().incrementLikesCount(post.postId)
                        }
                    } else {
                        postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                        currentLikesCount = maxOf(0, currentLikesCount - 1)
                        postHolder.binding.tvLikes.text = currentLikesCount.toString()
                        sharedPrefs.edit().putBoolean("liked_${post.postId}", false).apply()
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            com.veganbeauty.app.data.local.RootieDatabase.getDatabase(holder.itemView.context).communityDao().decrementLikesCount(post.postId)
                        }
                    }
                }

                postHolder.binding.tvComments.text = post.commentsCount.toString()
                postHolder.binding.tvReups.text = post.reupsCount.toString()

                postHolder.binding.ivComment.setOnClickListener {
                    val context = it.context
                    if (context is androidx.fragment.app.FragmentActivity) {
                        val bottomSheet = CommunityCommentBottomSheet.newInstance(post.postId, post.commentsCount)
                        bottomSheet.show(context.supportFragmentManager, CommunityCommentBottomSheet.TAG)
                    }
                }

                // Profile Click listener
                val onProfileClick = View.OnClickListener {
                    val context = it.context
                    if (context is androidx.fragment.app.FragmentActivity) {
                        if (post.authorId == "rootie_official" || post.authorId == "rootie_vn" || post.authorDisplayName.contains("Rootie", ignoreCase = true)) {
                            val newsFragment = com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment()
                            context.supportFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.main_container, newsFragment)
                                .addToBackStack(null)
                                .commit()
                        } else {
                            val profileFragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment().apply {
                                arguments = android.os.Bundle().apply {
                                    putString("USER_ID", post.authorId)
                                    putString("AVATAR_URL", post.authorAvatarUrl)
                                    putString("USER_NAME", post.authorDisplayName)
                                }
                            }
                            context.supportFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.main_container, profileFragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
                // Helper to get own user id
                fun getOwnUserId(context: android.content.Context): String {
                    var ownId = "test_001"
                    try {
                        val loggedInEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(context)
                        val usersJsonStr = context.assets.open("users.json").bufferedReader().use { it.readText() }
                        val usersJsonArray = org.json.JSONArray(usersJsonStr)
                        for (i in 0 until usersJsonArray.length()) {
                            val obj = usersJsonArray.getJSONObject(i)
                            if (obj.optString("email") == loggedInEmail) {
                                ownId = obj.optString("user_id", "test_001")
                                break
                            }
                        }
                    } catch(e: Exception) {}
                    return ownId
                }

                // Reup toggle and action
                val context = postHolder.itemView.context
                val ownUserId = getOwnUserId(context)
                
                // Author logic: hide follow button for own posts, configure more options
                if (post.authorId == ownUserId) {
                    postHolder.binding.tvFollow.visibility = View.GONE
                    postHolder.binding.ivMore.setOnClickListener { v ->
                        val popupView = LayoutInflater.from(context).inflate(R.layout.com_popup_more_author, null)
                        val popupWindow = android.widget.PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                        )
                        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                        popupWindow.elevation = 8f
                        popupView.findViewById<View>(R.id.tvEdit).setOnClickListener { popupWindow.dismiss() }
                        popupView.findViewById<View>(R.id.tvDelete).setOnClickListener { popupWindow.dismiss() }
                        popupView.findViewById<View>(R.id.tvPrivacy).setOnClickListener { popupWindow.dismiss() }
                        val xOffset = -(160 * context.resources.displayMetrics.density).toInt() + v.width
                        popupWindow.showAsDropDown(v, xOffset, 0)
                    }
                } else {
                    postHolder.binding.tvFollow.visibility = View.VISIBLE
                    postHolder.binding.ivMore.setOnClickListener { v ->
                        val popupView = LayoutInflater.from(context).inflate(R.layout.com_popup_more_other, null)
                        val popupWindow = android.widget.PopupWindow(
                            popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            true
                        )
                        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                        popupWindow.elevation = 8f
                        popupView.findViewById<View>(R.id.tvReport).setOnClickListener { popupWindow.dismiss() }
                        popupView.findViewById<View>(R.id.tvHide).setOnClickListener { popupWindow.dismiss() }
                        val xOffset = -(160 * context.resources.displayMetrics.density).toInt() + v.width
                        popupWindow.showAsDropDown(v, xOffset, 0)
                    }
                }
                var isReuped = com.veganbeauty.app.features.community.UserMemoryHelper.isPostReposted(context, ownUserId, post.postId)
                var currentReupsCount = post.reupsCount
                
                if (isReuped) {
                    postHolder.binding.ivReup.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    postHolder.binding.ivReup.clearColorFilter()
                }

                fun navigateToMyProfile(tabIndex: Int) {
                    if (context is androidx.fragment.app.FragmentActivity) {
                        val profileFragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment().apply {
                            arguments = android.os.Bundle().apply {
                                putInt("SELECTED_TAB", tabIndex)
                            }
                        }
                        context.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, profileFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }

                postHolder.binding.ivReup.setOnClickListener {
                    isReuped = com.veganbeauty.app.features.community.UserMemoryHelper.toggleRepost(context, ownUserId, post.postId)
                    if (isReuped) {
                        postHolder.binding.ivReup.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
                        currentReupsCount++
                    } else {
                        postHolder.binding.ivReup.clearColorFilter()
                        currentReupsCount = maxOf(0, currentReupsCount - 1)
                    }
                    postHolder.binding.tvReups.text = currentReupsCount.toString()
                }

                // Bookmark toggle and action
                var isBookmarked = com.veganbeauty.app.features.community.UserMemoryHelper.isPostSaved(context, ownUserId, post.postId)
                if (isBookmarked) {
                    postHolder.binding.ivBookmark.setImageResource(R.drawable.ic_bookmark)
                    postHolder.binding.ivBookmark.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
                } else {
                    postHolder.binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                    postHolder.binding.ivBookmark.clearColorFilter()
                }

                postHolder.binding.ivBookmark.setOnClickListener {
                    isBookmarked = com.veganbeauty.app.features.community.UserMemoryHelper.toggleSave(context, ownUserId, post.postId)
                    if (isBookmarked) {
                        postHolder.binding.ivBookmark.setImageResource(R.drawable.ic_bookmark)
                        postHolder.binding.ivBookmark.setColorFilter(android.graphics.Color.parseColor("#FFC107"))
                    } else {
                        postHolder.binding.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                        postHolder.binding.ivBookmark.clearColorFilter()
                    }
                }

                postHolder.binding.tvAuthorName.setOnClickListener(onProfileClick)
                postHolder.binding.ivAuthorAvatar.setOnClickListener(onProfileClick)
            }
            is CommunityFeedItem.SuggestedUsers -> {
                val usersHolder = holder as SuggestedUsersViewHolder
                val suggestionAdapter = SuggestionAdapter(item.users)
                usersHolder.binding.rvSuggestions.adapter = suggestionAdapter

                usersHolder.binding.tvSeeAllSuggestions.setOnClickListener {
                    val context = it.context
                    if (context is androidx.fragment.app.FragmentActivity) {
                        context.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, CommunityDiscoverPeopleFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            is CommunityFeedItem.SuggestedReels -> {
                val reelsHolder = holder as SuggestedReelsViewHolder
                val reelAdapter = ReelAdapter(item.reels)
                reelsHolder.binding.rvSuggestedReels.adapter = reelAdapter
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(
        newPosts: List<CommunityPostEntity>,
        newUsers: List<UserEntity>,
        newReels: List<ReelEntity>,
        newProducts: List<com.veganbeauty.app.data.local.entities.CommunityProduct> = emptyList()
    ) {
        if (newProducts.isNotEmpty()) {
            this.globalProducts = newProducts
        }
        val mergedItems = mutableListOf<CommunityFeedItem>()
        
        for (i in newPosts.indices) {
            mergedItems.add(CommunityFeedItem.Post(newPosts[i]))
            
            // Intersperse Suggested Users list after the 5th post (index 4)
            if (i == 4 && newUsers.isNotEmpty()) {
                mergedItems.add(CommunityFeedItem.SuggestedUsers(newUsers))
            }
            
            // Intersperse Suggested Reels list after the 9th post (index 8)
            if (i == 8 && newReels.isNotEmpty()) {
                mergedItems.add(CommunityFeedItem.SuggestedReels(newReels))
            }
        }
        
        // Safe fallbacks if lists are shorter
        if (newPosts.size <= 4) {
            if (newUsers.isNotEmpty()) mergedItems.add(CommunityFeedItem.SuggestedUsers(newUsers))
            if (newReels.isNotEmpty()) mergedItems.add(CommunityFeedItem.SuggestedReels(newReels))
        } else if (newPosts.size <= 8) {
            if (newReels.isNotEmpty()) mergedItems.add(CommunityFeedItem.SuggestedReels(newReels))
        }

        this.items = mergedItems
        notifyDataSetChanged()
    }
}

class SuggestionAdapter(private var users: List<UserEntity>) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(val binding: ComItemSuggestionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ComItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvUsername.text = user.username
        
        if (!user.avatar.isNullOrEmpty()) {
            holder.binding.ivAvatar.load(user.avatar) {
                decoderFactory(SvgDecoder.Factory())
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(R.drawable.logo)
            }
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray)
        }
        
        if (user.mutualCount > 0) {
            val friendName = user.firstMutualFriendName ?: "Ai đó"
            if (user.mutualCount == 1) {
                holder.binding.tvMutualCount.text = "Có $friendName đang theo dõi"
            } else {
                holder.binding.tvMutualCount.text = "Có $friendName và ${user.mutualCount - 1} người khác đang theo dõi"
            }
            holder.binding.llMutualInfo.visibility = View.VISIBLE
            
            // Show avatars if available
            val avatars = user.mutualFriendAvatars
            if (avatars.isNotEmpty()) {
                holder.binding.flMutualAvatars.visibility = View.VISIBLE
                
                // Show up to 3 avatars
                if (avatars.size >= 1) {
                    holder.binding.cvMutual1.visibility = View.VISIBLE
                    holder.binding.ivMutual1.load(avatars[0]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual1.visibility = View.GONE
                
                if (avatars.size >= 2) {
                    holder.binding.cvMutual2.visibility = View.VISIBLE
                    holder.binding.ivMutual2.load(avatars[1]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual2.visibility = View.GONE
                
                if (avatars.size >= 3) {
                    holder.binding.cvMutual3.visibility = View.VISIBLE
                    holder.binding.ivMutual3.load(avatars[2]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual3.visibility = View.GONE
                
            } else {
                holder.binding.flMutualAvatars.visibility = View.GONE
            }
        } else {
            holder.binding.llMutualInfo.visibility = View.GONE
            holder.binding.flMutualAvatars.visibility = View.GONE
        }
        
        holder.binding.root.setOnClickListener {
            val context = it.context
            if (context is androidx.fragment.app.FragmentActivity) {
                if (user.user_id == "rootie_official" || user.user_id == "rootie_vn" || user.username.contains("Rootie", ignoreCase = true)) {
                    val newsFragment = com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment()
                    context.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, newsFragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    val profileFragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment().apply {
                        arguments = android.os.Bundle().apply {
                            putString("USER_ID", user.user_id)
                            putString("AVATAR_URL", user.avatar)
                            putString("USER_NAME", user.username)
                        }
                    }
                    context.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, profileFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserEntity>) {
        users = newUsers
        notifyDataSetChanged()
    }
}

class ReelAdapter(private var reels: List<ReelEntity>, private val isGrid: Boolean = false) : RecyclerView.Adapter<ReelAdapter.ReelViewHolder>() {

    class ReelViewHolder(val binding: ComItemReelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val binding = ComItemReelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (isGrid) {
            val lp = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            // Use 1dp (or small px value) for the margins so that the gap is 2dp between items.
            val marginInPx = (1 * parent.context.resources.displayMetrics.density).toInt()
            lp.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            lp.marginStart = marginInPx
            lp.marginEnd = marginInPx
            binding.root.layoutParams = lp
        }
        return ReelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        val reel = reels[position]
        if (reel.thumbnailUrl.isNotEmpty()) {
            holder.binding.ivThumbnail.load(reel.thumbnailUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        } else {
            holder.binding.ivThumbnail.setImageResource(android.R.color.darker_gray)
        }

        // Bấm vào thước phim thì hiện lên giao diện để xem reels TikTok-style (hỗ trợ lướt)
        holder.binding.root.setOnClickListener {
            val context = holder.binding.root.context
            if (context is androidx.fragment.app.FragmentActivity) {
                val dialog = ReelPlayerDialog(reels, holder.bindingAdapterPosition)
                dialog.show(context.supportFragmentManager, "ReelPlayerDialog")
            }
        }
    }

    override fun getItemCount() = reels.size

    fun updateData(newReels: List<ReelEntity>) {
        reels = newReels
        notifyDataSetChanged()
    }
}
