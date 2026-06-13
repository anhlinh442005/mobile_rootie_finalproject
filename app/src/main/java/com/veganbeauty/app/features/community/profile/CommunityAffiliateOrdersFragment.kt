package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import coil.load
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CommunityAffiliateOrdersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_affiliate_orders, container, false)
        
        view.findViewById<LinearLayout>(R.id.navOverview)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<LinearLayout>(R.id.navOrders)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateOrdersFragment()).commit()
        }
        view.findViewById<LinearLayout>(R.id.navProducts)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateProductsFragment()).commit()
        }
        view.findViewById<LinearLayout>(R.id.navWithdraw)?.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.main_container, CommunityAffiliateWithdrawFragment()).commit()
        }
        
        loadOrdersData(view)
        
        return view
    }
    
    private fun loadOrdersData(view: View) {
        try {
            val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
            symbols.groupingSeparator = '.'
            val format = DecimalFormat("#,###đ", symbols)
            
            val jsonProducts = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
            val productsData = org.json.JSONObject(jsonProducts)
            val productsArr = productsData.optJSONArray("products") ?: org.json.JSONArray(jsonProducts)
            val productImagesMap = mutableMapOf<String, String>()
            for (i in 0 until productsArr.length()) {
                val p = productsArr.optJSONObject(i) ?: continue
                val categoryIds = p.optJSONArray("categoryId")
                val catId = if (categoryIds != null && categoryIds.length() > 0) categoryIds.optString(0) else ""
                val img = p.optString("mainImage", p.optString("image", ""))
                if (catId.isNotEmpty() && img.isNotEmpty()) {
                    productImagesMap[catId] = img
                }
                
                val id = p.optString("id", p.optString("_id"))
                if (id.isNotEmpty() && img.isNotEmpty()) {
                    productImagesMap[id] = img
                }
            }
            
            val jsonStr = requireContext().assets.open("affiliate.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() == 0) return
            
            val data = jsonArray.getJSONObject(0)
            val orders = data.optJSONArray("orders") ?: return
            
            val rvOrders = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvOrders)
            rvOrders.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            
            val orderList = mutableListOf<org.json.JSONObject>()
            for (i in 0 until orders.length()) {
                orderList.add(orders.getJSONObject(i))
            }
            
            rvOrders.adapter = AffiliateOrderAdapter(orderList, productImagesMap, format)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    inner class AffiliateOrderAdapter(
        private val items: List<org.json.JSONObject>,
        private val productImagesMap: Map<String, String>,
        private val format: DecimalFormat
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<AffiliateOrderAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val ivProduct1: ImageView = itemView.findViewById(R.id.ivProduct1)
            val ivProduct2: ImageView = itemView.findViewById(R.id.ivProduct2)
            val ivProduct3: ImageView = itemView.findViewById(R.id.ivProduct3)
            val tvMoreImages: TextView = itemView.findViewById(R.id.tvMoreImages)
            val tvTotalValue: TextView = itemView.findViewById(R.id.tvTotalValue)
            val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
            val tvCustomer: TextView = itemView.findViewById(R.id.tvCustomer)
            val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
            val tvOrderValue: TextView = itemView.findViewById(R.id.tvOrderValue)
            val tvCommission: TextView = itemView.findViewById(R.id.tvCommission)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.com_item_affiliate_order_detail, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = items[position]
            val orderId = order.optString("order_id")
            val orderDate = order.optString("order_date")
            val customer = order.optString("customer")
            val orderValue = order.optLong("order_value")
            val commission = order.optLong("commission")
            val status = order.optString("status")
            val productId = order.optString("product_id")
            val productImage = productImagesMap[productId] ?: order.optString("product_image")
            
            holder.tvOrderId.text = orderId
            
            if (status == "Đã duyệt" || status == "Thành công") {
                holder.tvStatus.text = "Thành công"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#56694E"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EAF1E7"))
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.tvStatus.compoundDrawablePadding = 0
            } else {
                holder.tvStatus.text = "Đang xử lý"
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
                holder.tvStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.tvStatus.compoundDrawablePadding = 0
            }
            
            holder.ivProduct1.visibility = View.VISIBLE
            if (productImage.isNotEmpty()) {
                holder.ivProduct1.load(productImage)
            }
            
            // Faking item count logic for UI presentation
            val itemCount = if (orderValue > 200000) 2 else 1
            holder.tvItemCount.text = "$itemCount sản phẩm"
            
            if (itemCount > 1) {
                holder.ivProduct2.visibility = View.VISIBLE
                if (productImage.isNotEmpty()) {
                    holder.ivProduct2.load(productImage)
                }
            } else {
                holder.ivProduct2.visibility = View.GONE
            }
            
            holder.ivProduct3.visibility = View.GONE
            holder.tvMoreImages.visibility = View.GONE
            
            holder.tvTotalValue.text = format.format(orderValue)
            holder.tvCustomer.text = customer
            holder.tvOrderDate.text = if (orderDate.contains(":")) orderDate else "$orderDate 10:30"
            holder.tvOrderValue.text = format.format(orderValue)
            holder.tvCommission.text = format.format(commission)
        }
    }
}
