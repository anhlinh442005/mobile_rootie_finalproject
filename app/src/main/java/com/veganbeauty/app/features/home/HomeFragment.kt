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
import androidx.lifecycle.lifecycleScope
import com.veganbeauty.app.features.shop.product.CartHelper
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
import com.veganbeauty.app.features.home.adapter.HomeProductGridAdapter
import com.veganbeauty.app.features.home.adapter.HomeFlashsaleAdapter
import com.veganbeauty.app.features.home.adapter.HomeTopSearchAdapter
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListFragment
import java.util.Calendar

class HomeFragment : RootieFragment() {

  private var _binding: HomeFragmentBinding? = null
  private val binding get() = _binding!!

  private lateinit var viewModel: ShopViewModel

  private val recentAdapter = HomeProductCardAdapter(onItemClick = ::openProductDetail, onAddToCart = ::addToCart)
  private val recommendationsAdapter = HomeProductGridAdapter(onItemClick = ::openProductDetail, onAddToCart = ::addToCart)
  private val bestsellerAdapter = HomeBestsellerAdapter(onItemClick = ::openProductDetail, onAddToCart = ::addToCart)
  private val topSearchAdapter = HomeTopSearchAdapter(onItemClick = ::openProductDetail)
  private val flashSaleAdapter = HomeFlashsaleAdapter(onItemClick = ::openProductDetail, onAddToCart = ::addToCart)
  private val categoryAdapter = HomeCategoryAdapter()
  private val bannerAdapter = HomeBannerAdapter()
  private val shortcutAdapter = com.veganbeauty.app.features.home.adapter.HomeShortcutAdapter()
  private var isShortcutsExpanded = false

  private fun navigateToFragment(fragment: androidx.fragment.app.Fragment) {
      parentFragmentManager.beginTransaction()
          .setCustomAnimations(
              android.R.anim.fade_in,
              android.R.anim.fade_out,
              android.R.anim.fade_in,
              android.R.anim.fade_out
          )
          .replace(R.id.main_container, fragment)
          .addToBackStack(null)
          .commit()
  }

  private fun navigateIfLoggedIn(fragmentProvider: () -> androidx.fragment.app.Fragment) {
      if (!com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(requireContext())) {
          com.veganbeauty.app.features.home.BottomNavHelper.showLoginRequiredDialog(requireContext())
          return
      }
      navigateToFragment(fragmentProvider())
  }

  private val allShortcuts by lazy {
      listOf(
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Quét sản phẩm", R.drawable.ic_qrscan) { navigateToFragment(com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Soi Da AI", R.drawable.ic_ai_outline) { navigateToFragment(com.veganbeauty.app.features.myskin.SkinScanFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Đặt lịch soi da", R.drawable.ic_calendar_outline) { navigateToFragment(com.veganbeauty.app.features.myskin.ChooseBranchFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Routine của tôi", R.drawable.ic_skincare) { navigateToFragment(com.veganbeauty.app.features.myskin.MySkinFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Dự báo da hôm nay", R.drawable.ic_water_drop_outline) { navigateIfLoggedIn { com.veganbeauty.app.features.weather.WeatherForecastFragment() } },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Kiểm tra dị ứng", R.drawable.ic_shield_outline) { navigateToFragment(com.veganbeauty.app.features.quiz.QuizTestIntroFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Hồ sơ làn da", R.drawable.ic_clipboard_outline) { navigateIfLoggedIn { com.veganbeauty.app.features.myskin.SkinHistoryFragment() } },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Nhắc chăm da", R.drawable.ic_bell) { navigateIfLoggedIn { com.veganbeauty.app.features.routine.SkinReminderFragment() } },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Đổi quà Rootie Xu", R.drawable.ic_gift) { navigateToFragment(com.veganbeauty.app.features.account.reward.AccountRewardFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Cửa hàng gần bạn", R.drawable.ic_store_outline) { navigateToFragment(com.veganbeauty.app.features.shop.store.ShopStoreSystemFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Beauty Explore", R.drawable.ic_reel_outline) { navigateToFragment(com.veganbeauty.app.features.community.com_feed.ComLoadingFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Tra cứu thành phần", R.drawable.ic_shortcut_ingredient) { Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() }
      )
  }

  private var flashSaleTimer: android.os.CountDownTimer? = null
  private var allProducts: List<ProductEntity> = emptyList()
  private var recommendationLimit = 6
  private var allRecommendationProducts: List<ProductEntity> = emptyList()

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
    setupShortcuts()
    setupHeaderActions()
    setupBottomNav()
    setupPromoClicks()
    setupParallaxScroll()
    startFlashSaleTimer()
    setupVideoPromo()
    binding.tvHomeSloganMarquee.isSelected = true

    binding.cardHomeRoutineWidget.setOnClickListener {
      parentFragmentManager.beginTransaction()
          .replace(R.id.main_container, com.veganbeauty.app.features.routine.SkinReminderFragment())
          .addToBackStack(null)
          .commit()
    }
    setupStreakWidget()
    setupQuizReminder()

    binding.btnLoadMoreRecommendations.setOnClickListener {
        recommendationLimit += 6
        updateRecommendationsList()
    }
  }

  private fun updateRecommendationsList() {
      val itemsToShow = allRecommendationProducts.take(recommendationLimit)
      recommendationsAdapter.submitList(itemsToShow)
      if (recommendationLimit >= allRecommendationProducts.size) {
          binding.btnLoadMoreRecommendations.visibility = View.GONE
      } else {
          binding.btnLoadMoreRecommendations.visibility = View.VISIBLE
      }
  }

  private fun setupShortcuts() {
      updateShortcutsList()
      binding.ivExpandShortcuts.setOnClickListener {
          isShortcutsExpanded = !isShortcutsExpanded
          updateShortcutsList()
      }
  }

  private fun updateShortcutsList() {
      if (isShortcutsExpanded) {
          shortcutAdapter.submitList(allShortcuts)
          binding.ivExpandShortcuts.setImageResource(R.drawable.ic_double_chevron_up)
      } else {
          shortcutAdapter.submitList(allShortcuts.take(8))
          binding.ivExpandShortcuts.setImageResource(R.drawable.ic_double_chevron_down)
      }
  }

  private fun setupVideoPromo() {
      val videoUrl = "https://image.cocoonvietnam.com/uploads/2_282afe8dca.mp4"
      binding.vvPromoSunscreen.setVideoPath(videoUrl)
      binding.vvPromoSunscreen.setOnPreparedListener { mp ->
          mp.isLooping = true
          mp.setVolume(0f, 0f)
          mp.start()
      }
  }


  private fun startFlashSaleTimer() {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    
    var endHour = -1

    if (currentHour in 1 until 5) {
        endHour = 5
    } else if (currentHour in 9 until 13) {
        endHour = 13
    } else if (currentHour in 16 until 24) {
        endHour = 24
    }

    val timeRemaining = if (endHour != -1) {
        val endTimeMillis = Calendar.getInstance().apply {
            if (endHour == 24) {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
            } else {
                set(Calendar.HOUR_OF_DAY, endHour)
            }
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        endTimeMillis - System.currentTimeMillis()
    } else {
        0L
    }

    if (timeRemaining > 0) {
        flashSaleTimer = object : android.os.CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerUI(millisUntilFinished)
            }
            override fun onFinish() {
                updateTimerUI(0)
            }
        }.start()
    } else {
        updateTimerUI(0)
    }
  }

  private fun updateTimerUI(millis: Long) {
      if (_binding == null) return
      val seconds = (millis / 1000) % 60
      val minutes = (millis / (1000 * 60)) % 60
      val hours = (millis / (1000 * 60 * 60)) % 24
      
      binding.tvFlashHour.text = String.format("%02d", hours)
      binding.tvFlashMinute.text = String.format("%02d", minutes)
      binding.tvFlashSecond.text = String.format("%02d", seconds)
  }

  private fun setupParallaxScroll() {
    binding.homeScrollView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
      val screenCenterY = scrollY + v.height / 2f
      val cardCenterY = binding.cvPromoLotus.top + binding.cvPromoLotus.height / 2f
      
      val distanceFromCenter = cardCenterY - screenCenterY
      
      val factorDown = -distanceFromCenter * 0.15f
      val factorUp = distanceFromCenter * 0.15f
      val factorUpSlow = distanceFromCenter * 0.08f

      binding.tvPromoLotusLeft.translationY = factorDown
      binding.llPromoLotusRight.translationY = factorDown
      binding.ivPromoLotus3.translationY = factorDown

      binding.ivPromoLotus1.translationY = factorUp
      binding.ivPromoLotus2.translationY = factorUpSlow
    }
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
    binding.rvShortcuts.apply {
      layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 4)
      adapter = shortcutAdapter
      isNestedScrollingEnabled = false
    }
    binding.rvFlashSale.apply {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = flashSaleAdapter
    }
    binding.rvRecentActivity.apply {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = recentAdapter
    }
    binding.rvRecommendations.apply {
      layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
      adapter = recommendationsAdapter
      isNestedScrollingEnabled = false
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


    binding.tvRecentSeeAll.setOnClickListener { openShop() }
    binding.tvCategoriesSeeAll.setOnClickListener { openShop() }
  }

  private fun setupPromoClicks() {
    binding.btnPromoSunscreen.setOnClickListener { openFeaturedProduct("chống nắng", "bí đao") }
    binding.btnPromoLotus.setOnClickListener { openFeaturedProduct("sen", "tinh chất") }
    
    val btnDiscoverAboutUs = binding.root.findViewById<View>(R.id.btn_discover_about_us)
    btnDiscoverAboutUs?.setOnClickListener {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, HomeAboutUsFragment())
            .addToBackStack(null)
            .commit()
    }
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

    flashSaleAdapter.submitList(products.shuffled().take(4))
    recentAdapter.submitList(products.take(8))

    allRecommendationProducts = products.shuffled().take(20) // Get a random pool of products for recommendations
    recommendationLimit = 6 // Reset limit when new products arrive
    updateRecommendationsList()

    bestsellerAdapter.submitList(products.sortedByDescending { it.price }.take(3))
    topSearchAdapter.submitList(products.take(3))

    val categories =
        products
            .map { it.category }
            .filter { it.isNotBlank() && it != "Chăm Sóc Tóc" && it != "Chăm Sóc Mái Tóc" }
            .distinct()
            .take(6)
            .map { categoryName ->
                val iconResId = when (categoryName) {
                    "Combo & Bộ Sản Phẩm" -> R.drawable.ic_combo
                    "Chăm sóc da", "Chăm Sóc Da Mặt" -> R.drawable.ic_skincare
                    "Tắm & Dưỡng Thể", "Chăm Sóc Cơ Thể" -> R.drawable.ic_shower
                    "Dưỡng Môi", "Chăm Sóc Môi" -> R.drawable.ic_lips
                    "Chăm Sóc Tóc", "Chăm Sóc Mái Tóc" -> R.drawable.ic_hair
                    else -> R.drawable.ic_grid
                }
                HomeCategoryItem(categoryName, iconResId)
            }
    categoryAdapter.submitList(categories)
  }

  private fun bindPromoImages(products: List<ProductEntity>) {
    // Promo image is replaced by VideoView


//    findProduct(products, "sen", "tinh chất")?.let { product ->
//      binding.ivPromoLotus.load(product.mainImage) {
//        crossfade(true)
//        placeholder(R.drawable.myphamxanh)
//      }
//    }
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

  private fun addToCart(product: ProductEntity) {
    CartHelper.addToCart(requireContext(), lifecycleScope, product, 1)
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
        
        // Show popup for first-time users
        if (lastTestTime == 0L && !hasShownQuizPopupThisSession) {
            hasShownQuizPopupThisSession = true
            view?.postDelayed({
                if (!isAdded) return@postDelayed
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quiz_test_new_user, null)
                val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
                
                // Entrance bouncing animation for the whole dialog
                val scaleX = android.animation.ObjectAnimator.ofFloat(dialogView, "scaleX", 0.7f, 1.05f, 1f)
                val scaleY = android.animation.ObjectAnimator.ofFloat(dialogView, "scaleY", 0.7f, 1.05f, 1f)
                val enterAnim = android.animation.AnimatorSet()
                enterAnim.playTogether(scaleX, scaleY)
                enterAnim.duration = 500
                enterAnim.interpolator = android.view.animation.OvershootInterpolator(1.5f)
                enterAnim.start()

                // Continuous jumping animation for the badge to catch attention
                val badgeLayout = dialogView.findViewById<View>(R.id.layout_badge)
                val bounceAnim = android.animation.ObjectAnimator.ofFloat(badgeLayout, "translationY", 0f, -20f, 0f)
                bounceAnim.duration = 1200
                bounceAnim.repeatCount = android.animation.ObjectAnimator.INFINITE
                bounceAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                bounceAnim.start()
            }, 5000)
        }
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
    flashSaleTimer?.cancel()
    _binding = null
  }

  companion object {
      private var hasShownQuizPopupThisSession = false
  }
}

