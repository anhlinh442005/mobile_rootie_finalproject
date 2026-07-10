package com.veganbeauty.app.features.community.com_feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.data.repository.CommunityRepository;

import java.util.List;

public class CommunityViewModel extends ViewModel {
    private final CommunityRepository repository;

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
    }

    public void refreshData() {
        repository.refreshCommunityData();
    }

    public void createPost(CommunityPostEntity post) {
        repository.createPost(post);
    }

    public LiveData<List<CommunityPostEntity>> getPosts() { return posts; }
    public LiveData<List<UserEntity>> getUsers() { return users; }
    public LiveData<List<ReelEntity>> getReels() { return reels; }
    public LiveData<List<YtVideoEntity>> getExploreVideos() { return exploreVideos; }
    public LiveData<List<IngredientEntity>> getIngredients() { return ingredients; }
}
