package com.veganbeauty.app.features.account.notification;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.data.repository.NotificationRepository;

public class NotificationViewModelFactory implements ViewModelProvider.Factory {
    private final NotificationRepository repository;

    public NotificationViewModelFactory(NotificationRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(NotificationViewModel.class)) {
            return (T) new NotificationViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
