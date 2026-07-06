package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import com.veganbeauty.app.data.repository.CommunityNotificationRepository;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentFeedBinding;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.CommunitySocialHelper;
import com.veganbeauty.app.features.community.message.CommunityMessageFragment;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.utils.ComBottomNavHelper;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.ProfileUpdateNotifier;
import com.veganbeauty.app.utils.SideMenuHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;

public class CommunityFeedFragment extends RootieFragment {

    private ComFragmentFeedBinding binding;
    private CommunityViewModel viewModel;
    private StoryAdapter storyAdapter = new StoryAdapter(new ArrayList<>());
    private PostAdapter postAdapter = new PostAdapter();

    private boolean isNavVisible = true;
    private int currentPage = 1;
    private final int postsPerPage = 5;
    private boolean isLoadingMore = false;
    private List<CommunityPostEntity> allFilteredPosts = new ArrayList<>();
    private String currentFilter = "Tất cả";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<UserEntity> lastStoryUsers = new ArrayList<>();

    private final ProfileUpdateNotifier.Listener profileUpdateListener = () -> {
        if (binding != null && isAdded()) {
            refreshStoriesFromSession();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentFeedBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        Context ctx = getContext();
        if (ctx == null) return;
        RootieDatabase db = RootieDatabase.getDatabase(ctx);
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(ctx), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.rvStories.setAdapter(storyAdapter);
        binding.rvPosts.setAdapter(postAdapter);
        ProfileUpdateNotifier.addListener(profileUpdateListener);
        refreshFollowingState();
        postAdapter.setOnFollowStateChangedListener(this::onFollowStateChanged);

        binding.ivHome.setOnClickListener(v -> ComBottomNavHelper.navigateToAppHome(this));

        binding.ivMenu.setOnClickListener(v -> {
            if (binding.drawerLayout != null) {
                SideMenuHelper.bindCurrentUser(binding.navView);
                SideMenuHelper.setupMenuNavigation(binding.navView, getParentFragmentManager(), binding.drawerLayout);
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        if (binding.drawerLayout != null) {
            binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerOpened(@NonNull View drawerView) {
                    if (binding != null && binding.navView != null) {
                        SideMenuHelper.bindCurrentUser(binding.navView);
                        SideMenuHelper.setupMenuNavigation(binding.navView, getParentFragmentManager(), binding.drawerLayout);
                    }
                }
            });
        }

        binding.ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        View ivCloseMenu = view.findViewById(R.id.ivCloseMenu);
        if (ivCloseMenu != null) {
            ivCloseMenu.setOnClickListener(v -> {
                if (binding.drawerLayout != null) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                }
            });
        }

        View llLogout = view.findViewById(R.id.llLogout);
        if (llLogout != null) {
            llLogout.setOnClickListener(v -> {
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.com_dialog_logout_confirm, null);
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create();

                TextView btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);
                TextView btnCancel = dialogView.findViewById(R.id.btnCancelLogout);

                btnConfirm.setOnClickListener(v1 -> {
                    dialog.dismiss();
                    ProfileSession.setLoggedIn(requireContext(), false);
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                });

                btnCancel.setOnClickListener(v12 -> dialog.dismiss());

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
                dialog.show();
            });
        }

        binding.ivSearch.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunitySearchFragment())
                    .addToBackStack(null)
                    .commit();
        });

        ComBottomNavHelper.setup(this, binding.comBottomNav.getRoot(), ComBottomNavHelper.TAB_FEED, tabId -> {
            if (tabId == ComBottomNavHelper.TAB_FEED) {
                binding.nsvFeed.smoothScrollTo(0, 0);
                mainHandler.postDelayed(() -> {
                    if (isAdded()) updateFeedData(true);
                }, 800);
                return ComBottomNavHelper.INTERCEPT_CONSUME;
            }
            return ComBottomNavHelper.INTERCEPT_NOT_HANDLED;
        });

        binding.nsvFeed.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int dy = scrollY - oldScrollY;
            if (dy > 15) {
                hideBottomNavigation();
            } else if (dy < -15) {
                showBottomNavigation();
            }

            if (!v.canScrollVertically(1) && !"Reels".equals(currentFilter)) {
                if (!isLoadingMore && currentPage * postsPerPage < allFilteredPosts.size()) {
                    isLoadingMore = true;
                    currentPage++;
                    updateFeedData(false);
                }
            }
        });

        setupFilters();

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            mainHandler.postDelayed(() -> {
                if (isAdded()) {
                    updateFeedData(true);
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }, 800);
        });

        binding.ivPlus.setOnClickListener(v -> {
            ComCreatePostBottomSheet bottomSheet = new ComCreatePostBottomSheet();
            bottomSheet.show(getParentFragmentManager(), ComCreatePostBottomSheet.TAG);
        });

        SideMenuHelper.bindCurrentUser(binding.navView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && binding.navView != null) {
            SideMenuHelper.bindCurrentUser(binding.navView);
        }
    }

    @Override
    protected Flow<Integer> getUnreadCountFlow(Context context) {
        return CommunityNotificationRepository.getInstance(context).getUnreadCount();
    }

    private void hideBottomNavigation() {
        if (!isNavVisible) return;
        isNavVisible = false;
        binding.comBottomNav.getRoot().animate()
                .translationY(binding.comBottomNav.getRoot().getHeight())
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();

        binding.clHeader.animate()
                .translationY(-binding.clHeader.getHeight())
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();
    }

    private void showBottomNavigation() {
        if (isNavVisible) return;
        isNavVisible = true;
        binding.comBottomNav.getRoot().animate()
                .translationY(0f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();

        binding.clHeader.animate()
                .translationY(0f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();
    }

    private void setupFilters() {
        TextView[] filters = {
                binding.tvFilterAll, binding.tvFilterRoutine, binding.tvFilterReview, binding.tvFilterReels,
                binding.tvFilterKienThuc, binding.tvFilterHoiDap, binding.tvFilterDaDau, binding.tvFilterMun,
                binding.tvFilterDaKho, binding.tvFilterThamMun, binding.tvFilterDiUng
        };

        for (TextView textView : filters) {
            textView.setOnClickListener(v -> {
                for (TextView tv : filters) {
                    tv.setBackground(requireContext().getDrawable(R.drawable.com_bg_filter_normal));
                    tv.setTextColor(requireContext().getColor(R.color.primary));
                }
                textView.setBackground(requireContext().getDrawable(R.drawable.bg_btn_buy));
                textView.setTextColor(Color.WHITE);

                String text = textView.getText().toString();
                currentFilter = "Dành cho bạn".equals(text) ? "Tất cả" : text;
                updateFeedData(true);
            });
        }
    }

    private void refreshFollowingState() {
        Context ctx = getContext();
        if (ctx == null) return;
        String userId = ProfileSessionHelper.getEffectiveUserId(ctx);
        if (userId == null || userId.isEmpty()) {
            userId = CommunitySocialHelper.resolveUserId(ctx);
        }
        LocalJsonReader reader = new LocalJsonReader(ctx);
        Map<String, List<String>> socialData = reader.getSocialDataForUser(userId);
        FeedDataCache.mySocialData = socialData;
        List<String> following = socialData.get("following");
        postAdapter.setFollowingUserIds(following != null ? new HashSet<>(following) : new HashSet<>());
    }

    private void onFollowStateChanged(String targetUserId, boolean isFollowing) {
        if (FeedDataCache.mySocialData == null) {
            FeedDataCache.mySocialData = new java.util.HashMap<>();
        }
        List<String> following = FeedDataCache.mySocialData.get("following");
        if (following == null) {
            following = new ArrayList<>();
            FeedDataCache.mySocialData.put("following", following);
        }
        if (isFollowing) {
            if (!following.contains(targetUserId)) {
                following.add(targetUserId);
            }
        } else {
            following.remove(targetUserId);
        }
    }

    private boolean matchesCurrentFilter(CommunityPostEntity post) {
        if ("Tất cả".equals(currentFilter)) return true;
        return currentFilter.equalsIgnoreCase(post.getType())
                || currentFilter.equalsIgnoreCase(post.getSkinType())
                || currentFilter.equalsIgnoreCase(post.getConcern());
    }

    private void updateFeedData(boolean resetData) {
        if (viewModel == null) return;
        List<CommunityPostEntity> postsList = viewModel.getPosts().getValue();
        if (postsList == null) postsList = new ArrayList<>();
        List<UserEntity> usersList = viewModel.getUsers().getValue();
        if (usersList == null) usersList = new ArrayList<>();
        List<com.veganbeauty.app.data.local.entities.ReelEntity> reelsList = viewModel.getReels().getValue();
        if (reelsList == null) reelsList = new ArrayList<>();

        if ("Reels".equals(currentFilter)) {
            binding.rvPosts.setLayoutManager(new GridLayoutManager(requireContext(), 3));
            int dp1 = (int) (1 * getResources().getDisplayMetrics().density);
            binding.rvPosts.setPadding(dp1, dp1, dp1, dp1);
            binding.rvPosts.setClipToPadding(false);
            ReelAdapter reelAdapter = new ReelAdapter(reelsList, true);
            binding.rvPosts.setAdapter(reelAdapter);
        } else {
            binding.rvPosts.setPadding(0, 0, 0, 0);
            binding.rvPosts.setClipToPadding(true);
            if (binding.rvPosts.getAdapter() != postAdapter) {
                binding.rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
                binding.rvPosts.setAdapter(postAdapter);
            }

            final List<CommunityPostEntity> finalPostsList = postsList;
            final List<UserEntity> finalUsersList = usersList;
            final List<com.veganbeauty.app.data.local.entities.ReelEntity> finalReelsList = reelsList;

            executor.execute(() -> {
                Context ctx = getContext();
                if (ctx == null) return;
                LocalJsonReader reader = new LocalJsonReader(ctx);
                if (FeedDataCache.productsList == null) FeedDataCache.productsList = reader.getProducts();
                if (FeedDataCache.newsList == null) FeedDataCache.newsList = reader.getCommunityNews();
                if (FeedDataCache.mySocialData == null) {
                    String userId = ProfileSessionHelper.getEffectiveUserId(ctx);
                    if (userId == null || userId.isEmpty()) {
                        userId = CommunitySocialHelper.resolveUserId(ctx);
                    }
                    FeedDataCache.mySocialData = reader.getSocialDataForUser(userId);
                }

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    if (resetData) {
                        allFilteredPosts.clear();
                        Set<String> seenIds = new HashSet<>();
                        for (CommunityPostEntity post : finalPostsList) {
                            if ("Tất cả".equals(currentFilter) ||
                                    currentFilter.equalsIgnoreCase(post.getType()) ||
                                    currentFilter.equalsIgnoreCase(post.getSkinType()) ||
                                    currentFilter.equalsIgnoreCase(post.getConcern())) {
                                if (post.getPostId() != null && !seenIds.contains(post.getPostId())) {
                                    allFilteredPosts.add(post);
                                    seenIds.add(post.getPostId());
                                }
                            }
                        }
                        
                        Collections.sort(allFilteredPosts, (o1, o2) -> {
                            return com.veganbeauty.app.utils.TimeFormatter.compareCreatedAtDesc(o1.getCreatedAt(), o2.getCreatedAt());
                        });

                        if (FeedDataCache.newsList != null && !FeedDataCache.newsList.isEmpty()) {
                            CommunityPostEntity news = null;
                            if ("Tất cả".equals(currentFilter)) {
                                news = FeedDataCache.newsList.get(new Random().nextInt(FeedDataCache.newsList.size()));
                            } else {
                                List<CommunityPostEntity> filteredNews = new ArrayList<>();
                                for (CommunityPostEntity n : FeedDataCache.newsList) {
                                    if (currentFilter.equalsIgnoreCase(n.getType()) || currentFilter.equalsIgnoreCase(n.getSkinType()) || currentFilter.equalsIgnoreCase(n.getConcern())) {
                                        filteredNews.add(n);
                                    }
                                }
                                if (!filteredNews.isEmpty()) news = filteredNews.get(new Random().nextInt(filteredNews.size()));
                            }
                            if (news != null) allFilteredPosts.add(0, news);
                        }

                        List<CommunityPostEntity> pinnedPosts = FeedDataCache.getPinnedPosts();
                        for (int i = pinnedPosts.size() - 1; i >= 0; i--) {
                            CommunityPostEntity pinned = pinnedPosts.get(i);
                            if (pinned == null || pinned.getPostId() == null) continue;
                            if (!matchesCurrentFilter(pinned)) continue;
                            String pinnedId = pinned.getPostId();
                            allFilteredPosts.removeIf(p -> p != null && pinnedId.equals(p.getPostId()));
                            allFilteredPosts.add(0, pinned);
                        }

                        currentPage = 1;
                    }

                    int endIndex = Math.min(currentPage * postsPerPage, allFilteredPosts.size());
                    List<CommunityPostEntity> pagedPostsList = new ArrayList<>(allFilteredPosts.subList(0, endIndex));

                    Map<String, List<String>> mySocialData = FeedDataCache.mySocialData;
                    List<String> suggestedIds = mySocialData != null && mySocialData.containsKey("suggested") ? mySocialData.get("suggested") : new ArrayList<>();
                    List<String> myFriendsList = mySocialData != null && mySocialData.containsKey("friends") ? mySocialData.get("friends") : new ArrayList<>();
                    Set<String> myFriends = new HashSet<>(myFriendsList);

                    List<UserEntity> suggestedUsers = new ArrayList<>();
                    for (UserEntity u : finalUsersList) {
                        if (suggestedIds.contains(u.getUser_id()) && !"test_001".equals(u.getUser_id())) {
                            suggestedUsers.add(u);
                        }
                    }

                    if (suggestedUsers.size() < 5) {
                        List<UserEntity> extra = new ArrayList<>();
                        for (UserEntity u : finalUsersList) {
                            if (!"test_001".equals(u.getUser_id()) && !myFriends.contains(u.getUser_id()) && !suggestedUsers.contains(u)) {
                                extra.add(u);
                            }
                        }
                        Collections.shuffle(extra);
                        int limit = Math.min(10 - suggestedUsers.size(), extra.size());
                        suggestedUsers.addAll(extra.subList(0, limit));
                    }

                    executor.execute(() -> {
                        Context ctx2 = getContext();
                        if (ctx2 == null) return;
                        List<String> sUserIds = new ArrayList<>();
                        for (UserEntity u : suggestedUsers) sUserIds.add(u.getUser_id());
                        Map<String, List<String>> mutualMap = new LocalJsonReader(ctx2).getMutualFriendsForUsers(new ArrayList<>(myFriends), new ArrayList<>(sUserIds));

                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            for (UserEntity user : suggestedUsers) {
                                List<String> mutualIds = mutualMap.getOrDefault(user.getUser_id(), new ArrayList<>());
                                user.setMutualCount(mutualIds.size());
                                if (!mutualIds.isEmpty()) {
                                    List<UserEntity> mutualUsers = new ArrayList<>();
                                    for (UserEntity u : finalUsersList) {
                                        if (mutualIds.contains(u.getUser_id())) mutualUsers.add(u);
                                    }
                                    if (!mutualUsers.isEmpty()) {
                                        UserEntity first = mutualUsers.get(0);
                                        user.setFirstMutualFriendName((first.getFull_name() != null && !first.getFull_name().trim().isEmpty()) ? first.getFull_name() : first.getUsername());
                                        List<String> avatars = new ArrayList<>();
                                        for (UserEntity mu : mutualUsers) {
                                            if (mu.getAvatar() != null) avatars.add(mu.getAvatar());
                                            if (avatars.size() == 3) break;
                                        }
                                        user.setMutualFriendAvatars(avatars);
                                    }
                                }
                            }
                            postAdapter.updateData(pagedPostsList, suggestedUsers, finalReelsList, FeedDataCache.productsList);
                            if (postAdapter != null) {
                                List<String> following = FeedDataCache.mySocialData != null
                                        ? FeedDataCache.mySocialData.get("following") : null;
                                if (following != null) {
                                    postAdapter.setFollowingUserIds(new HashSet<>(following));
                                }
                            }
                            isLoadingMore = false;
                        });
                    });
                });
            });
        }
    }

    @Override
    public void observeViewModel() {
        if (viewModel == null) return;
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            executor.execute(() -> {
                Context ctx = getContext();
                if (ctx == null) return;
                List<String> myFriendsIdsList = new LocalJsonReader(ctx).getFriendsForUser(
                        ProfileSessionHelper.getEffectiveUserId(ctx));
                if (myFriendsIdsList.isEmpty()) {
                    myFriendsIdsList = new LocalJsonReader(ctx).getFriendsForUser("test_001");
                }
                Set<String> myFriendsIds = new HashSet<>(myFriendsIdsList);

                String ownUserId = ProfileSessionHelper.getEffectiveUserId(ctx);
                if (ownUserId == null || ownUserId.isEmpty()) {
                    ownUserId = "test_001";
                }
                final String currentUserId = ownUserId;

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    List<UserEntity> safeUsers = users != null ? users : Collections.emptyList();
                    bindStories(safeUsers, myFriendsIds, currentUserId);
                    updateFeedData(true);
                });
            });
        });

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {});
        viewModel.getReels().observe(getViewLifecycleOwner(), reels -> {});
    }

    private void refreshStoriesFromSession() {
        Context ctx = getContext();
        if (ctx == null || binding == null) {
            return;
        }
        List<UserEntity> users = lastStoryUsers;
        if (users == null || users.isEmpty()) {
            users = viewModel != null && viewModel.getUsers().getValue() != null
                    ? viewModel.getUsers().getValue()
                    : Collections.emptyList();
        }
        List<String> myFriendsIdsList = new LocalJsonReader(ctx).getFriendsForUser(
                ProfileSessionHelper.getEffectiveUserId(ctx));
        if (myFriendsIdsList.isEmpty()) {
            myFriendsIdsList = new LocalJsonReader(ctx).getFriendsForUser("test_001");
        }
        String ownUserId = ProfileSessionHelper.getEffectiveUserId(ctx);
        if (ownUserId == null || ownUserId.isEmpty()) {
            ownUserId = "test_001";
        }
        bindStories(users, new HashSet<>(myFriendsIdsList), ownUserId);
    }

    private void bindStories(List<UserEntity> safeUsers, Set<String> myFriendsIds, String currentUserId) {
        List<UserEntity> allStories = new ArrayList<>(safeUsers);
        Collections.sort(allStories, (o1, o2) -> {
            boolean f1 = myFriendsIds.contains(o1.getUser_id());
            boolean f2 = myFriendsIds.contains(o2.getUser_id());
            return Boolean.compare(f2, f1);
        });

        UserEntity currentUser = null;
        for (UserEntity user : safeUsers) {
            if (currentUserId.equals(user.getUser_id())) {
                currentUser = user;
                break;
            }
        }

        Context ctx = getContext();
        String avatarUrl = resolveStoryAvatarUrl(ctx);

        UserEntity myStory = new UserEntity(
                currentUserId, "Tin của bạn", "Tin của bạn", "", "", "", avatarUrl, null
        );
        if (currentUser != null) {
            myStory.setBio(currentUser.getBio());
            myStory.setSkinType(currentUser.getSkinType());
            myStory.setConcerns(currentUser.getConcerns());
        }
        allStories.add(0, myStory);
        lastStoryUsers = new ArrayList<>(safeUsers);
        storyAdapter.updateData(allStories);
    }

    /** Session/local only — must not touch Room on the main thread. */
    private String resolveStoryAvatarUrl(@Nullable Context ctx) {
        if (ctx == null) {
            return "";
        }
        String sessionAvatar = ProfileSession.getAvatarStored(ctx);
        if (ProfileSessionHelper.isRemoteAvatarUrl(sessionAvatar)) {
            return sessionAvatar.trim();
        }
        String localAvatar = ProfileSessionHelper.getLocalAvatarFileUri(ctx);
        if (localAvatar != null) {
            return localAvatar;
        }
        if (ProfileSessionHelper.isUsableAvatarUrl(sessionAvatar)) {
            return sessionAvatar.trim();
        }
        return ProfileSession.getAvatar(ctx);
    }

    @Override
    public void onDestroyView() {
        ProfileUpdateNotifier.removeListener(profileUpdateListener);
        super.onDestroyView();
        binding = null;
    }
}
