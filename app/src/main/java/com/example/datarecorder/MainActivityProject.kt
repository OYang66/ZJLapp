package com.example.datarecorder

import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun MainActivity.addNewPackage() {
    if (currentProjectId <= 0L) {
        toast("请先新建或选择项目")
        return
    }

    saveScreenDataToCurrentPackage()

    val packageName = generateNextPackageName()
    packageStandardRowsMap[packageName] = mutableListOf()
    packageCurrentStandardRowMap[packageName] = StandardRow()
    packageFastRowsMap[packageName] = mutableListOf()
    packageCurrentFastRowMap[packageName] = FastRow()
    packageDateMap[packageName] = getTodayPackageDate()

    loadPackageToScreen(packageName)
    triggerAutoSave()
}

fun MainActivity.switchPackage(packageName: String) {
    if (packageName.isBlank()) return
    if (!getAllPackageNamesInOrder().contains(packageName)) return

    saveScreenDataToCurrentPackage()
    loadPackageToScreen(packageName)
}

fun MainActivity.showPackageMenuPopup(anchor: View) {
    if (currentProjectId <= 0L) {
        toast("请先新建或选择项目")
        return
    }

    val popup = PopupMenu(this, anchor)
    var order = 0

    getAllPackageNamesInOrder().forEachIndexed { index, packageName ->
        popup.menu.add(0, 1000 + index, order++, packageName)
    }
    popup.menu.add(0, 1, order, "增加包号")

    popup.setOnMenuItemClickListener { item ->
        when {
            item.itemId == 1 -> {
                addNewPackage()
                true
            }
            item.itemId >= 1000 -> {
                switchPackage(item.title.toString())
                true
            }
            else -> false
        }
    }
    popup.show()
}

suspend fun MainActivity.createProjectInternal(name: String, buildingName: String) {
    val newId = withContext(Dispatchers.IO) {
        repository.createProject(name, buildingName)
    }

    val project = withContext(Dispatchers.IO) {
        repository.getAllProjects().firstOrNull { it.id == newId }
    }

    if (project != null) {
        switchProject(project)
        currentBuildingName = buildingName
        resetForNewProjectWithoutPackage()
        saveCurrentProjectContent()
        toast("已创建项目：$name")
    }
}

fun MainActivity.switchProject(project: ProjectEntity) {
    saveScreenDataToCurrentPackage()

    currentProjectId = project.id
    currentProjectName = project.name
    currentBuildingName = project.buildingName
    tvProjectName.text = "当前项目：$currentProjectName"

    getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        .edit()
        .putLong("last_project_id", currentProjectId)
        .apply()

    clearStandardEditingState()
    clearFastEditingState()
    pendingReplaceStandardEditing = false
    pendingReplaceFastEditing = false
    pendingReplaceCurrentFastModel = false
    pendingReplaceCurrentStandardModel = false

    rebuildPackageMapsFromProject(project)
    updatePackageButtonText()
    updateDisplayTable()
}

suspend fun MainActivity.deleteProjectInternal(project: ProjectEntity) {
    withContext(Dispatchers.IO) {
        repository.deleteProject(project)
    }

    val remainProjects = withContext(Dispatchers.IO) {
        repository.getAllProjects()
    }

    if (project.id == currentProjectId) {
        val nextProject = remainProjects.firstOrNull()
        if (nextProject != null) {
            switchProject(nextProject)
        } else {
            val newId = withContext(Dispatchers.IO) {
                repository.createProject("默认项目", "1号楼")
            }
            val newProject = withContext(Dispatchers.IO) {
                repository.getAllProjects().firstOrNull { it.id == newId }
            }
            if (newProject != null) {
                switchProject(newProject)
            }
        }
    }

    toast("项目已删除")
}


fun MainActivity.confirmDeleteProject(project: ProjectEntity, parentDialog: AlertDialog) {
    AlertDialog.Builder(this)
        .setTitle("确认删除")
        .setMessage("确定删除项目“${project.name}”吗？\n删除后无法恢复。")
        .setNegativeButton("取消", null)
        .setPositiveButton("删除") { _, _ ->
            lifecycleScope.launch {
                deleteProjectInternal(project)
                parentDialog.dismiss()
                showProjectSelectDialog()
            }
        }
        .show()
}

