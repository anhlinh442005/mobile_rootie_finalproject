package com.veganbeauty.app.features.quiz

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import coil.load
import coil.transform.RoundedCornersTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.QuizTestResultBinding
import com.veganbeauty.app.features.home.BottomNavHelper
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.features.routine.SkinReminderFragment

class QuizTestResultFragment : RootieFragment() {

    private var _binding: QuizTestResultBinding? = null
    private val binding get() = _binding!!

    private val GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY
    private val morningSteps = mutableListOf<AiSkincareStep>()
    private val eveningSteps = mutableListOf<AiSkincareStep>()
    private var isMorningTab = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = QuizTestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Read results from SharedPreferences
        val prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SKIN_TYPE_RESULT", "Da thường") ?: "Da thường"
        val recommendation = prefs.getString("RECOMMENDATION", "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày.") ?: "Duy trì chế độ dưỡng ẩm cân bằng và làm sạch da dịu nhẹ hàng ngày."
        val flaggedSet = prefs.getStringSet("FLAGGED_GROUPS", emptySet()) ?: emptySet()
        val sensitivity = prefs.getInt("SENSITIVITY_PERCENT", 50)
        val hydration = prefs.getInt("HYDRATION_PERCENT", 50)
        val elasticity = prefs.getInt("ELASTICITY_PERCENT", 75)
        val sebum = prefs.getInt("SEBUM_PERCENT", 50)

        binding.tvSkinTypeResult.text = skinType
        binding.tvRecommendation.text = recommendation

        // Skin Type Description mapping
        val skinDesc = when (skinType) {
            "Da dầu nhạy cảm" -> "Làn da của bạn có xu hướng tiết nhiều dầu ở vùng chữ T nhưng dễ dàng bị kích ứng, đỏ rát bởi các tác nhân môi trường hoặc mỹ phẩm mạnh."
            "Da mụn" -> "Nền da dễ bị bít tắc lỗ chân lông, sinh nhân mụn đầu đen, mụn ẩn hoặc mụn viêm. Cần chú trọng làm sạch dịu nhẹ và kháng khuẩn."
            "Da nhạy cảm" -> "Làn da có hàng rào bảo vệ mỏng yếu, dễ đỏ rát, châm chích khi thay đổi thời tiết hoặc sử dụng sản phẩm có cồn/hương liệu."
            "Da khô" -> "Làn da thiếu hụt độ ẩm tự nhiên, thường xuyên có cảm giác căng chặt, bong tróc vảy nhỏ và dễ hình thành nếp nhăn sớm."
            "Da dầu" -> "Lượng bã nhờn hoạt động quá mức gây bóng loáng toàn mặt, lỗ chân lông to và dễ bám bụi bẩn hình thành mụn."
            "Da hỗn hợp" -> "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu nhờn, trong khi vùng chữ U (hai bên má) lại khô hoặc bình thường."
            "Da thường" -> "Làn da lý tưởng với độ ẩm cân bằng, lỗ chân lông nhỏ, da mịn màng khỏe mạnh và ít khi gặp các vấn đề kích ứng."
            "Da lão hóa" -> "Làn da bắt đầu xuất hiện các nếp nhăn nông sâu, độ đàn hồi kém, có thể có sạm nám và cần bổ sung chất chống oxy hóa mạnh."
            "Da mất nước" -> "Da có thể tiết dầu nhưng bề mặt vẫn căng khô, thô ráp do thiếu hụt lượng nước trong tế bào biểu bì."
            "Da dễ kích ứng" -> "Hàng rào bảo vệ da bị tổn thương nghiêm trọng, phản ứng tức thì với hầu hết các thành phần hoạt tính mạnh."
            else -> "Làn da cần được chăm sóc cân bằng và bảo vệ hàng ngày với các sản phẩm phù hợp."
        }
        binding.tvSkinTypeDesc.text = skinDesc

        // Setup back button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup done button (RETAKE QUIZ text button)
        binding.btnDone.setOnClickListener {
            // LÀM LẠI QUIZ: navigate directly to start Quiz
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestIntroFragment())
                .commit()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveSkinProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum, silent = false)
        }

        val isFirstTest = arguments?.getBoolean("IS_FIRST_TEST", false) ?: false
        if (isFirstTest) {
            binding.llAiRoutineSection.visibility = View.VISIBLE
            binding.llFooterButtons.visibility = View.GONE
            setupAiRoutineListeners(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum)
            loadAiRoutine(skinType, hydration, sebum, sensitivity, elasticity)
        } else {
            binding.llAiRoutineSection.visibility = View.GONE
            binding.llFooterButtons.visibility = View.VISIBLE
            binding.btnSaveProfile.visibility = View.GONE
            binding.btnSuggestRoutine.visibility = View.GONE
        }

        binding.cardAiAdvice.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.ai.SkinAiChatFragment())
                .addToBackStack(null)
                .commit()
        }

        // Process ingredients lists from quiz_thanhphan.json
        processIngredients(flaggedSet)

        // Process recommended products with risk evaluation
        recommendProducts(flaggedSet)

        // Setup bottom navigation bar
        BottomNavHelper.setup(
            fragment = this,
            root = binding.root,
            activeTabId = R.id.nav_myskin
        ) { tabId -> BottomNavHelper.navigate(this, tabId) }
    }

    private fun addPill(container: ViewGroup, text: String, backgroundResId: Int, textColorStr: String) {
        val pillView = LayoutInflater.from(context).inflate(R.layout.quiz_item_pill, container, false) as TextView
        pillView.text = text
        pillView.setBackgroundResource(backgroundResId)
        pillView.setTextColor(Color.parseColor(textColorStr))
        container.addView(pillView)
    }

    private fun processIngredients(flaggedGroups: Set<String>) {
        try {
            val jsonString = requireContext().assets.open("quiz_thanhphan.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val ingredientsArray = jsonObject.getJSONArray("ingredients")

            val avoidList = mutableListOf<String>()
            val cautionList = mutableListOf<String>()
            val safeList = mutableListOf<String>()

            for (i in 0 until ingredientsArray.length()) {
                val ing = ingredientsArray.getJSONObject(i)
                val viName = ing.getString("vi")
                val category = ing.getString("category")
                val risk = ing.getString("risk")

                if (flaggedGroups.contains(category)) {
                    when (risk) {
                        "avoid" -> avoidList.add(viName)
                        "caution" -> cautionList.add(viName)
                        "safe" -> safeList.add(viName)
                    }
                } else {
                    // Safe if not flagged
                    if (risk == "safe") {
                        safeList.add(viName)
                    } else if (risk == "caution") {
                        cautionList.add(viName)
                    }
                }
            }

            // Fill lists dynamically with pill badges
            binding.llAvoidPillsContainer.removeAllViews()
            binding.llCautionPillsContainer.removeAllViews()
            binding.llSuitablePillsContainer.removeAllViews()

            avoidList.distinct().forEach {
                addPill(binding.llAvoidPillsContainer, it, R.drawable.quiz_bg_pill_avoid, "#E91B2F")
            }
            if (avoidList.isEmpty()) {
                addPill(binding.llAvoidPillsContainer, "Cồn khô", R.drawable.quiz_bg_pill_avoid, "#E91B2F")
                addPill(binding.llAvoidPillsContainer, "Hương liệu", R.drawable.quiz_bg_pill_avoid, "#E91B2F")
            }

            cautionList.distinct().forEach {
                addPill(binding.llCautionPillsContainer, it, R.drawable.quiz_bg_pill_caution, "#677559")
            }
            if (cautionList.isEmpty()) {
                addPill(binding.llCautionPillsContainer, "Retinol", R.drawable.quiz_bg_pill_caution, "#677559")
                addPill(binding.llCautionPillsContainer, "BHA", R.drawable.quiz_bg_pill_caution, "#677559")
            }

            safeList.distinct().forEach {
                addPill(binding.llSuitablePillsContainer, it, R.drawable.quiz_bg_pill_suitable, "#67814D")
            }
            if (safeList.isEmpty()) {
                addPill(binding.llSuitablePillsContainer, "Glycerin", R.drawable.quiz_bg_pill_suitable, "#67814D")
                addPill(binding.llSuitablePillsContainer, "HA", R.drawable.quiz_bg_pill_suitable, "#67814D")
                addPill(binding.llSuitablePillsContainer, "Rau má", R.drawable.quiz_bg_pill_suitable, "#67814D")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getViName(chemicalName: String): String {
        return when (chemicalName.lowercase()) {
            "alcohol denat", "ethanol" -> "Cồn khô"
            "fragrance" -> "Hương liệu"
            "essential oil" -> "Tinh dầu"
            "sodium lauryl sulfate" -> "Chất tẩy rửa mạnh (SLS)"
            "sodium laureth sulfate" -> "Chất làm sạch mạnh"
            "cocamidopropyl betaine" -> "Chất tạo bọt"
            "retinol" -> "Retinol"
            "salicylic acid" -> "BHA"
            "glycolic acid" -> "AHA"
            "phenoxyethanol", "paraben" -> "Chất bảo quản"
            "propylene glycol" -> "Propylene Glycol"
            "glycerin" -> "Glycerin"
            "hyaluronic acid" -> "HA"
            "centella asiatica" -> "Rau má"
            "green tea" -> "Trà xanh"
            "aloe vera" -> "Nha đam"
            "silicone" -> "Silicone"
            "petrolatum" -> "Vaseline"
            else -> chemicalName
        }
    }

    private fun recommendProducts(flaggedGroups: Set<String>) {
        try {
            // Load products list from assets/products.json
            val jsonString = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")

            // Parse quiz_thanhphan.json to find chemical names to search for
            val tpString = requireContext().assets.open("quiz_thanhphan.json").bufferedReader().use { it.readText() }
            val tpObject = JSONObject(tpString)
            val tpArray = tpObject.getJSONArray("ingredients")

            // Map flagged categories to the chemical names to check
            val avoidChemicals = mutableSetOf<String>()
            val cautionChemicals = mutableSetOf<String>()

            for (i in 0 until tpArray.length()) {
                val ing = tpArray.getJSONObject(i)
                val name = ing.getString("name") // chemical name e.g. "Alcohol Denat"
                val category = ing.getString("category")
                val risk = ing.getString("risk")

                if (flaggedGroups.contains(category)) {
                    if (risk == "avoid") {
                        avoidChemicals.add(name.lowercase())
                    } else if (risk == "caution") {
                        cautionChemicals.add(name.lowercase())
                    }
                }
            }

            binding.llProductsContainer.removeAllViews()

            // Filter top 5 products to display
            var count = 0
            for (i in 0 until jsonArray.length()) {
                if (count >= 5) break
                val prodObj = jsonArray.getJSONObject(i)
                val prodName = prodObj.getString("name")
                val mainImage = prodObj.optString("mainImage", "")

                // Extract detailed ingredients to check compatibility
                val detailedIngredientsList = mutableListOf<String>()
                if (prodObj.has("detailedIngredients")) {
                    val detailedArray = prodObj.getJSONArray("detailedIngredients")
                    for (j in 0 until detailedArray.length()) {
                        detailedIngredientsList.add(detailedArray.getString(j).lowercase())
                    }
                }

                // Check for chemical occurrences
                val triggeredAvoids = mutableListOf<String>()
                val triggeredCautions = mutableListOf<String>()

                for (chem in avoidChemicals) {
                    for (ing in detailedIngredientsList) {
                        if (ing.contains(chem)) {
                            triggeredAvoids.add(chem)
                        }
                    }
                }

                for (chem in cautionChemicals) {
                    for (ing in detailedIngredientsList) {
                        if (ing.contains(chem)) {
                            triggeredCautions.add(chem)
                        }
                    }
                }

                // Inflate item view
                val itemView = LayoutInflater.from(context).inflate(R.layout.quiz_item_product_recommendation, binding.llProductsContainer, false)
                val tvProdName = itemView.findViewById<TextView>(R.id.tv_product_name)
                val tvProdDesc = itemView.findViewById<TextView>(R.id.tv_product_desc)
                val tvBadge = itemView.findViewById<TextView>(R.id.tv_compatibility_badge)
                val ivBadgeIcon = itemView.findViewById<ImageView>(R.id.iv_badge_icon)
                val ivImage = itemView.findViewById<ImageView>(R.id.iv_product_image)

                tvProdName.text = prodName

                // Load image elegantly
                if (mainImage.isNotEmpty()) {
                    ivImage.load(mainImage) {
                        crossfade(true)
                        transformations(RoundedCornersTransformation(10f))
                        placeholder(R.drawable.myphamxanh)
                    }
                } else {
                    ivImage.setImageResource(R.drawable.myphamxanh)
                }

                // Set compatibility badge style and detailed description
                if (triggeredAvoids.isNotEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle)
                    ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F04758"))
                    tvBadge.text = "Nguy cơ kích ứng cao"
                    tvBadge.setTextColor(Color.parseColor("#F04758"))
                    
                    val namesStr = triggeredAvoids.map { getViName(it) }.distinct().joinToString(", ")
                    tvProdDesc.text = "Sản phẩm chứa $namesStr không phù hợp với da nhạy cảm của bạn."
                } else if (triggeredCautions.isNotEmpty()) {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_circle)
                    ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E29400"))
                    tvBadge.text = "Cần thận trọng khi dùng"
                    tvBadge.setTextColor(Color.parseColor("#E29400"))

                    val namesStr = triggeredCautions.map { getViName(it) }.distinct().joinToString(", ")
                    tvProdDesc.text = "Sản phẩm chứa $namesStr cần lưu ý theo dõi khi sử dụng."
                } else {
                    ivBadgeIcon.setImageResource(R.drawable.quiz_ic_selected)
                    ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#375633"))
                    tvBadge.text = "Lành tính - Khuyên dùng"
                    tvBadge.setTextColor(Color.parseColor("#375633"))
                    tvProdDesc.text = "Công thức tối giản, lành tính giúp củng cố lớp màng ẩm tự nhiên mà không gây bí da."
                }

                binding.llProductsContainer.addView(itemView)
                count++
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showRoutineDialog(skinType: String) {
        val context = context ?: return
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .create()

        val dialogView = LayoutInflater.from(context).inflate(R.layout.quiz_dialog_routine, null)
        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val container = dialogView.findViewById<ViewGroup>(R.id.ll_steps_container)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_dialog)

        tvSubtitle.text = "Dành cho: $skinType"

        // Define routine steps based on skin type
        val steps = when (skinType) {
            "Da dầu nhạy cảm" -> listOf(
                Pair("Tẩy trang dịu nhẹ", "Sử dụng nước tẩy trang Micellar dịu nhẹ, không cồn khô hay hương liệu để làm sạch bụi bẩn vùng chữ T."),
                Pair("Sữa rửa mặt cân bằng", "Dùng sữa rửa mặt pH 5.5 - 6.0 (ví dụ Rootie Gentle Cleanser) làm sạch sâu mà không căng kích."),
                Pair("Làm dịu da", "Sử dụng nước hoa hồng (Toner) chiết xuất từ Rau má (Centella) hoặc Cúc la mã để phục hồi, làm dịu da đỏ rát."),
                Pair("Tinh chất cấp nước", "Bổ sung serum Hyaluronic Acid (HA) hoặc B5 cấp nước sâu cho các tế bào da mà không làm bít tắc."),
                Pair("Khóa ẩm gel nhẹ", "Sử dụng gel dưỡng ẩm dạng lỏng mỏng nhẹ để khóa ẩm cho da."),
                Pair("Chống nắng vật lý", "Sử dụng kem chống nắng vật lý SPF 30-50 mỏng nhẹ, lành tính không gây nhờn dính.")
            )
            "Da mụn" -> listOf(
                Pair("Tẩy trang chuyên biệt", "Sử dụng nước tẩy trang dành riêng cho da dầu mụn giúp kháng khuẩn hiệu quả."),
                Pair("Sữa rửa mặt ngừa mụn", "Sử dụng sữa rửa mặt chứa Salicylic Acid (BHA) nhẹ giúp làm sạch sâu bã nhờn."),
                Pair("Cân bằng & Sát khuẩn", "Dùng toner chứa tràm trà hoặc trà xanh làm dịu nốt mụn sưng đỏ."),
                Pair("Serum trị mụn & Thâm", "Thoa serum chứa Niacinamide hoặc BHA nhẹ để gom cồi mụn, mờ vết thâm."),
                Pair("Dưỡng ẩm dạng gel", "Khóa ẩm bằng gel dưỡng mỏng nhẹ không chứa dầu (oil-free) để tránh bít tắc lỗ chân lông."),
                Pair("Chống nắng không gây mụn", "Chọn kem chống nắng dạng lỏng nhẹ, ghi nhãn non-comedogenic (không sinh nhân mụn).")
            )
            "Da nhạy cảm" -> listOf(
                Pair("Tẩy trang siêu dịu nhẹ", "Nước tẩy trang micellar dành riêng cho da nhạy cảm, không cồn, không hương liệu."),
                Pair("Làm sạch không tạo bọt", "Dùng sữa rửa mặt dạng sữa hoặc gel dịu nhẹ, không chứa chất tẩy rửa mạnh."),
                Pair("Cấp ẩm & Làm dịu", "Sử dụng nước cân bằng giàu Panthenol (B5) và Rau má để củng cố màng bảo vệ da."),
                Pair("Tinh chất phục hồi", "Dùng tinh chất Ceramide hoặc B5 để phục hồi nhanh lớp sừng bị tổn thương."),
                Pair("Kem dưỡng phục hồi", "Dưỡng ẩm bằng kem dưỡng chuyên biệt cho da nhạy cảm để giữ nước và làm dịu."),
                Pair("Chống nắng vật lý", "KCN vật lý chứa Zinc Oxide và Titanium Dioxide bảo vệ da khỏi tia UV dịu nhẹ nhất.")
            )
            "Da khô" -> listOf(
                Pair("Tẩy trang dạng dầu/sữa", "Sử dụng dầu tẩy trang hoặc sữa tẩy trang cấp ẩm để hòa tan bụi bẩn mà không làm khô da."),
                Pair("Sữa rửa mặt cấp ẩm", "Dùng sữa rửa mặt dạng kem chứa glycerin hoặc hyaluronic acid, tránh các sản phẩm tạo bọt nhiều."),
                Pair("Dưỡng ẩm tầng sâu", "Thoa toner dưỡng ẩm đậm đặc dạng sữa (milky toner) ngay sau khi rửa mặt."),
                Pair("Serum nuôi dưỡng da", "Sử dụng serum HA kết hợp Peptide hoặc Vitamin E để chống lão hóa và cấp nước."),
                Pair("Kem dưỡng ẩm đậm đặc", "Khóa ẩm bằng kem dưỡng dạng cream (kem đặc) giàu dưỡng chất để ngăn ngừa mất nước."),
                Pair("Chống nắng cấp ẩm", "KCN có bổ sung các thành phần dưỡng ẩm để giữ da căng mướt suốt ngày.")
            )
            "Da dầu" -> listOf(
                Pair("Tẩy trang kiềm dầu", "Nước tẩy trang sạch sâu giúp lấy đi bã nhờn dư thừa vùng chữ T."),
                Pair("Sữa rửa mặt làm sạch sâu", "Sữa rửa mặt tạo bọt mịn hoặc chứa BHA nhẹ để làm thông thoáng lỗ chân lông."),
                Pair("Se khít lỗ chân lông", "Toner chứa BHA hoặc hạt Phỉ để cân bằng dầu thừa và se nhỏ lỗ chân lông."),
                Pair("Tinh chất kiểm soát dầu", "Sử dụng serum Niacinamide 10% kết hợp Kẽm (Zinc) để kiểm soát tuyến bã nhờn."),
                Pair("Gel dưỡng ẩm mỏng nhẹ", "Chọn dưỡng ẩm dạng gel-cream thấm nhanh, không bóng nhờn."),
                Pair("Chống nắng kiềm dầu", "KCN dạng sữa lỏng nhẹ có màng lọc kiềm dầu tốt (matte finish).")
            )
            else -> listOf(
                Pair("Tẩy trang cơ bản", "Làm sạch lớp bụi bẩn và kem chống nắng mỗi tối bằng nước tẩy trang dịu nhẹ."),
                Pair("Sữa rửa mặt dịu nhẹ", "Dùng sữa rửa mặt cân bằng ẩm để làm sạch nhẹ nhàng hàng ngày."),
                Pair("Cân bằng độ pH", "Sử dụng nước hoa hồng dịu nhẹ cấp ẩm tức thì cho da."),
                Pair("Tinh chất bảo vệ da", "Sử dụng serum Vitamin C (sáng) để chống oxy hóa hoặc HA (tối) để cấp ẩm."),
                Pair("Dưỡng ẩm cân bằng", "Sử dụng kem dưỡng ẩm dạng lotion hoặc gel-cream nhẹ nhàng."),
                Pair("Bảo vệ da", "Bôi kem chống nắng SPF 30+ hàng ngày để ngăn ngừa lão hóa sớm.")
            )
        }

        container.removeAllViews()
        steps.forEachIndexed { index, pair ->
            val stepView = LayoutInflater.from(context).inflate(R.layout.quiz_item_routine_step, container, false)
            val tvNum = stepView.findViewById<TextView>(R.id.tv_step_number)
            val tvTbl = stepView.findViewById<TextView>(R.id.tv_step_title)
            val tvDsc = stepView.findViewById<TextView>(R.id.tv_step_desc)

            tvNum.text = (index + 1).toString()
            tvTbl.text = pair.first
            tvDsc.text = pair.second
            container.addView(stepView)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupAiRoutineListeners(
        prefs: android.content.SharedPreferences,
        skinType: String,
        recommendation: String,
        flaggedSet: Set<String>,
        sensitivity: Int,
        hydration: Int,
        elasticity: Int,
        sebum: Int
    ) {
        binding.tabMorning.setOnClickListener {
            if (!isMorningTab) {
                isMorningTab = true
                updateTabUI()
                populateSteps()
            }
        }

        binding.tabEvening.setOnClickListener {
            if (isMorningTab) {
                isMorningTab = false
                updateTabUI()
                populateSteps()
            }
        }

        binding.btnApplyRoutine.setOnClickListener {
            saveSelectedStepsToProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum)
        }

        binding.tvRetakeQuizInline.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestIntroFragment())
                .commit()
        }
    }

    private fun updateTabUI() {
        if (isMorningTab) {
            binding.tabMorning.setBackgroundResource(R.drawable.bg_btn_solid)
            binding.tabMorning.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#677559"))
            binding.tvTabMorning.setTextColor(android.graphics.Color.WHITE)

            binding.tabEvening.setBackgroundResource(android.R.color.transparent)
            binding.tabEvening.backgroundTintList = null
            binding.tvTabEvening.setTextColor(android.graphics.Color.parseColor("#677559"))
        } else {
            binding.tabEvening.setBackgroundResource(R.drawable.bg_btn_solid)
            binding.tabEvening.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#677559"))
            binding.tvTabEvening.setTextColor(android.graphics.Color.WHITE)

            binding.tabMorning.setBackgroundResource(android.R.color.transparent)
            binding.tabMorning.backgroundTintList = null
            binding.tvTabMorning.setTextColor(android.graphics.Color.parseColor("#677559"))
        }
    }

    private fun loadAiRoutine(skinType: String, hydration: Int, sebum: Int, sensitivity: Int, elasticity: Int) {
        if (GEMINI_API_KEY.isBlank() || GEMINI_API_KEY == "YOUR_GEMINI_API_KEY_HERE") {
            generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity)
        } else {
            fetchRoutineFromAi(skinType, hydration, sebum, sensitivity, elasticity)
        }
    }

    private fun fetchRoutineFromAi(skinType: String, hydration: Int, sebum: Int, sensitivity: Int, elasticity: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.doOutput = true

                val prompt = """
                    Bạn là trợ lý Rootie AI chuyên thiết kế chu trình dưỡng da thuần chay (skincare routine) phù hợp nhất với các chỉ số da người dùng.
                    Hãy phân tích chi tiết làn da người dùng có các chỉ số sau:
                    - Loại da: $skinType
                    - Độ ẩm da: $hydration%
                    - Chỉ số bã nhờn: $sebum%
                    - Độ nhạy cảm da: $sensitivity%
                    - Độ đàn hồi: $elasticity%

                    Hãy đề xuất một routine buổi sáng và routine buổi tối phù hợp nhất sử dụng các sản phẩm thuần chay (Ví dụ: Gel rửa mặt bí đao, Tinh chất bí đao, Thạch bí đao, Thạch hoa hồng dưỡng ẩm, Sữa rửa mặt nghệ Hưng Yên, Nước hoa hồng Cao Bằng, Nước tẩy trang sen Hậu Giang, Sữa chống nắng bí đao...).
                    Lưu ý: Giải thích lý do vì sao mỗi sản phẩm/bước dưỡng lại hoàn hảo cho các chỉ số cụ thể của làn da này.

                    Trả về cấu trúc JSON chính xác như sau và KHÔNG kèm bất cứ ký tự nào khác ngoài JSON:
                    {
                      "assessment": "Một đoạn phân tích chi tiết, khoa học và ấm áp về tình trạng da của người dùng dựa trên độ ẩm, dầu nhờn và độ nhạy cảm.",
                      "morning_steps": [
                        {
                          "name": "Tên bước (ví dụ: Sữa rửa mặt)",
                          "product": "Tên sản phẩm thuần chay (ví dụ: Gel rửa mặt bí đao)",
                          "reason": "Giải thích chi tiết tại sao phù hợp (ví dụ: Giúp làm sạch sâu bã nhờn ở mức $sebum% nhưng giữ ẩm nhẹ nhàng cho da nhạy cảm)"
                        }
                      ],
                      "evening_steps": [
                        {
                          "name": "Tên bước",
                          "product": "Tên sản phẩm",
                          "reason": "Giải thích chi tiết tại sao phù hợp"
                        }
                      ]
                    }
                """.trimIndent()

                val requestJson = JSONObject()
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                }
                val contentsArray = JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                }
                requestJson.put("contents", contentsArray)

                val systemInstruction = JSONObject().apply {
                    val systemParts = JSONArray().apply {
                        put(JSONObject().put("text", "Bạn chỉ trả về duy nhất chuỗi JSON hợp lệ theo cấu trúc được yêu cầu."))
                    }
                    put("parts", systemParts)
                }
                requestJson.put("systemInstruction", systemInstruction)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 2000)
                }
                requestJson.put("generationConfig", generationConfig)

                connection.outputStream.use { os ->
                    val input = requestJson.toString().toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val candidates = json.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val textResult = parts.getJSONObject(0).getString("text").trim()
                            
                            val cleanJson = if (textResult.startsWith("```json")) {
                                textResult.substringAfter("```json").substringBeforeLast("```").trim()
                            } else if (textResult.startsWith("```")) {
                                textResult.substringAfter("```").substringBeforeLast("```").trim()
                            } else {
                                textResult
                            }

                            val routineObj = JSONObject(cleanJson)
                            val morningJson = routineObj.getJSONArray("morning_steps")
                            val eveningJson = routineObj.getJSONArray("evening_steps")

                            withContext(Dispatchers.Main) {
                                morningSteps.clear()
                                for (i in 0 until morningJson.length()) {
                                    val step = morningJson.getJSONObject(i)
                                    morningSteps.add(
                                        AiSkincareStep(
                                            index = i,
                                            name = step.getString("name"),
                                            recommendedProduct = step.getString("product"),
                                            description = step.getString("reason"),
                                            isChecked = true
                                        )
                                    )
                                }

                                eveningSteps.clear()
                                for (i in 0 until eveningJson.length()) {
                                    val step = eveningJson.getJSONObject(i)
                                    eveningSteps.add(
                                        AiSkincareStep(
                                            index = i,
                                            name = step.getString("name"),
                                            recommendedProduct = step.getString("product"),
                                            description = step.getString("reason"),
                                            isChecked = true
                                        )
                                    )
                                }

                                populateSteps()
                            }
                            return@launch
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    generateRuleBasedRoutine(skinType, hydration, sebum, sensitivity, elasticity)
                }
            }
        }
    }

    private fun generateRuleBasedRoutine(skinType: String, hydration: Int, sebum: Int, sensitivity: Int, elasticity: Int) {
        val lowerSkinType = skinType.lowercase()

        morningSteps.clear()
        eveningSteps.clear()

        if (lowerSkinType.contains("dầu") || lowerSkinType.contains("mụn") || lowerSkinType.contains("hỗn hợp thiên dầu")) {
            morningSteps.addAll(listOf(
                AiSkincareStep(0, "Sữa rửa mặt", "Gel rửa mặt Bí đao", "Loại bỏ dầu thừa ($sebum%) nhẹ nhàng mà không làm khô căng da."),
                AiSkincareStep(1, "Nước cân bằng", "Nước bí đao cân bằng da", "Làm sạch sâu bã nhờn vùng chữ T và kháng viêm ngừa mụn."),
                AiSkincareStep(2, "Tinh chất", "Tinh chất bí đao", "Chứa 7% Niacinamide giúp kiềm dầu tối đa và thu nhỏ lỗ chân lông."),
                AiSkincareStep(3, "Gel dưỡng ẩm", "Thạch bí đao dưỡng ẩm", "Cấp ẩm mỏng nhẹ dạng gel-cream, không gây bít tắc nang lông."),
                AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ tối ưu khỏi tia UV với màng lọc kiềm dầu thoáng nhẹ.")
            ))

            eveningSteps.addAll(listOf(
                AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang bí đao", "Hòa tan dầu thừa, bụi bẩn PM2.5 và kem chống nắng sâu trong lỗ chân lông."),
                AiSkincareStep(1, "Sữa rửa mặt", "Gel rửa mặt bí đao", "Làm sạch sâu da mặt để chuẩn bị cho các bước dưỡng tiếp theo."),
                AiSkincareStep(2, "Nước cân bằng", "Nước bí đao cân bằng da", "Cân bằng lại pH da và làm dịu nhanh các nốt mụn sưng đỏ."),
                AiSkincareStep(3, "Tinh chất", "Tinh chất bí đao", "Tập trung điều trị mụn ẩn và làm mờ thâm mụn ban đêm."),
                AiSkincareStep(4, "Dưỡng ẩm khóa nước", "Thạch bí đao dưỡng ẩm", "Giữ nước khóa ẩm dịu nhẹ giúp da phục hồi lúc ngủ.")
            ))
        } else if (lowerSkinType.contains("khô")) {
            morningSteps.addAll(listOf(
                AiSkincareStep(0, "Sữa rửa mặt", "Sữa rửa mặt nghệ Hưng Yên", "Làm sạch dịu nhẹ không bọt, bổ sung beta-carotene dưỡng ẩm."),
                AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cấp nước bù ẩm tức thì cho lớp sừng khô ráp."),
                AiSkincareStep(2, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Cung cấp vitamin C và curcumin chống oxy hóa, sáng da mặt."),
                AiSkincareStep(3, "Kem dưỡng ẩm", "Thạch hoa hồng dưỡng ẩm", "Nuôi dưỡng làn da căng mướt suốt 24 giờ liên tục."),
                AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ màng ẩm của da khô khỏi ánh nắng trực tiếp.")
            ))

            eveningSteps.addAll(listOf(
                AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang hoa hồng Cao Bằng", "Tẩy trang dịu nhẹ đồng thời cấp ẩm sâu cho da không bị khô ráp."),
                AiSkincareStep(1, "Sữa rửa mặt", "Sữa rửa mặt nghệ Hưng Yên", "Làm sạch sâu bụi bẩn mà vẫn giữ lại độ ẩm tự nhiên cho da."),
                AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cân bằng độ pH và bù nước tức thì sau khi rửa mặt."),
                AiSkincareStep(3, "Tinh chất phục hồi", "Tinh chất hoa hồng Cao Bằng", "Bổ sung acid amin nuôi dưỡng sâu các tế bào da mất nước."),
                AiSkincareStep(4, "Kem dưỡng đêm", "Thạch hoa hồng dưỡng ẩm", "Khóa dưỡng chất ban đêm giúp da mướt mịn căng tràn vào sáng hôm sau.")
            ))
        } else if (lowerSkinType.contains("nhạy cảm") || lowerSkinType.contains("dễ kích ứng")) {
            morningSteps.addAll(listOf(
                AiSkincareStep(0, "Sữa rửa mặt", "Sữa rửa mặt sen Hậu Giang", "Bảo vệ màng lipid nhạy cảm ($sensitivity%), làm sạch không sulfate."),
                AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Làm dịu nhanh cảm giác châm chích và đỏ rát."),
                AiSkincareStep(2, "Tinh chất phục hồi", "Tinh chất rau má", "Kích thích sinh collagen, phục hồi hàng rào bảo vệ bị suy yếu."),
                AiSkincareStep(3, "Dưỡng ẩm làm dịu", "Thạch bí đao dưỡng ẩm", "Làm mát và dưỡng ẩm dịu nhẹ cho da nhạy cảm cực kỳ an toàn."),
                AiSkincareStep(4, "Chống nắng vật lý", "Sữa chống nắng bí đao", "Bảo vệ da dịu nhẹ nhất khỏi tia UV mà không gây bí hay ngứa.")
            ))

            eveningSteps.addAll(listOf(
                AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang sen Hậu Giang", "Công thức Micellar làm sạch nhẹ nhàng không gây rát da."),
                AiSkincareStep(1, "Sữa rửa mặt", "Sữa rửa mặt sen Hậu Giang", "Sạch sâu dịu nhẹ bảo vệ da khỏi kích ứng."),
                AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Bù ẩm và cân bằng lại trạng thái ổn định cho da."),
                AiSkincareStep(3, "Tinh chất", "Tinh chất rau má", "Hỗ trợ làm lành nhanh các tổn thương của biểu bì nhạy cảm ban đêm."),
                AiSkincareStep(4, "Khóa ẩm dịu mát", "Thạch bí đao dưỡng ẩm", "Khóa ẩm dịu nhẹ không cồn, không hương liệu cho da nhạy cảm.")
            ))
        } else {
            morningSteps.addAll(listOf(
                AiSkincareStep(0, "Sữa rửa mặt", "Gel rửa mặt cà phê", "Rửa mặt sảng khoái với hạt cà phê siêu mịn khơi dậy năng lượng da."),
                AiSkincareStep(1, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Cấp ẩm dị mọc và cân bằng độ pH lý tưởng."),
                AiSkincareStep(2, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Làm đều màu da, mờ thâm mụn, chống oxy hóa."),
                AiSkincareStep(3, "Dưỡng ẩm", "Thạch hoa hồng dưỡng ẩm", "Dưỡng da căng mướt mịn màng tự nhiên."),
                AiSkincareStep(4, "Chống nắng", "Sữa chống nắng bí đao", "Bảo vệ da tối ưu dưới tia UV gay gắt.")
            ))

            eveningSteps.addAll(listOf(
                AiSkincareStep(0, "Tẩy trang", "Nước tẩy trang sen Hậu Giang", "Sạch thoáng bụi bẩn bã nhờn sau ngày dài."),
                AiSkincareStep(1, "Sữa rửa mặt", "Gel rửa mặt cà phê", "Làm sạch sâu lỗ chân lông nhẹ nhàng."),
                AiSkincareStep(2, "Nước cân bằng", "Nước hoa hồng Cao Bằng", "Khôi phục lại màng ẩm sau khi rửa mặt."),
                AiSkincareStep(3, "Tinh chất dưỡng sáng", "Tinh chất nghệ Hưng Yên", "Làm mờ thâm mụn sạm màu hiệu quả ban đêm."),
                AiSkincareStep(4, "Dưỡng ẩm ban đêm", "Thạch hoa hồng dưỡng ẩm", "Khóa ẩm giúp da mềm mịn và rạng rỡ vào sáng hôm sau.")
            ))
        }

        populateSteps()
    }

    private fun populateSteps() {
        binding.llAiRoutineStepsContainer.removeAllViews()
        val stepsList = if (isMorningTab) morningSteps else eveningSteps

        stepsList.forEach { step ->
            val stepView = LayoutInflater.from(requireContext()).inflate(R.layout.quiz_item_ai_routine_step, binding.llAiRoutineStepsContainer, false)
            
            val ivCheckbox = stepView.findViewById<ImageView>(R.id.iv_step_checkbox)
            val tvIndex = stepView.findViewById<TextView>(R.id.tv_step_index)
            val tvName = stepView.findViewById<TextView>(R.id.tv_step_name)
            val tvProduct = stepView.findViewById<TextView>(R.id.tv_recommended_product)
            val tvReason = stepView.findViewById<TextView>(R.id.tv_ai_reason)
            val layoutCard = stepView.findViewById<View>(R.id.layout_step_card)

            tvIndex.text = (step.index + 1).toString()
            tvName.text = step.name
            tvProduct.text = step.recommendedProduct
            tvReason.text = step.description

            fun updateCheckboxState() {
                if (step.isChecked) {
                    ivCheckbox.setImageResource(R.drawable.skin_ic_checkbox_checked)
                } else {
                    ivCheckbox.setImageResource(R.drawable.skin_ic_checkbox_unchecked)
                }
            }

            updateCheckboxState()

            ivCheckbox.setOnClickListener {
                step.isChecked = !step.isChecked
                updateCheckboxState()
            }

            layoutCard.setOnClickListener {
                step.isChecked = !step.isChecked
                updateCheckboxState()
            }

            binding.llAiRoutineStepsContainer.addView(stepView)
        }
    }

    private fun saveSkinProfile(
        prefs: android.content.SharedPreferences,
        skinType: String,
        recommendation: String,
        flaggedSet: Set<String>,
        sensitivity: Int,
        hydration: Int,
        elasticity: Int,
        sebum: Int,
        silent: Boolean = false
    ) {
        val skinAreas = prefs.getString("SKIN_AREAS_DESC", "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da.")

        val lastTestTime = com.veganbeauty.app.data.local.ProfileSession.getLastSkinTestTime(requireContext())
        val currentTime = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        val isEligibleForReward = lastTestTime == 0L || (currentTime - lastTestTime >= sevenDaysMs)

        // Save active skin type, recommendations and flagged groups
        prefs.edit().apply {
            putString("SAVED_USER_SKIN_TYPE", skinType)
            putString("SAVED_RECOMMENDATION", recommendation)
            putStringSet("SAVED_FLAGGED_GROUPS", flaggedSet)
            putInt("SAVED_SENSITIVITY", sensitivity)
            putInt("SAVED_HYDRATION", hydration)
            putInt("SAVED_ELASTICITY", elasticity)
            putInt("SAVED_SEBUM", sebum)
            putString("SAVED_SKIN_AREAS", skinAreas)
            putLong("KEY_LAST_SKIN_TEST_TIME", currentTime)
            putBoolean("KEY_HIDE_QUIZ_REMINDER_WEEKLY", false) // Reset dismiss state on new test
            apply()
        }

        // Save to quiz history list
        try {
            val historyStr = prefs.getString("QUIZ_HISTORY_LIST", "[]") ?: "[]"
            val historyArray = org.json.JSONArray(historyStr)
            
            val newLog = org.json.JSONObject().apply {
                put("date", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
                put("skinType", skinType)
                put("recommendation", recommendation)
                put("sensitivity", sensitivity)
                put("hydration", hydration)
                put("elasticity", elasticity)
                put("sebum", sebum)
            }
            
            historyArray.put(newLog)
            prefs.edit().putString("QUIZ_HISTORY_LIST", historyArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isEligibleForReward) {
            val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                db.rewardPointDao().insertRewardPoints(
                    com.veganbeauty.app.data.local.entities.RewardPointEntity(
                        orderId = "SYSTEM_WEEKLY_QUIZ",
                        points = 100,
                        reason = "Cập nhật làn da định kỳ hàng tuần (+100 xu)",
                        timestamp = currentTime
                    )
                )
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(requireContext())
                
                if (!silent) {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null)
                    val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
                    val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
                    val llCoinBadge = dialogView.findViewById<android.view.View>(R.id.ll_dialog_coin_badge)
                    val tvCoinText = dialogView.findViewById<TextView>(R.id.tv_dialog_coin_text)
                    val btnConfirm = dialogView.findViewById<android.view.View>(R.id.btn_dialog_confirm)
                    val tvConfirmText = dialogView.findViewById<TextView>(R.id.tv_dialog_confirm_text)
                    val btnCancel = dialogView.findViewById<android.view.View>(R.id.btn_dialog_cancel)

                    tvTitle.text = "Nhận 100 xu thành công!"
                    tvMsg.text = "Cảm ơn bạn đã cập nhật chỉ số da định kỳ. Bạn được cộng +100 xu vào ví thành viên!"
                    llCoinBadge.visibility = android.view.View.VISIBLE
                    tvCoinText.text = "Tặng +100 Xu thành viên"
                    tvConfirmText.text = "TUYỆT VỜI"
                    btnCancel.visibility = android.view.View.GONE

                    val customDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .create()
                    customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    btnConfirm.setOnClickListener {
                        customDialog.dismiss()
                        parentFragmentManager.popBackStack()
                    }

                    customDialog.setOnDismissListener {
                        parentFragmentManager.popBackStack()
                    }

                    customDialog.show()
                }
            }
        } else {
            if (!silent) {
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null)
                val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
                val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
                val btnConfirm = dialogView.findViewById<android.view.View>(R.id.btn_dialog_confirm)
                val tvConfirmText = dialogView.findViewById<TextView>(R.id.tv_dialog_confirm_text)
                val btnCancel = dialogView.findViewById<android.view.View>(R.id.btn_dialog_cancel)

                tvTitle.text = "Đã lưu hồ sơ da!"
                tvMsg.text = "Chỉ số da và loại da $skinType đã được lưu vào lịch sử của bạn."
                tvConfirmText.text = "ĐỒNG Ý"
                btnCancel.visibility = android.view.View.GONE

                val customDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                btnConfirm.setOnClickListener {
                    customDialog.dismiss()
                    parentFragmentManager.popBackStack()
                }

                customDialog.setOnDismissListener {
                    parentFragmentManager.popBackStack()
                }

                customDialog.show()
            }
        }
    }

    private fun saveSelectedStepsToProfile(
        prefs: android.content.SharedPreferences,
        skinType: String,
        recommendation: String,
        flaggedSet: Set<String>,
        sensitivity: Int,
        hydration: Int,
        elasticity: Int,
        sebum: Int
    ) {
        val morningSave = morningSteps.filter { it.isChecked }.map { step ->
            "${step.index}:${step.name}:${step.recommendedProduct}:true"
        }.toSet()

        val eveningSave = eveningSteps.filter { it.isChecked }.map { step ->
            "${step.index}:${step.name}:${step.recommendedProduct}:true"
        }.toSet()

        ProfileSession.setMorningSteps(requireContext(), morningSave)
        ProfileSession.setEveningSteps(requireContext(), eveningSave)

        // Silent save skin profile results & reward point calculations
        saveSkinProfile(prefs, skinType, recommendation, flaggedSet, sensitivity, hydration, elasticity, sebum, silent = true)

        var isNavigatingToReminder = false
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_save_success, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnConfirm = dialogView.findViewById<android.view.View>(R.id.btn_dialog_confirm)
        val tvConfirmText = dialogView.findViewById<TextView>(R.id.tv_dialog_confirm_text)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_dialog_cancel)

        tvTitle.text = "Áp dụng và lưu thành công!"
        tvMsg.text = "Đã lưu chỉ số da và áp dụng routine AI vào lịch trình hàng ngày.\n\nBạn có muốn thiết lập thời gian nhắc nhở routine không?"
        tvConfirmText.text = "CÀI ĐẶT NHẮC HẸN"
        btnCancel.text = "ĐỂ SAU"

        val customDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        customDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnConfirm.setOnClickListener {
            isNavigatingToReminder = true
            customDialog.dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        btnCancel.setOnClickListener {
            customDialog.dismiss()
            parentFragmentManager.popBackStack()
        }

        customDialog.setOnDismissListener {
            if (!isNavigatingToReminder) {
                parentFragmentManager.popBackStack()
            }
        }

        customDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class AiSkincareStep(
        val index: Int,
        val name: String,
        val recommendedProduct: String,
        val description: String,
        var isChecked: Boolean = true
    )
}
