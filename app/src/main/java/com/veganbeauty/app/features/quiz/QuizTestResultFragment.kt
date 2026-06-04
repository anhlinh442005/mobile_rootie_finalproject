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
import org.json.JSONArray
import org.json.JSONObject

class QuizTestResultFragment : RootieFragment() {

    private var _binding: QuizTestResultBinding? = null
    private val binding get() = _binding!!

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
            // LÀM LẠI QUIZ: pop back stack to clear result/questions and return to intro or start again
            parentFragmentManager.popBackStack()
        }

        // Setup save profile button
        binding.btnSaveProfile.setOnClickListener {
            // Save active skin type, recommendations and flagged groups
            prefs.edit().apply {
                putString("SAVED_USER_SKIN_TYPE", skinType)
                putString("SAVED_RECOMMENDATION", recommendation)
                putStringSet("SAVED_FLAGGED_GROUPS", flaggedSet)
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
                }
                
                historyArray.put(newLog)
                prefs.edit().putString("QUIZ_HISTORY_LIST", historyArray.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Toast.makeText(context, "Đã cập nhật $skinType vào hồ sơ da và lịch sử!", Toast.LENGTH_SHORT).show()
        }

        // Setup suggest routine button
        binding.btnSuggestRoutine.setOnClickListener {
            showRoutineDialog(skinType)
        }

        // Process ingredients lists from quiz_thanhphan.json
        processIngredients(flaggedSet)

        // Process recommended products with risk evaluation
        recommendProducts(flaggedSet)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
