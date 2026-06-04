package com.veganbeauty.app.features.account.reward

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.data.local.entities.UserGiftEntity
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountRewardFragmentBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountRewardFragment : RootieFragment() {

    private var _binding: AccountRewardFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: OrderRepository
    private var currentPoints = 8500

    private val myGiftsList = mutableListOf<UserGiftEntity>()
    private val filteredGiftsList = mutableListOf<UserGiftEntity>()
    
    private var activeFilter = "Tất cả" // For My Gifts tab
    private var activeExchangeFilter = "Tất cả" // For Exchange tab

    private lateinit var exchangeAdapter: ExchangeGiftsAdapter
    private lateinit var giftsAdapter: MyGiftsAdapter
    private lateinit var historyAdapter: HistoryAdapter

    private val redeemableGifts = listOf(
        RedeemableGift(
            giftId = "voucher_50k",
            title = "Voucher Giảm 50K",
            description = "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.",
            cost = 1200,
            expiryDate = "15/12/2026",
            code = "SAVE50K",
            giftType = "voucher",
            rankRequired = "Bạc"
        ),
        RedeemableGift(
            giftId = "gift_rose_water",
            title = "Nước Hoa Hồng Hữu Cơ",
            description = "Phiên bản giới hạn",
            cost = 5000,
            expiryDate = "15/12/2026",
            code = "ROSE5G",
            giftType = "product",
            rankRequired = "Bạc"
        ),
        RedeemableGift(
            giftId = "gift_freeship",
            title = "Miễn Phí Vận Chuyển Toàn Quốc",
            description = "Tối đa 30K phí ship",
            cost = 800,
            expiryDate = "Hôm nay",
            code = "FREESHIP",
            giftType = "freeship",
            rankRequired = "Đồng"
        ),
        RedeemableGift(
            giftId = "gift_holiday_combo",
            title = "Bộ Quà Tặng Mùa Lễ Hội",
            description = "Bộ quà tặng đặc biệt",
            cost = 5000,
            expiryDate = "31/12/2026",
            code = "HOLIDAY12",
            giftType = "gift",
            rankRequired = "Bạc"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountRewardFragmentBinding.inflate(inflater, container, false)
        setupRepository()
        return binding.root
    }

    private fun setupRepository() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Daily Checkin Action
        binding.btnCheckIn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.checkin.AccountCheckinFragment())
                .addToBackStack(null)
                .commit()
        }

        // Rich style warning banner
        binding.tvAlertWarning.text = Html.fromHtml("Bạn có <b>500 điểm</b> sắp hết hạn vào 30/11.", Html.FROM_HTML_MODE_COMPACT)

        // Setup Tab Selection
        setupTabs()

        // Setup category chips for Exchange Tab
        setupExchangeChips()

        // Setup filter chips for My Gifts Tab
        setupFilterChips()

        // Recycler View for Exchange tab
        exchangeAdapter = ExchangeGiftsAdapter(
            onItemClick = { gift ->
                openGiftDetail(gift.giftId, gift.title, gift.description, gift.cost, gift.expiryDate, gift.code, gift.giftType, false, -1, gift.rankRequired)
            },
            onActionClick = { gift ->
                performRedeemDirectly(gift)
            }
        )
        binding.rvExchangeGifts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExchangeGifts.adapter = exchangeAdapter
        applyExchangeFilter()

        // Recycler View for My Gifts tab
        giftsAdapter = MyGiftsAdapter(
            onItemClick = { gift ->
                openGiftDetail(gift.giftId, gift.title, gift.description, gift.cost, gift.expiryDate, gift.code, gift.giftType, true, gift.id, "Đồng")
            },
            onActionClick = { gift ->
                copyToClipboard(requireContext(), gift.code)
                Toast.makeText(context, "Đã sao chép mã voucher ${gift.code} thành công!", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvMyGifts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMyGifts.adapter = giftsAdapter

        // Recycler View for History tab
        historyAdapter = HistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter
    }

    override fun observeViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getTotalPointsFlow().collect { points ->
                val pointsVal = points ?: 0
                currentPoints = pointsVal
                binding.tvCurrentPoints.text = String.format("%,d", pointsVal).replace(',', '.')

                // Calculate progress to Gold rank (10,000 points) or Diamond rank (20,000 points)
                val (rank, progressText, progressPct) = when {
                    pointsVal >= 20000 -> Triple("Hạng Kim Cương", "Đạt hạng cao nhất", 100)
                    pointsVal >= 10000 -> {
                        val remaining = 20000 - pointsVal
                        val formattedRemaining = String.format("%,d", remaining).replace(',', '.')
                        val pct = (pointsVal - 10000) * 100 / 10000
                        Triple("Hạng Vàng", "Còn $formattedRemaining xu để đến Kim Cương", pct)
                    }
                    else -> {
                        val remaining = 10000 - pointsVal
                        val formattedRemaining = String.format("%,d", remaining).replace(',', '.')
                        val pct = if (pointsVal >= 1000) ((pointsVal - 1000) * 100 / 9000) else 0
                        val adjustedPct = if (pointsVal == 8500) 56 else pct
                        Triple("Hạng Bạc", "Còn $formattedRemaining xu để đến Vàng", adjustedPct)
                    }
                }
                binding.tvCurrentRank.text = rank
                binding.tvNextRankHint.text = progressText
                binding.pbRankProgress.progress = progressPct
                
                // Refresh exchange list in case points changed
                applyExchangeFilter()
            }
        }

        // Observe My Gifts
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllUserGifts().collect { gifts ->
                myGiftsList.clear()
                myGiftsList.addAll(gifts)
                applyGiftsFilter()
            }
        }

        // Observe History Logs
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getAllRewardHistory().collect { history ->
                val logs = history.filter { it.orderId != "SYSTEM" }
                val historyItems = mutableListOf<HistoryItem>()
                
                // Group by month
                val sdfGroup = SimpleDateFormat("'Tháng' MM, yyyy", Locale.getDefault())
                val grouped = logs.groupBy { sdfGroup.format(Date(it.timestamp)) }
                
                for ((monthHeader, items) in grouped) {
                    historyItems.add(HistoryItem.Header(monthHeader))
                    for (item in items) {
                        historyItems.add(HistoryItem.Transaction(item))
                    }
                }
                historyAdapter.submitList(historyItems)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AccountRewardDetailFragment.selectRewardTabOnResume == 1) {
            binding.btnTabMyGifts.performClick()
            AccountRewardDetailFragment.selectRewardTabOnResume = 0
        }
    }

    private fun setupTabs() {
        val tabEx = binding.btnTabExchange
        val tabMy = binding.btnTabMyGifts
        val tabHist = binding.btnTabHistory

        val tabs = listOf(tabEx, tabMy, tabHist)
        val textViews = listOf(binding.tabExchange, binding.tabMyGifts, binding.tabHistory)
        val indicators = listOf(binding.indicatorExchange, binding.indicatorMyGifts, binding.indicatorHistory)
        val layouts = listOf(binding.layoutExchangeTab, binding.layoutMyGiftsTab, binding.layoutHistoryTab)

        for (i in tabs.indices) {
            tabs[i].setOnClickListener {
                for (j in tabs.indices) {
                    if (j == i) {
                        textViews[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                        textViews[j].setTypeface(null, android.graphics.Typeface.BOLD)
                        indicators[j].visibility = View.VISIBLE
                        layouts[j].visibility = View.VISIBLE
                    } else {
                        textViews[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                        textViews[j].setTypeface(null, android.graphics.Typeface.NORMAL)
                        indicators[j].visibility = View.INVISIBLE
                        layouts[j].visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupExchangeChips() {
        val chipAll = binding.chipAllExchange
        val chipVoucher = binding.chipVoucherExchange
        val chipProduct = binding.chipProductExchange
        val chipGift = binding.chipGiftExchange

        val chips = listOf(chipAll, chipVoucher, chipProduct, chipGift)
        val filterNames = listOf("Tất cả", "Voucher giảm giá", "Sản phẩm", "Quà tặng")

        for (i in chips.indices) {
            chips[i].setOnClickListener {
                activeExchangeFilter = filterNames[i]
                for (j in chips.indices) {
                    if (j == i) {
                        chips[j].setBackgroundResource(R.drawable.tab_active_bg)
                        chips[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        chips[j].setBackgroundResource(R.drawable.tab_inactive_bg)
                        chips[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    }
                }
                applyExchangeFilter()
            }
        }
    }

    private fun setupFilterChips() {
        val chipAll = binding.chipAll
        val chipValid = binding.chipValid
        val chipExpired = binding.chipExpired

        val chips = listOf(chipAll, chipValid, chipExpired)
        val filterNames = listOf("Tất cả", "Còn hạn", "Hết hạn")

        for (i in chips.indices) {
            chips[i].setOnClickListener {
                activeFilter = filterNames[i]
                for (j in chips.indices) {
                    if (j == i) {
                        chips[j].setBackgroundResource(R.drawable.tab_active_bg)
                        chips[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        chips[j].setBackgroundResource(R.drawable.tab_inactive_bg)
                        chips[j].setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    }
                }
                applyGiftsFilter()
            }
        }
    }

    private fun applyExchangeFilter() {
        val filtered = when (activeExchangeFilter) {
            "Voucher giảm giá" -> redeemableGifts.filter { it.giftType == "voucher" || it.giftType == "freeship" }
            "Sản phẩm" -> redeemableGifts.filter { it.giftType == "product" }
            "Quà tặng" -> redeemableGifts.filter { it.giftType == "gift" }
            else -> redeemableGifts
        }
        exchangeAdapter.submitList(filtered, currentPoints)
    }

    private fun applyGiftsFilter() {
        filteredGiftsList.clear()
        when (activeFilter) {
            "Còn hạn" -> {
                filteredGiftsList.addAll(myGiftsList.filter { it.status == "Còn hạn" || it.status == "Hôm nay" })
            }
            "Hết hạn" -> {
                filteredGiftsList.addAll(myGiftsList.filter { it.status == "Hết hạn" })
            }
            else -> {
                filteredGiftsList.addAll(myGiftsList)
            }
        }
        giftsAdapter.submitList(filteredGiftsList)
    }

    private fun openGiftDetail(
        giftId: String,
        title: String,
        description: String,
        cost: Int,
        expiryDate: String,
        code: String,
        giftType: String,
        isOwned: Boolean,
        dbId: Int,
        rankRequired: String = "Đồng"
    ) {
        val detailFrag = AccountRewardDetailFragment.newInstance(
            giftId, title, description, cost, expiryDate, code, giftType, isOwned, dbId, rankRequired
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, detailFrag)
            .addToBackStack(null)
            .commit()
    }

    private fun performRedeemDirectly(gift: RedeemableGift) {
        if (currentPoints < gift.cost) {
            Toast.makeText(requireContext(), "Bạn không đủ xu để đổi quà tặng này!", Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val success = repository.redeemGift(
                giftId = gift.giftId,
                title = gift.title,
                description = gift.description,
                cost = gift.cost,
                expiryDate = gift.expiryDate,
                code = gift.code,
                giftType = gift.giftType
            )
            if (success) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Đổi quà thành công!")
                    .setMessage("Chúc mừng! Bạn đã đổi thành công ${gift.title}. Quà tặng đã được lưu vào mục 'Quà của tôi'.")
                    .setPositiveButton("Xem danh sách quà") { _, _ ->
                        binding.btnTabMyGifts.performClick()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Đổi quà thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Rootie Voucher Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Model for redeemable gifts
class RedeemableGift(
    val giftId: String,
    val title: String,
    val description: String,
    val cost: Int,
    val expiryDate: String,
    val code: String,
    val giftType: String,
    val rankRequired: String
)

// RecyclerView Adapter for Redeemable Gifts (Flat Card layout, Screenshot 1)
class ExchangeGiftsAdapter(
    private val onItemClick: (RedeemableGift) -> Unit,
    private val onActionClick: (RedeemableGift) -> Unit
) : RecyclerView.Adapter<ExchangeGiftsAdapter.ViewHolder>() {

    private val items = mutableListOf<RedeemableGift>()
    private var userPoints = 8500

    fun submitList(newItems: List<RedeemableGift>, points: Int) {
        items.clear()
        items.addAll(newItems)
        userPoints = points
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward_gift, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], userPoints, onItemClick, onActionClick)
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvGiftTitle)
        private val subtitle = view.findViewById<TextView>(R.id.tvGiftSubtitle)
        private val value = view.findViewById<TextView>(R.id.tvGiftValue)
        private val img = view.findViewById<ImageView>(R.id.ivGiftIcon)
        private val imgCircle = view.findViewById<View>(R.id.layoutIconCircle)
        private val actionBtn = view.findViewById<TextView>(R.id.btnAction)

        fun bind(
            gift: RedeemableGift,
            userPoints: Int,
            onClick: (RedeemableGift) -> Unit,
            onActionClick: (RedeemableGift) -> Unit
        ) {
            title.text = gift.title
            subtitle.text = if (gift.expiryDate == "Hôm nay") gift.description else "HSD: ${gift.expiryDate}"
            value.text = String.format("%,d đ", gift.cost).replace(',', '.')

            // Check rank condition. User points = 8500 (Silver/Bạc).
            // "Vàng" rank requires 10,000 points.
            val isLocked = (gift.rankRequired == "Vàng" && userPoints < 10000) || (gift.rankRequired == "Kim Cương" && userPoints < 20000)
            val isNotEnoughPoints = userPoints < gift.cost

            // Allow access to gift detail even if conditions are not met
            itemView.setOnClickListener { onClick(gift) }

            if (isLocked) {
                actionBtn.text = "Chưa đủ ĐK"
                actionBtn.isEnabled = false
                actionBtn.setTextColor(Color.parseColor("#A0AEC0"))
                actionBtn.setBackgroundResource(R.drawable.status_badge_bg)
                actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEFF0"))

                img.setImageResource(R.drawable.ic_lock)
                img.imageTintList = ColorStateList.valueOf(Color.parseColor("#A0AEC0"))
                imgCircle.setBackgroundResource(R.drawable.bg_circle_grey)
                
                actionBtn.setOnClickListener(null)
            } else if (isNotEnoughPoints) {
                actionBtn.text = "Đổi quà"
                actionBtn.isEnabled = false
                actionBtn.setTextColor(Color.parseColor("#A0AEC0"))
                actionBtn.setBackgroundResource(R.drawable.status_badge_bg)
                actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEFF0"))

                // Select icon based on giftType
                val iconRes = when (gift.giftType) {
                    "voucher" -> R.drawable.ic_voucher
                    "freeship" -> R.drawable.ic_truck
                    "gift" -> R.drawable.ic_gift
                    "product" -> R.drawable.ic_logo_ol
                    else -> R.drawable.ic_gift
                }
                img.setImageResource(iconRes)
                img.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.primary))
                imgCircle.setBackgroundResource(R.drawable.bg_circle_light_green)
                
                actionBtn.setOnClickListener(null)
            } else {
                actionBtn.text = "Đổi quà"
                actionBtn.isEnabled = true
                actionBtn.setTextColor(Color.WHITE)
                actionBtn.setBackgroundResource(R.drawable.tab_active_bg)
                actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34")) // Dark green button

                // Select icon based on giftType
                val iconRes = when (gift.giftType) {
                    "voucher" -> R.drawable.ic_voucher
                    "freeship" -> R.drawable.ic_truck
                    "gift" -> R.drawable.ic_gift
                    "product" -> R.drawable.ic_logo_ol
                    else -> R.drawable.ic_gift
                }
                img.setImageResource(iconRes)
                img.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.primary))
                imgCircle.setBackgroundResource(R.drawable.bg_circle_light_green)

                actionBtn.setOnClickListener { onActionClick(gift) }
            }
        }
    }
}

// RecyclerView Adapter for My Gifts (Screenshot 2)
class MyGiftsAdapter(
    private val onItemClick: (UserGiftEntity) -> Unit,
    private val onActionClick: (UserGiftEntity) -> Unit
) : RecyclerView.Adapter<MyGiftsAdapter.ViewHolder>() {

    private val items = mutableListOf<UserGiftEntity>()

    fun submitList(newItems: List<UserGiftEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward_gift, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick, onActionClick)
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvGiftTitle)
        private val subtitle = view.findViewById<TextView>(R.id.tvGiftSubtitle)
        private val value = view.findViewById<TextView>(R.id.tvGiftValue)
        private val badge = view.findViewById<TextView>(R.id.tvGiftStatusBadge)
        private val img = view.findViewById<ImageView>(R.id.ivGiftIcon)
        private val imgCircle = view.findViewById<View>(R.id.layoutIconCircle)
        private val actionBtn = view.findViewById<TextView>(R.id.btnAction)

        fun bind(
            gift: UserGiftEntity,
            onClick: (UserGiftEntity) -> Unit,
            onActionClick: (UserGiftEntity) -> Unit
        ) {
            title.text = gift.title
            
            val expiryText = if (gift.expiryDate == "Hôm nay") "HSD: Hôm nay" else "HSD: ${gift.expiryDate}"
            subtitle.text = "${gift.description}\n$expiryText"
            
            // Hide price label for owned gifts
            value.visibility = View.GONE
            badge.visibility = View.VISIBLE
            badge.text = gift.status

            // Format status badge & action button
            when (gift.status) {
                "Hôm nay" -> {
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF3E0")) // Light orange
                    badge.setTextColor(Color.parseColor("#D97706")) // Orange-red text
                    
                    actionBtn.visibility = View.VISIBLE
                    actionBtn.text = "Sử dụng"
                    actionBtn.setTextColor(Color.WHITE)
                    actionBtn.setBackgroundResource(R.drawable.tab_active_bg)
                    actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34"))
                }
                "Hết hạn" -> {
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEFF1")) // Light gray
                    badge.setTextColor(Color.parseColor("#7E8A83")) // Gray text
                    
                    actionBtn.visibility = View.GONE // Expired gifts don't have use button
                }
                else -> { // "Còn hạn"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9")) // Light green
                    badge.setTextColor(Color.parseColor("#2E7D32")) // Green text
                    
                    actionBtn.visibility = View.VISIBLE
                    actionBtn.text = "Sử dụng"
                    actionBtn.setTextColor(Color.WHITE)
                    actionBtn.setBackgroundResource(R.drawable.tab_active_bg)
                    actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34"))
                }
            }

            val iconRes = when (gift.giftType) {
                "voucher" -> R.drawable.ic_voucher
                "freeship" -> R.drawable.ic_truck
                "gift" -> R.drawable.ic_gift
                "product" -> R.drawable.ic_logo_ol
                else -> R.drawable.ic_gift
            }
            img.setImageResource(iconRes)
            img.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.primary))
            imgCircle.setBackgroundResource(R.drawable.bg_circle_light_green)

            itemView.setOnClickListener { onClick(gift) }
            actionBtn.setOnClickListener { onActionClick(gift) }
        }
    }
}

// History Log list grouped items
sealed class HistoryItem {
    data class Header(val title: String) : HistoryItem()
    data class Transaction(val data: RewardPointEntity) : HistoryItem()
}

// RecyclerView Adapter for points history (Screenshot 4)
class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<HistoryItem>()

    fun submitList(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.Header -> 0
            is HistoryItem.Transaction -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 20, 16, 8)
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.BLACK)
            }
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward_history, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is HistoryItem.Transaction -> {
                // Check if first or last item in a timeline chain
                val isFirst = position == 0 || items[position - 1] is HistoryItem.Header
                val isLast = position == itemCount - 1 || items[position + 1] is HistoryItem.Header
                
                (holder as ItemViewHolder).bind(item.data, isFirst, isLast, position == 1)
            }
        }
    }

    override fun getItemCount() = items.size

    class HeaderViewHolder(val tv: TextView) : RecyclerView.ViewHolder(tv) {
        fun bind(title: String) {
            tv.text = title
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvHistoryTitle)
        private val date = view.findViewById<TextView>(R.id.tvHistoryDate)
        private val value = view.findViewById<TextView>(R.id.tvHistoryPoints)
        private val status = view.findViewById<TextView>(R.id.tvHistoryStatus)
        private val dot = view.findViewById<View>(R.id.timelineDot)
        private val lineTop = view.findViewById<View>(R.id.timelineLineTop)
        private val lineBottom = view.findViewById<View>(R.id.timelineLineBottom)

        fun bind(item: RewardPointEntity, isFirst: Boolean, isLast: Boolean, isMostRecent: Boolean) {
            title.text = item.reason.replace("Đổi quà: ", "")
            
            val sdfTime = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
            date.text = sdfTime.format(Date(item.timestamp))
            
            val formattedVal = String.format("%,d", item.points).replace(',', '.')
            value.text = if (item.points > 0) "+ $formattedVal" else "$formattedVal"
            value.setTextColor(Color.parseColor("#1A202C"))

            // Most recent transaction dot is dark green, older ones are gray
            if (isMostRecent) {
                dot.setBackgroundResource(R.drawable.bg_circle_green)
                dot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3E4D44"))
            } else {
                dot.setBackgroundResource(R.drawable.bg_circle_grey)
                dot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#A0AEC0"))
            }

            // Show/hide top and bottom vertical lines to make timeline clean
            lineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
            lineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

            status.text = "Đã đổi quà"
        }
    }
}
