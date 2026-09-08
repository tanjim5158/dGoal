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

        if (tasks.isEmpty()) {

            MaterialAlertDialogBuilder(this)
                .setTitle("No tasks yet")
                .setMessage("Add some tasks first, then come back to choose which ones count toward your monthly goal.")
                .setPositiveButton("OK", null)
                .show()

            return
        }

        val taskNames = tasks.map { it.title }.toTypedArray()

        val checkedItems = tasks.map { it.isInMonthlyGoal }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Monthly Goal")
            .setMessage("Choose which tasks to track this month. Each one's target is to be completed every day of the month.")
            .setMultiChoiceItems(taskNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->

                tasks.forEachIndexed { index, task ->

                    task.isInMonthlyGoal = checkedItems[index]

                    lifecycleScope.launch {
                        db.taskDao().updateTask(task)
                    }
                }
            }
            .show()
    }

    private fun updateMonthlyGoalUI() {

        val goalTasks = tasks.filter { it.isInMonthlyGoal }

        if (goalTasks.isEmpty()) {

            binding.monthlyGoalText.text =
                "No monthly goal set"

            binding.monthlyGoalProgress.text =
                "Tap below to choose tasks for this month."

            return
        }

        val daysInMonth = java.util.Calendar.getInstance()
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        val target = goalTasks.size * daysInMonth

        val completed = goalTasks.sumOf {
            it.completedDays.size.coerceAtMost(daysInMonth)
        }

        val progress = (completed * 100 / target).coerceAtMost(100)

        binding.monthlyGoalText.text =
            "$completed / $target days completed"

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