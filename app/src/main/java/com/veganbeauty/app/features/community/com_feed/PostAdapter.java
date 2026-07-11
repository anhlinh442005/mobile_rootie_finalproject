package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;

import com.google.android.material.tabs.TabLayoutMediator;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.databinding.ComItemPostBinding;
import com.veganbeauty.app.databinding.ComItemReelBinding;
import com.veganbeauty.app.databinding.ComItemStoryBinding;
import com.veganbeauty.app.databinding.ComItemSuggestedReelsFeedBinding;
import com.veganbeauty.app.databinding.ComItemSuggestedUsersFeedBinding;
import com.veganbeauty.app.databinding.ComItemSuggestionBinding;
import com.veganbeauty.app.features.community.UserMemoryHelper;
import com.veganbeauty.app.features.community.CommunitySocialHelper;
import com.veganbeauty.app.utils.ComProfileNavigator;
import com.veganbeauty.app.utils.RootieBrandHelper;
import com.veganbeauty.app.utils.TimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

abstract class CommunityFeedItem {
    static class Post extends CommunityFeedItem {
        final CommunityPostEntity post;
        Post(CommunityPostEntity post) { this.post = post; }
    }
    static class SuggestedUsers extends CommunityFeedItem {
        final List<UserEntity> users;
        SuggestedUsers(List<UserEntity> users) { this.users = users; }
    }
    static class SuggestedReels extends CommunityFeedItem {
        final List<ReelEntity> reels;
        SuggestedReels(List<ReelEntity> reels) { this.reels = reels; }
    }
}

class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
    List<String> imageUrls;

    public ImageSliderAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        ImageViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(Color.parseColor("#EEEEEE"));
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);
        try {
            ImageLoader imageLoader = Coil.imageLoader(holder.itemView.getContext());
            ImageRequest.Builder requestBuilder = new ImageRequest.Builder(holder.itemView.getContext())
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .target(holder.imageView);

            if (url.startsWith("content://") || url.startsWith("file://")) {
                requestBuilder.data(Uri.parse(url));
            } else {
                requestBuilder.data(url);
            }
            imageLoader.enqueue(requestBuilder.build());
        } catch (Exception e) {
            holder.imageView.setBackgroundColor(Color.DKGRAY);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public void updateData(List<String> newUrls) {
        this.imageUrls = newUrls;
        notifyDataSetChanged();
    }
}

class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {
    private List<UserEntity> stories;

    public StoryAdapter(List<UserEntity> stories) {
        this.stories = stories;
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        final ComItemStoryBinding binding;
        StoryViewHolder(ComItemStoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ComItemStoryBinding binding = ComItemStoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new StoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        UserEntity story = stories.get(position);
        holder.binding.tvUsername.setText(story.getUsername());

        if (position == 0) {
            holder.binding.ivAdd.setVisibility(View.VISIBLE);
        } else {
            holder.binding.ivAdd.setVisibility(View.GONE);
        }

        if (story.getAvatar() != null && !story.getAvatar().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.binding.ivAvatar.getContext()).load(story.getAvatar()).placeholder(android.R.color.darker_gray).error(R.drawable.img_avatar).into(holder.binding.ivAvatar);
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray);
        }

        if (position == 0) {
            holder.binding.getRoot().setOnClickListener(null);
            holder.binding.ivAvatar.setOnClickListener(null);
            holder.binding.tvUsername.setOnClickListener(null);
        } else {
            View.OnClickListener onStoryProfileClick = v -> ComProfileNavigator.openProfile(
                    v.getContext(),
                    story.getUser_id(),
                    story.getAvatar(),
                    story.getUsername()
            );
            holder.binding.getRoot().setOnClickListener(onStoryProfileClick);
            holder.binding.ivAvatar.setOnClickListener(onStoryProfileClick);
            holder.binding.tvUsername.setOnClickListener(onStoryProfileClick);
        }
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    public void updateData(List<UserEntity> newStories) {
        this.stories = newStories;
        notifyDataSetChanged();
    }
}

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<CommunityFeedItem> items = new ArrayList<>();
    private List<CommunityProduct> globalProducts = new ArrayList<>();
    private final Set<String> followingUserIds = new HashSet<>();
    private OnFollowStateChangedListener followStateChangedListener;

    private static final int VIEW_TYPE_POST = 0;
    private static final int VIEW_TYPE_SUGGESTED_USERS = 1;
    private static final int VIEW_TYPE_SUGGESTED_REELS = 2;

    static class PostViewHolder extends RecyclerView.ViewHolder {
        final ComItemPostBinding binding;
        PostViewHolder(ComItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class SuggestedUsersViewHolder extends RecyclerView.ViewHolder {
        final ComItemSuggestedUsersFeedBinding binding;
        SuggestedUsersViewHolder(ComItemSuggestedUsersFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class SuggestedReelsViewHolder extends RecyclerView.ViewHolder {
        final ComItemSuggestedReelsFeedBinding binding;
        SuggestedReelsViewHolder(ComItemSuggestedReelsFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnFollowStateChangedListener {
        void onFollowStateChanged(String targetUserId, boolean isFollowing);
    }

    public void setOnFollowStateChangedListener(OnFollowStateChangedListener listener) {
        this.followStateChangedListener = listener;
    }

    public void setFollowingUserIds(Set<String> userIds) {
        followingUserIds.clear();
        if (userIds != null) {
            followingUserIds.addAll(userIds);
        }
        notifyDataSetChanged();
    }

    public void updateFollowingState(String targetUserId, boolean isFollowing) {
        if (targetUserId == null || targetUserId.isEmpty()) return;
        if (isFollowing) {
            followingUserIds.add(targetUserId);
        } else {
            followingUserIds.remove(targetUserId);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        CommunityFeedItem item = items.get(position);
        if (item instanceof CommunityFeedItem.Post) return VIEW_TYPE_POST;
        if (item instanceof CommunityFeedItem.SuggestedUsers) return VIEW_TYPE_SUGGESTED_USERS;
        if (item instanceof CommunityFeedItem.SuggestedReels) return VIEW_TYPE_SUGGESTED_REELS;
        return VIEW_TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SUGGESTED_USERS) {
            return new SuggestedUsersViewHolder(ComItemSuggestedUsersFeedBinding.inflate(inflater, parent, false));
        } else if (viewType == VIEW_TYPE_SUGGESTED_REELS) {
            return new SuggestedReelsViewHolder(ComItemSuggestedReelsFeedBinding.inflate(inflater, parent, false));
        } else {
            return new PostViewHolder(ComItemPostBinding.inflate(inflater, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CommunityFeedItem item = items.get(position);
        if (item instanceof CommunityFeedItem.Post) {
            PostViewHolder postHolder = (PostViewHolder) holder;
            CommunityPostEntity post = ((CommunityFeedItem.Post) item).post;

            String authorName = post.getAuthorDisplayName() != null && !post.getAuthorDisplayName().isEmpty() ? post.getAuthorDisplayName() : post.getAuthorUsername();
            authorName = com.veganbeauty.app.utils.IdentityMapper.mapName(holder.itemView.getContext(), post.getAuthorId(), authorName);
            postHolder.binding.tvAuthorName.setText(authorName);

            if ("rootie_official".equals(post.getAuthorId()) || "rootie_vn".equals(post.getAuthorId())) {
                android.graphics.drawable.Drawable verifiedIcon = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_verified);
                if (verifiedIcon != null) {
                    verifiedIcon.setBounds(0, 0, 36, 36);
                    postHolder.binding.tvAuthorName.setCompoundDrawables(null, null, verifiedIcon, null);
                    postHolder.binding.tvAuthorName.setCompoundDrawablePadding(8);
                }

                long timestamp = 0;
                try {
                    timestamp = Long.parseLong(post.getCreatedAt());
                } catch (Exception ignored) {}

                String dateStr = "11 thg 3";
                if (timestamp > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("d 'thg' M", new Locale("vi"));
                    dateStr = sdf.format(new Date(timestamp * 1000));
                }

                String text = dateStr + " •  ";
                SpannableStringBuilder spannable = new SpannableStringBuilder(text);
                android.graphics.drawable.Drawable publicIcon = ContextCompat.getDrawable(
                        holder.itemView.getContext(), R.drawable.ic_public);
                if (publicIcon != null) {
                    publicIcon = publicIcon.mutate();
                    publicIcon.setTint(ContextCompat.getColor(
                            holder.itemView.getContext(), R.color.primary));
                    int iconSize = (int) (14 * holder.itemView.getResources().getDisplayMetrics().density);
                    publicIcon.setBounds(0, 0, iconSize, iconSize);
                    ImageSpan imageSpan = new ImageSpan(publicIcon, ImageSpan.ALIGN_BASELINE);
                    spannable.setSpan(imageSpan, text.length() - 1, text.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                postHolder.binding.tvSkinType.setText(spannable);
                postHolder.binding.tvCreatedAt.setVisibility(View.GONE);
            } else {
                postHolder.binding.tvAuthorName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                List<String> tagList = new ArrayList<>();
                if (post.getSkinType() != null && !post.getSkinType().trim().isEmpty() && !"Không xác định".equals(post.getSkinType())) {
                    tagList.add(post.getSkinType());
                }
                if (post.getConcern() != null && !post.getConcern().trim().isEmpty() && !"Khác".equals(post.getConcern()) && !"Chung".equals(post.getConcern())) {
                    tagList.add(post.getConcern());
                }
                if (post.getType() != null && !post.getType().trim().isEmpty()) {
                    tagList.add(post.getType());
                }
                StringBuilder tags = new StringBuilder();
                for (int i = 0; i < tagList.size(); i++) {
                    tags.append(tagList.get(i));
                    if (i < tagList.size() - 1) tags.append(" • ");
                }
                postHolder.binding.tvSkinType.setText(tags.toString());
                postHolder.binding.tvCreatedAt.setVisibility(View.VISIBLE);
                postHolder.binding.tvCreatedAt.setText(TimeFormatter.getTimeAgo(post.getCreatedAt()));
            }

            postHolder.binding.tvLikes.setText(String.valueOf(post.getLikesCount()));
            postHolder.binding.tvComments.setText(String.valueOf(post.getCommentsCount()));
            postHolder.binding.tvContent.setText(post.getContent());

            String mappedAvatar = com.veganbeauty.app.utils.IdentityMapper.mapAvatar(holder.itemView.getContext(), post.getAuthorId(), post.getAuthorAvatarUrl());
            if (mappedAvatar != null && !mappedAvatar.isEmpty()) {
                String avatarUrl = com.veganbeauty.app.utils.RootieBrandHelper.resolveAvatar(
                        post.getAuthorId(), mappedAvatar);
                com.veganbeauty.app.utils.AvatarLoader.loadAvatar(postHolder.binding.ivAuthorAvatar, avatarUrl);
            } else if (com.veganbeauty.app.utils.RootieBrandHelper.isRootieUser(post.getAuthorId(), post.getAuthorDisplayName())) {
                com.bumptech.glide.Glide.with(postHolder.binding.ivAuthorAvatar.getContext()).load(com.veganbeauty.app.utils.RootieBrandHelper.AVATAR_URL).placeholder(android.R.color.darker_gray).error(R.drawable.img_avatar).into(postHolder.binding.ivAuthorAvatar);
            } else {
                postHolder.binding.ivAuthorAvatar.setImageResource(android.R.color.darker_gray);
            }

            List<String> linkedIds = new ArrayList<>();
            if (post.getLinkedProductIds() != null) {
                for (String id : post.getLinkedProductIds().split(",")) {
                    String trim = id.trim();
                    if (!trim.isEmpty()) linkedIds.add(trim);
                }
            }

            if (!linkedIds.isEmpty() && !globalProducts.isEmpty()) {
                List<CommunityProduct> matchingProducts = new ArrayList<>();
                for (CommunityProduct p : globalProducts) {
                    if (linkedIds.contains(p.getId())) matchingProducts.add(p);
                }
                if (!matchingProducts.isEmpty()) {
                    postHolder.binding.llUsedProducts.setVisibility(View.VISIBLE);
                    postHolder.binding.rvLinkedProducts.setAdapter(new PostLinkedProductAdapter(matchingProducts, post.getPostId(), post.getAuthorId()));
                } else {
                    postHolder.binding.llUsedProducts.setVisibility(View.GONE);
                }
            } else {
                postHolder.binding.llUsedProducts.setVisibility(View.GONE);
            }

            List<String> urls = new ArrayList<>();
            if (post.getMediaUrlsString() != null) {
                for (String url : post.getMediaUrlsString().split(",")) {
                    if (!url.trim().isEmpty()) urls.add(url.trim());
                }
            }

            ViewGroup rootLayout = (ViewGroup) postHolder.binding.getRoot();
            View contentContainer = postHolder.binding.llContentContainer;
            int currentIdx = rootLayout.indexOfChild(contentContainer);

            if (!urls.isEmpty()) {
                ImageSliderAdapter sliderAdapter = null;
                if (postHolder.binding.vpPostImages.getAdapter() instanceof ImageSliderAdapter) {
                    sliderAdapter = (ImageSliderAdapter) postHolder.binding.vpPostImages.getAdapter();
                }

                detachTabMediator(postHolder);

                if (sliderAdapter == null) {
                    sliderAdapter = new ImageSliderAdapter(urls);
                    postHolder.binding.vpPostImages.setAdapter(sliderAdapter);
                } else if (!sliderAdapter.imageUrls.equals(urls)) {
                    sliderAdapter.updateData(urls);
                }

                if (urls.size() <= 1) {
                    postHolder.binding.tabIndicator.setVisibility(View.GONE);
                } else {
                    postHolder.binding.tabIndicator.setVisibility(View.VISIBLE);
                    TabLayoutMediator mediator = new TabLayoutMediator(
                            postHolder.binding.tabIndicator,
                            postHolder.binding.vpPostImages,
                            (tab, position1) -> {}
                    );
                    mediator.attach();
                    postHolder.binding.vpPostImages.setTag(R.id.post_tab_mediator_tag, mediator);
                }
                postHolder.binding.flPostImagesContainer.setVisibility(View.VISIBLE);

                int expectedIdx = rootLayout.getChildCount() - 1;
                if (currentIdx != expectedIdx) {
                    rootLayout.removeView(contentContainer);
                    rootLayout.addView(contentContainer);
                }
            } else {
                detachTabMediator(postHolder);
                postHolder.binding.flPostImagesContainer.setVisibility(View.GONE);
                postHolder.binding.tabIndicator.setVisibility(View.GONE);
                if (currentIdx != 1) {
                    rootLayout.removeView(contentContainer);
                    rootLayout.addView(contentContainer, 1);
                }
            }

            Context context = postHolder.itemView.getContext();
            String currentUserIdForLike = getOwnUserId(context);
            android.content.SharedPreferences sharedPrefs = context.getSharedPreferences("rootie_prefs", Context.MODE_PRIVATE);
            final boolean[] isLiked = {sharedPrefs.getBoolean("liked_" + currentUserIdForLike + "_" + post.getPostId(), false)};
            final int[] currentLikesCount = {post.getLikesCount()};

            if (isLiked[0]) {
                postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_filled);
                postHolder.binding.ivLike.setImageTintList(null);
                postHolder.binding.ivLike.clearColorFilter();
            } else {
                postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart);
                postHolder.binding.ivLike.setImageTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary)));
            }

            postHolder.binding.ivLike.setOnClickListener(v -> {
                isLiked[0] = !isLiked[0];
                if (isLiked[0]) {
                    postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart_filled);
                    postHolder.binding.ivLike.setImageTintList(null);
                    postHolder.binding.ivLike.clearColorFilter();
                    currentLikesCount[0]++;
                    postHolder.binding.tvLikes.setText(String.valueOf(currentLikesCount[0]));
                    post.setLikesCount(currentLikesCount[0]);
                    sharedPrefs.edit().putBoolean("liked_" + currentUserIdForLike + "_" + post.getPostId(), true).apply();
                    if (!RootieBrandHelper.isRootieUser(post.getAuthorId())) {
                        new Thread(() -> RootieDatabase.getDatabase(context).communityDao().incrementLikesCount(post.getPostId())).start();
                    }
                    String currentUserId = getOwnUserId(context);
                    String currentUserName = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getFullName(context);
                    if (currentUserName == null || currentUserName.isEmpty()) currentUserName = "Người dùng";
                    String currentUserAvatar = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getAvatar(context);

                    if (!currentUserId.equals(post.getAuthorId())) {
                        com.veganbeauty.app.features.community.notification.CommunityNotificationHelper.addCommunityNotificationForUser(
                                context,
                                post.getAuthorId(),
                                "like_" + System.currentTimeMillis(),
                                currentUserId,
                                currentUserName,
                                currentUserAvatar != null ? currentUserAvatar : "",
                                "INTERACTION",
                                "LIKE",
                                "đã thích bài viết của bạn.",
                                post.getPostId(),
                                null
                        );
                    }
                } else {
                    postHolder.binding.ivLike.setImageResource(R.drawable.ic_heart);
                    postHolder.binding.ivLike.setImageTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary)));
                    currentLikesCount[0] = Math.max(0, currentLikesCount[0] - 1);
                    postHolder.binding.tvLikes.setText(String.valueOf(currentLikesCount[0]));
                    post.setLikesCount(currentLikesCount[0]);
                    sharedPrefs.edit().putBoolean("liked_" + currentUserIdForLike + "_" + post.getPostId(), false).apply();
                    if (!RootieBrandHelper.isRootieUser(post.getAuthorId())) {
                        new Thread(() -> RootieDatabase.getDatabase(context).communityDao().decrementLikesCount(post.getPostId())).start();
                    }
                }
            });

            postHolder.binding.tvComments.setText(String.valueOf(post.getCommentsCount()));
            postHolder.binding.tvReups.setText(String.valueOf(post.getReupsCount()));

            postHolder.binding.ivComment.setOnClickListener(v -> {
                if (context instanceof FragmentActivity) {
                    CommunityCommentBottomSheet bottomSheet = CommunityCommentBottomSheet.newInstance(post.getPostId(), post.getCommentsCount());
                    bottomSheet.show(((FragmentActivity) context).getSupportFragmentManager(), CommunityCommentBottomSheet.TAG);
                }
            });

            final String finalAuthorName = authorName;
            View.OnClickListener onProfileClick = v -> ComProfileNavigator.openProfile(
                    context,
                    post.getAuthorId(),
                    post.getAuthorAvatarUrl(),
                    finalAuthorName
            );

            String ownUserId = getOwnUserId(context);

            if (ownUserId.equals(post.getAuthorId())) {
                postHolder.binding.tvFollow.setVisibility(View.GONE);
                postHolder.binding.ivMore.setOnClickListener(v -> {
                    View popupView = LayoutInflater.from(context).inflate(R.layout.com_popup_more_author, null);
                    PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    popupWindow.setElevation(8f);
                    popupView.findViewById(R.id.tvEdit).setOnClickListener(v1 -> popupWindow.dismiss());
                    popupView.findViewById(R.id.tvDelete).setOnClickListener(v1 -> popupWindow.dismiss());
                    popupView.findViewById(R.id.tvPrivacy).setOnClickListener(v1 -> popupWindow.dismiss());
                    int xOffset = -(int) (160 * context.getResources().getDisplayMetrics().density) + v.getWidth();
                    popupWindow.showAsDropDown(v, xOffset, 0);
                });
            } else {
                postHolder.binding.tvFollow.setVisibility(View.VISIBLE);
                applyFollowButtonState(postHolder, post, authorName, context);
                postHolder.binding.ivMore.setOnClickListener(v -> {
                    View popupView = LayoutInflater.from(context).inflate(R.layout.com_popup_more_other, null);
                    PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    popupWindow.setElevation(8f);
                    popupView.findViewById(R.id.tvReport).setOnClickListener(v1 -> popupWindow.dismiss());
                    popupView.findViewById(R.id.tvHide).setOnClickListener(v1 -> popupWindow.dismiss());
                    int xOffset = -(int) (160 * context.getResources().getDisplayMetrics().density) + v.getWidth();
                    popupWindow.showAsDropDown(v, xOffset, 0);
                });
            }

            final boolean[] isReuped = {UserMemoryHelper.isPostReposted(context, ownUserId, post.getPostId())};
            final int[] currentReupsCount = {post.getReupsCount()};

            applyReupIcon(postHolder.binding.ivReup, postHolder.binding.tvReups, isReuped[0]);

            postHolder.binding.ivReup.setOnClickListener(v -> {
                isReuped[0] = UserMemoryHelper.toggleRepost(context, ownUserId, post);
                if (isReuped[0]) {
                    currentReupsCount[0]++;
                    
                    if (!ownUserId.equals(post.getAuthorId())) {
                        com.veganbeauty.app.features.community.notification.CommunityNotificationHelper.addCommunityNotificationForUser(
                                context,
                                post.getAuthorId(),
                                "repost_" + System.currentTimeMillis(),
                                ownUserId,
                                "Người dùng",
                                "",
                                "INTERACTION",
                                "REPOST",
                                "đã đăng lại bài viết của bạn.",
                                post.getPostId(),
                                null
                        );
                    }
                } else {
                    currentReupsCount[0] = Math.max(0, currentReupsCount[0] - 1);
                }
                applyReupIcon(postHolder.binding.ivReup, postHolder.binding.tvReups, isReuped[0]);
                postHolder.binding.tvReups.setText(String.valueOf(currentReupsCount[0]));
            });

            final boolean[] isBookmarked = {UserMemoryHelper.isPostSaved(context, ownUserId, post.getPostId())};
            applyBookmarkIcon(postHolder.binding.ivBookmark, isBookmarked[0]);
            // Backfill snapshot for older saves that only stored the post id
            if (isBookmarked[0]) {
                UserMemoryHelper.ensureSavedPostSnapshot(context, ownUserId, post);
            }

            postHolder.binding.ivBookmark.setOnClickListener(v -> {
                isBookmarked[0] = UserMemoryHelper.toggleSave(context, ownUserId, post);
                applyBookmarkIcon(postHolder.binding.ivBookmark, isBookmarked[0]);
            });

            postHolder.binding.llAuthorInfo.setOnClickListener(onProfileClick);
            postHolder.binding.tvAuthorName.setOnClickListener(onProfileClick);
            postHolder.binding.ivAuthorAvatar.setOnClickListener(onProfileClick);

        } else if (item instanceof CommunityFeedItem.SuggestedUsers) {
            SuggestedUsersViewHolder usersHolder = (SuggestedUsersViewHolder) holder;
            SuggestionAdapter suggestionAdapter = new SuggestionAdapter(
                    ((CommunityFeedItem.SuggestedUsers) item).users,
                    followingUserIds,
                    this::handleUserFollowToggle
            );
            usersHolder.binding.rvSuggestions.setAdapter(suggestionAdapter);

            usersHolder.binding.tvSeeAllSuggestions.setOnClickListener(v -> {
                Context context = v.getContext();
                if (context instanceof FragmentActivity) {
                    ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, new CommunityDiscoverPeopleFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        } else if (item instanceof CommunityFeedItem.SuggestedReels) {
            SuggestedReelsViewHolder reelsHolder = (SuggestedReelsViewHolder) holder;
            ReelAdapter reelAdapter = new ReelAdapter(((CommunityFeedItem.SuggestedReels) item).reels, false);
            reelsHolder.binding.rvSuggestedReels.setAdapter(reelAdapter);
        }
    }

    private void detachTabMediator(PostViewHolder holder) {
        Object tag = holder.binding.vpPostImages.getTag(R.id.post_tab_mediator_tag);
        if (tag instanceof TabLayoutMediator) {
            ((TabLayoutMediator) tag).detach();
            holder.binding.vpPostImages.setTag(R.id.post_tab_mediator_tag, null);
        }
        holder.binding.tabIndicator.removeAllTabs();
    }

    private void applyBookmarkIcon(ImageView ivBookmark, boolean isSaved) {
        ivBookmark.setImageResource(isSaved ? R.drawable.ic_save_full : R.drawable.ic_save);
        ivBookmark.clearColorFilter();
        ivBookmark.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(ivBookmark.getContext(), R.color.secondary)));
    }

    private void applyReupIcon(ImageView ivReup, TextView tvReups, boolean isReuped) {
        int colorRes = isReuped ? R.color.repost_active : R.color.secondary;
        int color = ContextCompat.getColor(ivReup.getContext(), colorRes);
        ivReup.clearColorFilter();
        ivReup.setImageTintList(ColorStateList.valueOf(color));
        tvReups.setTextColor(color);
        tvReups.setTypeface(null, isReuped ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof PostViewHolder) {
            detachTabMediator((PostViewHolder) holder);
        }
        super.onViewRecycled(holder);
    }

    private String getOwnUserId(Context context) {
        String ownId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(context);
        if (ownId == null || ownId.trim().isEmpty()) {
            ownId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context);
        }
        if (ownId == null || ownId.trim().isEmpty()) {
            ownId = "test_001";
        }
        return ownId.trim();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setFanpageFollowing(boolean following) {
        updateFollowingState(RootieBrandHelper.USER_ID_VN, following);
    }

    public void setOnFanpageUnfollowListener(Runnable action) {
        // Kept for compatibility with news page; feed uses unified follow state.
    }

    private void handleUserFollowToggle(Context context, String authorId, String authorName, boolean follow) {
        String ownUserId = getOwnUserId(context);
        String targetId = CommunitySocialHelper.resolveFollowTargetId(authorId, authorName);
        if (targetId.isEmpty() || ownUserId.equals(targetId)) {
            return;
        }

        CommunitySocialHelper.applyFollowChange(context, ownUserId, targetId, follow);
        updateFollowingState(targetId, follow);
        if (followStateChangedListener != null) {
            followStateChangedListener.onFollowStateChanged(targetId, follow);
        }
    }

    private void applyFollowButtonState(PostViewHolder postHolder, CommunityPostEntity post,
                                        String authorName, Context context) {
        boolean isFollowing = CommunitySocialHelper.isFollowingUser(followingUserIds, post.getAuthorId(), authorName);

        if (isFollowing) {
            bindFollowingButton(postHolder.binding.tvFollow);
            postHolder.binding.tvFollow.setOnClickListener(v ->
                    handleUserFollowToggle(context, post.getAuthorId(), authorName, false));
        } else {
            bindFollowButton(postHolder.binding.tvFollow, context);
            postHolder.binding.tvFollow.setOnClickListener(v ->
                    handleUserFollowToggle(context, post.getAuthorId(), authorName, true));
        }
    }

    private void bindFollowingButton(TextView button) {
        button.setText("Đã theo dõi");
        button.setBackgroundResource(R.drawable.com_bg_btn_following);
        button.setTextColor(Color.parseColor("#6E846A"));
        button.setClickable(true);
    }

    private void bindFollowButton(TextView button, Context context) {
        button.setText("Theo dõi");
        button.setBackgroundResource(R.drawable.com_bg_btn_follow);
        button.setTextColor(ContextCompat.getColor(context, R.color.primary));
        button.setClickable(true);
    }

    public void updateData(
            List<CommunityPostEntity> newPosts,
            List<UserEntity> newUsers,
            List<ReelEntity> newReels,
            List<CommunityProduct> newProducts
    ) {
        if (newProducts != null && !newProducts.isEmpty()) {
            this.globalProducts = newProducts;
        }
        List<CommunityFeedItem> mergedItems = new ArrayList<>();

        for (int i = 0; i < newPosts.size(); i++) {
            mergedItems.add(new CommunityFeedItem.Post(newPosts.get(i)));

            if (i == 4 && newUsers != null && !newUsers.isEmpty()) {
                mergedItems.add(new CommunityFeedItem.SuggestedUsers(newUsers));
            }

            if (i == 8 && newReels != null && !newReels.isEmpty()) {
                mergedItems.add(new CommunityFeedItem.SuggestedReels(newReels));
            }
        }

        if (newPosts.size() <= 4) {
            if (newUsers != null && !newUsers.isEmpty()) mergedItems.add(new CommunityFeedItem.SuggestedUsers(newUsers));
            if (newReels != null && !newReels.isEmpty()) mergedItems.add(new CommunityFeedItem.SuggestedReels(newReels));
        } else if (newPosts.size() <= 8) {
            if (newReels != null && !newReels.isEmpty()) mergedItems.add(new CommunityFeedItem.SuggestedReels(newReels));
        }

        this.items = mergedItems;
        notifyDataSetChanged();
    }
}

class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder> {
    private List<UserEntity> users;
    private final Set<String> followingUserIds;
    private final FollowToggleListener followToggleListener;

    interface FollowToggleListener {
        void onToggle(Context context, String userId, String username, boolean follow);
    }

    public SuggestionAdapter(List<UserEntity> users, Set<String> followingUserIds, FollowToggleListener followToggleListener) {
        this.users = users;
        this.followingUserIds = followingUserIds != null ? followingUserIds : new HashSet<>();
        this.followToggleListener = followToggleListener;
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        final ComItemSuggestionBinding binding;
        SuggestionViewHolder(ComItemSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SuggestionViewHolder(ComItemSuggestionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        UserEntity user = users.get(position);
        holder.binding.tvUsername.setText(user.getUsername());

        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.binding.ivAvatar.getContext()).load(user.getAvatar()).placeholder(android.R.color.darker_gray).error(R.drawable.logo).into(holder.binding.ivAvatar);
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray);
        }

        if (user.getMutualCount() > 0) {
            String friendName = user.getFirstMutualFriendName() != null ? user.getFirstMutualFriendName() : "Ai đó";
            if (user.getMutualCount() == 1) {
                holder.binding.tvMutualCount.setText("Có " + friendName + " đang theo dõi");
            } else {
                holder.binding.tvMutualCount.setText("Có " + friendName + " và " + (user.getMutualCount() - 1) + " người khác đang theo dõi");
            }
            holder.binding.llMutualInfo.setVisibility(View.VISIBLE);

            List<String> avatars = user.getMutualFriendAvatars();
            if (avatars != null && !avatars.isEmpty()) {
                holder.binding.flMutualAvatars.setVisibility(View.VISIBLE);

                if (avatars.size() >= 1) {
                    holder.binding.cvMutual1.setVisibility(View.VISIBLE);
                    loadMutualAvatar(holder.itemView.getContext(), avatars.get(0), holder.binding.ivMutual1);
                } else {
                    holder.binding.cvMutual1.setVisibility(View.GONE);
                }

                if (avatars.size() >= 2) {
                    holder.binding.cvMutual2.setVisibility(View.VISIBLE);
                    loadMutualAvatar(holder.itemView.getContext(), avatars.get(1), holder.binding.ivMutual2);
                } else {
                    holder.binding.cvMutual2.setVisibility(View.GONE);
                }

                if (avatars.size() >= 3) {
                    holder.binding.cvMutual3.setVisibility(View.VISIBLE);
                    loadMutualAvatar(holder.itemView.getContext(), avatars.get(2), holder.binding.ivMutual3);
                } else {
                    holder.binding.cvMutual3.setVisibility(View.GONE);
                }
            } else {
                holder.binding.flMutualAvatars.setVisibility(View.GONE);
            }
        } else {
            holder.binding.llMutualInfo.setVisibility(View.GONE);
            holder.binding.flMutualAvatars.setVisibility(View.GONE);
        }

        holder.binding.getRoot().setOnClickListener(v -> ComProfileNavigator.openProfile(
                v.getContext(),
                user.getUser_id(),
                user.getAvatar(),
                user.getUsername()
        ));

        boolean isFollowing = followingUserIds.contains(user.getUser_id());
        if (isFollowing) {
            holder.binding.tvFollow.setText("Đã theo dõi");
            holder.binding.tvFollow.setBackgroundResource(R.drawable.com_bg_btn_following);
            holder.binding.tvFollow.setTextColor(Color.parseColor("#6E846A"));
        } else {
            holder.binding.tvFollow.setText("Theo dõi");
            holder.binding.tvFollow.setBackgroundResource(R.drawable.com_bg_btn_follow);
            holder.binding.tvFollow.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        }

        holder.binding.tvFollow.setOnClickListener(v -> {
            if (followToggleListener == null) return;
            boolean currentlyFollowing = followingUserIds.contains(user.getUser_id());
            followToggleListener.onToggle(
                    v.getContext(),
                    user.getUser_id(),
                    user.getUsername(),
                    !currentlyFollowing
            );
        });
    }

    private void loadMutualAvatar(Context context, String url, ImageView target) {
        com.bumptech.glide.Glide.with(target.getContext()).load(url).error(R.drawable.img_avatar).circleCrop().into(target);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<UserEntity> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }
}

class ReelAdapter extends RecyclerView.Adapter<ReelAdapter.ReelViewHolder> {
    private List<ReelEntity> reels;
    private final boolean isGrid;

    public ReelAdapter(List<ReelEntity> reels, boolean isGrid) {
        this.reels = reels;
        this.isGrid = isGrid;
    }

    static class ReelViewHolder extends RecyclerView.ViewHolder {
        final ComItemReelBinding binding;
        ReelViewHolder(ComItemReelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public ReelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ComItemReelBinding binding = ComItemReelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        if (isGrid) {
            ViewGroup.LayoutParams lp = binding.getRoot().getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) lp;
                marginLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                int marginInPx = (int) (1 * parent.getContext().getResources().getDisplayMetrics().density);
                marginLp.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
                binding.getRoot().setLayoutParams(marginLp);
            }
        }
        return new ReelViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelViewHolder holder, int position) {
        ReelEntity reel = reels.get(position);
        if (reel.getThumbnailUrl() != null && !reel.getThumbnailUrl().isEmpty()) {
            ImageLoader imageLoader = Coil.imageLoader(holder.itemView.getContext());
            com.bumptech.glide.Glide.with(holder.binding.ivThumbnail.getContext()).load(reel.getThumbnailUrl()).placeholder(android.R.color.darker_gray).into(holder.binding.ivThumbnail);
        } else {
            holder.binding.ivThumbnail.setImageResource(android.R.color.darker_gray);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            Context context = holder.binding.getRoot().getContext();
            if (context instanceof FragmentActivity) {
                ReelPlayerDialog dialog = new ReelPlayerDialog(reels, holder.getAbsoluteAdapterPosition());
                dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "ReelPlayerDialog");
            }
        });
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }

    public void updateData(List<ReelEntity> newReels) {
        this.reels = newReels;
        notifyDataSetChanged();
    }
}
