package com.veganbeauty.app.features.community.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.veganbeauty.app.R
import coil.load
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CommunityAddAffiliateProductsFragment : Fragment() {

    private val allProducts = mutableListOf<JSONObject>()
    private val purchasedProductIds = mutableSetOf<String>()
    private val showcasedProductIds = mutableSetOf<String>()
    
    private var currentFilter = "AVAILABLE" // AVAILABLE, ALL, FACE, BODY, HAIR
    
    private lateinit var llAddProductsContainer: LinearLayout
    private lateinit var format: DecimalFormat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.com_fragment_affiliate_add_products, container, false)
        
        view.findViewById<ImageView>(R.id.ivBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        llAddProductsContainer = view.findViewById(R.id.llAddProductsContainer)
        
        val symbols = DecimalFormatSymbols(Locale("vi", "VN"))
        symbols.groupingSeparator = '.'
        format = DecimalFormat("#,###đ", symbols)
        
        setupChips(view)
        loadData()
        renderProducts()
        
        return view
    }
    
    private fun setupChips(view: View) {
        val chipAvailable = view.findViewById<TextView>(R.id.chipAvailable)
        val chipAll = view.findViewById<TextView>(R.id.chipAll)
        val chipFace = view.findViewById<TextView>(R.id.chipFace)
        val chipBody = view.findViewById<TextView>(R.id.chipBody)
        val chipHair = view.findViewById<TextView>(R.id.chipHair)
        
        val chips = listOf(chipAvailable, chipAll, chipFace, chipBody, chipHair)
        val filters = listOf("AVAILABLE", "ALL", "FACE", "BODY", "HAIR")
        
        for (i in chips.indices) {
            val chip = chips[i] ?: continue
            val filter = filters[i]
            
            chip.setOnClickListener {
                currentFilter = filter
                
                // Update UI
                for (c in chips) {
                    if (c == null) continue
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.com_bg_chip_selected)
                        c.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6E846A"))
                        c.setTextColor(Color.WHITE)
                    } else {
                        c.setBackgroundResource(R.drawable.com_bg_btn_outline)
                        c.backgroundTintList = null
                        c.setTextColor(Color.BLACK)
                    }
                }
                
                renderProducts()
            }
        }
    }
    
    private fun loadData() {
        try {
            val purchasedItemsMap = mutableMapOf<String, JSONObject>()
            
            val jsonOrders = requireContext().assets.open("orders.json").bufferedReader().use { it.readText() }
            val ordersData = JSONObject(jsonOrders)
            val ordersArr = ordersData.optJSONArray("orders") ?: JSONArray()
            for (i in 0 until ordersArr.length()) {
                val order = ordersArr.optJSONObject(i) ?: continue
                val status = order.optString("status")
                if (status != "Đã hủy") {
                    val items = order.optJSONArray("items") ?: continue
                    for (j in 0 until items.length()) {
                        val item = items.optJSONObject(j) ?: continue
                        val pId = item.optString("productId")
                        if (pId.isNotEmpty()) {
                            purchasedProductIds.add(pId)
                            if (!purchasedItemsMap.containsKey(pId)) {
                                val pseudoP = JSONObject()
                                pseudoP.put("id", pId)
                                pseudoP.put("name", item.optString("productName", "Sản phẩm Rootie"))
                                pseudoP.put("mainImage", item.optString("productImage", ""))
                                pseudoP.put("price", item.optLong("price", 0L))
                                pseudoP.put("category", "")
                                purchasedItemsMap[pId] = pseudoP
                            }
                        }
                    }
                }
            }
            
            // Load showcased products
            val affiliateArr = com.veganbeauty.app.features.community.affiliate.AffiliateHelper.getAffiliateData(requireContext())
            if (affiliateArr.length() > 0) {
                val affData = affiliateArr.getJSONObject(0)
                val affProducts = affData.optJSONArray("products") ?: JSONArray()
                for (i in 0 until affProducts.length()) {
                    val ap = affProducts.optJSONObject(i) ?: continue
                    val pId = ap.optString("id")
                    if (pId.isNotEmpty()) showcasedProductIds.add(pId)
                }
            }

            // Load all products
            val jsonProducts = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
            val productsData = JSONObject(jsonProducts)
            val productsArr = productsData.optJSONArray("products") ?: JSONArray(jsonProducts)
            
            allProducts.clear()
            for (i in 0 until productsArr.length()) {
                val p = productsArr.optJSONObject(i)
                if (p != null) {
                    val id = p.optString("id", p.optString("_id", ""))
                    purchasedItemsMap.remove(id)
                    allProducts.add(p)
                }
            }
            
            allProducts.addAll(purchasedItemsMap.values)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun renderProducts() {
        llAddProductsContainer.removeAllViews()
        
        val filtered = allProducts.filter { p ->
            val id = p.optString("id", p.optString("_id", ""))
            val subcats = p.optJSONArray("subcategory")?.toString()?.lowercase() ?: ""
            val cats = p.optString("category").lowercase()
            
            when (currentFilter) {
                "AVAILABLE" -> purchasedProductIds.contains(id)
                "ALL" -> true
                "FACE" -> subcats.contains("mặt") || cats.contains("mặt")
                "BODY" -> subcats.contains("cơ thể") || cats.contains("cơ thể") || subcats.contains("body")
                "HAIR" -> subcats.contains("tóc") || cats.contains("tóc") || subcats.contains("hair")
                else -> true
            }
        }
        
        for (p in filtered) {
            val id = p.optString("id", p.optString("_id", ""))
            val name = p.optString("name", "Sản phẩm")
            val img = p.optString("mainImage", p.optString("image", ""))
            val price = p.optLong("price", 0L)
            val commission = (price * 0.08).toLong()
            
            val isPurchased = purchasedProductIds.contains(id)
            
            val prefs = requireContext().getSharedPreferences("AffiliatePrefs", android.content.Context.MODE_PRIVATE)
            val hiddenProducts = prefs.getStringSet("hiddenProducts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val isShowcased = isPurchased && !hiddenProducts.contains(id)
            
            val item = LayoutInflater.from(context).inflate(R.layout.com_item_affiliate_add_product_card, llAddProductsContainer, false)
            
            item.findViewById<TextView>(R.id.tvProductName)?.text = name
            item.findViewById<TextView>(R.id.tvProductPrice)?.text = format.format(price)
            item.findViewById<TextView>(R.id.tvProductCommission)?.text = "Hoa hồng: ${format.format(commission)}"
            
            val ivProduct = item.findViewById<ImageView>(R.id.ivProductImage)
            if (img.isNotEmpty()) {
                ivProduct?.load(img)
            }
            
            val vOverlay = item.findViewById<View>(R.id.vOverlay)
            val btnAdd = item.findViewById<TextView>(R.id.btnAdd)
            
            if (isShowcased) {
                vOverlay?.visibility = View.GONE
                btnAdd?.text = "Đã trưng bày"
                btnAdd?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                btnAdd?.isEnabled = false
            } else if (isPurchased) {
                vOverlay?.visibility = View.GONE
                btnAdd?.text = "Trưng bày"
                btnAdd?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6E846A"))
                btnAdd?.isEnabled = true
                btnAdd?.setOnClickListener {
                    btnAdd.text = "Đã trưng bày"
                    btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
                    btnAdd.isEnabled = false
                    
                    val p = requireContext().getSharedPreferences("AffiliatePrefs", android.content.Context.MODE_PRIVATE)
                    val hSet = p.getStringSet("hiddenProducts", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    hSet.remove(id)
                    p.edit().putStringSet("hiddenProducts", hSet).apply()
                    
                    Toast.makeText(context, "Đã thêm sản phẩm vào cửa hàng", Toast.LENGTH_SHORT).show()
                }
            } else {
                vOverlay?.visibility = View.VISIBLE
                btnAdd?.text = "Trưng bày"
                btnAdd?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#6E846A"))
                btnAdd?.setOnClickListener {
                    Toast.makeText(context, "Bạn cần mua sản phẩm này để có thể gắn link affiliate", Toast.LENGTH_SHORT).show()
                }
            }
            
            item.setOnClickListener {
                val detailFragment = com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment()
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }
            
            llAddProductsContainer.addView(item)
        }
    }
}
