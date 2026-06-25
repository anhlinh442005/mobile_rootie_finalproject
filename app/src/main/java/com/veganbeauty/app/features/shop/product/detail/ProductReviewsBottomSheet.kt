package com.veganbeauty.app.features.shop.product.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.veganbeauty.app.databinding.ShopBottomSheetReviewsBinding

class ProductReviewsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetReviewsBinding? = null
    private val binding get() = _binding!!

    private val allReviews = mutableListOf<ProductReview>()
    private lateinit var adapter: ShopReviewAdapter
    private var selectedStars = 0 // 0 means All

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val productId = arguments?.getString(ARG_PRODUCT_ID) ?: ""
        val productName = arguments?.getString(ARG_PRODUCT_NAME) ?: ""
        val category = arguments?.getString(ARG_CATEGORY) ?: ""

        val (rating, totalCount) = ProductReviewHelper.getRatingStats(productId)
        
        binding.tvAverageRating.text = String.format(java.util.Locale.US, "%.1f", rating)
        binding.tvTotalRatingCount.text = "$totalCount reviews"

        // Load initial reviews
        val initialReviews = ProductReviewHelper.getReviews(productId, productName, category)
        allReviews.addAll(initialReviews)
        
        updateProgressBars()

        adapter = ShopReviewAdapter(emptyList())
        binding.rvBottomSheetReviews.adapter = adapter
        
        // Setup initial display
        filterAndDisplayReviews()

        // Setup filter tabs listener
        binding.tabLayoutStarFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedStars = when (tab?.position) {
                    0 -> 0 // Tất cả
                    1 -> 5 // 5 Sao
                    2 -> 4 // 4 Sao
                    3 -> 3 // 3 Sao
                    4 -> 2 // 2 Sao
                    5 -> 1 // 1 Sao
                    else -> 0
                }
                filterAndDisplayReviews()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Setup load more button listener
        binding.btnLoadMoreReviews.setOnClickListener {
            // Generate 5 random reviews
            val newReviews = ProductReviewHelper.getRandomReviews(productName, category, 5)
            allReviews.addAll(newReviews)
            updateProgressBars()
            filterAndDisplayReviews()
        }
    }

    private fun updateProgressBars() {
        val total = allReviews.size
        if (total > 0) {
            val star5 = allReviews.count { it.rating == 5 }
            val star4 = allReviews.count { it.rating == 4 }
            val star3 = allReviews.count { it.rating == 3 }
            val star2 = allReviews.count { it.rating == 2 }
            val star1 = allReviews.count { it.rating == 1 }

            binding.pbStar5.progress = (star5 * 100) / total
            binding.pbStar4.progress = (star4 * 100) / total
            binding.pbStar3.progress = (star3 * 100) / total
            binding.pbStar2.progress = (star2 * 100) / total
            binding.pbStar1.progress = (star1 * 100) / total
        }
    }

    private fun filterAndDisplayReviews() {
        val filtered = if (selectedStars == 0) {
            allReviews
        } else {
            allReviews.filter { it.rating == selectedStars }
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            binding.rvBottomSheetReviews.visibility = View.GONE
            binding.tvEmptyReviews.visibility = View.VISIBLE
            binding.tvEmptyReviews.text = if (selectedStars == 0) {
                "Chưa có đánh giá nào cho sản phẩm này."
            } else {
                "Chưa có đánh giá $selectedStars sao nào cho sản phẩm này."
            }
            binding.btnLoadMoreReviews.visibility = View.GONE
        } else {
            binding.rvBottomSheetReviews.visibility = View.VISIBLE
            binding.tvEmptyReviews.visibility = View.GONE
            if (filtered.size >= 3) {
                binding.btnLoadMoreReviews.visibility = View.VISIBLE
            } else {
                binding.btnLoadMoreReviews.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ProductReviewsBottomSheet"
        
        private const val ARG_PRODUCT_ID = "arg_product_id"
        private const val ARG_PRODUCT_NAME = "arg_product_name"
        private const val ARG_CATEGORY = "arg_category"

        fun newInstance(productId: String, productName: String, category: String): ProductReviewsBottomSheet {
            return ProductReviewsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUCT_ID, productId)
                    putString(ARG_PRODUCT_NAME, productName)
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }
}
