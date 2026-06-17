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

        view.findViewById<ImageView>(R.id.ivNotification)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.main_container, com.veganbeauty.app.features.community.notification.CommunityNotificationFragment())
                .addToBackStack(null)
                .commit()
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
            val affiliateOrders = allOrders.filter { it.isAffiliate && it.affiliate?.referrerUserId == currentUserId }
            
            var totalRevenue = 0L
            var totalCommission = 0L
            var availableBalance = 0L
            var pendingBalance = 0L
            var successfulOrdersCount = 0
            var pendingOrdersCount = 0
            val newCustomerIds = mutableSetOf<String>()

            for (order in affiliateOrders) {
                totalRevenue += order.totalAmount
                val commission = order.affiliate?.commissionAmount ?: 0L
                totalCommission += commission
                newCustomerIds.add(order.userId)
                
                if (order.affiliate?.commissionStatus == "confirmed") {
                    availableBalance += commission
                    successfulOrdersCount++
                } else if (order.affiliate?.commissionStatus == "pending") {
                    pendingBalance += commission
                    pendingOrdersCount++
                }
            }

            // Bind computed values to UI
            view.findViewById<TextView>(R.id.tvTotalRevenue)?.text = format.format(totalRevenue)
            view.findViewById<TextView>(R.id.tvPendingCommission)?.text = format.format(pendingBalance)
            view.findViewById<TextView>(R.id.tvTotalOrders)?.text = successfulOrdersCount.toString()
            view.findViewById<TextView>(R.id.tvNewCustomers)?.text = newCustomerIds.size.toString()
            view.findViewById<TextView>(R.id.tvAvailableBalance)?.text = format.format(availableBalance)
            view.findViewById<TextView>(R.id.tvWithdrawBalance)?.text = format.format(availableBalance)
            
            val allUsers = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getUsers()
            val currentUser = allUsers.find { it.user_id == currentUserId }
            if (currentUser != null) {
                view.findViewById<TextView>(R.id.tvUserName)?.text = "Xin chào, ${currentUser.full_name} 🌿"
                val ivAvatar = view.findViewById<ImageView>(R.id.ivAvatar)
                if (ivAvatar != null && !currentUser.avatar.isNullOrEmpty()) {
                    ivAvatar.load(currentUser.avatar)
                }
            }
            
            val allProducts = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getAllProducts()
            
            val llProductsSoldContainer = view.findViewById<LinearLayout>(R.id.llProductsSoldContainer)
            llProductsSoldContainer?.removeAllViews()
            
            val llOrdersContainer = view.findViewById<LinearLayout>(R.id.llOrdersContainer)
            llOrdersContainer?.removeAllViews()
            
            val productMap = mutableMapOf<String, Triple<Int, Long, String>>() // id -> (count, commission, image)
            val productNameMap = mutableMapOf<String, String>() // id -> name
            
            for (order in affiliateOrders) {
                if (order.status == "Đã hủy") continue
                val affiliateId = order.affiliate?.affiliate_id ?: order.id
                val orderDate = order.orderDate
                val orderValue = order.totalAmount
                val status = order.status
                val commission = order.affiliate?.commissionAmount ?: 0L
                
                val rowView = LayoutInflater.from(context).inflate(R.layout.com_item_revenue_order, llOrdersContainer, false)
                
                rowView.findViewById<TextView>(R.id.tvOrderId).text = affiliateId
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
                    val productImage = firstItem.productImage
                    if (productImage.isNotEmpty()) {
                        ivProductImage.load(productImage)
                    }
                }
                
                llOrdersContainer?.addView(rowView)
                
                for (item in order.items) {
                    val productId = item.productId
                    val name = item.productName
                    val image = item.productImage
                    
                    val itemComm = if (order.items.isNotEmpty()) commission / order.items.size else 0L
                    val existing = productMap[productId] ?: Triple(0, 0L, image)
                    productMap[productId] = Triple(existing.first + item.quantity, existing.second + itemComm, image)
                    productNameMap[productId] = name
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
            val affiliateWithdrawals = org.json.JSONArray()
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
