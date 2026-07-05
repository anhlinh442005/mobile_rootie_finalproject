package com.veganbeauty.app.features.community.com_feed;



import android.os.Bundle;

import android.os.Handler;

import android.os.Looper;

import android.view.LayoutInflater;

import android.view.View;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;

import androidx.lifecycle.ViewModelProvider;

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

import com.veganbeauty.app.utils.ComBottomNavHelper;

import com.veganbeauty.app.utils.ExploreVideoCache;

import java.util.ArrayList;

import java.util.Collections;

import java.util.List;



public class CommunityExploreFragment extends RootieFragment {



    private ComFragmentExploreBinding _binding;



    private CommunityViewModel viewModel;

    private ExploreVideoAdapter exploreAdapter = new ExploreVideoAdapter(new ArrayList<>(), null);



    private boolean isNavVisible = true;

    private ExploreVideoAdapter.VideoViewHolder currentPlayingHolder;

    private List<YtVideoEntity> currentVideos = Collections.emptyList();



    private void hideBottomNavigation() {

        if (!isNavVisible) return;

        isNavVisible = false;

        float height = _binding.comBottomNav.getRoot().getHeight();

        _binding.comBottomNav.getRoot().animate()

            .translationY(height)

            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())

            .setDuration(250)

            .start();



        exploreAdapter.setNavBarVisible(false, true);

    }



    private void showBottomNavigation() {

        if (isNavVisible) return;

        isNavVisible = true;

        _binding.comBottomNav.getRoot().animate()

            .translationY(0f)

            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())

            .setDuration(250)

            .start();



        exploreAdapter.setNavBarVisible(true, true);

    }



    private void playVideoAtPosition(int position) {

        exploreAdapter.setCurrentPosition(position);

        if (currentPlayingHolder != null) {

            currentPlayingHolder.pauseVideo();

            currentPlayingHolder = null;

        }



        RecyclerView rv = (RecyclerView) _binding.viewPagerExplore.getChildAt(0);

        if (rv == null) return;



        RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(position);

        if (holder instanceof ExploreVideoAdapter.VideoViewHolder) {

            ExploreVideoAdapter.VideoViewHolder videoHolder = (ExploreVideoAdapter.VideoViewHolder) holder;

            videoHolder.playVideo();

            currentPlayingHolder = videoHolder;

        } else {

            new Handler(Looper.getMainLooper()).postDelayed(() -> playVideoAtPosition(position), 150);

        }

    }



    private void prefetchAround(int position) {

        if (currentVideos.isEmpty()) return;

        if (!ExploreVideoCache.isNetworkAvailable(requireContext())) return;



        for (int offset = 0; offset <= 2; offset++) {

            int index = position + offset;

            if (index >= 0 && index < currentVideos.size()) {

                ExploreVideoCache.prefetch(requireContext(), currentVideos.get(index).getUrl());

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

        int navMargin = getResources().getDimensionPixelSize(R.dimen.com_content_padding_bottom);

        int compactMargin = getResources().getDimensionPixelSize(R.dimen.com_explore_content_margin_bottom_compact);

        exploreAdapter.setBottomMargins(navMargin, compactMargin);



        _binding.viewPagerExplore.setAdapter(exploreAdapter);

        _binding.viewPagerExplore.setOffscreenPageLimit(1);



        _binding.viewPagerExplore.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

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



                playVideoAtPosition(position);

                prefetchAround(position);

            }

        });



        _binding.ivBack.setOnClickListener(v -> ComBottomNavHelper.navigateToAppHome(this));



        _binding.ivSearch.setOnClickListener(v -> {

            getParentFragmentManager().beginTransaction()

                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

                .replace(R.id.main_container, new ExploreSearchFragment())

                .addToBackStack(null)

                .commit();

        });



        ComBottomNavHelper.setup(this, _binding.comBottomNav.getRoot(), ComBottomNavHelper.TAB_EXPLORE);

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



            currentVideos = finalVideos;

            exploreAdapter.updateData(finalVideos);



            if (!finalVideos.isEmpty()) {

                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    playVideoAtPosition(0);

                    prefetchAround(0);

                }, 300);

            }

        });

    }



    @Override

    public void onDestroyView() {

        currentPlayingHolder = null;

        super.onDestroyView();

        _binding = null;

    }

}

