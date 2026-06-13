package com.veganbeauty.app.features.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.CommunityProduct
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.veganbeauty.app.databinding.ComFragmentShowcaseBinding
import com.veganbeauty.app.databinding.ComItemShowcaseProductBinding
import java.text.NumberFormat
import java.util.Locale

class CommunityShowcaseFragment : Fragment() {

    private var _binding: ComFragmentShowcaseBinding? = null
    private val binding get() = _binding!!

    private var userId: String? = null
    private var avatarUrl: String? = null
    private var userName: String? = null
    private var coverUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString("USER_ID")
            avatarUrl = it.getString("AVATAR_URL")
            userName = it.getString("USER_NAME")
            coverUrl = it.getString("COVER_URL")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ComFragmentShowcaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup Header Info
        val titleText = userName ?: "Na Na"
        binding.tvShowcaseTitle.text = "Trang trưng bày của $titleText"
        
        binding.ivAvatar.load(avatarUrl ?: "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg") {
            crossfade(true)
            transformations(CircleCropTransformation())
            error(R.drawable.img_avatar)
        }
        
        binding.ivFilterSort.setOnClickListener {
            val bottomSheet = CommunitySortBottomSheet(currentSort = 0) { selectedSort ->
                // Do sorting logic if needed, or just update UI
                // Toast.makeText(requireContext(), "Sorted: $selectedSort", Toast.LENGTH_SHORT).show()
            }
            bottomSheet.show(parentFragmentManager, "CommunitySortBottomSheet")
        }
        
        binding.ivCover.load(coverUrl ?: avatarUrl ?: "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg") {
            crossfade(true)
            error(android.R.color.darker_gray)
        }

        // Read data
        val ctx = requireContext()
        val jsonReader = LocalJsonReader(ctx)
        
        // Followers count (Mocking 867 if not test_001, otherwise real from social)
        val socialData = jsonReader.getSocialDataForUser(userId ?: "test_001")
        val followersCount = socialData["followers"]?.size ?: 867
        binding.tvFollowers.text = followersCount.toString()

        // Read products from user_pro_display.json
        val showcaseData = jsonReader.getShowcaseProductsForUser(userId ?: "test_001")
        val productIds = showcaseData ?: emptyList()
        binding.tvProductCount.text = productIds.size.toString()

        val allProducts = jsonReader.getProducts()
        val displayProducts = allProducts.filter { productIds.contains(it.id) }
        
        // Fallback if no products mapped, show some random ones
        val finalProducts = if (displayProducts.isNotEmpty()) {
            displayProducts
        } else {
            val fallback = allProducts.shuffled().take(5)
            binding.tvProductCount.text = fallback.size.toString()
            fallback
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(context)
        binding.rvProducts.adapter = ShowcaseProductAdapter(finalProducts) { product, view ->
            val allEntityProducts = jsonReader.getAllProducts()
            val entityProduct = allEntityProducts.find { it.id == product.id }
            if (entityProduct != null) {
                com.veganbeauty.app.features.shop.product.CartHelper.addToCart(ctx, viewLifecycleOwner.lifecycleScope, entityProduct, 1)
                animateAddToCart(view, binding.ivCart)
            }
        }
        
        // Observe Cart Items
        val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            db.cartDao().getAllCartItems().collect { items ->
                val count = items.sumOf { it.quantity }
                if (count > 0) {
                    binding.tvCartBadge.visibility = View.VISIBLE
                    binding.tvCartBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    binding.tvCartBadge.visibility = View.GONE
                }
            }
        }
        
        // Link the cart icon to open the CartBottomSheet
        binding.ivCart.setOnClickListener {
            val bottomSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment()
            bottomSheet.show(parentFragmentManager, "CartBottomSheet")
        }
    }

    private fun animateAddToCart(startView: View, targetView: View) {
        val rootLayout = binding.root as ViewGroup
        val startLoc = IntArray(2)
        startView.getLocationInWindow(startLoc)
        val targetLoc = IntArray(2)
        targetView.getLocationInWindow(targetLoc)
        
        val rootLoc = IntArray(2)
        rootLayout.getLocationInWindow(rootLoc)

        val startX = startLoc[0] - rootLoc[0].toFloat()
        val startY = startLoc[1] - rootLoc[1].toFloat()
        val targetX = targetLoc[0] - rootLoc[0].toFloat()
        val targetY = targetLoc[1] - rootLoc[1].toFloat()

        val flyingIcon = android.widget.ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_cart)
            layoutParams = android.view.ViewGroup.LayoutParams(
                startView.width, startView.height
            )
            x = startX
            y = startY
            elevation = 100f
            // To ensure it shows above everything
            translationZ = 100f
        }
        
        rootLayout.addView(flyingIcon)

        flyingIcon.animate()
            .x(targetX)
            .y(targetY)
            .scaleX(0.2f)
            .scaleY(0.2f)
            .setDuration(500)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction {
                rootLayout.removeView(flyingIcon)
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ShowcaseProductAdapter(
    private val products: List<CommunityProduct>,
    private val onAddToCart: (CommunityProduct, View) -> Unit
) :
    RecyclerView.Adapter<ShowcaseProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ComItemShowcaseProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ComItemShowcaseProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.tvProductName.text = product.name
        
        val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        holder.binding.tvPrice.text = format.format(product.price)
        
        // Fake sold count and rating
        holder.binding.tvSold.text = "Đã bán ${product.sold}"
        
        if (product.rating > 0) {
            holder.binding.tvRating.visibility = View.VISIBLE
            holder.binding.ivStar.visibility = View.VISIBLE
            holder.binding.tvRating.text = String.format("%.1f", product.rating)
        } else {
            holder.binding.tvRating.visibility = View.GONE
            holder.binding.ivStar.visibility = View.GONE
        }

        if (product.mainImage.isNotEmpty()) {
            holder.binding.ivProductImage.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }
        
        // Discount badge logic based on originalPrice
        if (product.originalPrice != null && product.originalPrice > product.price) {
            val discount = ((product.originalPrice - product.price).toDouble() / product.originalPrice * 100).toInt()
            holder.binding.tvDiscountBadge.visibility = View.VISIBLE
            holder.binding.tvDiscountBadge.text = "-$discount%"
        } else {
            holder.binding.tvDiscountBadge.visibility = View.GONE
        }
        
        // Shop Name
        holder.binding.tvShopName.text = "Cocoon Vietnam >"

        holder.binding.tvBuy.setOnClickListener {
            onAddToCart(product, it)
        }
        holder.binding.ivAddToCart.setOnClickListener {
            onAddToCart(product, it)
        }
    }

    override fun getItemCount() = products.size
}
