package com.veganbeauty.app.features.community.com_feed;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.features.community.UserMemoryHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Detects feed list changes that require a full reload vs. count-only updates (like/comment). */
public final class CommunityPostsDiff {

    private CommunityPostsDiff() {
    }

    public static boolean hasStructuralChange(@Nullable List<CommunityPostEntity> oldPosts,
                                              @Nullable List<CommunityPostEntity> newPosts) {
        if (oldPosts == null || newPosts == null) return true;
        Set<String> oldIds = new HashSet<>();
        Set<String> newIds = new HashSet<>();
        for (CommunityPostEntity post : oldPosts) {
            if (post != null && post.getPostId() != null) oldIds.add(post.getPostId());
        }
        for (CommunityPostEntity post : newPosts) {
            if (post != null && post.getPostId() != null) newIds.add(post.getPostId());
        }
        return !oldIds.equals(newIds);
    }

    public static void syncCounts(List<CommunityPostEntity> target,
                                  @Nullable List<CommunityPostEntity> dbPosts) {
        if (target == null || target.isEmpty() || dbPosts == null) return;
        Map<String, CommunityPostEntity> byId = new HashMap<>();
        for (CommunityPostEntity post : dbPosts) {
            if (post != null && post.getPostId() != null) {
                byId.put(post.getPostId(), post);
            }
        }
        for (int i = 0; i < target.size(); i++) {
            CommunityPostEntity local = target.get(i);
            if (local == null || local.getPostId() == null) continue;
            CommunityPostEntity updated = byId.get(local.getPostId());
            if (updated != null) {
                CommunityPostEntity merged = UserMemoryHelper.pickRicherPost(local, updated);
                merged.setLikesCount(updated.getLikesCount());
                merged.setCommentsCount(updated.getCommentsCount());
                merged.setReupsCount(updated.getReupsCount());
                target.set(i, merged);
            }
        }
    }
}
