package com.example.dgoal

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.graphics.Color
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity
import com.example.dgoal.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var tasks = mutableListOf<Task>()



    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupButtons()

        lifecycleScope.launch {
            resetCompletedIfNewDay()
        }

        lifecycleScope.launch {
            db.taskDao().getAllTasks().collect { taskList ->

                tasks = taskList.toMutableList()

                updateTaskUI()
            }
        }
    }

    private suspend fun resetCompletedIfNewDay() {

        val prefs = getSharedPreferences("dgoal_prefs", MODE_PRIVATE)

        val today = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val lastOpenedDate = prefs.getString("last_opened_date", null)

        if (lastOpenedDate != today) {

            val allTasks = db.taskDao().getAllTasksOnce()

            allTasks.forEach { task ->
                if (task.completed) {
                    task.completed = false
                    db.taskDao().updateTask(task)
                }
            }

            prefs.edit().putString("last_opened_date", today).apply()
        }
    }

    private fun setupButtons() {

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }



        binding.homeButton.setOnClickListener {
            showMainScreen()
        }

        binding.tasksButton.setOnClickListener {
            showMainScreen()
        }

        binding.goalsButton.setOnClickListener {
            showGoalsScreen()
        }
    }

    private fun showMainScreen() {
        binding.mainScrollView.visibility = View.VISIBLE
        binding.goalsScrollView.visibility = View.GONE
    }

    private fun showGoalsScreen() {
        binding.mainScrollView.visibility = View.GONE
        binding.goalsScrollView.visibility = View.VISIBLE
        updateGoalsUI()
    }

    private fun showAddTaskDialog() {

        val input = TextInputEditText(this)
        input.hint = "Enter Task Here"
        input.setTextColor(Color.parseColor("#FFFFFF"))
        input.setHintTextColor(Color.parseColor("#7FA3C7"))
        input.setPadding(50, 30, 50, 20)

        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("What is to be done?")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->

                val taskTitle = input.text.toString().trim()

                if (taskTitle.isNotEmpty()) {

                    lifecycleScope.launch {
                        db.taskDao().insertTask(Task(title = taskTitle))
                    }
                }
            }
            .show()
    }




    private fun updateTaskUI() {

        binding.taskContainer.removeAllViews()

        if (tasks.isEmpty()) {

            binding.taskContainer.addView(
                TextView(this).apply {
                    text = "No tasks yet\nTap + Add Task to create your first task"
                    textSize = 15f
                    setTextColor(Color.parseColor("#8FA8C4"))
                    gravity = android.view.Gravity.CENTER
                }
            )

        } else {

            tasks.forEach { task ->

                val taskRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(10, 15, 10, 15)
                }

                val checkBox = CheckBox(this).apply {
                    text = task.title
                    textSize = 16f
                    setTextColor(Color.parseColor("#FFFFFF"))
                    isChecked = task.completed

                    setOnCheckedChangeListener { _, isChecked ->

                        task.completed = isChecked

                        val today = java.util.Calendar.getInstance()
                            .get(java.util.Calendar.DAY_OF_MONTH)

                        if (isChecked) {
                            task.completedDays.add(today)
                        } else {
                            task.completedDays.remove(today)
                        }

                        lifecycleScope.launch {
                            db.taskDao().updateTask(task)
                        }
                    }
                }

                val deleteButton = ImageButton(this).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    contentDescription = "Delete Task"
                    setColorFilter(Color.parseColor("#E57373"))
                    background = null

                    setOnClickListener {
                        lifecycleScope.launch {
                            db.taskDao().deleteTask(task)
                        }
                    }
                }

                taskRow.addView(
                    checkBox,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )

                taskRow.addView(
                    deleteButton,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                binding.taskContainer.addView(taskRow)
            }
        }

        updateProgress()
    }

    private fun updateGoalsUI() {

        binding.goalsContainer.removeAllViews()

        if (tasks.isEmpty()) {

            binding.goalsContainer.addView(
                TextView(this).apply {
                    text = "No tasks yet\nAdd tasks to start tracking monthly goals"
                    textSize = 15f
                    setTextColor(Color.parseColor("#8FA8C4"))
                    gravity = android.view.Gravity.CENTER
                }
            )

            return
        }

        val daysInMonth = java.util.Calendar.getInstance()
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        tasks.forEach { task ->

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 20, 0, 20)
            }

            val percentage =
                (task.completedDays.size * 100 / daysInMonth).coerceAtMost(100)

            // Container that stacks the ring and the letter circle on top of each other
            val ringSize = 130
            val ringStack = android.widget.FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ringSize, ringSize)
            }

            val pieChart = PieChartView(this).apply {
                this.percentage = percentage
            }

            ringStack.addView(
                pieChart,
                android.widget.FrameLayout.LayoutParams(ringSize, ringSize)
            )

            val letterCircle = TextView(this).apply {
                text = task.title.take(1).uppercase()
                textSize = 18f
                setTextColor(Color.parseColor("#FFFFFF"))
                gravity = android.view.Gravity.CENTER
                background = androidx.core.content.ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.bg_circle_ring_light
                )
            }

            val innerSize = (ringSize * 0.72f).toInt()
            val letterParams = android.widget.FrameLayout.LayoutParams(innerSize, innerSize)
            letterParams.gravity = android.view.Gravity.CENTER
            ringStack.addView(letterCircle, letterParams)

            row.addView(ringStack)

            val textColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 0, 0, 0)
            }

            textColumn.addView(
                TextView(this).apply {
                    text = task.title
                    textSize = 16f
                    setTextColor(Color.parseColor("#FFFFFF"))
                }
            )

            textColumn.addView(
                TextView(this).apply {
                    text = "${task.completedDays.size} / $daysInMonth days — $percentage%"
                    textSize = 13f
                    setTextColor(Color.parseColor("#8FA8C4"))
                }
            )

            row.addView(
                textColumn,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            binding.goalsContainer.addView(row)
        }
    }

    private fun updateProgress() {

        val total = tasks.size
        val completed = tasks.count { it.completed }

        val percentage = NativeBridge.calculateProgress(
            completed,
            total
        )

        binding.progressPercentage.text = "$percentage%"
        binding.todayProgress.progress = percentage
        binding.taskCount.text = "$completed of $total tasks completed"
    }
}