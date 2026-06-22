package com.veganbeauty.app.features.quiz

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.QuizTestLevelSelectionBinding

class QuizTestLevelSelectionFragment : RootieFragment() {

    private var _binding: QuizTestLevelSelectionBinding? = null
    private val binding get() = _binding!!

    private var isAdvancedSelected = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuizTestLevelSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back Button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Notification Button click
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Card Selection Clicks
        binding.cardQuizBasic.setOnClickListener {
            selectBasic()
        }

        binding.cardQuizAdvanced.setOnClickListener {
            selectAdvanced()
        }

        // Setup Continue Button
        binding.btnContinue.setOnClickListener {
            val level = if (isAdvancedSelected) "advanced" else "basic"
            val fragment = QuizTestQuestionsFragment.newInstance(level)
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Initialize UI with default Advanced selected
        updateUI()
    }

    private fun selectBasic() {
        isAdvancedSelected = false
        updateUI()
    }

    private fun selectAdvanced() {
        isAdvancedSelected = true
        updateUI()
    }

    private fun updateUI() {
        if (isAdvancedSelected) {
            // Card 1 Basic (Unselected)
            binding.cardQuizBasic.setBackgroundResource(R.drawable.quiz_bg_card_normal)
            binding.ivCheckBasic.visibility = View.GONE
            binding.flIconBasicBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F5EC"))
            binding.ivIconBasic.imageTintList = ColorStateList.valueOf(Color.parseColor("#3E4D44"))

            // Card 2 Advanced (Selected)
            binding.cardQuizAdvanced.setBackgroundResource(R.drawable.quiz_bg_card_border)
            binding.ivCheckAdvanced.visibility = View.VISIBLE
            binding.flIconAdvancedBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3E4D44"))
            binding.ivIconAdvanced.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        } else {
            // Card 1 Basic (Selected)
            binding.cardQuizBasic.setBackgroundResource(R.drawable.quiz_bg_card_border)
            binding.ivCheckBasic.visibility = View.VISIBLE
            binding.flIconBasicBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3E4D44"))
            binding.ivIconBasic.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))

            // Card 2 Advanced (Unselected)
            binding.cardQuizAdvanced.setBackgroundResource(R.drawable.quiz_bg_card_normal)
            binding.ivCheckAdvanced.visibility = View.GONE
            binding.flIconAdvancedBg.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0F5EC"))
            binding.ivIconAdvanced.imageTintList = ColorStateList.valueOf(Color.parseColor("#879578"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
