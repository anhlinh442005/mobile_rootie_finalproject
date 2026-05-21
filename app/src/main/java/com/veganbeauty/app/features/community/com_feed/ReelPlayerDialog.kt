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
    private val reel: ReelEntity
) : DialogFragment() {

    private var _binding: ComDialogReelPlayerBinding? = null
    private val binding get() = _binding!!

    private val videoUrls = listOf(
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupVideoPlayer()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    private fun setupViews() {
        // Close button click listener
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // Author details binding
        binding.tvAuthorName.text = "@${reel.authorUsername}"
        binding.tvCaption.text = reel.caption
        binding.tvLikesCount.text = formatCount(reel.likesCount)
        binding.tvCommentsCount.text = formatCount(reel.commentsCount)
        binding.tvShareCount.text = formatCount(reel.shareCount)

        // Load circular author avatar using Coil
        if (!reel.authorAvatarUrl.isNullOrEmpty()) {
            binding.ivAuthorAvatarRight.load(reel.authorAvatarUrl) {
                decoderFactory(SvgDecoder.Factory())
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(R.drawable.logo)
            }
        } else {
            binding.ivAuthorAvatarRight.setImageResource(android.R.color.darker_gray)
        }

        // Like toggle transition
        var isLiked = false
        binding.ivLike.setOnClickListener {
            isLiked = !isLiked
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                binding.ivLike.setColorFilter(Color.parseColor("#E53935"))
                binding.tvLikesCount.text = formatCount(reel.likesCount + 1)
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                binding.ivLike.setColorFilter(Color.WHITE)
                binding.tvLikesCount.text = formatCount(reel.likesCount)
            }
        }

        // Follow button toggle
        var isFollowing = false
        binding.btnFollow.setOnClickListener {
            isFollowing = !isFollowing
            if (isFollowing) {
                binding.btnFollow.text = "Đang theo dõi"
                binding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_normal)
                binding.btnFollow.setTextColor(Color.WHITE)
            } else {
                binding.btnFollow.text = "Theo dõi"
                binding.btnFollow.setBackgroundResource(R.drawable.com_bg_filter_selected)
                binding.btnFollow.setBackgroundColor(Color.parseColor("#E53935"))
                binding.btnFollow.setTextColor(Color.WHITE)
            }
        }
    }

    private fun setupVideoPlayer() {
        val h = intFromMd5(reel.videoId)
        val selectedVideoUrl = videoUrls[h % videoUrls.size]

        binding.pbLoading.visibility = View.VISIBLE
        binding.videoView.setZOrderMediaOverlay(true)
        binding.videoView.setVideoPath(selectedVideoUrl)

        binding.videoView.setOnPreparedListener { mp ->
            mediaPlayer = mp
            binding.pbLoading.visibility = View.GONE
            mp.isLooping = true
            binding.videoView.start()
            startProgressUpdateLoop()
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.pbLoading.visibility = View.GONE
            // Return true to gracefully suppress the default system "Can't play this video" popup
            true
        }

        // Play/Pause click toggle zone
        binding.viewClickZone.setOnClickListener {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
            } else {
                binding.videoView.start()
            }
        }
    }

    private fun startProgressUpdateLoop() {
        progressHandler.post(object : Runnable {
            override fun run() {
                if (binding.videoView.isPlaying) {
                    val current = binding.videoView.currentPosition
                    val duration = binding.videoView.duration
                    if (duration > 0) {
                        val progress = (current * 100) / duration
                        binding.playbackProgress.progress = progress
                    }
                }
                progressHandler.postDelayed(this, 250)
            }
        })
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
        try {
            binding.videoView.stopPlayback()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _binding = null
    }
}
