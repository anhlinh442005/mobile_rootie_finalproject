package com.veganbeauty.app.features.profile

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountVoucherBinding
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import com.veganbeauty.app.features.home.BottomNavHelper
import com.veganbeauty.app.features.profile.AccountVoucherDetailFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale


class AccountVoucherFragment : RootieFragment() {

    private var _binding: AccountVoucherBinding? = null
    private val binding get() = _binding!!

    private var allVouchers: List<VoucherItem> = emptyList()
    private var systemVouchers: List<VoucherItem> = emptyList()
    private val deletedSystemVoucherIds = mutableSetOf<String>()
    
    private lateinit var adapter: VoucherListAdapter
    private lateinit var repository: OrderRepository
    private var currentTab = 0 // 0: All, 1: Valid, 2: Expired

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountVoucherBinding.inflate(inflater, container, false)
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
        val context = requireContext()

        // 1. Back button navigation
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Bell icon click navigation to AccountNotificationFragment
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // 3. Load initial system vouchers
        systemVouchers = loadVouchersFromAssets(context)

        // 4. Initialize RecyclerView with empty list first
        adapter = VoucherListAdapter(emptyList())
        adapter.onDeleteClickListener = { voucher ->
            if (voucher.id.startsWith("db_")) {
                val dbId = voucher.id.substringAfter("db_").toIntOrNull()
                if (dbId != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val success = repository.deleteUserGiftById(dbId)
                        if (success) {
                            Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Xoá voucher thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                deletedSystemVoucherIds.add(voucher.id)
                val activeSystem = systemVouchers.filter { it.id !in deletedSystemVoucherIds && it.id !in deletedSystemVoucherIdsStatic }
                // Map the active list
                allVouchers = allVouchers.filter { it.id != voucher.id }
                Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show()
                refreshVoucherList()
            }
        }
        adapter.onItemClickListener = { voucher ->
            val detailFragment = AccountVoucherDetailFragment.newInstance(voucher)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvVouchers.layoutManager = LinearLayoutManager(context)
        binding.rvVouchers.adapter = adapter

        // 5. Handle tab filter selection clicks
        binding.tabAll.setOnClickListener {
            currentTab = 0
            updateTabSelection(binding.tabAll, listOf(binding.tabValid, binding.tabExpired))
            refreshVoucherList()
        }

        binding.tabValid.setOnClickListener {
            currentTab = 1
            updateTabSelection(binding.tabValid, listOf(binding.tabAll, binding.tabExpired))
            refreshVoucherList()
        }

        binding.tabExpired.setOnClickListener {
            currentTab = 2
            updateTabSelection(binding.tabExpired, listOf(binding.tabAll, binding.tabValid))
            refreshVoucherList()
        }

        // Set default tab state
        currentTab = 0
        updateTabSelection(binding.tabAll, listOf(binding.tabValid, binding.tabExpired))

        // 6. Highlight "Tài khoản" Bottom Navigation Tab
        highlightBottomTab(view)
        
        BottomNavHelper.setup(
            fragment = this,
            root = binding.root,
            activeTabId = R.id.nav_account
        ) { tabId -> BottomNavHelper.navigate(this, tabId) }
    }

    override fun observeViewModel() {
        // Observe database user gifts to dynamically merge redeemed vouchers into list
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllUserGifts().collect { dbGifts ->
                val mappedDbVouchers = dbGifts
                    .filter { it.giftType == "voucher_discount" || it.giftType == "voucher_freeship" }
                    .map { gift ->
                        val statusVal = computeStatusFromExpiry(gift.expiryDate)
                        VoucherItem(
                            id = "db_${gift.id}",
                            title = gift.title,
                            description = gift.description,
                            code = gift.code,
                            status = statusVal,
                            hsd = gift.expiryDate,
                            type = if (gift.giftType == "voucher_freeship") "free ship" else "discount",
                            fromGift = true,
                            quantity = null,
                            minOrderValue = gift.minOrderValue,
                            applicableProducts = gift.applicableProducts,
                            offerType = gift.offerType,
                            discountValue = gift.discountValue
                        )
                    }
                
                val activeSystem = systemVouchers.filter { it.id !in deletedSystemVoucherIds && it.id !in deletedSystemVoucherIdsStatic }
                allVouchers = activeSystem + mappedDbVouchers
                refreshVoucherList()
            }
        }
    }

    private fun refreshVoucherList() {
        val filteredList = when (currentTab) {
            0 -> allVouchers
            1 -> allVouchers.filter { it.status == "valid" || it.status == "expiring" }
            2 -> allVouchers.filter { it.status == "expired" }
            else -> allVouchers
        }
        val sortedList = filteredList.sortedWith(compareByDescending { it.status == "expiring" })
        adapter.updateList(sortedList)
        updateWarningBanner()
    }

    private fun updateWarningBanner() {
        val expiringCount = allVouchers.count { it.status == "expiring" }
        if (expiringCount > 0) {
            binding.bannerWarning.visibility = View.VISIBLE
            binding.tvWarningText.text = "$expiringCount voucher sắp hết hạn"
        } else {
            binding.bannerWarning.visibility = View.GONE
        }
    }

    private fun computeStatusFromExpiry(expiryStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val expiryDate = sdf.parse(expiryStr) ?: return "valid"
            
            val today = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.JUNE)
                set(Calendar.DAY_OF_MONTH, 11)
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

    private fun loadVouchersFromAssets(ctx: Context): List<VoucherItem> {
        return try {
            val jsonString = ctx.assets.open("vouchers.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<VoucherItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val hsdStr = obj.optString("hsd", "")
                val statusVal = computeStatusFromExpiry(hsdStr)
                
                list.add(
                    VoucherItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        code = obj.optString("code", ""),
                        status = statusVal,
                        hsd = hsdStr,
                        type = obj.optString("type", "discount"),
                        fromGift = obj.optBoolean("from-gift", false),
                        quantity = if (obj.has("quantity")) obj.getInt("quantity") else null,
                        minOrderValue = obj.optInt("minOrderValue", 0),
                        applicableProducts = obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        offerType = obj.optString("offerType", "fixed_amount"),
                        discountValue = obj.optInt("discountValue", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun updateTabSelection(selectedTab: TextView, inactiveTabs: List<TextView>) {
        selectedTab.setBackgroundResource(R.drawable.tab_active_bg)
        selectedTab.setTextColor(Color.WHITE)
        selectedTab.setTypeface(null, Typeface.BOLD)

        for (tab in inactiveTabs) {
            tab.setBackgroundResource(R.drawable.tab_inactive_bg)
            tab.setTextColor(Color.parseColor("#3E4D44"))
            tab.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun highlightBottomTab(view: View) {
        view.findViewById<ViewGroup>(R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? ImageView
            val label = navAccount.getChildAt(1) as? TextView
            icon?.setColorFilter(Color.parseColor("#677559"))
            label?.setTextColor(Color.parseColor("#677559"))
            label?.setTypeface(null, Typeface.BOLD)
        }
    }

    override fun onResume() {
        super.onResume()
        val activeSystem = systemVouchers.filter { it.id !in deletedSystemVoucherIds && it.id !in deletedSystemVoucherIdsStatic }
        val dbVouchers = allVouchers.filter { it.id.startsWith("db_") }
        allVouchers = activeSystem + dbVouchers
        refreshVoucherList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val deletedSystemVoucherIdsStatic = mutableSetOf<String>()
    }
}
