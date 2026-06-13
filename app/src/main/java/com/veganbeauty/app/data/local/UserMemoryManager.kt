package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HandbookCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    val videos: MutableList<YtVideoEntity> = mutableListOf()
)

class UserMemoryManager(private val context: Context) {
    private val fileName = "user_memory.json"

    fun getCategories(): MutableList<HandbookCategory> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return mutableListOf()
        val list = mutableListOf<HandbookCategory>()
        try {
            val jsonString = file.readText()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cat = HandbookCategory(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", "")
                )
                val videosArr = obj.optJSONArray("videos")
                if (videosArr != null) {
                    for (j in 0 until videosArr.length()) {
                        val vObj = videosArr.getJSONObject(j)
                        cat.videos.add(
                            YtVideoEntity(
                                id = vObj.optString("id", ""),
                                title = vObj.optString("title", ""),
                                url = vObj.optString("url", ""),
                                description = vObj.optString("description", ""),
                                username = vObj.optString("username", ""),
                                avatarUrl = if (vObj.has("avatarUrl") && !vObj.isNull("avatarUrl")) vObj.optString("avatarUrl") else null,
                                type = vObj.optString("type", "")
                            )
                        )
                    }
                }
                list.add(cat)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCategories(categories: List<HandbookCategory>) {
        val file = File(context.filesDir, fileName)
        try {
            val array = JSONArray()
            for (cat in categories) {
                val obj = JSONObject()
                obj.put("id", cat.id)
                obj.put("name", cat.name)
                val videosArr = JSONArray()
                for (v in cat.videos) {
                    val vObj = JSONObject()
                    vObj.put("id", v.id)
                    vObj.put("title", v.title)
                    vObj.put("url", v.url)
                    vObj.put("description", v.description)
                    vObj.put("username", v.username)
                    vObj.put("avatarUrl", v.avatarUrl)
                    vObj.put("type", v.type)
                    videosArr.put(vObj)
                }
                obj.put("videos", videosArr)
                array.put(obj)
            }
            val jsonString = array.toString()
            file.writeText(jsonString)
            
            // Sync to Firebase background
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val data = hashMapOf("handbookData" to jsonString)
                    db.collection("user_handbooks").document("test_001").set(data).await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addVideoToCategory(categoryName: String, video: YtVideoEntity) {
        val categories = getCategories()
        var category = categories.find { it.name == categoryName }
        if (category == null) {
            category = HandbookCategory(name = categoryName)
            categories.add(category)
        }
        if (category.videos.none { it.id == video.id || it.url == video.url }) {
            category.videos.add(video)
            saveCategories(categories)
        }
    }

    fun removeVideoFromCategory(categoryName: String, video: YtVideoEntity) {
        val categories = getCategories()
        val category = categories.find { it.name == categoryName }
        if (category != null) {
            category.videos.removeAll { it.id == video.id || it.url == video.url }
            saveCategories(categories)
        }
    }
}
