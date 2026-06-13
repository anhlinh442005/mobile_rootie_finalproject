package com.veganbeauty.app.features.community.beauty_hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.decode.SvgDecoder
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.databinding.ComFragmentNewsBinding
import com.veganbeauty.app.features.community.com_feed.PostAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommunityNewsFragment : RootieFragment() {

    private var _binding: ComFragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val postAdapter = PostAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.rvNews.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNews.adapter = postAdapter

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Hardcode official Rootie VietNam information
        binding.tvName.text = "Rootie VietNam"
        
        // Avatar is the brand logo
        binding.ivAvatar.setImageResource(R.drawable.imv_logo)
        
        // Cover image (fallback to img_beautyhub_banner if network fails)
        binding.ivCover.load("https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780843940/bbc9ba9c-790c-481c-9600-ad6736337cba_mszen2.png") {
            crossfade(true)
            error(R.drawable.img_beautyhub_banner)
            placeholder(R.drawable.img_beautyhub_banner)
        }
        
        // Fetch news posts and mutual friends
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val ctx = context ?: return@launchWhenStarted
            val jsonReader = LocalJsonReader(ctx)
            
            val mutualUsers = withContext(Dispatchers.IO) {
                try {
                    // 1. Get current logged in user ID
                    val loggedInEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(ctx)
                    var currentUserId = "test_001"
                    
                    val usersStr = ctx.assets.open("users.json").bufferedReader().use { it.readText() }
                    val usersArray = org.json.JSONArray(usersStr)
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        if (obj.optString("email") == loggedInEmail) {
                            currentUserId = obj.optString("user_id", "test_001")
                            break
                        }
                    }
                    
                    // 2. Read User_com_friend.json
                    val friendStr = ctx.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
                    val friendArray = org.json.JSONArray(friendStr)
                    
                    var myFollowing = mutableListOf<String>()
                    var rootieFollowers = mutableListOf<String>()
                    
                    for (i in 0 until friendArray.length()) {
                        val obj = friendArray.getJSONObject(i)
                        val uid = obj.optString("user_id")
                        if (uid == currentUserId) {
                            val followingArr = obj.optJSONArray("following")
                            if (followingArr != null) {
                                for (j in 0 until followingArr.length()) {
                                    myFollowing.add(followingArr.getString(j))
                                }
                            }
                        } else if (uid == "rootie_vn") {
                            val followersArr = obj.optJSONArray("followers")
                            if (followersArr != null) {
                                for (j in 0 until followersArr.length()) {
                                    rootieFollowers.add(followersArr.getString(j))
                                }
                            }
                        }
                    }
                    
                    // 3. Find intersection
                    val mutualIds = myFollowing.intersect(rootieFollowers.toSet()).toList()
                    
                    // 4. Get info from users.json
                    val mutualList = mutableListOf<Triple<String, String, String>>() // name, avatar, userId
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        val uid = obj.optString("user_id")
                        if (mutualIds.contains(uid)) {
                            val name = obj.optString("username", "Người dùng")
                            val avt = obj.optString("avatar", "")
                            mutualList.add(Triple(name, avt, uid))
                        }
                    }
                    mutualList
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            
            // 5. Update UI
            if (mutualUsers.isNotEmpty()) {
                binding.llMutualInfo.visibility = View.VISIBLE
                
                val avatars = mutualUsers.map { it.second }.filter { it.isNotEmpty() }
                binding.ivMutual1.visibility = if (avatars.isNotEmpty()) View.VISIBLE else View.GONE
                binding.ivMutual2.visibility = if (avatars.size > 1) View.VISIBLE else View.GONE
                binding.ivMutual3.visibility = if (avatars.size > 2) View.VISIBLE else View.GONE
                
                if (avatars.isNotEmpty()) {
                    binding.ivMutual1.load(avatars[0]) { decoderFactory(SvgDecoder.Factory()); transformations(CircleCropTransformation()); crossfade(true); error(R.drawable.img_avatar) }
                }
                if (avatars.size > 1) {
                    binding.ivMutual2.load(avatars[1]) { decoderFactory(SvgDecoder.Factory()); transformations(CircleCropTransformation()); crossfade(true); error(R.drawable.img_avatar) }
                }
                if (avatars.size > 2) {
                    binding.ivMutual3.load(avatars[2]) { decoderFactory(SvgDecoder.Factory()); transformations(CircleCropTransformation()); crossfade(true); error(R.drawable.img_avatar) }
                }
                
                val count = mutualUsers.size
                if (count == 1) {
                    binding.tvMutualCount.text = "Có ${mutualUsers[0].first} đang theo dõi"
                } else {
                    binding.tvMutualCount.text = "Có ${mutualUsers[0].first} và ${count - 1} người khác theo dõi"
                }
                
                binding.llMutualInfo.setOnClickListener {
                    val dialogView = layoutInflater.inflate(R.layout.com_dialog_mutual_followers, null)
                    val llFollowerList = dialogView.findViewById<LinearLayout>(R.id.llFollowerList)

                    val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()

                    mutualUsers.forEach { user ->
                        val itemView = layoutInflater.inflate(R.layout.com_item_mutual_follower, llFollowerList, false)
                        val ivAvatar = itemView.findViewById<ImageView>(R.id.ivItemAvatar)
                        val tvName = itemView.findViewById<TextView>(R.id.tvItemName)
                        
                        tvName.text = user.first
                        if (user.second.isNotEmpty()) {
                            ivAvatar.load(user.second) {
                                decoderFactory(SvgDecoder.Factory())
                                crossfade(true)
                                error(R.drawable.img_avatar)
                            }
                        }
                        
                        itemView.setOnClickListener {
                            dialog.dismiss()
                            parentFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.main_container, com.veganbeauty.app.features.community.profile.CommunityProfileFragment.newInstance(user.third))
                                .addToBackStack(null)
                                .commit()
                        }
                        
                        llFollowerList.addView(itemView)
                    }

                    dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    dialog.show()
                }
            } else {
                binding.llMutualInfo.visibility = View.GONE
            }

            // Load news from community_news.json
            val newsPosts = withContext(Dispatchers.IO) {
                jsonReader.getCommunityNews()
            }
            
            // Reverse so newest posts appear first
            val sortedNews = newsPosts.sortedByDescending { it.createdAt }
            postAdapter.updateData(sortedNews, emptyList(), emptyList(), emptyList())
        }
    }

    override fun observeViewModel() {
        // Not used
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
