package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.veganbeauty.app.R
import androidx.lifecycle.lifecycleScope
data class CommentItem(
    val userId: String = "",
    val avatarUrl: String,
    val username: String,
    val timeStr: String,
    val content: String,
    val likesCount: Int,
    val isAuthor: Boolean,
    val hasReply: Boolean = false,
    val replyUserId: String = "",
    val replyUsername: String = "",
    val replyContent: String = "",
    val replyTime: String = "",
    val replyLikesCount: Int = 0,
    val replyAvatarUrl: String = ""
)

class CommunityCommentBottomSheet : BottomSheetDialogFragment() {

    private var postId: String? = null
    private var expectedCommentsCount: Int = 5

    companion object {
        const val TAG = "CommunityCommentBottomSheet"
        private const val ARG_POST_ID = "post_id"
        private const val ARG_COMMENTS_COUNT = "comments_count"

        fun newInstance(postId: String, commentsCount: Int = 5): CommunityCommentBottomSheet {
            val fragment = CommunityCommentBottomSheet()
            val args = Bundle()
            args.putString(ARG_POST_ID, postId)
            args.putInt(ARG_COMMENTS_COUNT, commentsCount)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = arguments?.getString(ARG_POST_ID)
        expectedCommentsCount = arguments?.getInt(ARG_COMMENTS_COUNT, 5) ?: 5
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_bottom_sheet_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvComments = view.findViewById<RecyclerView>(R.id.rvComments)
        rvComments.layoutManager = LinearLayoutManager(context)
        
        val tvEmptyComments = view.findViewById<TextView?>(R.id.tvEmptyComments)
        
        val commentsList = mutableListOf<CommentItem>()
        
        try {
            val context = requireContext()
            val usersMap = com.veganbeauty.app.data.local.LocalJsonReader(context).getUsers().associateBy { it.user_id }
            
            // Read inline comments from community_posts.json
            val postsJsonStr = context.assets.open("community_posts.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            
            var inlineCommentsArray: org.json.JSONArray? = null
            
            // Helper function to search in a specific JSON array
            fun findCommentsInArray(jsonArray: org.json.JSONArray?, idKey: String): org.json.JSONArray? {
                if (jsonArray == null) return null
                for (i in 0 until jsonArray.length()) {
                    val postObj = jsonArray.getJSONObject(i)
                    if (postObj.optString(idKey) == (postId ?: "")) {
                        return postObj.optJSONArray("comments")
                    }
                }
                return null
            }

            // 1. Check community_posts.json
            var postsArray: org.json.JSONArray? = null
            try { postsArray = org.json.JSONArray(postsJsonStr) } catch (e: Exception) {
                try { postsArray = org.json.JSONObject(postsJsonStr).optJSONArray("posts") } catch (e2: Exception) {}
            }
            inlineCommentsArray = findCommentsInArray(postsArray, "post_id")
            
            // 2. Check community_news.json
            if (inlineCommentsArray == null || inlineCommentsArray!!.length() == 0) {
                try {
                    val newsJsonStr = context.assets.open("community_news.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
                    val newsArray = org.json.JSONArray(newsJsonStr)
                    inlineCommentsArray = findCommentsInArray(newsArray, "post_id")
                } catch (e: Exception) { e.printStackTrace() }
            }
            
            // 3. Check community_reels_fb.json
            if (inlineCommentsArray == null || inlineCommentsArray!!.length() == 0) {
                try {
                    val reelsJsonStr = context.assets.open("community_reels_fb.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
                    val reelsArray = org.json.JSONArray(reelsJsonStr)
                    inlineCommentsArray = findCommentsInArray(reelsArray, "reel_id")
                    if (inlineCommentsArray == null) {
                        inlineCommentsArray = findCommentsInArray(reelsArray, "post_id")
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            
            // 4. Check local_comments.json (User submitted comments during this session)
            try {
                val localFile = java.io.File(context.filesDir, "local_comments.json")
                if (localFile.exists()) {
                    val localArray = org.json.JSONArray(localFile.readText())
                    for (i in 0 until localArray.length()) {
                        val obj = localArray.getJSONObject(i)
                        if (obj.optString("post_id") == postId) {
                            if (inlineCommentsArray == null) {
                                inlineCommentsArray = org.json.JSONArray()
                            }
                            inlineCommentsArray!!.put(obj)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            if (inlineCommentsArray != null && inlineCommentsArray!!.length() > 0) {
                // Group top-level comments and their replies
                val topLevelComments = mutableListOf<org.json.JSONObject>()
                val repliesMap = mutableMapOf<String, org.json.JSONObject>()
                
                for (i in 0 until inlineCommentsArray.length()) {
                    val obj = inlineCommentsArray.getJSONObject(i)
                    val parentId = obj.optString("parent_id", "null")
                    if (parentId == "null" || parentId.isEmpty()) {
                        topLevelComments.add(obj)
                    } else {
                        repliesMap[parentId] = obj
                    }
                }
                
                for (obj in topLevelComments) {
                    val authorObj = obj.optJSONObject("author")
                    val userId = authorObj?.optString("user_id")?.takeIf { it.isNotEmpty() } ?: obj.optString("user_id", "")
                    val username = authorObj?.optString("username")?.takeIf { it.isNotEmpty() } ?: obj.optString("username", "") ?: usersMap[userId]?.username ?: ""
                    val avatarUrl = authorObj?.optString("avatar")?.takeIf { it.isNotEmpty() } ?: obj.optString("avatar_url", "") ?: usersMap[userId]?.avatar ?: ""
                    val commentId = obj.optString("comment_id", "")
                    
                    // Check for reply
                    val replyObj = repliesMap[commentId]
                    var hasReply = false
                    var replyUsername = ""; var replyContent = ""; var replyTime = ""; var replyLikes = 0; var replyAvatar = ""
                    var replyUserId = ""
                    if (replyObj != null) {
                        hasReply = true
                        val replyAuthor = replyObj.optJSONObject("author")
                        replyUserId = replyAuthor?.optString("user_id")?.takeIf { it.isNotEmpty() } ?: replyObj.optString("user_id", "")
                        replyUsername = replyAuthor?.optString("username")?.takeIf { it.isNotEmpty() } ?: replyObj.optString("username", "") ?: usersMap[replyUserId]?.username ?: ""
                        replyAvatar = replyAuthor?.optString("avatar")?.takeIf { it.isNotEmpty() } ?: replyObj.optString("avatar_url", "") ?: usersMap[replyUserId]?.avatar ?: ""
                        replyContent = replyObj.optString("content", "")
                        replyTime = replyObj.optString("created_at", "")
                        replyLikes = replyObj.optInt("likes_count", 0)
                    }
                    
                    val rawTime = obj.optString("created_at", "")
                    val timeStr = if (rawTime.isNotEmpty()) com.veganbeauty.app.utils.TimeFormatter.getTimeAgo(rawTime) else ""
                    
                    commentsList.add(CommentItem(
                        userId = userId,
                        avatarUrl = avatarUrl,
                        username = username,
                        timeStr = timeStr.ifEmpty { rawTime },
                        content = obj.optString("content", ""),
                        likesCount = obj.optInt("likes_count", 0),
                        isAuthor = obj.optBoolean("is_author", false),
                        hasReply = hasReply,
                        replyUserId = if (hasReply) repliesMap[commentId]?.optString("user_id", "") ?: "" else "",
                        replyUsername = replyUsername,
                        replyContent = replyContent,
                        replyTime = if (replyTime.isNotEmpty()) com.veganbeauty.app.utils.TimeFormatter.getTimeAgo(replyTime) else "",
                        replyLikesCount = replyLikes,
                        replyAvatarUrl = replyAvatar
                    ))
                }
            }
            
            // If still no comments, show empty state
            // Removed fake comments generation as requested by the user
        } catch (e: Exception) {
            e.printStackTrace()
        }        
        val tvCommentCount = view.findViewById<TextView>(R.id.tvCommentCount)
        if (commentsList.isEmpty()) {
            tvEmptyComments?.visibility = View.VISIBLE
            rvComments.visibility = View.GONE
            tvCommentCount.text = "Bình luận"
        } else {
            tvEmptyComments?.visibility = View.GONE
            rvComments.visibility = View.VISIBLE
            rvComments.adapter = CommentAdapter(commentsList)
            tvCommentCount.text = "${commentsList.size} bình luận"
        }
        
        // Hide/Show bottom sheet on dismiss/send
        view.findViewById<ImageView>(R.id.ivSendComment).setOnClickListener {
            dismiss()
        }
        
        // Handle sending new comment
        val etComment = view.findViewById<android.widget.EditText>(R.id.etComment)
        etComment.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND || 
                (event != null && event.action == android.view.KeyEvent.ACTION_DOWN && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER)) {
                
                val text = etComment.text.toString().trim()
                if (text.isNotEmpty() && postId != null) {
                    val myUserId = "test_001" // Current logged-in user
                    val myUsername = "Test User"
                    val myAvatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg"
                    
                    val timeStr = "Vừa xong"
                    val isoDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).apply { 
                        timeZone = java.util.TimeZone.getTimeZone("UTC") 
                    }.format(java.util.Date())
                    
                    val newComment = CommentItem(
                        userId = myUserId,
                        avatarUrl = myAvatar,
                        username = myUsername,
                        timeStr = timeStr,
                        content = text,
                        likesCount = 0,
                        isAuthor = false,
                        hasReply = false
                    )
                    
                    commentsList.add(0, newComment)
                    
                    tvEmptyComments?.visibility = View.GONE
                    rvComments.visibility = View.VISIBLE
                    if (rvComments.adapter == null) {
                        rvComments.adapter = CommentAdapter(commentsList)
                    } else {
                        rvComments.adapter?.notifyItemInserted(0)
                        rvComments.scrollToPosition(0)
                    }
                    tvCommentCount.text = "${commentsList.size} bình luận"
                    
                    etComment.setText("")
                    
                    // Hide keyboard
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etComment.windowToken, 0)
                    
                    val commentMap = hashMapOf<String, Any>(
                        "comment_id" to java.util.UUID.randomUUID().toString(),
                        "post_id" to postId!!,
                        "user_id" to myUserId,
                        "username" to myUsername,
                        "avatar_url" to myAvatar,
                        "content" to text,
                        "created_at" to isoDate,
                        "likes_count" to 0,
                        "is_author" to false
                    )

                    // Save to local storage for persistence in session
                    try {
                        val localFile = java.io.File(requireContext().filesDir, "local_comments.json")
                        val localArray = if (localFile.exists()) org.json.JSONArray(localFile.readText()) else org.json.JSONArray()
                        val newCommentJson = org.json.JSONObject(commentMap as Map<*, *>)
                        localArray.put(newCommentJson)
                        localFile.writeText(localArray.toString())
                    } catch (e: Exception) { e.printStackTrace() }

                    // Increment in database & Sync to Firestore
                    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                        com.veganbeauty.app.data.local.RootieDatabase.getDatabase(requireContext()).communityDao().incrementCommentsCount(postId!!)
                        com.veganbeauty.app.data.remote.FirestoreService().addCommentToPost(postId!!, commentMap)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    inner class CommentAdapter(private val comments: List<CommentItem>) :
        RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

        inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            val tvUsername: TextView = view.findViewById(R.id.tvUsername)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvContent: TextView = view.findViewById(R.id.tvContent)
            val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
            val tvAuthorTag: TextView = view.findViewById(R.id.tvAuthorTag)
            
            val llReplyContainer: LinearLayout = view.findViewById(R.id.llReplyContainer)
            val ivReplyAvatar: ImageView = view.findViewById(R.id.ivReplyAvatar)
            val tvReplyUsername: TextView = view.findViewById(R.id.tvReplyUsername)
            val tvReplyTime: TextView = view.findViewById(R.id.tvReplyTime)
            val tvReplyContent: TextView = view.findViewById(R.id.tvReplyContent)
            val tvReplyLikesCount: TextView = view.findViewById(R.id.tvReplyLikesCount)
            
            val llReplyContentContainer: LinearLayout = view.findViewById(R.id.llReplyContentContainer)
            val llViewMoreReplies: LinearLayout = view.findViewById(R.id.llViewMoreReplies)
            
            val ivLike: ImageView = view.findViewById(R.id.ivLike)
            val ivReplyLike: ImageView = view.findViewById(R.id.ivReplyLike)
            
            fun bind(item: CommentItem) {
                tvUsername.text = item.username
                tvTime.text = item.timeStr
                tvContent.text = item.content
                tvLikesCount.text = item.likesCount.toString()
                
                if (item.isAuthor) {
                    tvAuthorTag.visibility = View.VISIBLE
                } else {
                    tvAuthorTag.visibility = View.GONE
                }
                
                ivAvatar.load(item.avatarUrl) {
                    crossfade(true)
                    placeholder(R.drawable.img_avatar)
                    error(R.drawable.img_avatar)
                }

                // Profile Navigation Logic
                val navigateToProfile = View.OnClickListener {
                    val context = itemView.context
                    if (context is androidx.fragment.app.FragmentActivity && item.userId.isNotEmpty()) {
                        val profileFragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment().apply {
                            arguments = android.os.Bundle().apply {
                                putString("USER_ID", item.userId)
                            }
                        }
                        context.supportFragmentManager.beginTransaction()
                            .replace(R.id.main_container, profileFragment)
                            .addToBackStack(null)
                            .commit()
                        dismiss() // Close bottom sheet
                    }
                }
                
                ivAvatar.setOnClickListener(navigateToProfile)
                tvUsername.setOnClickListener(navigateToProfile)

                if (item.hasReply) {
                    llReplyContainer.visibility = View.VISIBLE
                    llReplyContentContainer.visibility = View.GONE
                    llViewMoreReplies.visibility = View.VISIBLE
                    
                    tvReplyUsername.text = item.replyUsername
                    tvReplyTime.text = item.replyTime
                    tvReplyContent.text = item.replyContent
                    tvReplyLikesCount.text = item.replyLikesCount.toString()
                    
                    ivReplyAvatar.load(item.replyAvatarUrl) {
                        crossfade(true)
                        placeholder(R.drawable.img_avatar)
                        error(R.drawable.img_avatar)
                    }
                    
                    // Reply Profile Navigation Logic
                    val navigateToReplyProfile = View.OnClickListener {
                        val context = itemView.context
                        if (context is androidx.fragment.app.FragmentActivity && item.replyUserId.isNotEmpty()) {
                            val profileFragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment().apply {
                                arguments = android.os.Bundle().apply {
                                    putString("USER_ID", item.replyUserId)
                                }
                            }
                            context.supportFragmentManager.beginTransaction()
                                .replace(R.id.main_container, profileFragment)
                                .addToBackStack(null)
                                .commit()
                            dismiss() // Close bottom sheet
                        }
                    }
                    
                    ivReplyAvatar.setOnClickListener(navigateToReplyProfile)
                    tvReplyUsername.setOnClickListener(navigateToReplyProfile)
                    
                    llViewMoreReplies.setOnClickListener {
                        llViewMoreReplies.visibility = View.GONE
                        llReplyContentContainer.visibility = View.VISIBLE
                    }
                } else {
                    llReplyContainer.visibility = View.GONE
                }
                
                var isLiked = false
                ivLike.setOnClickListener {
                    isLiked = !isLiked
                    if (isLiked) {
                        ivLike.setImageResource(R.drawable.ic_heart_filled)
                        tvLikesCount.text = (item.likesCount + 1).toString()
                    } else {
                        ivLike.setImageResource(R.drawable.ic_heart_outline)
                        tvLikesCount.text = item.likesCount.toString()
                    }
                }
                
                var isReplyLiked = false
                ivReplyLike.setOnClickListener {
                    isReplyLiked = !isReplyLiked
                    if (isReplyLiked) {
                        ivReplyLike.setImageResource(R.drawable.ic_heart_filled)
                        tvReplyLikesCount.text = "21"
                    } else {
                        ivReplyLike.setImageResource(R.drawable.ic_heart_outline)
                        tvReplyLikesCount.text = "20"
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.com_item_comment, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            holder.bind(comments[position])
        }

        override fun getItemCount(): Int = comments.size
    }
}
