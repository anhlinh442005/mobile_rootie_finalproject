package com.veganbeauty.app.features.quiz;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.QuizTestLevelSelectionBinding;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;

public class QuizTestLevelSelectionFragment extends RootieFragment {

    private QuizTestLevelSelectionBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private boolean isAdvancedSelected = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = QuizTestLevelSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.cardQuizBasic.setOnClickListener(v -> selectBasic());
        binding.cardQuizAdvanced.setOnClickListener(v -> selectAdvanced());

        binding.btnContinue.setOnClickListener(v -> {
            String level = isAdvancedSelected ? "advanced" : "basic";
            QuizTestQuestionsFragment fragment = QuizTestQuestionsFragment.newInstance(level);
            
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        updateUI();
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

    private void selectBasic() {
        isAdvancedSelected = false;
        updateUI();
    }

    private void selectAdvanced() {
        isAdvancedSelected = true;
        updateUI();
    }

    private void updateUI() {
        if (isAdvancedSelected) {
            binding.cardQuizBasic.setBackgroundResource(R.drawable.quiz_bg_option);
            binding.ivCheckBasic.setVisibility(View.GONE);
            binding.flIconBasicBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F0F5EC")));
            binding.ivIconBasic.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));

            binding.cardQuizAdvanced.setBackgroundResource(R.drawable.quiz_bg_card_border);
            binding.ivCheckAdvanced.setVisibility(View.VISIBLE);
            binding.flIconAdvancedBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
            binding.ivIconAdvanced.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
        } else {
            binding.cardQuizBasic.setBackgroundResource(R.drawable.quiz_bg_card_border);
            binding.ivCheckBasic.setVisibility(View.VISIBLE);
            binding.flIconBasicBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
            binding.ivIconBasic.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));

            binding.cardQuizAdvanced.setBackgroundResource(R.drawable.quiz_bg_option);
            binding.ivCheckAdvanced.setVisibility(View.GONE);
            binding.flIconAdvancedBg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F0F5EC")));
            binding.ivIconAdvanced.setImageTintList(ColorStateList.valueOf(Color.parseColor("#879578")));
        }
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
