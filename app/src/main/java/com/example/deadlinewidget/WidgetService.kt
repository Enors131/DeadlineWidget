package com.example.deadlinewidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val ctx: Context) : RemoteViewsFactory {
        private var tasks: List<Task> = emptyList()

        override fun onCreate() {}
        override fun onDataSetChanged() {
            tasks = TaskStore.load(ctx)
        }
        override fun onDestroy() {}

        override fun getCount() = tasks.size

        override fun getViewAt(position: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_item)
            if (position >= tasks.size) return rv
            val t = tasks[position]
            val d = t.daysLeft()
            rv.setTextViewText(R.id.item_name, t.name)
            rv.setTextViewText(R.id.item_days, d.toString())
            rv.setTextColor(R.id.item_days, ctx.getColor(daysColorRes(d)))
            rv.setOnClickFillInIntent(R.id.item_root, Intent())
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = tasks.getOrNull(position)?.id ?: position.toLong()
        override fun hasStableIds() = true
    }
}
