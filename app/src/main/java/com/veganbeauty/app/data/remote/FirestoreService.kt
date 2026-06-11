package com.veganbeauty.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.app.data.local.entities.*
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchAllProducts(): List<ProductEntity> {
        return try {
            val snapshot = db.collection("products").get().await()
            snapshot.documents.mapNotNull { doc ->
                // Mapping Firestore document to ProductEntity
                @Suppress("UNCHECKED_CAST")
                val albumList = doc.get("album") as? List<String> ?: emptyList()
                
                @Suppress("UNCHECKED_CAST")
                val keyIngredientsRaw = doc.get("keyIngredients") as? List<Map<String, Any>> ?: emptyList()
                val keyIngredientsList = keyIngredientsRaw.map { map ->
                    com.veganbeauty.app.data.local.entities.KeyIngredient(
                        name = map["name"] as? String ?: "",
                        description = map["description"] as? String ?: ""
                    )
                }
                
                @Suppress("UNCHECKED_CAST")
                val detailedList = doc.get("detailedIngredients") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val idealList = doc.get("idealFor") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val benefitsList = doc.get("benefits") as? List<String> ?: emptyList()

                val categoryIdRaw = doc.get("categoryId")
                val categoryIdsStr = when (categoryIdRaw) {
                    is List<*> -> categoryIdRaw.filterIsInstance<String>().joinToString(",")
                    is String -> categoryIdRaw
                    else -> ""
                }

                ProductEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    sku = doc.getString("sku") ?: "",
                    price = doc.getLong("price") ?: 0L,
                    category = doc.getString("category") ?: "",
                    categoryIds = categoryIdsStr,
                    brand = doc.getString("brand") ?: "",
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    description = doc.getString("description") ?: "",
                    mainImage = doc.getString("mainImage") ?: "",
                    suitableFor = doc.getString("suitableFor") ?: "",
                    origin = doc.getString("origin") ?: "",
                    expiryDate = doc.getString("expiryDate") ?: "",
                    isNew = doc.getBoolean("isNew") ?: false,
                    
                    album = albumList,
                    mainIngredientsSummary = doc.getString("mainIngredientsSummary") ?: "",
                    allergyInformation = doc.getString("allergyInformation") ?: "",
                    keyIngredients = keyIngredientsList,
                    detailedIngredients = detailedList,
                    storyDescription = doc.getString("storyDescription") ?: "",
                    storyImage = doc.getString("storyImage") ?: "",
                    idealFor = idealList,
                    benefits = benefitsList,
                    usage = doc.getString("usage") ?: "",
                    usageAmount = doc.getString("usageAmount") ?: "",
                    scent = doc.getString("scent") ?: "",
                    notes = doc.getString("notes") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun fetchAllUsers(): List<UserEntity> {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                UserEntity(
                    user_id = doc.id,
                    username = doc.getString("username") ?: "",
                    full_name = "",
                    email = "",
                    phone = "",
                    password = "",
                    avatar = doc.getString("avatar") ?: doc.getString("avatar_url") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveUser(user: UserEntity): Boolean {
        return try {
            val userMap = hashMapOf(
                "username" to user.username,
                "avatar" to (user.avatar ?: ""),
                "email" to user.email,
                "phone" to user.phone,
                "full_name" to user.full_name,
                "password" to user.password // Note: Plain text password saving in real apps is bad practice, but hashing is already applied in AuthRepository
            )
            db.collection("users").document(user.user_id).set(userMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllCommunityPosts(): List<CommunityPostEntity> {
        return try {
            val snapshot = db.collection("community_posts").get().await()
            snapshot.documents.mapNotNull { doc ->
                val authorMap = doc.get("author") as? Map<String, Any>
                
                val mediaRaw = doc.get("media") as? List<Any>
                val mediaUrls = mediaRaw?.mapNotNull { 
                    if (it is String) it
                    else if (it is Map<*, *>) it["url"] as? String
                    else null
                }?.joinToString(",") ?: ""

                val productsRaw = doc.get("linked_products") as? List<Any>
                val linkedProductIds = productsRaw?.mapNotNull {
                    if (it is String) it
                    else if (it is Map<*, *>) it["product_id"] as? String
                    else null
                }?.joinToString(",") ?: ""
                
                val reactionsMap = doc.get("reactions") as? Map<String, Any>
                val likesCount = (reactionsMap?.get("like") as? Number)?.toInt() ?: 0

                CommunityPostEntity(
                    postId = doc.id,
                    authorId = authorMap?.get("user_id")?.toString() ?: "",
                    authorUsername = authorMap?.get("username")?.toString() ?: "",
                    authorDisplayName = authorMap?.get("display_name")?.toString() ?: "",
                    authorAvatarUrl = authorMap?.get("avatar_url")?.toString() ?: authorMap?.get("avatar")?.toString() ?: "",
                    content = doc.getString("content") ?: "",
                    mediaUrlsString = mediaUrls,
                    createdAt = doc.getString("created_at") ?: "",
                    likesCount = likesCount,
                    commentsCount = doc.getLong("comments_count")?.toInt() ?: 0,
                    skinType = doc.getString("skin_type") ?: "",
                    concern = doc.getString("concern") ?: "",
                    type = doc.getString("type") ?: "",
                    linkedProductIds = linkedProductIds
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun fetchAllReels(): List<ReelEntity> {
        return try {
            val snapshot = db.collection("community_reels_fb").get().await()
            snapshot.documents.mapNotNull { doc ->
                val authorMap = doc.get("author") as? Map<String, Any>
                val statsMap = doc.get("stats") as? Map<String, Any>
                val videoMap = doc.get("video") as? Map<String, Any>
                
                ReelEntity(
                    videoId = doc.id,
                    caption = doc.getString("caption") ?: "",
                    authorId = authorMap?.get("user_id")?.toString() ?: "",
                    authorUsername = authorMap?.get("username")?.toString() ?: "",
                    authorDisplayName = authorMap?.get("display_name")?.toString() ?: "",
                    authorAvatarUrl = authorMap?.get("avatar_url")?.toString() ?: authorMap?.get("avatar")?.toString() ?: "",
                    likesCount = doc.getLong("likes_count")?.toInt() ?: 0,
                    commentsCount = doc.getLong("comments_count")?.toInt() ?: 0,
                    shareCount = (statsMap?.get("shares") as? Number)?.toInt() ?: 0,
                    thumbnailUrl = videoMap?.get("thumbnail")?.toString() ?: doc.getString("thumbnail_url") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun fetchAllExploreVideos(): List<YtVideoEntity> {
        return try {
            val snapshot = db.collection("community_video_yt").get().await()
            snapshot.documents.mapNotNull { doc ->
                val typeRaw = doc.get("Type") as? List<String>
                val typeStr = typeRaw?.joinToString(",") ?: ""
                YtVideoEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    url = doc.getString("url") ?: "",
                    description = doc.getString("short_description") ?: doc.getString("title") ?: "",
                    username = doc.getString("username") ?: "",
                    avatarUrl = doc.getString("avatar") ?: "",
                    type = typeStr
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun uploadAllExploreVideos(videos: List<YtVideoEntity>): Boolean {
        return try {
            val collection = db.collection("community_video_yt")
            
            // Delete old documents
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                collection.document(doc.id).delete().await()
            }
            
            for (video in videos) {
                val data = hashMapOf(
                    "title" to video.title,
                    "url" to video.url,
                    "short_description" to video.description,
                    "username" to video.username,
                    "avatar" to video.avatarUrl,
                    "Type" to video.type.split(",")
                )
                collection.document(video.id).set(data).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadAllCommunityMessages(messagesJson: String): Boolean {
        return try {
            val collection = db.collection("community_message")
            
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                collection.document(doc.id).delete().await()
            }
            
            val jsonArray = org.json.JSONArray(messagesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val map = hashMapOf<String, Any>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = obj.get(key)
                    if (value is org.json.JSONArray) {
                        val list = mutableListOf<Map<String, Any>>()
                        for (j in 0 until value.length()) {
                            val childObj = value.getJSONObject(j)
                            val childMap = hashMapOf<String, Any>()
                            val childKeys = childObj.keys()
                            while (childKeys.hasNext()) {
                                val childKey = childKeys.next()
                                childMap[childKey] = childObj.get(childKey)
                            }
                            list.add(childMap)
                        }
                        map[key] = list
                    } else {
                        map[key] = value
                    }
                }
                collection.document(obj.optString("id")).set(map).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllIngredients(): List<IngredientEntity> {
        return try {
            val snapshot = db.collection("ingredients").get().await()
            snapshot.documents.mapNotNull { doc ->
                IngredientEntity(
                    slug = doc.getString("slug") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    scientificName = doc.getString("scientificName") ?: doc.getString("scientific_name") ?: "",
                    image = doc.getString("image") ?: doc.getString("imageUrl") ?: doc.getString("image_url") ?: "",
                    description = doc.getString("description") ?: "",
                    uses = doc.getString("uses") ?: ""
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun uploadCommunityPost(post: CommunityPostEntity): Boolean {
        return try {
            val postMap = hashMapOf(
                "author" to mapOf(
                    "user_id" to post.authorId,
                    "username" to post.authorUsername,
                    "display_name" to post.authorDisplayName,
                    "avatar" to post.authorAvatarUrl
                ),
                "content" to post.content,
                "media" to post.mediaUrlsString.split(",").filter { it.isNotBlank() },
                "created_at" to post.createdAt,
                "reactions" to mapOf("like" to post.likesCount),
                "comments_count" to post.commentsCount,
                "skin_type" to post.skinType,
                "concern" to post.concern,
                "type" to post.type,
                "linked_products" to (post.linkedProductIds ?: "").split(",").filter { it.isNotBlank() }
            )
            db.collection("community_posts").document(post.postId).set(postMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadUserMemory(memory: UserMemoryEntity): Boolean {
        return try {
            val memoryMap = hashMapOf(
                "actionType" to memory.actionType,
                "targetUserId" to memory.targetUserId,
                "targetUsername" to memory.targetUsername,
                "targetAvatar" to memory.targetAvatar,
                "content" to memory.content,
                "timestamp" to memory.timestamp
            )
            db.collection("user_memory").document(memory.id).set(memoryMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAllStores(): List<StoreEntity> {
        return try {
            val snapshot = db.collection("stores").get().await()
            snapshot.documents.mapNotNull { doc ->
                val diaChiMap = doc.get("dia_chi") as? Map<String, Any>
                val toaDoMap = doc.get("toa_do") as? Map<String, Any>
                val lienHeMap = doc.get("thong_tin_lien_he") as? Map<String, Any>
                val hoatDongMap = doc.get("thoi_gian_hoat_dong") as? Map<String, Any>
                
                val thu26Map = hoatDongMap?.get("thu_2_6") as? Map<String, Any>
                val moCua = thu26Map?.get("mo_cua") as? String ?: "07:00"
                val dongCua = thu26Map?.get("dong_cua") as? String ?: "21:00"

                val phoneList = lienHeMap?.get("so_dien_thoai") as? List<*>
                val phoneStr = phoneList?.filterIsInstance<String>()?.joinToString(", ") ?: ""

                StoreEntity(
                    id = doc.id,
                    maCuaHang = doc.getString("ma_cua_hang") ?: "",
                    tenCuaHang = doc.getString("ten_cua_hang") ?: "",
                    loaiHinh = doc.getString("loai_hinh") ?: "",
                    soNha = diaChiMap?.get("so_nha") as? String ?: "",
                    duong = diaChiMap?.get("duong") as? String ?: "",
                    phuongXa = diaChiMap?.get("phuong_xa") as? String ?: "",
                    quanHuyen = diaChiMap?.get("quan_huyen") as? String ?: "",
                    tinhThanh = diaChiMap?.get("tinh_thanh") as? String ?: "",
                    diaChiDayDu = diaChiMap?.get("dia_chi_day_du") as? String ?: doc.getString("dia_chi_day_du") ?: "",
                    lat = (toaDoMap?.get("lat") as? Number)?.toDouble() ?: 0.0,
                    lng = (toaDoMap?.get("lng") as? Number)?.toDouble() ?: 0.0,
                    soDienThoai = phoneStr,
                    email = lienHeMap?.get("email") as? String ?: "",
                    moCua = moCua,
                    dongCua = dongCua,
                    trangThai = doc.getString("trang_thai") ?: "Đang hoạt động",
                    isActive = doc.getBoolean("isActive") ?: doc.getBoolean("is_active") ?: true
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}
