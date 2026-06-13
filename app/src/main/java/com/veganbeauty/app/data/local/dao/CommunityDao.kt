package com.veganbeauty.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.data.local.entities.UserMemoryEntity
import com.veganbeauty.app.data.local.entities.IngredientEntity
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    @Query("SELECT * FROM community_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<CommunityPostEntity>>

    @Query("DELETE FROM community_posts WHERE content LIKE :keyword")
    suspend fun deletePostByContent(keyword: String): Int

    @Query("DELETE FROM community_posts WHERE authorId = :authorId")
    suspend fun deletePostsByAuthorId(authorId: String): Int

    @Query("DELETE FROM community_posts")
    suspend fun deleteAllPosts(): Int

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers(): Int

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM reels")
    fun getAllReels(): Flow<List<ReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CommunityPostEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelEntity>): List<Long>

    @Query("SELECT * FROM explore_videos")
    fun getAllExploreVideos(): Flow<List<YtVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExploreVideos(videos: List<YtVideoEntity>): List<Long>

    @Query("DELETE FROM explore_videos")
    suspend fun deleteAllExploreVideos(): Int

    @Query("SELECT * FROM user_memory ORDER BY timestamp DESC")
    fun getAllUserMemories(): Flow<List<UserMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMemory(memory: UserMemoryEntity): Long

    @Query("SELECT * FROM ingredients")
    fun getAllIngredients(): Flow<List<IngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>): List<Long>

    @Query("SELECT * FROM community_blogs ORDER BY publishedAt DESC")
    fun getAllBlogs(): Flow<List<CommunityBlogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlogs(blogs: List<CommunityBlogEntity>): List<Long>

    @Query("UPDATE community_posts SET commentsCount = commentsCount + 1 WHERE postId = :postId")
    suspend fun incrementCommentsCount(postId: String)

    @Query("UPDATE community_posts SET likesCount = likesCount + 1 WHERE postId = :postId")
    suspend fun incrementLikesCount(postId: String)

    @Query("UPDATE community_posts SET likesCount = MAX(0, likesCount - 1) WHERE postId = :postId")
    suspend fun decrementLikesCount(postId: String)
}
