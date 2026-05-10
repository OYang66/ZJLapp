package com.example.datarecorder

import android.view.View
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog

// =========================
// 型号统计
// =========================

fun MainActivity.handleStandardTokenInput(token: String) {
	if (!ensurePackageSelected()) return

	if (hasStandardEditingTarget()) {
		appendTextToEditingStandardCell(token)
		return
	}

	appendStandardText(token)
}

fun MainActivity.appendStandardText(text: String) {
	when (currentStandardField) {
		StandardField.INSTALL_NO -> {
			if (!canAppendToInstallNo(text)) {
				toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
				return
			}
			currentStandardRow.installNo += text
			lastStandardField = StandardField.INSTALL_NO
		}

		StandardField.MODEL -> {
			currentStandardRow.model = appendModelToken(currentStandardRow.model, text)
			lastStandardField = StandardField.MODEL
		}

		StandardField.QUANTITY -> {
			if (text.all { it.isDigit() }) {
				currentStandardRow.quantity += text
				lastStandardField = StandardField.QUANTITY
			} else {
				if (containsLetters(text)) {
					toast("数量内不能输入字母")
				}
				return
			}
		}
	}

	refreshStandardVisibleCellsOnly()
	triggerAutoSaveDebounced()

}

fun MainActivity.appendStandardToLastField(text: String) {
	if (!ensurePackageSelected()) return

	if (hasStandardEditingTarget()) {
		appendTextToEditingStandardCell(text)
		return
	}

	when (lastStandardField) {
		StandardField.INSTALL_NO -> {
			if (!canAppendToInstallNo(text)) {
				toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
				return
			}
			currentStandardRow.installNo += text
		}

		StandardField.MODEL -> {
			currentStandardRow.model += text
		}

		StandardField.QUANTITY -> {
			if (text.all { it.isDigit() }) {
				currentStandardRow.quantity += text
			} else {
				if (containsLetters(text)) {
					toast("数量内不能输入字母")
				}
				return
			}
		}
	}

	refreshStandardVisibleCellsOnly()
	triggerAutoSaveDebounced()

}


fun MainActivity.canAppendToInstallNo(text: String): Boolean {
	return text.all { it.isDigit() } || text in allowedInstallNoTokens
}


fun MainActivity.moveToNextStandardColumn() {
	if (!ensurePackageSelected()) return

	if (moveEditingStandardCellToNextColumn()) {
		triggerAutoSaveDebounced()
		return
	}

	val oldField = currentStandardField

	currentStandardField = when (currentStandardField) {
		StandardField.INSTALL_NO -> StandardField.MODEL
		StandardField.MODEL -> StandardField.QUANTITY
		StandardField.QUANTITY -> StandardField.INSTALL_NO
	}
	lastStandardField = currentStandardField

	if (oldField != currentStandardField) {
		refreshStandardSelectionOnly(
			oldSavedRowIndex = null,
			oldWasCurrentRow = true,
			newSavedRowIndex = null,
			newWasCurrentRow = true
		)
	}

	triggerAutoSaveDebounced()
}


fun MainActivity.finishCurrentStandardRow() {
	if (!ensurePackageSelected()) return

	if (editingStandardRowIndex != null) {
		val oldRowIndex = editingStandardRowIndex
		clearStandardEditingState()
		if (oldRowIndex != null) {
			rebuildStandardRowOnly(oldRowIndex, false)
		}
		triggerAutoSaveDebounced()
		return
	}

	if (!currentStandardRow.isEmpty()) {
		val savedIndex = savedStandardRows.size
		savedStandardRows.add(currentStandardRow.copy())

		// 把原来的“当前行”改造成“已保存行”，不能再按 current row 重建
		rebuildStandardRowOnly(savedIndex, false)

		currentStandardRow = StandardRow()
		currentStandardField = StandardField.INSTALL_NO
		lastStandardField = StandardField.INSTALL_NO

		addStandardDataRow(
			displayIndex = savedStandardRows.size + 1,
			data = currentStandardRow.copy(),
			isCurrentRow = true,
			savedRowIndex = null
		)

		tvSummaryPrimary.visibility = View.VISIBLE
		tvSummarySecondary.visibility = View.GONE
		tvSummaryPrimary.text = "合计数量：${calculateStandardTotalQty()}"

		bodyVerticalScroll.post {
			bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
		}

		triggerAutoSaveDebounced()
		return
	}

	currentStandardField = StandardField.INSTALL_NO
	lastStandardField = StandardField.INSTALL_NO
	refreshStandardSelectionOnly(null, true, null, true)
	triggerAutoSaveDebounced()
}


fun MainActivity.deleteLastStandardInput() {
	if (!ensurePackageSelected()) return
	if (deleteFromEditingStandardCell()) return

	fun MainActivity.cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

	when (currentStandardField) {
		StandardField.INSTALL_NO -> {
			if (currentStandardRow.installNo.isNotEmpty()) {
				currentStandardRow.installNo = cutLast(currentStandardRow.installNo)
				lastStandardField = StandardField.INSTALL_NO
			}
		}

		StandardField.MODEL -> {
			if (currentStandardRow.model.isNotEmpty()) {
				currentStandardRow.model = cutLast(currentStandardRow.model)
				lastStandardField = StandardField.MODEL
			}
		}

		StandardField.QUANTITY -> {
			if (currentStandardRow.quantity.isNotEmpty()) {
				currentStandardRow.quantity = cutLast(currentStandardRow.quantity)
				lastStandardField = StandardField.QUANTITY
			}
		}
	}

	refreshStandardVisibleCellsOnly()
	triggerAutoSaveDebounced()
}


fun MainActivity.serializeStandardContent(): String {
	saveScreenDataToCurrentPackage()

	val allPackageNames = linkedSetOf<String>()
	allPackageNames.addAll(packageStandardRowsMap.keys)
	allPackageNames.addAll(packageCurrentStandardRowMap.keys)

	if (currentPackageName.isNotBlank()) {
		allPackageNames.add(currentPackageName)
	}

	if (allPackageNames.isEmpty()) return ""

	val packageBlocks = StringBuilder()

	allPackageNames.forEach { packageName ->
		val rows = packageStandardRowsMap[packageName] ?: mutableListOf()
		val current = packageCurrentStandardRowMap[packageName] ?: StandardRow()

		val hasSavedRows = rows.isNotEmpty()
		val hasCurrentRow = !current.isEmpty()

		if (!hasSavedRows && !hasCurrentRow) {
			return@forEach
		}

		packageBlocks.append("#PACKAGE=").append(packageName).append("\n")

		val packageDate = packageDateMap[packageName].orEmpty()
		packageBlocks.append("#PACKAGE_DATE=").append(packageDate).append("\n")

		packageBlocks.append("#ROWS").append("\n")
		rows.forEach {
			packageBlocks.append(
				listOf(it.installNo, it.model, it.quantity).joinToString("\t")
			).append("\n")
		}

		packageBlocks.append("#CURRENT_ROW").append("\n")
		packageBlocks.append(
			listOf(current.installNo, current.model, current.quantity).joinToString("\t")
		).append("\n")

		packageBlocks.append("#END_PACKAGE").append("\n")
	}

	if (packageBlocks.isEmpty()) return ""

	return buildString {
		append("#CURRENT_PACKAGE=").append(currentPackageName).append("\n")
		append(packageBlocks)
	}
}

fun MainActivity.deserializePackageStandardContent(content: String) {
	packageStandardRowsMap.clear()
	packageCurrentStandardRowMap.clear()

	if (content.isBlank()) return

	if (!content.contains("#PACKAGE=")) {
		if (content.trim().startsWith("#")) {
			return
		}

		val oldRows = deserializeStandardContentOld(content)
		if (oldRows.isNotEmpty()) {
			val defaultPackage = "第1包"
			packageStandardRowsMap[defaultPackage] = oldRows.toMutableList()
			packageCurrentStandardRowMap[defaultPackage] = StandardRow()
			packageDateMap[defaultPackage] = getTodayPackageDate()
			currentPackageName = defaultPackage
		}
		return
	}


	val lines = content.replace("\r\n", "\n").split("\n")
	var packageName = ""
	var packageDate = ""
	var inRows = false
	var inCurrentRow = false
	var rows = mutableListOf<StandardRow>()

	lines.forEach { line ->
		when {
			line.startsWith("#CURRENT_PACKAGE=") -> {
				currentPackageName = line.removePrefix("#CURRENT_PACKAGE=").trim()
			}

			line.startsWith("#PACKAGE=") -> {
				if (packageName.isNotBlank()) {
					packageStandardRowsMap[packageName] = rows
					packageCurrentStandardRowMap.putIfAbsent(packageName, StandardRow())
					packageDateMap[packageName] =
						if (packageDate.isBlank()) getTodayPackageDate() else packageDate
				}

				packageName = line.removePrefix("#PACKAGE=").trim()
				packageDate = ""
				rows = mutableListOf()
				inRows = false
				inCurrentRow = false
			}

			line.startsWith("#PACKAGE_DATE=") -> {
				packageDate = line.removePrefix("#PACKAGE_DATE=").trim()
			}

			line == "#ROWS" -> {
				inRows = true
				inCurrentRow = false
			}

			line == "#CURRENT_ROW" -> {
				inRows = false
				inCurrentRow = true
			}

			line == "#END_PACKAGE" -> {
				if (packageName.isNotBlank()) {
					packageStandardRowsMap[packageName] = rows
					packageCurrentStandardRowMap.putIfAbsent(packageName, StandardRow())
					packageDateMap[packageName] =
						if (packageDate.isBlank()) getTodayPackageDate() else packageDate
				}
				packageName = ""
				packageDate = ""
				inRows = false
				inCurrentRow = false
			}

			inRows && line.isNotBlank() -> {
				val parts = line.split("\t")
				rows.add(
					StandardRow(
						installNo = parts.getOrNull(0).orEmpty(),
						model = parts.getOrNull(1).orEmpty(),
						quantity = parts.getOrNull(2).orEmpty()
					)
				)
			}

			inCurrentRow -> {
				val parts = line.split("\t")
				packageCurrentStandardRowMap[packageName] = StandardRow(
					installNo = parts.getOrNull(0).orEmpty(),
					model = parts.getOrNull(1).orEmpty(),
					quantity = parts.getOrNull(2).orEmpty()
				)
				inCurrentRow = false
			}
		}
	}

	if (packageName.isNotBlank()) {
		packageStandardRowsMap[packageName] = rows
		packageCurrentStandardRowMap.putIfAbsent(packageName, StandardRow())
		packageDateMap[packageName] =
			if (packageDate.isBlank()) getTodayPackageDate() else packageDate
	}

	if (currentPackageName.isBlank()) {
		currentPackageName = packageStandardRowsMap.keys.firstOrNull().orEmpty()
	}
}


fun MainActivity.deserializeStandardContentOld(content: String): List<StandardRow> {
	if (content.isBlank()) return emptyList()

	return content.replace("\r\n", "\n")
		.split("\n")
		.mapNotNull { line ->
			if (line.isBlank()) return@mapNotNull null
			val parts = line.split("\t")
			StandardRow(
				installNo = parts.getOrNull(0).orEmpty(),
				model = parts.getOrNull(1).orEmpty(),
				quantity = parts.getOrNull(2).orEmpty()
			)
		}
}

fun MainActivity.renderStandardTable() {
	clearStandardCellRefs()

	addTableHeader("序号", "安装编号", "型号", "数量")

	if (savedStandardRows.isEmpty() && currentStandardRow.isEmpty()) {
		addStandardDataRow(
			displayIndex = 1,
			data = StandardRow(),
			isCurrentRow = true,
			savedRowIndex = null
		)
		return
	}

	savedStandardRows.forEachIndexed { index, row ->
		addStandardDataRow(
			displayIndex = index + 1,
			data = row,
			isCurrentRow = false,
			savedRowIndex = index
		)
	}

	addStandardDataRow(
		displayIndex = savedStandardRows.size + 1,
		data = currentStandardRow.copy(),
		isCurrentRow = true,
		savedRowIndex = null
	)
}

fun MainActivity.buildStandardDataRowView(
		displayIndex: Int,
		data: StandardRow,
		isCurrentRow: Boolean,
		savedRowIndex: Int? = null
): TableRow {
	val row = TableRow(this)

	row.addView(
		createTableCell(
			text = displayIndex.toString(),
			isHeader = false,
			highlight = isCurrentRow,
			onLongClick = {
				showStandardRowDeleteOptions(savedRowIndex, isCurrentRow)
				true
			}
		)
	)

	val installCell = createTableCell(
		text = data.installNo,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.INSTALL_NO
		} else {
			editingStandardRowIndex == null && currentStandardField == StandardField.INSTALL_NO
		},
		onClick = {
			if (savedRowIndex != null) {
				selectStandardSavedCell(savedRowIndex, StandardField.INSTALL_NO)
			} else {
				val oldField = currentStandardField
				clearStandardEditingState()
				pendingReplaceStandardEditing = false
				currentStandardField = StandardField.INSTALL_NO
				lastStandardField = StandardField.INSTALL_NO
				if (oldField != currentStandardField) {
					refreshStandardSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(installCell)

	val modelCell = createTableCell(
		text = data.model,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.MODEL
		} else {
			editingStandardRowIndex == null && currentStandardField == StandardField.MODEL
		},
		onClick = {
			if (savedRowIndex != null) {
				selectStandardSavedCell(savedRowIndex, StandardField.MODEL)
			} else {
				val oldField = currentStandardField
				clearStandardEditingState()
				pendingReplaceStandardEditing = false
				currentStandardField = StandardField.MODEL
				lastStandardField = StandardField.MODEL
				if (oldField != currentStandardField) {
					refreshStandardSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(modelCell)

	val quantityCell = createTableCell(
		text = data.quantity,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.QUANTITY
		} else {
			editingStandardRowIndex == null && currentStandardField == StandardField.QUANTITY
		},
		onClick = {
			if (savedRowIndex != null) {
				selectStandardSavedCell(savedRowIndex, StandardField.QUANTITY)
			} else {
				val oldField = currentStandardField
				clearStandardEditingState()
				pendingReplaceStandardEditing = false
				currentStandardField = StandardField.QUANTITY
				lastStandardField = StandardField.QUANTITY
				if (oldField != currentStandardField) {
					refreshStandardSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(quantityCell)

	if (savedRowIndex != null) {
		standardInstallCellMap[savedRowIndex] = installCell
		standardModelCellMap[savedRowIndex] = modelCell
		standardQuantityCellMap[savedRowIndex] = quantityCell
		standardRowViewMap[savedRowIndex] = row
	} else {
		currentStandardInstallCell = installCell
		currentStandardModelCell = modelCell
		currentStandardQuantityCell = quantityCell
		currentStandardRowView = row
	}

	return row
}

fun MainActivity.addStandardDataRow(
		displayIndex: Int,
		data: StandardRow,
		isCurrentRow: Boolean,
		savedRowIndex: Int? = null
) {
	tableBody.addView(
		buildStandardDataRowView(
			displayIndex = displayIndex,
			data = data,
			isCurrentRow = isCurrentRow,
			savedRowIndex = savedRowIndex
		)
	)
}

fun MainActivity.clearStandardCellRefs() {
	standardInstallCellMap.clear()
	standardModelCellMap.clear()
	standardQuantityCellMap.clear()
	currentStandardInstallCell = null
	currentStandardModelCell = null
	currentStandardQuantityCell = null
	standardRowViewMap.clear()
	currentStandardRowView = null
}


fun MainActivity.refreshStandardVisibleCellsOnly() {
	if (currentModeType != ModeType.STANDARD) return

	if (editingStandardRowIndex != null) {
		val rowIndex = editingStandardRowIndex ?: return
		val row = savedStandardRows.getOrNull(rowIndex) ?: return
		standardInstallCellMap[rowIndex]?.text = row.installNo
		standardModelCellMap[rowIndex]?.text = row.model
		standardQuantityCellMap[rowIndex]?.text = row.quantity
	} else {
		currentStandardInstallCell?.text = currentStandardRow.installNo
		currentStandardModelCell?.text = currentStandardRow.model
		currentStandardQuantityCell?.text = currentStandardRow.quantity
	}

	tvSummaryPrimary.visibility = View.VISIBLE
	tvSummarySecondary.visibility = View.GONE
	tvSummaryPrimary.text = "合计数量：${calculateStandardTotalQty()}"
}


fun MainActivity.refreshStandardSelectionOnly(
		oldSavedRowIndex: Int?,
		oldWasCurrentRow: Boolean,
		newSavedRowIndex: Int?,
		newWasCurrentRow: Boolean
) {
	if (currentModeType != ModeType.STANDARD) return
	rebuildStandardRowOnly(oldSavedRowIndex, oldWasCurrentRow)
	if (oldSavedRowIndex != newSavedRowIndex || oldWasCurrentRow != newWasCurrentRow) {
		rebuildStandardRowOnly(newSavedRowIndex, newWasCurrentRow)
	}
}

fun MainActivity.rebuildStandardRowOnly(savedRowIndex: Int?, isCurrentRow: Boolean) {
	if (currentModeType != ModeType.STANDARD) return

	val displayIndex = if (savedRowIndex != null) savedRowIndex + 1 else savedStandardRows.size + 1
	val data = if (savedRowIndex != null) {
		savedStandardRows.getOrNull(savedRowIndex)?.copy() ?: return
	} else {
		currentStandardRow.copy()
	}

	val newRow = buildStandardDataRowView(
		displayIndex = displayIndex,
		data = data,
		isCurrentRow = isCurrentRow,
		savedRowIndex = savedRowIndex
	)

	val targetIndex = if (savedRowIndex != null) savedRowIndex else tableBody.childCount - 1
	if (targetIndex < 0 || targetIndex >= tableBody.childCount) return

	tableBody.removeViewAt(targetIndex)
	tableBody.addView(newRow, targetIndex)
}

fun MainActivity.moveEditingStandardCellToNextColumn(): Boolean {
	val oldRowIndex = editingStandardRowIndex ?: return false
	val oldField = editingStandardField ?: return false

	editingStandardField = when (oldField) {
		StandardField.INSTALL_NO -> StandardField.MODEL
		StandardField.MODEL -> StandardField.QUANTITY
		StandardField.QUANTITY -> StandardField.INSTALL_NO
	}

	currentStandardField = editingStandardField!!
	lastStandardField = editingStandardField!!

	refreshStandardSelectionOnly(
		oldSavedRowIndex = oldRowIndex,
		oldWasCurrentRow = false,
		newSavedRowIndex = oldRowIndex,
		newWasCurrentRow = false
	)
	return true
}


fun MainActivity.appendTextToEditingStandardCell(text: String): Boolean {
	val rowIndex = editingStandardRowIndex ?: return false
	val field = editingStandardField ?: return false
	val row = savedStandardRows.getOrNull(rowIndex) ?: return false

	when (field) {
		StandardField.INSTALL_NO -> {
			if (!canAppendToInstallNo(text)) {
				toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
				return true
			}
			row.installNo = if (pendingReplaceStandardEditing) text else row.installNo + text
		}

		StandardField.MODEL -> {
			row.model = if (pendingReplaceStandardEditing) {
				appendModelToken("", text)
			} else {
				appendModelToken(row.model, text)
			}
		}

		StandardField.QUANTITY -> {
			if (!text.all { it.isDigit() }) {
				if (containsLetters(text)) {
					toast("数量内不能输入字母")
				}
				return true
			}
			row.quantity = if (pendingReplaceStandardEditing) text else row.quantity + text
		}
	}

	pendingReplaceStandardEditing = false
	lastStandardField = field
	refreshStandardVisibleCellsOnly()
	triggerAutoSaveDebounced()
	return true

}

fun MainActivity.deleteFromEditingStandardCell(): Boolean {
	val rowIndex = editingStandardRowIndex ?: return false
	val field = editingStandardField ?: return false
	val row = savedStandardRows.getOrNull(rowIndex) ?: return false

	fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

	when (field) {
		StandardField.INSTALL_NO -> row.installNo = cutLast(row.installNo)
		StandardField.MODEL -> row.model = cutLast(row.model)
		StandardField.QUANTITY -> row.quantity = cutLast(row.quantity)
	}

	pendingReplaceStandardEditing = false
	lastStandardField = field
	refreshStandardVisibleCellsOnly()
	triggerAutoSaveDebounced()
	return true

}


fun MainActivity.selectStandardSavedCell(rowIndex: Int, field: StandardField) {
	if (rowIndex !in savedStandardRows.indices) return
	editingStandardRowIndex = rowIndex
	editingStandardField = field
	currentStandardField = field
	lastStandardField = field
	pendingReplaceStandardEditing = true
	updateDisplayTable()
}

// =========================
// 删除行
// =========================

private fun MainActivity.showStandardRowDeleteOptions(savedRowIndex: Int?, isCurrentRow: Boolean) {
	if (savedRowIndex == null && (!isCurrentRow || currentStandardRow.isEmpty())) return

	confirmDeleteStandardRow(savedRowIndex, isCurrentRow)
}


fun MainActivity.guessLastStandardField(row: StandardRow): StandardField {
	return when {
		row.quantity.isNotEmpty() -> StandardField.QUANTITY
		row.model.isNotEmpty() -> StandardField.MODEL
		row.installNo.isNotEmpty() -> StandardField.INSTALL_NO
		else -> StandardField.INSTALL_NO
	}
}


fun MainActivity.hasStandardEditingTarget(): Boolean {
	return editingStandardRowIndex != null && editingStandardField != null
}


// =========================
// 编辑状态
// =========================

fun MainActivity.clearStandardEditingState() {
	editingStandardRowIndex = null
	editingStandardField = null
	pendingReplaceStandardEditing = false
}


fun MainActivity.calculateStandardTotalQty(): Int {
	val rows = mutableListOf<StandardRow>()
	rows.addAll(savedStandardRows)
	if (!currentStandardRow.isEmpty()) rows.add(currentStandardRow.copy())
	return rows.sumOf { it.quantity.toIntOrNull() ?: 1 }
}
