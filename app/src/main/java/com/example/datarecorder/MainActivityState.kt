package com.example.datarecorder

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================
// 状态保存/恢复相关
// =========================
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
		ModeType.RETURN_LOADING ->
			if (currentLoadingTripName.isBlank()) "车次" else currentLoadingTripName

		ModeType.QUALITY_FEEDBACK ->
			if (currentQualityFloorLabel.isBlank()) "铝模层数" else "铝模${currentQualityFloorLabel}层"

		else ->
			if (currentPackageName.isBlank()) "包号" else currentPackageName
	}
	btnPackageMenu.text = if (text.length > 6) text.take(6) + "…" else text
}


fun MainActivity.serializeQualityFeedbackContent(): String {
	val allRows = mutableListOf<QualityFeedbackRow>()
	allRows.addAll(
		qualityRows.map { row ->
			row.copy(
				photos = row.photos.map { it.copy() }.toMutableList()
			)
		}
	)

	val currentRowCopy = currentQualityRow.copy(
		photos = currentQualityRow.photos.map { it.copy() }.toMutableList()
	)

	val root = JSONObject()
	root.put("floorLabel", currentQualityFloorLabel)
	root.put("editingRowIndex", editingQualityRowIndex ?: -1)
	root.put("editingField", editingQualityField.name)

	val rowsArray = JSONArray()
	allRows.forEach { row ->
		rowsArray.put(serializeQualityRowToJson(row))
	}
	root.put("rows", rowsArray)

	root.put("currentRow", serializeQualityRowToJson(currentRowCopy))

	return root.toString()
}

fun MainActivity.deserializeQualityFeedbackContent(content: String) {
	qualityRows.clear()
	currentQualityRow = QualityFeedbackRow()
	currentQualityFloorLabel = "1"
	editingQualityRowIndex = null
	editingQualityField = QualityFeedbackField.MATERIAL_TYPE

	if (content.isBlank()) return

	try {
		val root = JSONObject(content)

		currentQualityFloorLabel = root.optString("floorLabel", "1").ifBlank { "1" }

		val rowsArray = root.optJSONArray("rows") ?: JSONArray()
		for (i in 0 until rowsArray.length()) {
			val rowObj = rowsArray.optJSONObject(i) ?: continue
			qualityRows.add(deserializeQualityRowFromJson(rowObj))
		}

		val currentRowObj = root.optJSONObject("currentRow")
		if (currentRowObj != null) {
			currentQualityRow = deserializeQualityRowFromJson(currentRowObj)
		}

		val editIndex = root.optInt("editingRowIndex", -1)
		editingQualityRowIndex = if (editIndex >= 0) editIndex else null

		val fieldName = root.optString(
			"editingField",
			QualityFeedbackField.MATERIAL_TYPE.name
		)
		editingQualityField = try {
			QualityFeedbackField.valueOf(fieldName)
		} catch (_: Exception) {
			QualityFeedbackField.MATERIAL_TYPE
		}
	} catch (e: Exception) {
		e.printStackTrace()
		if (currentBuildingName.isNotBlank()) {
			buildingQualityContentMap[currentBuildingName] = ""
		}
	}
}

private fun MainActivity.serializeQualityRowToJson(row: QualityFeedbackRow): JSONObject {
	val obj = JSONObject()
	obj.put("materialType", row.materialType)
	obj.put("installNo", row.installNo)
	obj.put("model", row.model)
	obj.put("qualityType", row.qualityType)
	obj.put("feedbackDesc", row.feedbackDesc)

	val photosArray = JSONArray()
	row.photos.forEach { photo ->
		val photoObj = JSONObject()
		photoObj.put("uriString", photo.uriString)
		photoObj.put("localPath", photo.localPath)
		photoObj.put("createTime", photo.createTime)
		photosArray.put(photoObj)
	}
	obj.put("photos", photosArray)

	return obj
}

private fun MainActivity.deserializeQualityRowFromJson(obj: JSONObject): QualityFeedbackRow {
	val row = QualityFeedbackRow()
	row.materialType = obj.optString("materialType", "")
	row.installNo = obj.optString("installNo", "")
	row.model = obj.optString("model", "")
	row.qualityType = obj.optString("qualityType", "")
	row.feedbackDesc = obj.optString("feedbackDesc", "")

	val photosArray = obj.optJSONArray("photos") ?: JSONArray()
	for (i in 0 until photosArray.length()) {
		val photoObj = photosArray.optJSONObject(i) ?: continue
		row.photos.add(
			QualityFeedbackPhotoItem(
				uriString = photoObj.optString("uriString", ""),
				localPath = photoObj.optString("localPath", ""),
				createTime = photoObj.optLong("createTime", System.currentTimeMillis())
			)
		)
	}

	return row
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
	if (currentModeType == ModeType.FAST) {
		scheduleSubDisplaySnapshotSync()
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


fun MainActivity.ensureDefaultPackageExists() {
	val allNames = linkedSetOf<String>()
	allNames.addAll(packageStandardRowsMap.keys)
	allNames.addAll(packageFastRowsMap.keys)
	allNames.addAll(packageCurrentStandardRowMap.keys)
	allNames.addAll(packageCurrentFastRowMap.keys)

	if (allNames.isEmpty()) {
		val defaultPackageName = "第1包"
		packageStandardRowsMap[defaultPackageName] = mutableListOf()
		packageCurrentStandardRowMap[defaultPackageName] = StandardRow()
		packageFastRowsMap[defaultPackageName] = mutableListOf()
		packageCurrentFastRowMap[defaultPackageName] = FastRow()
		ensurePackageDate(defaultPackageName)
		currentPackageName = defaultPackageName
		loadPackageToScreen(defaultPackageName)
		return
	}

	allNames.forEach { name ->
		packageStandardRowsMap.putIfAbsent(name, mutableListOf())
		packageCurrentStandardRowMap.putIfAbsent(name, StandardRow())
		packageFastRowsMap.putIfAbsent(name, mutableListOf())
		packageCurrentFastRowMap.putIfAbsent(name, FastRow())
		ensurePackageDate(name)
	}

	if (currentPackageName.isBlank() || !allNames.contains(currentPackageName)) {
		currentPackageName = allNames.first()
	}

	loadPackageToScreen(currentPackageName)
}

fun MainActivity.serializeProjectQualityBuildingsContent(): String {
	return wrapBuildingScopedContent(currentBuildingName, buildingQualityContentMap)
}

fun MainActivity.ensureDefaultLoadingTripExists() {
	if (loadingTripMap.isEmpty()) {
		val defaultTripName = "第1车"
		loadingTripMap[defaultTripName] = ReturnLoadingTripData(tripName = defaultTripName)
		currentLoadingTripName = defaultTripName
	}

	if (currentLoadingTripName.isBlank() || !loadingTripMap.containsKey(currentLoadingTripName)) {
		currentLoadingTripName = loadingTripMap.keys.first()
	}

	val trip = loadingTripMap[currentLoadingTripName]
		?: ReturnLoadingTripData(tripName = currentLoadingTripName)

	loadingAluminumRows.clear()
	loadingAluminumRows.addAll(trip.aluminumRows.map { it.copy() })

	loadingIronRows.clear()
	loadingIronRows.addAll(trip.ironRows.map { it.copy() })

	vehicleInfo = trip.vehicleInfo.copy()
	loadingAluminumWeightMode = trip.aluminumWeightMode
	loadingIronWeightMode = trip.ironWeightMode
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
	names.addAll(buildingQualityContentMap.keys)
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
	buildingQualityContentMap[safeBuildingName] = serializeQualityFeedbackContent()

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

	qualityRows.clear()
	currentQualityRow = QualityFeedbackRow()
	currentQualityFloorLabel = "1"
	editingQualityRowIndex = null
	editingQualityField = QualityFeedbackField.MATERIAL_TYPE


	savedStandardRows.clear()
	currentStandardRow = StandardRow()
	savedFastRows.clear()
	currentFastRow = FastRow()

	deserializePackageStandardContent(buildingStandardContentMap[currentBuildingName].orEmpty())
	deserializePackageFastContent(buildingFastContentMap[currentBuildingName].orEmpty())
	deserializeLoadingContent(buildingLoadingContentMap[currentBuildingName].orEmpty())
	deserializeQualityFeedbackContent(buildingQualityContentMap[currentBuildingName].orEmpty())

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
	if (currentModeType == ModeType.FAST) {
		scheduleSubDisplaySnapshotSync()
	}
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



