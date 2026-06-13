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
            
            binding.btnEditProfile.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(com.veganbeauty.app.R.id.main_container, CommunityEditProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            binding.btnEditProfile.text = "Theo dõi"
            binding.btnShareProfile.text = "Nhắn tin"
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


        // Count followers and following from User_com_friend.json
        try {
            val friendJsonStr = ctx.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val friendJsonArray = org.json.JSONArray(friendJsonStr)
            var followersCount = 0
            var followingCount = 0
            for (i in 0 until friendJsonArray.length()) {
                val obj = friendJsonArray.getJSONObject(i)
                if (obj.optString("user_id") == currentUserId) {
                    followersCount = obj.optJSONArray("followers")?.length() ?: 0
                    followingCount = obj.optJSONArray("following")?.length() ?: 0
                    break
                }
            }
            binding.tvFollowersCount.text = followersCount.toString()
            binding.tvFollowingCount.text = followingCount.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.tvFollowersCount.text = "0"
            binding.tvFollowingCount.text = "0"
        }

        // Load Skin Type from ProfileSession or SharedPreferences
        val prefs = ctx.getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
        val savedSkinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Sức khoẻ - Làm đẹp")
        binding.tvProfileSkinType.text = savedSkinType

        // Setup Grid RecyclerView for posts
        binding.rvPosts.layoutManager = GridLayoutManager(context, 3)

        // Load Posts from ViewModel (Room DB)
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = CommunityRepository(db.communityDao(), jsonReader, FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        val viewModel = ViewModelProvider(requireActivity(), factory)[CommunityViewModel::class.java]
        
        viewModel.posts.observe(viewLifecycleOwner) { allPosts ->
            val myPosts = allPosts
                .filter { it.authorId == currentUserId }
                .distinctBy { it.postId }          // deduplicate - each post appears once
                .sortedByDescending { it.createdAt }
            binding.tvPostCount.text = myPosts.size.toString()
            
            val adapter = ProfileGridAdapter(myPosts) { position ->
                val fragment = ProfilePostDetailFragment.newInstance(currentUserId, position)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            binding.rvPosts.adapter = adapter
        }

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
