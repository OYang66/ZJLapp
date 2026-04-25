package com.example.datarecorder

import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.ServerBuildingItem
import com.example.datarecorder.model.ServerProjectItem
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun MainActivity.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

private suspend fun MainActivity.syncServerProjectToLocal(projectName: String) {
    if (currentProjectId > 0L) {
        saveCurrentProjectContentNow()
    }

    val buildingResponse = RetrofitClient.api.getServerProjectBuildings(projectName)
    if (buildingResponse.code != 200 || buildingResponse.data == null) {
        toast(buildingResponse.message ?: "获取楼栋失败")
        return
    }

    val responseList: List<com.example.datarecorder.model.ServerBuildingItem> =
        buildingResponse.data ?: emptyList()

    val serverBuildings: List<String> = responseList
        .mapNotNull { item ->
            item.buildingName?.trim()
        }
        .filter { it.isNotEmpty() }
        .distinct()

    val finalBuildings: List<String> =
        if (serverBuildings.isEmpty()) listOf("1#") else serverBuildings

    // 只按项目名称找本地项目，不再按项目+楼栋创建多个项目
    val allLocalProjects = withContext(Dispatchers.IO) {
        repository.getAllProjects()
    }

    var targetProject: ProjectEntity? = allLocalProjects.firstOrNull { project ->
        project.name == projectName
    }

    val targetProjectId: Long = if (targetProject == null) {
        withContext(Dispatchers.IO) {
            repository.createProject(projectName, finalBuildings.first())
        }
    } else {
        targetProject.id
    }

    val latestProject = withContext(Dispatchers.IO) {
        repository.getProjectById(targetProjectId)
    } ?: run {
        toast("同步项目失败")
        return
    }

    // 先切到该项目
    switchProjectById(latestProject.id)

    // 把服务器楼栋同步到当前项目的楼栋Map里，只补充，不删除旧数据
    finalBuildings.forEach { buildingName ->
        buildingStandardContentMap.putIfAbsent(buildingName, "")
        buildingFastContentMap.putIfAbsent(buildingName, "")
        buildingLoadingContentMap.putIfAbsent(buildingName, "")
    }

    // 当前楼栋切到服务器返回的第一个楼栋
    currentBuildingName = finalBuildings.first()
    updateBuildingButtonText()

    // 关键：切项目后不要再把旧屏幕数据保存回去
    loadBuildingScopeToScreen(currentBuildingName, saveCurrentFirst = false)

    // 把同步后的楼栋结构保存回数据库
    saveCurrentProjectContentNow()

    toast("已同步项目：$projectName")
}


fun MainActivity.showCreateProjectDialog() {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
    }

    val inputKeyword = EditText(this).apply {
        hint = "输入关键字搜索项目名称"
        isSingleLine = true
    }

    val btnSearch = Button(this).apply {
        text = "搜索"
        isAllCaps = false
    }

    val listView = ListView(this)

    val emptyView = TextView(this).apply {
        text = "请输入关键字后点击搜索"
        gravity = Gravity.CENTER
        textSize = 15f
        setTextColor(0xFF666666.toInt())
        setPadding(0, dp(16), 0, dp(16))
    }

    val topRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    topRow.addView(
        inputKeyword,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
    )

    topRow.addView(
        btnSearch,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(8)
        }
    )

    container.addView(
        topRow,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    )

    container.addView(
        emptyView,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12)
        }
    )

    container.addView(
        listView,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(320)
        ).apply {
            topMargin = dp(8)
        }
    )

    val dialog = AlertDialog.Builder(this)
        .setTitle("新建项目")
        .setView(container)
        .setNegativeButton("取消", null)
        .create()

    val displayList = mutableListOf<String>()
    val projectList = mutableListOf<ServerProjectItem>()

    val adapter = ArrayAdapter(
        this,
        android.R.layout.simple_list_item_1,
        displayList
    )
    listView.adapter = adapter

    fun updateEmpty(text: String) {
        emptyView.text = text
        emptyView.visibility = View.VISIBLE
    }

    fun loadProjects(keyword: String?) {
        lifecycleScope.launch {
            try {
                updateEmpty("正在加载项目...")
                displayList.clear()
                projectList.clear()
                adapter.notifyDataSetChanged()

                val response = RetrofitClient.api.getServerProjectInfoList(keyword?.trim())

                if (response.code == 200 && response.data != null) {
                    val list: List<ServerProjectItem> = response.data ?: emptyList()
                    projectList.addAll(list)
                    displayList.addAll(
                        list.map { item: ServerProjectItem ->
                            item.projectName ?: "-"
                        }
                    )
                    adapter.notifyDataSetChanged()

                    if (displayList.isEmpty()) {
                        updateEmpty("未搜索到匹配项目")
                    } else {
                        emptyView.visibility = View.GONE
                    }
                } else {
                    updateEmpty(response.message ?: "加载项目失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateEmpty("加载项目失败：${e.message ?: "未知错误"}")
            }
        }
    }

    btnSearch.setOnClickListener {
        val keyword = inputKeyword.text.toString().trim()
        loadProjects(keyword)
    }

    inputKeyword.setOnEditorActionListener { _, _, _ ->
        val keyword = inputKeyword.text.toString().trim()
        loadProjects(keyword)
        true
    }

    listView.setOnItemClickListener { _, _, position, _ ->
        val item: ServerProjectItem = projectList.getOrNull(position) ?: return@setOnItemClickListener
        val projectName = item.projectName?.trim().orEmpty()
        if (projectName.isBlank()) {
            toast("项目名称无效")
            return@setOnItemClickListener
        }

        lifecycleScope.launch {
            try {
                hideKeyboard(inputKeyword)
                syncServerProjectToLocal(projectName)
                dialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                toast("同步项目失败：${e.message ?: "未知错误"}")
            }
        }
    }

    dialog.setOnShowListener {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        inputKeyword.requestFocus()
        loadProjects(null)
    }

    dialog.setOnDismissListener {
        hideKeyboard(inputKeyword)
        refreshMainLayoutAfterKeyboard()
    }

    dialog.show()
}
