package com.veganbeauty.app.features.community.com_feed;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.features.community.CommunitySocialHelper;
import com.veganbeauty.app.features.community.UserSocialSeeder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComLoadingFragment extends Fragment {

    private boolean isDataLoaded = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_fragment_loading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvLoadingMessage = view.findViewById(R.id.tvLoadingMessage);
        if (tvLoadingMessage != null) {
            tvLoadingMessage.setText("Đang kết nối đến cộng đồng Rootie...");
        }
        ImageView ivMascotLoading = view.findViewById(R.id.ivMascotLoading);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        view.post(() -> {
            ValueAnimator animator = ValueAnimator.ofInt(0, 100);
            animator.setDuration(1500);
            animator.addUpdateListener(animation -> {
                int progress = (int) animation.getAnimatedValue();
                progressBar.setProgress(progress);
                
                int width = progressBar.getWidth() - ivMascotLoading.getWidth();
                if (width > 0) {
                    ivMascotLoading.setTranslationX(width * progress / 100f);
                }
            });
            animator.start();
        });

        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        CommunityRepository repository = new CommunityRepository(
                db.communityDao(),
                new LocalJsonReader(requireContext()),
                new FirestoreService()
        );
        CommunityViewModelFactory factory = new CommunityViewModelFactory(repository);
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) return;

        CommunityViewModel viewModel = new ViewModelProvider(activity, factory).get(CommunityViewModel.class);

        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null && !posts.isEmpty()) {
                isDataLoaded = true;
            }
        });

        repository.seedFromAssetsSync();
        viewModel.refreshData();

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            long maxWaitMs = 8000L;
            
            while (!isDataLoaded && (System.currentTimeMillis() - startTime) < maxWaitMs) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            Context ctx = getContext();
            if (ctx != null) {
                try {
                    UserSocialSeeder.seedIfNeeded(ctx);
                    LocalJsonReader reader = new LocalJsonReader(ctx);
                    reader.syncSocialFromFirestore(new FirestoreService());
                    String userId = CommunitySocialHelper.resolveUserId(ctx);
                    FeedDataCache.productsList = reader.getProducts();
                    FeedDataCache.newsList = reader.getCommunityNews();
                    FeedDataCache.mySocialData = reader.getSocialDataForUser(userId);
                    FeedDataCache.clearPinnedPosts();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < 2500) {
                try {
                    Thread.sleep(2500 - elapsed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            androidx.fragment.app.FragmentActivity currentActivity = getActivity();
            if (currentActivity != null) {
                currentActivity.runOnUiThread(() -> {
                    if (isAdded() && !isDetached()) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.main_container, new CommunityFeedFragment())
                                .commitAllowingStateLoss();
                    }
                });
            }
        });
    }
}
