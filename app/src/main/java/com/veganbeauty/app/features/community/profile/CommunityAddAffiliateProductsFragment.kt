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
            // ── STEP 1: Get current userId from session ───────────────────────────────
            val currentEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
            var currentUserId = "test_001"
            try {
                val usersStr = requireContext().assets.open("users.json").bufferedReader().use { it.readText() }
                val usersArr = org.json.JSONArray(usersStr)
                for (i in 0 until usersArr.length()) {
                    val obj = usersArr.getJSONObject(i)
                    if (obj.optString("email") == currentEmail) {
                        currentUserId = obj.optString("user_id", "test_001")
                        break
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            // ── STEP 2: Load ONLY completed orders (Hoàn tất) of THIS user ─────────────
            purchasedProductIds.clear()
            val orders = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getAllOrders()
            for (order in orders) {
                // STRICT: only Hoàn tất, only this user's own orders
                if (order.userId == currentUserId && order.status == "Hoàn tất") {
                    for (item in order.items) {
                        if (item.productId.isNotEmpty()) purchasedProductIds.add(item.productId)
                    }
                }
            }

            // ── STEP 3: Load products from products.json, filter to purchased ones only ─
            val jsonProducts = requireContext().assets.open("products.json").bufferedReader().use { it.readText() }
            val productsData = JSONObject(jsonProducts)
            val productsArr = productsData.optJSONArray("products") ?: JSONArray()

            allProducts.clear()
            for (i in 0 until productsArr.length()) {
                val p = productsArr.optJSONObject(i) ?: continue
                val id = p.optString("id", p.optString("_id", ""))
                // Only include products that this user has actually purchased (Hoàn tất)
                if (purchasedProductIds.contains(id)) {
                    allProducts.add(p)
                }
            }
            // Note: products NOT found in products.json are intentionally excluded (data integrity)

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
            
            val currentUserId = "test_001"
            val helper = com.veganbeauty.app.features.community.affiliate.AffiliateProductsHelper
            val isDisplayed = helper.isProductDisplayed(requireContext(), currentUserId, id)
            val isShowcased = isPurchased && isDisplayed
            
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
                    helper.setProductDisplayed(requireContext(), currentUserId, id, true)
                    
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
