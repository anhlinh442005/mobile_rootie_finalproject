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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private var redeemableGifts: List<RedeemableGift> = emptyList()

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
        val db = RootieDatabase.getDatabase(requireContext())
        repository = OrderRepository(
            db.orderDao(),
            db.rewardPointDao(),
            db.userGiftDao(),
            LocalJsonReader(requireContext())
        )
    }

    override fun setupUI(view: View) {
        // Load dynamically from assets
        redeemableGifts = loadGiftsFromAssets(requireContext())

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

        // Warning banner
        binding.tvAlertWarning.text = Html.fromHtml("Bạn có <b>500 điểm</b> sắp hết hạn vào 30/11.", Html.FROM_HTML_MODE_COMPACT)

        setupTabs()
        setupExchangeChips()
        setupFilterChips()

        // Recycler View for Exchange tab
        exchangeAdapter = ExchangeGiftsAdapter(
            onItemClick = { gift ->
                val ownedItem = myGiftsList.find { it.giftId == gift.giftId }
                val isOwned = ownedItem != null
                val dbId = ownedItem?.id ?: -1
                openGiftDetail(gift, isOwned, dbId)
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
            onItemClick = { userGift ->
                val gift = RedeemableGift(
                    giftId = userGift.giftId,
                    title = userGift.title,
                    description = userGift.description,
                    cost = userGift.cost,
                    expiryDate = userGift.expiryDate,
                    code = userGift.code,
                    giftType = userGift.giftType,
                    status = "redeemed",
                    productId = userGift.productId,
                    minOrderValue = userGift.minOrderValue,
                    applicableProducts = userGift.applicableProducts,
                    offerType = userGift.offerType,
                    discountValue = userGift.discountValue,
                    rankRequired = "Đồng"
                )
                openGiftDetail(gift, true, userGift.id)
            },
            onActionClick = { userGift ->
                copyToClipboard(requireContext(), userGift.code)
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = repository.deleteUserGiftById(userGift.id)
                    if (success) {
                        Toast.makeText(context, "Mã quà tặng ${userGift.code} đã được áp dụng thành công!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Sử dụng quà tặng thất bại!", Toast.LENGTH_SHORT).show()
                    }
                    com.veganbeauty.app.features.home.BottomNavHelper.navigate(this@AccountRewardFragment, R.id.nav_shop)
                }
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
        val db = RootieDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getTotalPointsFlow().collect { points ->
                val pointsVal = points ?: 0
                currentPoints = pointsVal
                binding.tvCurrentPoints.text = String.format("%,d", pointsVal).replace(',', '.')

                // Range mapping: 0-4999 -> Thường, 5000-9999 -> Bạc, 10000-19999 -> Vàng, >=20000 -> VIP
                val (rank, progressText, progressPct) = when {
                    pointsVal >= 20000 -> Triple("Hạng VIP", "Đạt hạng cao nhất", 100)
                    pointsVal >= 10000 -> {
                        val remaining = 20000 - pointsVal
                        val formattedRemaining = String.format("%,d", remaining).replace(',', '.')
                        val pct = (pointsVal - 10000) * 100 / 10000
                        Triple("Hạng Vàng", "Còn $formattedRemaining xu để đến VIP", pct)
                    }
                    pointsVal >= 5000 -> {
                        val remaining = 10000 - pointsVal
                        val formattedRemaining = String.format("%,d", remaining).replace(',', '.')
                        val pct = (pointsVal - 5000) * 100 / 5000
                        Triple("Hạng Bạc", "Còn $formattedRemaining xu để đến Vàng", pct)
                    }
                    else -> {
                        val remaining = 5000 - pointsVal
                        val formattedRemaining = String.format("%,d", remaining).replace(',', '.')
                        val pct = pointsVal * 100 / 5000
                        Triple("Hạng Thường", "Còn $formattedRemaining xu để đến Bạc", pct)
                    }
                }
                binding.tvCurrentRank.text = rank
                binding.tvNextRankHint.text = progressText
                binding.pbRankProgress.progress = progressPct
                
                applyExchangeFilter()
            }
        }

        // Observe My Gifts
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllUserGifts().collect { gifts ->
                myGiftsList.clear()
                myGiftsList.addAll(gifts)
                
                // Dynamically sync status in redeemableGifts
                val ownedIds = gifts.map { it.giftId }.toSet()
                for (g in redeemableGifts) {
                    if (g.giftId in ownedIds) {
                        g.status = "redeemed"
                    } else {
                        g.status = "unredeemed"
                    }
                }
                
                applyGiftsFilter()
                applyExchangeFilter()
            }
        }

        // Observe History Logs
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getAllRewardHistory().collect { history ->
                val logs = history.filter { it.orderId != "SYSTEM" }
                val historyItems = mutableListOf<HistoryItem>()
                
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
            "Voucher giảm giá" -> redeemableGifts.filter { it.giftType == "voucher_discount" || it.giftType == "voucher_freeship" }
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
                filteredGiftsList.addAll(myGiftsList.filter { 
                    val status = computeStatusFromExpiry(it.expiryDate)
                    status == "valid" || status == "expiring"
                })
            }
            "Hết hạn" -> {
                filteredGiftsList.addAll(myGiftsList.filter { 
                    computeStatusFromExpiry(it.expiryDate) == "expired"
                })
            }
            else -> {
                filteredGiftsList.addAll(myGiftsList)
            }
        }
        giftsAdapter.submitList(filteredGiftsList)
    }

    private fun computeStatusFromExpiry(expiryStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val expiryDate = sdf.parse(expiryStr) ?: return "valid"
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val expiry = Calendar.getInstance().apply {
                time = expiryDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (expiry.before(today)) {
                "expired"
            } else if (expiry.equals(today)) {
                "expiring"
            } else {
                "valid"
            }
        } catch (e: Exception) {
            "valid"
        }
    }

    private fun loadGiftsFromAssets(ctx: Context): List<RedeemableGift> {
        return try {
            val jsonString = ctx.assets.open("gifts.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<RedeemableGift>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RedeemableGift(
                        giftId = obj.optString("giftId", ""),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        cost = obj.optInt("cost", 0),
                        expiryDate = obj.optString("expiryDate", ""),
                        code = obj.optString("code", ""),
                        giftType = obj.optString("giftType", "gift"),
                        status = obj.optString("status", "unredeemed"),
                        productId = if (obj.has("product_id")) obj.getString("product_id") else null,
                        minOrderValue = obj.optInt("minOrderValue", 0),
                        applicableProducts = obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        offerType = obj.optString("offerType", "fixed_amount"),
                        discountValue = obj.optInt("discountValue", 0),
                        rankRequired = obj.optString("rankRequired", "Đồng")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun openGiftDetail(gift: RedeemableGift, isOwned: Boolean, dbId: Int) {
        val detailFrag = AccountRewardDetailFragment.newInstance(
            giftId = gift.giftId,
            title = gift.title,
            description = gift.description,
            cost = gift.cost,
            expiryDate = gift.expiryDate,
            code = gift.code,
            giftType = gift.giftType,
            isOwned = isOwned,
            dbId = dbId,
            rankRequired = gift.rankRequired,
            minOrderValue = gift.minOrderValue,
            applicableProducts = gift.applicableProducts,
            offerType = gift.offerType,
            productId = gift.productId,
            discountValue = gift.discountValue
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
                giftType = gift.giftType,
                minOrderValue = gift.minOrderValue,
                applicableProducts = gift.applicableProducts,
                offerType = gift.offerType,
                productId = gift.productId,
                discountValue = gift.discountValue
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

class RedeemableGift(
    val giftId: String,
    val title: String,
    val description: String,
    val cost: Int,
    val expiryDate: String,
    val code: String,
    val giftType: String,
    var status: String,
    val productId: String? = null,
    val minOrderValue: Int = 0,
    val applicableProducts: String = "Tất cả sản phẩm",
    val offerType: String = "fixed_amount",
    val discountValue: Int = 0,
    val rankRequired: String
)

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
        private val badge = view.findViewById<TextView>(R.id.tvGiftStatusBadge)

        fun bind(
            gift: RedeemableGift,
            userPoints: Int,
            onClick: (RedeemableGift) -> Unit,
            onActionClick: (RedeemableGift) -> Unit
        ) {
            title.text = gift.title
            
            // Format expiryDate: check if today (2026-06-11)
            val isExpiringToday = gift.expiryDate.startsWith("2026-06-11")
            val datePart = if (gift.expiryDate.contains(" ")) gift.expiryDate.split(" ")[0] else gift.expiryDate
            subtitle.text = if (isExpiringToday) "Hôm nay" else "HSD: $datePart"
            value.text = String.format("%,d xu", gift.cost).replace(',', '.')

            val isLocked = (gift.rankRequired == "Vàng" && userPoints < 10000) || 
                           (gift.rankRequired == "VIP" && userPoints < 20000) ||
                           (gift.rankRequired == "Kim Cương" && userPoints < 20000)
            val isNotEnoughPoints = userPoints < gift.cost

            itemView.setOnClickListener { onClick(gift) }

            // Dynamic "Sắp hết hạn" status badge
            if (isExpiringToday) {
                badge.visibility = View.VISIBLE
                badge.text = "Sắp hết hạn"
                badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                badge.setTextColor(Color.parseColor("#D97706"))
            } else {
                badge.visibility = View.GONE
            }

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

                val iconRes = when (gift.giftType) {
                    "voucher_discount", "voucher" -> R.drawable.ic_voucher
                    "voucher_freeship", "freeship" -> R.drawable.ic_truck
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
                actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34"))

                val iconRes = when (gift.giftType) {
                    "voucher_discount", "voucher" -> R.drawable.ic_voucher
                    "voucher_freeship", "freeship" -> R.drawable.ic_truck
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
            
            val displayHsd = if (gift.expiryDate.contains(" ")) gift.expiryDate.split(" ")[0] else gift.expiryDate
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val isExpiringToday = gift.expiryDate.startsWith(todayStr)
            
            subtitle.text = "${gift.description}\nHSD: ${if (isExpiringToday) "Hôm nay" else displayHsd}"
            
            value.visibility = View.GONE
            badge.visibility = View.VISIBLE

            // Compute status dynamically
            val isExpired = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val exp = sdf.parse(gift.expiryDate)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                exp != null && exp.before(today)
            } catch (e: Exception) {
                false
            }

            val computedStatus = when {
                isExpired -> "Hết hạn"
                isExpiringToday -> "Hôm nay"
                else -> "Còn hạn"
            }
            badge.text = computedStatus

            // Format status badge & action button
            when (computedStatus) {
                "Hôm nay" -> {
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                    badge.setTextColor(Color.parseColor("#D97706"))
                    
                    actionBtn.visibility = View.VISIBLE
                    actionBtn.text = "Sử dụng"
                    actionBtn.setTextColor(Color.WHITE)
                    actionBtn.setBackgroundResource(R.drawable.tab_active_bg)
                    actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34"))
                }
                "Hết hạn" -> {
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECEFF1"))
                    badge.setTextColor(Color.parseColor("#7E8A83"))
                    
                    actionBtn.visibility = View.GONE
                }
                else -> { // "Còn hạn"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                    badge.setTextColor(Color.parseColor("#2E7D32"))
                    
                    actionBtn.visibility = View.VISIBLE
                    actionBtn.text = "Sử dụng"
                    actionBtn.setTextColor(Color.WHITE)
                    actionBtn.setBackgroundResource(R.drawable.tab_active_bg)
                    actionBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2D3A34"))
                }
            }

            val iconRes = when (gift.giftType) {
                "voucher_discount", "voucher" -> R.drawable.ic_voucher
                "voucher_freeship", "freeship" -> R.drawable.ic_truck
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
            if (item.points > 0) {
                value.text = "+ $formattedVal"
                value.setTextColor(Color.parseColor("#38A169"))
                status.text = "Nhận xu thành công"
                status.setTextColor(Color.parseColor("#2E7D32"))
                status.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
            } else {
                value.text = "$formattedVal"
                value.setTextColor(Color.parseColor("#E53E3E"))
                status.text = "Đổi quà thành công"
                status.setTextColor(Color.parseColor("#C53030"))
                status.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FED7D7"))
            }

            if (isMostRecent) {
                dot.setBackgroundResource(R.drawable.bg_circle_green)
                dot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3E4D44"))
            } else {
                dot.setBackgroundResource(R.drawable.bg_circle_grey)
                dot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#A0AEC0"))
            }

            lineTop.visibility = if (isFirst) View.INVISIBLE else View.VISIBLE
            lineBottom.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
        }
    }
}
