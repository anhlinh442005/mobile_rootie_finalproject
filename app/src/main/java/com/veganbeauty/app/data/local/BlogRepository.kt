package com.veganbeauty.app.data.local

import android.content.Context
import android.util.JsonReader
import com.veganbeauty.app.features.community.blog.BlogPost
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class BlogRepository(private val context: Context) {
    fun getBlogPosts(limit: Int = 10, targetCategory: String? = null, offset: Int = 0): List<BlogPost> {
        val result = mutableListOf<BlogPost>()
        try {
            val stream = context.assets.open("community_blog.json")
            val reader = JsonReader(InputStreamReader(stream, "UTF-8"))
            reader.beginArray()
            var count = 0
            while (reader.hasNext() && result.size < limit) {
                var title = ""
                var description = ""
                var date = ""
                var imageUrl = ""
                var category = ""
                var doctorName = ""
                var doctorAvatar = ""
                var doctorBio = ""
                var content = ""
                
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "title" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) reader.nextNull() else title = reader.nextString()
                        }
                        "shortDescription" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) reader.nextNull() else description = reader.nextString()
                        }
                        "publishedAt" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                val rawDate = reader.nextString()
                                try {
                                    val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                    val d = sdfIn.parse(rawDate)
                                    val sdfOut = SimpleDateFormat("d/M/yyyy", Locale("vi"))
                                    if (d != null) {
                                        date = sdfOut.format(d)
                                    }
                                } catch (e: Exception) {
                                    date = rawDate
                                }
                            }
                        }
                        "primaryImage" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    if (reader.nextName() == "url") {
                                        imageUrl = reader.nextString()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                        }
                        "category" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    if (reader.nextName() == "name") {
                                        category = reader.nextString()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                        }
                        "approver" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "fullName" -> {
                                            if (reader.peek() != android.util.JsonToken.NULL) doctorName = reader.nextString() else reader.nextNull()
                                        }
                                        "bio" -> {
                                            if (reader.peek() != android.util.JsonToken.NULL) doctorBio = reader.nextString() else reader.nextNull()
                                        }
                                        "avatar" -> {
                                            if (reader.peek() == android.util.JsonToken.NULL) {
                                                reader.nextNull()
                                            } else {
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    if (reader.nextName() == "url") {
                                                        doctorAvatar = reader.nextString()
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
                            }
                        }
                        "author" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "fullName" -> {
                                            if (reader.peek() != android.util.JsonToken.NULL) {
                                                val name = reader.nextString()
                                                if (doctorName.isEmpty()) doctorName = name
                                            } else reader.nextNull()
                                        }
                                        "bio" -> {
                                            if (reader.peek() != android.util.JsonToken.NULL) {
                                                val bio = reader.nextString()
                                                if (doctorBio.isEmpty()) doctorBio = bio
                                            } else reader.nextNull()
                                        }
                                        "avatar" -> {
                                            if (reader.peek() == android.util.JsonToken.NULL) {
                                                reader.nextNull()
                                            } else {
                                                reader.beginObject()
                                                while (reader.hasNext()) {
                                                    if (reader.nextName() == "url") {
                                                        val url = reader.nextString()
                                                        if (doctorAvatar.isEmpty()) doctorAvatar = url
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
                            }
                        }
                        "descriptionHtml" -> {
                            if (reader.peek() != android.util.JsonToken.NULL) {
                                content = reader.nextString()
                            } else {
                                reader.nextNull()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                
                if (targetCategory == null || category == targetCategory) {
                    if (count >= offset) {
                        result.add(BlogPost(title, description, date, imageUrl, category, doctorName, doctorAvatar, doctorBio, content))
                    }
                    count++
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
    
    fun getCategoryCounts(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        try {
            val stream = context.assets.open("community_blog.json")
            val reader = JsonReader(InputStreamReader(stream, "UTF-8"))
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "category") {
                        if (reader.peek() == android.util.JsonToken.NULL) {
                            reader.nextNull()
                        } else {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                if (reader.nextName() == "name") {
                                    val name = reader.nextString()
                                    counts[name] = counts.getOrDefault(name, 0) + 1
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return counts
    }
}
