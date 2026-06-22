package com.veganbeauty.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.app.data.local.entities.*
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchAllProducts(): List<ProductEntity> {
        return try {
            val snapshot = db.collection("products").get().await()
            snapshot.documents.mapNotNull { doc -> mapProductDocument(doc) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun fetchProductByBarcode(barcode: String): ProductEntity? {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("barcode", barcode.trim())
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.let { doc -> mapProductDocument(doc) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mapProductDocument(doc: com.google.firebase.firestore.DocumentSnapshot): ProductEntity {
        @Suppress("UNCHECKED_CAST")
        val albumList = doc.get("album") as? List<String> ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val keyIngredientsRaw = doc.get("keyIngredients") as? List<Map<String, Any>> ?: emptyList()
        val keyIngredientsList = keyIngredientsRaw.map { map ->
            KeyIngredient(
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

        return ProductEntity(
            id = doc.id,
            name = doc.getString("name") ?: "",
            sku = doc.getString("sku") ?: "",
            barcode = doc.getString("barcode") ?: "",
            price = doc.getLong("price") ?: 0L,
            originalPrice = doc.getLong("originalPrice"),
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
            notes = doc.getString("notes") ?: "",
            rating = doc.getDouble("rating")?.toFloat() ?: 0f,
            sold = doc.getLong("sold")?.toInt() ?: 0
        )
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
                    reupsCount = doc.getLong("reups_count")?.toInt() ?: 0,
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


    suspend fun addCommentToPost(postId: String, commentMap: Map<String, Any>): Boolean {
        return try {
            // Append to comments array and increment comments_count
            db.collection("community_posts").document(postId)
                .update(
                    "comments", com.google.firebase.firestore.FieldValue.arrayUnion(commentMap),
                    "comments_count", com.google.firebase.firestore.FieldValue.increment(1)
                ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun forceSyncLocalPostsToFirebase(postsJson: String, newsJson: String): Boolean {
        return try {
            val collection = db.collection("community_posts")
            
            // Delete old documents
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                collection.document(doc.id).delete().await()
            }
            
            // Upload Posts
            val postsArray = org.json.JSONObject(postsJson).getJSONArray("posts")
            for (i in 0 until postsArray.length()) {
                val obj = postsArray.getJSONObject(i)
                val map = jsonObjectToMap(obj)
                val id = obj.optString("post_id", java.util.UUID.randomUUID().toString())
                collection.document(id).set(map).await()
            }

            // Upload News
            val newsArray = org.json.JSONArray(newsJson)
            for (i in 0 until newsArray.length()) {
                val obj = newsArray.getJSONObject(i)
                val map = jsonObjectToMap(obj)
                val id = obj.optString("post_id", java.util.UUID.randomUUID().toString())
                collection.document(id).set(map).await()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun forceSyncCollection(collectionName: String, jsonStr: String, idKey: String, arrayKey: String? = null): Boolean {
        return try {
            val collection = db.collection(collectionName)
            
            // Delete old documents
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                collection.document(doc.id).delete().await()
            }
            
            // Upload new documents
            val jsonArray = if (arrayKey != null) {
                org.json.JSONObject(jsonStr).getJSONArray(arrayKey)
            } else {
                org.json.JSONArray(jsonStr)
            }
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val map = jsonObjectToMap(obj)
                val id = obj.optString(idKey).takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                collection.document(id).set(map).await()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun jsonObjectToMap(obj: org.json.JSONObject): Map<String, Any> {
        val map = hashMapOf<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.get(key)
            map[key] = when (value) {
                is org.json.JSONArray -> jsonArrayToList(value)
                is org.json.JSONObject -> jsonObjectToMap(value)
                org.json.JSONObject.NULL -> null
                else -> value
            } ?: ""
        }
        return map
    }

    suspend fun clearOldAffiliateData() {
        val collections = listOf(
            "affiliate_orders", "affiliate", "affiliates", "affiliate_wallets",
            "affiliate_products", "showcase_products", "user_showcases",
            "attached_products", "cart_affiliate", "affiliate_dashboard"
        )
        for (colName in collections) {
            try {
                val snapshot = db.collection(colName).get().await()
                for (doc in snapshot.documents) {
                    db.collection(colName).document(doc.id).delete().await()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun syncOrders(orders: List<OrderEntity>) {
        try {
            // Clear existing orders first
            val collection = db.collection("orders")
            val snapshot = collection.get().await()
            for (doc in snapshot.documents) {
                collection.document(doc.id).delete().await()
            }
            // Upload new orders
            for (order in orders) {
                collection.document(order.id).set(order).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun jsonArrayToList(arr: org.json.JSONArray): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until arr.length()) {
            val value = arr.get(i)
            list.add(when (value) {
                is org.json.JSONArray -> jsonArrayToList(value)
                is org.json.JSONObject -> jsonObjectToMap(value)
                org.json.JSONObject.NULL -> null
                else -> value
            } ?: "")
        }
        return list
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
                val tienNghiList = doc.get("tien_nghi") as? List<*>
                val tienNghiStr = tienNghiList?.filterIsInstance<String>()?.joinToString(",") ?: ""
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
                    isActive = doc.getBoolean("isActive") ?: doc.getBoolean("is_active") ?: true,
                    tienNghi = tienNghiStr
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun uploadBooking(booking: BookingHistoryEntity): Boolean {
        return try {
            val storeID = when {
                booking.storeName.contains("Cơ sở 1", ignoreCase = true) || booking.storeAddress.contains("Minh Khai", ignoreCase = true) -> "CH001"
                booking.storeName.contains("Cơ sở 5", ignoreCase = true) || booking.storeAddress.contains("Hoàng Văn Thụ", ignoreCase = true) -> "CH005"
                else -> ""
            }
            val bookingMap = hashMapOf(
                "userId" to booking.userId,
                "userName" to booking.userName,
                "userPhone" to booking.userPhone,
                "userEmail" to booking.userEmail,
                "serviceName" to booking.serviceName,
                "dateDisplay" to booking.dateDisplay,
                "monthDisplay" to booking.monthDisplay,
                "dayOfWeek" to booking.dayOfWeek,
                "time" to booking.time,
                "duration" to booking.duration,
                "storeName" to booking.storeName,
                "storeAddress" to booking.storeAddress,
                "storePhone" to booking.storePhone,
                "storeImage" to booking.storeImage,
                "note" to booking.note,
                "status" to booking.status,
                "policy" to booking.policy,
                "createdAt" to booking.createdAt,
                "completedAt" to booking.completedAt,
                "consultantName" to booking.consultantName,
                "consultantAvatar" to booking.consultantAvatar,
                "consultantRating" to booking.consultantRating.toDouble(),
                "userRating" to booking.userRating.toDouble(),
                "userReview" to booking.userReview,
                "reviewDate" to booking.reviewDate,
                "beforeImage" to booking.beforeImage,
                "afterImage" to booking.afterImage,
                "earnedPoints" to booking.earnedPoints,
                "totalPoints" to booking.totalPoints,
                "nextAppointmentDate" to booking.nextAppointmentDate,
                "nextAppointmentText" to booking.nextAppointmentText,
                "cancelledAt" to booking.cancelledAt,
                "cancelReason" to booking.cancelReason,
                "storeID" to storeID,
                "storeId" to storeID
            )
            db.collection("bookings").document(booking.id).set(bookingMap).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: String, cancelReason: String = ""): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status
            )
            if (cancelReason.isNotEmpty()) {
                updates["cancelReason"] = cancelReason
            }
            db.collection("bookings").document(bookingId).update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchBookingsForUser(email: String): List<BookingHistoryEntity> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("userEmail", email)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    BookingHistoryEntity(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userPhone = doc.getString("userPhone") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        serviceName = doc.getString("serviceName") ?: "",
                        dateDisplay = doc.getString("dateDisplay") ?: "",
                        monthDisplay = doc.getString("monthDisplay") ?: "",
                        dayOfWeek = doc.getString("dayOfWeek") ?: "",
                        time = doc.getString("time") ?: "",
                        duration = doc.getString("duration") ?: "",
                        storeName = doc.getString("storeName") ?: "",
                        storeAddress = doc.getString("storeAddress") ?: "",
                        storePhone = doc.getString("storePhone") ?: "",
                        storeImage = doc.getString("storeImage") ?: "",
                        note = doc.getString("note") ?: "",
                        status = doc.getString("status") ?: "",
                        policy = doc.getString("policy") ?: "",
                        createdAt = doc.getString("createdAt") ?: "",
                        completedAt = doc.getString("completedAt") ?: "",
                        consultantName = doc.getString("consultantName") ?: "",
                        consultantAvatar = doc.getString("consultantAvatar") ?: "",
                        consultantRating = doc.getDouble("consultantRating")?.toFloat() ?: 0.0f,
                        userRating = doc.getDouble("userRating")?.toFloat() ?: 0.0f,
                        userReview = doc.getString("userReview") ?: "",
                        reviewDate = doc.getString("reviewDate") ?: "",
                        beforeImage = doc.getString("beforeImage") ?: "",
                        afterImage = doc.getString("afterImage") ?: "",
                        earnedPoints = doc.getLong("earnedPoints")?.toInt() ?: 0,
                        totalPoints = doc.getLong("totalPoints")?.toInt() ?: 0,
                        nextAppointmentDate = doc.getString("nextAppointmentDate") ?: "",
                        nextAppointmentText = doc.getString("nextAppointmentText") ?: "",
                        cancelledAt = doc.getString("cancelledAt") ?: "",
                        cancelReason = doc.getString("cancelReason") ?: ""
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
