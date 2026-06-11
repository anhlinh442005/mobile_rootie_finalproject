package com.veganbeauty.app.features.community.com_feed

import android.graphics.Color
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.databinding.ComItemExploreVideoBinding
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent

class ExploreVideoAdapter(
    private var items: List<YtVideoEntity>
) : RecyclerView.Adapter<ExploreVideoAdapter.ExploreVideoViewHolder>() {

    private var currentPlayingHolder: ExploreVideoViewHolder? = null
    var contentTranslationY = 0f

    fun updateData(newItems: List<YtVideoEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreVideoViewHolder {
        val binding = ComItemExploreVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExploreVideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreVideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    override fun onViewAttachedToWindow(holder: ExploreVideoViewHolder) {
        super.onViewAttachedToWindow(holder)
        // Video playing is managed by ViewPager2 page change callback in Fragment
    }

    override fun onViewDetachedFromWindow(holder: ExploreVideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.stopVideo()
        if (currentPlayingHolder == holder) {
            currentPlayingHolder = null
        }
    }

    inner class ExploreVideoViewHolder(val itemBinding: ComItemExploreVideoBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        private var mediaPlayer: MediaPlayer? = null
        private var isPlayRequested = false
        private var isLiked = false
        private var isFollowing = false
        private var discAnimator: ObjectAnimator? = null

        @SuppressLint("ClickableViewAccessibility")
        fun bind(video: YtVideoEntity) {
            isPlayRequested = false
            itemBinding.llLeftContent.translationY = contentTranslationY
            itemBinding.llRightIcons.translationY = contentTranslationY

            itemBinding.tvAuthorName.text = video.username
            itemBinding.tvCaption.text = video.description
            itemBinding.tvCaption.maxLines = 3
            itemBinding.tvReadMore.visibility = View.GONE

            itemBinding.tvCaption.post {
                val layout = itemBinding.tvCaption.layout
                if (layout != null) {
                    val lines = layout.lineCount
                    if (lines > 0 && layout.getEllipsisCount(lines - 1) > 0) {
                        itemBinding.tvReadMore.visibility = View.VISIBLE
                    }
                }
            }

            itemBinding.tvReadMore.setOnClickListener {
                itemBinding.tvCaption.maxLines = Integer.MAX_VALUE
                itemBinding.tvReadMore.visibility = View.GONE
            }

            itemBinding.tvCaption.setOnClickListener {
                if (itemBinding.tvReadMore.visibility == View.GONE && itemBinding.tvCaption.maxLines == Integer.MAX_VALUE) {
                    itemBinding.tvCaption.maxLines = 3
                    itemBinding.tvCaption.post {
                        val layout = itemBinding.tvCaption.layout
                        if (layout != null && layout.lineCount > 0 && layout.getEllipsisCount(layout.lineCount - 1) > 0) {
                            itemBinding.tvReadMore.visibility = View.VISIBLE
                        }
                    }
                }
            }

            itemBinding.tvLikesCount.text = formatCount(video.likesCount)
            itemBinding.tvCommentsCount.text = formatCount(video.commentsCount)
            itemBinding.tvShareCount.text = formatCount(video.shareCount)
            itemBinding.tvBookmarkCount.text = formatCount((video.likesCount * 0.1).toInt())

            if (!video.avatarUrl.isNullOrEmpty()) {
                itemBinding.ivAuthorAvatarRight.load(video.avatarUrl) {
                    decoderFactory(SvgDecoder.Factory())
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                    error(R.drawable.logo)
                }
            } else {
                itemBinding.ivAuthorAvatarRight.setImageResource(android.R.color.darker_gray)
            }

            // Disc rotation
            discAnimator = ObjectAnimator.ofFloat(itemBinding.ivDisc, View.ROTATION, 0f, 360f).apply {
                duration = 3000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
            }

            itemBinding.ivLike.setOnClickListener {
                isLiked = !isLiked
                if (isLiked) {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                    itemBinding.ivLike.setColorFilter(Color.parseColor("#E53935"))
                    itemBinding.tvLikesCount.text = formatCount(video.likesCount + 1)
                } else {
                    itemBinding.ivLike.setImageResource(R.drawable.ic_heart_outline)
                    itemBinding.ivLike.setColorFilter(Color.WHITE)
                    itemBinding.tvLikesCount.text = formatCount(video.likesCount)
                }
            }

            itemBinding.ivAddFollow.setOnClickListener {
                isFollowing = !isFollowing
                if (isFollowing) {
                    itemBinding.ivAddFollow.visibility = View.GONE
                } else {
                    itemBinding.ivAddFollow.visibility = View.VISIBLE
                }
            }

            val gestureDetector = GestureDetector(itemBinding.root.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (itemBinding.videoView.isPlaying) {
                        itemBinding.videoView.pause()
                        discAnimator?.pause()
                    } else {
                        itemBinding.videoView.start()
                        if (discAnimator?.isStarted == true) {
                            discAnimator?.resume()
                        } else {
                            discAnimator?.start()
                        }
                    }
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    itemBinding.ivBigHeart.visibility = View.VISIBLE
                    itemBinding.ivBigHeart.alpha = 0f
                    itemBinding.ivBigHeart.scaleX = 0f
                    itemBinding.ivBigHeart.scaleY = 0f
                    itemBinding.ivBigHeart.translationX = 0f
                    itemBinding.ivBigHeart.translationY = 0f

                    itemBinding.ivBigHeart.animate()
                        .alpha(1f)
                        .scaleX(1.5f)
                        .scaleY(1.5f)
                        .setDuration(200)
                        .withEndAction {
                            itemBinding.ivBigHeart.animate()
                                .alpha(0f)
                                .translationYBy(-150f)
                                .scaleX(2f)
                                .scaleY(2f)
                                .setDuration(400)
                                .withEndAction {
                                    itemBinding.ivBigHeart.visibility = View.INVISIBLE
                                }
                                .start()
                        }
                        .start()

                    if (!isLiked) {
                        isLiked = true
                        itemBinding.ivLike.setImageResource(R.drawable.ic_heart_filled)
                        itemBinding.ivLike.setColorFilter(Color.parseColor("#E53935"))
                        itemBinding.tvLikesCount.text = formatCount(video.likesCount + 1)
                    }
                    return true
                }
            })

            itemBinding.viewClickZone.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            itemBinding.videoView.setZOrderMediaOverlay(true)
            itemBinding.videoView.setVideoPath(video.url)

            itemBinding.videoView.setOnPreparedListener { mp ->
                mediaPlayer = mp
                itemBinding.pbLoading.visibility = View.GONE
                mp.isLooping = true
                
                // Áp dụng tỷ lệ Center Crop để video lấp đầy chiều ngang (fit width)
                val videoWidth = mp.videoWidth.toFloat()
                val videoHeight = mp.videoHeight.toFloat()
                val viewWidth = itemBinding.videoView.width.toFloat()
                val viewHeight = itemBinding.videoView.height.toFloat()
                
                if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                    val videoRatio = videoWidth / videoHeight
                    val viewRatio = viewWidth / viewHeight
                    
                    if (videoRatio > viewRatio) {
                        // Video rộng hơn màn hình -> scale chiều ngang
                        itemBinding.videoView.scaleX = videoRatio / viewRatio
                        itemBinding.videoView.scaleY = 1f
                    } else {
                        // Video cao hơn màn hình -> scale chiều dọc để khớp chiều ngang
                        itemBinding.videoView.scaleX = 1f
                        itemBinding.videoView.scaleY = viewRatio / videoRatio
                    }
                }

                if (isPlayRequested) {
                    itemBinding.videoView.start()
                    if (discAnimator?.isStarted == true) {
                        discAnimator?.resume()
                    } else {
                        discAnimator?.start()
                    }
                }
            }

            itemBinding.videoView.setOnErrorListener { _, _, _ ->
                itemBinding.pbLoading.visibility = View.GONE
                true
            }
        }

        fun playVideo() {
            isPlayRequested = true
            if (mediaPlayer != null) {
                itemBinding.videoView.start()
                if (discAnimator?.isStarted == true) {
                    discAnimator?.resume()
                } else {
                    discAnimator?.start()
                }
            } else {
                itemBinding.pbLoading.visibility = View.VISIBLE
            }
        }

        fun stopVideo() {
            isPlayRequested = false
            try {
                if (itemBinding.videoView.isPlaying) {
                    itemBinding.videoView.pause()
                }
                itemBinding.videoView.seekTo(0)
                discAnimator?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun animateContent(translationY: Float) {
            itemBinding.llLeftContent.animate().translationY(translationY).setDuration(250).start()
            itemBinding.llRightIcons.animate().translationY(translationY).setDuration(250).start()
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
}
