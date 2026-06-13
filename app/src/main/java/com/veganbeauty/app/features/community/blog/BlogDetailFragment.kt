package com.veganbeauty.app.features.community.blog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import com.veganbeauty.app.R

class BlogDetailFragment : Fragment() {

    companion object {
        private const val ARG_BLOG_POST = "blog_post"

        fun newInstance(post: BlogPost): BlogDetailFragment {
            val fragment = BlogDetailFragment()
            val args = Bundle()
            args.putSerializable(ARG_BLOG_POST, post)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_blog_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val post = arguments?.getSerializable(ARG_BLOG_POST) as? BlogPost ?: return

        view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set Primary Image
        if (post.imageUrl.isNotEmpty()) {
            view.findViewById<ImageView>(R.id.ivPrimaryImage).load(post.imageUrl) {
                crossfade(true)
            }
        }

        // Set Texts
        view.findViewById<TextView>(R.id.tvCategory).text = post.category
        view.findViewById<TextView>(R.id.tvTitle).text = post.title
        view.findViewById<TextView>(R.id.tvDate).text = post.date
        view.findViewById<TextView>(R.id.tvShortDescription).text = post.description

        if (post.content.isNotEmpty()) {
            val tvContent = view.findViewById<TextView>(R.id.tvContent)
            // Remove image tags from HTML content
            val noImagesHtml = post.content.replace(Regex("<img[^>]*>"), "")
            tvContent.text = androidx.core.text.HtmlCompat.fromHtml(noImagesHtml, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT)
        } else {
            view.findViewById<View>(R.id.tvContent).parent?.let {
                (it as View).visibility = android.view.View.GONE
            }
        }

        // Set Doctor Info
        if (post.doctorName.isNotEmpty()) {
            view.findViewById<TextView>(R.id.tvDoctorName).text = post.doctorName
            view.findViewById<TextView>(R.id.tvDoctorBio).text = post.doctorBio
            if (post.doctorAvatar.isNotEmpty()) {
                view.findViewById<ImageView>(R.id.ivDoctorAvatar).load(post.doctorAvatar) {
                    crossfade(true)
                    error(R.drawable.img_avatar)
                }
            }
        } else {
            // Hide Doctor Block if no data
            view.findViewById<View>(R.id.tvDoctorName).parent.parent.parent?.let {
                (it as View).visibility = android.view.View.GONE
            }
        }

        // Load related articles
        val rvRelatedArticles = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvRelatedArticles)
        rvRelatedArticles.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val blogAdapter = com.veganbeauty.app.features.community.blog.BlogAdapter(emptyList()) { relatedPost ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, BlogDetailFragment.newInstance(relatedPost))
                .addToBackStack(null)
                .commit()
        }
        rvRelatedArticles.adapter = blogAdapter
        
        val blogRepo = com.veganbeauty.app.data.local.BlogRepository(requireContext())
        val allPosts = blogRepo.getBlogPosts(limit = 10, targetCategory = post.category)
        val relatedPosts = allPosts.filter { it.title != post.title }.take(6)
        
        if (relatedPosts.isNotEmpty()) {
            val featuredPost = relatedPosts[0]
            val flFeaturedPost = view.findViewById<android.widget.FrameLayout>(R.id.flFeaturedPost)
            flFeaturedPost.visibility = android.view.View.VISIBLE
            
            view.findViewById<TextView>(R.id.tvFeaturedTitle).text = featuredPost.title
            view.findViewById<TextView>(R.id.tvFeaturedTime).text = featuredPost.date
            view.findViewById<TextView>(R.id.tvFeaturedDesc).text = featuredPost.description
            
            if (featuredPost.imageUrl.isNotEmpty()) {
                view.findViewById<ImageView>(R.id.ivFeaturedImage).load(featuredPost.imageUrl) {
                    crossfade(true)
                    error(R.drawable.img_avatar)
                }
            }
            
            flFeaturedPost.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, BlogDetailFragment.newInstance(featuredPost))
                    .addToBackStack(null)
                    .commit()
            }
            
            blogAdapter.updateData(relatedPosts.drop(1))
        } else {
            view.findViewById<android.view.View>(R.id.llRelatedArticles).visibility = android.view.View.GONE
        }

        // Load related products
        val rvRelatedProducts = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvRelatedProducts)
        rvRelatedProducts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        val productAdapter = com.veganbeauty.app.features.shop.product.list.ShopListAdapter(
            onItemClick = {},
            onAddToCartClick = {}
        )
        rvRelatedProducts.adapter = productAdapter
        
        val allProducts = com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).getAllProducts()
        var relatedProducts = allProducts.filter { it.category.contains(post.category, ignoreCase = true) }
        
        if (relatedProducts.isEmpty() && post.category.equals("Chăm sóc tóc", ignoreCase = true)) {
            relatedProducts = allProducts.filter { it.category.contains("tóc", ignoreCase = true) }
        }
        if (relatedProducts.isEmpty() && post.category.contains("da", ignoreCase = true)) {
            relatedProducts = allProducts.filter { it.category.contains("da", ignoreCase = true) }
        }
        if (relatedProducts.isEmpty() && post.category.equals("Khỏe đẹp", ignoreCase = true)) {
            relatedProducts = allProducts.filter { it.category.contains("cơ thể", ignoreCase = true) }
        }
        if (relatedProducts.isEmpty()) {
            relatedProducts = allProducts
        }
        
        var currentProductCount = 6
        productAdapter.submitList(relatedProducts.take(currentProductCount))
        
        val btnLoadMoreProducts = view.findViewById<android.view.View>(R.id.btnLoadMoreProducts)
        if (relatedProducts.size > currentProductCount) {
            btnLoadMoreProducts.visibility = android.view.View.VISIBLE
        } else {
            btnLoadMoreProducts.visibility = android.view.View.GONE
        }
        
        btnLoadMoreProducts.setOnClickListener {
            currentProductCount += 2
            productAdapter.submitList(relatedProducts.take(currentProductCount))
            if (currentProductCount >= relatedProducts.size) {
                btnLoadMoreProducts.visibility = android.view.View.GONE
            }
        }
        
        val tvRelatedProductsLabel = view.findViewById<TextView>(R.id.tvRelatedProductsLabel)
        if (post.category.equals("Chăm sóc tóc", ignoreCase = true)) {
            tvRelatedProductsLabel.text = "Sản phẩm dưỡng tóc"
        } else if (post.category.contains("da", ignoreCase = true)) {
            tvRelatedProductsLabel.text = "Sản phẩm dưỡng da"
        } else if (post.category.contains("cơ thể", ignoreCase = true)) {
            tvRelatedProductsLabel.text = "Sản phẩm chăm sóc cơ thể"
        } else {
            tvRelatedProductsLabel.text = "Sản phẩm gợi ý"
        }
    }
}
