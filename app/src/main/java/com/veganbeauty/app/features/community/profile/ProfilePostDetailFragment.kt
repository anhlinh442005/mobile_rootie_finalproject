package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.features.community.com_feed.PostAdapter

class ProfilePostDetailFragment : Fragment() {

    private lateinit var rvPosts: RecyclerView
    private var userId: String = "test_001"
    private var initialPosition: Int = 0

    companion object {
        fun newInstance(userId: String, initialPosition: Int): ProfilePostDetailFragment {
            val fragment = ProfilePostDetailFragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            args.putInt("INITIAL_POSITION", initialPosition)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID") ?: "test_001"
            initialPosition = it.getInt("INITIAL_POSITION", 0)
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

        viewModel.posts.observe(viewLifecycleOwner) { allPosts ->
            val myPosts = allPosts
                .filter { it.authorId == userId }
                .distinctBy { it.postId }  // deduplicate by post ID - never show same post twice
                .sortedByDescending { it.createdAt }
            adapter.updateData(myPosts, emptyList(), emptyList(), productsList)
            layoutManager.scrollToPositionWithOffset(initialPosition, 0)
        }

    }
}
