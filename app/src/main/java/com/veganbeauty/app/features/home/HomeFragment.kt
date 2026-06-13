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

  private val recentAdapter = HomeProductCardAdapter(onItemClick = ::openProductDetail)
  private val recommendationsAdapter = HomeProductCardAdapter(onItemClick = ::openProductDetail)
  private val bestsellerAdapter = HomeBestsellerAdapter(onItemClick = ::openProductDetail)
  private val topSearchAdapter = HomeTopSearchAdapter(onItemClick = ::openProductDetail)
  private val flashSaleAdapter = HomeFlashsaleAdapter(onItemClick = ::openProductDetail)
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

  private val allShortcuts by lazy {
      listOf(
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Quét sản phẩm", R.drawable.ic_qrscan) { Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Soi Da AI", R.drawable.ic_face) { navigateToFragment(com.veganbeauty.app.features.myskin.SkinScanFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Đặt lịch soi da", R.drawable.ic_calendar) { navigateToFragment(com.veganbeauty.app.features.myskin.ChooseBranchFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Routine của tôi", R.drawable.ic_skincare) { navigateToFragment(com.veganbeauty.app.features.myskin.MySkinFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Dự báo da hôm nay", R.drawable.ic_water_drop) { Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Kiểm tra dị ứng", R.drawable.ic_warning_red) { navigateToFragment(com.veganbeauty.app.features.quiz.QuizTestIntroFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Hồ sơ làn da", R.drawable.ic_clipboard_outline) { navigateToFragment(com.veganbeauty.app.features.myskin.BookingHistoryFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Nhắc chăm da", R.drawable.ic_bell) { Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Đổi quà Rootie Xu", R.drawable.ic_gift) { navigateToFragment(com.veganbeauty.app.features.account.reward.AccountRewardFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Cửa hàng gần bạn", R.drawable.ic_store) { navigateToFragment(com.veganbeauty.app.features.myskin.ChooseBranchFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Beauty Explore", R.drawable.ic_reel_outline) { navigateToFragment(com.veganbeauty.app.features.community.com_feed.ComLoadingFragment()) },
          com.veganbeauty.app.features.home.adapter.HomeShortcutItem("Tra cứu thành phần", R.drawable.ic_shortcut_ingredient) { Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() }
      )
  }

  private var flashSaleTimer: android.os.CountDownTimer? = null
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
    setupShortcuts()
    setupHeaderActions()
    setupBottomNav()
    setupPromoClicks()
    setupParallaxScroll()
    startFlashSaleTimer()
    setupVideoPromo()
    binding.tvHomeSloganMarquee.isSelected = true
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
    
    val endHour = 12
    val startHour = 9
    
    val endTimeMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, endHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val nowMillis = System.currentTimeMillis()

    val timeRemaining = if (currentHour in startHour until endHour) {
        endTimeMillis - nowMillis
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
    recommendationsAdapter.submitList(products.takeLast(8).reversed())

    bestsellerAdapter.submitList(products.sortedByDescending { it.price }.take(3))
    topSearchAdapter.submitList(products.take(3))

    val categories =
        products
            .map { it.category }
            .filter { it.isNotBlank() }
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

  private fun openShop() {
    parentFragmentManager
        .beginTransaction()
        .replace(R.id.main_container, ShopListFragment())
        .commit()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    flashSaleTimer?.cancel()
    _binding = null
  }
}

