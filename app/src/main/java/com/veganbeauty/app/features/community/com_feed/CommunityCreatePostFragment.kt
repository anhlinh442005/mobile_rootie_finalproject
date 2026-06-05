package com.veganbeauty.app.features.community.com_feed

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.databinding.ComFragmentCreatePostBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityCreatePostFragment : RootieFragment() {

    private var _binding: ComFragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var communityDao: com.veganbeauty.app.data.local.dao.CommunityDao
    private val firestoreService = FirestoreService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val db = RootieDatabase.getDatabase(requireContext())
        communityDao = db.communityDao()

        binding.ivClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.llHorizontalIcons.visibility = View.GONE
                        binding.llVerticalList.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.llHorizontalIcons.visibility = View.VISIBLE
                        binding.llVerticalList.visibility = View.GONE
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        binding.vHandle.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrBlank()) {
                    binding.btnPost.setTextColor(Color.parseColor("#4B6554"))
                } else {
                    binding.btnPost.setTextColor(Color.parseColor("#9CA3AF"))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnPost.setOnClickListener {
            val content = binding.etContent.text.toString()
            if (content.isNotBlank()) {
                savePost(content)
            } else {
                Toast.makeText(context, "Vui lòng nhập nội dung bài viết", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePost(content: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val newPost = CommunityPostEntity(
            postId = UUID.randomUUID().toString(),
            authorId = "current_user",
            authorUsername = "anhlinh",
            authorDisplayName = "Ánh Linh",
            authorAvatarUrl = "", // Empty to trigger the fallback Ánh Linh avatar logic
            content = content,
            mediaUrlsString = "",
            createdAt = sdf.format(Date()),
            likesCount = 0,
            commentsCount = 0,
            skinType = "Da dầu", // Just mocking
            concern = "Routine",
            type = "post",
            linkedProductIds = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Save to local SQLite
            communityDao.insertPosts(listOf(newPost))
            // Upload to Firebase
            firestoreService.uploadCommunityPost(newPost)
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Đã đăng bài viết!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun observeViewModel() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

