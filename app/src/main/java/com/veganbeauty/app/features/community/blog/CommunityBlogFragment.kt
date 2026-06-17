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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CommunityBlogFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_blog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            val rvBlogList = view.findViewById<RecyclerView>(R.id.rvBlogList)
            val adapter = BlogAdapter(emptyList()) { post ->
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, BlogDetailFragment.newInstance(post))
                    .addToBackStack(null)
                    .commit()
                Unit
            }
            rvBlogList.layoutManager = LinearLayoutManager(requireContext())
            rvBlogList.adapter = adapter

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                try {
                    val ctx = context ?: return@launchWhenStarted
                    val repo = BlogRepository(ctx)
                    
                    view.findViewById<TextView>(R.id.tvCountSkincare)?.text = "677 bài viết"
                    view.findViewById<TextView>(R.id.tvCountHaircare)?.text = "215 bài viết"
                    view.findViewById<TextView>(R.id.tvCountBodycare)?.text = "823 bài viết"
                    view.findViewById<TextView>(R.id.tvCountCosmetics)?.text = "156 bài viết"
                    
                    val navigateToCategory = { categoryName: String ->
                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, BlogCategoryFragment.newInstance(categoryName))
                            .addToBackStack(null)
                            .commit()
                    }
                    
                    view.findViewById<View>(R.id.llCategorySkincare)?.setOnClickListener { navigateToCategory("Dưỡng da") }
                    view.findViewById<View>(R.id.llCategoryHaircare)?.setOnClickListener { navigateToCategory("Chăm sóc tóc") }
                    view.findViewById<View>(R.id.llCategoryBodycare)?.setOnClickListener { navigateToCategory("Chăm sóc cơ thể") }
                    view.findViewById<View>(R.id.llCategoryCosmetics)?.setOnClickListener { navigateToCategory("Mỹ phẩm") }

                    val posts = withContext(Dispatchers.IO) {
                        repo.getBlogPosts(20)
                    }

                    if (posts.isNotEmpty()) {
                        val featured = posts[0]
                        view.findViewById<TextView>(R.id.tvFeaturedTitle).text = featured.title
                        view.findViewById<TextView>(R.id.tvFeaturedTime).text = featured.date
                        view.findViewById<TextView>(R.id.tvFeaturedDesc).text = featured.description
                        if (featured.imageUrl.isNotEmpty()) {
                            view.findViewById<ImageView>(R.id.ivFeaturedImage).load(featured.imageUrl) {
                                crossfade(true)
                                error(R.color.gray_light)
                            }
                        }
                        adapter.updateData(posts.drop(1))
                    }
                } catch (e: Exception) {
                    view.findViewById<TextView>(R.id.tvFeaturedDesc)?.text = "Error inner: ${e.message}"
                }
            }
        } catch (e: Exception) {
            view.findViewById<TextView>(R.id.tvFeaturedDesc)?.text = "Error outer: ${e.message}"
        }
    }
}
