package com.veganbeauty.app.features.account.order;

import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.OrderRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderDetailViewModel extends RootieViewModel {

    private final OrderRepository repository;
    private final String orderId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MediatorLiveData<OrderEntity> _order = new MediatorLiveData<>();
    public final LiveData<OrderEntity> order = _order;

    public OrderDetailViewModel(OrderRepository repository, String orderId) {
        this.repository = repository;
        this.orderId = orderId;

        executor.execute(() -> {
            try {
                repository.syncOrdersFromAssetsBlocking();
                OrderEntity assetOrder = repository.findOrderByIdFromAssets(orderId);
                if (assetOrder != null) {
                    _order.postValue(assetOrder);
                } else {
                    repository.ensureOrderExists(orderId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        LiveData<List<OrderEntity>> orderListLiveData = FlowLiveDataConversions.asLiveData(repository.getOrderById(orderId));
        _order.addSource(orderListLiveData, list -> {
            if (list != null && !list.isEmpty()) {
                _order.setValue(list.get(0));
            }
        });
    }

    public void cancelOrder() {
        executor.execute(() -> {
            try {
                repository.updateOrderStatus(orderId, "Đã hủy");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void confirmReceived() {
        executor.execute(() -> {
            try {
                repository.updateOrderStatus(orderId, "Hoàn tất");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
