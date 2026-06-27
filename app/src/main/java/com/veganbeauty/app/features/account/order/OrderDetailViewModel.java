package com.veganbeauty.app.features.account.order;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.OrderRepository;

public class OrderDetailViewModel extends RootieViewModel {

    private final OrderRepository repository;
    private final String orderId;
    
    private final MutableLiveData<OrderEntity> _order = new MutableLiveData<>();
    public final LiveData<OrderEntity> order = _order;

    public OrderDetailViewModel(OrderRepository repository, String orderId) {
        this.repository = repository;
        this.orderId = orderId;
        
        new Thread(() -> {
            try {
                repository.ensureOrderExists(orderId);
                // Listen to Flow properly in a real app, here we simulate observing it
                repository.getOrderById(orderId).collect(new kotlinx.coroutines.flow.FlowCollector<OrderEntity>() {
                    @Override
                    public Object emit(OrderEntity value, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
                        _order.postValue(value);
                        return kotlin.Unit.INSTANCE;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void cancelOrder() {
        new Thread(() -> {
            try {
                repository.updateOrderStatus(orderId, "Đã hủy");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void confirmReceived() {
        new Thread(() -> {
            try {
                repository.updateOrderStatus(orderId, "Hoàn tất");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void reorder() {
        new Thread(() -> {
            try {
                // Placeholder
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
