package com.veganbeauty.app.features.account.order;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;

public class OrderListViewModel extends RootieViewModel {

    private final OrderRepository repository;
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<String> _selectedStatus = new MutableLiveData<>("Tất cả");
    public final LiveData<String> selectedStatus = _selectedStatus;

    private final MediatorLiveData<List<OrderEntity>> _filteredOrders = new MediatorLiveData<>();
    public final LiveData<List<OrderEntity>> filteredOrders = _filteredOrders;

    private final MutableLiveData<String> _orderStats = new MutableLiveData<>("");
    public final LiveData<String> orderStats = _orderStats;

    private List<OrderEntity> currentOrders = new ArrayList<>();
    private boolean roomSourceAttached = false;

    public OrderListViewModel(OrderRepository repository, Context appContext) {
        this.repository = repository;
        this.appContext = appContext;
        loadOrders();
    }

    private static final class BuyerIdentity {
        final String userId;
        final String phone;

        BuyerIdentity(String userId, String phone) {
            this.userId = userId;
            this.phone = phone;
        }
    }

    private BuyerIdentity resolveBuyerIdentity() {
        if (ProfileSession.isLoggedIn(appContext)) {
            String userId = ProfileSessionHelper.getEffectiveUserId(appContext);
            if (userId != null && userId.trim().isEmpty()) {
                userId = null;
            }
            String phone = ProfileSession.getPhone(appContext);
            if (phone != null && phone.trim().isEmpty()) {
                phone = null;
            }
            return new BuyerIdentity(userId, phone);
        }

        String guestPhone = ProfileSession.getGuestPhone(appContext);
        if (guestPhone != null && guestPhone.trim().isEmpty()) {
            guestPhone = null;
        }
        return new BuyerIdentity(null, guestPhone);
    }

    private void loadOrders() {
        executor.execute(() -> {
            repository.syncOrdersFromAssetsBlocking();
            BuyerIdentity identity = resolveBuyerIdentity();
            List<OrderEntity> assetOrders = repository.filterBuyerOrdersFromAssets(identity.userId, identity.phone);
            mainHandler.post(() -> {
                applyOrders(assetOrders);
                attachRoomSourceIfNeeded(identity.userId, identity.phone);
            });
        });
    }

    private void applyOrders(List<OrderEntity> orders) {
        currentOrders = orders != null ? new ArrayList<>(orders) : new ArrayList<>();
        updateFilteredOrders(currentOrders, _selectedStatus.getValue());
    }

    private void attachRoomSourceIfNeeded(String userId, String phone) {
        if (roomSourceAttached) {
            return;
        }
        roomSourceAttached = true;

        Flow<List<OrderEntity>> scopedOrders = repository.getOrdersForBuyer(userId, phone);
        LiveData<List<OrderEntity>> ordersLiveData = FlowLiveDataConversions.asLiveData(scopedOrders);
        _filteredOrders.addSource(ordersLiveData, roomOrders -> {
            if (roomOrders != null) {
                applyOrders(roomOrders);
            }
        });
        _filteredOrders.addSource(_selectedStatus, status ->
                updateFilteredOrders(currentOrders, status));
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
        _filteredOrders.setValue(filtered);

        int total = orders.size();
        int pending = 0;
        for (OrderEntity order : orders) {
            if ("Chờ xác nhận".equalsIgnoreCase(order.getStatus())) {
                pending++;
            }
        }
        _orderStats.setValue(total + " đơn • " + pending + " chờ xác nhận");
    }

    public void refreshOrders() {
        loadOrders();
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
