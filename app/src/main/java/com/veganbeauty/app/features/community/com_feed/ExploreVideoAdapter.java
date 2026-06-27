package com.veganbeauty.app.features.community.com_feed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExploreVideoAdapter extends RecyclerView.Adapter<ExploreVideoAdapter.VideoViewHolder> {

    private final List<YtVideoEntity> videos = new ArrayList<>();
    private final OnVideoInteractionListener listener;
    private final Set<String> likedVideoIds = new HashSet<>();
    private final Set<String> savedVideoIds = new HashSet<>();
    private final Set<String> expandedDescIds = new HashSet<>();

    public ExploreVideoAdapter(List<YtVideoEntity> initialVideos, OnVideoInteractionListener listener) {
        if (initialVideos != null) {
            this.videos.addAll(initialVideos);
        }
        this.listener = listener;
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
        YtVideoEntity video = videos.get(position);
        holder.bind(video);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.pauseVideo();
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
        private final ImageView ivThumbnail;
        private final ProgressBar progressBar;
        private final ImageView ivPlayPause;
        private final FrameLayout videoContainer;

        private final TextView tvUsername;
        private final TextView tvDescription;
        private final TextView tvMusic;
        private final ImageView ivAvatar;
        private final LinearLayout llProfileInfo;
        private final TextView tvFollowBtn;

        private final LinearLayout llLike;
        private final ImageView ivLike;
        private final TextView tvLikeCount;
        private final LinearLayout llComment;
        private final TextView tvCommentCount;
        private final LinearLayout llSave;
        private final ImageView ivSave;
        private final TextView tvSaveCount;
        private final LinearLayout llShare;
        private final TextView tvShareCount;
        private final LinearLayout llMore;

        private final LinearLayout llProductLink;
        private final TextView tvProductName;
        private final ImageView ivProductImage;

        private boolean isPlaying = false;
        private final Handler doubleTapHandler = new Handler(Looper.getMainLooper());
        private boolean isDoubleTapping = false;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            progressBar = itemView.findViewById(R.id.progressBar);
            ivPlayPause = itemView.findViewById(R.id.ivPlayPause);
            videoContainer = itemView.findViewById(R.id.videoContainer);

            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvMusic = itemView.findViewById(R.id.tvMusic);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            llProfileInfo = itemView.findViewById(R.id.llProfileInfo);
            tvFollowBtn = itemView.findViewById(R.id.tvFollowBtn);

            llLike = itemView.findViewById(R.id.llLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            llComment = itemView.findViewById(R.id.llComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            llSave = itemView.findViewById(R.id.llSave);
            ivSave = itemView.findViewById(R.id.ivSave);
            tvSaveCount = itemView.findViewById(R.id.tvSaveCount);
            llShare = itemView.findViewById(R.id.llShare);
            tvShareCount = itemView.findViewById(R.id.tvShareCount);
            llMore = itemView.findViewById(R.id.llMore);

            llProductLink = itemView.findViewById(R.id.llProductLink);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
        }

        public void bind(YtVideoEntity video) {
            setupVideoSource(video);
            setupUserInfo(video);
            setupEngagementStats(video);
            setupProductLink(video);
            setupInteractions(video);
            setupDescription(video);
        }

        private void setupVideoSource(YtVideoEntity video) {
            progressBar.setVisibility(View.VISIBLE);
            ivThumbnail.setVisibility(View.VISIBLE);
            ivPlayPause.setVisibility(View.GONE);

            String thumbUrl = getThumbnailUrl(video);
            ImageRequest thumbRequest = new ImageRequest.Builder(itemView.getContext())
                    .data(thumbUrl)
                    .crossfade(true)
                    .target(ivThumbnail)
                    .build();
            Coil.imageLoader(itemView.getContext()).enqueue(thumbRequest);

            if (video.getUrl().toLowerCase().contains("cloudinary") && video.getUrl().toLowerCase().endsWith(".mp4")) {
                videoView.setVideoURI(Uri.parse(video.getUrl()));
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    progressBar.setVisibility(View.GONE);
                    float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
                    float screenRatio = videoView.getWidth() / (float) videoView.getHeight();
                    float scaleX = videoRatio / screenRatio;
                    if (scaleX >= 1f) {
                        videoView.setScaleX(scaleX);
                    } else {
                        videoView.setScaleY(1f / scaleX);
                    }
                });

                videoView.setOnInfoListener((mp, what, extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        ivThumbnail.setVisibility(View.GONE);
                        return true;
                    }
                    return false;
                });
                
                videoView.setOnErrorListener((mp, what, extra) -> {
                    progressBar.setVisibility(View.GONE);
                    return true;
                });

            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        private String getThumbnailUrl(YtVideoEntity video) {
            if (video.getUrl().toLowerCase().contains("cloudinary") && video.getUrl().toLowerCase().endsWith(".mp4")) {
                return video.getUrl().replaceAll("(?i)\\.mp4$", ".jpg");
            }

            String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(video.getUrl());
            if (matcher.find()) {
                String videoId = matcher.group();
                return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            }
            return video.getUrl();
        }

        private void setupUserInfo(YtVideoEntity video) {
            tvUsername.setText(video.getUsername().trim().isEmpty() ? "@rootie_community" : "@" + video.getUsername().replace(" ", ""));

            int[] avatarRes = {R.drawable.img_avatar, R.drawable.ic_user_outline, R.drawable.img_avatar, R.drawable.ic_user_outline};
            int randomAvatar = avatarRes[(int) (Math.random() * avatarRes.length)];

            String avatarUrl = video.getAvatarUrl() == null || video.getAvatarUrl().trim().isEmpty() ? String.valueOf(randomAvatar) : video.getAvatarUrl();

            ImageRequest avatarRequest = new ImageRequest.Builder(itemView.getContext())
                    .data(avatarUrl.matches("\\d+") ? Integer.parseInt(avatarUrl) : avatarUrl)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .target(ivAvatar)
                    .build();
            Coil.imageLoader(itemView.getContext()).enqueue(avatarRequest);

            tvMusic.setText("♬ Nhạc nền gốc - " + (video.getUsername().trim().isEmpty() ? "Rootie" : video.getUsername()));
            tvMusic.setSelected(true);
        }

        private void setupDescription(YtVideoEntity video) {
            String fullDesc = video.getTitle() + "\n" + video.getDescription() + "\n" + video.getHashtags();
            if (expandedDescIds.contains(video.getId())) {
                tvDescription.setMaxLines(Integer.MAX_VALUE);
                tvDescription.setText(fullDesc);
            } else {
                tvDescription.setMaxLines(2);
                tvDescription.setText(fullDesc);
            }

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

            updateLikeUI(isLiked);
            updateSaveUI(isSaved);
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

        private void setupProductLink(YtVideoEntity video) {
            if (video.getHashtags().contains("review") || video.getHashtags().contains("skincare") || video.getTitle().contains("serum")) {
                llProductLink.setVisibility(View.VISIBLE);
                tvProductName.setText("Serum phục hồi da nhạy cảm " + (video.getUsername().trim().isEmpty() ? "B5" : video.getUsername()));

                ImageRequest prodRequest = new ImageRequest.Builder(itemView.getContext())
                        .data(R.drawable.img_product_placeholder_1)
                        .crossfade(true)
                        .target(ivProductImage)
                        .build();
                Coil.imageLoader(itemView.getContext()).enqueue(prodRequest);

                llProductLink.setOnClickListener(v -> {
                    if (listener != null) listener.onProductClick(video);
                });
            } else {
                llProductLink.setVisibility(View.GONE);
            }
        }

        private void setupInteractions(YtVideoEntity video) {
            videoContainer.setOnClickListener(v -> {
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

            llProfileInfo.setOnClickListener(v -> {
                if (listener != null) listener.onProfileClick(video.getUsername());
            });

            tvFollowBtn.setOnClickListener(v -> {
                tvFollowBtn.setVisibility(View.GONE);
            });

            llLike.setOnClickListener(v -> {
                boolean currentlyLiked = likedVideoIds.contains(video.getId());
                if (currentlyLiked) {
                    likedVideoIds.remove(video.getId());
                } else {
                    likedVideoIds.add(video.getId());
                }
                updateLikeUI(!currentlyLiked);
                setupEngagementStats(video);
                if (listener != null) listener.onLikeClick(video, !currentlyLiked);
            });

            llComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(video);
            });

            llShare.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(video);
            });

            llSave.setOnClickListener(v -> {
                boolean currentlySaved = savedVideoIds.contains(video.getId());
                if (currentlySaved) {
                    savedVideoIds.remove(video.getId());
                } else {
                    savedVideoIds.add(video.getId());
                }
                updateSaveUI(!currentlySaved);
                setupEngagementStats(video);
                if (listener != null) listener.onSaveClick(video, !currentlySaved);
            });
        }

        private void handleDoubleTap(YtVideoEntity video) {
            if (!likedVideoIds.contains(video.getId())) {
                likedVideoIds.add(video.getId());
                updateLikeUI(true);
                setupEngagementStats(video);
                if (listener != null) listener.onLikeClick(video, true);
            }

            final ImageView heartAnim = itemView.findViewById(R.id.ivHeartAnim);
            heartAnim.setVisibility(View.VISIBLE);
            heartAnim.setScaleX(0.5f);
            heartAnim.setScaleY(0.5f);
            heartAnim.setAlpha(1f);

            heartAnim.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        heartAnim.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(0f)
                                .setDuration(300)
                                .setStartDelay(200)
                                .withEndAction(() -> heartAnim.setVisibility(View.GONE))
                                .start();
                    })
                    .start();
        }

        private void togglePlayPause() {
            if (videoView.isPlaying()) {
                pauseVideo();
            } else {
                playVideo();
            }
        }

        public void playVideo() {
            if (!videoView.isPlaying() && videoView.getVisibility() == View.VISIBLE) {
                videoView.start();
                isPlaying = true;
                ivPlayPause.setVisibility(View.GONE);
            }
        }

        public void pauseVideo() {
            if (videoView.isPlaying()) {
                videoView.pause();
                isPlaying = false;
                showPlayPauseIcon();
            }
        }

        private void showPlayPauseIcon() {
            ivPlayPause.setVisibility(View.VISIBLE);
            ivPlayPause.setAlpha(1f);
            ivPlayPause.setScaleX(0.5f);
            ivPlayPause.setScaleY(0.5f);

            ivPlayPause.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start();
        }

        private void updateLikeUI(boolean isLiked) {
            if (isLiked) {
                ivLike.setImageResource(R.drawable.ic_heart_filled);
                ivLike.setColorFilter(Color.parseColor("#FF3B30"));
            } else {
                ivLike.setImageResource(R.drawable.ic_heart_outline);
                ivLike.setColorFilter(Color.WHITE);
            }
        }

        private void updateSaveUI(boolean isSaved) {
            if (isSaved) {
                ivSave.setImageResource(R.drawable.ic_bookmark_filled);
                ivSave.setColorFilter(Color.parseColor("#FDB913"));
            } else {
                ivSave.setImageResource(R.drawable.ic_bookmark_outline);
                ivSave.setColorFilter(Color.WHITE);
            }
        }
    }
}
