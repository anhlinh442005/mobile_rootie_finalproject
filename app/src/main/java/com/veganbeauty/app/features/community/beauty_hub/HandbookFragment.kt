package com.veganbeauty.app.features.community.beauty_hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HandbookFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_handbook, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val rvVideos = view.findViewById<RecyclerView>(R.id.rvVideos)
        val llFilters = view.findViewById<LinearLayout>(R.id.llFilters)
        val etSearch = view.findViewById<android.widget.EditText>(R.id.etSearch)
        val llEmptyState = view.findViewById<LinearLayout>(R.id.llEmptyState)
        val llHasDataState = view.findViewById<LinearLayout>(R.id.llHasDataState)

        // Setup filter chips
        val filters = listOf("Tất cả", "mụn ẩn", "trị thâm", "Sạch sâu", "Da dầu mụn")
        val dp = resources.displayMetrics.density.toInt()
        
        filters.forEachIndexed { index, filter ->
            val chip = TextView(requireContext()).apply {
                text = filter
                textSize = 12f
                setPadding(16 * dp, 6 * dp, 16 * dp, 6 * dp)
                if (index == 0) {
                    setBackgroundResource(R.drawable.com_bg_chip_selected)
                    setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    setBackgroundResource(R.drawable.com_bg_chip_default)
                    setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
                }
                
                val marginParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    marginEnd = 8 * dp
                    if (index == 0) marginStart = 16 * dp
                    if (index == filters.size - 1) marginEnd = 16 * dp
                }
                layoutParams = marginParams
            }
            llFilters.addView(chip)
        }

        // Toggle Empty/Has Data State
        val memoryManager = com.veganbeauty.app.data.local.UserMemoryManager(requireContext())
        val categories = memoryManager.getCategories()
        if (categories.isNotEmpty() && categories.any { it.videos.isNotEmpty() }) {
            llEmptyState.visibility = View.GONE
            llHasDataState.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvMyHandbookTitle)?.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.VISIBLE
            llHasDataState.visibility = View.GONE
            view.findViewById<TextView>(R.id.tvMyHandbookTitle)?.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val jsonReader = LocalJsonReader(requireContext())
            val allVideos = withContext(Dispatchers.IO) {
                jsonReader.getExploreVideos()
            }
            
            // Filter notebook videos exactly
            val notebookVideos = allVideos.filter { 
                it.type.lowercase().contains("notebook")
            }
            
            var adapter = HandbookVideoAdapter(
                videos = notebookVideos,
                isHorizontal = false,
                isSaved = { video -> memoryManager.getCategories().any { cat -> cat.videos.any { it.url == video.url } } },
                onItemClick = { video ->
                    showVideoPlayerDialog(video)
                },
                onHeartClick = { video ->
                    showSaveVideoDialog(video, memoryManager)
                }
            )
            rvVideos.adapter = adapter

            // Search filter
            etSearch.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s?.toString()?.lowercase() ?: ""
                    val filtered = if (query.isEmpty()) {
                        notebookVideos
                    } else {
                        notebookVideos.filter { 
                            it.title.lowercase().contains(query) || it.description.lowercase().contains(query)
                        }
                    }
                    adapter = HandbookVideoAdapter(
                        videos = filtered,
                        isHorizontal = false,
                        isSaved = { video -> memoryManager.getCategories().any { cat -> cat.videos.any { it.url == video.url } } },
                        onItemClick = { video ->
                            showVideoPlayerDialog(video)
                        },
                        onHeartClick = { video ->
                            showSaveVideoDialog(video, memoryManager)
                        }
                    )
                    rvVideos.adapter = adapter
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            // Open My Handbook Dialog
            val openDialogListener = View.OnClickListener {
                showMyHandbookDialog(memoryManager)
            }
            view.findViewById<View>(R.id.btnViewMyHandbook)?.setOnClickListener(openDialogListener)
        }
    }

    private fun showSaveVideoDialog(video: com.veganbeauty.app.data.local.entities.YtVideoEntity, memoryManager: com.veganbeauty.app.data.local.UserMemoryManager) {
        val dialog = android.app.Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(R.layout.com_dialog_save_handbook)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<ImageView>(R.id.ivCloseSaveDialog)?.setOnClickListener {
            dialog.dismiss()
        }

        val rvSaveCategories = dialog.findViewById<RecyclerView>(R.id.rvSaveCategories)
        val llCreateNew = dialog.findViewById<LinearLayout>(R.id.llCreateNew)
        val llNewCategoryInput = dialog.findViewById<LinearLayout>(R.id.llNewCategoryInput)
        val etNewCategory = dialog.findViewById<android.widget.EditText>(R.id.etNewCategory)
        val btnSaveNew = dialog.findViewById<android.widget.Button>(R.id.btnSaveNew)

        val categories = memoryManager.getCategories()
        
        rvSaveCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rvSaveCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val ll = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 24, 0, 24)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val rb = android.widget.RadioButton(parent.context).apply {
                    buttonTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#677559"))
                    isClickable = false
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val tv = TextView(parent.context).apply {
                    textSize = 16f
                    setTextColor(android.graphics.Color.parseColor("#3E4D44"))
                    setPadding(24, 0, 0, 0)
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(parent.context, R.font.be_vietnam_pro)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                ll.addView(rb)
                ll.addView(tv)
                return object : RecyclerView.ViewHolder(ll) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val catName = categories[position].name
                val ll = holder.itemView as LinearLayout
                val rb = ll.getChildAt(0) as android.widget.RadioButton
                val tv = ll.getChildAt(1) as TextView
                
                tv.text = catName
                rb.isChecked = false
                
                holder.itemView.setOnClickListener {
                    rb.isChecked = true
                    showCustomConfirmDialog(
                        title = "Xác nhận",
                        message = "Lưu video này vào sổ '$catName'?",
                        positiveText = "Lưu",
                        negativeText = "Huỷ",
                        onPositive = {
                            memoryManager.addVideoToCategory(catName, video)
                            android.widget.Toast.makeText(requireContext(), "Đã lưu vào $catName", android.widget.Toast.LENGTH_SHORT).show()
                            view?.findViewById<LinearLayout>(R.id.llEmptyState)?.visibility = View.GONE
                            view?.findViewById<LinearLayout>(R.id.llHasDataState)?.visibility = View.VISIBLE
                            view?.findViewById<TextView>(R.id.tvMyHandbookTitle)?.visibility = View.GONE
                            val rv = view?.findViewById<RecyclerView>(R.id.rvVideos)
                            rv?.adapter?.notifyDataSetChanged()
                            dialog.dismiss()
                        },
                        onNegative = {
                            rb.isChecked = false
                        }
                    )
                }
            }
            override fun getItemCount() = categories.size
        }

        llCreateNew.setOnClickListener {
            llCreateNew.visibility = View.GONE
            llNewCategoryInput.visibility = View.VISIBLE
        }

        btnSaveNew.setOnClickListener {
            val newCat = etNewCategory.text.toString().trim()
            if (newCat.isNotEmpty()) {
                memoryManager.addVideoToCategory(newCat, video)
                android.widget.Toast.makeText(requireContext(), "Đã lưu vào $newCat", android.widget.Toast.LENGTH_SHORT).show()
                view?.findViewById<LinearLayout>(R.id.llEmptyState)?.visibility = View.GONE
                view?.findViewById<LinearLayout>(R.id.llHasDataState)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.tvMyHandbookTitle)?.visibility = View.GONE
                // Refresh main adapter to update red heart
                val rv = view?.findViewById<RecyclerView>(R.id.rvVideos)
                rv?.adapter?.notifyDataSetChanged()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showMyHandbookDialog(memoryManager: com.veganbeauty.app.data.local.UserMemoryManager) {
        val dialog = android.app.Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(R.layout.com_dialog_my_handbook)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<ImageView>(R.id.ivClose)?.setOnClickListener {
            dialog.dismiss()
        }

        val rvCategories = dialog.findViewById<RecyclerView>(R.id.rvCategories)
        rvCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        // Custom adapter for categories inside the dialog
        val categories = memoryManager.getCategories()
        rvCategories.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.com_item_handbook_category, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val cat = categories[position]
                holder.itemView.findViewById<TextView>(R.id.tvCategoryName).text = cat.name
                val rvVideos = holder.itemView.findViewById<RecyclerView>(R.id.rvCategoryVideos)
                rvVideos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
                rvVideos.adapter = HandbookVideoAdapter(
                    videos = cat.videos,
                    isHorizontal = true,
                    isSaved = { true },
                    onItemClick = { clickedVideo -> showVideoPlayerDialog(clickedVideo) },
                    onHeartClick = { /* Maybe remove from saved? Ignore for now */ },
                    onDeleteClick = { video ->
                        showCustomConfirmDialog(
                            title = "Xoá video",
                            message = "Xoá video này khỏi sổ '${cat.name}'?",
                            positiveText = "Xoá",
                            negativeText = "Huỷ",
                            onPositive = {
                                memoryManager.removeVideoFromCategory(cat.name, video)
                                android.widget.Toast.makeText(requireContext(), "Đã xoá", android.widget.Toast.LENGTH_SHORT).show()
                                
                                val idx = cat.videos.indexOfFirst { it.url == video.url || it.id == video.id }
                                if (idx != -1) {
                                    cat.videos.removeAt(idx)
                                    rvVideos.adapter?.notifyItemRemoved(idx)
                                }
                                
                                val mainRv = this@HandbookFragment.view?.findViewById<RecyclerView>(R.id.rvVideos)
                                mainRv?.adapter?.notifyDataSetChanged()
                                
                                val currentCats = memoryManager.getCategories()
                                if (currentCats.isEmpty() || currentCats.all { it.videos.isEmpty() }) {
                                    this@HandbookFragment.view?.findViewById<LinearLayout>(R.id.llEmptyState)?.visibility = View.VISIBLE
                                    this@HandbookFragment.view?.findViewById<LinearLayout>(R.id.llHasDataState)?.visibility = View.GONE
                                    this@HandbookFragment.view?.findViewById<TextView>(R.id.tvMyHandbookTitle)?.visibility = View.VISIBLE
                                    dialog.dismiss()
                                }
                            },
                            onNegative = {}
                        )
                    }
                )
            }
            override fun getItemCount() = categories.size
        }

        dialog.show()
    }

    private fun showCustomConfirmDialog(
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        val confirmDialog = android.app.Dialog(requireContext())
        confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        confirmDialog.setContentView(R.layout.com_dialog_confirm)
        confirmDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        confirmDialog.findViewById<TextView>(R.id.tvConfirmTitle)?.text = title
        confirmDialog.findViewById<TextView>(R.id.tvConfirmMessage)?.text = message
        
        val btnPositive = confirmDialog.findViewById<android.widget.Button>(R.id.btnConfirmPositive)
        btnPositive?.text = positiveText
        btnPositive?.setOnClickListener {
            confirmDialog.dismiss()
            onPositive()
        }

        val btnNegative = confirmDialog.findViewById<android.widget.Button>(R.id.btnConfirmNegative)
        btnNegative?.text = negativeText
        btnNegative?.setOnClickListener {
            confirmDialog.dismiss()
            onNegative()
        }
        
        confirmDialog.show()
    }

    private fun showVideoPlayerDialog(video: com.veganbeauty.app.data.local.entities.YtVideoEntity) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.com_dialog_video_player)

        val webView = dialog.findViewById<android.webkit.WebView>(R.id.webViewPlayer)
        val ivClose = dialog.findViewById<android.widget.ImageView>(R.id.ivClosePlayer)
        val tvDescription = dialog.findViewById<android.widget.TextView>(R.id.tvVideoDescription)
        val rvRelated = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvRelatedVideos)

        // Close
        ivClose.setOnClickListener { dialog.dismiss() }
        
        // Buttons
        val btnSave = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveFavorite)
        val btnYoutube = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOpenYoutube)
        
        btnSave.setOnClickListener {
            val memoryManager = com.veganbeauty.app.data.local.UserMemoryManager(requireContext())
            showSaveVideoDialog(video, memoryManager)
        }
        
        btnYoutube.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(video.url))
            startActivity(intent)
        }
        
        // Data Fields
        val tvVideoTitle = dialog.findViewById<android.widget.TextView>(R.id.tvVideoTitle)
        val tvHashtags = dialog.findViewById<android.widget.TextView>(R.id.tvHashtags)
        val tvCategory = dialog.findViewById<android.widget.TextView>(R.id.tvCategory)
        val tvSource = dialog.findViewById<android.widget.TextView>(R.id.tvSource)
        val tvKeywords = dialog.findViewById<android.widget.TextView>(R.id.tvKeywords)

        tvVideoTitle.text = video.title
        
        tvHashtags.text = if (video.hashtags.isNotBlank()) video.hashtags else ""
        tvHashtags.visibility = if (video.hashtags.isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        
        tvCategory.text = if (video.type.isNotBlank()) video.type.uppercase() else "CHUNG"
        tvSource.text = if (video.username.isNotBlank()) video.username else "Cộng đồng Rootie"
        
        tvKeywords.text = if (video.keywords.isNotBlank()) video.keywords else "#rootie #lamdep #skincare"

        // Description
        tvDescription.text = if (video.description.isNotBlank()) video.description else "Không có mô tả cho video này."

        // WebView setup
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-G991U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.webChromeClient = android.webkit.WebChromeClient()
        
        val videoId = extractYouTubeVideoId(video.url)
        if (videoId != null) {
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    // Inject viewport meta tag to force YouTube to fit the WebView bounds
                    val metaJs = "javascript:(function() { " +
                                 "var meta = document.createElement('meta'); " +
                                 "meta.name = 'viewport'; " +
                                 "meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'; " +
                                 "document.getElementsByTagName('head')[0].appendChild(meta); " +
                                 "})();"
                    view?.evaluateJavascript(metaJs, null)

                    // Continuously hide YouTube UI elements since it's a Single Page Application
                    val js = "javascript:(function() { " +
                             "setInterval(function() { " +
                             "  var elems = document.querySelectorAll('ytm-header-bar, .mobile-topbar-header, ytm-mobile-topbar-renderer, ytm-item-section-renderer, ytm-engagement-panel, ytm-comment-section-renderer, ytm-pivot-bar-renderer, ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer, ytm-slim-video-metadata-section-renderer, .player-placeholder'); " +
                             "  for (var i=0; i<elems.length; i++) { elems[i].style.setProperty('display', 'none', 'important'); } " +
                             "  if(document.body) { " +
                             "    document.body.style.setProperty('background-color', '#000', 'important'); " +
                             "    document.body.style.setProperty('overflow', 'hidden', 'important'); " +
                             "    document.body.style.setProperty('margin', '0', 'important'); " +
                             "    document.body.style.setProperty('padding', '0', 'important'); " +
                             "  } " +
                             "  var players = document.querySelectorAll('.player-size, ytm-custom-control-renderer'); " +
                             "  for (var i=0; i<players.length; i++) { " +
                             "    players[i].style.setProperty('width', '100vw', 'important'); " +
                             "    players[i].style.setProperty('height', '100%', 'important'); " +
                             "  } " +
                             "}, 500); " +
                             "})();"
                    view?.evaluateJavascript(js, null)
                }
            }
            // Load the actual mobile watch page to bypass 'embedding disabled' restrictions
            webView.loadUrl("https://m.youtube.com/watch?v=$videoId")
        } else {
            // Fallback for MP4 (Cloudinary/TikTok CDN) links
            val html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#000;display:flex;align-items:center;justify-content:center;">
                <video width="100%" height="100%" controls autoplay playsinline>
                    <source src="${video.url}" type="video/mp4">
                    Your browser does not support the video tag.
                </video>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }

        // Fetch related
        viewLifecycleOwner.lifecycleScope.launch {
            val jsonReader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
            val allVideos = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { jsonReader.getExploreVideos() }
            val related = allVideos.filter { it.id != video.id && it.type.lowercase().contains("notebook") }.shuffled().take(4)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                rvRelated.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                rvRelated.adapter = RelatedVideoAdapter(related) { clickedVideo ->
                    dialog.dismiss()
                    showVideoPlayerDialog(clickedVideo)
                }
            }
        }

        dialog.show()
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|shorts\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }
}
