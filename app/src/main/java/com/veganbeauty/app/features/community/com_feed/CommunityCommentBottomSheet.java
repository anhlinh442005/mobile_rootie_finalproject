package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.ImageLoader;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.features.community.notification.CommunityNotificationHelper;
import com.veganbeauty.app.utils.ComProfileNavigator;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.TimeFormatter;
import com.veganbeauty.app.utils.UserAvatarHelper;

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
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

class ReplyItem {
    final String userId;
    final String avatarUrl;
    final String username;
    final String timeStr;
    final String content;
    final int likesCount;
    final String commentId;

    ReplyItem(String userId, String avatarUrl, String username, String timeStr, String content, int likesCount, String commentId) {
        this.userId = userId;
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.timeStr = timeStr;
        this.content = content;
        this.likesCount = likesCount;
        this.commentId = commentId;
    }
}

class CommentItem {
    final String userId;
    final String avatarUrl;
    final String username;
    final String timeStr;
    final String content;
    final int likesCount;
    final boolean isAuthor;
    final String commentId;
    final List<ReplyItem> replies;

    CommentItem(String userId, String avatarUrl, String username, String timeStr, String content, int likesCount, boolean isAuthor, String commentId, List<ReplyItem> replies) {
        this.userId = userId;
        this.avatarUrl = avatarUrl;
        this.username = username;
        this.timeStr = timeStr;
        this.content = content;
        this.likesCount = likesCount;
        this.isAuthor = isAuthor;
        this.commentId = commentId;
        this.replies = replies != null ? replies : new ArrayList<>();
    }
}

interface OnCommentReplyListener {
    void onReplyRequested(String commentId, String userId, String username);
}

public class CommunityCommentBottomSheet extends BottomSheetDialogFragment {

    private String postId = null;
    private int expectedCommentsCount = 5;
    private String replyingToCommentId = null;
    private String replyingToUserId = null;
    private String replyingToUsername = null;

    public static final String TAG = "CommunityCommentBottomSheet";
    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_COMMENTS_COUNT = "comments_count";
    private static final String ARG_TARGET_COMMENT_ID = "target_comment_id";

    public interface OnBottomSheetDismissListener {
        void onDismiss();
    }
    private OnBottomSheetDismissListener dismissListener;

    public void setOnDismissListener(OnBottomSheetDismissListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismiss();
        }
    }

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

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.app.Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            com.google.android.material.bottomsheet.BottomSheetDialog d = (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.post(() -> {
            View parent = (View) view.getParent();
            ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            parent.setLayoutParams(layoutParams);

            com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);
            int targetHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.65);
            try {
                behavior.setMaxHeight(targetHeight);
            } catch (Exception e) {}
            behavior.setPeekHeight(targetHeight);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        });

        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView tvEmptyComments = view.findViewById(R.id.tvEmptyComments);
        List<CommentItem> commentsList = loadCommentsList(requireContext());

        TextView tvCommentCount = view.findViewById(R.id.tvCommentCount);
        EditText etComment = view.findViewById(R.id.etComment);
        ImageView ivMyAvatar = view.findViewById(R.id.ivMyAvatar);

        Context context = requireContext();
        String myAvatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(context);
        if (ivMyAvatar != null) {
            com.bumptech.glide.Glide.with(context).load(myAvatar).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(ivMyAvatar);
        }

        TextView tvFakeCommentInput = view.findViewById(R.id.tvFakeCommentInput);

        Runnable[] refreshCommentsHolder = new Runnable[1];

        OnCommentReplyListener replyListener = (commentId, userId, username) -> {
            replyingToCommentId = commentId;
            replyingToUserId = userId;
            replyingToUsername = username;
            showInputOverlay(context, "@" + username + " ", refreshCommentsHolder[0]);
        };

        if (tvFakeCommentInput != null) {
            tvFakeCommentInput.setOnClickListener(v -> {
                showInputOverlay(context, "", refreshCommentsHolder[0]);
            });
        }

        Runnable refreshComments = () -> {
            List<CommentItem> refreshed = loadCommentsList(context);
            commentsList.clear();
            commentsList.addAll(refreshed);
            if (commentsList.isEmpty()) {
                if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.VISIBLE);
                rvComments.setVisibility(View.GONE);
                if (tvCommentCount != null) tvCommentCount.setText("Bình luận");
            } else {
                if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.GONE);
                rvComments.setVisibility(View.VISIBLE);
                if (rvComments.getAdapter() == null) {
                    rvComments.setAdapter(new CommentAdapter(commentsList, rvComments, tvEmptyComments, tvCommentCount, replyListener));
                } else {
                    rvComments.getAdapter().notifyDataSetChanged();
                }
                rvComments.scrollToPosition(commentsList.size() - 1);
                if (tvCommentCount != null) tvCommentCount.setText(commentsList.size() + " bình luận");
            }
        };
        refreshCommentsHolder[0] = refreshComments;

        if (commentsList.isEmpty()) {
            if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
            if (tvCommentCount != null) tvCommentCount.setText("Bình luận");
        } else {
            if (tvEmptyComments != null) tvEmptyComments.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
            rvComments.setAdapter(new CommentAdapter(commentsList, rvComments, tvEmptyComments, tvCommentCount, replyListener));
            if (tvCommentCount != null) tvCommentCount.setText(commentsList.size() + " bình luận");

            String targetCommentId = getArguments() != null ? getArguments().getString(ARG_TARGET_COMMENT_ID) : null;
            if (targetCommentId != null && !targetCommentId.isEmpty()) {
                int index = -1;
                for (int i = 0; i < commentsList.size(); i++) {
                    CommentItem it = commentsList.get(i);
                    if (targetCommentId.equals(it.commentId)) {
                        index = i;
                        break;
                    }
                    for (ReplyItem reply : it.replies) {
                        if (targetCommentId.equals(reply.commentId)) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) break;
                }
                if (index != -1) {
                    int finalIndex = index;
                    rvComments.post(() -> rvComments.scrollToPosition(finalIndex));
                }
            }
        }

        ImageView ivSendComment = view.findViewById(R.id.ivSendComment);
        if (ivSendComment != null) {
            ivSendComment.setOnClickListener(v -> dismiss());
        }
    }

    private void showInputOverlay(Context context, String initialText, Runnable refreshComments) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.com_dialog_comment_input);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // Translucent dialogs ignore ADJUST_RESIZE — lift input via IME insets instead.
            window.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            WindowCompat.setDecorFitsSystemWindows(window, false);
        }

        View contentRoot = dialog.findViewById(android.R.id.content);
        final View root = (contentRoot instanceof ViewGroup && ((ViewGroup) contentRoot).getChildCount() > 0)
                ? ((ViewGroup) contentRoot).getChildAt(0)
                : contentRoot;
        View llInputContainer = dialog.findViewById(R.id.llInputContainer);
        View viewDismiss = dialog.findViewById(R.id.viewDismiss);
        EditText etComment = dialog.findViewById(R.id.etComment);
        ImageView ivMyAvatar = dialog.findViewById(R.id.ivMyAvatar);
        ImageView ivSubmitComment = dialog.findViewById(R.id.ivSubmitComment);

        if (root != null && llInputContainer != null) {
            final int baseBottomPad = (int) (12 * context.getResources().getDisplayMetrics().density);
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                // Push the composer above the keyboard so typed text stays visible.
                llInputContainer.setTranslationY(-ime.bottom);
                llInputContainer.setPadding(
                        llInputContainer.getPaddingLeft(),
                        llInputContainer.getPaddingTop(),
                        llInputContainer.getPaddingRight(),
                        baseBottomPad + (ime.bottom > 0 ? 0 : nav.bottom)
                );
                return insets;
            });
            ViewCompat.requestApplyInsets(root);
        }

        String myAvatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(context);
        if (ivMyAvatar != null) {
            com.bumptech.glide.Glide.with(context).load(myAvatar).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(ivMyAvatar);
        }

        if (initialText != null && !initialText.isEmpty() && etComment != null) {
            etComment.setText(initialText);
            etComment.setSelection(initialText.length());
        }

        if (viewDismiss != null) {
            viewDismiss.setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && etComment != null) {
                    imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
                }
                dialog.dismiss();
            });
        }

        int[] emojiIds = {R.id.tvEmoji1, R.id.tvEmoji2, R.id.tvEmoji3, R.id.tvEmoji4, R.id.tvEmoji5, R.id.tvEmoji6, R.id.tvEmoji7, R.id.tvEmoji8};
        for (int emojiId : emojiIds) {
            TextView emojiView = dialog.findViewById(emojiId);
            if (emojiView != null && etComment != null) {
                emojiView.setOnClickListener(v -> {
                    etComment.append(emojiView.getText());
                    etComment.requestFocus();
                });
            }
        }

        View.OnClickListener submitAction = v -> {
            if (etComment == null) {
                return;
            }
            String text = etComment.getText().toString().trim();
            if (text.isEmpty() || postId == null) {
                return;
            }

            String resolvedUserId = ProfileSessionHelper.getEffectiveUserId(context);
            if (resolvedUserId == null || resolvedUserId.isEmpty()) {
                resolvedUserId = "test_001";
            }
            final String myUserId = resolvedUserId;
            String resolvedUsername = ProfileSession.getFullName(context);
            if (resolvedUsername == null || resolvedUsername.trim().isEmpty()) {
                resolvedUsername = ProfileSession.getUsername(context);
            }
            if (resolvedUsername == null || resolvedUsername.trim().isEmpty()) {
                resolvedUsername = "Người dùng";
            }
            final String myUsername = resolvedUsername;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String isoDate = sdf.format(new Date());
            String newCommentId = UUID.randomUUID().toString();
            String parentId = replyingToCommentId != null ? replyingToCommentId : "null";
            final String notifyParentUserId = replyingToUserId;
            final String savedParentCommentId = replyingToCommentId;

            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("comment_id", newCommentId);
            commentMap.put("post_id", postId);
            commentMap.put("user_id", myUserId);
            commentMap.put("username", myUsername);
            commentMap.put("avatar_url", myAvatar);
            commentMap.put("content", text);
            commentMap.put("created_at", isoDate);
            commentMap.put("likes_count", 0);
            commentMap.put("is_author", false);
            commentMap.put("parent_id", parentId);

            replyingToCommentId = null;
            replyingToUserId = null;
            replyingToUsername = null;

            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
            }
            dialog.dismiss();

            new Thread(() -> {
                try {
                    File localFile = new File(context.getFilesDir(), "local_comments.json");
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

                    RootieDatabase.getDatabase(context).communityDao().incrementCommentsCount(postId);
                    new FirestoreService().addCommentToPost(postId, commentMap);

                    if (savedParentCommentId != null && notifyParentUserId != null
                            && !notifyParentUserId.isEmpty() && !notifyParentUserId.equals(myUserId)) {
                        CommunityNotificationHelper.addCommunityNotificationForUser(
                                context,
                                notifyParentUserId,
                                UUID.randomUUID().toString(),
                                myUserId,
                                myUsername,
                                myAvatar,
                                "POST",
                                "REPLY",
                                "vừa trả lời bình luận của bạn",
                                postId,
                                newCommentId
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).post(refreshComments);
            }).start();
        };

        if (etComment != null) {
            etComment.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submitAction.onClick(v);
                    return true;
                }
                return false;
            });
        }

        if (ivSubmitComment != null) {
            ivSubmitComment.setOnClickListener(submitAction);
        }

        dialog.show();

        if (etComment != null) {
            etComment.requestFocus();
            etComment.post(() -> {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
                }
                if (root != null) {
                    ViewCompat.requestApplyInsets(root);
                }
            });
        }
    }

    private List<CommentItem> loadCommentsList(Context context) {
        List<CommentItem> commentsList = new ArrayList<>();
        try {
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
                    JSONArray newsArray = new JSONArray(newsSb.toString().replace("\uFEFF", ""));
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
                    JSONArray reelsArray = new JSONArray(reelsSb.toString().replace("\uFEFF", ""));
                    inlineCommentsArray = findCommentsInArray(reelsArray, "reel_id");
                    if (inlineCommentsArray == null) {
                        inlineCommentsArray = findCommentsInArray(reelsArray, "post_id");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

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

            if (inlineCommentsArray != null && inlineCommentsArray.length() > 0) {
                List<JSONObject> topLevelComments = new ArrayList<>();
                Map<String, List<JSONObject>> repliesMap = new HashMap<>();

                for (int i = 0; i < inlineCommentsArray.length(); i++) {
                    JSONObject obj = inlineCommentsArray.getJSONObject(i);
                    String parentId = obj.optString("parent_id", "null");
                    if ("null".equals(parentId) || parentId.isEmpty()) {
                        topLevelComments.add(obj);
                    } else {
                        List<JSONObject> replies = repliesMap.get(parentId);
                        if (replies == null) {
                            replies = new ArrayList<>();
                            repliesMap.put(parentId, replies);
                        }
                        replies.add(obj);
                    }
                }

                for (JSONObject obj : topLevelComments) {
                    JSONObject authorObj = obj.optJSONObject("author");
                    String userId = (authorObj != null && !authorObj.optString("user_id").isEmpty()) ? authorObj.optString("user_id") : obj.optString("user_id", "");
                    UserEntity userEntity = usersMap.get(userId);
                    String username = (authorObj != null && !authorObj.optString("username").isEmpty()) ? authorObj.optString("username") : (!obj.optString("username", "").isEmpty() ? obj.optString("username", "") : (userEntity != null ? userEntity.getUsername() : ""));
                    if (userEntity != null && userEntity.getFull_name() != null && !userEntity.getFull_name().trim().isEmpty()) {
                        username = userEntity.getFull_name();
                    }
                    String rawAvatar = (authorObj != null && !authorObj.optString("avatar").isEmpty()) ? authorObj.optString("avatar") : (!obj.optString("avatar_url", "").isEmpty() ? obj.optString("avatar_url", "") : (userEntity != null ? userEntity.getAvatar() : ""));
                    String avatarUrl = UserAvatarHelper.resolve(context, userId, rawAvatar, userEntity);
                    String commentId = obj.optString("comment_id", "");
                    
                    String currentUserId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context);
                    if (userId != null && userId.equals(currentUserId)) {
                        String sessionName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context);
                        if (sessionName != null && !sessionName.isEmpty()) username = sessionName;
                        String sessionAvatar = com.veganbeauty.app.data.local.ProfileSession.getAvatar(context);
                        if (sessionAvatar != null && !sessionAvatar.isEmpty()) avatarUrl = sessionAvatar;
                    }

                    List<JSONObject> replyList = repliesMap.get(commentId);
                    List<ReplyItem> replies = new ArrayList<>();
                    if (replyList != null) {
                        for (JSONObject replyObj : replyList) {
                            replies.add(parseReplyItem(context, replyObj, usersMap));
                        }
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
                            commentId,
                            replies
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return commentsList;
    }

    private ReplyItem parseReplyItem(Context context, JSONObject replyObj, Map<String, UserEntity> usersMap) {
        JSONObject replyAuthor = replyObj.optJSONObject("author");
        String replyUserId = (replyAuthor != null && !replyAuthor.optString("user_id").isEmpty())
                ? replyAuthor.optString("user_id") : replyObj.optString("user_id", "");
        UserEntity replyUser = usersMap.get(replyUserId);
        String replyUsername = (replyAuthor != null && !replyAuthor.optString("username").isEmpty())
                ? replyAuthor.optString("username")
                : (!replyObj.optString("username", "").isEmpty()
                ? replyObj.optString("username", "")
                : (replyUser != null ? replyUser.getUsername() : ""));
        if (replyUser != null && replyUser.getFull_name() != null && !replyUser.getFull_name().trim().isEmpty()) {
            replyUsername = replyUser.getFull_name();
        }
        String rawReplyAvatar = (replyAuthor != null && !replyAuthor.optString("avatar").isEmpty())
                ? replyAuthor.optString("avatar")
                : (!replyObj.optString("avatar_url", "").isEmpty()
                ? replyObj.optString("avatar_url", "")
                : (replyUser != null ? replyUser.getAvatar() : ""));
        String replyAvatar = UserAvatarHelper.resolve(context, replyUserId, rawReplyAvatar, replyUser);
        
        String currentUserId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context);
        if (replyUserId != null && replyUserId.equals(currentUserId)) {
            String sessionName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context);
            if (sessionName != null && !sessionName.isEmpty()) replyUsername = sessionName;
            String sessionAvatar = com.veganbeauty.app.data.local.ProfileSession.getAvatar(context);
            if (sessionAvatar != null && !sessionAvatar.isEmpty()) replyAvatar = sessionAvatar;
        }

        String replyTime = replyObj.optString("created_at", "");
        return new ReplyItem(
                replyUserId,
                replyAvatar,
                replyUsername,
                !replyTime.isEmpty() ? TimeFormatter.getTimeAgo(replyTime) : "",
                replyObj.optString("content", ""),
                replyObj.optInt("likes_count", 0),
                replyObj.optString("comment_id", "")
        );
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
        private static final int MAX_VISIBLE_REPLIES = 2;

        private final List<CommentItem> comments;
        private final RecyclerView rvComments;
        private final TextView tvEmptyComments;
        private final TextView tvCommentCount;
        private final OnCommentReplyListener replyListener;
        private final Set<String> expandedCommentIds = new HashSet<>();

        public CommentAdapter(List<CommentItem> comments, RecyclerView rvComments, TextView tvEmptyComments, TextView tvCommentCount, OnCommentReplyListener replyListener) {
            this.comments = comments;
            this.rvComments = rvComments;
            this.tvEmptyComments = tvEmptyComments;
            this.tvCommentCount = tvCommentCount;
            this.replyListener = replyListener;
        }

        class CommentViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar, ivLike;
            TextView tvUsername, tvTime, tvContent, tvLikesCount, tvAuthorTag, tvReplyAction;
            LinearLayout llReplyContainer, llRepliesList, llViewMoreReplies;
            TextView tvViewMoreReplies;

            CommentViewHolder(View view) {
                super(view);
                ivAvatar = view.findViewById(R.id.ivAvatar);
                tvUsername = view.findViewById(R.id.tvUsername);
                tvTime = view.findViewById(R.id.tvTime);
                tvContent = view.findViewById(R.id.tvContent);
                tvLikesCount = view.findViewById(R.id.tvLikesCount);
                tvAuthorTag = view.findViewById(R.id.tvAuthorTag);
                tvReplyAction = view.findViewById(R.id.tvReplyAction);
                llReplyContainer = view.findViewById(R.id.llReplyContainer);
                llRepliesList = view.findViewById(R.id.llRepliesList);
                llViewMoreReplies = view.findViewById(R.id.llViewMoreReplies);
                tvViewMoreReplies = view.findViewById(R.id.tvViewMoreReplies);
                ivLike = view.findViewById(R.id.ivLike);
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
                ComProfileNavigator.openProfile(holder.itemView.getContext(), item.userId, item.avatarUrl, item.username);
                dismiss();
            };

            holder.ivAvatar.setOnClickListener(navigateToProfile);
            holder.tvUsername.setOnClickListener(navigateToProfile);

            if (holder.tvReplyAction != null) {
                holder.tvReplyAction.setOnClickListener(v -> {
                    if (replyListener != null && item.commentId != null && !item.commentId.isEmpty()) {
                        replyListener.onReplyRequested(item.commentId, item.userId, item.username);
                    }
                });
            }

            bindReplies(holder, item, position);

            final boolean[] isLiked = {false};
            holder.ivLike.setOnClickListener(v -> {
                isLiked[0] = !isLiked[0];
                if (isLiked[0]) {
                    holder.ivLike.setImageResource(R.drawable.ic_heart_filled);
                    holder.tvLikesCount.setText(String.valueOf(item.likesCount + 1));
                } else {
                    holder.ivLike.setImageResource(R.drawable.ic_heart);
                    holder.tvLikesCount.setText(String.valueOf(item.likesCount));
                }
            });
        }

        private void bindReplies(CommentViewHolder holder, CommentItem item, int position) {
            holder.llRepliesList.removeAllViews();
            if (item.replies.isEmpty()) {
                holder.llReplyContainer.setVisibility(View.GONE);
                return;
            }

            holder.llReplyContainer.setVisibility(View.VISIBLE);

            boolean expanded = expandedCommentIds.contains(item.commentId);
            String targetCommentId = getArguments() != null ? getArguments().getString(ARG_TARGET_COMMENT_ID) : null;
            if (!expanded && targetCommentId != null) {
                for (ReplyItem reply : item.replies) {
                    if (targetCommentId.equals(reply.commentId)) {
                        expanded = true;
                        break;
                    }
                }
            }

            int total = item.replies.size();
            int visibleCount = expanded ? total : Math.min(MAX_VISIBLE_REPLIES, total);
            int hiddenCount = total - visibleCount;

            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (int i = 0; i < visibleCount; i++) {
                ReplyItem reply = item.replies.get(i);
                View replyView = inflater.inflate(R.layout.com_item_comment_reply, holder.llRepliesList, false);

                ImageView ivReplyAvatar = replyView.findViewById(R.id.ivReplyAvatar);
                TextView tvReplyUsername = replyView.findViewById(R.id.tvReplyUsername);
                TextView tvReplyTime = replyView.findViewById(R.id.tvReplyTime);
                TextView tvReplyContent = replyView.findViewById(R.id.tvReplyContent);
                TextView tvReplyLikesCount = replyView.findViewById(R.id.tvReplyLikesCount);
                ImageView ivReplyLike = replyView.findViewById(R.id.ivReplyLike);

                tvReplyUsername.setText(reply.username);
                tvReplyTime.setText(reply.timeStr);
                tvReplyContent.setText(reply.content);
                tvReplyLikesCount.setText(String.valueOf(reply.likesCount));

                com.bumptech.glide.Glide.with(ivReplyAvatar.getContext())
                        .load(reply.avatarUrl)
                        .placeholder(R.drawable.img_avatar)
                        .error(R.drawable.img_avatar)
                        .into(ivReplyAvatar);

                View.OnClickListener navigateToReplyProfile = v -> {
                    ComProfileNavigator.openProfile(holder.itemView.getContext(), reply.userId, reply.avatarUrl, reply.username);
                    dismiss();
                };
                ivReplyAvatar.setOnClickListener(navigateToReplyProfile);
                tvReplyUsername.setOnClickListener(navigateToReplyProfile);

                final boolean[] isReplyLiked = {false};
                ivReplyLike.setOnClickListener(v -> {
                    isReplyLiked[0] = !isReplyLiked[0];
                    if (isReplyLiked[0]) {
                        ivReplyLike.setImageResource(R.drawable.ic_heart_filled);
                        tvReplyLikesCount.setText(String.valueOf(reply.likesCount + 1));
                    } else {
                        ivReplyLike.setImageResource(R.drawable.ic_heart);
                        tvReplyLikesCount.setText(String.valueOf(reply.likesCount));
                    }
                });

                holder.llRepliesList.addView(replyView);
            }

            if (hiddenCount > 0) {
                holder.llViewMoreReplies.setVisibility(View.VISIBLE);
                holder.tvViewMoreReplies.setText(hiddenCount > 1
                        ? "Xem " + hiddenCount + " câu trả lời"
                        : "Xem 1 câu trả lời");
                holder.llViewMoreReplies.setOnClickListener(v -> {
                    expandedCommentIds.add(item.commentId);
                    notifyItemChanged(position);
                });
            } else {
                holder.llViewMoreReplies.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }
    }
}
