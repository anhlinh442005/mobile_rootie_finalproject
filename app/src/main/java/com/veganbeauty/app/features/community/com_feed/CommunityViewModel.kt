package com.veganbeauty.app.features.community.com_feed

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.data.repository.CommunityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommunityViewModel(private val repository: CommunityRepository) : ViewModel() {

    val posts = repository.allPosts.asLiveData()
    val users = repository.allUsers.asLiveData()
    val reels = repository.allReels.asLiveData()

    private val _exploreVideos = MutableLiveData<List<YtVideoEntity>>()
    val exploreVideos: androidx.lifecycle.LiveData<List<YtVideoEntity>> get() = _exploreVideos

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshCommunityData()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val videos = repository.getExploreVideos()
            _exploreVideos.postValue(videos)
        }
    }
}
