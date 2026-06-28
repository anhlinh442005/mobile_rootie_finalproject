package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.ImageLoader;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.utils.TimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

class CommentItem {
    final String userId;
    final String avatarUrl;
    final String username;
    final String timeStr;
    final String content;
    final int likesCount;
    final boolean isAuthor;
    final boolean hasReply;
    final String replyUserId;
    final String replyUsername;
    final String replyContent;
    final String replyTime;
    final int replyLikesCount;
    final String replyAvatarUrl;
    final String commentId;
    final String replyCommentId;

    public CommentItem(String userId, String avatarUrl, String username, String timeStr, String content, int likesCount, boolean isAuthor, boolean hasReply, String replyUserId, String replyUsername, String replyContent, String replyTime, int replyLikesCount, String replyAvatarUrl, String commentId, String replyCommentId) {
        this.userId = userId;
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.timeStr = timeStr;
        this.content = content;
        this.likesCount = likesCount;
        this.isAuthor = isAuthor;
        this.hasReply = hasReply;
        this.replyUserId = replyUserId;
        this.replyUsername = replyUsername;
        this.replyContent = replyContent;
        this.replyTime = replyTime;
        this.replyLikesCount = replyLikesCount;
        this.replyAvatarUrl = replyAvatarUrl;
        this.commentId = commentId;
        this.replyCommentId = replyCommentId;
    }
}

public class CommunityCommentBottomSheet extends BottomSheetDialogFragment {

    private String postId = null;
    private int expectedCommentsCount = 5;

    public static final String TAG = "CommunityCommentBottomSheet";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_COMMENTS_COUNT = "comments_count";
    private static final String ARG_TARGET_COMMENT_ID = "target_comment_id";

    public static CommunityCommentBottomSheet newInstance(String postId, int commentsCount) {
        return newInstance(postId, commentsCount, null);
    }

    public static CommunityCommentBottomSheet newInstance(String postId, int commentsCount, String targetCommentId) {
        CommunityCommentBottomSheet fragment = new CommunityCommentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, postId);
        args.putInt(ARG_COMMENTS_COUNT, commentsCount);
        args.putString(ARG_TARGET_COMMENT_ID, targetCommentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            postId = getArguments().getString(ARG_POST_ID);
            expectedCommentsCount = getArguments().getInt(ARG_COMMENTS_COUNT, 5);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView tvEmptyComments = view.findViewById(R.id.tvEmptyComments);
        List<CommentItem> commentsList = new ArrayList<>();

        try {
            Context context = requireContext();
            List<UserEntity> allUsers = new LocalJsonReader(context).getUsers();
            Map<String, UserEntity> usersMap = new HashMap<>();
            for (UserEntity u : allUsers) {
                usersMap.put(u.getUser_id(), u);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("community_posts.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String postsJsonStr = sb.toString().replace("\uFEFF", "");

            JSONArray inlineCommentsArray = null;

            JSONArray postsArray = null;
            try {
                postsArray = new JSONArray(postsJsonStr);
            } catch (Exception e) {
                try {
                    postsArray = new JSONObject(postsJsonStr).optJSONArray("posts");
                } catch (Exception ignored) {}
            }
            inlineCommentsArray = findCommentsInArray(postsArray, "post_id");

            if (inlineCommentsArray == null || inlineCommentsArray.length() == 0) {
                try {
                    BufferedReader newsReader = new BufferedReader(new InputStreamReader(context.getAssets().open("community_news.json")));
                    StringBuilder newsSb = new StringBuilder();
                    while ((line = newsReader.readLine()) != null) newsSb.append(line);
                    String newsJsonStr = newsSb.toString().replace("\uFEFF", "");
                    JSONArray newsArray = new JSONArray(newsJsonStr);
                    inlineCommentsArray = findCommentsInArray(newsArray, "post_id");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (inlineCommentsArray == null || inlineCommentsArray.length() == 0) {
                try {
                    BufferedReader reelsReader = new BufferedReader(new InputStreamReader(context.getAssets().open("community_reels_fb.json")));
                    StringBuilder reelsSb = new StringBuilder();
                    while ((line = reelsReader.readLine()) != null) reelsSb.append(line);
                    String reelsJsonStr = reelsSb.toString().replace("\uFEFF", "");
                    JSONArray reelsArray = new JSONArray(reelsJsonStr);
                    inlineCommentsArray = findCommentsInArray(reelsArray, "reel_id");
                    if (inlineCommentsArray == null) {
                        inlineCommentsArray = findCommentsInArray(reelsArray, "post_id");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                File localFile = new File(context.getFilesDir(), "local_comments.json");
                if (localFile.exists()) {
                    FileInputStream fis = new FileInputStream(localFile);
                    BufferedReader localReader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                    StringBuilder localSb = new StringBuilder();
                    while ((line = localReader.readLine()) != null) localSb.append(line);
                    localReader.close();
                    
                    JSONArray localArray = new JSONArray(localSb.toString());
                    for (int i = 0; i < localArray.length(); i++) {
                        JSONObject obj = localArray.getJSONObject(i);
                        if (postId != null && postId.equals(obj.optString("post_id"))) {
                            if (inlineCommentsArray == null) {
                                inlineCommentsArray = new JSONArray();
                            }
                            inlineCommentsArray.put(obj);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (inlineCommentsArray != null && inlineCommentsArray.length() > 0) {
                List<JSONObject> topLevelComments = new ArrayList<>();
                Map<String, JSONObject> repliesMap = new HashMap<>();

                for (int i = 0; i < inlineCommentsArray.length(); i++) {
                    JSONObject obj = inlineCommentsArray.getJSONObject(i);
                    String parentId = obj.optString("parent_id", "null");
                    if ("null".equals(parentId) || parentId.isEmpty()) {
                        topLevelComments.add(obj);
                    } else {
                        repliesMap.put(parentId, obj);
                    }
                }

                for (JSONObject obj : topLevelComments) {
                    JSONObject authorObj = obj.optJSONObject("author");
                    String userId = (authorObj != null && !authorObj.optString("user_id").isEmpty()) ? authorObj.optString("user_id") : obj.optString("user_id", "");
                    String username = (authorObj != null && !authorObj.optString("username").isEmpty()) ? authorObj.optString("username") : (!obj.optString("username", "").isEmpty() ? obj.optString("username", "") : (usersMap.containsKey(userId) ? usersMap.get(userId).getUsername() : ""));
                    String avatarUrl = (authorObj != null && !authorObj.optString("avatar").isEmpty()) ? authorObj.optString("avatar") : (!obj.optString("avatar_url", "").isEmpty() ? obj.optString("avatar_url", "") : (usersMap.containsKey(userId) ? usersMap.get(userId).getAvatar() : ""));
                    String commentId = obj.optString("comment_id", "");

                    JSONObject replyObj = repliesMap.get(commentId);
                    boolean hasReply = false;
                    String replyUsername = "", replyContent = "", replyTime = "", replyAvatar = "", replyUserId = "";
                    int replyLikes = 0;

                    if (replyObj != null) {
                        hasReply = true;
                        JSONObject replyAuthor = replyObj.optJSONObject("author");
                        replyUserId = (replyAuthor != null && !replyAuthor.optString("user_id").isEmpty()) ? replyAuthor.optString("user_id") : replyObj.optString("user_id", "");
                        replyUsername = (replyAuthor != null && !replyAuthor.optString("username").isEmpty()) ? replyAuthor.optString("username") : (!replyObj.optString("username", "").isEmpty() ? replyObj.optString("username", "") : (usersMap.containsKey(replyUserId) ? usersMap.get(replyUserId).getUsername() : ""));
                        replyAvatar = (replyAuthor != null && !replyAuthor.optString("avatar").isEmpty()) ? replyAuthor.optString("avatar") : (!replyObj.optString("avatar_url", "").isEmpty() ? replyObj.optString("avatar_url", "") : (usersMap.containsKey(replyUserId) ? usersMap.get(replyUserId).getAvatar() : ""));
                        replyContent = replyObj.optString("content", "");
                        replyTime = replyObj.optString("created_at", "");
                        replyLikes = replyObj.optInt("likes_count", 0);
                    }

                    String rawTime = obj.optString("created_at", "");
                    String timeStr = !rawTime.isEmpty() ? TimeFormatter.getTimeAgo(rawTime) : "";

                    commentsList.add(new CommentItem(
                            userId,
                            avatarUrl,
                            username,
                            timeStr.isEmpty() ? rawTime : timeStr,
                            obj.optString("content", ""),
                            obj.optInt("likes_count", 0),
                            obj.optBoolean("is_author", false),
                            hasReply,
                            hasReply && repliesMap.containsKey(commentId) ? repliesMap.get(commentId).optString("user_id", "") : "",
                            replyUsername,
                            replyContent,
                            !replyTime.isEmpty() ? TimeFormatter.getTimeAgo(replyTime) : "",
                            replyLikes,
                            replyAvatar,
                            commentId,
                            hasReply && repliesMap.containsKey(commentId) ? repliesMap.get(commentId).optString("comment_id", "") : ""
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView tvCommentCount = view.findViewById(R.id.tvCommentCount);
        TextView tvClearAllComments = view.findViewById(R.id.tvClearAllComments);

        if (commentsList.isEmpty()) {
            if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
            if (tvCommentCount != null) tvCommentCount.setText("Bình luận");
        } else {
            if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
            rvComments.setAdapter(new CommentAdapter(commentsList, rvComments, tvEmptyComments, tvCommentCount));
            if (tvCommentCount != null) tvCommentCount.setText(commentsList.size() + " bình luận");

            String targetCommentId = getArguments() != null ? getArguments().getString(ARG_TARGET_COMMENT_ID) : null;
            if (targetCommentId != null && !targetCommentId.isEmpty()) {
                int index = -1;
                for (int i = 0; i < commentsList.size(); i++) {
                    CommentItem it = commentsList.get(i);
                    if (targetCommentId.equals(it.commentId) || (it.hasReply && targetCommentId.equals(it.replyCommentId))) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    int finalIndex = index;
                    rvComments.post(() -> rvComments.scrollToPosition(finalIndex));
                }
            }
        }

        if (tvClearAllComments != null) {
            tvClearAllComments.setOnClickListener(v -> {
                if (!commentsList.isEmpty()) {
                    new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                            .setTitle("Xóa tất cả bình luận")
                            .setMessage("Bạn có chắc chắn muốn xóa toàn bộ bình luận?")
                            .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                                commentsList.clear();
                                if (rvComments.getAdapter() != null) rvComments.getAdapter().notifyDataSetChanged();
                                if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.VISIBLE);
                                rvComments.setVisibility(View.GONE);
                                if (tvCommentCount != null) tvCommentCount.setText("Bình luận");
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });
        }

        ImageView ivSendComment = view.findViewById(R.id.ivSendComment);
        if (ivSendComment != null) {
            ivSendComment.setOnClickListener(v -> dismiss());
        }

        EditText etComment = view.findViewById(R.id.etComment);
        ImageView ivSubmitComment = view.findViewById(R.id.ivSubmitComment);

        View.OnClickListener submitAction = v -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty() && postId != null) {
                String myUserId = "test_001";
                String myUsername = "Test User";
                String myAvatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg";

                String timeStr = "Vừa xong";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String isoDate = sdf.format(new Date());

                CommentItem newComment = new CommentItem(
                        myUserId, myAvatar, myUsername, timeStr, text, 0, false, false, "", "", "", "", 0, "", "", ""
                );

                commentsList.add(0, newComment);

                if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.GONE);
                rvComments.setVisibility(View.VISIBLE);
                if (rvComments.getAdapter() == null) {
                    rvComments.setAdapter(new CommentAdapter(commentsList, rvComments, tvEmptyComments, tvCommentCount));
                } else {
                    rvComments.getAdapter().notifyItemInserted(0);
                    rvComments.scrollToPosition(0);
                }
                if (tvCommentCount != null) tvCommentCount.setText(commentsList.size() + " bình luận");

                etComment.setText("");

                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
                }

                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("comment_id", UUID.randomUUID().toString());
                commentMap.put("post_id", postId);
                commentMap.put("user_id", myUserId);
                commentMap.put("username", myUsername);
                commentMap.put("avatar_url", myAvatar);
                commentMap.put("content", text);
                commentMap.put("created_at", isoDate);
                commentMap.put("likes_count", 0);
                commentMap.put("is_author", false);

                new Thread(() -> {
                    try {
                        File localFile = new File(requireContext().getFilesDir(), "local_comments.json");
                        JSONArray localArray;
                        if (localFile.exists()) {
                            FileInputStream fis = new FileInputStream(localFile);
                            BufferedReader localReader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                            StringBuilder localSb = new StringBuilder();
                            String l;
                            while ((l = localReader.readLine()) != null) localSb.append(l);
                            localReader.close();
                            localArray = new JSONArray(localSb.toString());
                        } else {
                            localArray = new JSONArray();
                        }
                        localArray.put(new JSONObject(commentMap));
                        FileOutputStream fos = new FileOutputStream(localFile);
                        fos.write(localArray.toString().getBytes(StandardCharsets.UTF_8));
                        fos.close();

                        RootieDatabase.getDatabase(requireContext()).communityDao().incrementCommentsCount(postId);
                        new FirestoreService().addCommentToPost(postId, commentMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        };

        etComment.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitAction.onClick(v);
                return true;
            }
            return false;
        });

        if (ivSubmitComment != null) {
            ivSubmitComment.setOnClickListener(submitAction);
        }
    }

    private JSONArray findCommentsInArray(JSONArray jsonArray, String idKey) {
        if (jsonArray == null) return null;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject postObj = jsonArray.optJSONObject(i);
            if (postObj != null && (postId != null && postId.equals(postObj.optString(idKey)))) {
                return postObj.optJSONArray("comments");
            }
        }
        return null;
    }

    class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
        private final List<CommentItem> comments;
        private final RecyclerView rvComments;
        private final TextView tvEmptyComments;
        private final TextView tvCommentCount;

        public CommentAdapter(List<CommentItem> comments, RecyclerView rvComments, TextView tvEmptyComments, TextView tvCommentCount) {
            this.comments = comments;
            this.rvComments = rvComments;
            this.tvEmptyComments = tvEmptyComments;
            this.tvCommentCount = tvCommentCount;
        }

        class CommentViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar, ivReplyAvatar, ivLike, ivReplyLike;
            TextView tvUsername, tvTime, tvContent, tvLikesCount, tvAuthorTag;
            LinearLayout llReplyContainer, llReplyContentContainer, llViewMoreReplies;
            TextView tvReplyUsername, tvReplyTime, tvReplyContent, tvReplyLikesCount;

            CommentViewHolder(View view) {
                super(view);
                ivAvatar = view.findViewById(R.id.ivAvatar);
                tvUsername = view.findViewById(R.id.tvUsername);
                tvTime = view.findViewById(R.id.tvTime);
                tvContent = view.findViewById(R.id.tvContent);
                tvLikesCount = view.findViewById(R.id.tvLikesCount);
                tvAuthorTag = view.findViewById(R.id.tvAuthorTag);

                llReplyContainer = view.findViewById(R.id.llReplyContainer);
                ivReplyAvatar = view.findViewById(R.id.ivReplyAvatar);
                tvReplyUsername = view.findViewById(R.id.tvReplyUsername);
                tvReplyTime = view.findViewById(R.id.tvReplyTime);
                tvReplyContent = view.findViewById(R.id.tvReplyContent);
                tvReplyLikesCount = view.findViewById(R.id.tvReplyLikesCount);

                llReplyContentContainer = view.findViewById(R.id.llReplyContentContainer);
                llViewMoreReplies = view.findViewById(R.id.llViewMoreReplies);

                ivLike = view.findViewById(R.id.ivLike);
                ivReplyLike = view.findViewById(R.id.ivReplyLike);
            }
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            CommentItem item = comments.get(position);
            holder.tvUsername.setText(item.username);
            holder.tvTime.setText(item.timeStr);
            holder.tvContent.setText(item.content);
            holder.tvLikesCount.setText(String.valueOf(item.likesCount));

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(holder.itemView.getContext(), R.style.CustomDialogTheme)
                        .setTitle("Xóa bình luận")
                        .setMessage("Bạn có chắc chắn muốn xóa bình luận này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            int pos = holder.getAbsoluteAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION && pos < comments.size()) {
                                comments.remove(pos);
                                notifyItemRemoved(pos);
                                String countText = comments.isEmpty() ? "Bình luận" : comments.size() + " bình luận";
                                if (tvCommentCount != null) tvCommentCount.setText(countText);
                                if (comments.isEmpty()) {
                                    if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.VISIBLE);
                                    if (rvComments != null) rvComments.setVisibility(View.GONE);
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });

            if (item.isAuthor) {
                holder.tvAuthorTag.setVisibility(View.VISIBLE);
            } else {
                holder.tvAuthorTag.setVisibility(View.GONE);
            }

            com.bumptech.glide.Glide.with(holder.ivAvatar.getContext()).load(item.avatarUrl).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).into(holder.ivAvatar);

            View.OnClickListener navigateToProfile = v -> {
                Context context = holder.itemView.getContext();
                if (context instanceof FragmentActivity && item.userId != null && !item.userId.isEmpty()) {
                    CommunityProfileFragment profileFragment = new CommunityProfileFragment();
                    Bundle args = new Bundle();
                    args.putString("USER_ID", item.userId);
                    profileFragment.setArguments(args);
                    ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, profileFragment)
                            .addToBackStack(null)
                            .commit();
                    dismiss();
                }
            };

            holder.ivAvatar.setOnClickListener(navigateToProfile);
            holder.tvUsername.setOnClickListener(navigateToProfile);

            if (item.hasReply) {
                holder.llReplyContainer.setVisibility(View.VISIBLE);
                String targetCommentId = getArguments() != null ? getArguments().getString(ARG_TARGET_COMMENT_ID) : null;
                boolean showReplyDirectly = targetCommentId != null && targetCommentId.equals(item.replyCommentId);
                holder.llReplyContentContainer.setVisibility(showReplyDirectly ? View.VISIBLE : View.GONE);
                holder.llViewMoreReplies.setVisibility(showReplyDirectly ? View.GONE : View.VISIBLE);

                holder.tvReplyUsername.setText(item.replyUsername);
                holder.tvReplyTime.setText(item.replyTime);
                holder.tvReplyContent.setText(item.replyContent);
                holder.tvReplyLikesCount.setText(String.valueOf(item.replyLikesCount));

                com.bumptech.glide.Glide.with(holder.ivReplyAvatar.getContext()).load(item.replyAvatarUrl).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).into(holder.ivReplyAvatar);

                View.OnClickListener navigateToReplyProfile = v -> {
                    Context context = holder.itemView.getContext();
                    if (context instanceof FragmentActivity && item.replyUserId != null && !item.replyUserId.isEmpty()) {
                        CommunityProfileFragment profileFragment = new CommunityProfileFragment();
                        Bundle args = new Bundle();
                        args.putString("USER_ID", item.replyUserId);
                        profileFragment.setArguments(args);
                        ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_container, profileFragment)
                                .addToBackStack(null)
                                .commit();
                        dismiss();
                    }
                };

                holder.ivReplyAvatar.setOnClickListener(navigateToReplyProfile);
                holder.tvReplyUsername.setOnClickListener(navigateToReplyProfile);

                holder.llViewMoreReplies.setOnClickListener(v -> {
                    holder.llViewMoreReplies.setVisibility(View.GONE);
                    holder.llReplyContentContainer.setVisibility(View.VISIBLE);
                });
            } else {
                holder.llReplyContainer.setVisibility(View.GONE);
            }

            final boolean[] isLiked = {false};
            holder.ivLike.setOnClickListener(v -> {
                isLiked[0] = !isLiked[0];
                if (isLiked[0]) {
                    holder.ivLike.setImageResource(R.drawable.ic_heart_filled);
                    holder.tvLikesCount.setText(String.valueOf(item.likesCount + 1));
                } else {
                    holder.ivLike.setImageResource(R.drawable.ic_heart_outline);
                    holder.tvLikesCount.setText(String.valueOf(item.likesCount));
                }
            });

            final boolean[] isReplyLiked = {false};
            holder.ivReplyLike.setOnClickListener(v -> {
                isReplyLiked[0] = !isReplyLiked[0];
                if (isReplyLiked[0]) {
                    holder.ivReplyLike.setImageResource(R.drawable.ic_heart_filled);
                    holder.tvReplyLikesCount.setText("21");
                } else {
                    holder.ivReplyLike.setImageResource(R.drawable.ic_heart_outline);
                    holder.tvReplyLikesCount.setText("20");
                }
            });
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}
