package com.veganbeauty.app.features.community.beauty_hub;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;


import coil.Coil;
import coil.ImageLoader;
import coil.decode.SvgDecoder;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.databinding.ComFragmentNewsBinding;
import com.veganbeauty.app.features.community.com_feed.PostAdapter;
import com.veganbeauty.app.features.community.message.ChatDetailFragment;
import com.veganbeauty.app.features.community.message.MessageHelper;
import com.veganbeauty.app.utils.RootieBrandHelper;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.Triple;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class CommunityNewsFragment extends RootieFragment {

    private ComFragmentNewsBinding binding;
    private final PostAdapter postAdapter = new PostAdapter();

    private String currentUserId = "test_001";
    private boolean isFollowing = false;
    private int rootieFollowersCount = 0;

    public static CommunityNewsFragment newInstance(String targetPostId) {
        CommunityNewsFragment fragment = new CommunityNewsFragment();
        Bundle args = new Bundle();
        args.putString("TARGET_POST_ID", targetPostId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        binding.rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNews.setAdapter(postAdapter);

        binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.tvName.setText("Rootie VietNam");
        binding.ivAvatar.setImageResource(R.drawable.imv_logo);

        com.bumptech.glide.Glide.with(binding.ivCover.getContext()).load("https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780843940/bbc9ba9c-790c-481c-9600-ad6736337cba_mszen2.png").placeholder(R.drawable.img_beautyhub_banner).error(R.drawable.img_beautyhub_banner).into(binding.ivCover);

        try {
            String loggedInEmail = ProfileSession.getEmail(requireContext());
            BufferedReader br = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("users.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            
            JSONArray usersArray = new JSONArray(sb.toString());
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject obj = usersArray.getJSONObject(i);
                if (loggedInEmail != null && loggedInEmail.equals(obj.optString("email"))) {
                    currentUserId = obj.optString("user_id", "test_001");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String friendJsonStr = getFriendsJsonString(requireContext());
            JSONArray friendJsonArray = new JSONArray(friendJsonStr);

            for (int i = 0; i < friendJsonArray.length(); i++) {
                JSONObject obj = friendJsonArray.getJSONObject(i);
                if (currentUserId.equals(obj.optString("user_id"))) {
                    JSONArray followingArr = obj.optJSONArray("following");
                    if (followingArr != null) {
                        for (int j = 0; j < followingArr.length(); j++) {
                            if ("rootie_vn".equals(followingArr.optString(j))) {
                                isFollowing = true;
                                break;
                            }
                        }
                    }
                }
                if ("rootie_vn".equals(obj.optString("user_id"))) {
                    JSONArray followersArr = obj.optJSONArray("followers");
                    rootieFollowersCount = followersArr != null ? followersArr.length() : 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateFollowButtonUI();

        binding.btnFollow.setOnClickListener(v -> {
            if (isFollowing) {
                View dialogView = getLayoutInflater().inflate(R.layout.com_dialog_unfollow_confirm, null);
                AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                        .setView(dialogView)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }

                dialogView.findViewById(R.id.btnCancel).setOnClickListener(cv -> dialog.dismiss());

                dialogView.findViewById(R.id.btnUnfollow).setOnClickListener(uv -> {
                    dialog.dismiss();
                    try {
                        File file = new File(requireContext().getFilesDir(), "User_com_friend.json");
                        String currentFriendStr = getFriendsJsonString(requireContext());
                        JSONArray friendJsonArray = new JSONArray(currentFriendStr);

                        for (int i = 0; i < friendJsonArray.length(); i++) {
                            JSONObject obj = friendJsonArray.getJSONObject(i);
                            if (currentUserId.equals(obj.optString("user_id"))) {
                                JSONArray followingArr = obj.optJSONArray("following");
                                if (followingArr == null) followingArr = new JSONArray();
                                JSONArray newFollowing = new JSONArray();
                                for (int j = 0; j < followingArr.length(); j++) {
                                    if (!"rootie_vn".equals(followingArr.optString(j))) {
                                        newFollowing.put(followingArr.getString(j));
                                    }
                                }
                                obj.put("following", newFollowing);
                            }
                            if ("rootie_vn".equals(obj.optString("user_id"))) {
                                JSONArray followersArr = obj.optJSONArray("followers");
                                if (followersArr == null) followersArr = new JSONArray();
                                JSONArray newFollowers = new JSONArray();
                                for (int j = 0; j < followersArr.length(); j++) {
                                    if (!currentUserId.equals(followersArr.optString(j))) {
                                        newFollowers.put(followersArr.getString(j));
                                    }
                                }
                                obj.put("followers", newFollowers);
                            }
                        }
                        isFollowing = false;
                        rootieFollowersCount = Math.max(0, rootieFollowersCount - 1);
                        FileWriter writer = new FileWriter(file);
                        writer.write(friendJsonArray.toString(2));
                        writer.close();
                        updateFollowButtonUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                dialog.show();
            } else {
                try {
                    File file = new File(requireContext().getFilesDir(), "User_com_friend.json");
                    String currentFriendStr = getFriendsJsonString(requireContext());
                    JSONArray friendJsonArray = new JSONArray(currentFriendStr);

                    for (int i = 0; i < friendJsonArray.length(); i++) {
                        JSONObject obj = friendJsonArray.getJSONObject(i);
                        if (currentUserId.equals(obj.optString("user_id"))) {
                            JSONArray followingArr = obj.optJSONArray("following");
                            if (followingArr == null) followingArr = new JSONArray();
                            followingArr.put("rootie_vn");
                            obj.put("following", followingArr);
                        }
                        if ("rootie_vn".equals(obj.optString("user_id"))) {
                            JSONArray followersArr = obj.optJSONArray("followers");
                            if (followersArr == null) followersArr = new JSONArray();
                            followersArr.put(currentUserId);
                            obj.put("followers", followersArr);
                        }
                    }
                    isFollowing = true;
                    rootieFollowersCount++;
                    FileWriter writer = new FileWriter(file);
                    writer.write(friendJsonArray.toString(2));
                    writer.close();
                    updateFollowButtonUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        binding.btnMessage.setOnClickListener(v -> {
            String convId = MessageHelper.getOrCreateConversation(
                    requireContext(),
                    currentUserId,
                    "rootie_vn",
                    "Rootie VietNam",
                    RootieBrandHelper.AVATAR_URL
            );
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, ChatDetailFragment.newInstance(convId))
                    .addToBackStack(null)
                    .commit();
        });

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((coroutineScope, continuation) -> {
            return BuildersKt.withContext(Dispatchers.getIO(), (cScope, cCont) -> {
                List<Triple<String, String, String>> mutualUsers = new ArrayList<>();
                List<CommunityPostEntity> newsPosts = new ArrayList<>();
                try {
                    String usersStr;
                    BufferedReader br = new BufferedReader(new InputStreamReader(requireContext().getAssets().open("users.json")));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    usersStr = sb.toString();
                    JSONArray usersArray = new JSONArray(usersStr);

                    String friendStr = getFriendsJsonString(requireContext());
                    JSONArray friendArray = new JSONArray(friendStr);

                    List<String> myFollowing = new ArrayList<>();
                    List<String> rootieFollowers = new ArrayList<>();

                    for (int i = 0; i < friendArray.length(); i++) {
                        JSONObject obj = friendArray.getJSONObject(i);
                        String uid = obj.optString("user_id");
                        if (currentUserId.equals(uid)) {
                            JSONArray followingArr = obj.optJSONArray("following");
                            if (followingArr != null) {
                                for (int j = 0; j < followingArr.length(); j++) {
                                    myFollowing.add(followingArr.getString(j));
                                }
                            }
                        } else if ("rootie_vn".equals(uid)) {
                            JSONArray followersArr = obj.optJSONArray("followers");
                            if (followersArr != null) {
                                for (int j = 0; j < followersArr.length(); j++) {
                                    rootieFollowers.add(followersArr.getString(j));
                                }
                            }
                        }
                    }

                    Set<String> mutualIds = new HashSet<>(myFollowing);
                    mutualIds.retainAll(new HashSet<>(rootieFollowers));

                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject obj = usersArray.getJSONObject(i);
                        String uid = obj.optString("user_id");
                        if (mutualIds.contains(uid)) {
                            String name = obj.optString("username", "Người dùng");
                            String avt = obj.optString("avatar", "");
                            mutualUsers.add(new Triple<>(name, avt, uid));
                        }
                    }

                    LocalJsonReader jsonReader = new LocalJsonReader(requireContext());
                    newsPosts.addAll(jsonReader.getCommunityNews());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                BuildersKt.withContext(Dispatchers.getMain(), (mScope, mCont) -> {
                    if (!mutualUsers.isEmpty()) {
                        binding.llMutualInfo.setVisibility(View.VISIBLE);

                        List<String> avatars = new ArrayList<>();
                        for (Triple<String, String, String> user : mutualUsers) {
                            if (!user.getSecond().isEmpty()) avatars.add(user.getSecond());
                        }

                        binding.ivMutual1.setVisibility(avatars.size() > 0 ? View.VISIBLE : View.GONE);
                        binding.ivMutual2.setVisibility(avatars.size() > 1 ? View.VISIBLE : View.GONE);
                        binding.ivMutual3.setVisibility(avatars.size() > 2 ? View.VISIBLE : View.GONE);

                        ImageLoader imageLoader = Coil.imageLoader(requireContext());
                        if (avatars.size() > 0) {
                            imageLoader.enqueue(new ImageRequest.Builder(binding.ivMutual1.getContext())
                                    .data(avatars.get(0))
                                    .decoderFactory(new SvgDecoder.Factory())
                                    .error(R.drawable.img_avatar)
                                    .transformations(new coil.transform.CircleCropTransformation())
                                    .target(binding.ivMutual1)
                                    .build());
                        }
                        if (avatars.size() > 1) {
                            imageLoader.enqueue(new ImageRequest.Builder(binding.ivMutual2.getContext())
                                    .data(avatars.get(1))
                                    .decoderFactory(new SvgDecoder.Factory())
                                    .error(R.drawable.img_avatar)
                                    .transformations(new coil.transform.CircleCropTransformation())
                                    .target(binding.ivMutual2)
                                    .build());
                        }
                        if (avatars.size() > 2) {
                            imageLoader.enqueue(new ImageRequest.Builder(binding.ivMutual3.getContext())
                                    .data(avatars.get(2))
                                    .decoderFactory(new SvgDecoder.Factory())
                                    .error(R.drawable.img_avatar)
                                    .transformations(new coil.transform.CircleCropTransformation())
                                    .target(binding.ivMutual3)
                                    .build());
                        }

                        int count = mutualUsers.size();
                        if (count == 1) {
                            binding.tvMutualCount.setText("Có " + mutualUsers.get(0).getFirst() + " đang theo dõi");
                        } else {
                            binding.tvMutualCount.setText("Có " + mutualUsers.get(0).getFirst() + " và " + (count - 1) + " người khác theo dõi");
                        }

                        binding.llMutualInfo.setOnClickListener(v -> {
                            View dialogView = getLayoutInflater().inflate(R.layout.com_dialog_mutual_followers, null);
                            LinearLayout llFollowerList = dialogView.findViewById(R.id.llFollowerList);

                            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                    .setView(dialogView)
                                    .create();

                            for (Triple<String, String, String> user : mutualUsers) {
                                View itemView = getLayoutInflater().inflate(R.layout.com_item_mutual_follower, llFollowerList, false);
                                ImageView ivAvatar = itemView.findViewById(R.id.ivItemAvatar);
                                TextView tvName = itemView.findViewById(R.id.tvItemName);

                                tvName.setText(user.getFirst());
                                if (!user.getSecond().isEmpty()) {
                                    imageLoader.enqueue(new ImageRequest.Builder(ivAvatar.getContext())
                                            .data(user.getSecond())
                                            .decoderFactory(new SvgDecoder.Factory())
                                            .error(R.drawable.img_avatar)
                                            .target(ivAvatar)
                                            .build());
                                }

                                itemView.setOnClickListener(iv -> {
                                    dialog.dismiss();
                                    getParentFragmentManager().beginTransaction()
                                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                            .replace(R.id.main_container, CommunityProfileFragment.newInstance(user.getThird()))
                                            .addToBackStack(null)
                                            .commit();
                                });

                                llFollowerList.addView(itemView);
                            }

                            if (dialog.getWindow() != null) {
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                            }
                            dialog.show();
                        });

                    } else {
                        binding.llMutualInfo.setVisibility(View.GONE);
                    }

                    Collections.sort(newsPosts, (p1, p2) -> {
                        String c1 = p1.getCreatedAt();
                        String c2 = p2.getCreatedAt();
                        if (c1 == null) c1 = "";
                        if (c2 == null) c2 = "";
                        try {
                            return Long.compare(Long.parseLong(c2), Long.parseLong(c1));
                        } catch (Exception e) {
                            return c2.compareTo(c1);
                        }
                    });
                    postAdapter.updateData(newsPosts, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

                    Bundle args = getArguments();
                    if (args != null) {
                        String targetPostId = args.getString("TARGET_POST_ID");
                        if (targetPostId != null && !targetPostId.isEmpty()) {
                            int index = -1;
                            for (int i = 0; i < newsPosts.size(); i++) {
                                if (targetPostId.equals(newsPosts.get(i).getPostId())) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                int finalIndex = index;
                                binding.rvNews.post(() -> {
                                    if (binding.rvNews.getLayoutManager() instanceof LinearLayoutManager) {
                                        ((LinearLayoutManager) binding.rvNews.getLayoutManager()).scrollToPositionWithOffset(finalIndex, 0);
                                    }
                                });
                            }
                        }
                    }
                    return kotlin.Unit.INSTANCE;
                }, cCont);
                return kotlin.Unit.INSTANCE;
            }, continuation);
        });
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            binding.btnFollow.setText("Đã theo dõi");
            binding.btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            binding.btnFollow.setTextColor(Color.parseColor("#6E846A"));
        } else {
            binding.btnFollow.setText("Theo dõi");
            binding.btnFollow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6E846A")));
            binding.btnFollow.setTextColor(Color.WHITE);
        }

        String mutualCountStr = "";
        String tvText = binding.tvMutualCount.getText().toString();
        if (tvText.contains("theo dõi")) {
            String[] parts = tvText.split(" ");
            String numStr = "6";
            for (int i = parts.length - 1; i >= 0; i--) {
                try {
                    Integer.parseInt(parts[i]);
                    numStr = parts[i];
                    break;
                } catch (NumberFormatException ignored) {}
            }
            mutualCountStr = " • " + numStr + " đang theo dõi";
        } else {
            mutualCountStr = " • 6 đang theo dõi";
        }
        binding.tvStats.setText(rootieFollowersCount + "K người theo dõi" + mutualCountStr + "\n1.046 bài viết");
    }

    private String getFriendsJsonString(Context ctx) {
        try {
            File file = new File(ctx.getFilesDir(), "User_com_friend.json");
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getAssets().open("User_com_friend.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    @Override
    public void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
