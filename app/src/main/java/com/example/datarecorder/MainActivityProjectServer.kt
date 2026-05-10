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
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher



private suspend fun MainActivity.syncServerProjectToLocal(projectName: String) {
    val safeProjectName = projectName.trim()
    if (safeProjectName.isBlank()) {
        toast("项目名称无效")
        return
    }

    if (currentProjectId > 0L && currentProjectName == safeProjectName) {
        showInfoCardDialog("项目已存在", "当前项目“$safeProjectName”已经打开，原有数据已保留。")
        return
    }

    val existingProjects = withContext(Dispatchers.IO) {
        repository.getProjectsByName(safeProjectName)
    }
    if (existingProjects.isNotEmpty()) {
        val previousProjectId = currentProjectId
        val existingProject = switchToExistingProjectByName(safeProjectName)
        if (existingProject != null && currentProjectId == existingProject.id && previousProjectId != existingProject.id) {
            showInfoCardDialog("项目已存在", "项目“$safeProjectName”已存在，已切换到现有项目，原有数据已保留。")
        } else {
            showInfoCardDialog("项目已存在", "本地已存在同名项目，已取消本次同步，原有数据不会被覆盖。")
        }
        return
    }

    if (currentProjectId > 0L) {
        saveCurrentProjectContentNow()
    }

    val buildingResponse = RetrofitClient.api.getServerProjectBuildings(safeProjectName)
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
        if (serverBuildings.isEmpty()) listOf("1号楼") else serverBuildings

    val targetProjectId = withContext(Dispatchers.IO) {
        repository.createProject(safeProjectName, finalBuildings.first())
    }

    val latestProject = withContext(Dispatchers.IO) {
        repository.getProjectById(targetProjectId)
    } ?: run {
        toast("同步项目失败")
        return
    }

    if (currentProjectId != latestProject.id) {
        switchProjectById(latestProject.id)
    }

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
    ensureDefaultPackageExists()
    ensureDefaultLoadingTripExists()
    updatePackageButtonText()

// 把默认第1包/第1车立即保存回数据库
    saveCurrentProjectContentNow()
    scheduleSubDisplaySnapshotSync()

    toast("已同步项目：$safeProjectName")

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
        text = "请输入关键字搜索项目名称"
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

    val dialog = createCardDialog(
        title = "新建项目",
        subtitle = "搜索服务器项目并同步到本地"
    ) { dlg ->
        addView(
            container,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            createDialogActionButton("关闭", primary = false) {
                dlg.dismiss()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
            ).apply {
                topMargin = dp(16)
            }
        )
    }

    val displayList = mutableListOf<String>()
    val projectList = mutableListOf<ServerProjectItem>()

    val adapter = ArrayAdapter(
        this,
        android.R.layout.simple_list_item_1,
        displayList
    )
    listView.adapter = adapter

    val searchHandler = Handler(Looper.getMainLooper())
    var pendingSearchRunnable: Runnable? = null

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
                        list.mapNotNull { item ->
                            item.projectName?.trim()?.takeIf { it.isNotEmpty() }
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

    fun triggerSearch(immediate: Boolean = false) {
        pendingSearchRunnable?.let { searchHandler.removeCallbacks(it) }

        val keyword = inputKeyword.text.toString().trim()

        val task = Runnable {
            loadProjects(keyword.ifBlank { null })
        }

        pendingSearchRunnable = task

        if (immediate) {
            task.run()
        } else {
            searchHandler.postDelayed(task, 300)
        }
    }

    btnSearch.setOnClickListener {
        triggerSearch(immediate = true)
    }

    inputKeyword.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            triggerSearch(immediate = false)
        }

        override fun afterTextChanged(s: Editable?) = Unit
    })

    inputKeyword.setOnEditorActionListener { _, _, _ ->
        triggerSearch(immediate = true)
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
        pendingSearchRunnable?.let { searchHandler.removeCallbacks(it) }
        hideKeyboard(inputKeyword)
        refreshMainLayoutAfterKeyboard()
    }

    dialog.show()
}

