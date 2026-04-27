package com.example.datarecorder

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.HorizontalScrollView
import org.apache.poi.ss.util.RegionUtil
import org.apache.poi.ss.usermodel.Sheet
import org.json.JSONArray
import org.json.JSONObject


// =========================
// 返厂装车 / 汇总导出
// =========================
private data class LoadingSummaryTripItem(
    val buildingName: String,
    val tripName: String,
    val vehicleInfo: VehicleInfo,
    val aluminumRows: List<ReturnLoadingRow>,
    val ironRows: List<ReturnLoadingRow>
)

private fun parseSummaryTripItemsFromLoadingContent(
    content: String,
    buildingName: String
): List<LoadingSummaryTripItem> {
    if (content.isBlank()) return emptyList()

    return try {
        val root = JSONObject(content)
        val trips = root.optJSONArray("trips") ?: JSONArray()
        val result = mutableListOf<LoadingSummaryTripItem>()

        for (i in 0 until trips.length()) {
            val obj = trips.getJSONObject(i)
            val tripName = obj.optString("tripName", "")
            if (tripName.isBlank()) continue

            val aluminumRows = mutableListOf<ReturnLoadingRow>()
            val ironRows = mutableListOf<ReturnLoadingRow>()

            val alArray = obj.optJSONArray("aluminumRows") ?: JSONArray()
            for (j in 0 until alArray.length()) {
                val rowObj = alArray.getJSONObject(j)
                aluminumRows.add(
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
                ironRows.add(
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
            val vehicle = VehicleInfo(
                grossWeight = vehicleObj.optString("grossWeight"),
                tareWeight = vehicleObj.optString("tareWeight"),
                middleAluminumWeight = vehicleObj.optString("middleAluminumWeight"),
                middleIronWeight = vehicleObj.optString("middleIronWeight"),
                woodEstimate = vehicleObj.optString("woodEstimate"),
                vehiclePlateNumber = vehicleObj.optString("vehiclePlateNumber"),
                loadingDate = vehicleObj.optString("loadingDate")
            )

            result.add(
                LoadingSummaryTripItem(
                    buildingName = buildingName,
                    tripName = tripName,
                    vehicleInfo = vehicle,
                    aluminumRows = aluminumRows,
                    ironRows = ironRows
                )
            )
        }

        result
    } catch (_: Exception) {
        emptyList()
    }
}

private fun sumRowWeights(rows: List<ReturnLoadingRow>): Double {
    return rows.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }
}

private fun sumRowArea(rows: List<ReturnLoadingRow>): Double {
    return rows.sumOf { it.areaOrWeight.toDoubleOrNull() ?: 0.0 }
}

private fun sumIronWeightByMaterial(rows: List<ReturnLoadingRow>, name: String): Double {
    return rows
        .filter { it.materialName.trim() == name }
        .sumOf { it.weight.toDoubleOrNull() ?: 0.0 }
}

private fun sumIronQuantityByMaterial(rows: List<ReturnLoadingRow>, name: String): Double {
    return rows
        .filter { it.materialName.trim() == name }
        .sumOf { it.areaOrWeight.toDoubleOrNull() ?: 0.0 }
}

private fun resolveIronSummaryUnit(materialName: String): String {
    val name = materialName.trim()
    return when {
        name == "销钉销片" || name == "铁钩铁锤" -> "套"
        name == "单支撑" || name == "斜撑" || name == "拉片小斜撑" || name == "对拉螺杆" -> "根"
        else -> "个"
    }
}

private fun parseLoadingDateForSort(dateText: String): Long {
    if (dateText.isBlank()) return Long.MAX_VALUE
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateText)?.time ?: Long.MAX_VALUE
    } catch (_: Exception) {
        Long.MAX_VALUE
    }
}

fun MainActivity.buildLoadingSummaryExcelBytes(projectName: String): ByteArray {
    saveCurrentBuildingScopeToMemory()

    val allItems = mutableListOf<LoadingSummaryTripItem>()
    getAllBuildingNamesInOrder().forEach { buildingName ->
        val content = buildingLoadingContentMap[buildingName].orEmpty()
        allItems.addAll(parseSummaryTripItemsFromLoadingContent(content, buildingName))
    }

    allItems.sortWith(
        compareBy<LoadingSummaryTripItem> { parseLoadingDateForSort(it.vehicleInfo.loadingDate) }
            .thenBy { it.buildingName }
            .thenBy { it.tripName }
    )


    val customIronNames = allItems
        .flatMap { item -> item.ironRows.map { row -> row.materialName.trim() } }
        .filter { it.isNotBlank() && it != "背楞" && it != "吊架" }
        .distinct()

    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("返厂汇总")

    val integerFormat = workbook.createDataFormat().getFormat("0")
    val decimalFormat = workbook.createDataFormat().getFormat("0.##")


    val titleFont = workbook.createFont().apply {
        bold = true
        fontHeightInPoints = 14
    }

    val boldFont = workbook.createFont().apply {
        bold = true
        fontHeightInPoints = 11
    }

    val normalFont = workbook.createFont().apply {
        bold = false
        fontHeightInPoints = 11
    }

    val titleStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        setFont(titleFont)
    }

    val headerStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        wrapText = true
        setFont(boldFont)
    }

    val bodyTextStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        wrapText = true
        setFont(normalFont)
    }

    val bodyIntegerStyle = workbook.createCellStyle().apply {
        cloneStyleFrom(bodyTextStyle)
        dataFormat = integerFormat
    }
    val bodyDecimalStyle = workbook.createCellStyle().apply {
        cloneStyleFrom(bodyTextStyle)
        dataFormat = decimalFormat
    }

    val boldTextStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        wrapText = true
        setFont(boldFont)
    }

    val boldIntegerStyle = workbook.createCellStyle().apply {
        cloneStyleFrom(boldTextStyle)
        dataFormat = integerFormat
    }
    val boldDecimalStyle = workbook.createCellStyle().apply {
        cloneStyleFrom(boldTextStyle)
        dataFormat = decimalFormat
    }

    fun row(index: Int, height: Float = 35f) = (sheet.getRow(index) ?: sheet.createRow(index)).apply {
        heightInPoints = height
    }

    fun fill(rowIndex: Int, start: Int, end: Int, style: XSSFCellStyle, height: Float = 35f) {
        val r = row(rowIndex, height)
        for (i in start..end) {
            val c = r.getCell(i) ?: r.createCell(i)
            c.cellStyle = style
        }
    }

    fun setText(rowIndex: Int, col: Int, value: String, style: XSSFCellStyle = bodyTextStyle) {
        val r = row(rowIndex)
        val c = r.getCell(col) ?: r.createCell(col)
        c.setCellValue(value)
        c.cellStyle = style
    }

    fun setNumber(
        rowIndex: Int,
        col: Int,
        value: Double?,
        style: XSSFCellStyle = bodyDecimalStyle,
        blankWhenZero: Boolean = false
    ) {
        val r = row(rowIndex)
        val c = r.getCell(col) ?: r.createCell(col)
        if (value == null || (blankWhenZero && value == 0.0)) {
            c.setBlank()
        } else {
            c.setCellValue(value)
        }
        c.cellStyle = style
    }


    val fixedColumns = listOf(
        "序号",
        "楼栋号",
        "返厂日期",
        "车牌号",
        "铝模板重量（kg）",
        "铝模板面积（㎡）",
        "铁件总重量（kg）",
        "背楞重量（kg）",
        "吊架重量（kg）"
    )

    val allColumns = fixedColumns + customIronNames

    allColumns.forEachIndexed { index, _ ->
        sheet.setColumnWidth(index, 12 * 256)
    }
    if (allColumns.lastIndex >= 8) {
        sheet.setColumnWidth(8, 14 * 256)
    }


    // 标题
    fill(0, 0, allColumns.lastIndex.coerceAtLeast(0), titleStyle, 35f)
    setText(0, 0, "返厂汇总表", titleStyle)
    safeMerge(sheet, 0, 0, 0, allColumns.lastIndex.coerceAtLeast(0))

    // 项目信息
    fill(1, 0, allColumns.lastIndex.coerceAtLeast(0), boldTextStyle, 40f)
    setText(1, 0, "项目名称：", boldTextStyle)
    setText(1, 1, projectName, boldTextStyle)
    safeMerge(sheet, 1, 1, 1, 3)
    setText(1, 4, "编制日期：", boldTextStyle)
    setText(1, 5, SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date()), boldTextStyle)
    safeMerge(sheet, 1, 1, 5, 7)
    setText(1, 8, "项目负责人：", boldTextStyle)
    if (allColumns.lastIndex >= 9) {
        safeMerge(sheet, 1, 1, 9, allColumns.lastIndex)
    }

    // 表头行
    fill(2, 0, allColumns.lastIndex.coerceAtLeast(0), headerStyle, 35f)
    allColumns.forEachIndexed { index, name ->
        setText(2, index, name, headerStyle)
    }

    // 单位行（新增）
    fill(3, 0, allColumns.lastIndex.coerceAtLeast(0), headerStyle, 35f)

    // 序号到吊架重量列之间纵向合并
    for (col in 0..8.coerceAtMost(allColumns.lastIndex)) {
        safeMerge(sheet, 2, 3, col, col)
        setText(2, col, fixedColumns[col], headerStyle)
    }

    // 动态铁物料单位行
    customIronNames.forEachIndexed { customIndex, materialName ->
        val col = 9 + customIndex
        setText(2, col, materialName, headerStyle)
        setText(3, col, resolveIronSummaryUnit(materialName), headerStyle)
    }

    var rowIndex = 4
    allItems.forEachIndexed { index, item ->
        fill(rowIndex, 0, allColumns.lastIndex.coerceAtLeast(0), bodyTextStyle, 35f)

        val aluminumSingleWeight = sumRowWeights(item.aluminumRows)
        val ironSingleWeight = sumRowWeights(item.ironRows)

        val aluminumDisplayWeight = if (aluminumSingleWeight == 0.0) {
            item.vehicleInfo.aluminumWeight()
        } else {
            aluminumSingleWeight
        }

        val ironDisplayWeight = if (ironSingleWeight == 0.0) {
            item.vehicleInfo.ironWeight()
        } else {
            ironSingleWeight
        }

        setNumber(rowIndex, 0, (index + 1).toDouble(), bodyIntegerStyle)
        setText(rowIndex, 1, item.buildingName, bodyTextStyle)
        setText(rowIndex, 2, item.vehicleInfo.loadingDate, bodyTextStyle)
        setText(rowIndex, 3, item.vehicleInfo.vehiclePlateNumber, bodyTextStyle)
        setNumber(rowIndex, 4, aluminumDisplayWeight, bodyIntegerStyle)
        setNumber(rowIndex, 5, sumRowArea(item.aluminumRows), bodyIntegerStyle, blankWhenZero = true)
        setNumber(rowIndex, 6, ironDisplayWeight, bodyIntegerStyle)
        setNumber(rowIndex, 7, sumIronWeightByMaterial(item.ironRows, "背楞"), bodyIntegerStyle, blankWhenZero = true)
        setNumber(rowIndex, 8, sumIronWeightByMaterial(item.ironRows, "吊架"), bodyIntegerStyle, blankWhenZero = true)



        customIronNames.forEachIndexed { customIndex, materialName ->
            val value = sumIronQuantityByMaterial(item.ironRows, materialName)
            setNumber(rowIndex, 9 + customIndex, value, bodyIntegerStyle, blankWhenZero = true)
        }


        rowIndex++
    }

    // 合计行
    fill(rowIndex, 0, allColumns.lastIndex.coerceAtLeast(0), boldTextStyle, 35f)
    setText(rowIndex, 0, "合计", boldTextStyle)
    safeMerge(sheet, rowIndex, rowIndex, 0, 3)

    val totalAlWeight = allItems.sumOf {
        val single = sumRowWeights(it.aluminumRows)
        if (single == 0.0) it.vehicleInfo.aluminumWeight() else single
    }
    val totalAlArea = allItems.sumOf { sumRowArea(it.aluminumRows) }
    val totalIronWeight = allItems.sumOf {
        val single = sumRowWeights(it.ironRows)
        if (single == 0.0) it.vehicleInfo.ironWeight() else single
    }
    val totalBack = allItems.sumOf { sumIronWeightByMaterial(it.ironRows, "背楞") }
    val totalHanger = allItems.sumOf { sumIronWeightByMaterial(it.ironRows, "吊架") }

    setNumber(rowIndex, 4, totalAlWeight, boldIntegerStyle)
    setNumber(rowIndex, 5, totalAlArea, boldIntegerStyle)
    setNumber(rowIndex, 6, totalIronWeight, boldIntegerStyle)
    setNumber(rowIndex, 7, totalBack, boldIntegerStyle)
    setNumber(rowIndex, 8, totalHanger, boldIntegerStyle)



    customIronNames.forEachIndexed { customIndex, materialName ->
        val total = allItems.sumOf { sumIronQuantityByMaterial(it.ironRows, materialName) }
        setNumber(rowIndex, 9 + customIndex, total, boldIntegerStyle, blankWhenZero = true)
    }


    applyOuterMediumBorder(sheet, 0, rowIndex, 0, allColumns.lastIndex.coerceAtLeast(0))

    return ByteArrayOutputStream().use { output ->
        workbook.write(output)
        workbook.close()
        output.toByteArray()
    }
}


private fun applyOuterMediumBorder(
    sheet: org.apache.poi.xssf.usermodel.XSSFSheet,
    firstRow: Int,
    lastRow: Int,
    firstCol: Int,
    lastCol: Int
) {
    if (lastRow < firstRow || lastCol < firstCol) return
    val region = CellRangeAddress(firstRow, lastRow, firstCol, lastCol)
    RegionUtil.setBorderTop(BorderStyle.MEDIUM, region, sheet)
    RegionUtil.setBorderBottom(BorderStyle.MEDIUM, region, sheet)
    RegionUtil.setBorderLeft(BorderStyle.MEDIUM, region, sheet)
    RegionUtil.setBorderRight(BorderStyle.MEDIUM, region, sheet)
}

fun MainActivity.scrollLoadingTablesToBottom() {
    bodyVerticalScroll.post {
        bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
    }

    loadingBodyVerticalScroll.post {
        loadingBodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
    }
}


fun MainActivity.getCurrentLoadingList(): MutableList<ReturnLoadingRow> {
    return if (currentLoadingEditType == ReturnLoadingType.ALUMINUM) {
        loadingAluminumRows
    } else {
        loadingIronRows
    }
}

fun MainActivity.applyLoadingMaterialSelection(type: ReturnLoadingType, materialName: String) {
    if (!ensureLoadingTripSelected()) return

    val currentList = getCurrentLoadingList()
    val currentRow = currentList.getOrNull(currentLoadingEditIndex)

    if (currentRow != null && currentLoadingField == ReturnLoadingField.MATERIAL_NAME) {
        if (currentLoadingEditType == type) {
            currentRow.materialName = materialName
        } else {
            currentList.removeAt(currentLoadingEditIndex)
            val targetList = if (type == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
            currentRow.type = type
            currentRow.materialName = materialName
            targetList.add(currentRow)
            currentLoadingEditType = type
            currentLoadingEditIndex = targetList.lastIndex
        }

        if (type == ReturnLoadingType.ALUMINUM) {
            val row = getCurrentLoadingRow()
            if (row != null && row.packageOrCount.isBlank() && !loadingAluminumUsePackageCount) {
                row.packageOrCount = generateNextLoadingPackageNo()
            }
            currentLoadingField = if (loadingAluminumUsePackageCount) {
                ReturnLoadingField.PACKAGE_OR_COUNT
            } else {
                ReturnLoadingField.AREA_OR_WEIGHT
            }
        } else {
            currentLoadingField = ReturnLoadingField.PACKAGE_OR_COUNT
        }

        renderLoadingTable()
        triggerAutoSave()
        return
    }

    addLoadingMaterial(type, materialName)
}


fun MainActivity.currentListOrCurrent(type: ReturnLoadingType, index: Int): ReturnLoadingRow? {
    val list = if (type == ReturnLoadingType.ALUMINUM) loadingAluminumRows else loadingIronRows
    return list.getOrNull(index)
}

fun MainActivity.generateNextLoadingPackageNo(): String {
    if (loadingAluminumUsePackageCount) return ""

    var max = 0
    loadingAluminumRows.forEach { row ->
        val value = row.packageOrCount.toIntOrNull() ?: 0
        if (value > max) max = value
    }
    return (max + 1).toString()
}


fun MainActivity.buildLoadingExcelBytes(projectName: String): ByteArray {
    saveLoadingScreenToCurrentTrip()

    val workbook = XSSFWorkbook()
    val tripNames = if (loadingTripMap.isEmpty()) listOf("第1车") else loadingTripMap.keys.toList()

    tripNames.forEach { tripName ->
        val tripData = loadingTripMap[tripName] ?: ReturnLoadingTripData(tripName = tripName)
        val vehicle = tripData.vehicleInfo
        val aluminumWeightMode = tripData.aluminumWeightMode
        val ironWeightMode = tripData.ironWeightMode

        val sheet = workbook.createSheet(tripName)

        val titleFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 14
        }

        val titleStyle: XSSFCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            setFont(titleFont)
        }

        val headerStyle: XSSFCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            wrapText = true
        }

        val bodyStyle: XSSFCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderTop = BorderStyle.THIN
            borderBottom = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            wrapText = true
        }

        val noBorderStyle: XSSFCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.LEFT
            verticalAlignment = VerticalAlignment.CENTER
        }

        fun row(index: Int, height: Float = 17f) = (sheet.getRow(index) ?: sheet.createRow(index)).apply {
            heightInPoints = height
        }

        fun fill(
            rowIndex: Int,
            start: Int = 0,
            end: Int = 8,
            style: XSSFCellStyle = bodyStyle,
            height: Float = 17f
        ) {
            val r = row(rowIndex, height)
            for (i in start..end) {
                val c = r.getCell(i) ?: r.createCell(i)
                c.cellStyle = style
            }
        }

        fun set(rowIndex: Int, col: Int, value: String, style: XSSFCellStyle = bodyStyle) {
            val r = row(rowIndex)
            val c = r.getCell(col) ?: r.createCell(col)
            c.setCellValue(value)
            c.cellStyle = style
        }

        sheet.setColumnWidth(0, 11 * 256)
        sheet.setColumnWidth(1, 13 * 256)
        sheet.setColumnWidth(2, 8 * 256)
        sheet.setColumnWidth(3, 14 * 256)
        sheet.setColumnWidth(4, 11 * 256)
        sheet.setColumnWidth(5, 9 * 256)
        sheet.setColumnWidth(6, 13 * 256)
        sheet.setColumnWidth(7, 14 * 256)
        sheet.setColumnWidth(8, 8 * 256)

        // 标题
        fill(0, style = titleStyle, height = 28f)
        set(0, 0, "返厂物料交接单（统计表）", titleStyle)
        safeMerge(sheet, 0, 0, 0, 8)

        // 顶部信息
        fill(1)
        set(1, 0, "项目名称")
        set(1, 1, if (projectName.isBlank()) currentProjectName else projectName)
        safeMerge(sheet, 1, 1, 1, 2)

        set(1, 3, "楼栋号")
        set(1, 4, currentBuildingName)

        set(1, 5, "车次")
        set(1, 6, tripName)
        safeMerge(sheet, 1, 1, 6, 8)

        fill(2)
        set(2, 0, "是否为最后一车材料：")
        safeMerge(sheet, 2, 2, 0, 1)
        set(2, 2, "○是")
        set(2, 3, "○否")
        set(2, 4, "装车时间")
        set(2, 5, vehicle.loadingDate)

        safeMerge(sheet, 2, 2, 5, 8)

        // 铝件表头
        fill(3, style = headerStyle)
        set(3, 0, "物料类别", headerStyle)
        set(3, 1, "物料名称", headerStyle)
        set(3, 2, "包号", headerStyle)
        set(3, 3, "面积（㎡）", headerStyle)
        set(3, 4, "重量（kg）", headerStyle)
        safeMerge(sheet, 3, 3, 5, 8)
        set(3, 5, "备      注", headerStyle)

        var rowIndex = 4

        val aluminumRows = if (tripData.aluminumRows.isEmpty()) {
            mutableListOf(ReturnLoadingRow(type = ReturnLoadingType.ALUMINUM))
        } else {
            tripData.aluminumRows.map { it.copy() }.toMutableList()
        }

        val ironRows = if (tripData.ironRows.isEmpty()) {
            mutableListOf(ReturnLoadingRow(type = ReturnLoadingType.IRON))
        } else {
            tripData.ironRows.map { it.copy() }.toMutableList()
        }

        val aluminumWeightSum = aluminumRows.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }
        val ironWeightSum = ironRows.sumOf { it.weight.toDoubleOrNull() ?: 0.0 }

        val aluminumExcelWeightTotal = if (aluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
            vehicle.aluminumWeight()
        } else {
            aluminumWeightSum
        }

        val ironExcelWeightTotal = if (ironWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
            vehicle.ironWeight()
        } else {
            ironWeightSum
        }



        // 铝件数据
        val aluminumStart = rowIndex
        aluminumRows.forEach { item ->
            fill(rowIndex)
            set(rowIndex, 1, item.materialName)
            set(rowIndex, 2, item.packageOrCount)
            set(rowIndex, 3, item.areaOrWeight)
            set(rowIndex, 4, if (aluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) "" else item.weight)

            safeMerge(sheet, rowIndex, rowIndex, 5, 8)
            set(rowIndex, 5, item.remark)
            rowIndex++
        }

        if (aluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL && aluminumRows.isNotEmpty()) {

            safeMerge(sheet, aluminumStart, rowIndex - 1, 4, 4)
            set(aluminumStart, 4, formatLoadingNumber(vehicle.aluminumWeight()))
        }


        // 铝件合计
        val aluminumTotalRow = rowIndex
        fill(aluminumTotalRow)
        set(aluminumTotalRow, 1, "返厂合计")
        set(aluminumTotalRow, 4, formatLoadingNumber(aluminumExcelWeightTotal))
        safeMerge(sheet, aluminumTotalRow, aluminumTotalRow, 5, 8)
        rowIndex++

        // 铝件签字区
        val aluminumSignRow1 = rowIndex
        fill(aluminumSignRow1)
        set(aluminumSignRow1, 2, "成品库确认")
        safeMerge(sheet, aluminumSignRow1, aluminumSignRow1, 2, 3)
        set(aluminumSignRow1, 5, "成品库：")
        safeMerge(sheet, aluminumSignRow1, aluminumSignRow1, 5, 8)
        rowIndex++

        val aluminumSignRow2 = rowIndex
        fill(aluminumSignRow2)
        set(aluminumSignRow2, 2, "子公司接收入确认")
        safeMerge(sheet, aluminumSignRow2, aluminumSignRow2, 2, 3)
        set(aluminumSignRow2, 5, "子公司接收入：")
        safeMerge(sheet, aluminumSignRow2, aluminumSignRow2, 5, 8)
        rowIndex++

        safeMerge(sheet, aluminumStart, aluminumSignRow2, 0, 0)
        set(aluminumStart, 0, "铝件/ 铝模板（含混凝土）")

        safeMerge(sheet, aluminumSignRow1, aluminumSignRow2, 1, 1)
        set(aluminumSignRow1, 1, "到厂后填写")

        // 铁件表头
        val ironHeaderRow = rowIndex
        fill(ironHeaderRow, style = headerStyle)
        set(ironHeaderRow, 1, "物料名称", headerStyle)
        set(ironHeaderRow, 2, "包数", headerStyle)
        set(ironHeaderRow, 3, "数量（件/套）", headerStyle)
        set(ironHeaderRow, 4, "重量（kg）", headerStyle)
        safeMerge(sheet, ironHeaderRow, ironHeaderRow, 5, 8)
        rowIndex++

        // 铁件数据
        val ironStart = rowIndex
        ironRows.forEach { item ->
            fill(rowIndex)
            set(rowIndex, 1, item.materialName)
            set(rowIndex, 2, item.packageOrCount)
            set(rowIndex, 3, item.areaOrWeight)
            set(rowIndex, 4, if (ironWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) "" else item.weight)

            safeMerge(sheet, rowIndex, rowIndex, 5, 8)
            set(rowIndex, 5, item.remark)
            rowIndex++
        }

        if (ironWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL && ironRows.isNotEmpty()) {

            safeMerge(sheet, ironStart, rowIndex - 1, 4, 4)
            set(ironStart, 4, formatLoadingNumber(vehicle.ironWeight()))
        }


        // 铁件合计
        val ironTotalRow = rowIndex
        fill(ironTotalRow)
        set(ironTotalRow, 1, "返厂合计")
        set(ironTotalRow, 4, formatLoadingNumber(ironExcelWeightTotal))
        safeMerge(sheet, ironTotalRow, ironTotalRow, 5, 8)
        rowIndex++

        // 铁件签字区
        val ironSignRow1 = rowIndex
        fill(ironSignRow1)
        set(ironSignRow1, 2, "成品库确认")
        safeMerge(sheet, ironSignRow1, ironSignRow1, 2, 3)
        set(ironSignRow1, 5, "成品库：")
        safeMerge(sheet, ironSignRow1, ironSignRow1, 5, 8)
        rowIndex++

        val ironSignRow2 = rowIndex
        fill(ironSignRow2)
        set(ironSignRow2, 2, "子公司接收入确认")
        safeMerge(sheet, ironSignRow2, ironSignRow2, 2, 3)
        set(ironSignRow2, 5, "子公司接收入：")
        safeMerge(sheet, ironSignRow2, ironSignRow2, 5, 8)
        rowIndex++

        safeMerge(sheet, ironStart, ironSignRow2, 0, 0)
        set(ironStart, 0, "铁件（含混凝土）")

        safeMerge(sheet, ironSignRow1, ironSignRow2, 1, 1)
        set(ironSignRow1, 1, "到厂后填写")

        // 工地填写 / 工厂填写
        val workStart = rowIndex
        val workItems = listOf(
            "装车毛重：" to vehicle.grossWeight,
            "车辆皮重：" to vehicle.tareWeight,
            "木方估算：" to vehicle.woodEstimate,
            "装车净重：" to formatLoadingNumber(vehicle.netWeight())
        )

        workItems.forEach { (label, value) ->
            fill(rowIndex)

            // 左侧：工地填写
            set(rowIndex, 1, label)
            safeMerge(sheet, rowIndex, rowIndex, 2, 3)
            set(rowIndex, 2, value)
            set(rowIndex, 4, "Kg")

            // 右侧：工厂填写
            set(rowIndex, 6, label)
            set(rowIndex, 7, "")
            set(rowIndex, 8, "Kg")

            rowIndex++
        }

        val workEnd = rowIndex - 1
        safeMerge(sheet, workStart, workEnd, 0, 0)
        set(workStart, 0, "工地填写")

        safeMerge(sheet, workStart, workEnd, 5, 5)
        set(workStart, 5, "工厂填写")

        // 运输信息
        fill(rowIndex)
        set(rowIndex, 0, "运输车牌号")
        set(rowIndex, 1, vehicle.vehiclePlateNumber)
        safeMerge(sheet, rowIndex, rowIndex, 1, 3)
        set(rowIndex, 4, "司机确认")
        safeMerge(sheet, rowIndex, rowIndex, 5, 8)
        rowIndex++

        fill(rowIndex, height = 28f)
        set(rowIndex, 0, "司机联系方式")
        safeMerge(sheet, rowIndex, rowIndex, 1, 3)
        set(rowIndex, 4, "司机确认时间")
        safeMerge(sheet, rowIndex, rowIndex, 5, 8)
        rowIndex++

        val framedLastRow = rowIndex - 1
        applyOuterMediumBorder(sheet, 0, framedLastRow, 0, 8)

        // 无框底部
        fill(rowIndex, style = noBorderStyle)
        set(rowIndex, 0, "交付方：", noBorderStyle)
        safeMerge(sheet, rowIndex, rowIndex, 1, 3)
        set(rowIndex, 4, "接收方：", noBorderStyle)
        safeMerge(sheet, rowIndex, rowIndex, 5, 8)
        rowIndex++

        fill(rowIndex, style = noBorderStyle)
        set(rowIndex, 0, "日    期：        年    月    日", noBorderStyle)
        safeMerge(sheet, rowIndex, rowIndex, 0, 3)
        set(rowIndex, 4, "日    期：        年    月    日", noBorderStyle)
        safeMerge(sheet, rowIndex, rowIndex, 4, 8)
    }

    return ByteArrayOutputStream().use { output ->
        workbook.write(output)
        workbook.close()
        output.toByteArray()
    }
}

