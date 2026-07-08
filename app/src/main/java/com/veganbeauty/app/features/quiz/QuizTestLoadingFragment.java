package com.veganbeauty.app.features.quiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.QuizTestLoadingBinding;

public class QuizTestLoadingFragment extends RootieFragment {

    private QuizTestLoadingBinding binding;
    private ValueAnimator progressAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestLoadingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        startAnalysisAnimation();
    }

    private void startAnalysisAnimation() {
        float density = getResources().getDisplayMetrics().density;
        int containerWidthPx = (int) (270 * density);
        int characterWidthPx = (int) (64 * density);
        float maxTranslation = containerWidthPx - characterWidthPx;

        progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(3000);
        progressAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            ViewGroup.LayoutParams lp = binding.vProgressFill.getLayoutParams();
            lp.width = (int) (containerWidthPx * fraction);
            binding.vProgressFill.setLayoutParams(lp);

            binding.ivLoadingCharacter.setTranslationX(fraction * maxTranslation);

            if (fraction < 0.35f) {
                binding.tvLoadingStatus.setText("Đang xác định độ nhạy cảm và thành phần cho làn da...");
            } else if (fraction < 0.70f) {
                binding.tvLoadingStatus.setText("Đang phân tích các thành phần phù hợp và nên tránh...");
            } else {
                binding.tvLoadingStatus.setText("Đang hoàn tất thiết lập routine chăm sóc da tối ưu...");
            }
        });

        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAdded()) {
                    QuizTestResultFragment resultFragment = new QuizTestResultFragment();
                    Bundle args = new Bundle();
                    args.putBoolean(QuizTestResultFragment.ARG_IS_FIRST_TEST, true);
                    resultFragment.setArguments(args);
                    
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, resultFragment)
                            .commit();
                }
            }
        });

        progressAnimator.start();
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        binding = null;
    }
}
