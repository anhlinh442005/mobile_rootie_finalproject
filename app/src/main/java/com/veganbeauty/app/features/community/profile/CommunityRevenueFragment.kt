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
            val jsonStr = requireContext().assets.open("affiliate.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() == 0) return
            
            val data = jsonArray.getJSONObject(0)
            val newCustomers = data.optInt("new_customers", 0)
            
            val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
            symbols.groupingSeparator = '.'
            val format = DecimalFormat("#,###đ", symbols)
            
            // ── COMPUTE all metrics from order list ──────────────────────────
            val orders = data.optJSONArray("orders") ?: org.json.JSONArray()
            var totalRevenue = 0L          // Tổng giá trị đơn hàng (mọi trạng thái)
            var successCommission = 0L     // Hoa hồng từ đơn THÀNH CÔNG  → đây là số dư khả dụng
            var pendingCommission = 0L     // Hoa hồng từ đơn ĐANG XỬ LÝ → tạm tính
            var successfulOrders = 0

            for (i in 0 until orders.length()) {
                val order = orders.getJSONObject(i)
                val status = order.optString("status")
                val commission = order.optLong("commission", 0)
                val orderValue = order.optLong("order_value", 0)
                totalRevenue += orderValue
                when (status) {
                    "Thành công", "Đã duyệt" -> {
                        successCommission += commission
                        successfulOrders++
                    }
                    "Đang xử lý" -> {
                        pendingCommission += commission
                    }
                }
            }

            // Trừ đi phần đã rút ra khỏi số dư khả dụng
            val withdrawals = data.optJSONArray("withdrawals")
            var totalWithdrawn = 0L
            if (withdrawals != null) {
                for (i in 0 until withdrawals.length()) {
                    val wd = withdrawals.getJSONObject(i)
                    if (wd.optString("status") == "Đã chuyển") {
                        totalWithdrawn += wd.optLong("amount", 0)
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
            
            // Load products image map using categoryId as foreign key to match affiliate.json product_id
            val productImagesMap = mutableMapOf<String, String>()
            try {
                val jsonProducts = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
                val productsData = org.json.JSONObject(jsonProducts)
                val productsArr = productsData.optJSONArray("products") ?: org.json.JSONArray(jsonProducts)
                for (i in 0 until productsArr.length()) {
                    val p = productsArr.optJSONObject(i) ?: continue
                    val categoryIds = p.optJSONArray("categoryId")
                    val catId = if (categoryIds != null && categoryIds.length() > 0) categoryIds.optString(0) else ""
                    val img = p.optString("mainImage", p.optString("image", ""))
                    if (catId.isNotEmpty() && img.isNotEmpty()) {
                        productImagesMap[catId] = img
                    }
                    
                    // Also map by primary ID just in case
                    val id = p.optString("id", p.optString("_id"))
                    if (id.isNotEmpty() && img.isNotEmpty()) {
                        productImagesMap[id] = img
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            val affiliateOrders = data.optJSONArray("orders") ?: return
            
            val llProductsSoldContainer = view.findViewById<LinearLayout>(R.id.llProductsSoldContainer)
            llProductsSoldContainer?.removeAllViews()
            
            val llOrdersContainer = view.findViewById<LinearLayout>(R.id.llOrdersContainer)
            llOrdersContainer?.removeAllViews()
            
            val productMap = mutableMapOf<String, Triple<Int, Long, String>>() // name -> (count, commission, image)
            
            for (i in 0 until affiliateOrders.length()) {
                val order = affiliateOrders.getJSONObject(i)
                val orderId = order.optString("order_id")
                val orderDate = order.optString("order_date")
                val productName = order.optString("product_name")
                val productId = order.optString("product_id")
                val productImage = productImagesMap[productId] ?: order.optString("product_image")
                val orderValue = order.optLong("order_value")
                val commission = order.optLong("commission")
                val status = order.optString("status")
                
                // Aggregate for products list
                val existing = productMap[productName] ?: Triple(0, 0L, productImage)
                productMap[productName] = Triple(existing.first + 1, existing.second + commission, productImage)
                
                val rowView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_order, llOrdersContainer, false)
                
                rowView.findViewById<TextView>(R.id.tvOrderId).text = orderId
                rowView.findViewById<TextView>(R.id.tvOrderDate).text = orderDate.split(" ")[0]
                rowView.findViewById<TextView>(R.id.tvOrderValue).text = format.format(orderValue)
                
                val tvStatus = rowView.findViewById<TextView>(R.id.tvStatus)
                if (status == "Đã duyệt" || status == "Thành công") {
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
                if (productImage.isNotEmpty()) {
                    ivProductImage.load(productImage)
                }
                
                llOrdersContainer?.addView(rowView)
            }
            
            for ((prodName, stats) in productMap) {
                val prodView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_product, llProductsSoldContainer, false)
                prodView.findViewById<TextView>(R.id.tvProductName).text = prodName
                prodView.findViewById<TextView>(R.id.tvProductSold).text = "Đã bán: ${stats.first}"
                prodView.findViewById<TextView>(R.id.tvProductCommission).text = "Hoa hồng: ${format.format(stats.second)}"
                
                val ivProduct = prodView.findViewById<ImageView>(R.id.ivProduct)
                if (stats.third.isNotEmpty()) {
                    ivProduct.load(stats.third)
                }
                
                llProductsSoldContainer?.addView(prodView)
            }
            
            // Withdrawals
            val affiliateWithdrawals = data.optJSONArray("withdrawals")
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
