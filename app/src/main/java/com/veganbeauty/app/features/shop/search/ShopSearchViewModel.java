package com.veganbeauty.app.features.shop.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;

import com.veganbeauty.app.data.local.dao.CommunityDao;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.data.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;
import kotlinx.coroutines.flow.FlowKt;

public class ShopSearchViewModel extends ViewModel {

    private final ProductRepository repository;
    private final CommunityDao communityDao;

    private final MutableLiveData<List<ProductEntity>> _hotDeals = new MutableLiveData<>();
    public final LiveData<List<ProductEntity>> hotDeals = _hotDeals;

    private final MutableLiveData<List<String>> _topSearchTerms = new MutableLiveData<>();
    public final LiveData<List<String>> topSearchTerms = _topSearchTerms;

    private final MutableLiveData<SearchSuggestions> _suggestions = new MutableLiveData<>();
    public final LiveData<SearchSuggestions> suggestions = _suggestions;

    private final MutableLiveData<Boolean> _dataReady = new MutableLiveData<>(false);
    public final LiveData<Boolean> dataReady = _dataReady;

    private List<ProductEntity> allProducts = new ArrayList<>();
    private List<YtVideoEntity> allVideos = new ArrayList<>();
    private List<CommunityBlogEntity> allBlogs = new ArrayList<>();
    private List<CommunityPostEntity> allPosts = new ArrayList<>();

    private final Map<String, String> subcategoryToIdMap = new HashMap<String, String>() {{
        put("Sữa rửa mặt", "f5877af6a55f88bcf57c17b4");
        put("Tẩy trang", "389971929086b2ce7fba9dd0");
        put("Chống nắng", "36cbf3f5c4b7a299ce2a2d0c");
        put("Nước cân bằng", "4e20d6bbc1203015ee2ecd48");
        put("Tinh chất", "b1b6cd208332d4f1e015a26c");
        put("Mặt nạ", "7667d982515426a9d88b787b");
        put("Kem dưỡng", "bb88a3306cf95af20d073594");
        put("Xịt khoáng", "9882d5fa14c74dd053e17f33");
        put("Tẩy da chết mặt", "c211afa24702f5d1ff86fe42");
        put("Sữa tắm", "7c70e845e829b374e57ee7b1");
        put("Tẩy da chết cơ thể", "b703bb813e660aa88076ee5a");
        put("Dưỡng thể", "8fce5340c618672aa1ae7fb3");
        put("Chăm sóc tóc", "24a75aa9d541feed638b1970");
        put("Tẩy da chết môi", "755731e01d8c579c633ae4d2");
        put("Dưỡng ẩm môi", "ded17e0716783c133b1a5b9a");
        put("Chăm sóc da mặt", "7176b5e7966be88daf95cfd4");
        put("Chăm sóc cơ thể", "f40c1f05dcf4059f25fb89a1");
        put("Chăm sóc mái tóc", "e0754dabb88699e92481e123");
        put("Chăm sóc môi", "bd1c0ff76b19b1b5a3130a79");
    }};

    private final List<String> parentCategories = Arrays.asList(
            "Chăm sóc da", "Tắm & Dưỡng thể", "Dưỡng môi", "Combo/Giftbox"
    );

    private final Map<String, String> subcategoryToParentCategory = new HashMap<String, String>() {{
        put("Sữa rửa mặt", "Chăm sóc da");
        put("Tẩy trang", "Chăm sóc da");
        put("Chống nắng", "Chăm sóc da");
        put("Nước cân bằng", "Chăm sóc da");
        put("Tinh chất", "Chăm sóc da");
        put("Mặt nạ", "Chăm sóc da");
        put("Kem dưỡng", "Chăm sóc da");
        put("Xịt khoáng", "Chăm sóc da");
        put("Tẩy da chết mặt", "Chăm sóc da");
        put("Sữa tắm", "Tắm & Dưỡng thể");
        put("Tẩy da chết cơ thể", "Tắm & Dưỡng thể");
        put("Dưỡng thể", "Tắm & Dưỡng thể");
        put("Chăm sóc tóc", "Chăm sóc tóc");
        put("Tẩy da chết môi", "Dưỡng môi");
        put("Dưỡng ẩm môi", "Dưỡng môi");
        put("Chăm sóc da mặt", "Combo/Giftbox");
        put("Chăm sóc cơ thể", "Combo/Giftbox");
        put("Chăm sóc mái tóc", "Combo/Giftbox");
        put("Chăm sóc môi", "Combo/Giftbox");
    }};

    public ShopSearchViewModel(ProductRepository repository, CommunityDao communityDao) {
        this.repository = repository;
        this.communityDao = communityDao;

        ViewModelKt.getViewModelScope(this).launch((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getIO(), (s2, c2) -> {
                    try {
                        repository.refreshProducts(c2);
                    } catch (Exception ignored) {}
                    return kotlin.Unit.INSTANCE;
                }, cont)
        );

        ViewModelKt.getViewModelScope(this).launch((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    FlowKt.combine(
                            repository.getAllProducts(),
                            communityDao.getAllExploreVideos(),
                            communityDao.getAllBlogs(),
                            communityDao.getAllPosts(),
                            (Object[] args, kotlin.coroutines.Continuation c) -> {
                                List<ProductEntity> products = (List<ProductEntity>) args[0];
                                List<YtVideoEntity> videos = (List<YtVideoEntity>) args[1];
                                List<CommunityBlogEntity> blogs = (List<CommunityBlogEntity>) args[2];
                                List<CommunityPostEntity> posts = (List<CommunityPostEntity>) args[3];

                                allProducts = products;
                                allVideos = videos;
                                allBlogs = blogs;
                                allPosts = posts;

                                loadHotDeals(products);
                                loadTopSearchTerms(products);
                                _dataReady.setValue(true);
                                return kotlin.Unit.INSTANCE;
                            }
                    ).collect(new FlowCollector<kotlin.Unit>() {
                        @Nullable
                        @Override
                        public Object emit(kotlin.Unit value, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont)
        );
    }

    public boolean isHotOrNew(ProductEntity product) {
        return product.isNew() || product.getPrice() >= 500000 || product.getCategory().toLowerCase().contains("combo");
    }

    public String getSubcategoryId(String name) {
        return subcategoryToIdMap.get(name);
    }

    public kotlin.Pair<String, String> getNavigationForTerm(String term) {
        if (parentCategories.contains(term)) return new kotlin.Pair<>(term, null);
        String parent = subcategoryToParentCategory.get(term);
        if (parent != null) return new kotlin.Pair<>(parent, term);
        return new kotlin.Pair<>(term, null);
    }

    public void updateSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            _suggestions.setValue(SearchSuggestions.Companion.getEMPTY());
        } else {
            _suggestions.setValue(computeSuggestions(keyword));
        }
    }

    public List<ProductEntity> searchProducts(String keyword, String sortOrder) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();
        List<ProductEntity> results = filterProducts(keyword);

        if ("NEWEST".equals(sortOrder)) {
            Collections.sort(results, (p1, p2) -> {
                int cmp = Boolean.compare(p2.isNew(), p1.isNew());
                if (cmp == 0) return p1.getName().compareTo(p2.getName());
                return cmp;
            });
        } else if ("PRICE_LOW".equals(sortOrder)) {
            Collections.sort(results, Comparator.comparingLong(ProductEntity::getPrice));
        } else if ("PRICE_HIGH".equals(sortOrder)) {
            Collections.sort(results, (p1, p2) -> Long.compare(p2.getPrice(), p1.getPrice()));
        }

        return results;
    }

    public List<YtVideoEntity> searchVideos(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();
        List<YtVideoEntity> matched = new ArrayList<>();
        for (YtVideoEntity video : allVideos) {
            if (video.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                    video.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
                matched.add(video);
            }
        }
        if (!matched.isEmpty()) return matched;

        List<YtVideoEntity> notebookVideos = new ArrayList<>();
        for (YtVideoEntity video : allVideos) {
            if (video.getType().toLowerCase().contains("notebook")) {
                notebookVideos.add(video);
                if (notebookVideos.size() >= 6) break;
            }
        }
        return notebookVideos;
    }

    private SearchSuggestions computeSuggestions(String keyword) {
        List<ProductEntity> matchedProducts = filterProducts(keyword);
        List<String> productNames = new ArrayList<>();
        for (ProductEntity p : matchedProducts) {
            if (!productNames.contains(p.getName())) {
                productNames.add(p.getName());
                if (productNames.size() >= 4) break;
            }
        }

        List<ContentSuggestion> contentItems = buildContentSuggestions(keyword);
        List<ProductEntity> previewProducts = new ArrayList<>(matchedProducts.subList(0, Math.min(3, matchedProducts.size())));

        return new SearchSuggestions(productNames, contentItems, previewProducts);
    }

    public boolean matchesKeyword(ProductEntity product, String keyword) {
        String lowerKw = keyword.toLowerCase();
        String subcategoryId = getSubcategoryId(keyword);

        if (product.getName().toLowerCase().contains(lowerKw) ||
                product.getDescription().toLowerCase().contains(lowerKw) ||
                product.getCategory().toLowerCase().contains(lowerKw)) {
            return true;
        }
        for (String ingredient : product.getDetailedIngredients()) {
            if (ingredient.toLowerCase().contains(lowerKw)) return true;
        }
        if (subcategoryId != null) {
            String[] ids = product.getCategoryIds().split(",");
            for (String id : ids) {
                if (id.trim().equals(subcategoryId)) return true;
            }
        }
        return false;
    }

    private List<ProductEntity> filterProducts(String keyword) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity p : allProducts) {
            if (matchesKeyword(p, keyword)) filtered.add(p);
        }
        return filtered;
    }

    private List<ContentSuggestion> buildContentSuggestions(String keyword) {
        String lowerKw = keyword.toLowerCase();
        List<ContentSuggestion> results = new ArrayList<>();

        for (String category : parentCategories) {
            if (category.toLowerCase().contains(lowerKw)) {
                results.add(new ContentSuggestion("Danh mục " + category, ContentSuggestionType.CATEGORY, category, null, null));
            }
        }

        for (String sub : subcategoryToIdMap.keySet()) {
            if (sub.toLowerCase().contains(lowerKw)) {
                String parent = subcategoryToParentCategory.get(sub);
                if (parent != null) {
                    results.add(new ContentSuggestion("Danh mục " + sub, ContentSuggestionType.CATEGORY, parent, sub, null));
                }
            }
        }

        int videoCount = 0;
        for (YtVideoEntity video : allVideos) {
            if (video.getTitle().toLowerCase().contains(lowerKw) || video.getDescription().toLowerCase().contains(lowerKw)) {
                results.add(new ContentSuggestion("Cẩm nang " + video.getTitle(), ContentSuggestionType.VIDEO, null, null, video.getUrl()));
                videoCount++;
                if (videoCount >= 2) break;
            }
        }

        int blogCount = 0;
        for (CommunityBlogEntity blog : allBlogs) {
            if (blog.getTitle().toLowerCase().contains(lowerKw) || blog.getShortDescription().toLowerCase().contains(lowerKw)) {
                results.add(new ContentSuggestion(blog.getTitle(), ContentSuggestionType.BLOG, null, null, null));
                blogCount++;
                if (blogCount >= 1) break;
            }
        }

        int postCount = 0;
        for (CommunityPostEntity post : allPosts) {
            if (post.getContent().toLowerCase().contains(lowerKw) || (post.getType() != null && post.getType().toLowerCase().contains(lowerKw))) {
                String label = post.getType() != null ? post.getType() + " " + (post.getContent().length() > 25 ? post.getContent().substring(0, 25) : post.getContent()) : (post.getContent().length() > 40 ? post.getContent().substring(0, 40) : post.getContent());
                results.add(new ContentSuggestion(label, ContentSuggestionType.POST, null, null, null));
                postCount++;
                if (postCount >= 1) break;
            }
        }

        if (results.size() < 2) {
            addFallbackSuggestions(lowerKw, results);
        }

        List<ContentSuggestion> uniqueResults = new ArrayList<>();
        Set<String> seenLabels = new HashSet<>();
        for (ContentSuggestion cs : results) {
            if (!seenLabels.contains(cs.getLabel())) {
                seenLabels.add(cs.getLabel());
                uniqueResults.add(cs);
                if (uniqueResults.size() >= 3) break;
            }
        }

        return uniqueResults;
    }

    private void addFallbackSuggestions(String keyword, List<ContentSuggestion> results) {
        Map<String, String[]> fallbackMap = new HashMap<>();
        fallbackMap.put("da", new String[]{"Chăm sóc da", "Cẩm nang chăm da"});
        fallbackMap.put("tóc", new String[]{"Chăm sóc tóc", "Cẩm nang chăm tóc"});
        fallbackMap.put("môi", new String[]{"Dưỡng môi", "Cẩm nang dưỡng môi"});
        fallbackMap.put("tắm", new String[]{"Tắm & Dưỡng thể", "Cẩm nang tắm gội"});
        fallbackMap.put("nắng", new String[]{"Chống nắng", "Cẩm nang chống nắng"});
        fallbackMap.put("bí", new String[]{"Chăm sóc da", "Cẩm nang bí đao"});

        for (Map.Entry<String, String[]> entry : fallbackMap.entrySet()) {
            if (!keyword.contains(entry.getKey())) continue;
            String category = entry.getValue()[0];
            String videoLabel = entry.getValue()[1];

            boolean hasCategory = false;
            for (ContentSuggestion cs : results) {
                if (category.equals(cs.getCategoryName())) { hasCategory = true; break; }
            }

            if (!hasCategory) {
                boolean isParent = parentCategories.contains(category);
                String parent = isParent ? category : subcategoryToParentCategory.get(category);
                if (parent != null) {
                    results.add(new ContentSuggestion(
                            "Danh mục " + category,
                            ContentSuggestionType.CATEGORY,
                            parent,
                            isParent ? null : category,
                            null
                    ));
                }
            }

            boolean hasVideo = false;
            for (ContentSuggestion cs : results) {
                if (cs.getType() == ContentSuggestionType.VIDEO) { hasVideo = true; break; }
            }

            if (!hasVideo) {
                YtVideoEntity foundVideo = null;
                for (YtVideoEntity v : allVideos) {
                    if (v.getTitle().toLowerCase().contains(entry.getKey()) || v.getDescription().toLowerCase().contains(entry.getKey())) {
                        foundVideo = v; break;
                    }
                }
                if (foundVideo == null) {
                    for (YtVideoEntity v : allVideos) {
                        if (v.getType().toLowerCase().contains("notebook")) {
                            foundVideo = v; break;
                        }
                    }
                }
                if (foundVideo != null) {
                    results.add(new ContentSuggestion(videoLabel, ContentSuggestionType.VIDEO, null, null, foundVideo.getUrl()));
                }
            }
            break;
        }
    }

    private void loadHotDeals(List<ProductEntity> products) {
        List<ProductEntity> deals = new ArrayList<>();
        for (ProductEntity p : products) {
            if (isHotOrNew(p)) {
                deals.add(p);
                if (deals.size() >= 3) break;
            }
        }
        _hotDeals.setValue(deals);
    }

    private void loadTopSearchTerms(List<ProductEntity> products) {
        if (products.isEmpty()) {
            _topSearchTerms.setValue(new ArrayList<>());
            return;
        }

        Map<String, Integer> idCounts = new HashMap<>();
        for (ProductEntity p : products) {
            String[] ids = p.getCategoryIds().split(",");
            for (String id : ids) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    idCounts.put(trimmed, idCounts.getOrDefault(trimmed, 0) + 1);
                }
            }
        }

        List<kotlin.Pair<String, Integer>> termCounts = new ArrayList<>();
        for (Map.Entry<String, String> entry : subcategoryToIdMap.entrySet()) {
            Integer count = idCounts.get(entry.getValue());
            if (count != null) {
                termCounts.add(new kotlin.Pair<>(entry.getKey(), count));
            }
        }

        Collections.sort(termCounts, (o1, o2) -> Integer.compare(o2.getSecond(), o1.getSecond()));

        List<String> terms = new ArrayList<>();
        for (kotlin.Pair<String, Integer> pair : termCounts) {
            terms.add(pair.getFirst());
        }

        if (terms.size() < 6) {
            Set<String> existing = new HashSet<>(terms);
            for (String category : parentCategories) {
                if (existing.size() >= 6) break;
                for (ProductEntity p : products) {
                    if (p.getCategory().toLowerCase().contains(category.toLowerCase())) {
                        existing.add(category);
                        break;
                    }
                }
            }
            List<String> finalTerms = new ArrayList<>(existing);
            _topSearchTerms.setValue(finalTerms.subList(0, Math.min(6, finalTerms.size())));
        } else {
            _topSearchTerms.setValue(terms.subList(0, 6));
        }
    }
}
