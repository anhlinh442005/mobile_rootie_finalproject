package com.veganbeauty.app.features.shop.search

import android.content.Context

object ShopSearchHistoryHelper {
    private const val PREFS_NAME = "ShopSearchPrefs"
    private const val KEY_HISTORY = "search_history"
    private const val MAX_ITEMS = 10

    fun getHistory(context: Context): List<String> {
        val historyString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        if (historyString.isEmpty()) return emptyList()
        return historyString.split("|||").filter { it.isNotEmpty() }
    }

    fun add(context: Context, query: String) {
        if (query.isBlank()) return
        val current = getHistory(context).toMutableList()
        current.remove(query)
        current.add(0, query)
        if (current.size > MAX_ITEMS) {
            current.removeAt(current.lastIndex)
        }
        save(context, current)
    }

    fun remove(context: Context, query: String) {
        save(context, getHistory(context).filter { it != query })
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, history: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, history.joinToString("|||"))
            .apply()
    }
}
