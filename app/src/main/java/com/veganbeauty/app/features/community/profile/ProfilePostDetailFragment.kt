package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.features.community.com_feed.PostAdapter
import kotlinx.coroutines.launch

class ProfilePostDetailFragment : Fragment() {

    private lateinit var rvPosts: RecyclerView
    private var userId: String = "test_001"
    private var initialPosition: Int = 0

    private var currentTab: Int = 0
    private var targetPostId: String? = null

    companion object {
        fun newInstance(userId: String, initialPosition: Int, currentTab: Int = 0, targetPostId: String? = null): ProfilePostDetailFragment {
            val fragment = ProfilePostDetailFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            args.putInt("INITIAL_POSITION", initialPosition)
            args.putInt("CURRENT_TAB", currentTab)
            args.putString("TARGET_POST_ID", targetPostId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: "test_001"
            initialPosition = it.getInt("INITIAL_POSITION", 0)
            currentTab = it.getInt("CURRENT_TAB", 0)
            targetPostId = it.getString("TARGET_POST_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_profile_post_detail, container, false)
        rvPosts = view.findViewById(R.id.rvPosts)
        
        // Back button
        view.findViewById<View>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(requireContext())
        val repository = com.veganbeauty.app.data.repository.CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), com.veganbeauty.app.data.remote.FirestoreService())
        val factory = com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory(repository)
        val viewModel = androidx.lifecycle.ViewModelProvider(requireActivity(), factory)[com.veganbeauty.app.features.community.com_feed.CommunityViewModel::class.java]

        val adapter = PostAdapter()
        val layoutManager = LinearLayoutManager(requireContext())
        rvPosts.layoutManager = layoutManager
        rvPosts.adapter = adapter

        val jsonReader = LocalJsonReader(requireContext())
        val productsList = jsonReader.getProducts()

        viewModel.posts.observe(viewLifecycleOwner) { dbPosts ->
            viewLifecycleOwner.lifecycleScope.launch {
                val newsList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    jsonReader.getCommunityNews()
                }
                val allPostsCombined = dbPosts + newsList

                var ownUserId = "test_001"
                try {
                    val loggedInEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
                    val usersJsonStr = requireContext().assets.open("users.json").bufferedReader().use { it.readText() }
                    val usersJsonArray = org.json.JSONArray(usersJsonStr)
                    for (i in 0 until usersJsonArray.length()) {
                        val obj = usersJsonArray.getJSONObject(i)
                        if (obj.optString("email") == loggedInEmail) {
                            ownUserId = obj.optString("user_id", "test_001")
                            break
                        }
                    }
                } catch(e: Exception) {}

                val finalUserId = if (!targetPostId.isNullOrEmpty()) {
                    val targetPost = allPostsCombined.find { it.postId == targetPostId }
                    targetPost?.authorId ?: userId
                } else {
                    userId
                }

                val myPosts = allPostsCombined.filter { post ->
                    when (currentTab) {
                        0 -> post.authorId == finalUserId
                        1 -> post.authorId == finalUserId
                        2 -> com.veganbeauty.app.features.community.UserMemoryHelper.isPostReposted(requireContext(), ownUserId, post.postId)
                        3 -> com.veganbeauty.app.features.community.UserMemoryHelper.isPostSaved(requireContext(), ownUserId, post.postId)
                        else -> post.authorId == finalUserId
                    }
                }.distinctBy { it.postId }.sortedByDescending { it.createdAt }

                adapter.updateData(myPosts, emptyList(), emptyList(), productsList)

                val scrollPos = if (!targetPostId.isNullOrEmpty()) {
                    val index = myPosts.indexOfFirst { it.postId == targetPostId }
                    if (index != -1) index else initialPosition
                } else {
                    initialPosition
                }
                layoutManager.scrollToPositionWithOffset(scrollPos, 0)
            }
        }

    }
}
