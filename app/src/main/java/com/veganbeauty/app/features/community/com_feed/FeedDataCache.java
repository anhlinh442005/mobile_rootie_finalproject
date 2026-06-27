package com.veganbeauty.app.features.community.com_feed;

import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import java.util.List;
import java.util.Map;

public class FeedDataCache {
    public static List<CommunityProduct> productsList = null;
    public static List<CommunityPostEntity> newsList = null;
    public static Map<String, List<String>> mySocialData = null;
}
