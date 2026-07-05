package com.veganbeauty.app.features.community.com_feed;

import android.animation.ValueAnimator;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.utils.ExploreVideoCache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExploreVideoAdapter extends RecyclerView.Adapter<ExploreVideoAdapter.VideoViewHolder> {

    private final List<YtVideoEntity> videos = new ArrayList<>();
    private final OnVideoInteractionListener listener;
    private final Set<String> likedVideoIds = new HashSet<>();
    private final Set<String> savedVideoIds = new HashSet<>();
    private final Set<String> expandedDescIds = new HashSet<>();

    private int currentPosition = 0;
    private int navVisibleBottomMarginPx;
    private int navHiddenBottomMarginPx;
    private boolean isNavBarVisible = true;

    public ExploreVideoAdapter(List<YtVideoEntity> initialVideos, OnVideoInteractionListener listener) {
        if (initialVideos != null) {
            this.videos.addAll(initialVideos);
        }
        this.listener = listener;
    }

    public void setBottomMargins(int navVisibleMarginPx, int navHiddenMarginPx) {
        this.navVisibleBottomMarginPx = navVisibleMarginPx;
        this.navHiddenBottomMarginPx = navHiddenMarginPx;
    }

    public void setNavBarVisible(boolean visible, boolean animate) {
        if (isNavBarVisible == visible) return;
        isNavBarVisible = visible;
        int targetMargin = visible ? navVisibleBottomMarginPx : navHiddenBottomMarginPx;
        applyBottomMarginToVisibleHolders(targetMargin, animate);
    }

    public void setCurrentPosition(int position) {
        currentPosition = position;
    }

    public void updateData(List<YtVideoEntity> newVideos) {
        this.videos.clear();
        if (newVideos != null) {
            this.videos.addAll(newVideos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_explore_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.bind(videos.get(position), position == currentPosition);
        holder.applyBottomMargin(isNavBarVisible ? navVisibleBottomMarginPx : navHiddenBottomMarginPx, false);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.getBindingAdapterPosition() == currentPosition) {
            holder.playVideo();
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pauseVideo();
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releaseVideo();
    }

    private void applyBottomMarginToVisibleHolders(int targetMarginPx, boolean animate) {
        RecyclerView recyclerView = attachedRecyclerView;
        if (recyclerView == null) return;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof VideoViewHolder) {
                ((VideoViewHolder) holder).applyBottomMargin(targetMarginPx, animate);
            }
        }
    }

    private RecyclerView attachedRecyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        attachedRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null;
        }
    }

    public interface OnVideoInteractionListener {
        void onLikeClick(YtVideoEntity video, boolean isLiked);
        void onCommentClick(YtVideoEntity video);
        void onShareClick(YtVideoEntity video);
        void onSaveClick(YtVideoEntity video, boolean isSaved);
        void onProfileClick(String username);
        void onProductClick(YtVideoEntity video);
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {

        private final VideoView videoView;
        private final ProgressBar progressBar;
        private final TextView tvOfflineHint;
        private final View clickZone;
        private final LinearLayout llLeftContent;
        private final LinearLayout llRightIcons;
        private final TextView tvUsername;
        private final TextView tvDescription;
        private final ImageView ivAvatar;
        private final TextView tvLikeCount;
        private final TextView tvCommentCount;
        private final TextView tvSaveCount;
        private final TextView tvShareCount;
        private final ImageView ivLike;
        private final ImageView ivBigHeart;

        private final Handler doubleTapHandler = new Handler(Looper.getMainLooper());
        private boolean isDoubleTapping = false;
        private boolean shouldAutoPlay = false;
        private boolean isPrepared = false;
        private ValueAnimator marginAnimator;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            progressBar = itemView.findViewById(R.id.pbLoading);
            tvOfflineHint = itemView.findViewById(R.id.tvOfflineHint);
            clickZone = itemView.findViewById(R.id.viewClickZone);
            llLeftContent = itemView.findViewById(R.id.llLeftContent);
            llRightIcons = itemView.findViewById(R.id.llRightIcons);
            tvUsername = itemView.findViewById(R.id.tvAuthorName);
            tvDescription = itemView.findViewById(R.id.tvCaption);
            ivAvatar = itemView.findViewById(R.id.ivAuthorAvatarRight);
            tvLikeCount = itemView.findViewById(R.id.tvLikesCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentsCount);
            tvSaveCount = itemView.findViewById(R.id.tvBookmarkCount);
            tvShareCount = itemView.findViewById(R.id.tvShareCount);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivBigHeart = itemView.findViewById(R.id.ivBigHeart);
        }

        public void bind(YtVideoEntity video, boolean autoPlay) {
            shouldAutoPlay = autoPlay;
            isPrepared = false;
            setupVideoSource(video);
            setupUserInfo(video);
            setupEngagementStats(video);
            setupInteractions(video);
            setupDescription(video);
        }

        void applyBottomMargin(int targetMarginPx, boolean animate) {
            if (llLeftContent == null || llRightIcons == null) return;

            ViewGroup.MarginLayoutParams leftParams = (ViewGroup.MarginLayoutParams) llLeftContent.getLayoutParams();
            ViewGroup.MarginLayoutParams rightParams = (ViewGroup.MarginLayoutParams) llRightIcons.getLayoutParams();
            int currentMargin = leftParams.bottomMargin;

            if (!animate || currentMargin == targetMarginPx) {
                if (marginAnimator != null) {
                    marginAnimator.cancel();
                    marginAnimator = null;
                }
                leftParams.bottomMargin = targetMarginPx;
                rightParams.bottomMargin = targetMarginPx;
                llLeftContent.setLayoutParams(leftParams);
                llRightIcons.setLayoutParams(rightParams);
                return;
            }

            if (marginAnimator != null) {
                marginAnimator.cancel();
            }

            marginAnimator = ValueAnimator.ofInt(currentMargin, targetMarginPx);
            marginAnimator.setDuration(250);
            marginAnimator.setInterpolator(new DecelerateInterpolator());
            marginAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                leftParams.bottomMargin = value;
                rightParams.bottomMargin = value;
                llLeftContent.setLayoutParams(leftParams);
                llRightIcons.setLayoutParams(rightParams);
            });
            marginAnimator.start();
        }

        private void setupVideoSource(YtVideoEntity video) {
            String url = video.getUrl() != null ? video.getUrl() : "";
            progressBar.setVisibility(View.VISIBLE);
            tvOfflineHint.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            releaseVideo();

            if (!url.toLowerCase().contains("cloudinary") || !url.toLowerCase().endsWith(".mp4")) {
                progressBar.setVisibility(View.GONE);
                videoView.setVisibility(View.GONE);
                tvOfflineHint.setVisibility(View.VISIBLE);
                tvOfflineHint.setText("Video không khả dụng.");
                return;
            }

            boolean hasNetwork = ExploreVideoCache.isNetworkAvailable(itemView.getContext());
            boolean hasCache = ExploreVideoCache.isCached(itemView.getContext(), url);
            if (!hasNetwork && !hasCache) {
                progressBar.setVisibility(View.GONE);
                videoView.setVisibility(View.GONE);
                tvOfflineHint.setVisibility(View.VISIBLE);
                return;
            }

            Uri playableUri = ExploreVideoCache.getPlayableUri(itemView.getContext(), url);
            if (hasNetwork) {
                ExploreVideoCache.prefetch(itemView.getContext(), url);
            }

            videoView.setVideoURI(playableUri);
            videoView.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.setLooping(true);
                progressBar.setVisibility(View.GONE);
                fitVideoToScreen(mp);
                if (shouldAutoPlay && getBindingAdapterPosition() == currentPosition) {
                    videoView.start();
                }
            });

            videoView.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    progressBar.setVisibility(View.GONE);
                    return true;
                }
                return false;
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                progressBar.setVisibility(View.GONE);
                if (!ExploreVideoCache.isCached(itemView.getContext(), url) && !ExploreVideoCache.isNetworkAvailable(itemView.getContext())) {
                    videoView.setVisibility(View.GONE);
                    tvOfflineHint.setVisibility(View.VISIBLE);
                }
                return true;
            });
        }

        private void fitVideoToScreen(MediaPlayer mp) {
            if (videoView.getWidth() <= 0 || videoView.getHeight() <= 0) return;
            float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
            float screenRatio = videoView.getWidth() / (float) videoView.getHeight();
            float scaleX = videoRatio / screenRatio;
            if (scaleX >= 1f) {
                videoView.setScaleX(scaleX);
                videoView.setScaleY(1f);
            } else {
                videoView.setScaleX(1f);
                videoView.setScaleY(1f / scaleX);
            }
        }

        private void setupUserInfo(YtVideoEntity video) {
            String username = video.getUsername() != null ? video.getUsername().trim() : "";
            tvUsername.setText(username.isEmpty() ? "@rootie_community" : "@" + username.replace(" ", ""));

            int[] avatarRes = {R.drawable.img_avatar, R.drawable.ic_account_outline, R.drawable.img_avatar, R.drawable.ic_account_outline};
            int randomAvatar = avatarRes[(int) (Math.random() * avatarRes.length)];

            String avatarUrl = video.getAvatarUrl() == null || video.getAvatarUrl().trim().isEmpty()
                    ? String.valueOf(randomAvatar) : video.getAvatarUrl();

            com.bumptech.glide.Glide.with(ivAvatar.getContext())
                    .load(avatarUrl.matches("\\d+") ? Integer.parseInt(avatarUrl) : avatarUrl)
                    .circleCrop()
                    .into(ivAvatar);
        }

        private void setupDescription(YtVideoEntity video) {
            String title = video.getTitle() != null ? video.getTitle() : "";
            String desc = video.getDescription() != null ? video.getDescription() : "";
            String hashtags = video.getHashtags() != null ? video.getHashtags() : "";
            String fullDesc = title + "\n" + desc + "\n" + hashtags;

            if (expandedDescIds.contains(video.getId())) {
                tvDescription.setMaxLines(Integer.MAX_VALUE);
            } else {
                tvDescription.setMaxLines(2);
            }
            tvDescription.setText(fullDesc);

            tvDescription.setOnClickListener(v -> {
                if (expandedDescIds.contains(video.getId())) {
                    expandedDescIds.remove(video.getId());
                    tvDescription.setMaxLines(2);
                } else {
                    expandedDescIds.add(video.getId());
                    tvDescription.setMaxLines(Integer.MAX_VALUE);
                }
            });
        }

        private void setupEngagementStats(YtVideoEntity video) {
            boolean isLiked = likedVideoIds.contains(video.getId());
            boolean isSaved = savedVideoIds.contains(video.getId());

            int displayLikes = video.getLikesCount() + (isLiked ? 1 : 0);
            int displaySaves = (video.getLikesCount() / 5) + (isSaved ? 1 : 0);

            tvLikeCount.setText(formatCount(displayLikes));
            tvCommentCount.setText(formatCount(video.getLikesCount() / 10));
            tvSaveCount.setText(formatCount(displaySaves));
            tvShareCount.setText(formatCount(video.getLikesCount() / 20));
        }

        private String formatCount(int count) {
            if (count >= 1000000) {
                return String.format("%.1fM", count / 1000000f);
            } else if (count >= 1000) {
                return String.format("%.1fK", count / 1000f);
            } else {
                return String.valueOf(count);
            }
        }

        private void setupInteractions(YtVideoEntity video) {
            if (clickZone == null) return;
            clickZone.setOnClickListener(v -> {
                if (isDoubleTapping) {
                    handleDoubleTap(video);
                    isDoubleTapping = false;
                    doubleTapHandler.removeCallbacksAndMessages(null);
                } else {
                    isDoubleTapping = true;
                    doubleTapHandler.postDelayed(() -> {
                        isDoubleTapping = false;
                        togglePlayPause();
                    }, 300);
                }
            });
        }

        private void handleDoubleTap(YtVideoEntity video) {
            if (!likedVideoIds.contains(video.getId())) {
                likedVideoIds.add(video.getId());
                setupEngagementStats(video);
                if (ivLike != null) {
                    ivLike.setImageResource(R.drawable.ic_heart_filled);
                }
                if (ivBigHeart != null) {
                    ivBigHeart.setVisibility(View.VISIBLE);
                    ivBigHeart.setAlpha(1f);
                    ivBigHeart.animate().alpha(0f).setDuration(600).withEndAction(() ->
                            ivBigHeart.setVisibility(View.INVISIBLE)).start();
                }
                if (listener != null) listener.onLikeClick(video, true);
            }
        }

        private void togglePlayPause() {
            if (videoView.getVisibility() != View.VISIBLE) return;
            if (videoView.isPlaying()) {
                pauseVideo();
            } else {
                playVideo();
            }
        }

        public void playVideo() {
            shouldAutoPlay = true;
            if (videoView.getVisibility() != View.VISIBLE) return;
            if (isPrepared && !videoView.isPlaying()) {
                videoView.start();
            } else if (!isPrepared) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        public void pauseVideo() {
            shouldAutoPlay = false;
            if (videoView.isPlaying()) {
                videoView.pause();
            }
        }

        public void releaseVideo() {
            try {
                videoView.stopPlayback();
            } catch (Exception ignored) {
            }
            isPrepared = false;
            videoView.setOnPreparedListener(null);
            videoView.setOnErrorListener(null);
            videoView.setOnInfoListener(null);
        }
    }
}
