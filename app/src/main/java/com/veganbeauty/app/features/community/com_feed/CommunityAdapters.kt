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

sealed class CommunityFeedItem {
    data class Post(val post: CommunityPostEntity) : CommunityFeedItem()
    data class SuggestedUsers(val users: List<UserEntity>) : CommunityFeedItem()
    data class SuggestedReels(val reels: List<ReelEntity>) : CommunityFeedItem()
}

class ImageSliderAdapter(private val imageUrls: List<String>) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

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
        holder.imageView.load(url) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }
    }

    override fun getItemCount() = imageUrls.size
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
        if (!story.avatarUrl.isNullOrEmpty()) {
            holder.binding.ivAvatar.load(story.avatarUrl) {
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
                postHolder.binding.tvAuthorName.text = post.authorUsername
                val tagList = mutableListOf<String>()
                post.skinType?.takeIf { it.isNotBlank() && it != "Không xác định" }?.let { tagList.add("✨ $it") }
                post.concern?.takeIf { it.isNotBlank() && it != "Khác" && it != "Chung" }?.let { tagList.add("🌿 $it") }
                post.type?.takeIf { it.isNotBlank() }?.let { tagList.add("🌿 $it") }
                postHolder.binding.tvSkinType.text = tagList.joinToString(" • ")
                
                postHolder.binding.tvCreatedAt.text = com.veganbeauty.app.utils.TimeFormatter.getTimeAgo(post.createdAt)
                
                postHolder.binding.tvLikes.text = post.likesCount.toString()
                postHolder.binding.tvComments.text = post.commentsCount.toString()
                postHolder.binding.tvContent.text = post.content
                
                // Load author avatar using Coil
                if (!post.authorAvatarUrl.isNullOrEmpty()) {
                    postHolder.binding.ivAuthorAvatar.load(post.authorAvatarUrl) {
                        decoderFactory(SvgDecoder.Factory())
                        crossfade(true)
                        placeholder(android.R.color.darker_gray)
                        error(R.drawable.logo)
                    }
                } else {
                    postHolder.binding.ivAuthorAvatar.setImageResource(android.R.color.darker_gray)
                }

                // Linked Products
                val linkedIds = post.linkedProductIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                if (linkedIds.isNotEmpty() && globalProducts.isNotEmpty()) {
                    val matchingProducts = globalProducts.filter { linkedIds.contains(it.id) }
                    if (matchingProducts.isNotEmpty()) {
                        postHolder.binding.llUsedProducts.visibility = View.VISIBLE
                        postHolder.binding.rvLinkedProducts.adapter = PostLinkedProductAdapter(matchingProducts)
                    } else {
                        postHolder.binding.llUsedProducts.visibility = View.GONE
                    }
                } else {
                    postHolder.binding.llUsedProducts.visibility = View.GONE
                }

                // Load post images slider (ViewPager2)
                val urls = post.mediaUrlsString.split(",").filter { it.isNotBlank() }
                val rootLayout = postHolder.binding.root
                rootLayout.removeView(postHolder.binding.llContentContainer)

                if (urls.isNotEmpty()) {
                    val sliderAdapter = ImageSliderAdapter(urls)
                    postHolder.binding.vpPostImages.adapter = sliderAdapter
                    
                    // Connect TabLayout dots indicator with ViewPager2
                    TabLayoutMediator(postHolder.binding.tabIndicator, postHolder.binding.vpPostImages) { _, _ -> }.attach()
                    
                    // If only 1 image, hide dots indicator to look clean
                    if (urls.size <= 1) {
                        postHolder.binding.tabIndicator.visibility = View.GONE
                    } else {
                        postHolder.binding.tabIndicator.visibility = View.VISIBLE
                    }
                    postHolder.binding.flPostImagesContainer.visibility = View.VISIBLE
                    rootLayout.addView(postHolder.binding.llContentContainer)
                } else {
                    postHolder.binding.flPostImagesContainer.visibility = View.GONE
                    postHolder.binding.tabIndicator.visibility = View.GONE
                    rootLayout.addView(postHolder.binding.llContentContainer, 1)
                }

                // Like toggle micro-interaction
                var isLiked = false
                postHolder.binding.ivLike.setOnClickListener {
                    isLiked = !isLiked
                    if (isLiked) {
                        postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                        postHolder.binding.tvLikes.text = (post.likesCount + 1).toString()
                    } else {
                        postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                        postHolder.binding.tvLikes.text = post.likesCount.toString()
                    }
                }
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
        
        if (!user.avatarUrl.isNullOrEmpty()) {
            holder.binding.ivAvatar.load(user.avatarUrl) {
                decoderFactory(SvgDecoder.Factory())
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(R.drawable.logo)
            }
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray)
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
