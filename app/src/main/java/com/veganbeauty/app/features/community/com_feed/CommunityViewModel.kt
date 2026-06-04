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
    val exploreVideos = repository.exploreVideos.asLiveData()
    val ingredients = repository.allIngredients.asLiveData()
    val blogs = repository.allBlogs.asLiveData()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshCommunityData()
        }
    }
}
