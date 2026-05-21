package com.veganbeauty.app.features.community.com_feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.data.repository.CommunityRepository
import kotlinx.coroutines.launch

class CommunityViewModel(private val repository: CommunityRepository) : ViewModel() {

    val posts = repository.allPosts.asLiveData()
    val users = repository.allUsers.asLiveData()
    val reels = repository.allReels.asLiveData()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshCommunityData()
        }
    }
}
