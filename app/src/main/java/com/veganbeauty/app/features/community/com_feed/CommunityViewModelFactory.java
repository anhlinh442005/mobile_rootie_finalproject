package com.veganbeauty.app.features.community.com_feed;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.data.repository.CommunityRepository;

public class CommunityViewModelFactory implements ViewModelProvider.Factory {
    private final CommunityRepository repository;

    public CommunityViewModelFactory(CommunityRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CommunityViewModel.class)) {
            return (T) new CommunityViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
