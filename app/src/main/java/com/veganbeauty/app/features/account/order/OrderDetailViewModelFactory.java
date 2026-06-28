package com.veganbeauty.app.features.account.order;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.veganbeauty.app.data.repository.OrderRepository;

public class OrderDetailViewModelFactory implements ViewModelProvider.Factory {
    private final OrderRepository repository;
    private final String orderId;

    public OrderDetailViewModelFactory(OrderRepository repository, String orderId) {
        this.repository = repository;
        this.orderId = orderId;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(OrderDetailViewModel.class)) {
            return (T) new OrderDetailViewModel(repository, orderId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
