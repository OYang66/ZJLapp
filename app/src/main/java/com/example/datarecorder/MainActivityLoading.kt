package com.example.datarecorder

import android.annotation.SuppressLint
import android.view.Gravity

import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import android.view.View
import android.graphics.Typeface
import android.widget.EditText
import android.widget.NumberPicker
import java.util.Locale



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
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "铝模")
    popup.menu.add(0, 2, 1, "SP")
    popup.menu.add(0, 3, 2, "铝箱")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "铝模")
                true
            }
            2 -> {
                applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "SP")
                true
            }
            3 -> {
                applyLoadingMaterialSelection(ReturnLoadingType.ALUMINUM, "铝箱")
                true
            }
            else -> false
        }
    }
    popup.show()
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

fun MainActivity.showLoadingRowActionMenu(anchor: View, listType: ReturnLoadingType, rowIndex: Int) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "删除")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                AlertDialog.Builder(this)
                    .setTitle("删除确认")
                    .setMessage("确认删除当前行吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        deleteLoadingRow(listType, rowIndex)
                        triggerAutoSave()
                    }
                    .show()
                true
            }
            else -> false
        }
    }

    popup.show()
}

fun MainActivity.showLoadingPackageModeMenu(anchor: View) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "包号")
    popup.menu.add(0, 2, 1, "包数")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                loadingAluminumUsePackageCount = false
                resequenceLoadingAluminumPackages()
                saveLoadingScreenToCurrentTrip()
                renderLoadingTable()
                true
            }
            2 -> {
                loadingAluminumUsePackageCount = true
                saveLoadingScreenToCurrentTrip()
                renderLoadingTable()
                true
            }
            else -> false
        }
    }

    popup.show()
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

    val weightCell = createTableCell(getLoadingWeightHeaderText(ReturnLoadingType.ALUMINUM), true).apply {

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

    val aluminumSummaryValue = if (loadingAluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
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

        triggerAutoSave()
    }
}


fun MainActivity.showLoadingWeightModeMenu(
    anchor: View,
    type: ReturnLoadingType
) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "单包重量")
    popup.menu.add(0, 2, 1, "过磅总重量")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                if (type == ReturnLoadingType.ALUMINUM) {
                    loadingAluminumWeightMode = LoadingWeightMode.SINGLE_PACKAGE
                } else {
                    loadingIronWeightMode = LoadingWeightMode.SINGLE_PACKAGE
                }
                saveLoadingScreenToCurrentTrip()
                renderLoadingTable()
                triggerAutoSave()
                true
            }

            2 -> {
                if (type == ReturnLoadingType.ALUMINUM) {
                    loadingAluminumWeightMode = LoadingWeightMode.WEIGHBRIDGE_TOTAL
                } else {
                    loadingIronWeightMode = LoadingWeightMode.WEIGHBRIDGE_TOTAL
                }
                saveLoadingScreenToCurrentTrip()
                renderLoadingTable()
                triggerAutoSave()
                true
            }

            else -> false
        }
    }
    popup.show()
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

    val weightCell = createTableCell(getLoadingWeightHeaderText(ReturnLoadingType.IRON), true).apply {

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

    val targetList = if (type == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
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
    triggerAutoSave()
    scrollLoadingTablesToBottom()
}





fun MainActivity.showIronMaterialDialog() {
    val items = listOf(
        "背楞","吊架","单支撑","销钉","销片","销钉销片","四方垫片","对拉螺母","对拉螺杆",
        "斜撑","圆管","调节底座","码仔","K板螺丝","T字螺杆","放线盒","上料箱","泵管盒",
        "方通扣","回型钩","凳子","拉片小斜撑","背楞接头","铁钩铁锤","其他铁件"
    )

    AlertDialog.Builder(this)
        .setTitle("铁物料")
        .setItems(items.toTypedArray()) { _, which ->
            val name = items[which]
            when (name) {
                "单支撑", "对拉螺杆", "斜撑", "圆管" -> {
                    NumberInputDialogHelper(
                        context = this,
                        titleText = "请输入长度",
                        initialValue = "",
                        allowDecimal = false
                    ) { input ->
                        val finalName = if (input.isBlank()) name else "${name}${input}mm"
                        applyLoadingMaterialSelection(ReturnLoadingType.IRON, finalName)

                    }.show()
                }
                else -> applyLoadingMaterialSelection(ReturnLoadingType.IRON, name)

            }
        }
        .setNegativeButton("取消", null)
        .show()
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
        triggerAutoSave()
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
        triggerAutoSave()

    }.show()
}

fun MainActivity.showVehiclePlateInputDialog(initialValue: String, onConfirm: (String) -> Unit) {
    val input = EditText(this).apply {
        setText(initialValue)
        setSelection(text.length)
        hint = "请输入运输车牌号"
        isSingleLine = true
    }

    AlertDialog.Builder(this)
        .setTitle("运输车牌号")
        .setView(input)
        .setNegativeButton("取消", null)
        .setPositiveButton("确定") { _, _ ->
            onConfirm(input.text.toString().trim())
        }
        .show()
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

    val container = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(8), dp(16), dp(8))
    }

    fun buildPicker(min: Int, max: Int, value: Int): NumberPicker {
        return NumberPicker(this).apply {
            minValue = min
            maxValue = max
            this.value = value.coerceIn(min, max)
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            wrapSelectorWheel = false
        }
    }

    val yearPicker = buildPicker(2020, 2100, initialYear)
    val monthPicker = buildPicker(1, 12, initialMonth)
    val dayPicker = buildPicker(1, 31, initialDay)

    container.addView(yearPicker)
    container.addView(TextView(this).apply { text = "年"; setPadding(dp(4), 0, dp(8), 0) })
    container.addView(monthPicker)
    container.addView(TextView(this).apply { text = "月"; setPadding(dp(4), 0, dp(8), 0) })
    container.addView(dayPicker)
    container.addView(TextView(this).apply { text = "日"; setPadding(dp(4), 0, 0, 0) })

    AlertDialog.Builder(this)
        .setTitle("装车时间")
        .setView(container)
        .setNegativeButton("取消", null)
        .setPositiveButton("确定") { _, _ ->
            val date = String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                yearPicker.value,
                monthPicker.value,
                dayPicker.value
            )
            onConfirm(date)
        }
        .show()
}

fun MainActivity.showVehicleInfoDialog() {
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(12), dp(18), dp(4))
    }

    fun buildItem(title: String): Pair<LinearLayout, TextView> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val label = TextView(this).apply {
            text = title
            textSize = 15f
            setTextColor(0xFF222222.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val value = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFF4E3D91.toInt())
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(label)
        row.addView(value)
        return row to value
    }

    val (rowPlate, tvPlate) = buildItem("运输车牌号")
    val (rowLoadingDate, tvLoadingDate) = buildItem("装车时间")
    val (rowGross, tvGross) = buildItem("装车毛重")
    val (rowTare, tvTare) = buildItem("车辆皮重")
    val (rowMidAl, tvMidAl) = buildItem("中途铝重")
    val (rowMidIron, tvMidIron) = buildItem("中途铁重")
    val (rowWood, tvWood) = buildItem("木方估算")
    val (rowNet, tvNet) = buildItem("装车净重")
    val (rowAl, tvAl) = buildItem("铝模重量")
    val (rowIron, tvIron) = buildItem("铁件重量")

    container.addView(rowPlate)
    container.addView(rowLoadingDate)
    container.addView(rowGross)
    container.addView(rowTare)
    container.addView(rowMidAl)
    container.addView(rowMidIron)
    container.addView(rowWood)
    container.addView(rowNet)
    container.addView(rowAl)
    container.addView(rowIron)

    fun refresh() {
        tvPlate.text = vehicleInfo.vehiclePlateNumber.ifBlank { "点击输入" }
        tvLoadingDate.text = vehicleInfo.loadingDate.ifBlank { "点击选择" }
        tvGross.text = vehicleInfo.grossWeight.ifBlank { "点击输入" }
        tvTare.text = vehicleInfo.tareWeight.ifBlank { "点击输入" }
        tvMidAl.text =
            if (vehicleInfo.middleIronWeight.isNotBlank()) "请先删除另一个中途数据"
            else vehicleInfo.middleAluminumWeight.ifBlank { "点击输入" }

        tvMidIron.text =
            if (vehicleInfo.middleAluminumWeight.isNotBlank()) "请先删除另一个中途数据"
            else vehicleInfo.middleIronWeight.ifBlank { "点击输入" }

        tvWood.text = vehicleInfo.woodEstimate.ifBlank { "点击输入" }
        tvNet.text = formatLoadingNumber(vehicleInfo.netWeight())
        tvAl.text = formatLoadingNumber(vehicleInfo.aluminumWeight())
        tvIron.text = formatLoadingNumber(vehicleInfo.ironWeight())

        renderLoadingTable()
    }

    fun input(title: String, setter: (String) -> Unit) {
        NumberInputDialogHelper(this, title, "", true) {
            setter(it)
            refresh()
            triggerAutoSave()
        }.show()
    }

    rowPlate.setOnClickListener {
        showVehiclePlateInputDialog(vehicleInfo.vehiclePlateNumber) {
            vehicleInfo.vehiclePlateNumber = it
            refresh()
            triggerAutoSave()
        }
    }

    rowLoadingDate.setOnClickListener {
        showLoadingDatePickerDialog(vehicleInfo.loadingDate) {
            vehicleInfo.loadingDate = it
            refresh()
            triggerAutoSave()
        }
    }

    rowGross.setOnClickListener { input("装车毛重") { vehicleInfo.grossWeight = it } }
    rowTare.setOnClickListener { input("车辆皮重") { vehicleInfo.tareWeight = it } }
    rowWood.setOnClickListener { input("木方估算") { vehicleInfo.woodEstimate = it } }

    rowMidAl.setOnClickListener {
        if (vehicleInfo.middleIronWeight.isNotBlank()) return@setOnClickListener
        input("中途铝重") { vehicleInfo.middleAluminumWeight = it }
    }

    rowMidIron.setOnClickListener {
        if (vehicleInfo.middleAluminumWeight.isNotBlank()) return@setOnClickListener
        input("中途铁重") { vehicleInfo.middleIronWeight = it }
    }

    refresh()

    AlertDialog.Builder(this)
        .setTitle("过磅信息")
        .setView(container)
        .setPositiveButton("关闭", null)
        .show()
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

    renderLoadingTable()
    triggerAutoSave()
}


fun MainActivity.moveLoadingToNextColumn() {
    currentLoadingField = when (currentLoadingField) {
        ReturnLoadingField.MATERIAL_NAME -> ReturnLoadingField.PACKAGE_OR_COUNT
        ReturnLoadingField.PACKAGE_OR_COUNT -> ReturnLoadingField.AREA_OR_WEIGHT
        ReturnLoadingField.AREA_OR_WEIGHT -> ReturnLoadingField.WEIGHT
        ReturnLoadingField.WEIGHT -> ReturnLoadingField.REMARK
        ReturnLoadingField.REMARK -> ReturnLoadingField.MATERIAL_NAME
    }
    renderLoadingTable()
    triggerAutoSave()
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
        renderLoadingTable()
        triggerAutoSave()
    } else {
        toast("请通过铝物料或铁物料新增数据")
    }
}



fun MainActivity.deleteLastLoadingValue() {
    val row = getCurrentLoadingRow() ?: return

    when (currentLoadingField) {
        ReturnLoadingField.MATERIAL_NAME -> return
        ReturnLoadingField.PACKAGE_OR_COUNT -> if (row.packageOrCount.isNotEmpty()) row.packageOrCount = row.packageOrCount.dropLast(1)
        ReturnLoadingField.AREA_OR_WEIGHT -> if (row.areaOrWeight.isNotEmpty()) row.areaOrWeight = row.areaOrWeight.dropLast(1)
        ReturnLoadingField.WEIGHT -> {
            if (!isLoadingWeightEditable(currentLoadingEditType)) return
            if (row.weight.isNotEmpty()) row.weight = row.weight.dropLast(1)
        }
        ReturnLoadingField.REMARK -> return
    }

    renderLoadingTable()
    triggerAutoSave()
}



fun MainActivity.getCurrentLoadingRow(): ReturnLoadingRow? {
    val list = if (currentLoadingEditType == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
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

