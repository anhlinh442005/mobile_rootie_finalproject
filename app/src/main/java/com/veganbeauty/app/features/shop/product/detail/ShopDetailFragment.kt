package com.veganbeauty.app.features.shop.product.detail

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import org.json.JSONObject
import com.veganbeauty.app.core.view.FlowLayout
import coil.load
import com.google.android.material.tabs.TabLayout
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopProductDetailBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShopDetailFragment : RootieFragment() {

    private var _binding: ShopProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: ProductEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup mock adapters to show dummy UI while testing
        binding.rvHandbook.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_handbook, 3)
        binding.rvRelatedProducts.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_card, 2)
        binding.rvRecentlyViewed.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_card, 2)

        binding.btnSearch.setOnClickListener {
            val searchFragment = com.veganbeauty.app.features.shop.search.ShopSearchFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnCart.setOnClickListener {
            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment()
            cartSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG)
        }

        binding.btnBuyOnline.setOnClickListener {
            product?.let { p ->
                val bottomSheet = com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                    product = p,
                    onAddToCartClick = { prod, quantity ->
                        com.veganbeauty.app.features.shop.product.CartHelper.addToCart(requireContext(), lifecycleScope, prod, quantity)
                    },
                    onBuyNowClick = { prod, quantity ->
                        val checkoutItem = com.veganbeauty.app.data.local.entities.CartItemEntity(
                            id = prod.id,
                            name = prod.name,
                            image = prod.mainImage,
                            price = prod.price,
                            quantity = quantity,
                            isSelected = true
                        )
                        val checkoutFragment = com.veganbeauty.app.features.shop.product.ShopCheckoutFragment.newInstance(arrayListOf(checkoutItem))
                        parentFragmentManager.beginTransaction()
                            .replace(com.veganbeauty.app.R.id.main_container, checkoutFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }
        }

        // Setup tab selection behavior
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    binding.llTabInfo.visibility = View.GONE
                    binding.llTabIngredients.visibility = View.GONE
                    binding.llTabStory.visibility = View.GONE
                    binding.llTabBenefits.visibility = View.GONE
                    binding.llTabUsage.visibility = View.GONE
                    
                    when (it.position) {
                        0 -> binding.llTabInfo.visibility = View.VISIBLE
                        1 -> binding.llTabIngredients.visibility = View.VISIBLE
                        2 -> binding.llTabStory.visibility = View.VISIBLE
                        3 -> binding.llTabBenefits.visibility = View.VISIBLE
                        4 -> binding.llTabUsage.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Hiển thị dữ liệu sản phẩm nếu có
        product?.let { displayProduct(it) }
    }

    override fun observeViewModel() {
        super.observeViewModel()
        lifecycleScope.launch {
            val db = RootieDatabase.getDatabase(requireContext())
            db.cartDao().getAllCartItems().collect { items ->
                val totalQty = items.sumOf { it.quantity }
                if (totalQty > 0) {
                    binding.tvCartBadge.visibility = View.VISIBLE
                    binding.tvCartBadge.text = totalQty.toString()
                } else {
                    binding.tvCartBadge.visibility = View.GONE
                }
            }
        }
    }

    fun setProduct(product: ProductEntity) {
        this.product = product
        if (_binding != null) {
            displayProduct(product)
        }
    }

    private fun displayProduct(product: ProductEntity) {
        binding.tvProductName.text = product.name
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        binding.tvPrice.text = formatter.format(product.price)
        
        // Tạm thời giả định giá gốc cao hơn 20%
        val originalPrice = (product.price * 1.2).toLong()
        binding.tvOriginalPrice.text = formatter.format(originalPrice)
        binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        // 1. Setup carousel
        val albumList = if (product.album.isNotEmpty()) product.album else listOf(product.mainImage)
        binding.vpProductImage.adapter = ProductImageAdapter(albumList)
        setupIndicators(albumList.size)

        // Reset selected tab to the first one (Thông tin)
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        binding.llTabInfo.visibility = View.VISIBLE
        binding.llTabIngredients.visibility = View.GONE
        binding.llTabStory.visibility = View.GONE
        binding.llTabBenefits.visibility = View.GONE
        binding.llTabUsage.visibility = View.GONE

        // 2. Bind Tab 1: Thông tin
        binding.tvDescription.text = product.description
        binding.tvSuitableFor.text = product.suitableFor
        binding.tvMainIngredientsSummary.text = product.mainIngredientsSummary
        
        if (product.allergyInformation.isNotEmpty()) {
            binding.cvAllergy.visibility = View.VISIBLE
            binding.tvAllergyInformation.text = product.allergyInformation
        } else {
            binding.cvAllergy.visibility = View.VISIBLE
            binding.tvAllergyInformation.text = "Sản phẩm chứa thành phần tự nhiên lành tính. Thử trên vùng da nhỏ trước khi sử dụng nếu bạn có cơ địa nhạy cảm."
        }

        // 3. Bind Tab 2: Thành phần
        binding.llKeyIngredientsContainer.removeAllViews()
        if (product.keyIngredients.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "Chưa cập nhật thông tin thành phần nổi bật."
                setTextColor(Color.parseColor("#888888"))
                textSize = 14f
                setPadding(0, 8, 0, 8)
            }
            binding.llKeyIngredientsContainer.addView(emptyTv)
        } else {
            product.keyIngredients.forEach { ingredient ->
                val titleTv = TextView(requireContext()).apply {
                    text = "• ${ingredient.name}"
                    setTextColor(Color.parseColor("#333333"))
                    setTypeface(null, Typeface.BOLD)
                    textSize = 14f
                    setPadding(0, 8, 0, 4)
                }
                val descTv = TextView(requireContext()).apply {
                    text = ingredient.description
                    setTextColor(Color.parseColor("#666666"))
                    textSize = 13f
                    setPadding(16, 0, 0, 8)
                }
                binding.llKeyIngredientsContainer.addView(titleTv)
                binding.llKeyIngredientsContainer.addView(descTv)
            }
        }

        if (product.detailedIngredients.isNotEmpty()) {
            binding.tvDetailedIngredients.text = product.detailedIngredients.joinToString(", ")
        } else {
            binding.tvDetailedIngredients.text = "Chưa cập nhật bảng thành phần đầy đủ."
        }

        // 4. Bind Tab 3: Câu chuyện
        if (product.storyDescription.isNotEmpty()) {
            binding.tvStoryDescription.text = product.storyDescription
            if (product.storyImage.isNotEmpty()) {
                binding.cvStoryImage.visibility = View.VISIBLE
                binding.ivStoryImage.load(product.storyImage) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                }
            } else {
                binding.cvStoryImage.visibility = View.GONE
            }
        } else {
            binding.tvStoryDescription.text = "Câu chuyện về sản phẩm đang được cập nhật."
            binding.cvStoryImage.visibility = View.GONE
        }

        // 5. Bind Tab 4: Công dụng
        binding.llIdealForContainer.removeAllViews()
        if (product.idealFor.isEmpty()) {
            binding.tvIdealForHeader.visibility = View.GONE
        } else {
            binding.tvIdealForHeader.visibility = View.VISIBLE
            product.idealFor.forEach { ideal ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                val icon = ImageView(requireContext()).apply {
                    val sizePx = (16 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                        marginEnd = (8 * resources.displayMetrics.density).toInt()
                    }
                    setImageResource(com.veganbeauty.app.R.drawable.ic_check)
                    setColorFilter(Color.parseColor("#455B49"))
                }
                val text = TextView(requireContext()).apply {
                    this.text = ideal
                    setTextColor(Color.parseColor("#444444"))
                    textSize = 14f
                }
                row.addView(icon)
                row.addView(text)
                binding.llIdealForContainer.addView(row)
            }
        }

        binding.llBenefitsContainer.removeAllViews()
        if (product.benefits.isEmpty()) {
            binding.tvBenefitsHeader.visibility = View.GONE
        } else {
            binding.tvBenefitsHeader.visibility = View.VISIBLE
            product.benefits.forEach { benefit ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
                val icon = ImageView(requireContext()).apply {
                    val sizePx = (16 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                        marginEnd = (8 * resources.displayMetrics.density).toInt()
                    }
                    setImageResource(com.veganbeauty.app.R.drawable.ic_check)
                    setColorFilter(Color.parseColor("#455B49"))
                }
                val text = TextView(requireContext()).apply {
                    this.text = benefit
                    setTextColor(Color.parseColor("#444444"))
                    textSize = 14f
                }
                row.addView(icon)
                row.addView(text)
                binding.llBenefitsContainer.addView(row)
            }
        }

        // 6. Bind Tab 5: Cách sử dụng
        if (product.usage.isNotEmpty()) {
            binding.tvUsage.text = product.usage
        } else {
            binding.tvUsage.text = "Chưa cập nhật hướng dẫn sử dụng sản phẩm."
        }

        if (product.usageAmount.isNotEmpty()) {
            binding.cvUsageAmount.visibility = View.VISIBLE
            binding.tvUsageAmount.text = product.usageAmount
        } else {
            binding.cvUsageAmount.visibility = View.GONE
        }

        if (product.scent.isNotEmpty()) {
            binding.cvScent.visibility = View.VISIBLE
            binding.tvScent.text = product.scent
        } else {
            binding.cvScent.visibility = View.GONE
        }

        if (product.notes.isNotEmpty()) {
            binding.cvNotes.visibility = View.VISIBLE
            binding.tvNotes.text = product.notes
        } else {
            binding.cvNotes.visibility = View.GONE
        }

        checkProductCompatibility(product)
    }

    private fun checkProductCompatibility(product: ProductEntity) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
        val hasQuiz = prefs.contains("SAVED_USER_SKIN_TYPE")
        
        if (!hasQuiz) {
            binding.cvSkinCompatibility.visibility = View.VISIBLE
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#FEFBF4"))
            binding.cvSkinCompatibility.strokeColor = Color.parseColor("#DDDFC4")
            binding.ivCompatibilityIcon.setImageResource(com.veganbeauty.app.R.drawable.quiz_ic_sparkles)
            binding.ivCompatibilityIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#677559"))
            binding.tvCompatibilityTitle.text = "Kiểm tra độ phù hợp làn da"
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#677559"))
            binding.tvCompatibilitySubtitle.text = "Làm quiz test da để nhận phân tích chi tiết độ phù hợp của sản phẩm này."
            binding.flIrritatingPills.visibility = View.GONE
            binding.vCompatibilityDivider.visibility = View.GONE
            binding.llCompatibilityReasons.visibility = View.GONE
            binding.cvSkinCompatibility.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.quiz.QuizTestIntroFragment())
                    .addToBackStack(null)
                    .commit()
            }
            return
        }

        binding.cvSkinCompatibility.setOnClickListener(null)
        val flaggedSet = prefs.getStringSet("SAVED_FLAGGED_GROUPS", emptySet()) ?: emptySet()

        val avoidChemicals = mutableSetOf<String>()
        val cautionChemicals = mutableSetOf<String>()

        try {
            val tpString = ctx.assets.open("quiz_thanhphan.json").bufferedReader().use { it.readText() }
            val tpObject = JSONObject(tpString)
            val tpArray = tpObject.getJSONArray("ingredients")

            for (i in 0 until tpArray.length()) {
                val ing = tpArray.getJSONObject(i)
                val name = ing.getString("name")
                val category = ing.getString("category")
                val risk = ing.getString("risk")

                if (flaggedSet.contains(category)) {
                    if (risk == "avoid") {
                        avoidChemicals.add(name.lowercase())
                    } else if (risk == "caution") {
                        cautionChemicals.add(name.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val detailedIngredientsList = product.detailedIngredients.map { it.lowercase() }
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

        binding.cvSkinCompatibility.visibility = View.VISIBLE

        if (triggeredAvoids.isNotEmpty() || triggeredCautions.isNotEmpty()) {
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#FFF2DF"))
            binding.cvSkinCompatibility.strokeColor = Color.parseColor("#D2945D")
            binding.ivCompatibilityIcon.setImageResource(com.veganbeauty.app.R.drawable.quiz_ic_warning_triangle)
            binding.ivCompatibilityIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FE851A"))
            
            binding.tvCompatibilityTitle.text = "Cảnh báo thành phần không phù hợp"
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#FE851A"))
            
            val totalCount = triggeredAvoids.distinct().size + triggeredCautions.distinct().size
            binding.tvCompatibilitySubtitle.text = "Sản phẩm chứa $totalCount thành phần gây kích ứng cho làn da của bạn."

            binding.flIrritatingPills.visibility = View.VISIBLE
            binding.flIrritatingPills.removeAllViews()
            
            (triggeredAvoids.distinct() + triggeredCautions.distinct()).forEach { name ->
                val pillView = LayoutInflater.from(ctx).inflate(com.veganbeauty.app.R.layout.quiz_item_pill, binding.flIrritatingPills, false) as TextView
                pillView.text = name
                pillView.setBackgroundResource(com.veganbeauty.app.R.drawable.quiz_bg_pill_avoid)
                pillView.setTextColor(Color.parseColor("#EB862D"))
                binding.flIrritatingPills.addView(pillView)
            }

            binding.vCompatibilityDivider.visibility = View.VISIBLE
            binding.llCompatibilityReasons.visibility = View.VISIBLE

            val avoidCount = triggeredAvoids.distinct().size
            if (avoidCount > 0) {
                binding.llReason1.visibility = View.VISIBLE
                binding.tvReason1Title.text = "$avoidCount thành phần có nguy cơ kích ứng cao"
                binding.tvReason1Desc.text = "Dựa trên hồ sơ dị ứng và biểu hiện da của bạn."
            } else {
                binding.llReason1.visibility = View.GONE
            }

            val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "") ?: ""
            if (skinType.lowercase().contains("nhạy cảm")) {
                binding.llReason2.visibility = View.VISIBLE
                binding.tvReason2Title.text = "Làn da đang nhạy cảm"
                binding.tvReason2Desc.text = "Hồ sơ da của bạn cho thấy hàng rào bảo vệ da đang yếu."
            } else {
                binding.llReason2.visibility = View.GONE
            }
        } else {
            binding.cvSkinCompatibility.setCardBackgroundColor(Color.parseColor("#EDF3ED"))
            binding.cvSkinCompatibility.strokeColor = Color.parseColor("#A2B5A2")
            binding.ivCompatibilityIcon.setImageResource(com.veganbeauty.app.R.drawable.quiz_ic_wavy_check)
            binding.ivCompatibilityIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#12B76A"))
            
            binding.tvCompatibilityTitle.text = "Sản phẩm phù hợp với da của bạn"
            binding.tvCompatibilityTitle.setTextColor(Color.parseColor("#67814D"))
            binding.tvCompatibilitySubtitle.text = "Bảng thành phần cực kỳ lành tính và hoàn toàn phù hợp với nền da hiện tại."
            
            binding.flIrritatingPills.visibility = View.GONE
            binding.vCompatibilityDivider.visibility = View.GONE
            binding.llCompatibilityReasons.visibility = View.GONE
        }
    }

    private fun getViName(chemicalName: String): String {
        return when (chemicalName.lowercase()) {
            "alcohol denat", "ethanol" -> "Alcohol"
            "fragrance" -> "Fragrance"
            "parfum" -> "Parfum"
            "essential oil" -> "Tinh dầu"
            "sodium lauryl sulfate" -> "SLS"
            "sodium laureth sulfate" -> "Sulfate"
            "cocamidopropyl betaine" -> "Cocamidopropyl"
            "retinol" -> "Retinol"
            "salicylic acid" -> "BHA"
            "glycolic acid" -> "AHA"
            "phenoxyethanol", "paraben" -> "Paraben"
            "propylene glycol" -> "Propylene Glycol"
            "glycerin" -> "Glycerin"
            "hyaluronic acid" -> "HA"
            "centella asiatica" -> "Rau má"
            "green tea" -> "Trà xanh"
            "aloe vera" -> "Nha đam"
            "silicone" -> "Silicone"
            "petrolatum" -> "Petrolatum"
            else -> chemicalName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    private fun setupIndicators(count: Int) {
        binding.llIndicatorContainer.removeAllViews()
        if (count <= 1) return
        val indicators = ArrayList<ImageView>()

        for (i in 0 until count) {
            val size = if (i == 0) 8 else 6 // in dp
            val sizePx = (size * resources.displayMetrics.density).toInt()
            val layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                setMargins(4, 0, 4, 0)
            }
            val imageView = ImageView(requireContext()).apply {
                setImageResource(if (i == 0) com.veganbeauty.app.R.drawable.bg_circle_green else com.veganbeauty.app.R.drawable.bg_circle_grey)
            }
            indicators.add(imageView)
            binding.llIndicatorContainer.addView(imageView, layoutParams)
        }

        binding.vpProductImage.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                for (i in 0 until count) {
                    val size = if (i == position) 8 else 6 // in dp
                    val sizePx = (size * resources.displayMetrics.density).toInt()
                    indicators[i].layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                        setMargins(4, 0, 4, 0)
                    }
                    indicators[i].setImageResource(if (i == position) com.veganbeauty.app.R.drawable.bg_circle_green else com.veganbeauty.app.R.drawable.bg_circle_grey)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MockAdapter(private val layoutId: Int, private val itemCountToReturn: Int) : RecyclerView.Adapter<MockAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        override fun getItemCount() = itemCountToReturn
    }

    inner class ProductImageAdapter(private val images: List<String>) : RecyclerView.Adapter<ProductImageAdapter.ViewHolder>() {
        inner class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val imageView = LayoutInflater.from(parent.context).inflate(com.veganbeauty.app.R.layout.shop_item_product_image, parent, false) as ImageView
            return ViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.imageView.load(images[position]) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }

        override fun getItemCount(): Int = images.size
    }
}
