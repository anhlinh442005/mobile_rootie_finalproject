package com.veganbeauty.app.features.community.com_feed;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

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
import com.veganbeauty.app.databinding.ComFragmentFeedBinding;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.message.CommunityMessageFragment;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.home.HomeFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ComFragmentFeedBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.Companion.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.rvStories.setAdapter(storyAdapter);
        binding.rvPosts.setAdapter(postAdapter);

        binding.ivHome.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            BottomNavHelper.INSTANCE.navigate(this, R.id.nav_home);
        });

        binding.ivMenu.setOnClickListener(v -> {
            DrawerLayout drawerLayout = view.findViewById(R.id.drawerLayout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

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
                DrawerLayout drawerLayout = view.findViewById(R.id.drawerLayout);
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
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

        binding.comBottomNav.navComFeed.setOnClickListener(v -> {
            binding.nsvFeed.smoothScrollTo(0, 0);
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getMain(), (coroutineScope, continuation1) -> {
                    try {
                        kotlinx.coroutines.DelayKt.delay(800, continuation1);
                        updateFeedData(true);
                    } catch (Exception ignored) {
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        });

        binding.comBottomNav.navComProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.comBottomNav.navComHub.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityBeautyHubFragment())
                    .commit();
        });

        binding.comBottomNav.navComExplore.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityExploreFragment())
                    .commit();
        });

        binding.comBottomNav.navComChat.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityMessageFragment())
                    .commit();
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
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getMain(), (coroutineScope, continuation1) -> {
                    try {
                        kotlinx.coroutines.DelayKt.delay(800, continuation1);
                        updateFeedData(true);
                        binding.swipeRefreshLayout.setRefreshing(false);
                    } catch (Exception ignored) {
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        });

        binding.ivPlus.setOnClickListener(v -> {
            ComCreatePostBottomSheet bottomSheet = new ComCreatePostBottomSheet();
            bottomSheet.show(getParentFragmentManager(), ComCreatePostBottomSheet.TAG);
        });

        updateSideMenuUserInfo(null);
    }

    private void updateSideMenuUserInfo(UserEntity user) {
        if (getView() == null) return;
        NavigationView navView = getView().findViewById(R.id.navView);
        if (navView == null) return;

        ImageView ivAvatar = navView.findViewById(R.id.ivSideMenuAvatar);
        TextView tvDisplayName = navView.findViewById(R.id.tvSideMenuDisplayName);
        TextView tvUsername = navView.findViewById(R.id.tvSideMenuUsername);

        if (user != null) {
            if (tvDisplayName != null) tvDisplayName.setText(user.getUsername());
            if (tvUsername != null) tvUsername.setText("@" + user.getUsername().toLowerCase().replace(" ", "_"));

            if (user.getAvatar() != null && !user.getAvatar().isEmpty() && ivAvatar != null) {
                ivAvatar.setVisibility(View.VISIBLE);
                ImageLoader imageLoader = Coil.imageLoader(requireContext());
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(user.getAvatar())
                        .crossfade(true)
                        .transformations(new CircleCropTransformation())
                        .placeholder(R.drawable.img_avatar)
                        .target(ivAvatar)
                        .build();
                imageLoader.enqueue(request);
            }
        } else {
            if (tvDisplayName != null) tvDisplayName.setText("Ánh Linh");
            if (tvUsername != null) tvUsername.setText("@eng_lyns");
            if (ivAvatar != null) {
                ImageLoader imageLoader = Coil.imageLoader(requireContext());
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data("https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg")
                        .crossfade(true)
                        .transformations(new CircleCropTransformation())
                        .placeholder(R.drawable.img_avatar)
                        .target(ivAvatar)
                        .build();
                imageLoader.enqueue(request);
            }
        }
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
                textView.setBackground(requireContext().getDrawable(R.drawable.com_bg_filter_selected));
                textView.setTextColor(Color.WHITE);

                String text = textView.getText().toString();
                currentFilter = "Dành cho bạn".equals(text) ? "Tất cả" : text;
                updateFeedData(true);
            });
        }
    }

    private void updateFeedData(boolean resetData) {
        List<CommunityPostEntity> postsList = viewModel.getPosts().getValue();
        if (postsList == null) postsList = new ArrayList<>();
        List<UserEntity> usersList = viewModel.getUsers().getValue();
        if (usersList == null) usersList = new ArrayList<>();
        List<CommunityPostEntity> reelsList = viewModel.getReels().getValue();
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
            final List<CommunityPostEntity> finalReelsList = reelsList;

            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getIO(), (coroutineScope, continuation1) -> {
                    try {
                        LocalJsonReader reader = new LocalJsonReader(requireContext());
                        if (FeedDataCache.productsList == null) {
                            FeedDataCache.productsList = reader.getProducts();
                        }
                        if (FeedDataCache.newsList == null) {
                            FeedDataCache.newsList = reader.getCommunityNews();
                        }
                        if (FeedDataCache.mySocialData == null) {
                            FeedDataCache.mySocialData = reader.getSocialDataForUser("test_001");
                        }

                        BuildersKt.withContext(Dispatchers.getMain(), (coroutineScopeMain, continuationMain) -> {
                            if (resetData) {
                                allFilteredPosts.clear();
                                if (!"Tất cả".equals(currentFilter)) {
                                    Set<String> seenIds = new HashSet<>();
                                    for (CommunityPostEntity post : finalPostsList) {
                                        if (currentFilter.equalsIgnoreCase(post.getType()) ||
                                                currentFilter.equalsIgnoreCase(post.getSkinType()) ||
                                                currentFilter.equalsIgnoreCase(post.getConcern())) {
                                            if (!seenIds.contains(post.getPostId())) {
                                                allFilteredPosts.add(post);
                                                seenIds.add(post.getPostId());
                                            }
                                        }
                                    }
                                } else {
                                    Set<String> seenIds = new HashSet<>();
                                    for (CommunityPostEntity post : finalPostsList) {
                                        if (!seenIds.contains(post.getPostId())) {
                                            allFilteredPosts.add(post);
                                            seenIds.add(post.getPostId());
                                        }
                                    }
                                }
                                Collections.sort(allFilteredPosts, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));

                                if (FeedDataCache.newsList != null && !FeedDataCache.newsList.isEmpty()) {
                                    if ("Tất cả".equals(currentFilter)) {
                                        CommunityPostEntity randomNews = FeedDataCache.newsList.get(new Random().nextInt(FeedDataCache.newsList.size()));
                                        allFilteredPosts.add(0, randomNews);
                                    } else {
                                        List<CommunityPostEntity> filteredNews = new ArrayList<>();
                                        for (CommunityPostEntity n : FeedDataCache.newsList) {
                                            if (currentFilter.equalsIgnoreCase(n.getType()) ||
                                                    currentFilter.equalsIgnoreCase(n.getSkinType()) ||
                                                    currentFilter.equalsIgnoreCase(n.getConcern())) {
                                                filteredNews.add(n);
                                            }
                                        }
                                        if (!filteredNews.isEmpty()) {
                                            CommunityPostEntity randomNews = filteredNews.get(new Random().nextInt(filteredNews.size()));
                                            allFilteredPosts.add(0, randomNews);
                                        }
                                    }
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

                            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope2, continuation2) -> {
                                return BuildersKt.withContext(Dispatchers.getIO(), (coroutineScopeIO, continuationIO) -> {
                                    List<String> sUserIds = new ArrayList<>();
                                    for (UserEntity u : suggestedUsers) sUserIds.add(u.getUser_id());
                                    Map<String, List<String>> mutualMap = new LocalJsonReader(requireContext()).getMutualFriendsForUsers(myFriends, sUserIds);

                                    BuildersKt.withContext(Dispatchers.getMain(), (coroutineScopeMain2, continuationMain2) -> {
                                        for (UserEntity user : suggestedUsers) {
                                            List<String> mutualIds = mutualMap.containsKey(user.getUser_id()) ? mutualMap.get(user.getUser_id()) : new ArrayList<>();
                                            user.setMutualCount(mutualIds.size());
                                            if (!mutualIds.isEmpty()) {
                                                List<UserEntity> mutualUsers = new ArrayList<>();
                                                for (UserEntity u : finalUsersList) {
                                                    if (mutualIds.contains(u.getUser_id())) {
                                                        mutualUsers.add(u);
                                                    }
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
                                        isLoadingMore = false;
                                        return kotlin.Unit.INSTANCE;
                                    }, continuationIO);
                                    return kotlin.Unit.INSTANCE;
                                }, continuation2);
                            });

                            return kotlin.Unit.INSTANCE;
                        }, continuation1);
                    } catch (Exception ignored) {
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        }
    }

    @Override
    public void observeViewModel() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, continuation) -> {
                return BuildersKt.withContext(Dispatchers.getIO(), (coroutineScope, continuation1) -> {
                    try {
                        List<String> myFriendsIdsList = new LocalJsonReader(requireContext()).getFriendsForUser("test_001");
                        Set<String> myFriendsIds = new HashSet<>(myFriendsIdsList);

                        BuildersKt.withContext(Dispatchers.getMain(), (coroutineScopeMain, continuationMain) -> {
                            List<UserEntity> allStories = new ArrayList<>(users);
                            Collections.sort(allStories, (o1, o2) -> {
                                boolean f1 = myFriendsIds.contains(o1.getUser_id());
                                boolean f2 = myFriendsIds.contains(o2.getUser_id());
                                return Boolean.compare(f2, f1);
                            });

                            if (!allStories.isEmpty()) {
                                UserEntity myStory = new UserEntity(
                                        "test_001", "Tin của bạn", allStories.get(0).getFull_name(), "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
                                        allStories.get(0).getBio(), allStories.get(0).getSkinType(), allStories.get(0).getConcerns()
                                );
                                allStories.add(0, myStory);
                            }
                            storyAdapter.updateData(allStories);
                            updateFeedData(true);
                            return kotlin.Unit.INSTANCE;
                        }, continuation1);
                    } catch (Exception ignored) {
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            });
        });

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {});
        viewModel.getReels().observe(getViewLifecycleOwner(), reels -> {});
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
