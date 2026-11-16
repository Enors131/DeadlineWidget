package com.example.deadlinewidget

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : Activity() {

    companion object {
        const val EXTRA_ADD = "extra_add"
    }

    private val adapter = TaskAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = findViewById<ListView>(R.id.task_list)
        list.emptyView = findViewById(R.id.empty)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ -> showTaskDialog(adapter.getItem(pos)) }
        findViewById<View>(R.id.fab).setOnClickListener { showTaskDialog(null) }

        reload()

        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_ADD, false)) {
            showTaskDialog(null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ADD, false)) showTaskDialog(null)
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        adapter.items = TaskStore.load(this)
        adapter.notifyDataSetChanged()
        DeadlineWidgetProvider.refresh(this)
    }

    private fun formatDate(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    private fun showTaskDialog(task: Task?) {
        val view = layoutInflater.inflate(R.layout.dialog_task, null)
        val nameInput = view.findViewById<EditText>(R.id.input_name)
        val dateBtn = view.findViewById<TextView>(R.id.btn_date)

        var due = task?.dueEpochDay ?: LocalDate.now().plusDays(7).toEpochDay()
        nameInput.setText(task?.name ?: "")
        dateBtn.text = formatDate(due)

        dateBtn.setOnClickListener {
            val d = LocalDate.ofEpochDay(due)
            DatePickerDialog(
                this, R.style.AppDialog,
                { _, y, m, day ->
                    due = LocalDate.of(y, m + 1, day).toEpochDay()
                    dateBtn.text = formatDate(due)
                },
                d.year, d.monthValue - 1, d.dayOfMonth
            ).show()
        }

        val builder = AlertDialog.Builder(this, R.style.AppDialog)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
        if (task != null) {
            builder.setNeutralButton(R.string.delete) { _, _ ->
                TaskStore.remove(this, task.id)
                reload()
            }
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            nameInput.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    nameInput.error = getString(R.string.name_required)
                    return@setOnClickListener
                }
                val id = task?.id ?: System.currentTimeMillis()
                TaskStore.upsert(this, Task(id, name, due))
                reload()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private inner class TaskAdapter : BaseAdapter() {
        var items: List<Task> = emptyList()

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_task, parent, false)
            val t = items[position]
            val d = t.daysLeft()
            v.findViewById<TextView>(R.id.item_name).text = t.name
            v.findViewById<TextView>(R.id.item_date).text = formatDate(t.dueEpochDay)
            val days = v.findViewById<TextView>(R.id.item_days)
            days.text = d.toString()
            days.setTextColor(this@MainActivity.getColor(daysColorRes(d)))
            return v
        }
    }
}
