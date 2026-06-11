package com.veganbeauty.app.features.shop.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.dao.CommunityDao
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ShopSearchViewModel(
    private val repository: ProductRepository,
    private val communityDao: CommunityDao
) : ViewModel() {

    private val _hotDeals = MutableLiveData<List<ProductEntity>>()
    val hotDeals: LiveData<List<ProductEntity>> = _hotDeals

    private val _topSearchTerms = MutableLiveData<List<String>>()
    val topSearchTerms: LiveData<List<String>> = _topSearchTerms

    private val _suggestions = MutableLiveData<SearchSuggestions>()
    val suggestions: LiveData<SearchSuggestions> = _suggestions

    private val _dataReady = MutableLiveData(false)
    val dataReady: LiveData<Boolean> = _dataReady

    private var allProducts: List<ProductEntity> = emptyList()
    private var allVideos: List<YtVideoEntity> = emptyList()
    private var allBlogs: List<CommunityBlogEntity> = emptyList()
    private var allPosts: List<CommunityPostEntity> = emptyList()

    private val subcategoryToIdMap = mapOf(
        "Sữa rửa mặt" to "f5877af6a55f88bcf57c17b4",
        "Tẩy trang" to "389971929086b2ce7fba9dd0",
        "Chống nắng" to "36cbf3f5c4b7a299ce2a2d0c",
        "Nước cân bằng" to "4e20d6bbc1203015ee2ecd48",
        "Tinh chất" to "b1b6cd208332d4f1e015a26c",
        "Mặt nạ" to "7667d982515426a9d88b787b",
        "Kem dưỡng" to "bb88a3306cf95af20d073594",
        "Xịt khoáng" to "9882d5fa14c74dd053e17f33",
        "Tẩy da chết mặt" to "c211afa24702f5d1ff86fe42",
        "Sữa tắm" to "7c70e845e829b374e57ee7b1",
        "Tẩy da chết cơ thể" to "b703bb813e660aa88076ee5a",
        "Dưỡng thể" to "8fce5340c618672aa1ae7fb3",
        "Chăm sóc tóc" to "24a75aa9d541feed638b1970",
        "Tẩy da chết môi" to "755731e01d8c579c633ae4d2",
        "Dưỡng ẩm môi" to "ded17e0716783c133b1a5b9a",
        "Chăm sóc da mặt" to "7176b5e7966be88daf95cfd4",
        "Chăm sóc cơ thể" to "f40c1f05dcf4059f25fb89a1",
        "Chăm sóc mái tóc" to "e0754dabb88699e92481e123",
        "Chăm sóc môi" to "bd1c0ff76b19b1b5a3130a79"
    )

    private val parentCategories = listOf(
        "Chăm sóc da", "Tắm & Dưỡng thể", "Dưỡng môi", "Combo/Giftbox"
    )

    private val subcategoryToParentCategory = mapOf(
        "Sữa rửa mặt" to "Chăm sóc da",
        "Tẩy trang" to "Chăm sóc da",
        "Chống nắng" to "Chăm sóc da",
        "Nước cân bằng" to "Chăm sóc da",
        "Tinh chất" to "Chăm sóc da",
        "Mặt nạ" to "Chăm sóc da",
        "Kem dưỡng" to "Chăm sóc da",
        "Xịt khoáng" to "Chăm sóc da",
        "Tẩy da chết mặt" to "Chăm sóc da",
        "Sữa tắm" to "Tắm & Dưỡng thể",
        "Tẩy da chết cơ thể" to "Tắm & Dưỡng thể",
        "Dưỡng thể" to "Tắm & Dưỡng thể",
        "Chăm sóc tóc" to "Chăm sóc tóc",
        "Tẩy da chết môi" to "Dưỡng môi",
        "Dưỡng ẩm môi" to "Dưỡng môi",
        "Chăm sóc da mặt" to "Combo/Giftbox",
        "Chăm sóc cơ thể" to "Combo/Giftbox",
        "Chăm sóc mái tóc" to "Combo/Giftbox",
        "Chăm sóc môi" to "Combo/Giftbox"
    )

    init {
        viewModelScope.launch {
            try {
                repository.refreshProducts()
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            combine(
                repository.allProducts,
                communityDao.getAllExploreVideos(),
                communityDao.getAllBlogs(),
                communityDao.getAllPosts()
            ) { products, videos, blogs, posts ->
                allProducts = products
                allVideos = videos
                allBlogs = blogs
                allPosts = posts
                loadHotDeals(products)
                loadTopSearchTerms(products)
                _dataReady.value = true
            }.collect { }
        }
    }

    fun isHotOrNew(product: ProductEntity): Boolean =
        product.isNew || product.price >= 500000 || product.category.contains("Combo", ignoreCase = true)

    fun getSubcategoryId(name: String): String? = subcategoryToIdMap[name]

    fun getNavigationForTerm(term: String): Pair<String, String?> {
        if (term in parentCategories) return term to null
        val parent = subcategoryToParentCategory[term]
        if (parent != null) return parent to term
        return term to null
    }

    fun updateSuggestions(keyword: String) {
        _suggestions.value = if (keyword.isBlank()) {
            SearchSuggestions.EMPTY
        } else {
            computeSuggestions(keyword)
        }
    }

    fun searchProducts(keyword: String, sortOrder: String = "BEST_SELLING"): List<ProductEntity> {
        if (keyword.isBlank()) return emptyList()
        var results = filterProducts(keyword)
        results = when (sortOrder) {
            "NEWEST" -> results.sortedWith(compareByDescending<ProductEntity> { it.isNew }.thenBy { it.name })
            "PRICE_LOW" -> results.sortedBy { it.price }
            "PRICE_HIGH" -> results.sortedByDescending { it.price }
            else -> results
        }
        return results
    }

    fun searchVideos(keyword: String): List<YtVideoEntity> {
        if (keyword.isBlank()) return emptyList()
        val matched = allVideos.filter { video ->
            video.title.contains(keyword, ignoreCase = true) ||
                video.description.contains(keyword, ignoreCase = true)
        }
        if (matched.isNotEmpty()) return matched
        return allVideos.filter { it.type.contains("notebook", ignoreCase = true) }.take(6)
    }

    private fun computeSuggestions(keyword: String): SearchSuggestions {
        val matchedProducts = filterProducts(keyword)
        val productNames = matchedProducts.map { it.name }.distinct().take(4)
        val contentItems = buildContentSuggestions(keyword)
        val previewProducts = matchedProducts.take(3)
        return SearchSuggestions(productNames, contentItems, previewProducts)
    }

    fun matchesKeyword(product: ProductEntity, keyword: String): Boolean {
        val subcategoryId = getSubcategoryId(keyword)
        return product.name.contains(keyword, ignoreCase = true) ||
            product.description.contains(keyword, ignoreCase = true) ||
            product.category.contains(keyword, ignoreCase = true) ||
            product.detailedIngredients.any { it.contains(keyword, ignoreCase = true) } ||
            (subcategoryId != null && product.categoryIds.split(",").contains(subcategoryId))
    }

    private fun filterProducts(keyword: String): List<ProductEntity> {
        return allProducts.filter { matchesKeyword(it, keyword) }
    }

    private fun buildContentSuggestions(keyword: String): List<ContentSuggestion> {
        val results = mutableListOf<ContentSuggestion>()

        parentCategories.filter { it.contains(keyword, ignoreCase = true) }.forEach { category ->
            results.add(
                ContentSuggestion("Danh mục $category", ContentSuggestionType.CATEGORY, categoryName = category)
            )
        }

        subcategoryToIdMap.keys.filter { it.contains(keyword, ignoreCase = true) }.forEach { sub ->
            val parent = subcategoryToParentCategory[sub] ?: return@forEach
            results.add(
                ContentSuggestion(
                    "Danh mục $sub",
                    ContentSuggestionType.CATEGORY,
                    categoryName = parent,
                    subcategoryName = sub
                )
            )
        }

        allVideos.filter {
            it.title.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true)
        }.take(2).forEach { video ->
            results.add(
                ContentSuggestion(
                    "Cẩm nang ${video.title}",
                    ContentSuggestionType.VIDEO,
                    videoUrl = video.url
                )
            )
        }

        allBlogs.filter {
            it.title.contains(keyword, ignoreCase = true) || it.shortDescription.contains(keyword, ignoreCase = true)
        }.take(1).forEach { blog ->
            results.add(
                ContentSuggestion(blog.title, ContentSuggestionType.BLOG)
            )
        }

        allPosts.filter {
            it.content.contains(keyword, ignoreCase = true) ||
                (it.type?.contains(keyword, ignoreCase = true) == true)
        }.take(1).forEach { post ->
            val label = post.type?.let { "$it ${post.content.take(25)}" } ?: post.content.take(40)
            results.add(ContentSuggestion(label, ContentSuggestionType.POST))
        }

        if (results.size < 2) {
            addFallbackSuggestions(keyword.lowercase(), results)
        }

        return results.distinctBy { it.label }.take(3)
    }

    private fun addFallbackSuggestions(keyword: String, results: MutableList<ContentSuggestion>) {
        val fallbackMap = mapOf(
            "da" to ("Chăm sóc da" to "Cẩm nang chăm da"),
            "tóc" to ("Chăm sóc tóc" to "Cẩm nang chăm tóc"),
            "môi" to ("Dưỡng môi" to "Cẩm nang dưỡng môi"),
            "tắm" to ("Tắm & Dưỡng thể" to "Cẩm nang tắm gội"),
            "nắng" to ("Chống nắng" to "Cẩm nang chống nắng"),
            "bí" to ("Chăm sóc da" to "Cẩm nang bí đao")
        )

        for ((key, pair) in fallbackMap) {
            if (!keyword.contains(key)) continue
            val (category, videoLabel) = pair
            if (results.none { it.categoryName == category }) {
                val parent = if (category in parentCategories) category else subcategoryToParentCategory[category]
                if (parent != null) {
                    results.add(
                        ContentSuggestion(
                            if (category in parentCategories) "Danh mục $category" else "Danh mục $category",
                            ContentSuggestionType.CATEGORY,
                            categoryName = if (category in parentCategories) category else parent,
                            subcategoryName = if (category in parentCategories) null else category
                        )
                    )
                }
            }
            if (results.none { it.type == ContentSuggestionType.VIDEO }) {
                val video = allVideos.firstOrNull {
                    it.title.contains(key, ignoreCase = true) || it.description.contains(key, ignoreCase = true)
                } ?: allVideos.firstOrNull { it.type.contains("notebook", ignoreCase = true) }
                if (video != null) {
                    results.add(
                        ContentSuggestion(videoLabel, ContentSuggestionType.VIDEO, videoUrl = video.url)
                    )
                }
            }
            break
        }
    }

    private fun loadHotDeals(products: List<ProductEntity>) {
        _hotDeals.value = products.filter { isHotOrNew(it) }.take(3)
    }

    private fun loadTopSearchTerms(products: List<ProductEntity>) {
        if (products.isEmpty()) {
            _topSearchTerms.value = emptyList()
            return
        }

        val idCounts = products
            .flatMap { it.categoryIds.split(",").filter { id -> id.isNotEmpty() } }
            .groupingBy { it }
            .eachCount()

        val terms = subcategoryToIdMap
            .mapNotNull { (name, id) -> idCounts[id]?.let { count -> name to count } }
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()

        if (terms.size < 6) {
            val existing = terms.toMutableSet()
            for (category in parentCategories) {
                if (existing.size >= 6) break
                if (products.any { it.category.contains(category, ignoreCase = true) }) {
                    existing.add(category)
                }
            }
            _topSearchTerms.value = existing.toList().take(6)
        } else {
            _topSearchTerms.value = terms.take(6)
        }
    }
}
