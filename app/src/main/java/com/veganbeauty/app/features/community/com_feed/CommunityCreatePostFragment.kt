package com.veganbeauty.app.features.community.com_feed

import android.app.AlertDialog
import android.widget.EditText
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import coil.load
import com.veganbeauty.app.R
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

    private fun setupChips(container: android.widget.LinearLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is android.widget.TextView) {
                if (child.text == "+") continue
                child.setBackgroundResource(R.drawable.com_bg_chip_solid_normal)
                child.setTextColor(Color.parseColor("#4B6554"))
                child.setOnClickListener {
                    val isSelected = child.currentTextColor == Color.WHITE
                    if (isSelected) {
                        child.setBackgroundResource(R.drawable.com_bg_chip_solid_normal)
                        child.setTextColor(Color.parseColor("#4B6554"))
                    } else {
                        child.setBackgroundResource(R.drawable.com_bg_filter_selected)
                        child.setTextColor(Color.WHITE)
                    }
                }
            }
        }
    }

    private val selectedImageUris = mutableListOf<String>()
    private val selectedProductIds = mutableListOf<String>()

    private fun copyUriToInternalStorage(uri: android.net.Uri): String {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return uri.toString()
            val fileName = "post_img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(5)}.jpg"
            val file = java.io.File(requireContext().filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return "file://${file.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            return uri.toString()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val savedUris = uris.map { uri -> uri to copyUriToInternalStorage(uri) }
            
            withContext(Dispatchers.Main) {
                savedUris.forEach { (originalUri, savedUriStr) ->
                    selectedImageUris.add(savedUriStr)
                    val container = android.widget.FrameLayout(requireContext()).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            (100 * resources.displayMetrics.density).toInt(), 
                            (100 * resources.displayMetrics.density).toInt()
                        ).apply { marginEnd = (12 * resources.displayMetrics.density).toInt() }
                    }
                    
                    val iv = com.google.android.material.imageview.ShapeableImageView(requireContext()).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                        setBackgroundColor(Color.LTGRAY)
                        val radius = 12 * resources.displayMetrics.density
                        shapeAppearanceModel = shapeAppearanceModel.toBuilder().setAllCornerSizes(radius).build()
                        setImageURI(originalUri)
                    }
                    
                    val ivClose = android.widget.ImageView(requireContext()).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            (24 * resources.displayMetrics.density).toInt(),
                            (24 * resources.displayMetrics.density).toInt()
                        ).apply {
                            gravity = android.view.Gravity.TOP or android.view.Gravity.END
                            topMargin = (4 * resources.displayMetrics.density).toInt()
                            marginEnd = (4 * resources.displayMetrics.density).toInt()
                        }
                        setImageResource(R.drawable.ic_close)
                        setBackgroundResource(R.drawable.bg_circle_white) // Make it more visible
                        setPadding(12, 12, 12, 12)
                        setOnClickListener {
                            (container.parent as? android.view.ViewGroup)?.removeView(container)
                            selectedImageUris.remove(savedUriStr)
                        }
                    }
                    
                    container.addView(iv)
                    container.addView(ivClose)
                    binding.llImagePreviewContainer?.addView(container)
                }
            }
        }
    }

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

    private var loggedUserId = "test_001"
    private var loggedUsername = "Test User"
    private var loggedDisplayName = "Test User"
    private var loggedAvatarUrl = ""

    private fun loadCurrentUserInfo() {
        val ctx = requireContext()
        val loggedEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(ctx)
        try {
            val usersJson = ctx.assets.open("users.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val usersArr = org.json.JSONArray(usersJson)
            for (i in 0 until usersArr.length()) {
                val u = usersArr.getJSONObject(i)
                if (u.optString("email") == loggedEmail) {
                    loggedUserId = u.optString("user_id", "test_001")
                    loggedUsername = u.optString("username", "")
                    loggedDisplayName = u.optString("full_name", loggedUsername)
                    loggedAvatarUrl = u.optString("avatar", "")
                    break
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun setupUI(view: View) {
        // Hide keyboard when tapping outside
        val touchListener = View.OnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                binding.etContent.clearFocus()
            }
            false
        }
        binding.root.setOnTouchListener(touchListener)
        (binding.root.getChildAt(1) as? androidx.core.widget.NestedScrollView)?.setOnTouchListener(touchListener)

        val db = RootieDatabase.getDatabase(requireContext())
        communityDao = db.communityDao()

        loadCurrentUserInfo()
        binding.tvUserName.text = loggedDisplayName
        if (loggedAvatarUrl.isNotEmpty()) {
            binding.ivUserAvatar?.let { iv ->
                iv.load(loggedAvatarUrl) {
                    crossfade(true)
                    transformations(coil.transform.CircleCropTransformation())
                    placeholder(R.drawable.img_avatar)
                }
            }
        } else {
            binding.ivUserAvatar?.setImageResource(R.drawable.img_avatar)
        }

        setupChips(binding.llTopicsContainer)
        setupChips(binding.llSkinIssuesContainer)

        binding.ivClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.tvAddSkinIssue?.setOnClickListener {
            val editText = EditText(context)
            editText.hint = "Nhập vấn đề về da"
            AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setTitle("Thêm vấn đề về da")
                .setView(editText)
                .setPositiveButton("Thêm") { _, _ ->
                    val issue = editText.text.toString()
                    if (issue.isNotBlank()) {
                        val newChip = android.widget.TextView(context).apply {
                            text = issue
                            setTextColor(Color.WHITE)
                            setBackgroundResource(R.drawable.com_bg_filter_selected)
                            setPadding(
                                (12 * resources.displayMetrics.density).toInt(),
                                (4 * resources.displayMetrics.density).toInt(),
                                (12 * resources.displayMetrics.density).toInt(),
                                (4 * resources.displayMetrics.density).toInt()
                            )
                            textSize = 12f
                            val params = android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            params.marginEnd = (8 * resources.displayMetrics.density).toInt()
                            layoutParams = params
                            
                            setOnClickListener {
                                val isSelected = currentTextColor == Color.WHITE
                                if (isSelected) {
                                    setBackgroundResource(R.drawable.com_bg_chip_solid_normal)
                                    setTextColor(Color.parseColor("#4B6554"))
                                } else {
                                    setBackgroundResource(R.drawable.com_bg_filter_selected)
                                    setTextColor(Color.WHITE)
                                }
                            }
                        }
                        binding.llSkinIssuesContainer.addView(newChip, binding.llSkinIssuesContainer.childCount - 1)
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
        
        binding.root.findViewById<android.view.View>(R.id.tvAddTopic)?.setOnClickListener {
            val editText = EditText(context)
            editText.hint = "Nhập chủ đề bài đăng"
            AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setTitle("Thêm chủ đề")
                .setView(editText)
                .setPositiveButton("Thêm") { _, _ ->
                    val topic = editText.text.toString()
                    if (topic.isNotBlank()) {
                        val newChip = android.widget.TextView(context).apply {
                            text = topic
                            setTextColor(Color.WHITE)
                            setBackgroundResource(R.drawable.com_bg_filter_selected)
                            setPadding(
                                (12 * resources.displayMetrics.density).toInt(),
                                (4 * resources.displayMetrics.density).toInt(),
                                (12 * resources.displayMetrics.density).toInt(),
                                (4 * resources.displayMetrics.density).toInt()
                            )
                            textSize = 12f
                            val params = android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            params.marginEnd = (8 * resources.displayMetrics.density).toInt()
                            layoutParams = params
                            
                            setOnClickListener {
                                val isSelected = currentTextColor == Color.WHITE
                                if (isSelected) {
                                    setBackgroundResource(R.drawable.com_bg_chip_solid_normal)
                                    setTextColor(Color.parseColor("#4B6554"))
                                } else {
                                    setBackgroundResource(R.drawable.com_bg_filter_selected)
                                    setTextColor(Color.WHITE)
                                }
                            }
                        }
                        binding.llTopicsContainer.addView(newChip, binding.llTopicsContainer.childCount - 1)
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        binding.root.findViewById<android.view.View>(R.id.llPrivacy)?.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(context, v)
            popup.menu.add("Công khai")
            popup.menu.add("Bạn bè")
            popup.menu.add("Chỉ mình tôi")
            popup.menu.add("Tuỳ chọn")
            popup.setOnMenuItemClickListener { item ->
                binding.root.findViewById<android.widget.TextView>(R.id.tvPrivacy)?.text = item.title
                true
            }
            popup.show()
        }
        
        binding.llAddMedia?.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        binding.ivAddMediaHorizontal?.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        
        binding.llAddProduct?.setOnClickListener {
            showProductSelectionDialog()
        }
        
        binding.ivAddProductHorizontal?.setOnClickListener {
            showProductSelectionDialog()
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

    private fun showProductSelectionDialog() {
        try {
            val jsonStr = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val jsonObject = org.json.JSONObject(jsonStr)
            val allProductsArray = jsonObject.optJSONArray("products") ?: org.json.JSONArray()
            
            // Read user's showcase products
            // Read user's showcase products
            val productsMap = mutableMapOf<String, org.json.JSONObject>()
            try {
                val prefs = requireContext().getSharedPreferences("AffiliatePrefs", android.content.Context.MODE_PRIVATE)
                val hiddenProducts = prefs.getStringSet("hiddenProducts", mutableSetOf())?.toSet() ?: emptySet()
                
                // From orders.json
                val jsonOrders = requireContext().assets.open("orders.json").bufferedReader().use { it.readText() }
                val ordersData = org.json.JSONObject(jsonOrders)
                val ordersArr = ordersData.optJSONArray("orders") ?: org.json.JSONArray()
                for (i in 0 until ordersArr.length()) {
                    val order = ordersArr.optJSONObject(i) ?: continue
                    val status = order.optString("status")
                    if (status != "Đã hủy") {
                        val items = order.optJSONArray("items") ?: continue
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j) ?: continue
                            val pId = item.optString("productId")
                            if (pId.isNotEmpty() && !hiddenProducts.contains(pId)) {
                                val obj = org.json.JSONObject()
                                obj.put("id", pId)
                                obj.put("name", item.optString("productName"))
                                obj.put("thumbnail_url", item.optString("productImage"))
                                productsMap[pId] = obj
                            }
                        }
                    }
                }
                
                // From affiliate.json
                val jsonAffiliate = requireContext().assets.open("affiliate.json").bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(jsonAffiliate)
                if (jsonArray.length() > 0) {
                    val data = jsonArray.getJSONObject(0)
                    val orders = data.optJSONArray("orders") ?: org.json.JSONArray()
                    for (i in 0 until orders.length()) {
                        val order = orders.getJSONObject(i)
                        val pId = order.optString("product_id")
                        if (pId.isNotEmpty() && !hiddenProducts.contains(pId)) {
                            if (!productsMap.containsKey(pId)) {
                                val obj = org.json.JSONObject()
                                obj.put("id", pId)
                                obj.put("name", order.optString("product_name"))
                                obj.put("thumbnail_url", order.optString("product_image"))
                                productsMap[pId] = obj
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val productsArray = org.json.JSONArray()
            for (obj in productsMap.values) {
                productsArray.put(obj)
            }
            
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), 0)
            val container = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 48, 0, 48)
                setBackgroundResource(R.drawable.com_bg_top_rounded_white)
            }
            
            val title = android.widget.TextView(requireContext()).apply {
                text = "Chọn sản phẩm để gắn"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.BLACK)
                setPadding(48, 0, 48, 48)
            }
            container.addView(title)
            
            val scrollView = android.widget.ScrollView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (400 * resources.displayMetrics.density).toInt()
                )
            }
            
            val listContainer = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            
            if (productsArray.length() == 0) {
                val tvEmpty = android.widget.TextView(requireContext()).apply {
                    text = "Bạn chưa trưng bày sản phẩm nào trong cửa hàng."
                    textSize = 14f
                    setTextColor(Color.GRAY)
                    setPadding(48, 48, 48, 48)
                    gravity = android.view.Gravity.CENTER
                }
                listContainer.addView(tvEmpty)
            } else {
                for (i in 0 until productsArray.length()) {
                    val prod = productsArray.getJSONObject(i)
                    val id = prod.optString("id", "")
                    val name = prod.optString("name", "Sản phẩm không tên")
                val img = prod.optString("thumbnail_url", "")
                val brand = prod.optString("brand", "")
                
                val itemLayout = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(48, 24, 48, 24)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    
                    val iv = android.widget.ImageView(requireContext()).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            (50 * resources.displayMetrics.density).toInt(),
                            (50 * resources.displayMetrics.density).toInt()
                        ).apply { marginEnd = 32 }
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                    if (img.isNotEmpty()) {
                        iv.load(img) {
                            crossfade(true)
                        }
                    } else {
                        iv.setImageResource(R.color.gray_light)
                    }
                    
                    val textLayout = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    
                    val tvName = android.widget.TextView(requireContext()).apply {
                        text = name
                        textSize = 14f
                        setTextColor(Color.BLACK)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    val tvBrand = android.widget.TextView(requireContext()).apply {
                        text = brand
                        textSize = 12f
                        setTextColor(Color.GRAY)
                        setPadding(0, 8, 0, 0)
                    }
                    
                    textLayout.addView(tvName)
                    textLayout.addView(tvBrand)
                    
                    addView(iv)
                    addView(textLayout)
                    
                    setOnClickListener {
                        if (!selectedProductIds.contains(id)) {
                            selectedProductIds.add(id)
                            Toast.makeText(context, "Đã gắn: $name", Toast.LENGTH_SHORT).show()
                            
                            val chip = android.widget.TextView(context).apply {
                                text = "🛍️ $name"
                                setTextColor(Color.parseColor("#4B6554"))
                                setBackgroundResource(R.drawable.com_bg_chip_solid_normal)
                                setPadding(24, 12, 24, 12)
                                textSize = 12f
                                val params = android.widget.LinearLayout.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply { 
                                    topMargin = 16 
                                    marginEnd = 16
                                }
                                layoutParams = params
                                
                                setOnClickListener {
                                    selectedProductIds.remove(id)
                                    (parent as? android.view.ViewGroup)?.removeView(this)
                                }
                            }
                            binding.llImagePreviewContainer?.parent?.let { parentView ->
                                if (parentView is android.widget.LinearLayout) {
                                    parentView.addView(chip)
                                }
                            }
                        }
                        bottomSheetDialog.dismiss()
                    }
                }
                listContainer.addView(itemLayout)
                
                if (i < productsArray.length() - 1) {
                    val divider = android.view.View(requireContext()).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply {
                            marginStart = 48
                            marginEnd = 48
                        }
                        setBackgroundColor(Color.parseColor("#EEEEEE"))
                    }
                    listContainer.addView(divider)
                }
            }
            }
            
            scrollView.addView(listContainer)
            container.addView(scrollView)
            bottomSheetDialog.setContentView(container)
            
            val bottomSheet = bottomSheetDialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            }
            
            bottomSheetDialog.show()
                
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải danh sách sản phẩm", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePost(content: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        
        var selectedTopic = ""
        var selectedSkinIssue = ""
        
        for (i in 0 until binding.llTopicsContainer.childCount) {
            val child = binding.llTopicsContainer.getChildAt(i)
            if (child is android.widget.TextView && child.currentTextColor == Color.WHITE && child.text != "+") {
                selectedTopic = child.text.toString()
                break
            }
        }
        
        for (i in 0 until binding.llSkinIssuesContainer.childCount) {
            val child = binding.llSkinIssuesContainer.getChildAt(i)
            if (child is android.widget.TextView && child.currentTextColor == Color.WHITE && child.text != "+") {
                selectedSkinIssue = child.text.toString()
                break
            }
        }
        
        val newPost = CommunityPostEntity(
            postId = UUID.randomUUID().toString(),
            authorId = loggedUserId,
            authorUsername = loggedUsername,
            authorDisplayName = loggedDisplayName,
            authorAvatarUrl = loggedAvatarUrl,
            content = content,
            mediaUrlsString = selectedImageUris.joinToString(","),
            createdAt = sdf.format(Date()),
            likesCount = 0,
            commentsCount = 0,
            reupsCount = 0,
            skinType = selectedSkinIssue,
            concern = selectedTopic,
            type = selectedTopic,
            linkedProductIds = selectedProductIds.joinToString(",")
        )
        
        binding.root.findViewById<android.view.View>(R.id.flLoading)?.visibility = View.VISIBLE
        binding.btnPost.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            communityDao.insertPosts(listOf(newPost))
            firestoreService.uploadCommunityPost(newPost)
            
            withContext(Dispatchers.Main) {
                binding.root.findViewById<android.view.View>(R.id.flLoading)?.visibility = View.GONE
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

