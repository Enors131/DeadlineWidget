package com.example.deadlinewidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TaskStore {
    private const val PREFS = "tasks_prefs"
    private const val KEY = "tasks"

    fun load(ctx: Context): List<Task> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Task(o.getLong("id"), o.getString("name"), o.getLong("due"))
            }.sortedBy { t -> t.dueEpochDay }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(ctx: Context, list: List<Task>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("due", it.dueEpochDay)
            )
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(ctx: Context, task: Task) {
        save(ctx, load(ctx).filter { it.id != task.id } + task)
    }

    fun remove(ctx: Context, id: Long) {
        save(ctx, load(ctx).filter { it.id != id })
    }
}
