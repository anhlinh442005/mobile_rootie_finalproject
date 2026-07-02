package com.veganbeauty.app.features.community.com_feed;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedDataCache {
    public static List<CommunityProduct> productsList = null;
    public static List<CommunityPostEntity> newsList = null;
    public static Map<String, List<String>> mySocialData = null;
    private static final List<CommunityPostEntity> pinnedPosts = new ArrayList<>();

    public static void addPinnedPost(CommunityPostEntity post) {
        if (post == null) return;
        for (int i = pinnedPosts.size() - 1; i >= 0; i--) {
            if (post.getPostId().equals(pinnedPosts.get(i).getPostId())) {
                pinnedPosts.remove(i);
            }
        }
        pinnedPosts.add(0, post);
    }

    public static List<CommunityPostEntity> getPinnedPosts() {
        return new ArrayList<>(pinnedPosts);
    }

    public static void clearPinnedPosts() {
        pinnedPosts.clear();
    }
}
