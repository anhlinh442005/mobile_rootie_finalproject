package com.veganbeauty.app.features.community.com_feed;

import android.animation.ValueAnimator;
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

import com.veganbeauty.app.R;

/**
 * Lightweight splash before community feed. Data loading is handled by {@link CommunityBootstrap}.
 */
public class ComLoadingFragment extends Fragment {

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

        if (progressBar != null && ivMascotLoading != null) {
            view.post(() -> {
                if (!isAdded()) {
                    return;
                }
                ValueAnimator animator = ValueAnimator.ofInt(0, 100);
                animator.setDuration(1200);
                animator.addUpdateListener(animation -> {
                    if (!isAdded() || progressBar == null || ivMascotLoading == null) {
                        return;
                    }
                    int progress = (int) animation.getAnimatedValue();
                    progressBar.setProgress(progress);
                    int width = progressBar.getWidth() - ivMascotLoading.getWidth();
                    if (width > 0) {
                        ivMascotLoading.setTranslationX(width * progress / 100f);
                    }
                });
                animator.start();
            });
        }

        CommunityBootstrap.ensureLoaded(requireContext());

        view.postDelayed(this::openCommunityFeed, 600);
    }

    private void openCommunityFeed() {
        if (!isAdded()) {
            return;
        }
        View root = getView();
        if (!CommunityBootstrap.isLocalSeedReady()) {
            if (root != null) {
                root.postDelayed(this::openCommunityFeed, 250);
            }
            return;
        }
        try {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityFeedFragment())
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
