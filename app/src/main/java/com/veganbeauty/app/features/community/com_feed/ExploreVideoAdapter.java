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

    public void setSavedVideoIds(Set<String> ids) {
        savedVideoIds.clear();
        if (ids != null) {
            savedVideoIds.addAll(ids);
        }
        notifyDataSetChanged();
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
        private final ImageView ivComment;
        private final ImageView ivBigHeart;
        private final ImageView ivShare;

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
            ivComment = itemView.findViewById(R.id.ivComment);
            ivBigHeart = itemView.findViewById(R.id.ivBigHeart);
            ivShare = itemView.findViewById(R.id.ivShare);
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

            int displayLikes = (isLiked ? 1 : 0);
            int displayComments = getRealCommentCount(video.getId());
            int displaySaves = (isSaved ? 1 : 0);
            int displayShares = 0;

            tvLikeCount.setText(String.valueOf(displayLikes));
            tvCommentCount.setText(String.valueOf(displayComments));
            tvSaveCount.setText(String.valueOf(displaySaves));
            tvShareCount.setText(String.valueOf(displayShares));

            // Update icon states
            if (ivLike != null) {
                ivLike.setImageResource(R.drawable.ic_heart_filled);
                if (isLiked) {
                    ivLike.setColorFilter(android.graphics.Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    ivLike.clearColorFilter();
                }
            }
            ImageView ivSave = itemView.findViewById(R.id.ivSave);
            if (ivSave != null) {
                ivSave.setImageResource(R.drawable.ic_save_full);
                if (isSaved) {
                    ivSave.setColorFilter(android.graphics.Color.parseColor("#EEDB5B"), android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    ivSave.clearColorFilter();
                }
            }
        }

        private int getRealCommentCount(String videoId) {
            try {
                java.io.File file = new java.io.File(itemView.getContext().getFilesDir(), "local_comments.json");
                if (!file.exists()) return 0;
                
                StringBuilder sb = new StringBuilder();
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                
                org.json.JSONArray allComments = new org.json.JSONArray(sb.toString());
                int count = 0;
                for (int i = 0; i < allComments.length(); i++) {
                    org.json.JSONObject comment = allComments.getJSONObject(i);
                    if (videoId.equals(comment.optString("post_id"))) {
                        count++;
                    }
                }
                return count;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
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
            if (ivShare != null) {
                ivShare.setOnClickListener(v -> {
                    android.content.Context ctx = itemView.getContext();
                    if (ctx instanceof androidx.appcompat.app.AppCompatActivity) {
                        androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) ctx;
                        CommunityShareBottomSheet shareSheet = CommunityShareBottomSheet.newInstance(video.getId(), video.getUrl());
                        shareSheet.show(activity.getSupportFragmentManager(), CommunityShareBottomSheet.TAG);
                    }
                });

                ivShare.setOnLongClickListener(v -> {
                    android.content.Context ctx = itemView.getContext();
                    View popupView = LayoutInflater.from(ctx).inflate(R.layout.com_popup_share_video, null);
                    android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

                    popupWindow.setElevation(10f);
                    popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                    TextView tvRepostVideo = popupView.findViewById(R.id.tvRepostVideo);
                    TextView tvViewReposts = popupView.findViewById(R.id.tvViewReposts);

                    tvRepostVideo.setOnClickListener(v1 -> {
                        popupWindow.dismiss();
                        if (listener != null) listener.onShareClick(video);
                        android.widget.Toast.makeText(ctx, "Đã đăng lại video", android.widget.Toast.LENGTH_SHORT).show();
                    });

                    tvViewReposts.setOnClickListener(v2 -> {
                        popupWindow.dismiss();
                        if (itemView.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                            androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) itemView.getContext();
                            ExploreSearchFragment fragment = new ExploreSearchFragment();
                            android.os.Bundle args = new android.os.Bundle();
                            args.putBoolean("SAVED_MODE", true);
                            fragment.setArguments(args);
                            activity.getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });

                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int popupWidth = popupView.getMeasuredWidth();
                    int xOffset = -(popupWidth - ivShare.getWidth()); 
                    int yOffset = 0; 
                    popupWindow.showAsDropDown(ivShare, xOffset, yOffset);

                    return true;
                });
            }

            if (ivLike != null) {
                ivLike.setOnClickListener(v -> {
                    boolean isLiked = likedVideoIds.contains(video.getId());
                    if (isLiked) {
                        likedVideoIds.remove(video.getId());
                    } else {
                        likedVideoIds.add(video.getId());
                        if (ivBigHeart != null) {
                            ivBigHeart.setVisibility(View.VISIBLE);
                            ivBigHeart.setAlpha(1f);
                            ivBigHeart.animate().alpha(0f).setDuration(600).withEndAction(() ->
                                    ivBigHeart.setVisibility(View.INVISIBLE)).start();
                        }
                    }
                    setupEngagementStats(video);
                    if (listener != null) listener.onLikeClick(video, !isLiked);
                });
            }

            if (clickZone != null) {
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

            if (ivComment != null) {
                ivComment.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                        androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) itemView.getContext();
                        CommunityCommentBottomSheet bottomSheet = CommunityCommentBottomSheet.newInstance(video.getId(), getRealCommentCount(video.getId()));
                        bottomSheet.setOnDismissListener(() -> {
                            setupEngagementStats(video);
                        });
                        bottomSheet.show(activity.getSupportFragmentManager(), CommunityCommentBottomSheet.TAG);
                    }
                    if (listener != null) listener.onCommentClick(video);
                });
            }

            ImageView ivSave = itemView.findViewById(R.id.ivSave);
            if (ivSave != null) {
                ivSave.setOnClickListener(v -> {
                    boolean isSaved = savedVideoIds.contains(video.getId());
                    if (isSaved) {
                        savedVideoIds.remove(video.getId());
                    } else {
                        savedVideoIds.add(video.getId());
                    }
                    setupEngagementStats(video);
                    if (listener != null) listener.onSaveClick(video, !isSaved);
                });

                ivSave.setOnLongClickListener(v -> {
                    android.content.Context ctx = itemView.getContext();
                    View popupView = LayoutInflater.from(ctx).inflate(R.layout.com_popup_save_video, null);
                    android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView,
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

                    // To make elevation/shadow work
                    popupWindow.setElevation(10f);
                    popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

                    TextView tvSaveVideo = popupView.findViewById(R.id.tvSaveVideo);
                    TextView tvViewSaved = popupView.findViewById(R.id.tvViewSaved);

                    if (savedVideoIds.contains(video.getId())) {
                        tvSaveVideo.setText("Bỏ lưu video");
                    } else {
                        tvSaveVideo.setText("Lưu video");
                    }

                    tvSaveVideo.setOnClickListener(v1 -> {
                        popupWindow.dismiss();
                        boolean isSaved = savedVideoIds.contains(video.getId());
                        if (isSaved) {
                            savedVideoIds.remove(video.getId());
                            if (listener != null) listener.onSaveClick(video, false);
                        } else {
                            savedVideoIds.add(video.getId());
                            if (listener != null) listener.onSaveClick(video, true);
                        }
                        setupEngagementStats(video);
                    });

                    tvViewSaved.setOnClickListener(v2 -> {
                        popupWindow.dismiss();
                        if (itemView.getContext() instanceof androidx.fragment.app.FragmentActivity) {
                            androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) itemView.getContext();
                            ExploreSearchFragment fragment = new ExploreSearchFragment();
                            android.os.Bundle args = new android.os.Bundle();
                            args.putBoolean("SAVED_MODE", true);
                            fragment.setArguments(args);
                            activity.getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });

                    // Show it below the icon
                    popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int popupWidth = popupView.getMeasuredWidth();
                    int xOffset = -(popupWidth - ivSave.getWidth()); // align right edge roughly
                    int yOffset = 0; // show below
                    popupWindow.showAsDropDown(ivSave, xOffset, yOffset);

                    return true;
                });
            }
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
