package com.veganbeauty.app.features.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.QuizTestIntroBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;

public class QuizTestIntroFragment extends RootieFragment {

    private QuizTestIntroBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestIntroBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.layoutNotification.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit());

        binding.btnStartQuiz.setOnClickListener(v -> {
            if (!ProfileSession.isQuizRewardEligible(requireContext())) {
                int daysLeft = ProfileSession.getDaysUntilQuizReward(requireContext());
                Toast.makeText(
                        requireContext(),
                        "Bạn đã kiểm tra da gần đây. Vui lòng quay lại sau " + daysLeft + " ngày để nhận thưởng 100 xu.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new QuizTestLevelSelectionFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.rlHeader,
                binding.quizScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.quizScroll);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected void observeViewModel() {}
}
