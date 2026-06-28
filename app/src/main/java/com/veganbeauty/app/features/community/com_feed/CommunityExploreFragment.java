package com.veganbeauty.app.features.community.com_feed;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.databinding.ComFragmentExploreBinding;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommunityExploreFragment extends RootieFragment {

    private ComFragmentExploreBinding _binding;

    private CommunityViewModel viewModel;
    private ExploreVideoAdapter exploreAdapter = new ExploreVideoAdapter(new ArrayList<>(), null);

    private boolean isNavVisible = true;

    private void hideBottomNavigation() {
        if (!isNavVisible) return;
        isNavVisible = false;
        float height = _binding.comBottomNav.getRoot().getHeight();
        _binding.comBottomNav.getRoot().animate()
            .translationY(height)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start();
        
        updateVideoContentTranslation(height);
    }

    private void showBottomNavigation() {
        if (isNavVisible) return;
        isNavVisible = true;
        _binding.comBottomNav.getRoot().animate()
            .translationY(0f)
            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
            .setDuration(250)
            .start();
            
        updateVideoContentTranslation(0f);
    }

    private void updateVideoContentTranslation(float translationY) {
        // exploreAdapter.setContentTranslationY(translationY);
        RecyclerView rv = (RecyclerView) _binding.viewPagerExplore.getChildAt(0);
        if (rv != null && rv.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
            int first = layoutManager.findFirstVisibleItemPosition();
            int last = layoutManager.findLastVisibleItemPosition();
            if (first != -1 && last != -1) {
                for (int i = first; i <= last; i++) {
                    RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(i);
                    // if (holder instanceof ExploreVideoAdapter.VideoViewHolder) {
                    //    ((ExploreVideoAdapter.VideoViewHolder) holder).animateContent(translationY);
                    // }
                }
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ComFragmentExploreBinding.inflate(inflater, container, false);
        setupViewModel();
        return _binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(db.communityDao(), new LocalJsonReader(requireContext()), new FirestoreService());
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CommunityViewModel.class);
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.viewPagerExplore.setAdapter(exploreAdapter);
        _binding.viewPagerExplore.setOffscreenPageLimit(1);

        _binding.viewPagerExplore.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            // private ExploreVideoAdapter.VideoViewHolder currentPlayingHolder = null;
            private int lastPosition = 0;

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                
                if (position > lastPosition) {
                    hideBottomNavigation();
                } else if (position < lastPosition) {
                    showBottomNavigation();
                }
                lastPosition = position;
                
                // if (currentPlayingHolder != null) {
                //    currentPlayingHolder.stopVideo();
                // }
                
                RecyclerView rv = (RecyclerView) _binding.viewPagerExplore.getChildAt(0);
                if (rv != null) {
                    RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(position);
                    if (holder instanceof ExploreVideoAdapter.VideoViewHolder) {
                        // ((ExploreVideoAdapter.VideoViewHolder) holder).playVideo();
                        // currentPlayingHolder = (ExploreVideoAdapter.VideoViewHolder) holder;
                    } else {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            RecyclerView.ViewHolder delayedHolder = rv.findViewHolderForAdapterPosition(position);
                            if (delayedHolder instanceof ExploreVideoAdapter.VideoViewHolder) {
                                // ((ExploreVideoAdapter.VideoViewHolder) delayedHolder).playVideo();
                                // currentPlayingHolder = (ExploreVideoAdapter.VideoViewHolder) delayedHolder;
                            }
                        }, 200);
                    }
                }
            }
        });

        _binding.ivBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        _binding.ivSearch.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new ExploreSearchFragment())
                .addToBackStack(null)
                .commit();
        });

        _binding.comBottomNav.navComFeed.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new CommunityFeedFragment())
                .commit();
        });

        _binding.comBottomNav.navComProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new com.veganbeauty.app.features.community.profile.CommunityProfileFragment())
                .commit();
        });

        _binding.comBottomNav.navComHub.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new com.veganbeauty.app.features.community.beauty_hub.HandbookFragment())
                .commit();
        });

        _binding.comBottomNav.navComChat.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new com.veganbeauty.app.features.community.message.CommunityMessageFragment())
                .commitAllowingStateLoss();
        });

        ImageView exploreIcon = (ImageView) _binding.comBottomNav.navComExplore.getChildAt(0);
        if (exploreIcon != null) {
            exploreIcon.setColorFilter(getResources().getColor(R.color.primary, null));
        }
        TextView exploreText = (TextView) _binding.comBottomNav.navComExplore.getChildAt(1);
        if (exploreText != null) {
            exploreText.setTextColor(getResources().getColor(R.color.primary, null));
            exploreText.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        ImageView feedIcon = (ImageView) _binding.comBottomNav.navComFeed.getChildAt(0);
        if (feedIcon != null) {
            feedIcon.setColorFilter(getResources().getColor(R.color.tertiary, null));
        }
        TextView feedText = (TextView) _binding.comBottomNav.navComFeed.getChildAt(1);
        if (feedText != null) {
            feedText.setTextColor(getResources().getColor(R.color.tertiary, null));
            feedText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    @Override
    protected void observeViewModel() {
        viewModel.getExploreVideos().observe(getViewLifecycleOwner(), videos -> {
            List<YtVideoEntity> safeVideos = videos != null ? videos : Collections.emptyList();
            List<YtVideoEntity> finalVideos = new ArrayList<>(safeVideos);
            Collections.shuffle(finalVideos);
            
            String targetId = getArguments() != null ? getArguments().getString("target_video_id") : null;
            if (targetId != null) {
                YtVideoEntity targetVideo = null;
                for (YtVideoEntity v : finalVideos) {
                    if (v.getId().equals(targetId)) {
                        targetVideo = v;
                        break;
                    }
                }
                if (targetVideo != null) {
                    finalVideos.remove(targetVideo);
                    finalVideos.add(0, targetVideo);
                }
            }

            exploreAdapter.updateData(finalVideos);
            
            if (!finalVideos.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    RecyclerView rv = (RecyclerView) _binding.viewPagerExplore.getChildAt(0);
                    if (rv != null) {
                        RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(0);
                        if (holder instanceof ExploreVideoAdapter.VideoViewHolder) {
                            // ((ExploreVideoAdapter.VideoViewHolder) holder).playVideo();
                        }
                    }
                }, 500);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
