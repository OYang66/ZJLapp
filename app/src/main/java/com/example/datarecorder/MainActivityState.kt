package com.example.datarecorder

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun MainActivity.getTodayPackageDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

fun MainActivity.getPackageDate(packageName: String): String {
    return packageDateMap[packageName].orEmpty()
}

fun MainActivity.ensurePackageDate(packageName: String) {
    if (packageName.isBlank()) return
    if (packageDateMap[packageName].isNullOrBlank()) {
        packageDateMap[packageName] = getTodayPackageDate()
    }
}

fun MainActivity.updatePackageButtonText() {
    val text = when (currentModeType) {
        ModeType.RETURN_LOADING -> if (currentLoadingTripName.isBlank()) "车次" else currentLoadingTripName
        else -> if (currentPackageName.isBlank()) "包号" else currentPackageName
    }
    btnPackageMenu.text = if (text.length > 6) text.take(6) + "…" else text
}



fun MainActivity.ensurePackageSelected(): Boolean {
    if (currentProjectId <= 0L) {
        toast("请先选择项目")
        return false
    }
    if (currentPackageName.isBlank()) {
        toast("请先点击包号并增加包号，才能输入数据")
        return false
    }
    return true
}

fun MainActivity.clearAllPackageMaps() {
    packageStandardRowsMap.clear()
    packageCurrentStandardRowMap.clear()
    packageFastRowsMap.clear()
    packageCurrentFastRowMap.clear()
    packageDateMap.clear()

    savedStandardRows.clear()
    currentStandardRow = StandardRow()
    savedFastRows.clear()
    currentFastRow = FastRow()

    currentPackageName = ""
    updatePackageButtonText()
    updateBuildingButtonText()
}


fun MainActivity.saveScreenDataToCurrentPackage() {
    if (currentPackageName.isBlank()) return

    packageStandardRowsMap[currentPackageName] =
        savedStandardRows.map { it.copy() }.toMutableList()
    packageCurrentStandardRowMap[currentPackageName] = currentStandardRow.copy()

    packageFastRowsMap[currentPackageName] =
        savedFastRows.map { it.copy() }.toMutableList()
    packageCurrentFastRowMap[currentPackageName] = currentFastRow.copy()
}

fun MainActivity.loadPackageToScreen(packageName: String) {
    currentPackageName = packageName
    updatePackageButtonText()

    savedStandardRows.clear()
    savedStandardRows.addAll(
        packageStandardRowsMap[packageName]?.map { it.copy() } ?: emptyList()
    )
    currentStandardRow = packageCurrentStandardRowMap[packageName]?.copy() ?: StandardRow()
    currentStandardField = StandardField.INSTALL_NO
    lastStandardField = StandardField.INSTALL_NO

    savedFastRows.clear()
    savedFastRows.addAll(
        packageFastRowsMap[packageName]?.map { it.copy() } ?: emptyList()
    )
    currentFastRow = packageCurrentFastRowMap[packageName]?.copy() ?: FastRow()
    currentFastNumericField = FastField.WIDTH
    currentFastActiveField = FastField.WIDTH
    lastFastField = FastField.WIDTH

    clearStandardEditingState()
    clearFastEditingState()
    pendingReplaceStandardEditing = false
    pendingReplaceFastEditing = false
    pendingReplaceCurrentFastModel = false
    pendingReplaceCurrentStandardModel = false

    if (currentModeType == ModeType.STANDARD || currentModeType == ModeType.FAST) {
        updateDisplayTable()
    }
}


fun MainActivity.generateNextPackageName(): String {
    val maxIndex = getAllPackageNamesInOrder()
        .mapNotNull { name ->
            Regex("""第(\d+)包""").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        .maxOrNull() ?: 0
    return "第${maxIndex + 1}包"
}

fun MainActivity.getAllPackageNamesInOrder(): List<String> {
    val names = linkedSetOf<String>()
    names.addAll(packageStandardRowsMap.keys)
    names.addAll(packageFastRowsMap.keys)
    return names.toList()
}
fun MainActivity.updateBuildingButtonText() {
    val text = if (currentBuildingName.isBlank()) "楼栋" else currentBuildingName
    btnBuildingMenu.text = if (text.length > 6) text.take(6) + "…" else text
}


fun MainActivity.getAllBuildingNamesInOrder(): List<String> {
    val names = linkedSetOf<String>()
    if (currentBuildingName.isNotBlank()) names.add(currentBuildingName)
    names.addAll(buildingStandardContentMap.keys)
    names.addAll(buildingFastContentMap.keys)
    names.addAll(buildingLoadingContentMap.keys)
    return names.toList()
}

fun MainActivity.saveCurrentBuildingScopeToMemory() {
    val safeBuildingName = if (currentBuildingName.isBlank()) "1号楼" else currentBuildingName

    if (currentBuildingName.isBlank()) {
        currentBuildingName = safeBuildingName
        updateBuildingButtonText()
    }

    saveScreenDataToCurrentPackage()
    saveLoadingScreenToCurrentTrip()

    buildingStandardContentMap[safeBuildingName] = serializeStandardContent()
    buildingFastContentMap[safeBuildingName] = serializeFastContent()
    buildingLoadingContentMap[safeBuildingName] = serializeLoadingContent()
}


fun MainActivity.loadBuildingScopeToScreen(
    buildingName: String,
    saveCurrentFirst: Boolean = true
) {
    if (saveCurrentFirst) {
        saveCurrentBuildingScopeToMemory()
    }

    currentBuildingName = if (buildingName.isBlank()) "1号楼" else buildingName
    updateBuildingButtonText()

    clearAllPackageMaps()
    loadingTripMap.clear()
    loadingAluminumRows.clear()
    loadingIronRows.clear()
    currentLoadingTripName = ""
    vehicleInfo = VehicleInfo()

    savedStandardRows.clear()
    currentStandardRow = StandardRow()
    savedFastRows.clear()
    currentFastRow = FastRow()

    deserializePackageStandardContent(buildingStandardContentMap[currentBuildingName].orEmpty())
    deserializePackageFastContent(buildingFastContentMap[currentBuildingName].orEmpty())
    deserializeLoadingContent(buildingLoadingContentMap[currentBuildingName].orEmpty())

    val allNames = linkedSetOf<String>()
    allNames.addAll(packageStandardRowsMap.keys)
    allNames.addAll(packageFastRowsMap.keys)
    allNames.addAll(packageCurrentStandardRowMap.keys)
    allNames.addAll(packageCurrentFastRowMap.keys)

    if (allNames.isEmpty()) {
        currentPackageName = ""
        savedStandardRows.clear()
        currentStandardRow = StandardRow()
        savedFastRows.clear()
        currentFastRow = FastRow()
        updatePackageButtonText()
    } else {
        if (currentPackageName.isBlank() || !allNames.contains(currentPackageName)) {
            currentPackageName = allNames.first()
        }
        loadPackageToScreen(currentPackageName)
    }

    if (currentLoadingTripName.isBlank() || !loadingTripMap.containsKey(currentLoadingTripName)) {
        currentLoadingTripName = loadingTripMap.keys.firstOrNull().orEmpty()
    }

    updatePackageButtonText()
    switchMode(currentModeType)
}




fun MainActivity.resetForNewProjectWithoutPackage() {
    clearAllPackageMaps()

    clearStandardEditingState()
    clearFastEditingState()
    pendingReplaceStandardEditing = false
    pendingReplaceFastEditing = false
    pendingReplaceCurrentFastModel = false
    pendingReplaceCurrentStandardModel = false

    savedStandardRows.clear()
    currentStandardRow = StandardRow()
    currentStandardField = StandardField.INSTALL_NO
    lastStandardField = StandardField.INSTALL_NO

    savedFastRows.clear()
    currentFastRow = FastRow()
    currentFastNumericField = FastField.WIDTH
    currentFastActiveField = FastField.WIDTH
    lastFastField = FastField.WIDTH

    updateBuildingButtonText()
    updatePackageButtonText()
    updateDisplayTable()

}
