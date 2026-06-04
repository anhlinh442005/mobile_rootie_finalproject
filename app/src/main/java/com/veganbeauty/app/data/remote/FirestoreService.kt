package com.veganbeauty.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.app.data.local.entities.ProductEntity
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
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    description = doc.getString("description") ?: "",
                    mainImage = doc.getString("mainImage") ?: "",
                    suitableFor = doc.getString("suitableFor") ?: "",
                    origin = doc.getString("origin") ?: "",
                    expiryDate = doc.getString("expiryDate") ?: "",
                    
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
        } catch (e: Exception) {
            emptyList()
        }
    }
}
