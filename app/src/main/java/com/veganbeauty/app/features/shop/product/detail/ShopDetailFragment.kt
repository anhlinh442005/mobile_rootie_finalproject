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
import com.veganbeauty.app.features.shop.store.ShopStoreSelectionFragment
import com.veganbeauty.app.features.shop.store.ShopStoreSystemFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShopDetailFragment : RootieFragment() {

    private var _binding: ShopProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: ProductEntity? = null
    private var cartVoucherCode: String? = null
    private var cartVoucherDiscount = 0L

    private lateinit var handbookAdapter: com.veganbeauty.app.features.community.beauty_hub.NotebookVideoAdapter
    private lateinit var reviewAdapter: ShopReviewAdapter
    private lateinit var relatedAdapter: ShopHorizontalProductAdapter
    private lateinit var recentlyViewedAdapter: ShopHorizontalProductAdapter

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

        // Setup real adapters for dynamic data
        handbookAdapter = com.veganbeauty.app.features.community.beauty_hub.NotebookVideoAdapter(emptyList())
        binding.rvHandbook.adapter = handbookAdapter

        reviewAdapter = ShopReviewAdapter(emptyList())
        binding.rvReviews.adapter = reviewAdapter

        relatedAdapter = ShopHorizontalProductAdapter(
            emptyList(),
            onItemClick = { prod -> navigateToProduct(prod) },
            onAddToCartClick = { prod -> addToCart(prod) }
        )
        binding.rvRelatedProducts.adapter = relatedAdapter

        recentlyViewedAdapter = ShopHorizontalProductAdapter(
            emptyList(),
            onItemClick = { prod -> navigateToProduct(prod) },
            onAddToCartClick = { prod -> addToCart(prod) }
        )
        binding.rvRecentlyViewed.adapter = recentlyViewedAdapter

        binding.btnAllReviews.setOnClickListener {
            product?.let { p ->
                val bottomSheet = ProductReviewsBottomSheet.newInstance(p.id, p.name, p.category)
                bottomSheet.show(parentFragmentManager, ProductReviewsBottomSheet.TAG)
            }
        }

        binding.btnSearch.setOnClickListener {
            val searchFragment = com.veganbeauty.app.features.shop.search.ShopSearchFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnCart.setOnClickListener {
            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                cartVoucherCode,
                cartVoucherDiscount
            )
            cartSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG)
        }

        // Voucher result listener to re-open cart
        parentFragmentManager.setFragmentResultListener(
            com.veganbeauty.app.features.shop.product.ShopVoucherFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val code = bundle.getString(com.veganbeauty.app.features.shop.product.ShopVoucherFragment.RESULT_VOUCHER_CODE)
            val discount = bundle.getLong(com.veganbeauty.app.features.shop.product.ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L)
            cartVoucherCode = code
            cartVoucherDiscount = discount

            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                cartVoucherCode,
                cartVoucherDiscount
            )
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

        binding.btnFindStore.setOnClickListener {
            val storeFragment = ShopStoreSystemFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, storeFragment)
                .addToBackStack(null)
                .commit()
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
        viewLifecycleOwner.lifecycleScope.launch {
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

        // 7. Load review statistics & reviews list
        val (averageRating, totalReviews) = ProductReviewHelper.getRatingStats(product.id)
        binding.tvRatingValue.text = String.format(java.util.Locale.US, "%.1f", averageRating)
        binding.tvReviewsCount.text = "($totalReviews reviews)"

        val reviews = ProductReviewHelper.getReviews(product.id, product.name, product.category)
        reviewAdapter.updateData(reviews.take(3)) // Show only 3 reviews on product details page

        // 8. Add to Recently Viewed Products in SharedPreferences
        val sharedPrefs = requireContext().getSharedPreferences("rootie_prefs", android.content.Context.MODE_PRIVATE)
        val recentlyViewedStr = sharedPrefs.getString("recently_viewed_ids", "") ?: ""
        val idList = recentlyViewedStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        idList.remove(product.id)
        idList.add(0, product.id)
        val limitedList = idList.take(10)
        sharedPrefs.edit().putString("recently_viewed_ids", limitedList.joinToString(",")).apply()

        // 9. Load Related Products and Recently Viewed Products
        lifecycleScope.launch {
            val db = RootieDatabase.getDatabase(requireContext())
            db.productDao().getAllProducts().collect { allProducts ->
                // -- Related Products Logic --
                val otherProducts = allProducts.filter { it.id != product.id }
                val currentSubcategories = product.categoryIds.split(",").filter { it.isNotEmpty() }
                
                val subcategoryIds = listOf(
                    "f5877af6a55f88bcf57c17b4", "389971929086b2ce7fba9dd0", "36cbf3f5c4b7a299ce2a2d0c",
                    "4e20d6bbc1203015ee2ecd48", "b1b6cd208332d4f1e015a26c", "7667d982515426a9d88b787b",
                    "bb88a3306cf95af20d073594", "9882d5fa14c74dd053e17f33", "c211afa24702f5d1ff86fe42",
                    "7c70e845e829b374e57ee7b1", "b703bb813e660aa88076ee5a", "8fce5340c618672aa1ae7fb3",
                    "24a75aa9d541feed638b1970", "755731e01d8c579c633ae4d2", "ded17e0716783c133b1a5b9a"
                )
                
                val productSubcategories = currentSubcategories.filter { it in subcategoryIds }
                val productParentCategories = currentSubcategories.filter { it !in subcategoryIds }
                
                val subcategoryMatches = otherProducts.filter { other ->
                    val otherIds = other.categoryIds.split(",")
                    otherIds.any { it in productSubcategories }
                }
                
                val parentMatches = otherProducts.filter { other ->
                    other !in subcategoryMatches &&
                    other.categoryIds.split(",").any { it in productParentCategories }
                }
                
                val generalCategoryMatches = otherProducts.filter { other ->
                    other !in subcategoryMatches &&
                    other !in parentMatches &&
                    other.category.equals(product.category, ignoreCase = true)
                }
                
                val finalRelated = (subcategoryMatches + parentMatches + generalCategoryMatches).take(8)
                relatedAdapter.updateData(finalRelated)

                // -- Recently Viewed Products Logic --
                val currentViewedStr = sharedPrefs.getString("recently_viewed_ids", "") ?: ""
                val currentIds = currentViewedStr.split(",").filter { it.isNotEmpty() }
                val viewedProducts = currentIds
                    .filter { it != product.id }
                    .mapNotNull { id -> allProducts.find { it.id == id } }
                
                recentlyViewedAdapter.updateData(viewedProducts)
            }
        }

        // 10. Load Related Handbooks (Videos)
        lifecycleScope.launch {
            val db = RootieDatabase.getDatabase(requireContext())
            db.communityDao().getAllExploreVideos().collect { allVideos ->
                val videos = if (allVideos.isEmpty()) {
                    val localReader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
                    val localVideos = localReader.getExploreVideos()
                    if (localVideos.isNotEmpty()) {
                        db.communityDao().insertExploreVideos(localVideos)
                    }
                    localVideos
                } else {
                    allVideos
                }
                
                val filteredVideos = videos.filter { video ->
                    video.type.contains("notebook", ignoreCase = true)
                }
                
                val rankedVideos = filteredVideos.map { video ->
                    var score = 0
                    val textToSearch = (video.title + " " + video.description).lowercase()
                    
                    val ingredients = listOf("bí đao", "nghệ", "cà phê", "bưởi", "hoa hồng", "dừa", "tràm trà", "sen", "rau má", "bồ kết")
                    for (ing in ingredients) {
                        if (product.name.lowercase().contains(ing)) {
                            if (textToSearch.contains(ing)) {
                                score += 10
                            }
                            if (ing == "bí đao" && (textToSearch.contains("mụn") || textToSearch.contains("thâm"))) score += 5
                            if (ing == "nghệ" && (textToSearch.contains("sáng da") || textToSearch.contains("thâm") || textToSearch.contains("curcumin"))) score += 5
                            if (ing == "bưởi" && (textToSearch.contains("tóc") || textToSearch.contains("rụng") || textToSearch.contains("gội"))) score += 5
                            if (ing == "cà phê" && (textToSearch.contains("tẩy tế bào chết") || textToSearch.contains("body") || textToSearch.contains("scrub"))) score += 5
                        }
                    }
                    
                    if (product.category.lowercase().contains("da") || product.categoryIds.contains("7176b5e7966be88daf95cfd4")) {
                        if (textToSearch.contains("skincare") || textToSearch.contains("da") || textToSearch.contains("mặt")) {
                            score += 2
                        }
                    }
                    
                    Pair(video, score)
                }.sortedByDescending { it.second }
                
                val finalVideos = rankedVideos.map { it.first }.take(4)
                handbookAdapter.updateData(finalVideos)
            }
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

    private fun navigateToProduct(prod: ProductEntity) {
        val detailFragment = ShopDetailFragment().apply {
            setProduct(prod)
        }
        parentFragmentManager.beginTransaction()
            .replace(com.veganbeauty.app.R.id.main_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun addToCart(prod: ProductEntity) {
        com.veganbeauty.app.features.shop.product.CartHelper.addToCart(requireContext(), lifecycleScope, prod, 1)
    }
}
