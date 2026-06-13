package com.veganbeauty.app.features.shop.product.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.ShopBottomSheetReviewsBinding

class ProductReviewsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetReviewsBinding? = null
    private val binding get() = _binding!!

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

        val reviews = ProductReviewHelper.getReviews(productId, productName, category)
        
        // Calculate progress percentages based on generated reviews
        val total = reviews.size
        if (total > 0) {
            val star5 = reviews.count { it.rating == 5 }
            val star4 = reviews.count { it.rating == 4 }
            val star3 = reviews.count { it.rating == 3 }
            val star2 = reviews.count { it.rating == 2 }
            val star1 = reviews.count { it.rating == 1 }

            binding.pbStar5.progress = (star5 * 100) / total
            binding.pbStar4.progress = (star4 * 100) / total
            binding.pbStar3.progress = (star3 * 100) / total
            binding.pbStar2.progress = (star2 * 100) / total
            binding.pbStar1.progress = (star1 * 100) / total
        }

        val adapter = ShopReviewAdapter(reviews)
        binding.rvBottomSheetReviews.adapter = adapter
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
