package com.veganbeauty.app.features.community.blog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.BlogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BlogCategoryFragment : Fragment() {

    companion object {
        private const val ARG_CATEGORY_NAME = "category_name"

        fun newInstance(categoryName: String): BlogCategoryFragment {
            val fragment = BlogCategoryFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY_NAME, categoryName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_blog_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryName = arguments?.getString(ARG_CATEGORY_NAME) ?: "Danh mục"

        view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<TextView>(R.id.tvCategoryTitle).text = categoryName

        val rvBlogList = view.findViewById<RecyclerView>(R.id.rvBlogList)
        val navigateToDetail: (BlogPost) -> Unit = { post ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, BlogDetailFragment.newInstance(post))
                .addToBackStack(null)
                .commit()
        }
        val adapter = BlogAdapter(emptyList(), navigateToDetail)
        rvBlogList.layoutManager = LinearLayoutManager(requireContext())
        rvBlogList.adapter = adapter
        
        val btnLoadMore = view.findViewById<View>(R.id.btnLoadMore)
        var currentOffset = 0
        var allPosts = mutableListOf<BlogPost>()

        fun loadData(limit: Int) {
            btnLoadMore.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                val ctx = context ?: return@launchWhenStarted
                val repo = BlogRepository(ctx)
                
                val newPosts = withContext(Dispatchers.IO) {
                    repo.getBlogPosts(limit, categoryName, currentOffset)
                }

                if (newPosts.isNotEmpty()) {
                    if (currentOffset == 0) {
                        // Set featured post
                        val featured = newPosts[0]
                        view.findViewById<TextView>(R.id.tvFeaturedTitle).text = featured.title
                        view.findViewById<TextView>(R.id.tvFeaturedTime).text = featured.date
                        view.findViewById<TextView>(R.id.tvFeaturedDesc).text = featured.description
                        if (featured.imageUrl.isNotEmpty()) {
                            view.findViewById<ImageView>(R.id.ivFeaturedImage).load(featured.imageUrl) {
                                crossfade(true)
                                error(R.color.gray_light)
                            }
                        }
                        
                        view.findViewById<View>(R.id.tvFeaturedTitle).parent.parent?.let { parentView ->
                            (parentView as View).setOnClickListener {
                                navigateToDetail(featured)
                            }
                        }
                        
                        allPosts.addAll(newPosts.drop(1))
                    } else {
                        allPosts.addAll(newPosts)
                    }
                    
                    adapter.updateData(allPosts)
                    currentOffset += newPosts.size
                    
                    // If we got exactly the limit we requested, there might be more
                    btnLoadMore.visibility = if (newPosts.size == limit) View.VISIBLE else View.GONE
                } else {
                    if (currentOffset == 0) {
                        view.findViewById<TextView>(R.id.tvFeaturedTitle).text = "Chưa có bài viết nào"
                        view.findViewById<TextView>(R.id.tvFeaturedDesc).text = "Vui lòng quay lại sau."
                    }
                    btnLoadMore.visibility = View.GONE
                }
                btnLoadMore.isEnabled = true
            }
        }

        // Initial load: 1 featured + 5 list items = 6
        loadData(6)
        
        btnLoadMore.setOnClickListener {
            loadData(5)
        }
    }
}
