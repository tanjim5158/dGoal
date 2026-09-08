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

    private var monthlyGoal = 0

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        val prefs = getSharedPreferences("dgoal_prefs", MODE_PRIVATE)
        monthlyGoal = prefs.getInt("monthly_goal", 0)

        setupButtons()

        lifecycleScope.launch {
            db.taskDao().getAllTasks().collect { taskList ->

                tasks = taskList.toMutableList()

                updateTaskUI()
            }
        }
    }

    private fun setupButtons() {

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        binding.setGoalButton.setOnClickListener {
            showSetGoalDialog()
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
        input.hint = "Enter task name"
        input.setPadding(50, 20, 50, 20)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Task")
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

    private fun showSetGoalDialog() {

        val input = TextInputEditText(this)

        input.hint = "Number of tasks"
        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        input.setPadding(50, 20, 50, 20)

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Monthly Goal")
            .setMessage("How many tasks do you want to complete this month?")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set") { _, _ ->

                val goalText = input.text.toString().trim()

                if (goalText.isNotEmpty()) {

                    val goal = goalText.toIntOrNull()

                    if (goal != null && goal > 0) {

                        monthlyGoal = goal

                        val prefs = getSharedPreferences("dgoal_prefs", MODE_PRIVATE)
                        prefs.edit().putInt("monthly_goal", goal).apply()

                        updateMonthlyGoalUI()

                    }
                }
            }
            .show()
    }

    private fun updateMonthlyGoalUI() {

        if (monthlyGoal <= 0) {

            binding.monthlyGoalText.text =
                "No monthly goal set"

            binding.monthlyGoalProgress.text =
                "Set a goal and start making progress."

            return
        }

        val completed = tasks.count { it.completed }

        val progress =
            (completed * 100 / monthlyGoal).coerceAtMost(100)

        binding.monthlyGoalText.text =
            "$completed / $monthlyGoal tasks completed"

        binding.monthlyGoalProgress.text =
            "$progress%"
    }

    private fun updateTaskUI() {

        binding.taskContainer.removeAllViews()

        if (tasks.isEmpty()) {

            binding.taskContainer.addView(
                TextView(this).apply {
                    text = "No tasks yet\nTap + Add Task to create your first task"
                    textSize = 15f
                    setTextColor(Color.parseColor("#777B82"))
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
                    setTextColor(Color.parseColor("#17191C"))
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
        updateMonthlyGoalUI()
    }

    private fun updateGoalsUI() {

        binding.goalsContainer.removeAllViews()

        if (tasks.isEmpty()) {

            binding.goalsContainer.addView(
                TextView(this).apply {
                    text = "No tasks yet\nAdd tasks to start tracking monthly goals"
                    textSize = 15f
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

            val pieChart = PieChartView(this).apply {
                this.percentage = percentage
            }

            row.addView(
                pieChart,
                LinearLayout.LayoutParams(120, 120)
            )

            val textColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(30, 0, 0, 0)
            }

            textColumn.addView(
                TextView(this).apply {
                    text = task.title
                    textSize = 16f
                    setTextColor(Color.parseColor("#17191C"))
                }
            )

            textColumn.addView(
                TextView(this).apply {
                    text = "${task.completedDays.size} / $daysInMonth days — $percentage%"
                    textSize = 13f
                    setTextColor(Color.parseColor("#777B82"))
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