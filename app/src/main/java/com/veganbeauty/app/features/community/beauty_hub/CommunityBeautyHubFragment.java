package com.veganbeauty.app.features.community.beauty_hub;

import android.content.Context;
import android.graphics.Typeface;
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
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentBeautyHubBinding;
import com.veganbeauty.app.features.community.blog.CommunityBlogFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModel;
import com.veganbeauty.app.features.community.com_feed.CommunityViewModelFactory;
import com.veganbeauty.app.features.community.message.CommunityMessageFragment;
import com.veganbeauty.app.features.community.notification.CommunityNotificationFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.home.HomeFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunityBeautyHubFragment extends RootieFragment {

    private ComFragmentBeautyHubBinding binding;
    private CommunityViewModel viewModel;

    private IngredientAdapter ingredientAdapter;
    private LatestKnowledgeAdapter blogAdapter;
    private NotebookVideoAdapter notebookAdapter;

    private boolean isNavVisible = true;

    private final int blogsPerPage = 10;
    private int blogCurrentOffset = 0;
    private boolean isLoadingMoreBlogs = false;
    private boolean hasMoreBlogs = true;
    private final List<CommunityBlogEntity> loadedBlogs = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = ComFragmentBeautyHubBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(),
                new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(CommunityViewModel.class);
    }

    @Override
    public void setupUI(View view) {
        ingredientAdapter = new IngredientAdapter(new ArrayList<>());
        blogAdapter = new LatestKnowledgeAdapter(new ArrayList<>());
        notebookAdapter = new NotebookVideoAdapter(new ArrayList<>());

        binding.rvIngredients.setAdapter(ingredientAdapter);
        binding.rvBlogs.setAdapter(blogAdapter);
        binding.rvNotebooks.setAdapter(notebookAdapter);

        binding.btnHome.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new HomeFragment())
                    .commit();
        });

        binding.ivNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left,
                            R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.llShortcutNews.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in,
                            android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityNewsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.llShortcutBlog.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in,
                            android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityBlogFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.llShortcutIngredient.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in,
                            android.R.anim.fade_out)
                    .replace(R.id.main_container, new IngredientFragment())
                    .addToBackStack(null)
                    .commit();
        });

        View llShortcutHandbook = binding.getRoot().findViewById(R.id.llShortcutHandbook);
        if (llShortcutHandbook != null) {
            llShortcutHandbook.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in,
                                android.R.anim.fade_out)
                        .replace(R.id.main_container, new HandbookFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        binding.btnExploreFeed.setOnClickListener(v -> {
            if (ProfileSession.isLoggedIn(requireContext())) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityFeedFragment())
                        .commit();
            } else {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
            }
        });

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadedBlogs.clear();
            blogCurrentOffset = 0;
            hasMoreBlogs = true;
            isLoadingMoreBlogs = false;
            viewModel.refreshData();
            loadNextBlogPage();
            mainHandler.postDelayed(() -> binding.swipeRefreshLayout.setRefreshing(false), 800);
        });

        binding.nsvHub.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int dy = scrollY - oldScrollY;
                    if (dy > 15) {
                        hideBottomNavigation();
                    } else if (dy < -15) {
                        showBottomNavigation();
                    }
                    if (!v.canScrollVertically(1) && hasMoreBlogs && !isLoadingMoreBlogs) {
                        loadNextBlogPage();
                    }
                });

        setupBottomNavigation();
        loadNextBlogPage();
    }

    private void loadNextBlogPage() {
        if (isLoadingMoreBlogs || !hasMoreBlogs)
            return;
        if (getContext() == null)
            return;
        isLoadingMoreBlogs = true;
        final Context appContext = getContext();

        executor.execute(() -> {
            List<CommunityBlogEntity> newBlogs;
            try {
                newBlogs = new LocalJsonReader(appContext).getCommunityBlogs(blogsPerPage, blogCurrentOffset);
            } catch (Exception e) {
                e.printStackTrace();
                newBlogs = new ArrayList<>();
            }

            List<CommunityBlogEntity> finalNewBlogs = newBlogs;
            mainHandler.post(() -> {
                if (!isAdded())
                    return;
                if (finalNewBlogs.isEmpty()) {
                    hasMoreBlogs = false;
                } else {
                    blogCurrentOffset += finalNewBlogs.size();
                    loadedBlogs.addAll(finalNewBlogs);

                    if (!loadedBlogs.isEmpty()) {
                        CommunityBlogEntity featuredBlog = loadedBlogs.get(0);
                        List<CommunityBlogEntity> remainingBlogs = loadedBlogs.subList(1, loadedBlogs.size());

                        binding.layoutFeaturedBlog.setVisibility(View.VISIBLE);
                        binding.tvFeaturedBlogTitle.setText(featuredBlog.getTitle());
                        binding.tvFeaturedBlogDate
                                .setText(featuredBlog.getPublishedAt() + " • " + featuredBlog.getShortDescription());

                        com.bumptech.glide.Glide.with(binding.ivFeaturedBlog.getContext())
                                .load(featuredBlog.getImageUrl()).into(binding.ivFeaturedBlog);

                        blogAdapter.updateData(new ArrayList<>(remainingBlogs));
                    }

                    if (finalNewBlogs.size() < blogsPerPage) {
                        hasMoreBlogs = false;
                    }
                }
                isLoadingMoreBlogs = false;
            });
        });
    }

    private void setupBottomNavigation() {
        binding.comBottomNav.navComFeed.setOnClickListener(v -> {
            if (ProfileSession.isLoggedIn(requireContext())) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityFeedFragment())
                        .commit();
            } else {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
            }
        });

        binding.comBottomNav.navComProfile.setOnClickListener(v -> {
            if (ProfileSession.isLoggedIn(requireContext())) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityProfileFragment())
                        .commit();
            } else {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
            }
        });

        binding.comBottomNav.navComExplore.setOnClickListener(v -> {
            if (ProfileSession.isLoggedIn(requireContext())) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityExploreFragment())
                        .commit();
            } else {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
            }
        });

        binding.comBottomNav.navComChat.setOnClickListener(v -> {
            if (ProfileSession.isLoggedIn(requireContext())) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new CommunityMessageFragment())
                        .commitAllowingStateLoss();
            } else {
                BottomNavHelper.showLoginRequiredDialog(requireContext());
            }
        });

        binding.comBottomNav.navComHub.setOnClickListener(v -> binding.nsvHub.smoothScrollTo(0, 0));

        // Keep Beauty Hub active visually
        if (binding.comBottomNav.navComHub.getChildCount() > 1) {
            ImageView hubIcon = (ImageView) binding.comBottomNav.navComHub.getChildAt(0);
            hubIcon.setColorFilter(getResources().getColor(R.color.primary, null));
            TextView hubText = (TextView) binding.comBottomNav.navComHub.getChildAt(1);
            hubText.setTextColor(getResources().getColor(R.color.primary, null));
            hubText.setTypeface(null, Typeface.BOLD);
        }

        if (binding.comBottomNav.navComFeed.getChildCount() > 1) {
            ImageView feedIcon = (ImageView) binding.comBottomNav.navComFeed.getChildAt(0);
            feedIcon.setColorFilter(getResources().getColor(R.color.tertiary, null));
            TextView feedText = (TextView) binding.comBottomNav.navComFeed.getChildAt(1);
            feedText.setTextColor(getResources().getColor(R.color.tertiary, null));
            feedText.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void hideBottomNavigation() {
        if (!isNavVisible)
            return;
        isNavVisible = false;
        binding.comBottomNav.getRoot().animate()
                .translationY(binding.comBottomNav.getRoot().getHeight())
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();
    }

    private void showBottomNavigation() {
        if (isNavVisible)
            return;
        isNavVisible = true;
        binding.comBottomNav.getRoot().animate()
                .translationY(0f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(250)
                .start();
    }

    @Override
    public void observeViewModel() {
        viewModel.getIngredients().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                ingredientAdapter.updateData(items.subList(0, Math.min(10, items.size())));
            }
        });

        viewModel.getExploreVideos().observe(getViewLifecycleOwner(), videos -> {
            if (videos != null) {
                List<com.veganbeauty.app.data.local.entities.YtVideoEntity> notebooks = new ArrayList<>();
                for (com.veganbeauty.app.data.local.entities.YtVideoEntity v : videos) {
                    if (v.getType() != null && v.getType().toLowerCase().contains("notebook")) {
                        notebooks.add(v);
                    }
                }
                notebookAdapter.updateData(notebooks);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
