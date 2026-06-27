package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.UserMemoryEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface CommunityDao {
    @Query("SELECT * FROM community_posts ORDER BY createdAt DESC")
    Flow<List<CommunityPostEntity>> getAllPosts();

    @Query("DELETE FROM community_posts WHERE content LIKE :keyword")
    Object deletePostByContent(String keyword, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("DELETE FROM community_posts WHERE authorId = :authorId")
    Object deletePostsByAuthorId(String authorId, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("DELETE FROM community_posts")
    Object deleteAllPosts(kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("DELETE FROM users")
    Object deleteAllUsers(kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("SELECT * FROM users")
    Flow<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM reels")
    Flow<List<ReelEntity>> getAllReels();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertPosts(List<CommunityPostEntity> posts, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertUsers(List<UserEntity> users, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertReels(List<ReelEntity> reels, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Query("SELECT * FROM explore_videos")
    Flow<List<YtVideoEntity>> getAllExploreVideos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertExploreVideos(List<YtVideoEntity> videos, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Query("DELETE FROM explore_videos")
    Object deleteAllExploreVideos(kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("SELECT * FROM user_memory ORDER BY timestamp DESC")
    Flow<List<UserMemoryEntity>> getAllUserMemories();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertUserMemory(UserMemoryEntity memory, kotlin.coroutines.Continuation<? super Long> continuation);

    @Query("SELECT * FROM ingredients")
    Flow<List<IngredientEntity>> getAllIngredients();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertIngredients(List<IngredientEntity> ingredients, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Query("SELECT * FROM community_blogs ORDER BY publishedAt DESC")
    Flow<List<CommunityBlogEntity>> getAllBlogs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertBlogs(List<CommunityBlogEntity> blogs, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Query("UPDATE community_posts SET commentsCount = commentsCount + 1 WHERE postId = :postId")
    Object incrementCommentsCount(String postId, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE postId = :postId")
    Object incrementLikesCount(String postId, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("UPDATE community_posts SET likesCount = MAX(0, likesCount - 1) WHERE postId = :postId")
    Object decrementLikesCount(String postId, kotlin.coroutines.Continuation<? super Integer> continuation);
}
