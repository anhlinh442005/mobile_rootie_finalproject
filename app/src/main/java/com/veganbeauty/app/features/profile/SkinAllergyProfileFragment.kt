package com.veganbeauty.app.features.profile

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.FragmentSkinAllergyProfileBinding
import com.veganbeauty.app.features.home.BottomNavHelper
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import com.veganbeauty.app.features.quiz.QuizTestResultFragment
import com.veganbeauty.app.features.routine.SkinReminderFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SkinAllergyProfileFragment : RootieFragment() {

    private var _binding: FragmentSkinAllergyProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSkinAllergyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da hỗn hợp thiên dầu") ?: "Da hỗn hợp thiên dầu"
        val recommendation = prefs.getString("SAVED_RECOMMENDATION", "Hãy duy trì thói quen chăm sóc da lành tính hàng ngày...") ?: "Hãy duy trì thói quen chăm sóc da lành tính hàng ngày..."
        val flaggedSet = prefs.getStringSet("SAVED_FLAGGED_GROUPS", emptySet()) ?: emptySet()

        binding.tvSkinTypeResult.text = skinType
        binding.tvRecommendation.text = recommendation
        binding.tvSkinTypeDesc.text = getSkinTypeDesc(skinType)

        // Read or derive skin metrics
        val sensitivity = prefs.getInt("SAVED_SENSITIVITY", -1).let {
            if (it != -1) it else getDerivedSensitivity(skinType)
        }
        val hydration = prefs.getInt("SAVED_HYDRATION", -1).let {
            if (it != -1) it else getDerivedHydration(skinType)
        }
        val elasticity = prefs.getInt("SAVED_ELASTICITY", -1).let {
            if (it != -1) it else getDerivedElasticity(skinType)
        }
        val sebum = prefs.getInt("SAVED_SEBUM", -1).let {
            if (it != -1) it else getDerivedSebum(skinType)
        }
        val skinAreas = prefs.getString("SAVED_SKIN_AREAS", null) ?: getDerivedSkinAreas(skinType)

        // Bind skin metrics to layout progress bars and text values
        binding.pbSensitivity.progress = sensitivity
        binding.tvSensitivityVal.text = "$sensitivity%"
        binding.pbHydration.progress = hydration
        binding.tvHydrationVal.text = "$hydration%"
        binding.pbElasticity.progress = elasticity
        binding.tvElasticityVal.text = "$elasticity%"
        binding.pbSebum.progress = sebum
        binding.tvSebumVal.text = "$sebum%"
        binding.tvSkinAreasDesc.text = skinAreas

        // Setup Header Actions
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup Bottom Action Buttons
        binding.btnViewSkinReport.setOnClickListener {
            // Write transient keys first so QuizTestResultFragment can load it immediately
            prefs.edit().apply {
                putString("SKIN_TYPE_RESULT", skinType)
                putString("RECOMMENDATION", recommendation)
                putStringSet("FLAGGED_GROUPS", flaggedSet)
                putInt("SENSITIVITY_PERCENT", sensitivity)
                putInt("HYDRATION_PERCENT", hydration)
                putInt("ELASTICITY_PERCENT", elasticity)
                putInt("SEBUM_PERCENT", sebum)
                putString("SKIN_AREAS_DESC", skinAreas)
                apply()
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestResultFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnSetupRoutine.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, SkinReminderFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnRetakeQuiz.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, QuizTestIntroFragment())
                .addToBackStack(null)
                .commit()
        }

        // Process ingredients lists from quiz_thanhphan.json
        processIngredients(flaggedSet)

        // Load and check compatibility of products in use
        checkInUseProducts(flaggedSet)

        // Setup bottom navigation bar (Account tab active)
        BottomNavHelper.setup(
            fragment = this,
            root = binding.root,
            activeTabId = R.id.nav_account
        ) { tabId -> BottomNavHelper.navigate(this, tabId) }
    }

    private fun addPill(container: ViewGroup, text: String, backgroundResId: Int, textColorStr: String) {
        val pillView = LayoutInflater.from(context).inflate(R.layout.quiz_item_pill, container, false) as TextView
        pillView.text = text
        pillView.setBackgroundResource(backgroundResId)
        pillView.setTextColor(Color.parseColor(textColorStr))
        container.addView(pillView)
    }

    private fun getSkinTypeDesc(skinType: String): String {
        val lower = skinType.lowercase()
        return when {
            lower.contains("dầu") && lower.contains("nhạy cảm") ->
                "Làn da của bạn tiết nhiều dầu nhờn, lỗ chân lông to và rất dễ bị kích ứng, đỏ rát hoặc nổi mẩn khi gặp thành phần hoạt chất mạnh hoặc thay đổi thời tiết."
            lower.contains("khô") && lower.contains("nhạy cảm") ->
                "Làn da thiếu ẩm, thường xuyên bong tróc, căng rát và có hàng rào bảo vệ da yếu, cực kỳ nhạy cảm với cồn khô và hương liệu."
            lower.contains("dầu") ->
                "Làn da tiết nhiều bã nhờn toàn mặt hoặc vùng chữ T, dễ bít tắc lỗ chân lông gây mụn. Cần tập trung làm sạch sâu và cấp ẩm dạng gel mỏng nhẹ."
            lower.contains("khô") ->
                "Làn da thiếu hụt dầu tự nhiên, bề mặt thô ráp, xuất hiện các nếp nhăn li ti. Cần được bổ sung độ ẩm và khóa ẩm bằng các loại kem dưỡng đậm đặc."
            else ->
                "Làn da của bạn ở trạng thái tương đối cân bằng, tuy nhiên cần chăm sóc đều đặn để duy trì độ ẩm và bảo vệ da trước các tác nhân ô nhiễm môi trường."
        }
    }

    // Fallback derivations for older quiz results
    private fun getDerivedSensitivity(skinType: String): Int {
        val lower = skinType.lowercase()
        return when {
            lower.contains("nhạy cảm") || lower.contains("kích ứng") -> 85
            lower.contains("mụn") -> 65
            lower.contains("dầu") || lower.contains("khô") -> 45
            else -> 15
        }
    }

    private fun getDerivedHydration(skinType: String): Int {
        val lower = skinType.lowercase()
        return when {
            lower.contains("mất nước") -> 25
            lower.contains("khô") -> 35
            lower.contains("dầu") -> 60
            lower.contains("thường") -> 85
            else -> 55
        }
    }

    private fun getDerivedElasticity(skinType: String): Int {
        val lower = skinType.lowercase()
        return when {
            lower.contains("lão hóa") -> 40
            lower.contains("thường") -> 92
            else -> 75
        }
    }

    private fun getDerivedSebum(skinType: String): Int {
        val lower = skinType.lowercase()
        return when {
            lower.contains("dầu") -> 90
            lower.contains("hỗn hợp") -> 70
            lower.contains("khô") || lower.contains("mất nước") -> 20
            else -> 50
        }
    }

    private fun getDerivedSkinAreas(skinType: String): String {
        val lower = skinType.lowercase()
        return when {
            lower.contains("dầu") && lower.contains("nhạy cảm") ->
                "Vùng chữ T (trán, mũi, cằm) tiết nhiều dầu thừa, bóng nhờn; hai bên má nhạy cảm, dễ nổi mẩn đỏ, châm chích."
            lower.contains("mụn") ->
                "Vùng trán và cằm dễ bị bít tắc gây mụn; lượng dầu phân bổ không đều làm bít tắc cổ nang lông."
            lower.contains("mất nước") ->
                "Vùng chữ U (mũi và má) căng khô, thiếu nước trầm trọng; vùng chữ T có thể đổ dầu nhẹ do phản ứng bù ẩm."
            lower.contains("nhạy cảm") ->
                "Toàn bộ bề mặt da có lớp màng bảo vệ yếu, mỏng và dễ phản ứng châm chích với mọi mỹ phẩm mới."
            lower.contains("khô") ->
                "Khô ráp toàn mặt, vùng má căng chặt và có xu hướng bong tróc vảy da chết li ti."
            lower.contains("dầu") ->
                "Lượng dầu hoạt động mạnh mẽ trên toàn bộ khuôn mặt, bóng loáng đặc biệt ở vùng chữ T và hai bên cánh mũi."
            lower.contains("hỗn hợp") ->
                "Vùng chữ T đổ dầu nhiều và lỗ chân lông to; vùng chữ U (má) bình thường hoặc khô nhẹ."
            lower.contains("thường") ->
                "Độ ẩm phân bổ đều đặn, vùng chữ T dầu nhẹ không đáng kể, vùng má mịn màng đàn hồi tốt."
            else ->
                "Độ ẩm và bã nhờn phân bổ tương đối đồng đều trên các vùng da."
        }
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
                    if (risk == "safe") {
                        safeList.add(viName)
                    } else if (risk == "caution") {
                        cautionList.add(viName)
                    }
                }
            }

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

    private fun checkInUseProducts(flaggedGroups: Set<String>) {
        try {
            val jsonString = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")

            val tpString = requireContext().assets.open("quiz_thanhphan.json").bufferedReader().use { it.readText() }
            val tpObject = JSONObject(tpString)
            val tpArray = tpObject.getJSONArray("ingredients")

            val avoidChemicals = mutableSetOf<String>()
            val cautionChemicals = mutableSetOf<String>()

            for (i in 0 until tpArray.length()) {
                val ing = tpArray.getJSONObject(i)
                val name = ing.getString("name")
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

            // Base mock in-use products IDs
            val inUseIds = mutableSetOf(
                "5f29c9fa19873eb44aedced4", // Gel bí đao rửa mặt 140ml
                "1ba0d34dab3627b376a567c7", // Thạch bí đao 30ml
                "1e499ed75a31e4a02af2d962", // Sữa chống nắng bí đao 50ml
                "50c50369a5552d24a4c319b6"  // Tinh chất bí đao 70ml
            )

            val db = RootieDatabase.getDatabase(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                db.orderDao().getAllOrders().collect { orderList ->
                    val combinedIds = inUseIds.toMutableSet()
                    for (order in orderList) {
                        for (item in order.items) {
                            combinedIds.add(item.productId)
                        }
                    }
                    renderProductsInUse(jsonArray, combinedIds, avoidChemicals, cautionChemicals)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun renderProductsInUse(
        jsonArray: JSONArray,
        inUseIds: Set<String>,
        avoidChemicals: Set<String>,
        cautionChemicals: Set<String>
    ) {
        if (_binding == null) return
        binding.llProductsInUseContainer.removeAllViews()

        for (i in 0 until jsonArray.length()) {
            val prodObj = jsonArray.getJSONObject(i)
            val id = prodObj.getString("id")
            if (!inUseIds.contains(id)) continue

            val prodName = prodObj.getString("name")
            val mainImage = prodObj.optString("mainImage", "")

            val detailedIngredientsList = mutableListOf<String>()
            if (prodObj.has("detailedIngredients")) {
                val detailedArray = prodObj.getJSONArray("detailedIngredients")
                for (j in 0 until detailedArray.length()) {
                    detailedIngredientsList.add(detailedArray.getString(j).lowercase())
                }
            }

            val triggeredAvoids = mutableListOf<String>()
            val triggeredCautions = mutableListOf<String>()

            for (chem in avoidChemicals) {
                for (ing in detailedIngredientsList) {
                    if (ing.contains(chem)) {
                        triggeredAvoids.add(getViName(chem))
                    }
                }
            }

            for (chem in cautionChemicals) {
                for (ing in detailedIngredientsList) {
                    if (ing.contains(chem)) {
                        triggeredCautions.add(getViName(chem))
                    }
                }
            }

            val itemView = LayoutInflater.from(context).inflate(R.layout.quiz_item_product_recommendation, binding.llProductsInUseContainer, false)
            val tvProdName = itemView.findViewById<TextView>(R.id.tv_product_name)
            val tvProdDesc = itemView.findViewById<TextView>(R.id.tv_product_desc)
            val tvBadge = itemView.findViewById<TextView>(R.id.tv_compatibility_badge)
            val ivBadgeIcon = itemView.findViewById<ImageView>(R.id.iv_badge_icon)
            val ivImage = itemView.findViewById<ImageView>(R.id.iv_product_image)

            tvProdName.text = prodName

            if (mainImage.isNotEmpty()) {
                ivImage.load(mainImage) {
                    crossfade(true)
                    transformations(RoundedCornersTransformation(10f))
                    placeholder(R.drawable.myphamxanh)
                }
            } else {
                ivImage.setImageResource(R.drawable.myphamxanh)
            }

            if (triggeredAvoids.isNotEmpty()) {
                ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle)
                ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F04758"))
                tvBadge.text = "Nguy cơ kích ứng cao"
                tvBadge.setTextColor(Color.parseColor("#F04758"))
                val avoidStr = triggeredAvoids.distinct().joinToString(", ")
                tvProdDesc.text = "Sản phẩm có chứa $avoidStr không phù hợp với loại da nhạy cảm của bạn, dễ gây dị ứng hoặc kích ứng đỏ rát."
            } else if (triggeredCautions.isNotEmpty()) {
                ivBadgeIcon.setImageResource(R.drawable.quiz_ic_warning_triangle)
                ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EAAA08"))
                tvBadge.text = "Cần lưu ý khi dùng"
                tvBadge.setTextColor(Color.parseColor("#EAAA08"))
                val cautionStr = triggeredCautions.distinct().joinToString(", ")
                tvProdDesc.text = "Sản phẩm chứa $cautionStr. Cần theo dõi sát sao biểu hiện của da khi sử dụng sản phẩm này."
            } else {
                ivBadgeIcon.setImageResource(R.drawable.quiz_ic_wavy_check)
                ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#12B76A"))
                tvBadge.text = "Phù hợp"
                tvBadge.setTextColor(Color.parseColor("#12B76A"))
                tvProdDesc.text = "Bảng thành phần cực kỳ lành tính và hoàn toàn phù hợp với nền da hiện tại của bạn."
            }

            binding.llProductsInUseContainer.addView(itemView)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
