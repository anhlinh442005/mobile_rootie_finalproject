package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import com.veganbeauty.app.R
import coil.load

class CommunityRevenueFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_revenue, container, false)
        
        val ivBack = view.findViewById<ImageView>(R.id.ivBack)
        ivBack?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        view.findViewById<LinearLayout>(R.id.navOrders)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateOrdersFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<LinearLayout>(R.id.navProducts)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateProductsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<LinearLayout>(R.id.navWithdraw)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateWithdrawFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<TextView>(R.id.tvViewAllProducts)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateProductsFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<TextView>(R.id.tvViewAllOrders)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateOrdersFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<TextView>(R.id.tvViewAllWithdrawals)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateWithdrawFragment())
                .addToBackStack(null)
                .commit()
        }
        
        view.findViewById<TextView>(R.id.btnWithdraw)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CommunityAffiliateWithdrawFragment())
                .addToBackStack(null)
                .commit()
        }
        
        loadAffiliateData(view)
        
        return view
    }

    private fun loadAffiliateData(view: View) {
        try {
            val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
            symbols.groupingSeparator = '.'
            val format = DecimalFormat("#,###đ", symbols)
            
            // ── COMPUTE all metrics from order list ──────────────────────────
            val allOrders = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getAllOrders()
            val currentUserId = "test_001" // Or get from session
            val affiliateOrders = allOrders.filter { it.affiliate != null && it.affiliate.referrerUserId == currentUserId }
            
            var totalRevenue = 0L
            var successCommission = 0L
            var pendingCommission = 0L
            var successfulOrders = 0

            for (order in affiliateOrders) {
                val status = order.status
                val orderValue = order.totalAmount
                val commission = order.affiliate?.commissionAmount ?: 0L
                
                if (status != "Đã hủy") {
                    totalRevenue += orderValue
                }
                
                when (status) {
                    "Hoàn tất", "Thành công", "Đã duyệt" -> {
                        successCommission += commission
                        successfulOrders++
                    }
                    "Đang xử lý", "Chờ xác nhận", "Đang giao" -> {
                        pendingCommission += commission
                    }
                }
            }

            // Trừ đi phần đã rút ra khỏi số dư khả dụng
            var totalWithdrawn = 0L
            val jsonArray = com.veganbeauty.app.features.community.affiliate.AffiliateHelper.getAffiliateData(requireContext())
            var affiliateData = org.json.JSONObject()
            var newCustomers = 0
            if (jsonArray.length() > 0) {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.optString("userId") == currentUserId) {
                        affiliateData = obj
                        newCustomers = obj.optInt("newCustomers", 0)
                        break
                    }
                }
                val withdrawals = affiliateData.optJSONArray("withdrawals")
                if (withdrawals != null) {
                    for (i in 0 until withdrawals.length()) {
                        val wd = withdrawals.getJSONObject(i)
                        if (wd.optString("status") == "Đã chuyển") {
                            totalWithdrawn += wd.optLong("amount", 0)
                        }
                    }
                }
            }
            val availableBalance = (successCommission - totalWithdrawn).coerceAtLeast(0L)
            // ────────────────────────────────────────────────────────────────
            
            // Load user data
            var userName = "Linh Nguyễn"
            var avatarUrl = ""
            var userIdStr = "test_001"
            try {
                val jsonStrUser = requireContext().assets.open("users.json").bufferedReader().use { it.readText() }
                val userArray = JSONArray(jsonStrUser)
                for (i in 0 until userArray.length()) {
                    val u = userArray.getJSONObject(i)
                    if (u.optString("user_id") == "test_001" || u.optString("username") == "Test User") {
                        userName = u.optString("full_name", u.optString("username", "Linh Nguyễn"))
                        avatarUrl = u.optString("avatar", "")
                        userIdStr = u.optString("user_id", "test_001")
                        break
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            // Rank
            var totalPurchased = 0L
            try {
                val ordersJson = requireContext().assets.open("orders.json").bufferedReader().use { it.readText() }
                val ordersObj = org.json.JSONObject(ordersJson)
                val ordersArr = ordersObj.optJSONArray("orders") ?: org.json.JSONArray()
                for (i in 0 until ordersArr.length()) {
                    val order = ordersArr.getJSONObject(i)
                    if (order.optString("userId") == userIdStr && order.optString("status") != "Đã hủy") {
                        val items = order.optJSONArray("items") ?: org.json.JSONArray()
                        for (j in 0 until items.length()) {
                            val item = items.getJSONObject(j)
                            totalPurchased += (item.optLong("price", 0) * item.optLong("quantity", 1))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            var rankName = "Thành viên Đồng"
            var rankIcon = "🥉"
            if (totalPurchased >= 20_000_000) { rankName = "Thành viên Kim Cương"; rankIcon = "💎" }
            else if (totalPurchased >= 10_000_000) { rankName = "Thành viên Vàng"; rankIcon = "🥇" }
            else if (totalPurchased >= 5_000_000) { rankName = "Thành viên Bạc"; rankIcon = "🥈" }
            
            view.findViewById<TextView>(R.id.tvUserName)?.text = "Xin chào, $userName 🌿"
            view.findViewById<TextView>(R.id.tvUserRank)?.text = "$rankIcon $rankName"
            val ivAvatar = view.findViewById<ImageView>(R.id.ivAvatar)
            if (avatarUrl.isNotEmpty() && ivAvatar != null) {
                ivAvatar.load(avatarUrl) { transformations(coil.transform.CircleCropTransformation()) }
            }
            
            // Bind computed values to UI
            view.findViewById<TextView>(R.id.tvTotalRevenue)?.text = format.format(totalRevenue)
            view.findViewById<TextView>(R.id.tvPendingCommission)?.text = format.format(pendingCommission)
            view.findViewById<TextView>(R.id.tvTotalOrders)?.text = successfulOrders.toString()
            view.findViewById<TextView>(R.id.tvNewCustomers)?.text = newCustomers.toString()
            view.findViewById<TextView>(R.id.tvAvailableBalance)?.text = format.format(availableBalance)
            view.findViewById<TextView>(R.id.tvWithdrawBalance)?.text = format.format(availableBalance)
            
            val allProducts = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getAllProducts()
            
            val llProductsSoldContainer = view.findViewById<LinearLayout>(R.id.llProductsSoldContainer)
            llProductsSoldContainer?.removeAllViews()
            
            val llOrdersContainer = view.findViewById<LinearLayout>(R.id.llOrdersContainer)
            llOrdersContainer?.removeAllViews()
            
            val productMap = mutableMapOf<String, Triple<Int, Long, String>>() // id -> (count, commission, image)
            val productNameMap = mutableMapOf<String, String>() // id -> name
            
            for (order in affiliateOrders) {
                if (order.status == "Đã hủy") continue
                val orderId = order.orderId
                val orderDate = order.orderDate
                val orderValue = order.totalAmount
                val status = order.status
                val commission = order.affiliate?.commissionAmount ?: 0L
                
                val rowView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_order, llOrdersContainer, false)
                
                rowView.findViewById<TextView>(R.id.tvOrderId).text = orderId
                rowView.findViewById<TextView>(R.id.tvOrderDate).text = orderDate.split(" ")[0]
                rowView.findViewById<TextView>(R.id.tvOrderValue).text = format.format(orderValue)
                
                val tvStatus = rowView.findViewById<TextView>(R.id.tvStatus)
                if (status == "Hoàn tất" || status == "Đã duyệt" || status == "Thành công") {
                    tvStatus.text = "Thành công"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#6E846A"))
                    tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAF1E7"))
                    tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                } else {
                    tvStatus.text = "Đang xử lý"
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                    tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
                    tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
                
                val ivProductImage = rowView.findViewById<ImageView>(R.id.ivProductImage)
                if (order.items.isNotEmpty()) {
                    val firstItem = order.items.first()
                    val pData = allProducts.find { it.id == firstItem.productId }
                    val productImage = pData?.mainImage ?: ""
                    if (productImage.isNotEmpty()) {
                        ivProductImage.load(productImage)
                    }
                }
                
                llOrdersContainer?.addView(rowView)
                
                for (item in order.items) {
                    val productId = item.productId
                    val pData = allProducts.find { it.id == productId }
                    
                    if (pData != null) {
                        val itemComm = if (order.items.isNotEmpty()) commission / order.items.size else 0L
                        val existing = productMap[productId] ?: Triple(0, 0L, pData.mainImage)
                        productMap[productId] = Triple(existing.first + item.quantity, existing.second + itemComm, pData.mainImage)
                        productNameMap[productId] = pData.name
                    }
                }
            }
            
            for ((prodId, stats) in productMap) {
                val pName = productNameMap[prodId] ?: "Sản phẩm"
                val prodView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_product, llProductsSoldContainer, false)
                prodView.findViewById<TextView>(R.id.tvProductName).text = pName
                prodView.findViewById<TextView>(R.id.tvProductSold).text = "Đã bán: ${stats.first}"
                prodView.findViewById<TextView>(R.id.tvProductCommission).text = "Hoa hồng: ${format.format(stats.second)}"
                
                val ivProduct = prodView.findViewById<ImageView>(R.id.ivProduct)
                if (stats.third.isNotEmpty()) {
                    ivProduct.load(stats.third)
                }
                
                llProductsSoldContainer?.addView(prodView)
            }
            
            // Withdrawals
            val affiliateWithdrawals = affiliateData.optJSONArray("withdrawals")
            val llWithdrawalsContainer = view.findViewById<LinearLayout>(R.id.llWithdrawalsContainer)
            llWithdrawalsContainer?.removeAllViews()
            
            if (affiliateWithdrawals != null) {
                for (i in 0 until affiliateWithdrawals.length()) {
                    val wd = affiliateWithdrawals.optJSONObject(i) ?: continue
                    val wdDate = wd.optString("date")
                    val wdAmount = wd.optLong("amount")
                    val wdStatus = wd.optString("status")
                    
                    val wdView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_withdrawal, llWithdrawalsContainer, false)
                    wdView.findViewById<TextView>(R.id.tvWithdrawDate).text = wdDate
                    wdView.findViewById<TextView>(R.id.tvWithdrawAmount).text = format.format(wdAmount)
                    
                    val tvStatus = wdView.findViewById<TextView>(R.id.tvWithdrawStatus)
                    if (wdStatus == "Đã chuyển") {
                        tvStatus.text = "Đã chuyển"
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#6E846A"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAF1E7"))
                    } else {
                        tvStatus.text = wdStatus
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
                    }
                    
                    llWithdrawalsContainer?.addView(wdView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
