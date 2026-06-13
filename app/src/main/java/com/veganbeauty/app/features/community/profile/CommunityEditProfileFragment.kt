package com.veganbeauty.app.features.community.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.databinding.ComFragmentEditProfileBinding
import com.veganbeauty.app.data.repository.CommunityRepository
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory
import com.veganbeauty.app.data.remote.FirestoreService

class CommunityEditProfileFragment : Fragment() {

    private var _binding: ComFragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    
    private var selectedAvatarUri: Uri? = null

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                selectedAvatarUri = resultUri
                binding.ivAvatar.load(resultUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val destUri = Uri.fromFile(java.io.File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
                val options = com.yalantis.ucrop.UCrop.Options()
                options.setCircleDimmedLayer(true)
                options.setShowCropGrid(false)
                options.setToolbarTitle("Chỉnh sửa ảnh")
                
                val uCropIntent = com.yalantis.ucrop.UCrop.of(uri, destUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(800, 800)
                    .withOptions(options)
                    .getIntent(requireContext())
                    
                cropImageLauncher.launch(uCropIntent)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        val ownUserId = "test_001"

        // Load existing data
        binding.etDisplayName.setText(ProfileSession.getFullName(ctx))
        val uname = ProfileSession.getUsername(ctx)
        binding.etUsername.setText(if (uname.startsWith("@")) uname else "@$uname")
        binding.etBio.setText(ProfileSession.getBio(ctx))

        val avatarUrl = ProfileSession.getAvatar(ctx)
        if (avatarUrl.isNotEmpty()) {
            binding.ivAvatar.load(avatarUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(android.R.color.darker_gray)
                error(R.drawable.img_avatar)
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.img_avatar)
        }

        val coverUrl = ProfileSession.getPrimaryImage(ctx)
        if (coverUrl.isNotEmpty()) {
            binding.ivCover.load(coverUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }

        // Load followers/following
        try {
            val friendJsonStr = ctx.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val friendJsonArray = org.json.JSONArray(friendJsonStr)
            var followersCount = 0
            var followingCount = 0
            for (i in 0 until friendJsonArray.length()) {
                val obj = friendJsonArray.getJSONObject(i)
                if (obj.optString("user_id") == ownUserId) {
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

        // Load post count
        try {
            val db = RootieDatabase.getDatabase(requireContext())
            val repository = CommunityRepository(db.communityDao(), LocalJsonReader(requireContext()), FirestoreService())
            val factory = CommunityViewModelFactory(repository)
            val viewModel = ViewModelProvider(requireActivity(), factory)[CommunityViewModel::class.java]
            viewModel.posts.observe(viewLifecycleOwner) { allPosts ->
                val myPosts = allPosts.filter { it.authorId == ownUserId }
                binding.tvPostCount.text = myPosts.size.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Setup Listeners
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val saveAction = View.OnClickListener {
            val newName = binding.etDisplayName.text.toString().trim()
            var newUname = binding.etUsername.text.toString().trim()
            val newBio = binding.etBio.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(ctx, "Tên hiển thị không được để trống", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (newUname.isEmpty()) {
                Toast.makeText(ctx, "Tên người dùng không được để trống", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            
            if (!newUname.startsWith("@")) {
                newUname = "@$newUname"
            }

            ProfileSession.setFullName(ctx, newName)
            ProfileSession.setUsername(ctx, newUname)
            ProfileSession.setBio(ctx, newBio)
            
            if (selectedAvatarUri != null) {
                ProfileSession.setAvatar(ctx, selectedAvatarUri.toString())
            }

            Toast.makeText(ctx, "Lưu thông tin thành công", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        binding.tvSaveTop.setOnClickListener(saveAction)
        binding.btnSaveBottom.setOnClickListener(saveAction)

        val pickAvatarAction = View.OnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.ivAvatar.setOnClickListener(pickAvatarAction)
        binding.ivCameraIcon.setOnClickListener(pickAvatarAction)
        binding.tvChangeAvatarHint.setOnClickListener(pickAvatarAction)
        
        binding.btnChangeCover.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            // Just simulate click for now
            Toast.makeText(ctx, "Tính năng đổi ảnh bìa đang được cập nhật", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
