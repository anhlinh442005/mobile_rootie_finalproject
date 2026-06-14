package com.veganbeauty.app.features.home

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.HomeFragmentBinding
import com.veganbeauty.app.features.home.adapter.HomeBannerAdapter
import com.veganbeauty.app.features.home.adapter.HomeBannerItem
import com.veganbeauty.app.features.home.adapter.HomeBestsellerAdapter
import com.veganbeauty.app.features.home.adapter.HomeCategoryAdapter
import com.veganbeauty.app.features.home.adapter.HomeCategoryItem
import com.veganbeauty.app.features.home.adapter.HomeProductCardAdapter
import com.veganbeauty.app.features.home.adapter.HomeTopSearchAdapter
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListFragment

class HomeFragment : RootieFragment() {

  private var _binding: HomeFragmentBinding? = null
  private val binding get() = _binding!!

  private lateinit var viewModel: ShopViewModel

  private val recentAdapter = HomeProductCardAdapter(onItemClick = ::openProductDetail)
  private val recommendationsAdapter = HomeProductCardAdapter(onItemClick = ::openProductDetail)
  private val bestsellerAdapter = HomeBestsellerAdapter(onItemClick = ::openProductDetail)
  private val topSearchAdapter = HomeTopSearchAdapter(onItemClick = ::openProductDetail)
  private val categoryAdapter = HomeCategoryAdapter()
  private val bannerAdapter = HomeBannerAdapter()

  private var allProducts: List<ProductEntity> = emptyList()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = HomeFragmentBinding.inflate(inflater, container, false)
    setupViewModel()
    return binding.root
  }

  private fun setupViewModel() {
    val db =
        RootieDatabase.getDatabase(requireContext())
    val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))

    viewModel =
        ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                  @Suppress("UNCHECKED_CAST")
                  override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShopViewModel(repository) as T
                  }
                })[ShopViewModel::class.java]
  }

  override fun setupUI(view: View) {
    setupBanner()
    setupRecyclerViews()
    setupHeaderActions()
    setupBottomNav()
    setupPromoClicks()
    binding.tvHomeSloganMarquee.isSelected = true

    binding.cardHomeRoutineWidget.setOnClickListener {
      parentFragmentManager.beginTransaction()
          .replace(R.id.main_container, com.veganbeauty.app.features.routine.SkinReminderFragment())
          .addToBackStack(null)
          .commit()
    }
    setupStreakWidget()
    setupQuizReminder()
  }

  private fun setupBanner() {
    val bannerImages =
        listOf(
            R.drawable.imv_banner_jp,
            R.drawable.imv_banner_au,
            R.drawable.imv_banner_songxanh,
            R.drawable.imv_banner_taiwan)

    val banners = bannerImages.map { HomeBannerItem(imageRes = it) }

    bannerAdapter.submitBanners(banners)
    binding.vpBanner.adapter = bannerAdapter
    setupBannerDots(banners.size)

    binding.vpBanner.registerOnPageChangeCallback(
        object : ViewPager2.OnPageChangeCallback() {
          override fun onPageSelected(position: Int) {
            updateBannerDots(position)
          }
        })
  }

  private fun setupBannerDots(count: Int) {
    binding.llBannerDots.removeAllViews()
    repeat(count) {
      val dot = View(requireContext())
      val size = resources.getDimensionPixelSize(R.dimen.home_banner_dot_size)
      val params =
          LinearLayout.LayoutParams(size, size).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.home_banner_dot_margin)
          }
      dot.layoutParams = params
      dot.background = createDotDrawable(false)
      binding.llBannerDots.addView(dot)
    }
    updateBannerDots(0)
  }

  private fun updateBannerDots(activeIndex: Int) {
    for (i in 0 until binding.llBannerDots.childCount) {
      binding.llBannerDots.getChildAt(i).background = createDotDrawable(i == activeIndex)
    }
  }

  private fun createDotDrawable(active: Boolean): GradientDrawable {
    return GradientDrawable().apply {
      shape = GradientDrawable.OVAL
      setColor(if (active) 0xFFFFFFFF.toInt() else 0x88FFFFFF.toInt())
    }
  }

  private fun setupRecyclerViews() {
    binding.rvRecentActivity.apply {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = recentAdapter
    }
    binding.rvRecommendations.apply {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = recommendationsAdapter
    }
    binding.rvBestsellers.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = bestsellerAdapter
      isNestedScrollingEnabled = false
    }
    binding.rvTopSearch.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = topSearchAdapter
      isNestedScrollingEnabled = false
    }
    binding.rvCategories.apply {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = categoryAdapter
    }
  }

  private fun setupHeaderActions() {
    binding.root.findViewById<View>(R.id.home_header_search_bar).setOnClickListener {
      Toast.makeText(context, "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
    }
    binding.root.findViewById<View>(R.id.home_header_qr_btn).setOnClickListener {
      Toast.makeText(context, "Mở trình quét mã QR", Toast.LENGTH_SHORT).show()
    }
    binding.root.findViewById<View>(R.id.home_header_notification_btn).setOnClickListener {
      Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
    }

    binding.tvRecentSeeAll.setOnClickListener { openShop() }
    binding.tvCategoriesSeeAll.setOnClickListener { openShop() }
    binding.tvRecommendationsSeeAll.setOnClickListener { openShop() }
  }

  private fun setupPromoClicks() {
    binding.btnPromoSunscreen.setOnClickListener { openFeaturedProduct("chống nắng", "bí đao") }
    binding.btnPromoLotus.setOnClickListener { openFeaturedProduct("sen", "tinh chất") }
  }

  private fun setupBottomNav() {
    BottomNavHelper.setup(fragment = this, root = binding.root, activeTabId = R.id.nav_home) {
        tabId ->
      BottomNavHelper.navigate(this, tabId)
    }
  }

  override fun observeViewModel() {
    viewModel.products.observe(viewLifecycleOwner) { products ->
      allProducts = products
      bindProductSections(products)
      bindPromoImages(products)
    }
  }

  private fun bindProductSections(products: List<ProductEntity>) {
    if (products.isEmpty()) return

    recentAdapter.submitList(products.take(8))
    recommendationsAdapter.submitList(products.takeLast(8).reversed())

    bestsellerAdapter.submitList(products.sortedByDescending { it.price }.take(3))
    topSearchAdapter.submitList(products.take(3))

    val categories =
        products
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .take(6)
            .map { HomeCategoryItem(it) }
    categoryAdapter.submitList(categories)
  }

  private fun bindPromoImages(products: List<ProductEntity>) {
    findProduct(products, "chống nắng", "bí đao")?.let { product ->
      binding.ivPromoSunscreen.load(product.mainImage) {
        crossfade(true)
        placeholder(R.drawable.myphamxanh)
      }
    }

    findProduct(products, "sen", "tinh chất")?.let { product ->
      binding.ivPromoLotus.load(product.mainImage) {
        crossfade(true)
        placeholder(R.drawable.myphamxanh)
      }
    }
  }

  private fun findProduct(
      products: List<ProductEntity>,
      vararg keywords: String
  ): ProductEntity? {
    return products.firstOrNull { product ->
      val name = product.name.lowercase()
      keywords.all { name.contains(it.lowercase()) }
    }
  }

  private fun openFeaturedProduct(vararg keywords: String) {
    val product = findProduct(allProducts, *keywords)
    if (product != null) {
      openProductDetail(product)
    } else {
      openShop()
    }
  }

  private fun openProductDetail(product: ProductEntity) {
    val detailFragment = ShopDetailFragment()
    detailFragment.setProduct(product)
    parentFragmentManager
        .beginTransaction()
        .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out)
        .replace(R.id.main_container, detailFragment)
        .addToBackStack(null)
        .commit()
  }

  private fun openShop() {
    parentFragmentManager
        .beginTransaction()
        .replace(R.id.main_container, ShopListFragment())
        .commit()
  }

  private fun setupStreakWidget() {
    val ctx = context ?: return
    val currentStreak = com.veganbeauty.app.data.local.ProfileSession.getSkinStreak(ctx)
    binding.tvHomeStreakCount.text = "Chuỗi chăm da của bạn: $currentStreak ngày 🔥"

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val todayStr = sdf.format(java.util.Date())
    val hasCompletedMorningToday = com.veganbeauty.app.data.local.ProfileSession.isMorningRewardAwarded(ctx, todayStr)
    val hasCompletedEveningToday = com.veganbeauty.app.data.local.ProfileSession.isEveningRewardAwarded(ctx, todayStr)

    val descText = when {
        hasCompletedMorningToday && hasCompletedEveningToday -> "Tuyệt vời! Bạn đã hoàn thành tất cả routine hôm nay! 🎉"
        hasCompletedMorningToday -> "Bạn đã hoàn thành Routine Sáng! Đừng quên Routine Tối nhé! 🌙"
        hasCompletedEveningToday -> "Bạn đã hoàn thành Routine Tối! Đừng quên Routine Sáng ngày mai nhé! ☀️"
        else -> "Hôm nay bạn chưa hoàn thành Routine nào, bấm để bắt đầu chăm da nhé! ✨"
    }
    binding.tvHomeStreakDesc.text = descText
  }

  private fun setupQuizReminder() {
    val context = context ?: return
    val lastTestTime = com.veganbeauty.app.data.local.ProfileSession.getLastSkinTestTime(context)
    val isDismissed = com.veganbeauty.app.data.local.ProfileSession.isQuizReminderDismissedWeekly(context)
    val currentTime = System.currentTimeMillis()
    val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
    
    val needsTest = lastTestTime != 0L && (currentTime - lastTestTime >= sevenDaysMs)
    
    if (needsTest) {
        // Reset the dismiss flag because a new week has started
        if (currentTime - lastTestTime >= sevenDaysMs && lastTestTime != 0L && isDismissed) {
            com.veganbeauty.app.data.local.ProfileSession.setQuizReminderDismissedWeekly(context, false)
        }
        
        // Show Home Widget (if not dismissed)
        val shouldShowWidget = !com.veganbeauty.app.data.local.ProfileSession.isQuizReminderDismissedWeekly(context)
        if (shouldShowWidget) {
            binding.quizTestWeeklyReminderLayout.root.visibility = View.VISIBLE
            binding.quizTestWeeklyReminderLayout.root.setOnClickListener {
                navigateToQuizIntro()
            }
            binding.quizTestWeeklyReminderLayout.quizTestBtnDismissReminder.setOnClickListener {
                com.veganbeauty.app.data.local.ProfileSession.setQuizReminderDismissedWeekly(context, true)
                binding.quizTestWeeklyReminderLayout.root.visibility = View.GONE
                Toast.makeText(context, "Đã ẩn nhắc nhở tuần này", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.quizTestWeeklyReminderLayout.root.visibility = View.GONE
        }
        
        // Show Popup Dialog once per app session (Stunning visual design, no default black/white theme background)
        if (!hasShownQuizPopupThisSession) {
            hasShownQuizPopupThisSession = true
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quiz_test_weekly_reminder, null)
            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create()
            
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            dialogView.findViewById<View>(R.id.btn_dialog_confirm).setOnClickListener {
                dialog.dismiss()
                navigateToQuizIntro()
            }
            dialogView.findViewById<View>(R.id.btn_dialog_cancel).setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    } else {
        binding.quizTestWeeklyReminderLayout.root.visibility = View.GONE
    }
  }

  private fun navigateToQuizIntro() {
      parentFragmentManager.beginTransaction()
          .replace(R.id.main_container, com.veganbeauty.app.features.quiz.QuizTestIntroFragment())
          .addToBackStack(null)
          .commit()
  }

  override fun onResume() {
      super.onResume()
      setupStreakWidget()
      setupQuizReminder()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  companion object {
      private var hasShownQuizPopupThisSession = false
  }
}

