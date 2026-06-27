package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.dao.CommunityDao;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.BuildersKt;

public class CommunityRepository {

    private final CommunityDao communityDao;
    private final LocalJsonReader localJsonReader;
    private final FirestoreService firestoreService;

    public CommunityRepository(CommunityDao communityDao, LocalJsonReader localJsonReader, FirestoreService firestoreService) {
        this.communityDao = communityDao;
        this.localJsonReader = localJsonReader;
        this.firestoreService = firestoreService;
    }

    public Flow<List<CommunityPostEntity>> getAllPosts() {
        return communityDao.getAllPosts();
    }

    public Flow<List<UserEntity>> getAllUsers() {
        return communityDao.getAllUsers();
    }

    public Flow<List<ReelEntity>> getAllReels() {
        return communityDao.getAllReels();
    }

    public Flow<List<YtVideoEntity>> getExploreVideos() {
        return communityDao.getAllExploreVideos();
    }

    public Flow<List<IngredientEntity>> getAllIngredients() {
        return communityDao.getAllIngredients();
    }

    public Flow<List<CommunityBlogEntity>> getAllBlogs() {
        return communityDao.getAllBlogs();
    }

    public Object refreshCommunityData(kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, suspendContinuation) -> {
            try {
                communityDao.deleteAllPosts();
                communityDao.deleteAllUsers();
                communityDao.deleteAllExploreVideos();

                loadFromLocalAssets();

                try {
                    firestoreService.forceSyncCollection("users", localJsonReader.getRawUsersJson(), "user_id");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    firestoreService.forceSyncCollection("skin_history", localJsonReader.getRawSkinHistoryJson(), "id");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    firestoreService.forceSyncCollection("skin_bookings", localJsonReader.getRawSkinBookingsJson(), "id", "bookings");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    private void loadFromLocalAssets() {
        try {
            List<UserEntity> localUsers = localJsonReader.getUsers();
            
            List<CommunityPostEntity> rawPosts = localJsonReader.getCommunityPosts();
            List<CommunityPostEntity> localPosts = new ArrayList<>();
            if (rawPosts != null) {
                for (CommunityPostEntity post : rawPosts) {
                    UserEntity matchedUser = null;
                    if (localUsers != null) {
                        for (UserEntity u : localUsers) {
                            if (u.getUser_id() != null && u.getUser_id().equals(post.getAuthorId()) ||
                                (u.getUsername() != null && u.getUsername().equalsIgnoreCase(post.getAuthorUsername()))) {
                                matchedUser = u;
                                break;
                            }
                        }
                    }
                    if (matchedUser != null && matchedUser.getAvatar() != null && !matchedUser.getAvatar().isEmpty()) {
                        post.setAuthorAvatarUrl(matchedUser.getAvatar());
                    }
                    localPosts.add(post);
                }
            }

            List<ReelEntity> rawReels = localJsonReader.getReels();
            List<ReelEntity> localReels = new ArrayList<>();
            if (rawReels != null) {
                for (ReelEntity reel : rawReels) {
                    UserEntity matchedUser = null;
                    if (localUsers != null) {
                        for (UserEntity u : localUsers) {
                            if (u.getUser_id() != null && u.getUser_id().equals(reel.getAuthorId()) ||
                                (u.getUsername() != null && u.getUsername().equalsIgnoreCase(reel.getAuthorUsername()))) {
                                matchedUser = u;
                                break;
                            }
                        }
                    }
                    if (matchedUser != null && matchedUser.getAvatar() != null && !matchedUser.getAvatar().isEmpty()) {
                        reel.setAuthorAvatarUrl(matchedUser.getAvatar());
                    }
                    localReels.add(reel);
                }
            }

            List<YtVideoEntity> rawVideos = localJsonReader.getExploreVideos();
            List<YtVideoEntity> localVideos = new ArrayList<>();
            if (rawVideos != null) {
                for (YtVideoEntity video : rawVideos) {
                    UserEntity matchedUser = null;
                    if (localUsers != null) {
                        for (UserEntity u : localUsers) {
                            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(video.getUsername())) {
                                matchedUser = u;
                                break;
                            }
                        }
                    }
                    if (matchedUser != null && matchedUser.getAvatar() != null && !matchedUser.getAvatar().isEmpty()) {
                        video.setAvatarUrl(matchedUser.getAvatar());
                    }
                    localVideos.add(video);
                }
            }

            if (localUsers != null && !localUsers.isEmpty()) {
                communityDao.deleteAllUsers();
                communityDao.insertUsers(localUsers);
            }
            if (!localPosts.isEmpty()) {
                communityDao.deleteAllPosts();
                communityDao.insertPosts(localPosts);
            }
            if (!localReels.isEmpty()) {
                communityDao.insertReels(localReels);
            }
            if (!localVideos.isEmpty()) {
                communityDao.deleteAllExploreVideos();
                communityDao.insertExploreVideos(localVideos);
            }

            List<IngredientEntity> localIngredients = localJsonReader.getIngredients();
            if (localIngredients != null && !localIngredients.isEmpty()) {
                communityDao.insertIngredients(localIngredients);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
