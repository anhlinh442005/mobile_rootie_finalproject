package com.veganbeauty.app.features.shop;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.lifecycle.FlowLiveDataConversions;

import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.launch;

public class ShopViewModel extends ViewModel {

    private final ProductRepository repository;

    private final MutableStateFlow<String> _categoryFilter = StateFlowKt.MutableStateFlow(null);
    private final MutableStateFlow<String> _subcategoryFilter = StateFlowKt.MutableStateFlow("Tất cả");
    private final MutableStateFlow<String> _sortOrder = StateFlowKt.MutableStateFlow("BEST_SELLING");

    private final MutableStateFlow<List<String>> _subcategories = StateFlowKt.MutableStateFlow(Collections.singletonList("Tất cả"));
    private final LiveData<List<String>> subcategories;

    private final Map<String, String> subcategoryToIdMap;

    private final MutableStateFlow<Set<String>> _skinTypesFilter = StateFlowKt.MutableStateFlow(new HashSet<>());
    private final LiveData<Set<String>> skinTypesFilter;

    private final MutableStateFlow<String> _priceRangeFilter = StateFlowKt.MutableStateFlow(null);
    private final LiveData<String> priceRangeFilter;

    private final MutableStateFlow<Set<String>> _benefitsFilter = StateFlowKt.MutableStateFlow(new HashSet<>());
    private final LiveData<Set<String>> benefitsFilter;

    private final MutableStateFlow<Set<String>> _ingredientsFilter = StateFlowKt.MutableStateFlow(new HashSet<>());
    private final LiveData<Set<String>> ingredientsFilter;

    private final LiveData<List<ProductEntity>> products;

    public static class AdvancedFilterState {
        public final Set<String> skinTypes;
        public final String priceRange;
        public final Set<String> benefits;
        public final Set<String> ingredients;

        public AdvancedFilterState(Set<String> skinTypes, String priceRange, Set<String> benefits, Set<String> ingredients) {
            this.skinTypes = skinTypes != null ? skinTypes : new HashSet<>();
            this.priceRange = priceRange;
            this.benefits = benefits != null ? benefits : new HashSet<>();
            this.ingredients = ingredients != null ? ingredients : new HashSet<>();
        }
    }

    public ShopViewModel(ProductRepository repository) {
        this.repository = repository;
        subcategories = FlowLiveDataConversions.asLiveData(_subcategories, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);
        skinTypesFilter = FlowLiveDataConversions.asLiveData(_skinTypesFilter, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);
        priceRangeFilter = FlowLiveDataConversions.asLiveData(_priceRangeFilter, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);
        benefitsFilter = FlowLiveDataConversions.asLiveData(_benefitsFilter, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);
        ingredientsFilter = FlowLiveDataConversions.asLiveData(_ingredientsFilter, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);

        subcategoryToIdMap = new HashMap<>();
        subcategoryToIdMap.put("Sữa rửa mặt", "f5877af6a55f88bcf57c17b4");
        subcategoryToIdMap.put("Tẩy trang", "389971929086b2ce7fba9dd0");
        subcategoryToIdMap.put("Chống nắng", "36cbf3f5c4b7a299ce2a2d0c");
        subcategoryToIdMap.put("Nước cân bằng", "4e20d6bbc1203015ee2ecd48");
        subcategoryToIdMap.put("Tinh chất", "b1b6cd208332d4f1e015a26c");
        subcategoryToIdMap.put("Mặt nạ", "7667d982515426a9d88b787b");
        subcategoryToIdMap.put("Kem dưỡng", "bb88a3306cf95af20d073594");
        subcategoryToIdMap.put("Xịt khoáng", "9882d5fa14c74dd053e17f33");
        subcategoryToIdMap.put("Tẩy da chết mặt", "c211afa24702f5d1ff86fe42");

        subcategoryToIdMap.put("Sữa tắm", "7c70e845e829b374e57ee7b1");
        subcategoryToIdMap.put("Tẩy da chết cơ thể", "b703bb813e660aa88076ee5a");
        subcategoryToIdMap.put("Dưỡng thể", "8fce5340c618672aa1ae7fb3");

        subcategoryToIdMap.put("Chăm sóc tóc", "24a75aa9d541feed638b1970");

        subcategoryToIdMap.put("Tẩy da chết môi", "755731e01d8c579c633ae4d2");
        subcategoryToIdMap.put("Dưỡng ẩm môi", "ded17e0716783c133b1a5b9a");

        subcategoryToIdMap.put("Chăm sóc da mặt", "7176b5e7966be88daf95cfd4");
        subcategoryToIdMap.put("Chăm sóc cơ thể", "f40c1f05dcf4059f25fb89a1");
        subcategoryToIdMap.put("Chăm sóc mái tóc", "e0754dabb88699e92481e123");
        subcategoryToIdMap.put("Chăm sóc môi", "bd1c0ff76b19b1b5a3130a79");

        Flow<AdvancedFilterState> advancedFilterFlow = kotlinx.coroutines.flow.FlowKt.combine(
                _skinTypesFilter,
                _priceRangeFilter,
                _benefitsFilter,
                _ingredientsFilter,
                (skinTypes, priceRange, benefits, ingredients) -> new AdvancedFilterState(
                        (Set<String>) skinTypes,
                        (String) priceRange,
                        (Set<String>) benefits,
                        (Set<String>) ingredients
                )
        );

        Flow<List<ProductEntity>> combinedFlow = kotlinx.coroutines.flow.FlowKt.combine(
                repository.getAllProducts(),
                _categoryFilter,
                _subcategoryFilter,
                _sortOrder,
                advancedFilterFlow,
                (list, category, subcategory, sortOrder, advFilter) -> {
                    List<ProductEntity> productList = (List<ProductEntity>) list;
                    String cat = (String) category;
                    String subCat = (String) subcategory;
                    String sort = (String) sortOrder;
                    AdvancedFilterState adv = (AdvancedFilterState) advFilter;

                    List<ProductEntity> filteredList = new ArrayList<>(productList);

                    if (cat != null && !cat.equals("Tất cả")) {
                        List<String> targetIds = new ArrayList<>();
                        switch (cat) {
                            case "Chăm sóc da":
                                targetIds.add("f5877af6a55f88bcf57c17b4"); targetIds.add("389971929086b2ce7fba9dd0");
                                targetIds.add("36cbf3f5c4b7a299ce2a2d0c"); targetIds.add("4e20d6bbc1203015ee2ecd48");
                                targetIds.add("b1b6cd208332d4f1e015a26c"); targetIds.add("7667d982515426a9d88b787b");
                                targetIds.add("bb88a3306cf95af20d073594"); targetIds.add("9882d5fa14c74dd053e17f33");
                                targetIds.add("c211afa24702f5d1ff86fe42");
                                break;
                            case "Tắm & Dưỡng thể":
                                targetIds.add("7c70e845e829b374e57ee7b1"); targetIds.add("b703bb813e660aa88076ee5a");
                                targetIds.add("8fce5340c618672aa1ae7fb3");
                                break;
                            case "Chăm sóc tóc":
                                targetIds.add("24a75aa9d541feed638b1970");
                                break;
                            case "Dưỡng môi":
                                targetIds.add("755731e01d8c579c633ae4d2"); targetIds.add("ded17e0716783c133b1a5b9a");
                                break;
                            case "Combo/Giftbox":
                                targetIds.add("7176b5e7966be88daf95cfd4"); targetIds.add("f40c1f05dcf4059f25fb89a1");
                                targetIds.add("e0754dabb88699e92481e123"); targetIds.add("bd1c0ff76b19b1b5a3130a79");
                                break;
                        }

                        List<ProductEntity> temp = new ArrayList<>();
                        for (ProductEntity product : filteredList) {
                            String[] productCategoryIds = product.getCategoryIds().split(",");
                            boolean match = false;
                            for (String id : productCategoryIds) {
                                if (targetIds.contains(id.trim())) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match) {
                                temp.add(product);
                            }
                        }
                        filteredList = temp;
                    }

                    if (subCat != null && !subCat.equals("Tất cả")) {
                        String subcategoryId = subcategoryToIdMap.get(subCat);
                        List<ProductEntity> temp = new ArrayList<>();
                        if (subcategoryId != null) {
                            for (ProductEntity product : filteredList) {
                                String[] productCategoryIds = product.getCategoryIds().split(",");
                                boolean match = false;
                                for (String id : productCategoryIds) {
                                    if (id.trim().equals(subcategoryId)) {
                                        match = true;
                                        break;
                                    }
                                }
                                if (match) temp.add(product);
                            }
                        } else {
                            for (ProductEntity product : filteredList) {
                                if (product.getName().toLowerCase().contains(subCat.toLowerCase()) ||
                                        product.getDescription().toLowerCase().contains(subCat.toLowerCase())) {
                                    temp.add(product);
                                }
                            }
                        }
                        filteredList = temp;
                    }

                    if (!adv.skinTypes.isEmpty()) {
                        List<ProductEntity> temp = new ArrayList<>();
                        for (ProductEntity product : filteredList) {
                            if (product.getSuitableFor().toLowerCase().contains("mọi loại da")) {
                                temp.add(product);
                            } else {
                                boolean match = false;
                                for (String skinType : adv.skinTypes) {
                                    if (product.getSuitableFor().toLowerCase().contains(skinType.toLowerCase())) {
                                        match = true;
                                        break;
                                    }
                                }
                                if (match) temp.add(product);
                            }
                        }
                        filteredList = temp;
                    }

                    if (adv.priceRange != null) {
                        List<ProductEntity> temp = new ArrayList<>();
                        for (ProductEntity product : filteredList) {
                            boolean match = false;
                            long price = product.getPrice();
                            switch (adv.priceRange) {
                                case "Dưới 100.000đ":
                                    if (price < 100000) match = true;
                                    break;
                                case "100.000đ - 300.000đ":
                                    if (price >= 100000 && price <= 300000) match = true;
                                    break;
                                case "300.000đ - 500.000đ":
                                    if (price > 300000 && price <= 500000) match = true;
                                    break;
                                case "Trên 500.000đ":
                                    if (price > 500000) match = true;
                                    break;
                                default:
                                    match = true;
                            }
                            if (match) temp.add(product);
                        }
                        filteredList = temp;
                    }

                    if (!adv.benefits.isEmpty()) {
                        List<ProductEntity> temp = new ArrayList<>();
                        for (ProductEntity product : filteredList) {
                            boolean match = false;
                            for (String benefit : adv.benefits) {
                                if (product.getDescription().toLowerCase().contains(benefit.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                                if (product.getBenefits() != null) {
                                    for (String b : product.getBenefits()) {
                                        if (b.toLowerCase().contains(benefit.toLowerCase())) {
                                            match = true;
                                            break;
                                        }
                                    }
                                }
                                if (match) break;
                            }
                            if (match) temp.add(product);
                        }
                        filteredList = temp;
                    }

                    if (!adv.ingredients.isEmpty()) {
                        List<ProductEntity> temp = new ArrayList<>();
                        for (ProductEntity product : filteredList) {
                            boolean match = false;
                            for (String ingredient : adv.ingredients) {
                                if (product.getName().toLowerCase().contains(ingredient.toLowerCase()) ||
                                        product.getDescription().toLowerCase().contains(ingredient.toLowerCase())) {
                                    match = true;
                                    break;
                                }
                                if (product.getDetailedIngredients() != null) {
                                    for (String i : product.getDetailedIngredients()) {
                                        if (i.toLowerCase().contains(ingredient.toLowerCase())) {
                                            match = true;
                                            break;
                                        }
                                    }
                                }
                                if (match) break;
                            }
                            if (match) temp.add(product);
                        }
                        filteredList = temp;
                    }

                    if ("NEWEST".equals(sort)) {
                        Collections.sort(filteredList, (p1, p2) -> {
                            int newCompare = Boolean.compare(p2.isNew(), p1.isNew());
                            if (newCompare != 0) return newCompare;
                            return p1.getName().compareTo(p2.getName());
                        });
                    } else if ("PRICE_LOW".equals(sort)) {
                        Collections.sort(filteredList, (p1, p2) -> Long.compare(p1.getPrice(), p2.getPrice()));
                    } else if ("PRICE_HIGH".equals(sort)) {
                        Collections.sort(filteredList, (p1, p2) -> Long.compare(p2.getPrice(), p1.getPrice()));
                    }

                    return filteredList;
                }
        );

        products = FlowLiveDataConversions.asLiveData(combinedFlow, kotlin.coroutines.EmptyCoroutineContext.INSTANCE, 5000L);

        refreshProducts();
    }

    public LiveData<List<String>> getSubcategories() { return subcategories; }
    public LiveData<Set<String>> getSkinTypesFilter() { return skinTypesFilter; }
    public Set<String> getCurrentSkinTypes() { return _skinTypesFilter.getValue(); }
    public LiveData<String> getPriceRangeFilter() { return priceRangeFilter; }
    public String getCurrentPriceRange() { return _priceRangeFilter.getValue(); }
    public LiveData<Set<String>> getBenefitsFilter() { return benefitsFilter; }
    public Set<String> getCurrentBenefits() { return _benefitsFilter.getValue(); }
    public LiveData<Set<String>> getIngredientsFilter() { return ingredientsFilter; }
    public Set<String> getCurrentIngredients() { return _ingredientsFilter.getValue(); }
    public LiveData<List<ProductEntity>> getProducts() { return products; }

    public void setCategoryFilter(String category) {
        _categoryFilter.setValue(category);
        _subcategoryFilter.setValue("Tất cả");
        updateSubcategories(category != null ? category : "Tất cả");
    }

    public void setSubcategoryFilter(String subcategory) {
        _subcategoryFilter.setValue(subcategory);
    }

    public void setSortOrder(String order) {
        _sortOrder.setValue(order);
    }

    public void setAdvancedFilters(Set<String> skinTypes, String priceRange, Set<String> benefits, Set<String> ingredients) {
        _skinTypesFilter.setValue(skinTypes);
        _priceRangeFilter.setValue(priceRange);
        _benefitsFilter.setValue(benefits);
        _ingredientsFilter.setValue(ingredients);
    }

    public void clearAdvancedFilters() {
        _skinTypesFilter.setValue(new HashSet<>());
        _priceRangeFilter.setValue(null);
        _benefitsFilter.setValue(new HashSet<>());
        _ingredientsFilter.setValue(new HashSet<>());
    }

    private void updateSubcategories(String category) {
        List<String> list = new ArrayList<>();
        list.add("Tất cả");
        if (category != null) {
            switch (category) {
                case "Chăm sóc da":
                    list.add("Chống nắng"); list.add("Tẩy trang"); list.add("Sữa rửa mặt");
                    list.add("Tẩy da chết mặt"); list.add("Mặt nạ"); list.add("Nước cân bằng");
                    list.add("Tinh chất"); list.add("Kem dưỡng"); list.add("Xịt khoáng");
                    break;
                case "Tắm & Dưỡng thể":
                    list.add("Tẩy da chết cơ thể"); list.add("Sữa tắm"); list.add("Dưỡng thể");
                    break;
                case "Dưỡng môi":
                    list.add("Tẩy da chết môi"); list.add("Dưỡng ẩm môi");
                    break;
                case "Combo/Giftbox":
                    list.add("Chăm sóc da mặt"); list.add("Chăm sóc cơ thể");
                    list.add("Chăm sóc mái tóc"); list.add("Chăm sóc môi");
                    break;
            }
        }
        _subcategories.setValue(list);
    }

    public void refreshProducts() {
        kotlinx.coroutines.BuildersKt.launch(
                ViewModelKt.getViewModelScope(this),
                kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (coroutineScope, continuation) -> {
                    try {
                        repository.refreshProducts(continuation);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }
}
