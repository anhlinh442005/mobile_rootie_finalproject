package com.veganbeauty.app.features.community.com_feed;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FeedDataCache {
    public static List<CommunityProduct> productsList = null;
    public static List<CommunityPostEntity> newsList = null;
    public static Map<String, List<String>> mySocialData = null;
    private static final List<CommunityPostEntity> pinnedPosts = new ArrayList<>();
    private static CommunityPostEntity featuredNewsPost = null;
    private static String featuredNewsFilter = null;

    public static void addPinnedPost(CommunityPostEntity post) {
        if (post == null || post.getPostId() == null) return;
        String postId = post.getPostId();
        for (int i = pinnedPosts.size() - 1; i >= 0; i--) {
            CommunityPostEntity existing = pinnedPosts.get(i);
            if (existing != null && postId.equals(existing.getPostId())) {
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

    public static void clearFeaturedNews() {
        featuredNewsPost = null;
        featuredNewsFilter = null;
    }

    /** Keeps the same Rootie news card at the top until the user pull-to-refreshes. */
    public static CommunityPostEntity resolveFeaturedNews(String currentFilter) {
        if (newsList == null || newsList.isEmpty()) {
            return null;
        }
        if (featuredNewsPost != null
                && featuredNewsFilter != null
                && featuredNewsFilter.equals(currentFilter)
                && matchesNewsFilter(featuredNewsPost, currentFilter)) {
            for (CommunityPostEntity candidate : newsList) {
                if (candidate != null
                        && featuredNewsPost.getPostId() != null
                        && featuredNewsPost.getPostId().equals(candidate.getPostId())) {
                    return candidate;
                }
            }
            return featuredNewsPost;
        }

        List<CommunityPostEntity> candidates = new ArrayList<>();
        if ("Tất cả".equals(currentFilter)) {
            candidates.addAll(newsList);
        } else {
            for (CommunityPostEntity news : newsList) {
                if (matchesNewsFilter(news, currentFilter)) {
                    candidates.add(news);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        CommunityPostEntity picked = candidates.get(new Random().nextInt(candidates.size()));
        featuredNewsPost = picked;
        featuredNewsFilter = currentFilter;
        return picked;
    }

    private static boolean matchesNewsFilter(CommunityPostEntity news, String currentFilter) {
        if (news == null || "Tất cả".equals(currentFilter)) {
            return true;
        }
        return currentFilter.equalsIgnoreCase(news.getType())
                || currentFilter.equalsIgnoreCase(news.getSkinType())
                || currentFilter.equalsIgnoreCase(news.getConcern());
    }
}
