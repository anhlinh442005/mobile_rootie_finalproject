package com.veganbeauty.app.features.quiz

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.QuizTestQuestionsBinding
import org.json.JSONArray
import org.json.JSONObject

class QuizTestQuestionsFragment : RootieFragment() {

    private var _binding: QuizTestQuestionsBinding? = null
    private val binding get() = _binding!!

    private var questions: JSONArray = JSONArray()
    private var currentQuestionIndex = 0
    private var levelType = "advanced"
    private var totalQuestionsLimit = 20

    // Store selected option index for each question
    private val selectedAnswers = HashMap<Int, Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuizTestQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        loadQuestions()

        // Back button on header (goes to prev question if possible, otherwise exits quiz)
        binding.btnBack.setOnClickListener {
            if (currentQuestionIndex > 0) {
                goToPrevQuestion()
            } else {
                exitQuiz()
            }
        }

        // Back button on bottom navigation
        binding.btnPrevQuestion.setOnClickListener {
            goToPrevQuestion()
        }

        displayQuestion()
    }

    private fun loadQuestions() {
        try {
            levelType = arguments?.getString(ARG_LEVEL_TYPE) ?: "advanced"
            totalQuestionsLimit = if (levelType == "basic") 10 else 20

            val jsonString = requireContext().assets.open("quiz_cauhoi.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val allQuestions = jsonObject.getJSONArray("questions")

            // Map all questions by their ID for quick lookup
            val questionsMap = HashMap<String, JSONObject>()
            for (i in 0 until allQuestions.length()) {
                val q = allQuestions.getJSONObject(i)
                questionsMap[q.getString("id")] = q
            }

            // Curated question IDs to cover general skin diagnosis and ingredient mapping
            val targetIds = if (levelType == "basic") {
                // 10 key questions (5 general skin typing + 5 ingredient sensitivity checks)
                listOf("q1", "q2", "q3", "q4", "q5", "q11", "q12", "q13", "q14", "q17")
            } else {
                // 20 comprehensive questions (10 general skin typing + 10 ingredient checks)
                listOf(
                    "q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10",
                    "q11", "q12", "q13", "q14", "q15", "q16", "q17", "q18", "q19", "q20"
                )
            }

            val limitedList = JSONArray()
            for (id in targetIds) {
                questionsMap[id]?.let {
                    limitedList.put(it)
                }
            }
            questions = limitedList

            binding.pbQuiz.max = questions.length()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi tải câu hỏi quiz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayQuestion() {
        if (questions.length() == 0) return

        binding.pbQuiz.progress = currentQuestionIndex + 1
        binding.tvProgressText.text = "Câu ${currentQuestionIndex + 1} / ${questions.length()}"

        val questionObj = questions.getJSONObject(currentQuestionIndex)
        val questionText = questionObj.getString("question")
        binding.tvQuestionText.text = questionText

        val optionsArray = questionObj.getJSONArray("options")
        binding.llOptionsContainer.removeAllViews()

        val savedAnswer = selectedAnswers[currentQuestionIndex]

        for (i in 0 until optionsArray.length()) {
            val optionObj = optionsArray.getJSONObject(i)
            val optionText = optionObj.getString("text")

            val optionView = LayoutInflater.from(context).inflate(R.layout.quiz_item_option, binding.llOptionsContainer, false)
            val tvOptionText = optionView.findViewById<TextView>(R.id.tv_option_text)
            val flOptionIconBg = optionView.findViewById<FrameLayout>(R.id.fl_option_icon_bg)
            val ivOptionIcon = optionView.findViewById<ImageView>(R.id.iv_option_icon)
            
            tvOptionText.text = optionText

            // Map custom option icons based on keyword matching and index fallbacks
            val optionTextLower = optionText.lowercase()
            val iconResId = when {
                optionTextLower.contains("khô") || optionTextLower.contains("căng") || optionTextLower.contains("yếu") || optionTextLower.contains("mỏng") || optionTextLower.contains("thiếu") -> {
                    R.drawable.quiz_ic_option_dry
                }
                optionTextLower.contains("dầu") || optionTextLower.contains("nhờn") || optionTextLower.contains("bóng") || optionTextLower.contains("mụn") || optionTextLower.contains("nhiều") || optionTextLower.contains("to") -> {
                    R.drawable.quiz_ic_option_oily
                }
                optionTextLower.contains("hỗn hợp") || optionTextLower.contains("không đều") || optionTextLower.contains("ửng đỏ") || optionTextLower.contains("rát") || optionTextLower.contains("kích ứng") || optionTextLower.contains("thỉnh thoảng") || optionTextLower.contains("nhạy cảm") || optionTextLower.contains("dễ") -> {
                    R.drawable.quiz_ic_option_combination
                }
                optionTextLower.contains("bình thường") || optionTextLower.contains("không") || optionTextLower.contains("khỏe") || optionTextLower.contains("ít") -> {
                    R.drawable.quiz_ic_option_normal
                }
                else -> {
                    when (i) {
                        0 -> R.drawable.quiz_ic_option_dry
                        1 -> R.drawable.quiz_ic_option_normal
                        2 -> R.drawable.quiz_ic_option_oily
                        else -> R.drawable.quiz_ic_option_combination
                    }
                }
            }
            ivOptionIcon.setImageResource(iconResId)

            // If this option was previously selected, highlight it
            if (savedAnswer != null && savedAnswer == i) {
                optionView.setBackgroundResource(R.drawable.quiz_bg_option)
                optionView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D8E8C6"))
                tvOptionText.setTextColor(Color.parseColor("#3E4D44"))
                tvOptionText.setTypeface(null, android.graphics.Typeface.BOLD)
                flOptionIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44"))
                ivOptionIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            } else {
                optionView.setBackgroundResource(R.drawable.quiz_bg_option)
                optionView.backgroundTintList = null
                tvOptionText.setTextColor(Color.parseColor("#3E4D44"))
                tvOptionText.setTypeface(null, android.graphics.Typeface.NORMAL)
                flOptionIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EAF2E3"))
                ivOptionIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44"))
            }

            optionView.setOnClickListener {
                // Select option
                selectedAnswers[currentQuestionIndex] = i
                
                // Show highlight effect immediately
                optionView.setBackgroundResource(R.drawable.quiz_bg_option)
                optionView.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D8E8C6"))
                tvOptionText.setTextColor(Color.parseColor("#3E4D44"))
                tvOptionText.setTypeface(null, android.graphics.Typeface.BOLD)
                flOptionIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3E4D44"))
                ivOptionIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))

                // Advance to next question after a very short delay
                optionView.postDelayed({
                    goToNextQuestion()
                }, 200)
            }

            binding.llOptionsContainer.addView(optionView)
        }

        // Show/hide bottom back button
        if (currentQuestionIndex == 0) {
            binding.btnPrevQuestion.visibility = View.INVISIBLE
        } else {
            binding.btnPrevQuestion.visibility = View.VISIBLE
        }
    }

    private fun goToNextQuestion() {
        if (currentQuestionIndex < questions.length() - 1) {
            currentQuestionIndex++
            displayQuestion()
        } else {
            calculateAndFinish()
        }
    }

    private fun goToPrevQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
            displayQuestion()
        }
    }

    private fun exitQuiz() {
        parentFragmentManager.popBackStack()
    }

    private fun calculateAndFinish() {
        // 1. Initialize scores
        val scores = HashMap<String, Int>()
        val skinTypes = listOf("dry", "oily", "sensitive", "dehydrated", "aging", "reactive", "acne", "normal", "combination")
        for (type in skinTypes) {
            scores[type] = 0
        }

        val flaggedGroups = HashSet<String>()

        // 2. Accumulate scores and flagged groups
        for (qIdx in 0 until questions.length()) {
            val selectedOptIdx = selectedAnswers[qIdx] ?: continue
            val questionObj = questions.getJSONObject(qIdx)
            val optionObj = questionObj.getJSONArray("options").getJSONObject(selectedOptIdx)

            // Add score points
            if (optionObj.has("score")) {
                val scoreObj = optionObj.getJSONObject("score")
                val keys = scoreObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = scoreObj.getInt(key)
                    scores[key] = (scores[key] ?: 0) + value
                }
            }

            // Flag irritation group
            if (optionObj.has("group")) {
                val groupVal = optionObj.getString("group")
                if (groupVal.isNotEmpty() && groupVal != "null") {
                    flaggedGroups.add(groupVal)
                }
            }
        }

        // 3. Determine primary skin type
        val primaryTypes = listOf("oily", "dry", "normal", "combination")
        var primaryType = "normal"
        var maxPrimaryScore = -1

        for (pt in primaryTypes) {
            val s = scores[pt] ?: 0
            if (s > maxPrimaryScore) {
                maxPrimaryScore = s
                primaryType = pt
            }
        }

        // Apply rules from quiz_loaida.json or custom logic rules
        var finalSkinTypeKey = primaryType
        val sensitiveScore = scores["sensitive"] ?: 0
        val dehydratedScore = scores["dehydrated"] ?: 0
        val acneScore = scores["acne"] ?: 0

        // Custom resolution matching the rules in quiz_loaida.json
        if (primaryType == "oily" && sensitiveScore >= 3) {
            finalSkinTypeKey = "oily_sensitive"
        } else if (primaryType == "dry" && dehydratedScore >= 3) {
            finalSkinTypeKey = "dehydrated"
        } else if (primaryType == "oily" && acneScore >= 3) {
            finalSkinTypeKey = "acne"
        } else if (sensitiveScore >= 4) {
            finalSkinTypeKey = "sensitive"
        }

        // Translate final skin type to Vietnamese
        val finalSkinTypeName = when (finalSkinTypeKey) {
            "normal" -> "Da thường"
            "dry" -> "Da khô"
            "oily" -> "Da dầu"
            "combination" -> "Da hỗn hợp"
            "sensitive" -> "Da nhạy cảm"
            "acne" -> "Da mụn"
            "aging" -> "Da lão hóa"
            "dehydrated" -> "Da mất nước"
            "reactive" -> "Da dễ kích ứng"
            "oily_sensitive" -> "Da dầu nhạy cảm"
            else -> "Da thường"
        }

        // Recommendation text
        val recommendation = when (finalSkinTypeKey) {
            "oily_sensitive" -> "Nên dùng sản phẩm dịu nhẹ, không cồn, không hương liệu và cấp nước nhẹ nhàng."
            "acne" -> "Tập trung kháng viêm, ngừa khuẩn, tránh các sản phẩm dầu khoáng gây bít tắc lỗ chân lông."
            "dehydrated" -> "Cấp ẩm tầng sâu bằng Hyaluronic Acid và glycerin, tránh sữa rửa mặt tạo bọt mạnh."
            "sensitive" -> "Sử dụng các thành phần làm dịu chiết xuất rau má, trà xanh, tránh tuyệt đối cồn khô."
            "dry" -> "Cấp ẩm bằng kem dưỡng ẩm đậm đặc, khóa ẩm tốt để tránh mất nước qua da."
            "oily" -> "Kiềm dầu nhẹ nhàng, cấp nước dạng gel, rửa mặt sạch sâu bằng chất làm sạch dịu nhẹ."
            else -> "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày."
        }

        // 4. Save results to SharedPreferences
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("SKIN_TYPE_RESULT", finalSkinTypeName)
            putString("RECOMMENDATION", recommendation)
            putStringSet("FLAGGED_GROUPS", flaggedGroups)
            apply()
        }

        // 5. Navigate to Loading analysis screen
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, QuizTestLoadingFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LEVEL_TYPE = "level_type"

        fun newInstance(level: String): QuizTestQuestionsFragment {
            val fragment = QuizTestQuestionsFragment()
            val args = Bundle()
            args.putString(ARG_LEVEL_TYPE, level)
            fragment.arguments = args
            return fragment
        }
    }
}
