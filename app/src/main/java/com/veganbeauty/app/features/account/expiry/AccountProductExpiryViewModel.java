package com.veganbeauty.app.features.account.expiry;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;
import com.veganbeauty.app.data.repository.ProductRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class AccountProductExpiryViewModel extends ViewModel {

    private final ProductRepository repository;
    private final String userId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    public final LiveData<String> searchQuery = _searchQuery;

    private final MutableLiveData<ExpiryFilterState> _selectedFilter = new MutableLiveData<>(ExpiryFilterState.ALL);
    public final LiveData<ExpiryFilterState> selectedFilter = _selectedFilter;

    private final MediatorLiveData<List<ExpiryProductUiModel>> _allExpiryProducts = new MediatorLiveData<>();
    public final LiveData<List<ExpiryProductUiModel>> allExpiryProducts = _allExpiryProducts;

    private final MutableLiveData<List<ExpiryProductUiModel>> _soonExpiryProducts = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<ExpiryProductUiModel>> soonExpiryProducts = _soonExpiryProducts;

    private Date baselineDate;
    private List<UserProductExpiryEntity> currentProducts = new ArrayList<>();

    public AccountProductExpiryViewModel(ProductRepository repository, String userId) {
        this.repository = repository;
        this.userId = userId;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            baselineDate = sdf.parse("04/06/2026");
        } catch (Exception e) {
            baselineDate = new Date();
        }

        executor.execute(() -> {
            try {
                repository.seedExpiryProductsIfEmpty(userId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        _allExpiryProducts.addSource(_searchQuery, query -> updateAllProducts());
        _allExpiryProducts.addSource(_selectedFilter, filter -> updateAllProducts());

        observeProducts();
    }

    private void observeProducts() {
        Flow<List<UserProductExpiryEntity>> productsFlow = repository.getExpiryProductsForUser(userId);
        executor.execute(() -> {
            try {
                productsFlow.collect(new FlowCollector<List<UserProductExpiryEntity>>() {
                    @Override
                    public Object emit(List<UserProductExpiryEntity> value, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        currentProducts = value != null ? value : new ArrayList<>();
                        updateAllProducts();
                        updateSoonProducts();
                        return kotlin.Unit.INSTANCE;
                    }
                }, new kotlin.coroutines.Continuation<kotlin.Unit>() {
                    @NonNull
                    @Override
                    public kotlin.coroutines.CoroutineContext getContext() {
                        return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                    }
                    @Override
                    public void resumeWith(@NonNull Object o) {}
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateAllProducts() {
        String query = _searchQuery.getValue() != null ? _searchQuery.getValue().toLowerCase() : "";
        ExpiryFilterState filter = _selectedFilter.getValue() != null ? _selectedFilter.getValue() : ExpiryFilterState.ALL;

        List<ExpiryProductUiModel> filtered = new ArrayList<>();
        for (UserProductExpiryEntity product : currentProducts) {
            ExpiryProductUiModel uiModel = mapToUiModel(product.toProductEntity());
            boolean matchesQuery = uiModel.getProduct().getName().toLowerCase().contains(query);
            boolean matchesFilter = false;

            switch (filter) {
                case ALL:
                    matchesFilter = true;
                    break;
                case EXPIRED:
                    matchesFilter = uiModel.getRemainingDays() <= 0;
                    break;
                case SOON:
                    matchesFilter = uiModel.isUrgent();
                    break;
                case VALID:
                    matchesFilter = uiModel.getRemainingDays() > 14;
                    break;
            }

            if (matchesQuery && matchesFilter) {
                filtered.add(uiModel);
            }
        }

        Collections.sort(filtered, Comparator.comparingInt(ExpiryProductUiModel::getRemainingDays));
        _allExpiryProducts.postValue(filtered);
    }

    private void updateSoonProducts() {
        List<ExpiryProductUiModel> soon = new ArrayList<>();
        for (UserProductExpiryEntity product : currentProducts) {
            ExpiryProductUiModel uiModel = mapToUiModel(product.toProductEntity());
            if (uiModel.isUrgent()) {
                soon.add(uiModel);
            }
        }
        Collections.sort(soon, Comparator.comparingInt(ExpiryProductUiModel::getRemainingDays));
        _soonExpiryProducts.postValue(soon);
    }

    public void setSearchQuery(String query) {
        _searchQuery.setValue(query);
    }

    public void setSelectedFilter(ExpiryFilterState filter) {
        _selectedFilter.setValue(filter);
    }

    public void deleteExpiryProduct(String productId) {
        executor.execute(() -> {
            try {
                repository.deleteExpiryProduct(userId, productId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private ExpiryProductUiModel mapToUiModel(ProductEntity product) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date expiry = null;
        try {
            if (product.getExpiryDate() != null) {
                expiry = sdf.parse(product.getExpiryDate());
            }
        } catch (Exception e) {
            // ignored
        }

        if (expiry == null) {
            return new ExpiryProductUiModel(product, 365, "HSD Không xác định", 10, false);
        }

        long diffMs = expiry.getTime() - baselineDate.getTime();
        int diffDays = (int) (diffMs / (1000 * 60 * 60 * 24));

        boolean isUrgent = diffDays >= 1 && diffDays <= 14;
        String text;
        int progress;

        if (diffDays <= 0) {
            text = "Hết hạn";
            progress = 100;
        } else if (diffDays < 30) {
            int weeks = diffDays / 7;
            text = weeks > 0 ? "Còn " + weeks + " tuần" : "Còn " + diffDays + " ngày";
            progress = 80 + (30 - diffDays) / 2;
        } else {
            int months = diffDays / 30;
            text = "Còn " + months + " tháng";
            int baseProgress = Math.max(0, 730 - diffDays) * 100 / 730;
            progress = Math.max(10, Math.min(60, baseProgress));
        }

        return new ExpiryProductUiModel(product, diffDays, text, progress, isUrgent);
    }
}
