package com.veganbeauty.app.features.community.com_feed

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommunitySearchFragment : RootieFragment() {

    private var _binding: ComFragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommunityViewModel
    private lateinit var userAdapter: UserSearchAdapter
    private lateinit var postAdapter: PostAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentSearchBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), FirestoreService())
        val factory = CommunityViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[CommunityViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
        }

        userAdapter = UserSearchAdapter(emptyList()) { user ->
            // Open Profile
            val fragment = com.veganbeauty.app.features.community.profile.CommunityProfileFragment.newInstance(user.user_id)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        postAdapter = PostAdapter()

        concatAdapter = ConcatAdapter(userAdapter, postAdapter)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = concatAdapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.ivClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                binding.llSuggestions.visibility = if (query.isEmpty()) View.VISIBLE else View.GONE
                binding.rvSearchResults.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                performSearch(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup suggestions
        for (i in 0 until binding.llSuggestionItems.childCount) {
            val textView = binding.llSuggestionItems.getChildAt(i) as? android.widget.TextView
            textView?.setOnClickListener {
                val text = textView.text.toString().replace("• ", "").trim()
                binding.etSearch.setText(text)
                binding.etSearch.setSelection(text.length)
                
                // Hide keyboard to show results
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        binding.llRefresh.setOnClickListener {
            val children = mutableListOf<View>()
            for (i in 0 until binding.llSuggestionItems.childCount) {
                children.add(binding.llSuggestionItems.getChildAt(i))
            }
            children.shuffle()
            binding.llSuggestionItems.removeAllViews()
            children.forEach { binding.llSuggestionItems.addView(it) }
        }

        binding.tvSearchAction.setOnClickListener {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        // Request focus
        binding.etSearch.requestFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            userAdapter.updateData(emptyList())
            postAdapter.updateData(emptyList(), emptyList(), emptyList(), emptyList())
            binding.tvEmpty.visibility = View.GONE
            return
        }

        val lowerQuery = query.lowercase()

        val allUsers = viewModel.users.value ?: emptyList()
        val allPosts = viewModel.posts.value ?: emptyList()
        val allReels = viewModel.reels.value ?: emptyList()

        val matchedUsers = allUsers.filter {
            it.username.lowercase().contains(lowerQuery) || it.full_name.lowercase().contains(lowerQuery)
        }

        val matchedPosts = allPosts.filter {
            (it.content.lowercase().contains(lowerQuery)) || 
            (it.type?.lowercase()?.contains(lowerQuery) == true) || 
            (it.skinType?.lowercase()?.contains(lowerQuery) == true) || 
            (it.concern?.lowercase()?.contains(lowerQuery) == true) ||
            matchedUsers.any { u -> u.user_id == it.authorId } // also match if author is in matched users
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = context ?: return@launch
            val productsList = FeedDataCache.productsList ?: withContext(Dispatchers.IO) { LocalJsonReader(ctx).getProducts() }.also { FeedDataCache.productsList = it }

            userAdapter.updateData(matchedUsers)
            postAdapter.updateData(matchedPosts, emptyList(), allReels, productsList)

            if (matchedUsers.isEmpty() && matchedPosts.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
            }
        }
    }

    override fun observeViewModel() {
        // Observers are not strictly needed because we only search over loaded data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        _binding = null
    }
}
