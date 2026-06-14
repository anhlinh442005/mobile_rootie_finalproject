package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
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

class CommunityAffiliateProductsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_affiliate_products, container, false)
        
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
        
        loadProductsData(view)
        
        return view
    }
    
    private fun loadProductsData(view: View) {
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
            
            data class ProductStats(val name: String, var count: Int, var commission: Long, val image: String, val price: Long)
            val productMap = mutableMapOf<String, ProductStats>()
            
            try {
                val jsonOrders = requireContext().assets.open("orders.json").bufferedReader().use { it.readText() }
                val ordersData = org.json.JSONObject(jsonOrders)
                val ordersArr = ordersData.optJSONArray("orders") ?: JSONArray()
                for (i in 0 until ordersArr.length()) {
                    val order = ordersArr.optJSONObject(i) ?: continue
                    val status = order.optString("status")
                    if (status != "Đã hủy") {
                        val items = order.optJSONArray("items") ?: continue
                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j) ?: continue
                            val pId = item.optString("productId")
                            val pName = item.optString("productName")
                            val pImg = item.optString("productImage")
                            val pPrice = item.optLong("price", 0L)
                            if (pId.isNotEmpty() && !productMap.containsKey(pId)) {
                                val resolvedImg = productImagesMap[pId] ?: pImg
                                productMap[pId] = ProductStats(pName, 0, 0L, resolvedImg, pPrice)
                            }
                        }
                    }
                }
            } catch(e: Exception) { e.printStackTrace() }
            
            for (i in 0 until orders.length()) {
                val order = orders.getJSONObject(i)
                val productName = order.optString("product_name")
                val productId = order.optString("product_id")
                val commission = order.optLong("commission")
                val productImage = productImagesMap[productId] ?: order.optString("product_image")
                
                val stats = productMap[productId]
                if (stats != null) {
                    stats.count += 1
                    stats.commission += commission
                } else {
                    productMap[productId] = ProductStats(productName, 1, commission, productImage, 0L)
                }
            }
            
            val prefs = requireContext().getSharedPreferences("AffiliatePrefs", android.content.Context.MODE_PRIVATE)
            val hiddenProducts = prefs.getStringSet("hiddenProducts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            
            val llProductsContainer = view.findViewById<LinearLayout>(R.id.llProductsContainer)
            llProductsContainer?.removeAllViews()
            
            for ((prodId, stats) in productMap) {
                if (hiddenProducts.contains(prodId)) continue
                
                val prodView = LayoutInflater.from(context).inflate(R.layout.com_item_affiliate_product_card, llProductsContainer, false)
                
                prodView.findViewById<TextView>(R.id.tvProductName).text = stats.name
                prodView.findViewById<TextView>(R.id.tvProductSold).text = "Đã bán\n${stats.count}"
                prodView.findViewById<TextView>(R.id.tvProductPrice)?.text = format.format(stats.price)
                prodView.findViewById<TextView>(R.id.tvProductCommission)?.text = "Hoa hồng: ${format.format(stats.commission)}đ"
                
                val ivProduct = prodView.findViewById<ImageView>(R.id.ivProductImage)
                if (stats.image.isNotEmpty()) {
                    ivProduct?.load(stats.image)
                }
                
                prodView.setOnClickListener {
                    val detailFragment = com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment()
                    parentFragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }

                val swDisplay = prodView.findViewById<android.widget.Switch>(R.id.swDisplay)
                swDisplay?.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!isChecked) {
                        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete, null)
                        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                            .setView(dialogView)
                            
                        val dialog = builder.create()
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        dialog.setCancelable(false)
                        
                        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
                            swDisplay.isChecked = true
                            dialog.dismiss()
                        }
                        
                        dialogView.findViewById<View>(R.id.btnConfirm).setOnClickListener {
                            llProductsContainer?.removeView(prodView)
                            hiddenProducts.add(prodId)
                            prefs.edit().putStringSet("hiddenProducts", hiddenProducts).apply()
                            dialog.dismiss()
                        }
                        
                        dialog.show()
                    }
                }
                
                llProductsContainer?.addView(prodView)
            }
            
            view.findViewById<View>(R.id.btnAddProduct)?.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, CommunityAddAffiliateProductsFragment())
                    .addToBackStack(null)
                    .commit()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
