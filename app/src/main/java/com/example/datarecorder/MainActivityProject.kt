package com.example.datarecorder

import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


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

    if (currentModeType == ModeType.RETURN_LOADING) {
        loadingTripMap.keys.forEachIndexed { index, tripName ->
            popup.menu.add(0, 1000 + index, order++, tripName)
        }
        popup.menu.add(0, 1, order, "增加车次")
    } else {
        getAllPackageNamesInOrder().forEachIndexed { index, packageName ->
            popup.menu.add(0, 1000 + index, order++, packageName)
        }
        popup.menu.add(0, 1, order, "增加包号")
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
    saveLoadingScreenToCurrentTrip()

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
    deserializeLoadingContent(project.loadingContent)

    updatePackageButtonText()
    switchMode(currentModeType)
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
    loadingTripMap[tripName] = ReturnLoadingTripData(tripName = tripName)
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

    currentLoadingEditType = ReturnLoadingType.ALUMINUM
    currentLoadingEditIndex = if (loadingAluminumRows.isEmpty()) -1 else 0
    currentLoadingField = ReturnLoadingField.MATERIAL_NAME

    renderLoadingTable()
    triggerAutoSave()
}


fun MainActivity.saveLoadingScreenToCurrentTrip() {
    if (currentLoadingTripName.isBlank()) return

    loadingTripMap[currentLoadingTripName] = ReturnLoadingTripData(
        tripName = currentLoadingTripName,
        aluminumRows = loadingAluminumRows.map { it.copy() }.toMutableList(),
        ironRows = loadingIronRows.map { it.copy() }.toMutableList(),
        vehicleInfo = vehicleInfo.copy()
    )
}

fun MainActivity.serializeLoadingContent(): String {
    saveLoadingScreenToCurrentTrip()

    val root = JSONObject()
    root.put("currentLoadingTripName", currentLoadingTripName)
    root.put("loadingAluminumColumnMode", if (loadingAluminumUsePackageCount) "COUNT" else "PACKAGE_NO")

    val trips = JSONArray()
    loadingTripMap.values.forEach { trip ->
        val obj = JSONObject()
        obj.put("tripName", trip.tripName)

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
        obj.put("vehicleInfo", vehicleObj)

        trips.put(obj)
    }

    root.put("trips", trips)
    return root.toString()
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

    val trips = root.optJSONArray("trips") ?: JSONArray()
    for (i in 0 until trips.length()) {
        val obj = trips.getJSONObject(i)
        val tripName = obj.optString("tripName", "")
        if (tripName.isBlank()) continue

        val trip = ReturnLoadingTripData(tripName = tripName)

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
            woodEstimate = vehicleObj.optString("woodEstimate")
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
    } else {
        loadingAluminumRows.clear()
        loadingIronRows.clear()
        vehicleInfo = VehicleInfo()
    }

    currentLoadingEditType = ReturnLoadingType.ALUMINUM
    currentLoadingEditIndex = if (loadingAluminumRows.isEmpty()) -1 else 0
    currentLoadingField = ReturnLoadingField.MATERIAL_NAME
}

