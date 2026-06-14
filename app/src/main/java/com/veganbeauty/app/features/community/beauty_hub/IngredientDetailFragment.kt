package com.veganbeauty.app.features.community.beauty_hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.IngredientEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IngredientDetailFragment : Fragment() {

    private var ingredientSlug: String? = null

    companion object {
        private const val ARG_SLUG = "slug"
        fun newInstance(slug: String) = IngredientDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SLUG, slug)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ingredientSlug = arguments?.getString(ARG_SLUG)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.com_fragment_ingredient_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val jsonReader = LocalJsonReader(requireContext())
            val ingredients = withContext(Dispatchers.IO) {
                jsonReader.getIngredients()
            }
            val ingredient = ingredients.find { it.slug == ingredientSlug } ?: return@launchWhenStarted

            view.findViewById<ImageView>(R.id.ivHeader).load(ingredient.image) {
                crossfade(true)
            }
            view.findViewById<TextView>(R.id.tvName).text = ingredient.name
            view.findViewById<TextView>(R.id.tvScientificName).text = ingredient.scientificName
            view.findViewById<TextView>(R.id.tvOrigin).text = ingredient.origin
            val tabIntro = view.findViewById<TextView>(R.id.tabIntro)
            val tabUses = view.findViewById<TextView>(R.id.tabUses)
            val tvContent = view.findViewById<TextView>(R.id.tvContent)
            
            // Default to Intro
            tvContent.text = ingredient.description
            
            val colorActive = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
            val colorInactive = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)
            
            tabIntro.setOnClickListener {
                tabIntro.setTextColor(colorActive)
                tabIntro.setBackgroundResource(R.drawable.com_bg_tab_active)
                tabUses.setTextColor(colorInactive)
                tabUses.setBackgroundResource(android.R.color.transparent)
                tvContent.text = ingredient.description
            }
            
            tabUses.setOnClickListener {
                tabUses.setTextColor(colorActive)
                tabUses.setBackgroundResource(R.drawable.com_bg_tab_active)
                tabIntro.setTextColor(colorInactive)
                tabIntro.setBackgroundResource(android.R.color.transparent)
                tvContent.text = ingredient.uses
            }

            view.findViewById<TextView>(R.id.tvProductsTitle).text = "Sản phẩm chứa ${ingredient.name.lowercase()}"

            // Add types chips
            val llTypes = view.findViewById<LinearLayout>(R.id.llTypes)
            llTypes.removeAllViews()
            val dp = resources.displayMetrics.density.toInt()
            ingredient.types.forEach { type ->
                val chip = TextView(requireContext()).apply {
                    text = type
                    textSize = 12f
                    setPadding(12 * dp, 6 * dp, 12 * dp, 6 * dp)
                    setBackgroundResource(R.drawable.com_bg_chip_type)
                    setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary))
                    val marginParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = 8 * dp }
                    layoutParams = marginParams
                }
                llTypes.addView(chip)
            }

            // Products
            val allProducts = withContext(Dispatchers.IO) {
                jsonReader.getAllProducts()
            }
            var matchingProducts = allProducts.filter {
                it.mainIngredientsSummary.contains(ingredient.name, ignoreCase = true) ||
                it.detailedIngredients.any { ing -> ing.contains(ingredient.name, ignoreCase = true) } ||
                it.name.contains(ingredient.name, ignoreCase = true) ||
                it.description.contains(ingredient.name, ignoreCase = true) ||
                it.keyIngredients.any { ki -> ki.name.contains(ingredient.name, ignoreCase = true) }
            }
            if (matchingProducts.isEmpty()) {
                matchingProducts = allProducts.shuffled().take(4)
            } else {
                matchingProducts = matchingProducts.shuffled().take(6)
            }

            val rvProducts = view.findViewById<RecyclerView>(R.id.rvProducts)
            val adapter = com.veganbeauty.app.features.shop.product.list.ShopListAdapter(
                onItemClick = {},
                onAddToCartClick = {}
            )
            adapter.submitList(matchingProducts)
            rvProducts.adapter = adapter
        }
    }
}
