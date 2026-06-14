package com.veganbeauty.app.features.quiz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.QuizTestLoadingBinding

class QuizTestLoadingFragment : RootieFragment() {

    private var _binding: QuizTestLoadingBinding? = null
    private val binding get() = _binding!!
    private var progressAnimator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuizTestLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Start simulated server-side skin analysis animation
        startAnalysisAnimation()
    }

    private fun startAnalysisAnimation() {
        val density = resources.displayMetrics.density
        val containerWidthPx = (270 * density).toInt()
        val characterWidthPx = (64 * density).toInt()
        val maxTranslation = (containerWidthPx - characterWidthPx).toFloat()

        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000 // 3 seconds analysis simulation
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float

                // Update progress width
                val lp = binding.vProgressFill.layoutParams
                lp.width = (containerWidthPx * fraction).toInt()
                binding.vProgressFill.layoutParams = lp

                // Move scooter character along the track
                binding.ivLoadingCharacter.translationX = fraction * maxTranslation

                // Dynamically update status text based on progress stage
                when {
                    fraction < 0.35f -> {
                        binding.tvLoadingStatus.text = "Đang xác định độ nhạy cảm và thành phần cho làn da..."
                    }
                    fraction < 0.70f -> {
                        binding.tvLoadingStatus.text = "Đang phân tích các thành phần phù hợp và nên tránh..."
                    }
                    else -> {
                        binding.tvLoadingStatus.text = "Đang hoàn tất thiết lập routine chăm sóc da tối ưu..."
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isAdded) {
                        // Navigate to the final quiz results screen
                        val resultFragment = QuizTestResultFragment().apply {
                            arguments = android.os.Bundle().apply {
                                putBoolean("IS_FIRST_TEST", true)
                            }
                        }
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.main_container, resultFragment)
                            .commit()
                    }
                }
            })
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressAnimator?.cancel()
        _binding = null
    }
}
