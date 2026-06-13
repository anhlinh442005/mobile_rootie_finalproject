package com.veganbeauty.app.features.community.com_feed

object FeedDataCache {
    var productsList: List<com.veganbeauty.app.data.local.entities.CommunityProduct>? = null
    var newsList: List<com.veganbeauty.app.data.local.entities.CommunityPostEntity>? = null
    var mySocialData: Map<String, List<String>>? = null
}
