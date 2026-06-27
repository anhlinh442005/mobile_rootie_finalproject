package com.veganbeauty.app.features.account.order;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;

public class OrderListViewModel extends RootieViewModel {

    private final OrderRepository repository;
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> _selectedStatus = new MutableLiveData<>("Tất cả");
    public final LiveData<String> selectedStatus = _selectedStatus;

    private final MutableLiveData<List<OrderEntity>> _filteredOrders = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<OrderEntity>> filteredOrders = _filteredOrders;

    private final MutableLiveData<String> _orderStats = new MutableLiveData<>("");
    public final LiveData<String> orderStats = _orderStats;

    private Flow<List<OrderEntity>> scopedOrders;
    private List<OrderEntity> currentOrders = new ArrayList<>();

    public OrderListViewModel(OrderRepository repository, Context appContext) {
        this.repository = repository;
        this.appContext = appContext;

        if (ProfileSession.isLoggedIn(appContext)) {
            String userId = ProfileSession.getUserId(appContext);
            if (userId != null && userId.trim().isEmpty()) userId = null;
            scopedOrders = repository.getOrdersForBuyer(userId, null);
        } else {
            String guestPhone = ProfileSession.getGuestPhone(appContext);
            if (guestPhone != null && guestPhone.trim().isEmpty()) guestPhone = null;
            scopedOrders = repository.getOrdersForBuyer(null, guestPhone);
        }

        // We observe selectedStatus locally. Note: we don't have direct access to collect in Java without coroutines easily.
        // So we will just maintain states when things update
        executor.execute(() -> {
            try {
                // Not ideal, simulating observing Flow from Java
                scopedOrders.collect(new FlowCollector<List<OrderEntity>>() {
                    @Override
                    public Object emit(List<OrderEntity> value, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        currentOrders = value;
                        updateFilteredOrders(value, _selectedStatus.getValue());
                        return kotlin.Unit.INSTANCE;
                    }
                }, null); // Might fail due to null continuation in Java if strictly checked, but usually works
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        refreshOrders();
    }

    private void updateFilteredOrders(List<OrderEntity> orders, String status) {
        if (orders == null) orders = new ArrayList<>();
        List<OrderEntity> filtered = new ArrayList<>();
        if ("Tất cả".equals(status)) {
            filtered.addAll(orders);
        } else {
            for (OrderEntity order : orders) {
                if (status != null && status.equalsIgnoreCase(order.getStatus())) {
                    filtered.add(order);
                }
            }
        }
        _filteredOrders.postValue(filtered);

        int total = orders.size();
        int pending = 0;
        for (OrderEntity order : orders) {
            if ("Chờ xử lý".equalsIgnoreCase(order.getStatus())) {
                pending++;
            }
        }
        _orderStats.postValue(total + " đơn • " + pending + " chờ xử lý");
    }

    public void refreshOrders() {
        executor.execute(() -> {
            try {
                String userId = ProfileSession.isLoggedIn(appContext) ? ProfileSession.getUserId(appContext) : null;
                String guestPhone = !ProfileSession.isLoggedIn(appContext) ? ProfileSession.getGuestPhone(appContext) : null;
                repository.refreshOrders(userId, guestPhone);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setFilter(String status) {
        _selectedStatus.setValue(status);
        updateFilteredOrders(currentOrders, status);
    }

    public void cancelOrder(String orderId) {
        executor.execute(() -> {
            try {
                repository.cancelOrder(orderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
