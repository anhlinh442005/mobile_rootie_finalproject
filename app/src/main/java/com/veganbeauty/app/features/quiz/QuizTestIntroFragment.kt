package com.veganbeauty.app.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.QuizTestIntroBinding

class QuizTestIntroFragment : RootieFragment() {

    private var _binding: QuizTestIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuizTestIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back Button click
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Start Quiz click
        binding.btnStartQuiz.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestLevelSelectionFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
