package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.ComFragmentProfileBinding
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.data.remote.FirestoreService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CommunityProfileFragment : Fragment() {

    private var _binding: ComFragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val ARG_USER_ID = "USER_ID"
        fun newInstance(userId: String) = CommunityProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_ID, userId)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        val jsonReader = LocalJsonReader(ctx)
        
        binding.ivBack.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment())
                    .commit()
            }
        }

        val passedUserId = arguments?.getString(ARG_USER_ID) ?: arguments?.getString("USER_ID")
        val loggedInEmail = ProfileSession.getEmail(ctx)
        
        var ownUserId = "test_001"
        try {
            val usersJsonStr = ctx.assets.open("users.json").bufferedReader().use { it.readText() }
            val usersJsonArray = org.json.JSONArray(usersJsonStr)
            for (i in 0 until usersJsonArray.length()) {
                val obj = usersJsonArray.getJSONObject(i)
                if (obj.optString("email") == loggedInEmail) {
                    ownUserId = obj.optString("user_id", "test_001")
                    break
                }
            }
        } catch(e: Exception) {}

        val currentUserId = passedUserId ?: ownUserId
        val isOwnProfile = (currentUserId == ownUserId)

        var bioText = if (isOwnProfile) ProfileSession.getBio(ctx) else "Empowering confidence through beauty and self-care."
        var jsonPrimaryImage = ""
        var jsonUsername = if (isOwnProfile) ProfileSession.getUsername(ctx) else "@$currentUserId"
        var jsonAvatar = ""
        var jsonFullName = ""
        
        try {
            val usersJsonStr = ctx.assets.open("users.json").bufferedReader().use { it.readText() }
            val usersJsonArray = org.json.JSONArray(usersJsonStr)
            for (i in 0 until usersJsonArray.length()) {
                val obj = usersJsonArray.getJSONObject(i)
                if (obj.optString("user_id") == currentUserId) {
                    val bio = obj.optString("bio")
                    if (bio.isNotEmpty() && !isOwnProfile) bioText = bio
                    jsonPrimaryImage = obj.optString("primary_image")
                    jsonFullName = obj.optString("full_name")
                    val uname = obj.optString("username")
                    if (uname.isNotEmpty() && !isOwnProfile) jsonUsername = "@" + uname.replace(" ", "").lowercase()
                    jsonAvatar = obj.optString("avatar")
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        var finalAvatarUrl = arguments?.getString("AVATAR_URL") ?: jsonAvatar
        var fallbackName = arguments?.getString("USER_NAME") ?: jsonFullName

        fun getFriendsJsonString(ctx: android.content.Context): String {
            val file = java.io.File(ctx.filesDir, "User_com_friend.json")
            if (file.exists()) {
                return file.readText()
            }
            return ctx.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
        }

        var currentFollowersCount = 0
        var currentFollowingCount = 0
        var isFollowing = false

        // Count followers and following from User_com_friend.json
        try {
            val friendJsonStr = getFriendsJsonString(ctx)
            val friendJsonArray = org.json.JSONArray(friendJsonStr)
            
            // Check if currentUserId is being followed by ownUserId
            for (i in 0 until friendJsonArray.length()) {
                val obj = friendJsonArray.getJSONObject(i)
                if (obj.optString("user_id") == ownUserId) {
                    val followingArr = obj.optJSONArray("following")
                    if (followingArr != null) {
                        for (j in 0 until followingArr.length()) {
                            if (followingArr.optString(j) == currentUserId) {
                                isFollowing = true
                                break
                            }
                        }
                    }
                    break
                }
            }

            for (i in 0 until friendJsonArray.length()) {
                val obj = friendJsonArray.getJSONObject(i)
                if (obj.optString("user_id") == currentUserId) {
                    currentFollowersCount = obj.optJSONArray("followers")?.length() ?: 0
                    currentFollowingCount = obj.optJSONArray("following")?.length() ?: 0
                    break
                }
            }
            binding.tvFollowersCount.text = currentFollowersCount.toString()
            binding.tvFollowingCount.text = currentFollowingCount.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvFollowersCount.text = "0"
            binding.tvFollowingCount.text = "0"
        }

        if (isOwnProfile) {
            binding.tvName.text = ProfileSession.getFullName(ctx)
            val uname = ProfileSession.getUsername(ctx)
            binding.tvUsername.text = if (uname.startsWith("@")) uname else "@$uname"
            
            finalAvatarUrl = ProfileSession.getAvatar(ctx)
        } else {
            binding.tvName.text = if (fallbackName.isNotEmpty()) fallbackName else "Người dùng"
            binding.tvUsername.text = jsonUsername
        }
        
        if (finalAvatarUrl.isNotEmpty()) {
            binding.ivAvatar.load(finalAvatarUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(android.R.color.darker_gray)
                error(R.drawable.img_avatar)
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.img_avatar)
        }
        
        if (isOwnProfile) {
            binding.btnEditProfile.text = "Chỉnh sửa"
            binding.btnShareProfile.text = "Chia sẻ"
            binding.btnEditProfile.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6E846A"))
            binding.btnEditProfile.setTextColor(android.graphics.Color.WHITE)
            
            binding.btnEditProfile.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(com.veganbeauty.app.R.id.main_container, CommunityEditProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            if (isFollowing) {
                binding.btnEditProfile.text = "Đã theo dõi"
                binding.btnEditProfile.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                binding.btnEditProfile.setTextColor(android.graphics.Color.parseColor("#6E846A"))
            } else {
                binding.btnEditProfile.text = "Theo dõi"
                binding.btnEditProfile.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6E846A"))
                binding.btnEditProfile.setTextColor(android.graphics.Color.WHITE)
            }
            binding.btnShareProfile.text = "Nhắn tin"
            
            binding.btnShareProfile.setOnClickListener {
                val convId = com.veganbeauty.app.features.community.message.MessageHelper.getOrCreateConversation(
                    ctx,
                    ownUserId,
                    currentUserId,
                    if (fallbackName.isNotEmpty()) fallbackName else "Người dùng",
                    finalAvatarUrl
                )
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.community.message.ChatDetailFragment.newInstance(convId))
                    .addToBackStack(null)
                    .commit()
            }
            
            binding.btnEditProfile.setOnClickListener {
                try {
                    val file = java.io.File(ctx.filesDir, "User_com_friend.json")
                    val currentFriendStr = getFriendsJsonString(ctx)
                    val friendJsonArray = org.json.JSONArray(currentFriendStr)
                    
                    if (!isFollowing) {
                        // Action: Follow
                        for (i in 0 until friendJsonArray.length()) {
                            val obj = friendJsonArray.getJSONObject(i)
                            if (obj.optString("user_id") == ownUserId) {
                                val followingArr = obj.optJSONArray("following") ?: org.json.JSONArray()
                                followingArr.put(currentUserId)
                                obj.put("following", followingArr)
                            }
                            if (obj.optString("user_id") == currentUserId) {
                                val followersArr = obj.optJSONArray("followers") ?: org.json.JSONArray()
                                followersArr.put(ownUserId)
                                obj.put("followers", followersArr)
                            }
                        }
                        isFollowing = true
                        currentFollowersCount++
                        
                        binding.btnEditProfile.text = "Đã theo dõi"
                        binding.btnEditProfile.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                        binding.btnEditProfile.setTextColor(android.graphics.Color.parseColor("#6E846A"))
                    } else {
                        // Action: Unfollow
                        for (i in 0 until friendJsonArray.length()) {
                            val obj = friendJsonArray.getJSONObject(i)
                            if (obj.optString("user_id") == ownUserId) {
                                val followingArr = obj.optJSONArray("following") ?: org.json.JSONArray()
                                val newFollowing = org.json.JSONArray()
                                for (j in 0 until followingArr.length()) {
                                    if (followingArr.optString(j) != currentUserId) {
                                        newFollowing.put(followingArr.getString(j))
                                    }
                                }
                                obj.put("following", newFollowing)
                            }
                            if (obj.optString("user_id") == currentUserId) {
                                val followersArr = obj.optJSONArray("followers") ?: org.json.JSONArray()
                                val newFollowers = org.json.JSONArray()
                                for (j in 0 until followersArr.length()) {
                                    if (followersArr.optString(j) != ownUserId) {
                                        newFollowers.put(followersArr.getString(j))
                                    }
                                }
                                obj.put("followers", newFollowers)
                            }
                        }
                        isFollowing = false
                        currentFollowersCount = maxOf(0, currentFollowersCount - 1)
                        
                        binding.btnEditProfile.text = "Theo dõi"
                        binding.btnEditProfile.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6E846A"))
                        binding.btnEditProfile.setTextColor(android.graphics.Color.WHITE)
                    }
                    
                    file.writeText(friendJsonArray.toString(2))
                    binding.tvFollowersCount.text = currentFollowersCount.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.tvBio.text = bioText
        
        val primaryImage = if (jsonPrimaryImage.isNotEmpty()) jsonPrimaryImage else if (isOwnProfile) ProfileSession.getPrimaryImage(ctx) else ""
        if (primaryImage.isNotEmpty()) {
            binding.ivCover.load(primaryImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        } else {
            binding.ivCover.setImageResource(R.color.primary)
        }
        
        // Load Avatar into Highlights to make them unique per user
        if (finalAvatarUrl.isNotEmpty()) {
            binding.ivHighlight1.load(finalAvatarUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
            binding.ivHighlight2.load(finalAvatarUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }




        // Load Skin Type from ProfileSession or SharedPreferences
        val prefs = ctx.getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
        val savedSkinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Sức khoẻ - Làm đẹp")
        binding.tvProfileSkinType.text = savedSkinType

        // Setup Grid RecyclerView for posts
        binding.rvPosts.layoutManager = GridLayoutManager(context, 3)

        val initialTab = arguments?.getInt("SELECTED_TAB", 0) ?: 0
        var currentTab = initialTab

        val sharedPrefs = ctx.getSharedPreferences("rootie_prefs", android.content.Context.MODE_PRIVATE)

        fun updateTabsUI() {
            binding.ivTabGrid.setColorFilter(if (currentTab == 0) android.graphics.Color.parseColor("#6E846A") else android.graphics.Color.parseColor("#888888"))
            binding.vTabGridIndicator.visibility = if (currentTab == 0) View.VISIBLE else View.INVISIBLE
            
            binding.ivTabVideo.setColorFilter(if (currentTab == 1) android.graphics.Color.parseColor("#6E846A") else android.graphics.Color.parseColor("#888888"))
            
            binding.ivTabReup.setColorFilter(if (currentTab == 2) android.graphics.Color.parseColor("#6E846A") else android.graphics.Color.parseColor("#888888"))
            
            binding.ivTabBookmark.setColorFilter(if (currentTab == 3) android.graphics.Color.parseColor("#6E846A") else android.graphics.Color.parseColor("#888888"))
        }

        // Load Posts from ViewModel (Room DB)
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = CommunityRepository(db.communityDao(), jsonReader, FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        val viewModel = ViewModelProvider(requireActivity(), factory)[CommunityViewModel::class.java]
        
        fun loadPostsForCurrentTab(allPosts: List<com.veganbeauty.app.data.local.entities.CommunityPostEntity>) {
            val myPosts = allPosts.filter { post ->
                when (currentTab) {
                    0 -> post.authorId == currentUserId
                    1 -> post.authorId == currentUserId // Implement video filter if needed
                    2 -> com.veganbeauty.app.features.community.UserMemoryHelper.isPostReposted(ctx, ownUserId, post.postId)
                    3 -> com.veganbeauty.app.features.community.UserMemoryHelper.isPostSaved(ctx, ownUserId, post.postId)
                    else -> post.authorId == currentUserId
                }
            }.distinctBy { it.postId }.sortedByDescending { it.createdAt }

            if (currentTab == 0) {
                binding.tvPostCount.text = myPosts.size.toString()
            }
            
            val adapter = ProfileGridAdapter(myPosts) { position ->
                val fragment = ProfilePostDetailFragment.newInstance(currentUserId, position, currentTab)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            binding.rvPosts.adapter = adapter
        }

        var currentAllPosts: List<com.veganbeauty.app.data.local.entities.CommunityPostEntity> = emptyList()

        viewModel.posts.observe(viewLifecycleOwner) { dbPosts ->
            viewLifecycleOwner.lifecycleScope.launch {
                val newsList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.veganbeauty.app.data.local.LocalJsonReader(ctx).getCommunityNews()
                }
                currentAllPosts = dbPosts + newsList
                loadPostsForCurrentTab(currentAllPosts)
            }
        }

        binding.rlTabGrid.setOnClickListener {
            currentTab = 0
            updateTabsUI()
            loadPostsForCurrentTab(currentAllPosts)
        }
        binding.ivTabVideo.setOnClickListener {
            currentTab = 1
            updateTabsUI()
            loadPostsForCurrentTab(currentAllPosts)
        }
        binding.ivTabReup.setOnClickListener {
            currentTab = 2
            updateTabsUI()
            loadPostsForCurrentTab(currentAllPosts)
        }
        binding.ivTabBookmark.setOnClickListener {
            currentTab = 3
            updateTabsUI()
            loadPostsForCurrentTab(currentAllPosts)
        }

        updateTabsUI()

        // Handle showcase click
        binding.llShowcase.setOnClickListener {
            val fragment = CommunityShowcaseFragment().apply {
                arguments = Bundle().apply {
                    putString("USER_ID", currentUserId)
                    putString("AVATAR_URL", finalAvatarUrl)
                    putString("USER_NAME", binding.tvName.text.toString())
                    putString("COVER_URL", primaryImage)
                }
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.llRevenue.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityRevenueFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
