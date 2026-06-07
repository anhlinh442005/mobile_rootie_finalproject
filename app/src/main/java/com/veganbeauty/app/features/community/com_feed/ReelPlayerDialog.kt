package com.veganbeauty.app.features.community.com_feed

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.fragment.app.DialogFragment
import coil.load
import coil.decode.SvgDecoder
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.databinding.ComDialogReelPlayerBinding
import java.security.MessageDigest

class ReelPlayerDialog(
    private val reels: List<ReelEntity>,
    private val initialPosition: Int
) : DialogFragment() {

    private var _binding: ComDialogReelPlayerBinding? = null
    private val binding get() = _binding!!

    private val videoUrls = listOf(
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423179/tiktok_nwm_7231472377438276870_n5xk8h.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423182/tiktok_nwm_7559978021822713106_ojcm1u.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423181/tiktok_nwm_7538058081125633298_c9sifg.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423180/tiktok_nwm_7487926346300148998_l8eetu.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423178/tiktok_nwm_7641962033369337096_nkiv9h.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423178/tiktok_nwm_7478916826060164370_uficoo.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423177/tiktok_nwm_7603600256147737876_xwwweq.mp4",
        "https://res.cloudinary.com/dpjkzxjl2/video/upload/v1779423177/tiktok_nwm_7504330582579563784_rtlvxf.mp4"
    )

    private val progressHandler = Handler(Looper.getMainLooper())
    private var isMuted = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full screen dialog style with black background
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComDialogReelPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var currentPlayingHolder: ReelPlayerViewHolder? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup Back Button
        binding.ivBack.setOnClickListener {
            dismiss()
        }

        // Setup ViewPager2
        val adapter = ReelPagerAdapter(reels)
        binding.viewPagerReels.adapter = adapter
        binding.viewPagerReels.setCurrentItem(initialPosition, false)
        
        binding.viewPagerReels.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Stop previous video
                currentPlayingHolder?.stopVideo()
                
                // Play new video
                val rv = binding.viewPagerReels.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
                val holder = rv?.findViewHolderForAdapterPosition(position) as? ReelPlayerViewHolder
                if (holder != null) {
                    holder.playVideo()
                    currentPlayingHolder = holder
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    inner class ReelPagerAdapter(private val items: List<ReelEntity>) : androidx.recyclerview.widget.RecyclerView.Adapter<ReelPlayerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelPlayerViewHolder {
            val itemBinding = com.veganbeauty.app.databinding.ComItemReelPlayerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ReelPlayerViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ReelPlayerViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
        
        override fun onViewAttachedToWindow(holder: ReelPlayerViewHolder) {
            super.onViewAttachedToWindow(holder)
            if (holder.bindingAdapterPosition == binding.viewPagerReels.currentItem) {
                holder.playVideo()
                currentPlayingHolder = holder
            }
        }
        
        override fun onViewDetachedFromWindow(holder: ReelPlayerViewHolder) {
            super.onViewDetachedFromWindow(holder)
            holder.stopVideo()
            if (currentPlayingHolder == holder) {
                currentPlayingHolder = null
            }
        }
    }

    inner class ReelPlayerViewHolder(val itemBinding: com.veganbeauty.app.databinding.ComItemReelPlayerBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemBinding.root) {
        private var mediaPlayer: MediaPlayer? = null
        private var isLiked = false
        private var isFollowing = false

        fun bind(reel: ReelEntity) {
            // Author details binding
            itemBinding.tvAuthorName.text = "@${reel.authorUsername}"
            itemBinding.tvCaption.text = reel.caption
            itemBinding.tvLikesCount.text = formatCount(reel.likesCount)
            itemBinding.tvCommentsCount.text = formatCount(reel.commentsCount)
            itemBinding.tvShareCount.text = formatCount(reel.shareCount)

            // Load circular author avatar using Coil
            if (!reel.authorAvatarUrl.isNullOrEmpty()) {
                itemBinding.ivAuthorAvatarRight.load(reel.authorAvatarUrl) {
                    decoderFactory(SvgDecoder.Factory())
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                    error(R.drawable.logo)
                }
            } else {
                itemBinding.ivAuthorAvatarRight.setImageResource(android.R.color.darker_gray)
            }

            // Like toggle
            itemBinding.ivLike.setOnClickListener {
                isLiked = !isLiked
                if (isLiked) {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                    itemBinding.ivLike.setColorFilter(Color.parseColor("#E53935"))
                    itemBinding.tvLikesCount.text = formatCount(reel.likesCount + 1)
                } else {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                    itemBinding.ivLike.setColorFilter(Color.WHITE)
                    itemBinding.tvLikesCount.text = formatCount(reel.likesCount)
                }
            }

            // Follow toggle
            itemBinding.btnFollow.setOnClickListener {
                isFollowing = !isFollowing
                if (isFollowing) {
                    itemBinding.btnFollow.text = "Đang theo dõi"
                    itemBinding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_normal)
                    itemBinding.btnFollow.setTextColor(Color.WHITE)
                } else {
                    itemBinding.btnFollow.text = "Theo dõi"
                    itemBinding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_selected)
                    itemBinding.btnFollow.setBackgroundColor(Color.parseColor("#E53935"))
                    itemBinding.btnFollow.setTextColor(Color.WHITE)
                }
            }

            // Play/Pause toggle
            itemBinding.viewClickZone.setOnClickListener {
                if (itemBinding.videoView.isPlaying) {
                    itemBinding.videoView.pause()
                } else {
                    itemBinding.videoView.start()
                }
            }

            // Setup Video Path
            val h = intFromMd5(reel.videoId)
            val selectedVideoUrl = videoUrls[h % videoUrls.size]
            itemBinding.videoView.setZOrderMediaOverlay(true)
            itemBinding.videoView.setVideoPath(selectedVideoUrl)

            itemBinding.videoView.setOnPreparedListener { mp ->
                mediaPlayer = mp
                itemBinding.pbLoading.visibility = View.GONE
                mp.isLooping = true
                if (bindingAdapterPosition == binding.viewPagerReels.currentItem) {
                    itemBinding.videoView.start()
                }
            }

            itemBinding.videoView.setOnErrorListener { _, _, _ ->
                itemBinding.pbLoading.visibility = View.GONE
                true
            }
        }

        fun playVideo() {
            if (mediaPlayer != null) {
                itemBinding.videoView.start()
            } else {
                itemBinding.pbLoading.visibility = View.VISIBLE
            }
        }

        fun stopVideo() {
            try {
                if (itemBinding.videoView.isPlaying) {
                    itemBinding.videoView.pause()
                }
                itemBinding.videoView.seekTo(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    private fun intFromMd5(s: String): Int {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(s.toByteArray())
            var value = 0
            for (i in 0..3) {
                value = (value shl 8) or (digest[i].toInt() and 0xFF)
            }
            Math.abs(value)
        } catch (e: Exception) {
            s.hashCode()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressHandler.removeCallbacksAndMessages(null)
        currentPlayingHolder?.stopVideo()
        _binding = null
    }
}
