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

import java.util.List;

import kotlinx.coroutines.flow.Flow;

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

    public void seedFromAssetsSync() {
        seedFromAssets();
    }

    public void refreshCommunityData() {
        new Thread(() -> {
            try {
                seedFromAssets();

                List<CommunityPostEntity> remotePosts = firestoreService.fetchAllCommunityPosts();
                if (remotePosts != null && !remotePosts.isEmpty()) {
                    communityDao.insertPosts(remotePosts);
                }

                List<UserEntity> remoteUsers = firestoreService.fetchAllUsers();
                if (remoteUsers != null && !remoteUsers.isEmpty()) {
                    communityDao.insertUsers(remoteUsers);
                }

                List<ReelEntity> remoteReels = firestoreService.fetchAllReels();
                if (remoteReels != null && !remoteReels.isEmpty()) {
                    communityDao.insertReels(remoteReels);
                }

                List<YtVideoEntity> remoteVideos = firestoreService.fetchAllExploreVideos();
                if (remoteVideos != null && !remoteVideos.isEmpty()) {
                    communityDao.insertExploreVideos(remoteVideos);
                }

                List<IngredientEntity> remoteIngredients = firestoreService.fetchAllIngredients();
                if (remoteIngredients != null && !remoteIngredients.isEmpty()) {
                    communityDao.insertIngredients(remoteIngredients);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void seedFromAssets() {
        try {
            List<CommunityPostEntity> posts = localJsonReader.getCommunityPosts();
            if (posts != null && !posts.isEmpty()) {
                communityDao.insertPosts(posts);
            }

            List<UserEntity> users = localJsonReader.getUsers();
            if (users != null && !users.isEmpty()) {
                communityDao.insertUsers(users);
            }

            List<ReelEntity> reels = localJsonReader.getReels();
            if (reels != null && !reels.isEmpty()) {
                communityDao.insertReels(reels);
            }

            List<YtVideoEntity> videos = localJsonReader.getExploreVideos();
            if (videos != null && !videos.isEmpty()) {
                communityDao.insertExploreVideos(videos);
            }

            List<IngredientEntity> ingredients = localJsonReader.getIngredients();
            if (ingredients != null && !ingredients.isEmpty()) {
                communityDao.insertIngredients(ingredients);
            }

            List<CommunityBlogEntity> blogs = localJsonReader.getCommunityBlogs(200, 0);
            if (blogs != null && !blogs.isEmpty()) {
                communityDao.insertBlogs(blogs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
