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
        set(2, 4, "统计时间")
        set(2, 5, SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date()))
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

        val aluminumExcelWeightTotal = if (loadingAluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
            vehicle.aluminumWeight()
        } else {
            aluminumWeightSum
        }

        val ironExcelWeightTotal = if (loadingIronWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) {
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
            set(rowIndex, 4, if (loadingAluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) "" else item.weight)
            safeMerge(sheet, rowIndex, rowIndex, 5, 8)
            set(rowIndex, 5, item.remark)
            rowIndex++
        }

        if (loadingAluminumWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL && aluminumRows.isNotEmpty()) {
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
            set(rowIndex, 4, if (loadingIronWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL) "" else item.weight)
            safeMerge(sheet, rowIndex, rowIndex, 5, 8)
            set(rowIndex, 5, item.remark)
            rowIndex++
        }

        if (loadingIronWeightMode == LoadingWeightMode.WEIGHBRIDGE_TOTAL && ironRows.isNotEmpty()) {
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

