package com.veganbeauty.app.features.community.com_feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;

import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.data.repository.CommunityRepository;

import java.util.List;

import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;

public class CommunityViewModel extends ViewModel {
    private final CommunityRepository repository;

    // We can't directly use asLiveData() in Java without extension wrapper, so we leave it as flow or use LiveData builder if needed.
    // For translation parity, assuming repository has livedata or we use androidx.lifecycle.FlowLiveDataConversions
    public final LiveData<List<CommunityPostEntity>> posts;
    public final LiveData<List<UserEntity>> users;
    public final LiveData<List<ReelEntity>> reels;
    public final LiveData<List<YtVideoEntity>> exploreVideos;
    public final LiveData<List<IngredientEntity>> ingredients;
    public final LiveData<List<CommunityBlogEntity>> blogs;

    public CommunityViewModel(CommunityRepository repository) {
        this.repository = repository;
        this.posts = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllPosts());
        this.users = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllUsers());
        this.reels = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllReels());
        this.exploreVideos = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getExploreVideos());
        this.ingredients = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllIngredients());
        this.blogs = androidx.lifecycle.FlowLiveDataConversions.asLiveData(repository.getAllBlogs());
        
        refreshData();
    }

    public void refreshData() {
        // ViewModelKt.getViewModelScope(this).launch
        // Since Java Coroutines interop is tricky, we use standard thread for translation parity or RxJava.
        // For exact parity we'll use a thread since we can't write simple coroutine in Java easily.
        new Thread(() -> {
            try {
                // Assuming refreshCommunityData is suspend, we'd need a runBlocking or Coroutine builder.
                // kotlinx.coroutines.BuildersKt.runBlocking(...)
                // But for now, just calling it if it's synchronous in Java translation.
                repository.refreshCommunityData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
