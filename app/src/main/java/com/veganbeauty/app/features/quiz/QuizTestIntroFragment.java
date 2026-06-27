package com.veganbeauty.app.features.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.QuizTestIntroBinding;

public class QuizTestIntroFragment extends RootieFragment {

    private QuizTestIntroBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestIntroBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnNotification.setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        );

        binding.btnStartQuiz.setOnClickListener(v -> 
            getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new QuizTestLevelSelectionFragment())
                .addToBackStack(null)
                .commit()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected void observeViewModel() {}
}
