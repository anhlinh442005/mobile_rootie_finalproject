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
    int deletePostByContent(String keyword);

    @Query("DELETE FROM community_posts WHERE authorId = :authorId")
    int deletePostsByAuthorId(String authorId);

    @Query("DELETE FROM community_posts")
    int deleteAllPosts();

    @Query("DELETE FROM users")
    int deleteAllUsers();

    @Query("SELECT * FROM users")
    Flow<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM reels")
    Flow<List<ReelEntity>> getAllReels();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertPosts(List<CommunityPostEntity> posts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertUsers(List<UserEntity> users);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertReels(List<ReelEntity> reels);

    @Query("SELECT * FROM explore_videos")
    Flow<List<YtVideoEntity>> getAllExploreVideos();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertExploreVideos(List<YtVideoEntity> videos);

    @Query("DELETE FROM explore_videos")
    int deleteAllExploreVideos();

    @Query("SELECT * FROM user_memory ORDER BY timestamp DESC")
    Flow<List<UserMemoryEntity>> getAllUserMemories();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUserMemory(UserMemoryEntity memory);

    @Query("SELECT * FROM ingredients")
    Flow<List<IngredientEntity>> getAllIngredients();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertIngredients(List<IngredientEntity> ingredients);

    @Query("SELECT * FROM community_blogs ORDER BY publishedAt DESC")
    Flow<List<CommunityBlogEntity>> getAllBlogs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertBlogs(List<CommunityBlogEntity> blogs);

    @Query("UPDATE community_posts SET commentsCount = commentsCount + 1 WHERE postId = :postId")
    int incrementCommentsCount(String postId);

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE postId = :postId")
    int incrementLikesCount(String postId);

    @Query("UPDATE community_posts SET likesCount = MAX(0, likesCount - 1) WHERE postId = :postId")
    int decrementLikesCount(String postId);
}
