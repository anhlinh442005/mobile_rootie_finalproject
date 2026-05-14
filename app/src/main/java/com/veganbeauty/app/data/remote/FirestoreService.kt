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
                ProductEntity(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    sku = doc.getString("sku") ?: "",
                    price = doc.getLong("price") ?: 0L,
                    category = doc.getString("category") ?: "",
                    stock = doc.getLong("stock")?.toInt() ?: 0,
                    description = doc.getString("description") ?: "",
                    mainImage = doc.getString("mainImage") ?: "",
                    suitableFor = doc.getString("suitableFor") ?: "",
                    origin = doc.getString("origin") ?: "",
                    expiryDate = doc.getString("expiryDate") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
