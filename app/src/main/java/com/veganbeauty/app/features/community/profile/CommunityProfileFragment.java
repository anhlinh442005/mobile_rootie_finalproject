package com.veganbeauty.app.features.community.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.view.GravityCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.navigation.NavigationView;
import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentProfileBinding;
import com.veganbeauty.app.features.community.UserMemoryHelper;
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.features.community.message.ChatDetailFragment;
import com.veganbeauty.app.features.community.message.MessageHelper;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.SideMenuHelper;

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

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class CommunityProfileFragment extends RootieFragment {

    private ComFragmentProfileBinding binding;
    private CommunityViewModel viewModel;

    private static final String ARG_USER_ID = "USER_ID";
    private int currentTab = 0;
    private List<CommunityPostEntity> currentAllPosts = new ArrayList<>();
    private String currentUserId = "test_001";
    private String ownUserId = "test_001";
    private boolean isFollowing = false;
    private int currentFollowersCount = 0;
    private int currentFollowingCount = 0;

    public static CommunityProfileFragment newInstance(String userId) {
        CommunityProfileFragment fragment = new CommunityProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context ctx = requireContext();
        LocalJsonReader jsonReader = new LocalJsonReader(ctx);

        binding.ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityFeedFragment())
                        .commit();
            }
        });

        binding.ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.ivSettings.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.END));

        NavigationView navView = binding.getRoot().findViewById(R.id.navView);
        if (navView != null) {
            ImageView ivCloseMenu = navView.findViewById(R.id.ivCloseMenu);
            if (ivCloseMenu != null) {
                ivCloseMenu.setOnClickListener(v -> binding.drawerLayout.closeDrawer(GravityCompat.END));
            }
            LinearLayout llLogout = navView.findViewById(R.id.llLogout);
            if (llLogout != null) {
                llLogout.setOnClickListener(v -> showLogoutDialog());
            }
            SideMenuHelper.bindCurrentUser(navView);
        }

        String passedUserId = null;
        if (getArguments() != null) {
            passedUserId = getArguments().getString(ARG_USER_ID);
            if (passedUserId == null) {
                passedUserId = getArguments().getString("USER_ID");
            }
        }

        String loggedInEmail = ProfileSession.INSTANCE.getEmail(ctx);
        try {
            String usersJsonStr = jsonReader.getRawUsersJson();
            JSONArray usersJsonArray = new JSONArray(usersJsonStr);
            for (int i = 0; i < usersJsonArray.length(); i++) {
                JSONObject obj = usersJsonArray.getJSONObject(i);
                if (obj.optString("email").equals(loggedInEmail)) {
                    ownUserId = obj.optString("user_id", "test_001");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentUserId = (passedUserId != null) ? passedUserId : ownUserId;
        boolean isOwnProfile = currentUserId.equals(ownUserId);

        String bioText = isOwnProfile ? ProfileSession.INSTANCE.getBio(ctx) : "Empowering confidence through beauty and self-care.";
        String jsonPrimaryImage = "";
        String jsonUsername = isOwnProfile ? ProfileSession.INSTANCE.getUsername(ctx) : "@" + currentUserId;
        String jsonAvatar = "";
        String jsonFullName = "";

        try {
            String usersJsonStr = jsonReader.getRawUsersJson();
            JSONArray usersJsonArray = new JSONArray(usersJsonStr);
            for (int i = 0; i < usersJsonArray.length(); i++) {
                JSONObject obj = usersJsonArray.getJSONObject(i);
                if (obj.optString("user_id").equals(currentUserId)) {
                    String bio = obj.optString("bio");
                    if (!bio.isEmpty() && !isOwnProfile) bioText = bio;
                    jsonPrimaryImage = obj.optString("primary_image");
                    jsonFullName = obj.optString("full_name");
                    String uname = obj.optString("username");
                    if (!uname.isEmpty() && !isOwnProfile) jsonUsername = "@" + uname.replace(" ", "").toLowerCase();
                    jsonAvatar = obj.optString("avatar");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String argAvatarUrl = getArguments() != null ? getArguments().getString("AVATAR_URL") : null;
        final String finalAvatarUrl = (argAvatarUrl != null) ? argAvatarUrl : jsonAvatar;
        
        String argUserName = getArguments() != null ? getArguments().getString("USER_NAME") : null;
        String fallbackName = (argUserName != null) ? argUserName : jsonFullName;

        try {
            String friendJsonStr = getFriendsJsonString(ctx);
            JSONArray friendJsonArray = new JSONArray(friendJsonStr);

            for (int i = 0; i < friendJsonArray.length(); i++) {
                JSONObject obj = friendJsonArray.getJSONObject(i);
                if (obj.optString("user_id").equals(ownUserId)) {
                    JSONArray followingArr = obj.optJSONArray("following");
                    if (followingArr != null) {
                        for (int j = 0; j < followingArr.length(); j++) {
                            if (followingArr.optString(j).equals(currentUserId)) {
                                isFollowing = true;
                                break;
                            }
                        }
                    }
                    break;
                }
            }

            for (int i = 0; i < friendJsonArray.length(); i++) {
                JSONObject obj = friendJsonArray.getJSONObject(i);
                if (obj.optString("user_id").equals(currentUserId)) {
                    JSONArray followersArr = obj.optJSONArray("followers");
                    if (followersArr != null) currentFollowersCount = followersArr.length();
                    JSONArray followingArr = obj.optJSONArray("following");
                    if (followingArr != null) currentFollowingCount = followingArr.length();
                    break;
                }
            }
            binding.tvFollowersCount.setText(String.valueOf(currentFollowersCount));
            binding.tvFollowingCount.setText(String.valueOf(currentFollowingCount));
        } catch (Exception e) {
            e.printStackTrace();
            binding.tvFollowersCount.setText("0");
            binding.tvFollowingCount.setText("0");
        }

        if (isOwnProfile) {
            String fullName = ProfileSession.INSTANCE.getFullName(ctx);
            if (fullName != null) binding.tvName.setText(fullName);
            String uname = ProfileSession.INSTANCE.getUsername(ctx);
            if (uname != null) binding.tvUsername.setText(uname.startsWith("@") ? uname : "@" + uname);
        } else {
            binding.tvName.setText(!fallbackName.isEmpty() ? fallbackName : "Người dùng");
            binding.tvUsername.setText(jsonUsername);
        }

        String effectiveAvatarUrl = isOwnProfile
                ? ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx)
                : finalAvatarUrl;
        if (effectiveAvatarUrl != null && !effectiveAvatarUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(binding.ivAvatar.getContext()).load(effectiveAvatarUrl).error(R.drawable.img_avatar).circleCrop().into(binding.ivAvatar);
        } else {
            binding.ivAvatar.setImageResource(R.drawable.img_avatar);
        }

        if (isOwnProfile) {
            new Thread(() -> {
                UserEntity savedUser = ProfileSessionHelper.findCurrentUser(ctx);
                String resolvedAvatar = ProfileSessionHelper.resolveEffectiveAvatarUrl(ctx, savedUser);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    ProfileSession.setAvatar(ctx, resolvedAvatar);
                    com.bumptech.glide.Glide.with(binding.ivAvatar.getContext())
                            .load(resolvedAvatar)
                            .error(R.drawable.img_avatar)
                            .circleCrop()
                            .into(binding.ivAvatar);
                });
            }).start();
        }

        if (isOwnProfile) {
            binding.btnEditProfile.setText("Chỉnh sửa");
            binding.btnShareProfile.setText("Chia sẻ");
            binding.btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6E846A")));
            binding.btnEditProfile.setTextColor(Color.WHITE);

            binding.btnEditProfile.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityEditProfileFragment())
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            if (isFollowing) {
                binding.btnEditProfile.setText("Đã theo dõi");
                binding.btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                binding.btnEditProfile.setTextColor(Color.parseColor("#6E846A"));
            } else {
                binding.btnEditProfile.setText("Theo dõi");
                binding.btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6E846A")));
                binding.btnEditProfile.setTextColor(Color.WHITE);
            }
            binding.btnShareProfile.setText("Nhắn tin");

            binding.btnShareProfile.setOnClickListener(v -> {
                String partnerName = !fallbackName.isEmpty() ? fallbackName : "Người dùng";
                String convId = MessageHelper.getOrCreateConversation(ctx, ownUserId, currentUserId, partnerName, finalAvatarUrl);
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, ChatDetailFragment.newInstance(convId))
                        .addToBackStack(null)
                        .commit();
            });

            binding.btnEditProfile.setOnClickListener(v -> toggleFollowStatus(ctx));
        }

        binding.tvBio.setText(bioText);

        String primaryImage = !jsonPrimaryImage.isEmpty() ? jsonPrimaryImage : (isOwnProfile ? ProfileSession.INSTANCE.getPrimaryImage(ctx) : "");
        if (primaryImage != null && !primaryImage.isEmpty()) {
            com.bumptech.glide.Glide.with(binding.ivCover.getContext()).load(primaryImage).into(binding.ivCover);
        } else {
            binding.ivCover.setImageResource(R.color.primary);
        }

        if (effectiveAvatarUrl != null && !effectiveAvatarUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(binding.ivHighlight1.getContext()).load(effectiveAvatarUrl).into(binding.ivHighlight1);
            com.bumptech.glide.Glide.with(binding.ivHighlight2.getContext()).load(effectiveAvatarUrl).into(binding.ivHighlight2);
        }

        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String savedSkinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Sức khoẻ - Làm đẹp");
        binding.tvProfileSkinType.setText(savedSkinType);

        binding.rvPosts.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        if (getArguments() != null) {
            currentTab = getArguments().getInt("SELECTED_TAB", 0);
        }

        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), jsonReader, new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);

        binding.rlTabGrid.setOnClickListener(v -> {
            currentTab = 0;
            updateTabsUI();
            loadPostsForCurrentTab(currentAllPosts);
        });
        binding.ivTabVideo.setOnClickListener(v -> {
            currentTab = 1;
            updateTabsUI();
            loadPostsForCurrentTab(currentAllPosts);
        });
        binding.ivTabReup.setOnClickListener(v -> {
            currentTab = 2;
            updateTabsUI();
            loadPostsForCurrentTab(currentAllPosts);
        });
        binding.ivTabBookmark.setOnClickListener(v -> {
            currentTab = 3;
            updateTabsUI();
            loadPostsForCurrentTab(currentAllPosts);
        });

        updateTabsUI();

        binding.llShowcase.setOnClickListener(v -> {
            CommunityShowcaseFragment fragment = new CommunityShowcaseFragment();
            Bundle args = new Bundle();
            args.putString("USER_ID", currentUserId);
            args.putString("AVATAR_URL", finalAvatarUrl);
            args.putString("USER_NAME", binding.tvName.getText().toString());
            args.putString("COVER_URL", primaryImage);
            fragment.setArguments(args);
            
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.llRevenue.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityRevenueFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void showLogoutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.com_dialog_logout_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextView btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelLogout);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            ProfileSession.INSTANCE.setLoggedIn(requireContext(), false);
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private String getFriendsJsonString(Context ctx) throws Exception {
        File file = new File(ctx.getFilesDir(), "User_com_friend.json");
        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getAssets().open("User_com_friend.json")));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        }
    }

    private void toggleFollowStatus(Context ctx) {
        try {
            File file = new File(ctx.getFilesDir(), "User_com_friend.json");
            String currentFriendStr = getFriendsJsonString(ctx);
            JSONArray friendJsonArray = new JSONArray(currentFriendStr);

            if (!isFollowing) {
                for (int i = 0; i < friendJsonArray.length(); i++) {
                    JSONObject obj = friendJsonArray.getJSONObject(i);
                    if (obj.optString("user_id").equals(ownUserId)) {
                        JSONArray followingArr = obj.optJSONArray("following");
                        if (followingArr == null) followingArr = new JSONArray();
                        followingArr.put(currentUserId);
                        obj.put("following", followingArr);
                    }
                    if (obj.optString("user_id").equals(currentUserId)) {
                        JSONArray followersArr = obj.optJSONArray("followers");
                        if (followersArr == null) followersArr = new JSONArray();
                        followersArr.put(ownUserId);
                        obj.put("followers", followersArr);
                    }
                }
                isFollowing = true;
                currentFollowersCount++;

                binding.btnEditProfile.setText("Đã theo dõi");
                binding.btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
                binding.btnEditProfile.setTextColor(Color.parseColor("#6E846A"));
            } else {
                for (int i = 0; i < friendJsonArray.length(); i++) {
                    JSONObject obj = friendJsonArray.getJSONObject(i);
                    if (obj.optString("user_id").equals(ownUserId)) {
                        JSONArray followingArr = obj.optJSONArray("following");
                        if (followingArr == null) followingArr = new JSONArray();
                        JSONArray newFollowing = new JSONArray();
                        for (int j = 0; j < followingArr.length(); j++) {
                            if (!followingArr.optString(j).equals(currentUserId)) {
                                newFollowing.put(followingArr.getString(j));
                            }
                        }
                        obj.put("following", newFollowing);
                    }
                    if (obj.optString("user_id").equals(currentUserId)) {
                        JSONArray followersArr = obj.optJSONArray("followers");
                        if (followersArr == null) followersArr = new JSONArray();
                        JSONArray newFollowers = new JSONArray();
                        for (int j = 0; j < followersArr.length(); j++) {
                            if (!followersArr.optString(j).equals(ownUserId)) {
                                newFollowers.put(followersArr.getString(j));
                            }
                        }
                        obj.put("followers", newFollowers);
                    }
                }
                isFollowing = false;
                currentFollowersCount = Math.max(0, currentFollowersCount - 1);

                binding.btnEditProfile.setText("Theo dõi");
                binding.btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6E846A")));
                binding.btnEditProfile.setTextColor(Color.WHITE);
            }

            FileWriter fw = new FileWriter(file);
            fw.write(friendJsonArray.toString(2));
            fw.close();
            binding.tvFollowersCount.setText(String.valueOf(currentFollowersCount));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTabsUI() {
        binding.ivTabGrid.setColorFilter(currentTab == 0 ? Color.parseColor("#6E846A") : Color.parseColor("#888888"));
        binding.vTabGridIndicator.setVisibility(currentTab == 0 ? View.VISIBLE : View.INVISIBLE);

        binding.ivTabVideo.setColorFilter(currentTab == 1 ? Color.parseColor("#6E846A") : Color.parseColor("#888888"));
        binding.ivTabReup.setColorFilter(currentTab == 2 ? Color.parseColor("#6E846A") : Color.parseColor("#888888"));
        binding.ivTabBookmark.setColorFilter(currentTab == 3 ? Color.parseColor("#6E846A") : Color.parseColor("#888888"));
    }

    private void loadPostsForCurrentTab(List<CommunityPostEntity> allPosts) {
        List<CommunityPostEntity> myPosts = new ArrayList<>();
        HashSet<String> seenIds = new HashSet<>();
        
        for (CommunityPostEntity post : allPosts) {
            if (seenIds.contains(post.getPostId())) continue;
            
            boolean include = false;
            if (currentTab == 0) {
                include = currentUserId.equals(post.getAuthorId());
            } else if (currentTab == 1) {
                include = currentUserId.equals(post.getAuthorId()); // Video filter logic if needed
            } else if (currentTab == 2) {
                include = com.veganbeauty.app.features.community.UserMemoryHelper.isPostReposted(requireContext(), ownUserId, post.getPostId());
            } else if (currentTab == 3) {
                include = com.veganbeauty.app.features.community.UserMemoryHelper.isPostSaved(requireContext(), ownUserId, post.getPostId());
            } else {
                include = currentUserId.equals(post.getAuthorId());
            }

            if (include) {
                myPosts.add(post);
                seenIds.add(post.getPostId());
            }
        }

        Collections.sort(myPosts, (o1, o2) -> {
            String t1 = o1.getCreatedAt() != null ? o1.getCreatedAt() : "";
            String t2 = o2.getCreatedAt() != null ? o2.getCreatedAt() : "";
            return t2.compareTo(t1);
        });

        if (currentTab == 0) {
            binding.tvPostCount.setText(String.valueOf(myPosts.size()));
        }

        ProfileGridAdapter adapter = new ProfileGridAdapter(myPosts, position -> {
            ProfilePostDetailFragment fragment = ProfilePostDetailFragment.newInstance(currentUserId, position, currentTab, "");
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvPosts.setAdapter(adapter);
    }

    @Override
    public void observeViewModel() {
        Context ctx = getContext();
        if (ctx == null) return;
        LocalJsonReader jsonReader = new LocalJsonReader(ctx);
        viewModel.getPosts().observe(getViewLifecycleOwner(), dbPosts -> {
            if (binding == null || !isAdded()) return;
            List<CommunityPostEntity> newsList = jsonReader.getCommunityNews();
            currentAllPosts = new ArrayList<>();
            if (dbPosts != null) currentAllPosts.addAll(dbPosts);
            if (newsList != null) currentAllPosts.addAll(newsList);
            loadPostsForCurrentTab(currentAllPosts);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            NavigationView navView = binding.getRoot().findViewById(R.id.navView);
            if (navView != null) {
                SideMenuHelper.bindCurrentUser(navView);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
