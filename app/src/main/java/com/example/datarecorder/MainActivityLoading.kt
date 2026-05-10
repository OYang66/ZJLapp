package com.example.datarecorder

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.text.DecimalFormat
import java.util.Locale

// =========================
// 返厂装车
// =========================

fun MainActivity.initLoadingModeArea() {
	val btnAddMaterial = findViewById<Button>(R.id.btnAddIronMaterial)
	val btnVehicleInfo = findViewById<Button>(R.id.btnVehicleInfo)
	val btnAluminum = findViewById<Button>(R.id.btnLoadingAluminum)

	val btnNum0 = findViewById<Button>(R.id.btnLoading0)
	val btnNum1 = findViewById<Button>(R.id.btnLoading1)
	val btnNum2 = findViewById<Button>(R.id.btnLoading2)
	val btnNum3 = findViewById<Button>(R.id.btnLoading3)
	val btnNum4 = findViewById<Button>(R.id.btnLoading4)
	val btnNum5 = findViewById<Button>(R.id.btnLoading5)
	val btnNum6 = findViewById<Button>(R.id.btnLoading6)
	val btnNum7 = findViewById<Button>(R.id.btnLoading7)
	val btnNum8 = findViewById<Button>(R.id.btnLoading8)
	val btnNum9 = findViewById<Button>(R.id.btnLoading9)
	val btnNum00 = findViewById<Button>(R.id.btnLoading00)
	val btnDot = findViewById<Button>(R.id.btnLoadingDot)
	val btnNextCol = findViewById<Button>(R.id.btnLoadingNextColumn)
	val btnNextRow = findViewById<Button>(R.id.btnLoadingNextRow)
	val btnBackspace = findViewById<Button>(R.id.btnLoadingBackspace)

	btnAluminum.setOnClickListener { showAluminumMaterialDialog(it) }
	btnAddMaterial.setOnClickListener { showIronMaterialDialog() }
	btnVehicleInfo.setOnClickListener { showVehicleInfoDialog() }

	val numButtons = listOf(
		btnNum0 to "0", btnNum1 to "1", btnNum2 to "2", btnNum3 to "3", btnNum4 to "4",
		btnNum5 to "5", btnNum6 to "6", btnNum7 to "7", btnNum8 to "8", btnNum9 to "9",
		btnNum00 to "00"
	)

	numButtons.forEach { (btn, value) ->
		btn.setOnClickListener { appendLoadingValue(value) }
	}

	btnDot.setOnClickListener { appendLoadingValue(".") }
	btnNextCol.setOnClickListener { moveLoadingToNextColumn() }
	btnNextRow.setOnClickListener { moveLoadingToNextRow() }
	btnBackspace.setOnClickListener { deleteLastLoadingValue() }

	renderLoadingTable()
}

fun MainActivity.showAluminumMaterialDialog(anchor: View) {
	showMenuCardPopup(
		anchor = anchor,
		title = "选择铝物料",
		subtitle = "点击后新增一条铝模装车记录",
		sections = listOf(
			MenuCardSection(
				items = listOf(
					MenuCardItem("铝模", onClick = { applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "铝模") }, accent = true),
					MenuCardItem("SP", onClick = { applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "SP") }),
					MenuCardItem("铝箱", onClick = { applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "铝箱") })
				)
			)
		)
	)
}

fun MainActivity.resequenceLoadingAluminumPackages() {
	if (loadingAluminumUsePackageCount) return

	loadingAluminumRows.forEachIndexed { index, row ->
		row.packageOrCount = (index + 1).toString()
	}
}


fun MainActivity.deleteLoadingRow(listType: ReturnLoadingType, rowIndex: Int) {
	when (listType) {
		ReturnLoadingType.ALUMINUM -> {
			if (rowIndex !in loadingAluminumRows.indices) return
			loadingAluminumRows.removeAt(rowIndex)
			resequenceLoadingAluminumPackages()

			currentLoadingEditType = ReturnLoadingType.ALUMINUM
			currentLoadingEditIndex = when {
				loadingAluminumRows.isEmpty() -> -1
				rowIndex > loadingAluminumRows.lastIndex -> loadingAluminumRows.lastIndex
				else -> rowIndex
			}
		}

		ReturnLoadingType.IRON -> {
			if (rowIndex !in loadingIronRows.indices) return
			loadingIronRows.removeAt(rowIndex)

			currentLoadingEditType = ReturnLoadingType.IRON
			currentLoadingEditIndex = when {
				loadingIronRows.isEmpty() -> -1
				rowIndex > loadingIronRows.lastIndex -> loadingIronRows.lastIndex
				else -> rowIndex
			}
		}
	}

	currentLoadingField = ReturnLoadingField.MATERIAL_NAME
	saveLoadingScreenToCurrentTrip()
	renderLoadingTable()
}

fun MainActivity.getLoadingAluminumColumnTitle(): String {
	return if (loadingAluminumUsePackageCount) "包数" else "包号"
}

fun MainActivity.showLoadingRowActionMenu(
		anchor: View,
		listType: ReturnLoadingType,
		rowIndex: Int
) {
	showMenuCardPopup(
		anchor = anchor,
		title = "当前行操作",
		subtitle = "长按物料名称可打开操作菜单",
		sections = listOf(
			MenuCardSection(
				items = listOf(
					MenuCardItem("删除当前行", onClick = {
						showConfirmCardDialog(
							title = "删除确认",
							message = "确认删除当前行吗？",
							confirmText = "删除",
							dangerMessage = true
						) {
							deleteLoadingRow(listType, rowIndex)
							triggerAutoSaveDebounced()
						}
					}, danger = true)
				)
			)
		)
	)
}

fun MainActivity.showLoadingPackageModeMenu(anchor: View) {
	showMenuCardPopup(
		anchor = anchor,
		title = "铝模列模式",
		subtitle = if (loadingAluminumUsePackageCount) "当前按包数录入" else "当前按包号录入",
		sections = listOf(
			MenuCardSection(
				items = listOf(
					MenuCardItem(
						"包号",
						onClick = {
							loadingAluminumUsePackageCount = false
							resequenceLoadingAluminumPackages()
							saveLoadingScreenToCurrentTrip()
							renderLoadingTable()
						},
						selected = !loadingAluminumUsePackageCount
					),
					MenuCardItem(
						"包数",
						onClick = {
							loadingAluminumUsePackageCount = true
							saveLoadingScreenToCurrentTrip()
							renderLoadingTable()
						},
						selected = loadingAluminumUsePackageCount
					)
				)
			)
		)
	)
}

fun MainActivity.buildLoadingHeaderRow(): TableRow {
	val headerRow = TableRow(this)

	headerRow.addView(createTableCell("物料名称", true))

	val packageCell = createTableCell(getLoadingAluminumColumnTitle(), true).apply {
		isClickable = true
		isFocusable = true
		setOnClickListener { showLoadingPackageModeMenu(this) }
	}
	headerRow.addView(packageCell)

	headerRow.addView(createTableCell("面积", true))

	val weightCell =
		createTableCell(getLoadingWeightHeaderText(ReturnLoadingType.ALUMINUM), true).apply {

			isClickable = true
			isFocusable = true
			setOnClickListener { showLoadingWeightModeMenu(this, ReturnLoadingType.ALUMINUM) }
		}
	headerRow.addView(weightCell)

	headerRow.addView(createTableCell("备注", true))
	return headerRow
}

@SuppressLint("SetTextI18n")
fun MainActivity.renderLoadingTable() {
	resequenceLoadingAluminumPackages()

	tableHeader.removeAllViews()
	tableBody.removeAllViews()
	loadingTableHeader.removeAllViews()
	loadingTable.removeAllViews()


	tableHeader.addView(buildLoadingHeaderRow())

	fun addEditableLoadingRow(
			target: TableLayout,
			listType: ReturnLoadingType,
			rowIndex: Int,
			rowData: ReturnLoadingRow
	) {
		val row = TableRow(this)

		val materialCell = createTableCell(
			text = rowData.materialName,
			isHeader = false,
			selected = currentLoadingEditType == listType &&
					currentLoadingEditIndex == rowIndex &&
					currentLoadingField == ReturnLoadingField.MATERIAL_NAME,
			onClick = {
				currentLoadingEditType = listType
				currentLoadingEditIndex = rowIndex
				currentLoadingField = ReturnLoadingField.MATERIAL_NAME
				renderLoadingTable()
			}
		).apply {
			isLongClickable = true
			setOnLongClickListener {
				showLoadingRowActionMenu(this, listType, rowIndex)
				true
			}
		}
		row.addView(materialCell)

		row.addView(
			createTableCell(
				text = rowData.packageOrCount,
				isHeader = false,
				selected = currentLoadingEditType == listType &&
						currentLoadingEditIndex == rowIndex &&
						currentLoadingField == ReturnLoadingField.PACKAGE_OR_COUNT,
				onClick = {
					currentLoadingEditType = listType
					currentLoadingEditIndex = rowIndex
					currentLoadingField = ReturnLoadingField.PACKAGE_OR_COUNT
					renderLoadingTable()
				}
			)
		)

		row.addView(
			createTableCell(
				text = rowData.areaOrWeight,
				isHeader = false,
				selected = currentLoadingEditType == listType &&
						currentLoadingEditIndex == rowIndex &&
						currentLoadingField == ReturnLoadingField.AREA_OR_WEIGHT,
				onClick = {
					currentLoadingEditType = listType
					currentLoadingEditIndex = rowIndex
					currentLoadingField = ReturnLoadingField.AREA_OR_WEIGHT
					renderLoadingTable()
				}
			)
		)

		row.addView(
			createTableCell(
				text = getLoadingDisplayedWeight(listType, rowData),
				isHeader = false,
				selected = currentLoadingEditType == listType &&
						currentLoadingEditIndex == rowIndex &&
						currentLoadingField == ReturnLoadingField.WEIGHT,
				onClick = {
					currentLoadingEditType = listType
					currentLoadingEditIndex = rowIndex
					currentLoadingField = ReturnLoadingField.WEIGHT
					renderLoadingTable()
				}
			)
		)


		row.addView(
			createTableCell(
				text = rowData.remark,
				isHeader = false,
				selected = false,
				onClick = null,
				onLongClick = if (listType == ReturnLoadingType.IRON) {
					{
						deductAluminumBoxWeightForRow(rowIndex)
						true
					}
				} else null
			)
		)

		target.addView(row)
	}

	if (loadingAluminumRows.isEmpty()) {
		val row = TableRow(this)
		repeat(5) { row.addView(createTableCell("", false)) }
		tableBody.addView(row)
	} else {
		loadingAluminumRows.forEachIndexed { index, rowData ->
			addEditableLoadingRow(tableBody, ReturnLoadingType.ALUMINUM, index, rowData)
		}
	}


	val aluminumSingleTotal = loadingAluminumRows.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }
	val ironSingleTotal = loadingIronRows.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }

	val aluminumSummaryValue =
		if (loadingAluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
			vehicleInfo.aluminumWeight()
		} else {
			aluminumSingleTotal
		}

	val ironSummaryValue = if (loadingIronWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
		vehicleInfo.ironWeight()
	} else {
		ironSingleTotal
	}

	tvSummaryPrimary.visibility = View.VISIBLE
	tvSummarySecondary.visibility = View.VISIBLE
	tvSummaryPrimary.text = "铝模单包称重合计：${formatLoadingNumber(aluminumSummaryValue)}"
	tvSummarySecondary.text = "铝模过磅重量：${formatLoadingNumber(vehicleInfo.aluminumWeight())}"

	loadingTableHeader.addView(buildIronLoadingHeaderRow())

	if (loadingIronRows.isEmpty()) {
		val row = TableRow(this)
		repeat(5) { row.addView(createTableCell("", false)) }
		loadingTable.addView(row)
	} else {
		loadingIronRows.forEachIndexed { index, rowData ->
			addEditableLoadingRow(loadingTable, ReturnLoadingType.IRON, index, rowData)
		}
	}

	tvLoadingIronWeighbridge.visibility = View.VISIBLE
	tvLoadingIronTotal.visibility = View.VISIBLE
	tvLoadingIronWeighbridge.text = "铁件过磅重量：${formatLoadingNumber(vehicleInfo.ironWeight())}"
	tvLoadingIronTotal.text = "铁件单包称重合计：${formatLoadingNumber(ironSummaryValue)}"


	saveLoadingScreenToCurrentTrip()
	if (currentLoadingTripName.isNotBlank()) {
		loadingHeaderHorizontalScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
			if (loadingBodyHorizontalScroll.scrollX != scrollX) {
				loadingBodyHorizontalScroll.scrollTo(scrollX, 0)
			}
		}

		loadingBodyHorizontalScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
			if (loadingHeaderHorizontalScroll.scrollX != scrollX) {
				loadingHeaderHorizontalScroll.scrollTo(scrollX, 0)
			}
		}

		triggerAutoSaveDebounced()
	}
}


fun MainActivity.showLoadingWeightModeMenu(
		anchor: View,
		type: ReturnLoadingType
) {
	val isAluminum = type == ReturnLoadingType.ALUMINUM
	val currentMode = if (isAluminum) loadingAluminumWeightMode else loadingIronWeightMode
	val title = if (isAluminum) "铝模重量模式" else "铁件重量模式"

	showMenuCardPopup(
		anchor = anchor,
		title = title,
		subtitle = "选择单包重量或过磅总重量",
		sections = listOf(
			MenuCardSection(
				items = listOf(
					MenuCardItem(
						"单包重量",
						onClick = {
							if (isAluminum) {
								loadingAluminumWeightMode = LoadingWeightMode.SINGLE_PACKAGE
							} else {
								loadingIronWeightMode = LoadingWeightMode.SINGLE_PACKAGE
							}
							saveLoadingScreenToCurrentTrip()
							renderLoadingTable()
							triggerAutoSaveDebounced()
						},
						selected = currentMode == LoadingWeightMode.SINGLE_PACKAGE
					),
					MenuCardItem(
						"过磅总重量",
						onClick = {
							if (isAluminum) {
								loadingAluminumWeightMode = LoadingWeightMode.WEIGHBRIDGE_TOTAL
							} else {
								loadingIronWeightMode = LoadingWeightMode.WEIGHBRIDGE_TOTAL
							}
							saveLoadingScreenToCurrentTrip()
							renderLoadingTable()
							triggerAutoSaveDebounced()
						},
						selected = currentMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL
					)
				)
			)
		)
	)
}


fun MainActivity.getLoadingWeightHeaderText(type: ReturnLoadingType): String {
	val mode = when (type) {
		ReturnLoadingType.ALUMINUM -> loadingAluminumWeightMode
		ReturnLoadingType.IRON -> loadingIronWeightMode
	}

	return when (mode) {
		LoadingWeightMode.UNSELECTED -> "选择重量"
		LoadingWeightMode.SINGLE_PACKAGE -> "单包重量"
		LoadingWeightMode.WEIGHBRIDGE_TOTAL -> "过磅总重量"
	}
}

fun MainActivity.ensureLoadingWeightModeSelected(type: ReturnLoadingType): Boolean {
	val mode = when (type) {
		ReturnLoadingType.ALUMINUM -> loadingAluminumWeightMode
		ReturnLoadingType.IRON -> loadingIronWeightMode
	}

	if (mode != LoadingWeightMode.UNSELECTED) return true

	val targetName = if (type == ReturnLoadingType.ALUMINUM) "铝模" else "铁件"
	toast("请先点击${targetName}重量，选择单包重量或过磅总重量")
	return false
}


fun MainActivity.isLoadingWeightEditable(type: ReturnLoadingType): Boolean {
	return when (type) {
		ReturnLoadingType.ALUMINUM -> loadingAluminumWeightMode == LoadingWeightMode.SINGLE_PACKAGE
		ReturnLoadingType.IRON -> loadingIronWeightMode == LoadingWeightMode.SINGLE_PACKAGE
	}
}

fun MainActivity.getLoadingDisplayedWeight(
		type: ReturnLoadingType,
		row: ReturnLoadingRow
): String {
	return if (isLoadingWeightEditable(type)) row.weight else ""
}

fun MainActivity.buildIronLoadingHeaderRow(): TableRow {
	val headerRow = TableRow(this)

	headerRow.addView(createTableCell("物料名称", true))
	headerRow.addView(createTableCell("包数", true))
	headerRow.addView(createTableCell("数量", true))

	val weightCell =
		createTableCell(getLoadingWeightHeaderText(ReturnLoadingType.IRON), true).apply {

			isClickable = true
			isFocusable = true
			setOnClickListener { showLoadingWeightModeMenu(this, ReturnLoadingType.IRON) }
		}
	headerRow.addView(weightCell)

	headerRow.addView(createTableCell("备注", true))
	return headerRow
}

fun MainActivity.addLoadingMaterial(type: ReturnLoadingType, materialName: String) {
	if (!ensureLoadingTripSelected()) return
	if (!ensureLoadingWeightModeSelected(type)) return

	val targetList =
		if (type == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
	val row = ReturnLoadingRow(type = type, materialName = materialName)

	if (type == ReturnLoadingType.ALUMINUM && !loadingAluminumUsePackageCount) {
		row.packageOrCount = generateNextLoadingPackageNo()
	}

	targetList.add(row)

	currentLoadingEditType = type
	currentLoadingEditIndex = targetList.lastIndex
	currentLoadingField = if (type == ReturnLoadingType.ALUMINUM) {
		if (loadingAluminumUsePackageCount) {
			ReturnLoadingField.PACKAGE_OR_COUNT
		} else {
			ReturnLoadingField.AREA_OR_WEIGHT
		}
	} else {
		ReturnLoadingField.PACKAGE_OR_COUNT
	}

	if (!isLoadingWeightEditable(type) && currentLoadingField == ReturnLoadingField.WEIGHT) {
		currentLoadingField = if (type == ReturnLoadingType.ALUMINUM) {
			if (loadingAluminumUsePackageCount) ReturnLoadingField.PACKAGE_OR_COUNT else ReturnLoadingField.AREA_OR_WEIGHT
		} else {
			ReturnLoadingField.PACKAGE_OR_COUNT
		}
	}

	renderLoadingTable()
	triggerAutoSaveDebounced()
	scrollLoadingTablesToBottom()
}



fun MainActivity.deductAluminumBoxWeightForRow(rowIndex: Int) {
	val row = loadingIronRows.getOrNull(rowIndex) ?: run {
		toast("请先输入铁件名称和重量")
		return
	}

	if (row.materialName.isBlank() || row.weight.isBlank()) {
		toast("请先输入铁件名称和重量")
		return
	}

	NumberInputDialogHelper(
		context = this,
		titleText = "请输入铝箱重量",
		initialValue = "",
		allowDecimal = true
	) { input ->
		val value = input.toDoubleOrNull() ?: return@NumberInputDialogHelper
		val origin = row.weight.toDoubleOrNull() ?: 0.0
		row.weight = formatLoadingNumber(origin - value)
		row.remark = "已扣除铝箱重量"

		loadingAluminumRows.add(
			ReturnLoadingRow(
				type = ReturnLoadingType.ALUMINUM,
				materialName = "铝箱",
				weight = formatLoadingNumber(value)
			)
		)

		currentLoadingEditType = ReturnLoadingType.IRON
		currentLoadingEditIndex = rowIndex
		currentLoadingField = ReturnLoadingField.REMARK

		renderLoadingTable()
		triggerAutoSaveDebounced()
	}.show()
}

fun MainActivity.deductAluminumBoxWeight() {
	if (currentLoadingEditType != ReturnLoadingType.IRON) {
		toast("请先创建铁件信息")
		return
	}

	val row = loadingIronRows.getOrNull(currentLoadingEditIndex) ?: run {
		toast("请先输入铁件名称和重量")
		return
	}

	if (row.materialName.isBlank() || row.weight.isBlank()) {
		toast("请先输入铁件名称和重量")
		return
	}

	NumberInputDialogHelper(
		context = this,
		titleText = "请输入铝箱重量",
		initialValue = "",
		allowDecimal = true
	) { input ->
		val value = input.toDoubleOrNull() ?: return@NumberInputDialogHelper
		val origin = row.weight.toDoubleOrNull() ?: 0.0
		row.weight = formatLoadingNumber(origin - value)
		row.remark = "已扣除铝箱重量"

		loadingAluminumRows.add(
			ReturnLoadingRow(
				type = ReturnLoadingType.ALUMINUM,
				materialName = "铝箱",
				weight = formatLoadingNumber(value)
			)
		)

		currentLoadingEditType = ReturnLoadingType.IRON
		currentLoadingEditIndex = loadingIronRows.lastIndex
		currentLoadingField = ReturnLoadingField.WEIGHT

		renderLoadingTable()
		triggerAutoSaveDebounced()

	}.show()
}

fun MainActivity.showVehiclePlateInputDialog(initialValue: String, onConfirm: (String) -> Unit) {
	val input = EditText(this).apply {
		setText(initialValue)
		setSelection(text.length)
		hint = "请输入运输车牌号"
		isSingleLine = true
		setPadding(dp(12), dp(10), dp(12), dp(10))
		background = createCellBackground(0xFFF8F5FF.toInt(), 0xFFE4DAFF.toInt(), 1, 12f)
	}

	createCardDialog(
		title = "运输车牌号",
		subtitle = "填写后会立即更新当前车次信息"
	) { dlg ->
		addView(createDialogSectionTitle("车牌号码"))
		addView(input)
		addView(
			createDialogActionRow(
				dialog = dlg,
				confirmText = "确定",
				onConfirm = {
					onConfirm(input.text.toString().trim())
					dlg.dismiss()
				}
			),
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = dp(16)
			}
		)
	}.show()
}

fun MainActivity.showLoadingDatePickerDialog(initialValue: String, onConfirm: (String) -> Unit) {
	val now = java.util.Calendar.getInstance()

	val initialYear: Int
	val initialMonth: Int
	val initialDay: Int

	val parts = initialValue.split("-")
	if (parts.size == 3) {
		initialYear = parts[0].toIntOrNull() ?: now.get(java.util.Calendar.YEAR)
		initialMonth = parts[1].toIntOrNull() ?: (now.get(java.util.Calendar.MONTH) + 1)
		initialDay = parts[2].toIntOrNull() ?: now.get(java.util.Calendar.DAY_OF_MONTH)
	} else {
		initialYear = now.get(java.util.Calendar.YEAR)
		initialMonth = now.get(java.util.Calendar.MONTH) + 1
		initialDay = now.get(java.util.Calendar.DAY_OF_MONTH)
	}

	fun buildPicker(min: Int, max: Int, value: Int): NumberPicker {
		return NumberPicker(this).apply {
			minValue = min
			maxValue = max
			this.value = value.coerceIn(min, max)
			descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
			wrapSelectorWheel = false
			setBackgroundColor(0xFFF8F5FF.toInt())
		}
	}

	val yearPicker = buildPicker(2020, 2100, initialYear)
	val monthPicker = buildPicker(1, 12, initialMonth)
	val dayPicker = buildPicker(1, 31, initialDay)

	val pickerRow = LinearLayout(this).apply {
		orientation = LinearLayout.HORIZONTAL
		gravity = Gravity.CENTER
		setPadding(dp(8), dp(8), dp(8), dp(8))
		background = createCellBackground(0xFFF8F5FF.toInt(), 0xFFE4DAFF.toInt(), 1, 12f)
		addView(yearPicker)
		addView(TextView(this@showLoadingDatePickerDialog).apply {
			text = "年"
			setPadding(dp(4), 0, dp(8), 0)
			setTextColor(0xFF6C56B3.toInt())
		})
		addView(monthPicker)
		addView(TextView(this@showLoadingDatePickerDialog).apply {
			text = "月"
			setPadding(dp(4), 0, dp(8), 0)
			setTextColor(0xFF6C56B3.toInt())
		})
		addView(dayPicker)
		addView(TextView(this@showLoadingDatePickerDialog).apply {
			text = "日"
			setPadding(dp(4), 0, 0, 0)
			setTextColor(0xFF6C56B3.toInt())
		})
	}

	createCardDialog(
		title = "装车时间",
		subtitle = "选择后会立即更新当前车次信息"
	) { dlg ->
		addView(createDialogSectionTitle("日期选择"))
		addView(pickerRow)
		addView(
			createDialogActionRow(
				dialog = dlg,
				confirmText = "确定",
				onConfirm = {
					val date = String.format(
						Locale.getDefault(),
						"%04d-%02d-%02d",
						yearPicker.value,
						monthPicker.value,
						dayPicker.value
					)
					onConfirm(date)
					dlg.dismiss()
				}
			),
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = dp(16)
			}
		)
	}.show()
}



fun MainActivity.appendLoadingValue(value: String) {
	val row = getCurrentLoadingRow() ?: return

	when (currentLoadingField) {
		ReturnLoadingField.MATERIAL_NAME -> return
		ReturnLoadingField.PACKAGE_OR_COUNT -> row.packageOrCount += value
		ReturnLoadingField.AREA_OR_WEIGHT -> row.areaOrWeight += value
		ReturnLoadingField.WEIGHT -> {
			if (!isLoadingWeightEditable(currentLoadingEditType)) return
			row.weight += value
		}

		ReturnLoadingField.REMARK -> return
	}

	updateDisplayTable()
	triggerAutoSaveDebounced()
}


fun MainActivity.moveLoadingToNextColumn() {
	currentLoadingField = when (currentLoadingField) {
		ReturnLoadingField.MATERIAL_NAME -> ReturnLoadingField.PACKAGE_OR_COUNT
		ReturnLoadingField.PACKAGE_OR_COUNT -> ReturnLoadingField.AREA_OR_WEIGHT
		ReturnLoadingField.AREA_OR_WEIGHT -> ReturnLoadingField.WEIGHT
		ReturnLoadingField.WEIGHT -> ReturnLoadingField.REMARK
		ReturnLoadingField.REMARK -> ReturnLoadingField.MATERIAL_NAME
	}
	updateDisplayTable()
	triggerAutoSaveDebounced()
}

fun MainActivity.moveLoadingToNextRow() {
	if (!ensureLoadingTripSelected()) return

	val list = if (currentLoadingEditType == ReturnLoadingType.ALUMINUM) {
		loadingAluminumRows
	} else {
		loadingIronRows
	}

	if (list.isEmpty()) {
		toast("请先通过铝物料或铁物料新增数据")
		return
	}

	val nextIndex = currentLoadingEditIndex + 1
	if (nextIndex in list.indices) {
		val targetField = currentLoadingField
		currentLoadingEditIndex = nextIndex
		currentLoadingField = targetField
		updateDisplayTable()
		triggerAutoSaveDebounced()
	} else {
		toast("请通过铝物料或铁物料新增数据")
	}
}


fun MainActivity.deleteLastLoadingValue() {
	val row = getCurrentLoadingRow() ?: return

	when (currentLoadingField) {
		ReturnLoadingField.MATERIAL_NAME -> return
		ReturnLoadingField.PACKAGE_OR_COUNT -> if (row.packageOrCount.isNotEmpty()) row.packageOrCount =
			row.packageOrCount.dropLast(1)

		ReturnLoadingField.AREA_OR_WEIGHT -> if (row.areaOrWeight.isNotEmpty()) row.areaOrWeight =
			row.areaOrWeight.dropLast(1)

		ReturnLoadingField.WEIGHT -> {
			if (!isLoadingWeightEditable(currentLoadingEditType)) return
			if (row.weight.isNotEmpty()) row.weight = row.weight.dropLast(1)
		}

		ReturnLoadingField.REMARK -> return
	}

	updateDisplayTable()
	triggerAutoSaveDebounced()
}


fun MainActivity.getCurrentLoadingRow(): ReturnLoadingRow? {
	val list =
		if (currentLoadingEditType == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
	return list.getOrNull(currentLoadingEditIndex)
}

fun MainActivity.ensureLoadingTripSelected(): Boolean {
	if (currentProjectId <= 0L) {
		toast("请先选择项目")
		return false
	}
	if (currentLoadingTripName.isBlank()) {
		toast("请先增加车次")
		return false
	}
	return true
}

fun MainActivity.formatLoadingNumber(value: Double): String {
	return DecimalFormat("0.##").format(value)
}

