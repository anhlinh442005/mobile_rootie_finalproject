package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.*
import org.json.JSONObject

class LocalJsonReader(private val context: Context) {
    fun getContext(): Context = context

    fun getSocialDataForUser(userId: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>(
            "friends" to emptyList(),
            "following" to emptyList(),
            "followers" to emptyList(),
            "friend_requests" to emptyList(),
            "suggested" to emptyList()
        )
        try {
            val jsonString = context.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("user_id") == userId) {
                    val keys = listOf("friends", "following", "followers", "friend_requests", "suggested")
                    for (key in keys) {
                        if (obj.has(key)) {
                            val jsonArray = obj.getJSONArray(key)
                            val list = mutableListOf<String>()
                            for (j in 0 until jsonArray.length()) {
                                list.add(jsonArray.getString(j))
                            }
                            result[key] = list
                        }
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getMutualFriendsForUsers(myFriends: Set<String>, targetUserIds: List<String>): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        try {
            val jsonString = context.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uid = obj.getString("user_id")
                if (targetUserIds.contains(uid)) {
                    val friendsArray = obj.getJSONArray("friends")
                    val mutuals = mutableListOf<String>()
                    for (j in 0 until friendsArray.length()) {
                        val fid = friendsArray.getString(j)
                        if (myFriends.contains(fid)) {
                            mutuals.add(fid)
                        }
                    }
                    result[uid] = mutuals
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getFriendsForUser(userId: String): List<String> {
        return try {
            val jsonString = context.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("user_id") == userId) {
                    val friendsArray = obj.getJSONArray("friends")
                    val list = mutableListOf<String>()
                    for (j in 0 until friendsArray.length()) {
                        list.add(friendsArray.getString(j))
                    }
                    return list
                }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getShowcaseProductsForUser(userId: String): List<String> {
        val list = mutableSetOf<String>()

        // 1. Get from affiliate_product.json
        try {
            val file = java.io.File(context.filesDir, "affiliate_product_local.json")
            val root = if (file.exists()) {
                org.json.JSONObject(file.readText())
            } else {
                val assetStr = context.assets.open("affiliate_product.json").bufferedReader().use { it.readText() }
                org.json.JSONObject(assetStr)
            }
            val arr = root.optJSONArray("affiliate_products")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("userId") == userId) {
                        val productsArr = obj.optJSONArray("products")
                        if (productsArr != null) {
                            for (j in 0 until productsArr.length()) {
                                val p = productsArr.getJSONObject(j)
                                if (p.optBoolean("affiliate_display", true)) {
                                    list.add(p.optString("productId"))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Get from orders where referrerUserId == userId
        try {
            val allOrders = getAllOrders()
            val affiliateOrders = allOrders.filter { it.isAffiliate && it.affiliate?.referrerUserId == userId }
            for (order in affiliateOrders) {
                for (item in order.items) {
                    list.add(item.productId)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 3. Get from community posts where authorId == userId and has linked products
        try {
            val allPosts = getCommunityPosts()
            val userPosts = allPosts.filter { it.authorId == userId }
            for (post in userPosts) {
                val ids = post.linkedProductIds ?: ""
                if (ids.isNotEmpty()) {
                    ids.split(",").forEach { 
                        if (it.isNotBlank()) list.add(it.trim())
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        return list.toList()
    }

    fun getAllProducts(): List<ProductEntity> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            val productList = mutableListOf<ProductEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val categoryIdRaw = obj.opt("categoryId")
                val categoryIdsStr = when (categoryIdRaw) {
                    is org.json.JSONArray -> {
                        val list = mutableListOf<String>()
                        for (j in 0 until categoryIdRaw.length()) {
                            list.add(categoryIdRaw.getString(j))
                        }
                        list.joinToString(",")
                    }
                    is String -> categoryIdRaw
                    else -> ""
                }

                val albumArr = obj.optJSONArray("album")
                val albumList = mutableListOf<String>()
                if (albumArr != null) {
                    for (j in 0 until albumArr.length()) {
                        albumList.add(albumArr.getString(j))
                    }
                }

                val keyArr = obj.optJSONArray("keyIngredients")
                val keyIngredientsList = mutableListOf<com.veganbeauty.app.data.local.entities.KeyIngredient>()
                if (keyArr != null) {
                    for (j in 0 until keyArr.length()) {
                        val keyObj = keyArr.getJSONObject(j)
                        keyIngredientsList.add(
                            com.veganbeauty.app.data.local.entities.KeyIngredient(
                                name = keyObj.optString("name", ""),
                                description = keyObj.optString("description", "")
                            )
                        )
                    }
                }

                val detailedArr = obj.optJSONArray("detailedIngredients")
                val detailedIngredientsList = mutableListOf<String>()
                if (detailedArr != null) {
                    for (j in 0 until detailedArr.length()) {
                        detailedIngredientsList.add(detailedArr.getString(j))
                    }
                }

                val idealArr = obj.optJSONArray("idealFor")
                val idealForList = mutableListOf<String>()
                if (idealArr != null) {
                    for (j in 0 until idealArr.length()) {
                        idealForList.add(idealArr.getString(j))
                    }
                }

                val benefitsArr = obj.optJSONArray("benefits")
                val benefitsList = mutableListOf<String>()
                if (benefitsArr != null) {
                    for (j in 0 until benefitsArr.length()) {
                        benefitsList.add(benefitsArr.getString(j))
                    }
                }

                productList.add(
                    ProductEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        sku = obj.getString("sku"),
                        barcode = obj.optString("barcode", ""),
                        price = obj.getLong("price"),
                        originalPrice = if (obj.has("originalPrice")) obj.getLong("originalPrice") else null,
                        category = obj.getString("category"),
                        categoryIds = categoryIdsStr,
                        brand = obj.optString("brand", ""),
                        stock = obj.getInt("stock"),
                        description = obj.optString("description", ""),
                        mainImage = obj.optString("mainImage", ""),
                        suitableFor = obj.optString("suitableFor", ""),
                        origin = obj.optString("origin", ""),
                        expiryDate = obj.optString("expiryDate", ""),
                        isNew = obj.optBoolean("newProduct", false) || obj.optBoolean("isNew", false),
                        
                        album = albumList,
                        mainIngredientsSummary = obj.optString("mainIngredientsSummary", ""),
                        allergyInformation = obj.optString("allergyInformation", ""),
                        keyIngredients = keyIngredientsList,
                        detailedIngredients = detailedIngredientsList,
                        storyDescription = obj.optString("storyDescription", ""),
                        storyImage = obj.optString("storyImage", ""),
                        idealFor = idealForList,
                        benefits = benefitsList,
                        usage = obj.optString("usage", ""),
                        usageAmount = obj.optString("usageAmount", ""),
                        scent = obj.optString("scent", ""),
                        notes = obj.optString("notes", ""),
                        rating = obj.optDouble("rating", 0.0).toFloat(),
                        sold = obj.optInt("sold", 0)
                    )
                )
            }
            productList
        } catch (e: Exception) { emptyList() }
    }

    fun getProducts(): List<CommunityProduct> {
        return getAllProducts().map { 
            CommunityProduct(it.id, it.name, it.mainImage, it.price.toInt(), it.originalPrice?.toInt(), it.rating, it.sold) 
        }
    }
    fun getUsers(): List<UserEntity> {
        return try {
            val jsonString = context.assets.open("users.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val list = mutableListOf<UserEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    UserEntity(
                        user_id = obj.optString("user_id", obj.optString("id", java.util.UUID.randomUUID().toString())),
                        username = obj.optString("username", ""),
                        full_name = obj.optString("full_name", ""),
                        email = obj.optString("email", ""),
                        phone = obj.optString("phone", ""),
                        password = obj.optString("password", ""),
                        avatar = obj.optString("avatar", null)?.takeIf { it.isNotBlank() } ?: "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png",
                        primary_image = obj.optString("primary_image", null)
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun getCommunityPosts(): List<CommunityPostEntity> {
        return try {
            val jsonString = context.assets.open("community_posts.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val usersMap = getUsers().associateBy { it.user_id }
            val assetPosts = parsePosts(jsonString, usersMap).toMutableList()
            
            // Read from local_posts.json
            val localFile = java.io.File(context.filesDir, "local_posts.json")
            if (localFile.exists()) {
                val localJsonString = localFile.readText()
                val localPosts = parsePosts(localJsonString, usersMap)
                assetPosts.addAll(0, localPosts) // Add to top
            }
            
            assetPosts
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun getCommunityNews(): List<CommunityPostEntity> {
        return try {
            val jsonString = context.assets.open("community_news.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val usersMap = getUsers().associateBy { it.user_id }
            parsePosts(jsonString, usersMap)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parsePosts(jsonString: String, usersMap: Map<String, UserEntity>): List<CommunityPostEntity> {
        val jsonArray = try {
            org.json.JSONArray(jsonString)
        } catch (e: Exception) {
            val root = org.json.JSONObject(jsonString)
            root.getJSONArray("posts")
        }
        val postList = mutableListOf<CommunityPostEntity>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val authorObj = obj.optJSONObject("author")
            val reactionsObj = obj.optJSONObject("reactions")
            
            val mediaArr = obj.optJSONArray("media")
            val mediaUrls = mutableListOf<String>()
            if (mediaArr != null) {
                for (j in 0 until mediaArr.length()) {
                    val mediaObj = mediaArr.getJSONObject(j)
                    mediaUrls.add(mediaObj.getString("url"))
                }
            }
            
            val linkedProductsArr = obj.optJSONArray("linked_products")
            val linkedProducts = mutableListOf<String>()
            if (linkedProductsArr != null) {
                for (j in 0 until linkedProductsArr.length()) {
                    linkedProducts.add(linkedProductsArr.getString(j))
                }
            }
            
            val contentStr = if (obj.has("content")) obj.getString("content") else obj.optString("message", "")
            
            val mediaUrlsStr = if (mediaUrls.isNotEmpty()) {
                mediaUrls.joinToString(",")
            } else {
                val imageObj = obj.optJSONObject("image")
                if (imageObj != null) {
                    imageObj.optString("uri", "")
                } else {
                    val imageArr = obj.optJSONArray("image")
                    if (imageArr != null) {
                        val arrUrls = mutableListOf<String>()
                        for (j in 0 until imageArr.length()) {
                            val imgItem = imageArr.getJSONObject(j)
                            val uri = imgItem.optString("uri", "")
                            if (uri.isNotEmpty()) arrUrls.add(uri)
                        }
                        arrUrls.joinToString(",")
                    } else {
                        ""
                    }
                }
            }

            val timestampInt = obj.optInt("timestamp", 0)
            val createdAtStr = if (timestampInt > 0) timestampInt.toString() else obj.optString("created_at", "")

            val reupsCount = obj.optInt("reups_count", 0)

            val authorId = authorObj?.optString("user_id", authorObj.optString("id", "")) ?: ""
            val realUser = usersMap[authorId]
            
            val authorUsername = realUser?.username ?: authorObj?.optString("username") ?: ""
            val authorDisplayName = realUser?.full_name?.takeIf { it.isNotBlank() } ?: realUser?.username ?: authorObj?.optString("display_name", authorObj.optString("username", "")) ?: ""
            
            // Rootie official posts: always use the brand logo, never a random avatar
            val authorAvatarUrl = if (authorId == "rootie_official" || authorId == "rootie_vn") {
                "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/favicon_r7kqwf.png"
            } else {
                // Priority: avatar_url in the post's author object → usersMap avatar → profile_picture_url
                val avatarFromPost = authorObj?.optString("avatar_url", "")?.takeIf { it.isNotEmpty() }
                    ?: authorObj?.optString("profile_picture_url", "")?.takeIf { it.isNotEmpty() }
                avatarFromPost ?: realUser?.avatar ?: "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
            }
            
            postList.add(CommunityPostEntity(
                postId = obj.getString("post_id"),
                authorId = authorId,
                authorUsername = authorUsername,
                authorDisplayName = authorDisplayName,
                authorAvatarUrl = authorAvatarUrl,
                content = contentStr,
                createdAt = createdAtStr,
                likesCount = reactionsObj?.optInt("like", obj.optInt("reactions_count", 0)) ?: obj.optInt("reactions_count", 0),
                commentsCount = obj.optJSONArray("comments")?.length() ?: obj.optInt("comments_count", 0),
                reupsCount = reupsCount,
                skinType = obj.optString("skin_type"),
                concern = obj.optString("concern"),
                mediaUrlsString = mediaUrlsStr,
                type = obj.optString("type"),
                linkedProductIds = linkedProducts.joinToString(",")
            ))
        }
        return postList
    }
    
    fun getReels(): List<ReelEntity> {
        return try {
            val jsonString = context.assets.open("community_reels_fb.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val usersMap = getUsers().associateBy { it.user_id }
            val jsonArray = org.json.JSONArray(jsonString)
            val reelList = mutableListOf<ReelEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val authorObj = obj.optJSONObject("author")
                val statsObj = obj.optJSONObject("stats")
                val videoObj = obj.optJSONObject("video")
                
                val authorId = authorObj?.optString("user_id") ?: ""
                val realUser = usersMap[authorId]
                val authorUsername = realUser?.username ?: authorObj?.optString("username") ?: ""
                val authorDisplayName = realUser?.full_name?.takeIf { it.isNotBlank() } ?: realUser?.username ?: authorObj?.optString("display_name") ?: ""
                val authorAvatarUrl = realUser?.avatar ?: authorObj?.optString("avatar_url", authorObj?.optString("avatar"))?.takeIf { it.isNotBlank() } ?: "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png"
                
                reelList.add(ReelEntity(
                    videoId = obj.optString("video_id", java.util.UUID.randomUUID().toString()),
                    caption = obj.optString("caption", ""),
                    authorId = authorId,
                    authorUsername = authorUsername,
                    authorDisplayName = authorDisplayName,
                    authorAvatarUrl = authorAvatarUrl,
                    likesCount = statsObj?.optInt("likes") ?: obj.optInt("likes_count", 0),
                    commentsCount = statsObj?.optInt("comments") ?: obj.optInt("comment_count", 0),
                    shareCount = statsObj?.optInt("shares") ?: obj.optInt("share_count", 0),
                    thumbnailUrl = videoObj?.optString("thumbnail") ?: obj.optString("thumbnail_url", "")
                ))
            }
            reelList
        } catch (e: Exception) { emptyList() }
    }
    
    fun getExploreVideos(): List<YtVideoEntity> {
        return try {
            val jsonString = context.assets.open("community_video_yt.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val videoList = mutableListOf<YtVideoEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeArray = obj.optJSONArray("Type")
                val types = mutableListOf<String>()
                if (typeArray != null) {
                    for (j in 0 until typeArray.length()) {
                        types.add(typeArray.getString(j))
                    }
                }
                val entity = YtVideoEntity(
                    id = obj.optString("_id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    url = obj.optString("url", ""),
                    description = obj.optString("long_description", obj.optString("short_description", obj.optString("title", ""))),
                    username = obj.optString("username", "Unknown User"),
                    avatarUrl = obj.optString("avatar", null),
                    type = types.joinToString(","),
                    likesCount = obj.optInt("likes", (100..5000).random())
                )
                
                val hashArr = obj.optJSONArray("hashtags")
                val hashList = mutableListOf<String>()
                if (hashArr != null) {
                    for (j in 0 until hashArr.length()) { hashList.add(hashArr.getString(j)) }
                }
                entity.hashtags = hashList.joinToString(" ")
                
                val kwArr = obj.optJSONArray("keywords")
                val kwList = mutableListOf<String>()
                if (kwArr != null) {
                    for (j in 0 until kwArr.length()) { kwList.add("#${kwArr.getString(j).replace(" ", "")}") }
                }
                entity.keywords = kwList.joinToString(" ")
                
                videoList.add(entity)
            }
            videoList
        } catch (e: Exception) { emptyList() }
    }
    fun getAllNotifications(): List<NotificationItem> {
        return try {
            val jsonString = context.assets.open("notification_account.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val list = mutableListOf<NotificationItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NotificationItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        time = obj.getString("time"),
                        category = obj.getString("category"),
                        tag = obj.optString("tag").takeIf { it.isNotEmpty() },
                        voucherCode = obj.optString("voucherCode").takeIf { it.isNotEmpty() },
                        actionText = obj.optString("actionText").takeIf { it.isNotEmpty() },
                        isRead = obj.optBoolean("isRead", false),
                        section = obj.getString("section"),
                        iconResName = obj.getString("iconResName"),
                        notificationType = obj.optString("notificationType").takeIf { it.isNotEmpty() },
                        orderId = obj.optString("orderId").takeIf { it.isNotEmpty() },
                        scheduleId = obj.optString("scheduleId").takeIf { it.isNotEmpty() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    fun getAllOrders(): List<OrderEntity> {
        return try {
            val jsonString = context.assets.open("orders.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonString)
            val jsonArray = root.getJSONArray("orders")
            val orderList = mutableListOf<OrderEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val orderItems = mutableListOf<com.veganbeauty.app.data.local.entities.OrderItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    orderItems.add(
                        com.veganbeauty.app.data.local.entities.OrderItem(
                            productId = itemObj.getString("productId"),
                            productName = itemObj.getString("productName"),
                            productImage = itemObj.getString("productImage"),
                            quantity = itemObj.optInt("quantity", 1),
                            price = itemObj.optLong("price", 0L)
                        )
                    )
                }
                
                val isAffiliate = obj.optBoolean("isAffiliate", false)
                val affObj = obj.optJSONObject("affiliate")
                val affiliateInfo = if (isAffiliate && affObj != null) {
                    com.veganbeauty.app.data.local.entities.AffiliateInfo(
                        isAffiliateOrder = affObj.optBoolean("isAffiliateOrder", true),
                        affiliate_id = affObj.optString("affiliate_id", ""),
                        affiliateCode = affObj.optString("affiliateCode", ""),
                        referrerUserId = affObj.optString("referrerUserId", ""),
                        referrerName = affObj.optString("referrerName", ""),
                        sourceType = affObj.optString("sourceType", "community_post"),
                        sourcePostId = affObj.optString("sourcePostId", null),
                        commissionRate = affObj.optDouble("commissionRate", 0.0),
                        commissionAmount = affObj.optLong("commissionAmount", 0L),
                        commissionStatus = affObj.optString("commissionStatus", "pending")
                    )
                } else null
                
                orderList.add(
                    OrderEntity(
                        id = obj.getString("id"),
                        userId = obj.optString("userId", null)?.takeIf { it.isNotBlank() },
                        orderDate = obj.getString("orderDate"),
                        orderTime = obj.getString("orderTime"),
                        status = obj.getString("status"),
                        totalAmount = obj.getLong("totalAmount"),
                        subTotal = obj.optLong("subTotal", obj.getLong("totalAmount")),
                        items = orderItems,
                        isGuest = obj.optBoolean("isGuest", false),
                        shippingName = obj.optString("shippingName", "Nguyễn Văn A"),
                        shippingPhone = obj.optString("shippingPhone", "090 123 4567"),
                        shippingAddress = obj.optString("shippingAddress", "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh"),
                        shippingCost = obj.optLong("shippingCost", 30000L),
                        voucherDiscount = obj.optLong("voucherDiscount", 0L),
                        paymentMethod = obj.optString("paymentMethod", "Thanh toán qua Ví MoMo"),
                        expectedDeliveryTime = obj.optString("expectedDeliveryTime", null),
                        deliveryDate = obj.optString("deliveryDate", null),
                        isAffiliate = isAffiliate,
                        affiliate = affiliateInfo,
                        hasReview = obj.optBoolean("hasReview", false),
                        reviewStars = obj.optInt("reviewStars", 0),
                        reviewText = obj.optString("reviewText", null),
                        reviewImage = obj.optString("reviewImage", null),
                        isAnonymous = obj.optBoolean("isAnonymous", false),
                        recommendToFriends = obj.optBoolean("recommendToFriends", false),
                        billingName = obj.optString("billingName", null)?.takeIf { it.isNotBlank() },
                        billingPhone = obj.optString("billingPhone", null)?.takeIf { it.isNotBlank() },
                        billingEmail = obj.optString("billingEmail", null)?.takeIf { it.isNotBlank() }
                    )
                )
            }
            orderList
        } catch (e: Exception) { 
            e.printStackTrace()
            emptyList() 
        }
    }

    fun getIngredients(): List<IngredientEntity> {
        return try {
            val jsonString = context.assets.open("ingredient.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val list = mutableListOf<IngredientEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typesArr = obj.optJSONArray("types")
                val typesList = mutableListOf<String>()
                if (typesArr != null) {
                    for (j in 0 until typesArr.length()) {
                        typesList.add(typesArr.getString(j))
                    }
                }
                list.add(IngredientEntity(
                    slug = obj.optString("slug", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    scientificName = obj.optString("scientific_name", ""),
                    image = obj.optString("image", ""),
                    origin = obj.optString("origin", ""),
                    description = obj.optString("description", ""),
                    uses = obj.optString("uses", ""),
                    types = typesList
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    fun getCommunityBlogs(limit: Int = 10, offset: Int = 0): List<CommunityBlogEntity> {
        val list = mutableListOf<CommunityBlogEntity>()
        try {
            val inputStream = context.assets.open("community_blog.json")
            val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
            reader.beginArray()
            var skipped = 0
            while (reader.hasNext() && list.size < limit) {
                // Skip các blog đã load (offset) mà không parse - chỉ skip qua để tiết kiệm RAM
                if (skipped < offset) {
                    reader.skipValue()
                    skipped++
                    continue
                }
                
                var id = ""
                var title = ""
                var shortDesc = ""
                var imageUrl = ""
                var publishedAt = ""
                
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    when (name) {
                        "_id" -> id = reader.nextString()
                        "title" -> title = reader.nextString()
                        "shortDescription" -> shortDesc = reader.nextString()
                        "publishedAt" -> publishedAt = reader.nextString()
                        "primaryImage" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while(reader.hasNext()){
                                    if(reader.nextName() == "url") {
                                        if (reader.peek() == android.util.JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            imageUrl = reader.nextString()
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (id.isEmpty()) id = java.util.UUID.randomUUID().toString()
                list.add(CommunityBlogEntity(id, title, shortDesc, imageUrl, publishedAt))
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getMessagesData(): String {
        val file = java.io.File(context.filesDir, "community_message.json")
        val prefs = context.getSharedPreferences("RootiePrefs", android.content.Context.MODE_PRIVATE)
        val isRefreshed = prefs.getBoolean("is_msg_refreshed_v2", false)
        if (!file.exists() || !isRefreshed) {
            val assetStream = context.assets.open("community_message.json")
            file.writeBytes(assetStream.readBytes())
            assetStream.close()
            prefs.edit().putBoolean("is_msg_refreshed_v2", true).apply()
        }
        return file.readText()
    }

    fun saveMessagesData(jsonString: String) {
        val file = java.io.File(context.filesDir, "community_message.json")
        file.writeText(jsonString)
    }

    fun getRawPostsJson(): String {
        return try {
            val assetStr = context.assets.open("community_posts.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
            val assetRoot = try {
                org.json.JSONObject(assetStr)
            } catch (e: Exception) {
                val array = org.json.JSONArray(assetStr)
                val obj = org.json.JSONObject()
                obj.put("posts", array)
                obj
            }
            val assetPostsArray = assetRoot.optJSONArray("posts") ?: org.json.JSONArray()
            
            val localFile = java.io.File(context.filesDir, "local_posts.json")
            if (localFile.exists()) {
                val localArray = org.json.JSONArray(localFile.readText())
                val combinedArray = org.json.JSONArray()
                // local first
                for (i in 0 until localArray.length()) {
                    combinedArray.put(localArray.getJSONObject(i))
                }
                for (i in 0 until assetPostsArray.length()) {
                    combinedArray.put(assetPostsArray.getJSONObject(i))
                }
                assetRoot.put("posts", combinedArray)
            }
            assetRoot.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }
    
    fun saveLocalPost(post: CommunityPostEntity) {
        try {
            val file = java.io.File(context.filesDir, "local_posts.json")
            val postsArray = if (file.exists()) {
                org.json.JSONArray(file.readText())
            } else {
                org.json.JSONArray()
            }
            
            val postObj = org.json.JSONObject()
            postObj.put("post_id", post.postId)
            postObj.put("content", post.content)
            postObj.put("created_at", post.createdAt)
            postObj.put("reactions_count", post.likesCount)
            postObj.put("comments_count", post.commentsCount)
            postObj.put("reups_count", post.reupsCount)
            postObj.put("skin_type", post.skinType ?: "")
            postObj.put("concern", post.concern ?: "")
            postObj.put("type", post.type ?: "")
            
            val authorObj = org.json.JSONObject()
            authorObj.put("user_id", post.authorId)
            authorObj.put("username", post.authorUsername)
            authorObj.put("display_name", post.authorDisplayName)
            authorObj.put("avatar_url", post.authorAvatarUrl ?: "")
            postObj.put("author", authorObj)
            
            if (post.mediaUrlsString.isNotEmpty()) {
                val mediaArray = org.json.JSONArray()
                post.mediaUrlsString.split(",").forEach { url ->
                    val mediaObj = org.json.JSONObject()
                    mediaObj.put("url", url)
                    mediaArray.put(mediaObj)
                }
                postObj.put("media", mediaArray)
            }
            
            if (!post.linkedProductIds.isNullOrEmpty()) {
                val linkedArray = org.json.JSONArray()
                post.linkedProductIds.split(",").forEach { id ->
                    linkedArray.put(id)
                }
                postObj.put("linked_products", linkedArray)
            }
            
            // Add to top
            val newArray = org.json.JSONArray()
            newArray.put(postObj)
            for (i in 0 until postsArray.length()) {
                newArray.put(postsArray.getJSONObject(i))
            }
            
            file.writeText(newArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getRawNewsJson(): String = try { context.assets.open("community_news.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "[]" }
    fun getRawUsersJson(): String = try { context.assets.open("users.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "[]" }
    fun getRawReelsJson(): String = try { context.assets.open("community_reels_fb.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "[]" }
    fun getRawIngredientsJson(): String = try { context.assets.open("ingredient.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "[]" }
    fun getRawProductsJson(): String = try { context.assets.open("products.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "{}" }
    fun getRawSkinHistoryJson(): String = try { context.assets.open("skin_history.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "[]" }
    fun getRawSkinBookingsJson(): String = try { context.assets.open("skin_bookings.json").bufferedReader().use { it.readText() }.removePrefix("\uFEFF") } catch (e: Exception) { "{}" }

    fun getStores(): List<StoreEntity> = getAllStores()

    fun getAllStores(): List<StoreEntity> {
        val list = mutableListOf<StoreEntity>()
        try {
            val jsonString = context.assets.open("rootie_stores.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val idObj = obj.optJSONObject("_id")
                val id = idObj?.optString("\$oid", java.util.UUID.randomUUID().toString()) ?: java.util.UUID.randomUUID().toString()
                
                val storeCode = obj.optString("ma_cua_hang", "")
                val storeName = obj.optString("ten_cua_hang", "")
                val type = obj.optString("loai_hinh", "Cửa hàng mỹ phẩm")
                
                val addressObj = obj.optJSONObject("dia_chi")
                val address = addressObj?.optString("dia_chi_day_du", "") ?: ""
                val province = addressObj?.optString("tinh_thanh", "") ?: ""
                val district = addressObj?.optString("quan_huyen", "") ?: ""
                val ward = addressObj?.optString("phuong_xa", "") ?: ""
                val street = addressObj?.optString("duong", "") ?: ""
                val number = addressObj?.optString("so_nha", "") ?: ""
                
                val toaDoObj = obj.optJSONObject("toa_do")
                val lat = toaDoObj?.optDouble("lat", 0.0) ?: 0.0
                val lng = toaDoObj?.optDouble("lng", 0.0) ?: 0.0
                
                val contactObj = obj.optJSONObject("thong_tin_lien_he")
                val email = contactObj?.optString("email", "") ?: ""
                val phoneArray = contactObj?.optJSONArray("so_dien_thoai")
                val phone = if (phoneArray != null && phoneArray.length() > 0) {
                    val tempPhones = mutableListOf<String>()
                    for (j in 0 until phoneArray.length()) {
                        tempPhones.add(phoneArray.getString(j))
                    }
                    tempPhones.joinToString(",")
                } else {
                    contactObj?.optString("so_dien_thoai", "") ?: ""
                }
                
                val timeObj = obj.optJSONObject("thoi_gian_hoat_dong")
                val thu26Obj = timeObj?.optJSONObject("thu_2_6")
                val openHours = if (thu26Obj != null) {
                    "${thu26Obj.optString("mo_cua", "08:00")} - ${thu26Obj.optString("dong_cua", "22:00")}"
                } else {
                    "08:00 - 22:00"
                }
                
                val imagesArray = obj.optJSONArray("hinh_anh")
                val imageUrl = if (imagesArray != null && imagesArray.length() > 0) {
                    imagesArray.getString(0)
                } else {
                    ""
                }
                
                val tienNghiArray = obj.optJSONArray("tien_nghi")
                val tienNghiStr = if (tienNghiArray != null) {
                    val tempTienNghi = mutableListOf<String>()
                    for (j in 0 until tienNghiArray.length()) {
                        tempTienNghi.add(tienNghiArray.getString(j))
                    }
                    tempTienNghi.joinToString(",")
                } else {
                    ""
                }
                
                // Mock distance based on ID or random to make it look realistic (1.0 to 15.0 km)
                val randomSeed = id.hashCode()
                val mockDistance = 1.0 + (Math.abs(randomSeed) % 140) / 10.0
                
                list.add(
                    StoreEntity(
                        id = id,
                        maCuaHang = storeCode,
                        tenCuaHang = storeName,
                        loaiHinh = type,
                        soNha = number,
                        duong = street,
                        phuongXa = ward,
                        quanHuyen = district,
                        tinhThanh = province,
                        diaChiDayDu = address,
                        lat = lat,
                        lng = lng,
                        soDienThoai = phone,
                        email = email,
                        moCua = openHours.split(" - ").firstOrNull() ?: "08:00",
                        dongCua = openHours.split(" - ").lastOrNull() ?: "22:00",
                        trangThai = obj.optString("trang_thai", "Đang hoạt động"),
                        isActive = obj.optBoolean("isActive", true),
                        tienNghi = tienNghiStr,
                        imageUrl = imageUrl,
                        distance = mockDistance
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    companion object {
        private var cachedBookings: MutableList<BookingHistoryEntity>? = null
        private var cachedSkinHistory: org.json.JSONArray? = null
    }

    fun getSkinHistory(): org.json.JSONArray {
        if (cachedSkinHistory == null) {
            try {
                val file = java.io.File(context.filesDir, "skin_history.json")
                val jsonString = if (file.exists()) {
                    file.readText()
                } else {
                    context.assets.open("skin_history.json").bufferedReader().use { it.readText() }
                }
                cachedSkinHistory = org.json.JSONArray(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                cachedSkinHistory = org.json.JSONArray()
            }
        }
        return cachedSkinHistory!!
    }

    fun addSkinHistory(item: org.json.JSONObject) {
        if (cachedSkinHistory == null) {
            getSkinHistory()
        }
        // Thêm vào đầu danh sách
        val newArray = org.json.JSONArray()
        newArray.put(item)
        for (i in 0 until cachedSkinHistory!!.length()) {
            newArray.put(cachedSkinHistory!!.getJSONObject(i))
        }
        cachedSkinHistory = newArray

        // Lưu vào bộ nhớ trong để giữ data
        try {
            val file = java.io.File(context.filesDir, "skin_history.json")
            file.writeText(newArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getUserBookingHistory(email: String): List<BookingHistoryEntity> {
        if (cachedBookings == null) {
            val list = mutableListOf<BookingHistoryEntity>()
            try {
                val file = java.io.File(context.filesDir, "local_bookings.json")
                val jsonString = if (file.exists()) {
                    file.readText()
                } else {
                    context.assets.open("skin_bookings.json").bufferedReader().use { it.readText() }
                }
                val root = org.json.JSONObject(jsonString)
                val jsonArray = root.getJSONArray("bookings")
                for (i in 0 until jsonArray.length()) {
                    val histObj = jsonArray.getJSONObject(i)
                    val dateDisplay = histObj.optString("dateDisplay", "")
                    val time = histObj.optString("time", "")
                    var status = histObj.optString("status", "")
                    var cancelReason = histObj.optString("cancelReason", "")

                    if (status == "Sắp diễn ra") {
                        try {
                            val regex = Regex("(\\d+)\\s+tháng\\s+(\\d+),\\s+(\\d+)")
                            val match = regex.find(dateDisplay)
                            if (match != null) {
                                val day = match.groupValues[1].toInt()
                                val month = match.groupValues[2].toInt()
                                val year = match.groupValues[3].toInt()

                                val timeParts = time.split(" - ").firstOrNull()?.trim()?.split(":")
                                val hour = timeParts?.getOrNull(0)?.toIntOrNull() ?: 0
                                val minute = timeParts?.getOrNull(1)?.toIntOrNull() ?: 0

                                val bookingCal = java.util.Calendar.getInstance()
                                bookingCal.set(year, month - 1, day, hour, minute, 0)

                                if (bookingCal.time.before(java.util.Date())) {
                                    status = "Đã huỷ"
                                    if (cancelReason.isEmpty()) {
                                        cancelReason = "Hệ thống tự động huỷ do đã quá hạn lịch hẹn."
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    list.add(
                        BookingHistoryEntity(
                            id = histObj.optString("id", ""),
                            userId = histObj.optString("userId", ""),
                            userName = histObj.optString("userName", ""),
                            userPhone = histObj.optString("userPhone", ""),
                            userEmail = histObj.optString("userEmail", ""),
                            serviceName = histObj.optString("serviceName", ""),
                            dateDisplay = dateDisplay,
                            monthDisplay = histObj.optString("monthDisplay", ""),
                            dayOfWeek = histObj.optString("dayOfWeek", ""),
                            time = time,
                            duration = histObj.optString("duration", ""),
                            storeName = histObj.optString("storeName", ""),
                            storeAddress = histObj.optString("storeAddress", ""),
                            storePhone = histObj.optString("storePhone", ""),
                            storeImage = histObj.optString("storeImage", ""),
                            note = histObj.optString("note", ""),
                            status = status,
                            policy = histObj.optString("policy", ""),
                            createdAt = histObj.optString("createdAt", ""),
                            completedAt = histObj.optString("completedAt", ""),
                            skinResults = histObj.optJSONArray("skinResults")?.let { resultsArray ->
                                val results = mutableListOf<String>()
                                for (j in 0 until resultsArray.length()) {
                                    results.add(resultsArray.getString(j))
                                }
                                results
                            } ?: emptyList(),
                            consultantName = histObj.optString("consultantName", ""),
                            consultantAvatar = histObj.optString("consultantAvatar", ""),
                            consultantRating = histObj.optDouble("consultantRating", 0.0).toFloat(),
                            userRating = histObj.optDouble("userRating", 0.0).toFloat(),
                            userReview = histObj.optString("userReview", ""),
                            reviewDate = histObj.optString("reviewDate", ""),
                            beforeImage = histObj.optString("beforeImage", ""),
                            afterImage = histObj.optString("afterImage", ""),
                            earnedPoints = histObj.optInt("earnedPoints", 0),
                            totalPoints = histObj.optInt("totalPoints", 0),
                            nextAppointmentDate = histObj.optString("nextAppointmentDate", ""),
                            nextAppointmentText = histObj.optString("nextAppointmentText", ""),
                            cancelledAt = histObj.optString("cancelledAt", ""),
                            cancelReason = cancelReason
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedBookings = list
        }
        return cachedBookings!!.filter { it.userEmail == email }
    }

    private fun saveBookingsToLocalFile(bookings: List<BookingHistoryEntity>) {
        try {
            val root = org.json.JSONObject()
            val array = org.json.JSONArray()
            for (booking in bookings) {
                val obj = org.json.JSONObject()
                obj.put("id", booking.id)
                obj.put("userId", booking.userId)
                obj.put("userName", booking.userName)
                obj.put("userPhone", booking.userPhone)
                obj.put("userEmail", booking.userEmail)
                obj.put("serviceName", booking.serviceName)
                obj.put("dateDisplay", booking.dateDisplay)
                obj.put("monthDisplay", booking.monthDisplay)
                obj.put("dayOfWeek", booking.dayOfWeek)
                obj.put("time", booking.time)
                obj.put("duration", booking.duration)
                obj.put("storeName", booking.storeName)
                obj.put("storeAddress", booking.storeAddress)
                obj.put("storePhone", booking.storePhone)
                obj.put("storeImage", booking.storeImage)
                obj.put("note", booking.note)
                obj.put("status", booking.status)
                obj.put("policy", booking.policy)
                obj.put("createdAt", booking.createdAt)
                obj.put("completedAt", booking.completedAt)
                
                val skinResultsArray = org.json.JSONArray()
                for (res in booking.skinResults) {
                    skinResultsArray.put(res)
                }
                obj.put("skinResults", skinResultsArray)
                
                obj.put("consultantName", booking.consultantName)
                obj.put("consultantAvatar", booking.consultantAvatar)
                obj.put("consultantRating", booking.consultantRating.toDouble())
                obj.put("userRating", booking.userRating.toDouble())
                obj.put("userReview", booking.userReview)
                obj.put("reviewDate", booking.reviewDate)
                obj.put("beforeImage", booking.beforeImage)
                obj.put("afterImage", booking.afterImage)
                obj.put("earnedPoints", booking.earnedPoints)
                obj.put("totalPoints", booking.totalPoints)
                obj.put("nextAppointmentDate", booking.nextAppointmentDate)
                obj.put("nextAppointmentText", booking.nextAppointmentText)
                obj.put("cancelledAt", booking.cancelledAt)
                obj.put("cancelReason", booking.cancelReason)
                array.put(obj)
            }
            root.put("bookings", array)
            val file = java.io.File(context.filesDir, "local_bookings.json")
            file.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateBookingStatus(bookingId: String, newStatus: String, cancelReason: String = "") {
        cachedBookings = cachedBookings?.map {
            if (it.id == bookingId) it.copy(status = newStatus, cancelReason = cancelReason) else it
        }?.toMutableList()
        cachedBookings?.let { saveBookingsToLocalFile(it) }
    }

    fun addBooking(booking: BookingHistoryEntity) {
        if (cachedBookings == null) {
            // Force initialize if it's null
            getUserBookingHistory(booking.userEmail)
        }
        // Thêm lịch mới vào đầu danh sách
        cachedBookings?.add(0, booking)
        cachedBookings?.let { saveBookingsToLocalFile(it) }
    }
}
