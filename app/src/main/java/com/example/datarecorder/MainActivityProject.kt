package com.example.datarecorder

import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// =========================
// 项目 / 楼栋 / 包号 / 车次”逻辑
// =========================
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

    if (currentModeType == ModeType.QUALITY_FEEDBACK) {
        showQualityFloorInputDialog()
        return
    }


    val popup = PopupMenu(this, anchor)
    var order = 0

    if (currentModeType == ModeType.RETURN_LOADING) {
        loadingTripMap.keys.forEachIndexed { index, tripName ->
            popup.menu.add(0, 1000 + index, order++, tripName)
        }
        popup.menu.add(0, 1, order++, "增加车次")
        popup.menu.add(0, 2, order, "删除当前车次")
    } else {
        getAllPackageNamesInOrder().forEachIndexed { index, packageName ->
            popup.menu.add(0, 1000 + index, order++, packageName)
        }
        popup.menu.add(0, 1, order++, "增加包号")
        popup.menu.add(0, 2, order, "删除当前包号")
    }

    popup.setOnMenuItemClickListener { item ->
        when {
            item.itemId == 1 -> {
                if (currentModeType == ModeType.RETURN_LOADING) {
                    addNewLoadingTrip()
                } else {
                    addNewPackage()
                }
                true
            }

            item.itemId == 2 -> {
                confirmDeleteCurrentPackageOrTrip()
                true
            }

            item.itemId >= 1000 -> {
                if (currentModeType == ModeType.RETURN_LOADING) {
                    switchLoadingTrip(item.title.toString())
                } else {
                    switchPackage(item.title.toString())
                }
                true
            }

            else -> false
        }
    }
    popup.show()
}

suspend fun MainActivity.createProjectInternal(name: String, buildingName: String) {
    if (currentProjectId > 0L) {
        saveCurrentProjectContentNow()
    }

    val newId = withContext(Dispatchers.IO) {
        repository.createProject(name, buildingName)
    }

    switchProjectById(newId)
    toast("已创建项目：$name")
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



fun MainActivity.addNewLoadingTrip() {
    if (currentProjectId <= 0L) {
        toast("请先新建或选择项目")
        return
    }

    saveLoadingScreenToCurrentTrip()

    var maxIndex = 0
    loadingTripMap.keys.forEach { name ->
        val value = Regex("""第(\d+)车""").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (value > maxIndex) maxIndex = value
    }

    val tripName = "第${maxIndex + 1}车"
    loadingTripMap[tripName] = ReturnLoadingTripData(
        tripName = tripName,
        aluminumWeightMode = LoadingWeightMode.UNSELECTED,
        ironWeightMode = LoadingWeightMode.UNSELECTED
    )

    switchLoadingTrip(tripName)
    triggerAutoSave()
}
fun MainActivity.switchLoadingTrip(tripName: String) {
    if (currentLoadingTripName.isNotBlank() && loadingTripMap.containsKey(currentLoadingTripName)) {
        saveLoadingScreenToCurrentTrip()
    }

    currentLoadingTripName = tripName
    btnPackageMenu.text = tripName

    val trip = loadingTripMap[tripName] ?: ReturnLoadingTripData(tripName = tripName)

    loadingAluminumRows.clear()
    loadingAluminumRows.addAll(trip.aluminumRows.map { it.copy() })
    loadingIronRows.clear()
    loadingIronRows.addAll(trip.ironRows.map { it.copy() })
    vehicleInfo = trip.vehicleInfo.copy()
    loadingAluminumWeightMode = trip.aluminumWeightMode
    loadingIronWeightMode = trip.ironWeightMode

    currentLoadingEditType = ReturnLoadingType.ALUMINUM
    currentLoadingEditIndex = if (loadingAluminumRows.isEmpty()) -1 else 0
    currentLoadingField = ReturnLoadingField.MATERIAL_NAME

    renderLoadingTable()
    triggerAutoSave()
}


fun MainActivity.deleteCurrentPackage() {
    if (currentPackageName.isBlank()) {
        toast("当前没有可删除的包号")
        return
    }

    val targetName = currentPackageName
    val allNames = getAllPackageNamesInOrder()
    if (!allNames.contains(targetName)) {
        toast("当前包号不存在")
        return
    }

    packageStandardRowsMap.remove(targetName)
    packageCurrentStandardRowMap.remove(targetName)
    packageFastRowsMap.remove(targetName)
    packageCurrentFastRowMap.remove(targetName)
    packageDateMap.remove(targetName)

    val remainNames = getAllPackageNamesInOrder()
    if (remainNames.isNotEmpty()) {
        val oldIndex = allNames.indexOf(targetName)
        val nextName = remainNames.getOrNull(oldIndex.coerceAtMost(remainNames.lastIndex)) ?: remainNames.first()
        loadPackageToScreen(nextName)
    } else {
        resetForNewProjectWithoutPackage()
    }

    triggerAutoSave()
    toast("已删除：$targetName")
}

fun MainActivity.deleteCurrentLoadingTrip() {
    if (currentLoadingTripName.isBlank()) {
        toast("当前没有可删除的车次")
        return
    }

    val targetName = currentLoadingTripName
    val allNames = loadingTripMap.keys.toList()
    if (!loadingTripMap.containsKey(targetName)) {
        toast("当前车次不存在")
        return
    }

    loadingTripMap.remove(targetName)

    val remainNames = loadingTripMap.keys.toList()
    if (remainNames.isNotEmpty()) {
        val oldIndex = allNames.indexOf(targetName)
        val nextName = remainNames.getOrNull(oldIndex.coerceAtMost(remainNames.lastIndex)) ?: remainNames.first()
        switchLoadingTrip(nextName)
    } else {
        currentLoadingTripName = ""
        btnPackageMenu.text = "车次"
        loadingAluminumRows.clear()
        loadingIronRows.clear()
        vehicleInfo = VehicleInfo()
        currentLoadingEditType = ReturnLoadingType.ALUMINUM
        currentLoadingEditIndex = -1
        currentLoadingField = ReturnLoadingField.MATERIAL_NAME
        renderLoadingTable()
        triggerAutoSave()
    }

    toast("已删除：$targetName")
}


fun MainActivity.confirmDeleteCurrentPackageOrTrip() {
    if (currentModeType == ModeType.RETURN_LOADING) {
        if (currentLoadingTripName.isBlank()) {
            toast("当前没有可删除的车次")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("是否删除${currentLoadingTripName}数据？\n删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deleteCurrentLoadingTrip()
            }
            .show()
    } else {
        if (currentPackageName.isBlank()) {
            toast("当前没有可删除的包号")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("是否删除${currentPackageName}数据？\n删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                deleteCurrentPackage()
            }
            .show()
    }
}


fun MainActivity.saveLoadingScreenToCurrentTrip() {
    if (currentLoadingTripName.isBlank()) return

    loadingTripMap[currentLoadingTripName] = ReturnLoadingTripData(
        tripName = currentLoadingTripName,
        aluminumRows = loadingAluminumRows.map { it.copy() }.toMutableList(),
        ironRows = loadingIronRows.map { it.copy() }.toMutableList(),
        vehicleInfo = vehicleInfo.copy(),
        aluminumWeightMode = loadingAluminumWeightMode,
        ironWeightMode = loadingIronWeightMode
    )
}


fun MainActivity.serializeLoadingContent(): String {
    saveLoadingScreenToCurrentTrip()

    val root = JSONObject()
    root.put("currentLoadingTripName", currentLoadingTripName)
    root.put("loadingAluminumColumnMode", if (loadingAluminumUsePackageCount) "COUNT" else "PACKAGE_NO")
    root.put("loadingAluminumWeightMode", loadingAluminumWeightMode.name)
    root.put("loadingIronWeightMode", loadingIronWeightMode.name)


    val trips = JSONArray()
    loadingTripMap.values.forEach { trip ->
        val obj = JSONObject()
        obj.put("tripName", trip.tripName)
        obj.put("aluminumWeightMode", trip.aluminumWeightMode.name)
        obj.put("ironWeightMode", trip.ironWeightMode.name)


        val alArray = JSONArray()
        trip.aluminumRows.forEach { row ->
            val rowObj = JSONObject()
            rowObj.put("materialName", row.materialName)
            rowObj.put("packageOrCount", row.packageOrCount)
            rowObj.put("areaOrWeight", row.areaOrWeight)
            rowObj.put("weight", row.weight)
            rowObj.put("remark", row.remark)
            alArray.put(rowObj)
        }
        obj.put("aluminumRows", alArray)

        val ironArray = JSONArray()
        trip.ironRows.forEach { row ->
            val rowObj = JSONObject()
            rowObj.put("materialName", row.materialName)
            rowObj.put("packageOrCount", row.packageOrCount)
            rowObj.put("areaOrWeight", row.areaOrWeight)
            rowObj.put("weight", row.weight)
            rowObj.put("remark", row.remark)
            ironArray.put(rowObj)
        }
        obj.put("ironRows", ironArray)

        val vehicleObj = JSONObject()
        vehicleObj.put("grossWeight", trip.vehicleInfo.grossWeight)
        vehicleObj.put("tareWeight", trip.vehicleInfo.tareWeight)
        vehicleObj.put("middleAluminumWeight", trip.vehicleInfo.middleAluminumWeight)
        vehicleObj.put("middleIronWeight", trip.vehicleInfo.middleIronWeight)
        vehicleObj.put("woodEstimate", trip.vehicleInfo.woodEstimate)
        vehicleObj.put("vehiclePlateNumber", trip.vehicleInfo.vehiclePlateNumber)
        vehicleObj.put("loadingDate", trip.vehicleInfo.loadingDate)
        obj.put("vehicleInfo", vehicleObj)


        trips.put(obj)
    }

    root.put("trips", trips)
    return root.toString()
}

fun MainActivity.wrapBuildingScopedContent(
    currentBuilding: String,
    contentMap: Map<String, String>
): String {
    val root = JSONObject()
    root.put("currentBuildingName", currentBuilding)

    val buildings = JSONArray()
    contentMap.forEach { (buildingName, content) ->
        val obj = JSONObject()
        obj.put("buildingName", buildingName)
        obj.put("content", content)
        buildings.put(obj)
    }

    root.put("buildings", buildings)
    return root.toString()
}

fun MainActivity.parseBuildingScopedContent(
    raw: String,
    fallbackBuildingName: String
): Pair<String, LinkedHashMap<String, String>> {
    val result = linkedMapOf<String, String>()
    val defaultBuilding = if (fallbackBuildingName.isBlank()) "1#" else fallbackBuildingName

    if (raw.isBlank()) {
        result[defaultBuilding] = ""
        return defaultBuilding to result
    }

    return try {
        val root = JSONObject(raw)
        if (!root.has("buildings")) {
            result[defaultBuilding] = raw
            defaultBuilding to result
        } else {
            val current = root.optString("currentBuildingName", defaultBuilding)
            val buildings = root.optJSONArray("buildings") ?: JSONArray()

            for (i in 0 until buildings.length()) {
                val obj = buildings.getJSONObject(i)
                val buildingName = obj.optString("buildingName", "")
                if (buildingName.isBlank()) continue
                result[buildingName] = obj.optString("content", "")
            }

            if (result.isEmpty()) {
                result[defaultBuilding] = ""
            }

            val safeCurrent = when {
                current.isNotBlank() && result.containsKey(current) -> current
                result.containsKey(defaultBuilding) -> defaultBuilding
                result.isNotEmpty() -> result.keys.first()
                else -> defaultBuilding
            }

            safeCurrent to result
        }
    } catch (_: Exception) {
        result[defaultBuilding] = raw
        defaultBuilding to result
    }
}


fun MainActivity.serializeProjectStandardBuildingsContent(): String {
    saveCurrentBuildingScopeToMemory()
    return wrapBuildingScopedContent(currentBuildingName, buildingStandardContentMap)
}

fun MainActivity.serializeProjectFastBuildingsContent(): String {
    saveCurrentBuildingScopeToMemory()
    return wrapBuildingScopedContent(currentBuildingName, buildingFastContentMap)
}

fun MainActivity.serializeProjectLoadingBuildingsContent(): String {
    saveCurrentBuildingScopeToMemory()
    return wrapBuildingScopedContent(currentBuildingName, buildingLoadingContentMap)
}

fun MainActivity.showBuildingMenuPopup(anchor: View) {
    if (currentProjectId <= 0L) {
        toast("请先新建或选择项目")
        return
    }

    val popup = PopupMenu(this, anchor)
    var order = 0

    getAllBuildingNamesInOrder().forEachIndexed { index, buildingName ->
        popup.menu.add(0, 1000 + index, order++, buildingName)
    }
    popup.menu.add(0, 1, order++, "增加楼栋")
    popup.menu.add(0, 2, order, "删除当前楼栋")

    popup.setOnMenuItemClickListener { item ->
        when {
            item.itemId == 1 -> {
                showCreateBuildingDialog()
                true
            }

            item.itemId == 2 -> {
                confirmDeleteCurrentBuilding()
                true
            }

            item.itemId >= 1000 -> {
                switchBuilding(item.title.toString())
                true
            }

            else -> false
        }
    }
    popup.show()
}

fun MainActivity.switchProject(project: ProjectEntity) {
    lifecycleScope.launch {
        switchProjectById(project.id)
    }
}


suspend fun MainActivity.switchProjectById(projectId: Long) {
    if (projectId <= 0L) return

    if (currentProjectId > 0L) {
        saveCurrentProjectContentNow()
    }

    val latestProject = withContext(Dispatchers.IO) {
        repository.getProjectById(projectId)
    } ?: run {
        toast("项目不存在或已删除")
        return
    }

    currentProjectId = latestProject.id
    currentProjectName = latestProject.name
    currentBuildingName = if (latestProject.buildingName.isBlank()) "1号楼" else latestProject.buildingName
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

    rebuildPackageMapsFromProject(latestProject)
}


fun MainActivity.showCreateBuildingDialog() {
    val input = android.widget.EditText(this).apply {
        hint = "请输入楼栋号，如：2号楼"
        isSingleLine = true
    }

    AlertDialog.Builder(this)
        .setTitle("增加楼栋")
        .setView(input)
        .setNegativeButton("取消", null)
        .setPositiveButton("确定", null)
        .create()
        .also { dialog ->
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isBlank()) {
                        toast("楼栋号不能为空")
                        return@setOnClickListener
                    }

                    if (getAllBuildingNamesInOrder().contains(name)) {
                        toast("楼栋已存在")
                        return@setOnClickListener
                    }

                    saveCurrentBuildingScopeToMemory()
                    buildingStandardContentMap[name] = ""
                    buildingFastContentMap[name] = ""
                    buildingLoadingContentMap[name] = ""
                    switchBuilding(name)
                    saveCurrentProjectContent()
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
}

fun MainActivity.switchBuilding(buildingName: String) {
    if (buildingName.isBlank()) return

    saveCurrentBuildingScopeToMemory()
    loadBuildingScopeToScreen(buildingName)
    saveCurrentProjectContent()
}

fun MainActivity.confirmDeleteCurrentBuilding() {
    val target = currentBuildingName
    if (target.isBlank()) {
        toast("当前没有可删除的楼栋")
        return
    }

    val allBuildings = getAllBuildingNamesInOrder()
    if (allBuildings.size <= 1) {
        toast("至少保留一个楼栋")
        return
    }

    AlertDialog.Builder(this)
        .setTitle("确认删除")
        .setMessage("是否删除${target}数据？\n删除后无法恢复。")
        .setNegativeButton("取消", null)
        .setPositiveButton("删除") { _, _ ->
            deleteCurrentBuilding()
        }
        .show()
}

fun MainActivity.deleteCurrentBuilding() {
    val target = currentBuildingName
    if (target.isBlank()) return

    saveCurrentBuildingScopeToMemory()

    val allBuildings = getAllBuildingNamesInOrder()
    val oldIndex = allBuildings.indexOf(target)

    buildingStandardContentMap.remove(target)
    buildingFastContentMap.remove(target)
    buildingLoadingContentMap.remove(target)

    val remain = getAllBuildingNamesInOrder()
    val nextBuilding = remain.getOrNull(oldIndex.coerceAtMost(remain.lastIndex)) ?: remain.first()

    loadBuildingScopeToScreen(nextBuilding)
    saveCurrentProjectContent()
    toast("已删除：$target")
}


fun MainActivity.deserializeLoadingContent(content: String) {
    loadingTripMap.clear()
    loadingAluminumRows.clear()
    loadingIronRows.clear()
    currentLoadingTripName = ""
    vehicleInfo = VehicleInfo()

    if (content.isBlank()) return

    val root = JSONObject(content)
    val restoredTripName = root.optString("currentLoadingTripName", "")
    loadingAluminumUsePackageCount =
        root.optString("loadingAluminumColumnMode", "PACKAGE_NO") == "COUNT"
    loadingAluminumWeightMode = try {
        LoadingWeightMode.valueOf(
            root.optString("loadingAluminumWeightMode", LoadingWeightMode.SINGLE_PACKAGE.name)
        )
    } catch (_: Exception) {
        LoadingWeightMode.SINGLE_PACKAGE
    }

    loadingIronWeightMode = try {
        LoadingWeightMode.valueOf(
            root.optString("loadingIronWeightMode", LoadingWeightMode.SINGLE_PACKAGE.name)
        )
    } catch (_: Exception) {
        LoadingWeightMode.SINGLE_PACKAGE
    }


    val trips = root.optJSONArray("trips") ?: JSONArray()
    for (i in 0 until trips.length()) {
        val obj = trips.getJSONObject(i)
        val tripName = obj.optString("tripName", "")
        if (tripName.isBlank()) continue

        val trip = ReturnLoadingTripData(tripName = tripName)
        trip.aluminumWeightMode = try {
            LoadingWeightMode.valueOf(
                obj.optString("aluminumWeightMode", LoadingWeightMode.SINGLE_PACKAGE.name)
            )
        } catch (_: Exception) {
            LoadingWeightMode.SINGLE_PACKAGE
        }

        trip.ironWeightMode = try {
            LoadingWeightMode.valueOf(
                obj.optString("ironWeightMode", LoadingWeightMode.SINGLE_PACKAGE.name)
            )
        } catch (_: Exception) {
            LoadingWeightMode.SINGLE_PACKAGE
        }


        val alArray = obj.optJSONArray("aluminumRows") ?: JSONArray()
        for (j in 0 until alArray.length()) {
            val rowObj = alArray.getJSONObject(j)
            trip.aluminumRows.add(
                ReturnLoadingRow(
                    type = ReturnLoadingType.ALUMINUM,
                    materialName = rowObj.optString("materialName"),
                    packageOrCount = rowObj.optString("packageOrCount"),
                    areaOrWeight = rowObj.optString("areaOrWeight"),
                    weight = rowObj.optString("weight"),
                    remark = rowObj.optString("remark")
                )
            )
        }

        val ironArray = obj.optJSONArray("ironRows") ?: JSONArray()
        for (j in 0 until ironArray.length()) {
            val rowObj = ironArray.getJSONObject(j)
            trip.ironRows.add(
                ReturnLoadingRow(
                    type = ReturnLoadingType.IRON,
                    materialName = rowObj.optString("materialName"),
                    packageOrCount = rowObj.optString("packageOrCount"),
                    areaOrWeight = rowObj.optString("areaOrWeight"),
                    weight = rowObj.optString("weight"),
                    remark = rowObj.optString("remark")
                )
            )
        }

        val vehicleObj = obj.optJSONObject("vehicleInfo") ?: JSONObject()
        trip.vehicleInfo = VehicleInfo(
            grossWeight = vehicleObj.optString("grossWeight"),
            tareWeight = vehicleObj.optString("tareWeight"),
            middleAluminumWeight = vehicleObj.optString("middleAluminumWeight"),
            middleIronWeight = vehicleObj.optString("middleIronWeight"),
            woodEstimate = vehicleObj.optString("woodEstimate"),
            vehiclePlateNumber = vehicleObj.optString("vehiclePlateNumber"),
            loadingDate = vehicleObj.optString("loadingDate")
        )


        loadingTripMap[tripName] = trip
    }

    val targetTripName = when {
        restoredTripName.isNotBlank() && loadingTripMap.containsKey(restoredTripName) -> restoredTripName
        loadingTripMap.isNotEmpty() -> loadingTripMap.keys.first()
        else -> ""
    }

    currentLoadingTripName = targetTripName

    if (targetTripName.isNotBlank()) {
        val trip = loadingTripMap[targetTripName]
        loadingAluminumRows.clear()
        loadingAluminumRows.addAll(trip?.aluminumRows?.map { it.copy() } ?: emptyList())
        loadingIronRows.clear()
        loadingIronRows.addAll(trip?.ironRows?.map { it.copy() } ?: emptyList())
        vehicleInfo = trip?.vehicleInfo?.copy() ?: VehicleInfo()
        loadingAluminumWeightMode = trip?.aluminumWeightMode ?: LoadingWeightMode.SINGLE_PACKAGE
        loadingIronWeightMode = trip?.ironWeightMode ?: LoadingWeightMode.SINGLE_PACKAGE
    } else {
        loadingAluminumRows.clear()
        loadingIronRows.clear()
        vehicleInfo = VehicleInfo()
        loadingAluminumWeightMode = LoadingWeightMode.UNSELECTED
        loadingIronWeightMode = LoadingWeightMode.UNSELECTED
    }


    currentLoadingEditType = ReturnLoadingType.ALUMINUM
    currentLoadingEditIndex = if (loadingAluminumRows.isEmpty()) -1 else 0
    currentLoadingField = ReturnLoadingField.MATERIAL_NAME
}



fun MainActivity.showProjectSelectDialog() {
    lifecycleScope.launch {
        val projects = withContext(Dispatchers.IO) { repository.getAllProjects() }

        if (projects.isEmpty()) {
            toast("暂无项目")
            return@launch
        }

        val scrollView = ScrollView(this@showProjectSelectDialog)
        val container = LinearLayout(this@showProjectSelectDialog).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        scrollView.addView(container)

        val dialog = AlertDialog.Builder(this@showProjectSelectDialog)
            .setTitle("选择项目")
            .setView(scrollView)
            .setNegativeButton("关闭", null)
            .create()

        projects.forEach { project ->
            val row = LinearLayout(this@showProjectSelectDialog).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(6), dp(6), dp(6))
            }

            val nameView = TextView(this@showProjectSelectDialog).apply {
                text = project.name

                textSize = 16f
                setTextColor(0xFF222222.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener {
                    lifecycleScope.launch {
                        switchProjectById(project.id)
                        dialog.dismiss()
                    }
                }
            }

            val deleteBtn = Button(this@showProjectSelectDialog).apply {
                text = "删除"
                textSize = 12f
                isAllCaps = false
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(10), dp(4), dp(10), dp(4))
                setOnClickListener {
                    confirmDeleteProject(project, dialog)
                }
            }

            row.addView(nameView)
            row.addView(deleteBtn)
            container.addView(row)
        }

        dialog.show()
    }
}



fun MainActivity.showProjectMenuPopup(anchor: View) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "选择项目")
    popup.menu.add(0, 2, 1, "新建项目")
    popup.menu.add(0, 3, 2, "查看服务器项目")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                showProjectSelectDialog()
                true
            }
            2 -> {
                showCreateProjectDialog()
                true
            }
            3 -> {
                loadServerProjectList()
                true
            }
            else -> false
        }
    }
    popup.show()
}



fun MainActivity.rebuildPackageMapsFromProject(project: ProjectEntity) {
    buildingStandardContentMap.clear()
    buildingFastContentMap.clear()
    buildingLoadingContentMap.clear()
    buildingQualityContentMap.clear()


    val defaultBuilding = if (project.buildingName.isBlank()) "1号楼" else project.buildingName

    val (standardCurrentBuilding, standardMap) =
        parseBuildingScopedContent(project.standardContent, defaultBuilding)
    val (fastCurrentBuilding, fastMap) =
        parseBuildingScopedContent(project.fastContent, defaultBuilding)
    val (loadingCurrentBuilding, loadingMap) =
        parseBuildingScopedContent(project.loadingContent, defaultBuilding)
    val (qualityCurrentBuilding, qualityMap) =
        parseBuildingScopedContent(project.qualityContent, defaultBuilding)

    buildingStandardContentMap.putAll(standardMap)
    buildingFastContentMap.putAll(fastMap)
    buildingLoadingContentMap.putAll(loadingMap)
    buildingQualityContentMap.putAll(qualityMap)


    val allBuildings = linkedSetOf<String>()
    allBuildings.add(defaultBuilding)
    allBuildings.addAll(buildingStandardContentMap.keys)
    allBuildings.addAll(buildingFastContentMap.keys)
    allBuildings.addAll(buildingLoadingContentMap.keys)
    allBuildings.addAll(buildingQualityContentMap.keys)

    currentBuildingName = listOf(
        project.buildingName,
        standardCurrentBuilding,
        fastCurrentBuilding,
        loadingCurrentBuilding,
        qualityCurrentBuilding
    ).firstOrNull { it.isNotBlank() && allBuildings.contains(it) } ?: defaultBuilding

    allBuildings.forEach {
        buildingStandardContentMap.putIfAbsent(it, "")
        buildingFastContentMap.putIfAbsent(it, "")
        buildingLoadingContentMap.putIfAbsent(it, "")
        buildingQualityContentMap.putIfAbsent(it, "")
    }


    // 切换项目时，绝对不能先保存当前屏幕旧数据到新项目
    loadBuildingScopeToScreen(currentBuildingName, saveCurrentFirst = false)
    ensureDefaultPackageExists()
    ensureDefaultLoadingTripExists()
    updatePackageButtonText()

}

