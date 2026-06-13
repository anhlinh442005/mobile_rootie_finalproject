package com.veganbeauty.app.features.community

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object UserMemoryHelper {
    private const val FILE_NAME = "user_memory.json"

    private fun getMemoryFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    private fun readMemory(context: Context): JSONArray {
        val file = getMemoryFile(context)
        return if (file.exists()) {
            try {
                JSONArray(file.readText())
            } catch (e: Exception) {
                JSONArray()
            }
        } else {
            JSONArray()
        }
    }

    private fun writeMemory(context: Context, jsonArray: JSONArray) {
        getMemoryFile(context).writeText(jsonArray.toString())
    }

    private fun getUserMemory(context: Context, userId: String): JSONObject {
        val memoryArray = readMemory(context)
        for (i in 0 until memoryArray.length()) {
            val obj = memoryArray.getJSONObject(i)
            if (obj.optString("userId") == userId) {
                return obj
            }
        }
        val newObj = JSONObject().apply {
            put("userId", userId)
            put("repostedPostIds", JSONArray())
            put("savedPostIds", JSONArray())
        }
        return newObj
    }

    private fun saveUserMemory(context: Context, userMemory: JSONObject) {
        val memoryArray = readMemory(context)
        var found = false
        for (i in 0 until memoryArray.length()) {
            val obj = memoryArray.getJSONObject(i)
            if (obj.optString("userId") == userMemory.optString("userId")) {
                memoryArray.put(i, userMemory)
                found = true
                break
            }
        }
        if (!found) {
            memoryArray.put(userMemory)
        }
        writeMemory(context, memoryArray)
    }

    fun isPostReposted(context: Context, userId: String, postId: String): Boolean {
        val memory = getUserMemory(context, userId)
        val arr = memory.optJSONArray("repostedPostIds") ?: return false
        for (i in 0 until arr.length()) {
            if (arr.getString(i) == postId) return true
        }
        return false
    }

    fun toggleRepost(context: Context, userId: String, postId: String): Boolean {
        val memory = getUserMemory(context, userId)
        var arr = memory.optJSONArray("repostedPostIds")
        if (arr == null) arr = JSONArray()
        
        var foundIdx = -1
        for (i in 0 until arr.length()) {
            if (arr.getString(i) == postId) {
                foundIdx = i
                break
            }
        }
        
        val isNowReposted = if (foundIdx >= 0) {
            arr.remove(foundIdx)
            false
        } else {
            arr.put(postId)
            true
        }
        
        memory.put("repostedPostIds", arr)
        saveUserMemory(context, memory)
        return isNowReposted
    }

    fun isPostSaved(context: Context, userId: String, postId: String): Boolean {
        val memory = getUserMemory(context, userId)
        val arr = memory.optJSONArray("savedPostIds") ?: return false
        for (i in 0 until arr.length()) {
            if (arr.getString(i) == postId) return true
        }
        return false
    }

    fun toggleSave(context: Context, userId: String, postId: String): Boolean {
        val memory = getUserMemory(context, userId)
        var arr = memory.optJSONArray("savedPostIds")
        if (arr == null) arr = JSONArray()
        
        var foundIdx = -1
        for (i in 0 until arr.length()) {
            if (arr.getString(i) == postId) {
                foundIdx = i
                break
            }
        }
        
        val isNowSaved = if (foundIdx >= 0) {
            arr.remove(foundIdx)
            false
        } else {
            arr.put(postId)
            true
        }
        
        memory.put("savedPostIds", arr)
        saveUserMemory(context, memory)
        return isNowSaved
    }
}
