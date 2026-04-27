package com.example.datarecorder

import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.ClientAnchor
import org.apache.poi.ss.usermodel.Drawing
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================
// 通用导出分享相关
// =========================
fun MainActivity.insertExcelLogo(
    workbook: XSSFWorkbook,
    sheet: org.apache.poi.ss.usermodel.Sheet,
    fromCol: Int,
    toCol: Int,
    fromRow: Int,
    toRow: Int
) {
    val bitmap = BitmapFactory.decodeResource(this.resources, R.drawable.excel_logo) ?: return
    val stream = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
    val bytes = stream.toByteArray()

    val pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG)
    val drawing: Drawing<*> = sheet.createDrawingPatriarch()
    val helper = workbook.creationHelper

    val anchor: ClientAnchor = helper.createClientAnchor().apply {
        setCol1(fromCol)
        setCol2(toCol)
        setRow1(fromRow)
        setRow2(toRow)
        dx1 = 10000
        dy1 = 5000
        dx2 = -10000
        dy2 = -5000
        anchorType = ClientAnchor.AnchorType.MOVE_AND_RESIZE
    }

    drawing.createPicture(anchor, pictureIdx)
}

 fun MainActivity.setCell(
    row: org.apache.poi.ss.usermodel.Row,
    col: Int,
    value: String,
    style: XSSFCellStyle
) {
    row.createCell(col).apply {
        setCellValue(value)
        cellStyle = style
    }
}

 fun MainActivity.fillRowStyle(
    row: org.apache.poi.ss.usermodel.Row,
    startCol: Int,
    endCol: Int,
    style: XSSFCellStyle
) {
    for (i in startCol..endCol) {
        val cell = row.getCell(i) ?: row.createCell(i)
        cell.cellStyle = style
    }
}
 fun MainActivity.createExcelMainTitleStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    val style = workbook.createCellStyle()
    style.alignment = HorizontalAlignment.CENTER
    style.verticalAlignment = VerticalAlignment.CENTER
    style.borderTop = BorderStyle.THIN
    style.borderBottom = BorderStyle.THIN
    style.borderLeft = BorderStyle.THIN
    style.borderRight = BorderStyle.THIN

    val font = workbook.createFont()
    font.bold = true
    font.fontHeightInPoints = 16
    style.setFont(font)
    return style
}

 fun MainActivity.createExcelSubTitleStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    val style = workbook.createCellStyle()
    style.alignment = HorizontalAlignment.CENTER
    style.verticalAlignment = VerticalAlignment.CENTER
    style.borderTop = BorderStyle.THIN
    style.borderBottom = BorderStyle.THIN
    style.borderLeft = BorderStyle.THIN
    style.borderRight = BorderStyle.THIN

    val font = workbook.createFont()
    font.bold = true
    font.fontHeightInPoints = 12
    style.setFont(font)
    return style
}

 fun MainActivity.createExcelMetaLabelStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    val style = workbook.createCellStyle()
    style.alignment = HorizontalAlignment.CENTER
    style.verticalAlignment = VerticalAlignment.CENTER
    style.borderTop = BorderStyle.THIN
    style.borderBottom = BorderStyle.THIN
    style.borderLeft = BorderStyle.THIN
    style.borderRight = BorderStyle.THIN

    val font = workbook.createFont()
    font.bold = true
    font.fontHeightInPoints = 11
    style.setFont(font)
    return style
}

 fun MainActivity.createExcelMetaValueStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    val style = workbook.createCellStyle()
    style.alignment = HorizontalAlignment.CENTER
    style.verticalAlignment = VerticalAlignment.CENTER
    style.borderTop = BorderStyle.THIN
    style.borderBottom = BorderStyle.THIN
    style.borderLeft = BorderStyle.THIN
    style.borderRight = BorderStyle.THIN

    val font = workbook.createFont()
    font.fontHeightInPoints = 11
    style.setFont(font)
    return style
}

 fun MainActivity.createExcelTitleStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    return workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
    }
}

 fun MainActivity.createExcelHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    return workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
    }
}


 fun MainActivity.createExcelBodyStyle(workbook: XSSFWorkbook): XSSFCellStyle {
    return workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
    }
}

 fun MainActivity.buildStandardExcelBytes(projectName: String): ByteArray {
    saveScreenDataToCurrentPackage()

    val workbook = XSSFWorkbook()

    val mainTitleStyle = createExcelMainTitleStyle(workbook)
    val subTitleStyle = createExcelSubTitleStyle(workbook)
    val metaLabelStyle = createExcelMetaLabelStyle(workbook)
    val metaValueStyle = createExcelMetaValueStyle(workbook)
    val headerStyle = createExcelHeaderStyle(workbook)
    val bodyStyle = createExcelBodyStyle(workbook)

    val packageNames = if (packageStandardRowsMap.isEmpty()) {
        listOf("第1包")
    } else {
        getAllPackageNamesInOrder()
    }

    packageNames.forEach { packageName ->
        ensurePackageDate(packageName)

        val sheet = workbook.createSheet(packageName)
        val packageDate = getPackageDate(packageName)

        val rows = mutableListOf<StandardRow>()
        val savedRows = packageStandardRowsMap[packageName]
        if (savedRows != null) {
            rows.addAll(savedRows.map { row -> row.copy() })
        }
        val currentRow = packageCurrentStandardRowMap[packageName]
        if (currentRow != null && !currentRow.isEmpty()) {
            rows.add(currentRow.copy())
        }




        var rowIndex = 0

        val row0 = sheet.createRow(rowIndex++)
        row0.heightInPoints = 26f
        setCell(row0, 0, "中建铝新材料成都有限公司", mainTitleStyle)
        safeMerge(sheet,0, 0, 0, 4)
        fillRowStyle(row0, 0, 4, mainTitleStyle)

        val row1 = sheet.createRow(rowIndex++)
        row1.heightInPoints = 24f
        setCell(row1, 0, "型号统计登记表", subTitleStyle)
        safeMerge(sheet,1, 1, 0, 4)
        fillRowStyle(row1, 0, 4, subTitleStyle)

        val row2 = sheet.createRow(rowIndex++)
        setCell(row2, 0, "项目名称", metaLabelStyle)
        setCell(row2, 1, projectName, metaValueStyle)
        safeMerge(sheet,2, 2, 1, 2)
        fillRowStyle(row2, 1, 2, metaValueStyle)
        setCell(row2, 3, "楼栋号", metaLabelStyle)
        setCell(row2, 4, currentBuildingName, metaValueStyle)

        val row3 = sheet.createRow(rowIndex++)
        setCell(row3, 0, "打包日期", metaLabelStyle)
        setCell(row3, 1, packageDate, metaValueStyle)
        safeMerge(sheet,3, 3, 1, 2)
        fillRowStyle(row3, 1, 2, metaValueStyle)
        setCell(row3, 3, "包号", metaLabelStyle)
        setCell(row3, 4, packageName, metaValueStyle)

        val row4 = sheet.createRow(rowIndex++)
        listOf("序号", "安装编号", "型号", "数量", "备注").forEachIndexed { index, text ->
            setCell(row4, index, text, headerStyle)
        }

        var totalQty = 0

        if (rows.isEmpty()) {
            val row = sheet.createRow(rowIndex++)
            for (i in 0..4) {
                setCell(row, i, "", bodyStyle)
            }
        } else {
            rows.forEachIndexed { index, item ->
                val qty = item.quantity.toIntOrNull() ?: 1
                totalQty += qty

                val row = sheet.createRow(rowIndex++)
                setCell(row, 0, (index + 1).toString(), bodyStyle)
                setCell(row, 1, item.installNo, bodyStyle)
                setCell(row, 2, item.model, bodyStyle)
                setCell(row, 3, if (item.quantity.isBlank()) "1" else item.quantity, bodyStyle)
                setCell(row, 4, "", bodyStyle)
            }
        }

        val totalRow = sheet.createRow(rowIndex++)
        setCell(totalRow, 0, "合计", headerStyle)
        safeMerge(sheet,rowIndex - 1, rowIndex - 1, 0, 2)
        fillRowStyle(totalRow, 0, 2, headerStyle)
        setCell(totalRow, 3, totalQty.toString(), headerStyle)
        setCell(totalRow, 4, "", headerStyle)

        sheet.setColumnWidth(0, 10 * 256)
        sheet.setColumnWidth(1, 20 * 256)
        sheet.setColumnWidth(2, 18 * 256)
        sheet.setColumnWidth(3, 10 * 256)
        sheet.setColumnWidth(4, 16 * 256)
    }

    val output = ByteArrayOutputStream()
    workbook.use {
        it.write(output)
    }
    return output.toByteArray()
}

 fun MainActivity.createFastModeSheet(
    workbook: XSSFWorkbook,
    sheetName: String,
    packageName: String,
    rows: List<FastRow>
) {
    val sheet = workbook.createSheet(sheetName)

    val defaultFont = workbook.createFont().apply {
        fontName = "宋体"
        fontHeightInPoints = 11
    }

    val titleFont = workbook.createFont().apply {
        fontName = "宋体"
        bold = true
        fontHeightInPoints = 20
    }

    val subTitleFont = workbook.createFont().apply {
        fontName = "宋体"
        fontHeightInPoints = 12
    }

    val headerFont = workbook.createFont().apply {
        fontName = "宋体"
        bold = true
        fontHeightInPoints = 11
    }

    val boldFont = workbook.createFont().apply {
        fontName = "宋体"
        bold = true
        fontHeightInPoints = 11
    }

    val titleStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        setFont(titleFont)
    }

    val subTitleStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        setFont(subTitleFont)
    }

    val infoStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(defaultFont)
        wrapText = true
    }

    val infoHeaderStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(headerFont)
        wrapText = true
    }

    val tableHeaderStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(headerFont)
        wrapText = true
    }

    val tableCellStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(defaultFont)
    }

    val totalStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(boldFont)
    }

    val signStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.LEFT
        verticalAlignment = VerticalAlignment.CENTER
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderBottom = BorderStyle.THIN
        setFont(boldFont)
    }

    val projectName = currentProjectName.ifBlank { "未命名项目" }
    val packageDate = getPackageDate(packageName).ifBlank { getTodayPackageDate() }
    val buildingName = currentBuildingName.ifBlank { "-" }

    sheet.setColumnWidth(0, 8 * 256)
    sheet.setColumnWidth(1, 16 * 256)
    sheet.setColumnWidth(2, 10 * 256)
    sheet.setColumnWidth(3, 10 * 256)
    sheet.setColumnWidth(4, 10 * 256)
    sheet.setColumnWidth(5, 10 * 256)
    sheet.setColumnWidth(6, 12 * 256)
    sheet.setColumnWidth(7, 14 * 256)
    sheet.setColumnWidth(8, 16 * 256)

    val row0 = sheet.createRow(0).apply { heightInPoints = 34f }
    val row1 = sheet.createRow(1).apply { heightInPoints = 24f }
    val row2 = sheet.createRow(2).apply { heightInPoints = 24f }
    val row3 = sheet.createRow(3).apply { heightInPoints = 24f }
    val row4 = sheet.createRow(4).apply { heightInPoints = 24f }

    for (c in 0..8) {
        row0.createCell(c).cellStyle = infoStyle
        row1.createCell(c).cellStyle = infoStyle
        row2.createCell(c).cellStyle = infoStyle
        row3.createCell(c).cellStyle = infoStyle
        row4.createCell(c).cellStyle = tableHeaderStyle
    }

    safeMerge(sheet,0, 1, 0, 1)
    safeMerge(sheet,0, 0, 2, 8)
    safeMerge(sheet,1, 1, 2, 8)
    safeMerge(sheet,2, 2, 0, 1)
    safeMerge(sheet,2, 2, 2, 4)
    safeMerge(sheet,2, 2, 6, 8)
    safeMerge(sheet,3, 3, 0, 1)
    safeMerge(sheet,3, 3, 2, 4)
    safeMerge(sheet,3, 3, 6, 8)

    row0.getCell(2).setCellValue("中建铝新材料成都有限公司")
    row0.getCell(2).cellStyle = titleStyle

    row1.getCell(2).setCellValue("返厂铝件登记表")
    row1.getCell(2).cellStyle = subTitleStyle

    row2.getCell(0).setCellValue("项目名称")
    row2.getCell(0).cellStyle = infoHeaderStyle
    row2.getCell(2).setCellValue(projectName)
    row2.getCell(2).cellStyle = infoStyle
    row2.getCell(5).setCellValue("楼栋号：")
    row2.getCell(5).cellStyle = infoHeaderStyle
    row2.getCell(6).setCellValue(buildingName)
    row2.getCell(6).cellStyle = infoStyle

    row3.getCell(0).setCellValue("包  号")
    row3.getCell(0).cellStyle = infoHeaderStyle
    row3.getCell(2).setCellValue(packageName)
    row3.getCell(2).cellStyle = infoStyle
    row3.getCell(5).setCellValue("打包日期")
    row3.getCell(5).cellStyle = infoHeaderStyle
    row3.getCell(6).setCellValue(packageDate)
    row3.getCell(6).cellStyle = infoStyle

    row4.getCell(0).setCellValue("序号")
    row4.getCell(1).setCellValue("材料名称")
    row4.getCell(2).setCellValue("宽度")
    row4.getCell(3).setCellValue("型号")
    row4.getCell(4).setCellValue("长度")
    row4.getCell(5).setCellValue("数量")
    row4.getCell(6).setCellValue("单位面积")
    row4.getCell(7).setCellValue("合计面积")
    row4.getCell(8).setCellValue("备注")

    var rowIndex = 5
    var totalQty = 0
    var totalUnitAreaRaw = 0.0
    var totalAreaRaw = 0.0

    rows.forEachIndexed { index, item ->
        val row = sheet.createRow(rowIndex).apply { heightInPoints = 24f }

        val qty = calcFastQty(item)
        val unitAreaRaw = calcFastUnitAreaRaw(item)
        val totalAreaForRow = calcFastTotalAreaRaw(item)

        totalQty += qty
        totalUnitAreaRaw += unitAreaRaw
        totalAreaRaw += totalAreaForRow

        for (c in 0..8) {
            row.createCell(c).cellStyle = tableCellStyle
        }

        row.getCell(0).setCellValue((index + 1).toString())
        row.getCell(1).setCellValue("铝模")
        row.getCell(2).setCellValue(item.width)
        row.getCell(3).setCellValue(item.model)
        row.getCell(4).setCellValue(item.length)
        row.getCell(5).setCellValue(qty.toString())
        row.getCell(6).setCellValue(areaToSquareMeterText(unitAreaRaw))
        row.getCell(7).setCellValue(areaToSquareMeterText(totalAreaForRow))
        row.getCell(8).setCellValue("")

        rowIndex++
    }

    val totalRow = sheet.createRow(rowIndex).apply { heightInPoints = 24f }
    for (c in 0..8) {
        totalRow.createCell(c).cellStyle = totalStyle
    }

    safeMerge(sheet,rowIndex, rowIndex, 0, 1)
    totalRow.getCell(0).setCellValue("合计")
    totalRow.getCell(5).setCellValue(totalQty.toString())
    totalRow.getCell(6).setCellValue(areaToSquareMeterText(totalUnitAreaRaw))
    totalRow.getCell(7).setCellValue(areaToSquareMeterText(totalAreaRaw))
    totalRow.getCell(8).setCellValue("")

    val signRow = sheet.createRow(rowIndex + 1).apply { heightInPoints = 24f }
    for (c in 0..8) {
        signRow.createCell(c).cellStyle = signStyle
    }

    safeMerge(sheet,rowIndex + 1, rowIndex + 1, 0, 4)
    safeMerge(sheet,rowIndex + 1, rowIndex + 1, 5, 8)
    signRow.getCell(0).setCellValue("项目记录人：")
    signRow.getCell(5).setCellValue("中建铝记录人：")

    insertExcelLogo(
        workbook = workbook,
        sheet = sheet,
        fromCol = 0,
        toCol = 2,
        fromRow = 0,
        toRow = 2
    )
}

fun MainActivity.requestExportFolderAndExport() {
    if (currentProjectId <= 0) {
        toast("请先选择项目")
        return
    }

    ioExecutor.execute {
        try {
            saveScreenDataToCurrentPackage()
            saveLoadingScreenToCurrentTrip()

            if (currentModeType == ModeType.QUALITY_FEEDBACK) {
                val qualityFileName = buildQualityFeedbackWordFileName()
                val qualityBytes = buildQualityFeedbackWordBytes()

                pendingExportQualityFileName = qualityFileName
                pendingExportQualityBytes = qualityBytes

                runOnUiThread {
                    exportFolderPickerLauncher.launch(getLastExportFolderUri())
                }
                return@execute
            }

            val standardFileName =
                buildExcelFileName("${currentProjectName}_${currentBuildingName}_${modeNameStandard}")
            val fastFileName =
                buildExcelFileName("${currentProjectName}_${currentBuildingName}_${modeNameFast}")
            val loadingFileName =
                buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂装车")

            val standardBytes = buildStandardExcelBytes(currentProjectName)
            val fastBytes = buildFastExcelBytes(currentProjectName)
            val loadingBytes = buildLoadingExcelBytes(currentProjectName)

            pendingExportStandardFileName = standardFileName
            pendingExportFastFileName = fastFileName
            pendingExportLoadingFileName = loadingFileName

            pendingExportStandardBytes = standardBytes
            pendingExportFastBytes = fastBytes
            pendingExportLoadingBytes = loadingBytes

            runOnUiThread {
                exportFolderPickerLauncher.launch(getLastExportFolderUri())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                clearPendingExportData()
                toast("导出失败：${e.message ?: "未知错误"}")
            }
        }
    }
}

fun MainActivity.shareLoadingSummaryProject() {
    if (currentProjectId <= 0) {
        toast("请先选择项目")
        return
    }

    ioExecutor.execute {
        try {
            saveCurrentBuildingScopeToMemory()

            val fileName = buildExcelFileName("${currentProjectName}_返厂汇总")
            val excelBytes = buildLoadingSummaryExcelBytes(currentProjectName)

            val shareDir = File(cacheDir, "share")
            if (!shareDir.exists()) {
                shareDir.mkdirs()
            }

            val file = File(shareDir, fileName)
            file.writeBytes(excelBytes)

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            runOnUiThread {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = getExcelMimeType(fileName)
                        putExtra(Intent.EXTRA_SUBJECT, fileName)
                        putExtra(Intent.EXTRA_TITLE, fileName)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(fileName, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooserIntent = Intent.createChooser(shareIntent, "分享到")

                    val resInfoList = packageManager.queryIntentActivities(
                        chooserIntent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )

                    for (resolveInfo in resInfoList) {
                        val targetPackageName = resolveInfo.activityInfo.packageName
                        grantUriPermission(
                            targetPackageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    startActivity(chooserIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast("分享失败：${e.message ?: "未知错误"}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                toast("返厂汇总表失败：${e.message ?: "未知错误"}")
            }
        }
    }
}

 fun MainActivity.buildFastExcelBytes(projectName: String): ByteArray {
    saveScreenDataToCurrentPackage()

    val workbook = XSSFWorkbook()
    val packageNames = if (getAllPackageNamesInOrder().isEmpty()) {
        listOf("第1包")
    } else {
        getAllPackageNamesInOrder()
    }

    packageNames.forEach { packageName ->
        val rows = mutableListOf<FastRow>()
        val savedRows = packageFastRowsMap[packageName]
        if (savedRows != null) {
            rows.addAll(savedRows.map { row -> row.copy() })
        }
        val currentRow = packageCurrentFastRowMap[packageName]
        if (currentRow != null && !currentRow.isEmpty()) {
            rows.add(currentRow.copy())
        }



        createFastModeSheet(
            workbook = workbook,
            sheetName = packageName,
            packageName = packageName,
            rows = rows
        )
    }

    return ByteArrayOutputStream().use { output ->
        workbook.write(output)
        workbook.close()
        output.toByteArray()
    }
}

 fun MainActivity.buildExcelFileName(projectName: String): String {
    val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "${safeName}_$time.xlsx"
}

 fun MainActivity.exportExcelLegacy(fileName: String, excelBytes: ByteArray) {
    val downloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val exportDir = File(downloadDir, "铝模板统计")

    if (!exportDir.exists()) exportDir.mkdirs()

    val file = File(exportDir, fileName)
    file.writeBytes(excelBytes)
}

fun MainActivity.exportCurrentProjectToSelectedFolder(treeUri: Uri) {
    if (currentModeType == ModeType.QUALITY_FEEDBACK) {
        val qualityFileName = pendingExportQualityFileName
        val qualityBytes = pendingExportQualityBytes

        if (qualityFileName == null || qualityBytes == null) {
            toast("导出失败：没有可导出的数据")
            clearPendingExportData()
            return
        }

        ioExecutor.execute {
            try {
                writeBytesToTreeUri(treeUri, qualityFileName, qualityBytes)

                runOnUiThread {
                    toast("质量反馈已导出")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    toast("导出失败：${e.message ?: "未知错误"}")
                }
            } finally {
                clearPendingExportData()
            }
        }
        return
    }

    val standardFileName = pendingExportStandardFileName
    val fastFileName = pendingExportFastFileName
    val loadingFileName = pendingExportLoadingFileName

    val standardBytes = pendingExportStandardBytes
    val fastBytes = pendingExportFastBytes
    val loadingBytes = pendingExportLoadingBytes

    if (standardFileName == null || fastFileName == null || loadingFileName == null ||
        standardBytes == null || fastBytes == null || loadingBytes == null
    ) {
        toast("导出失败：没有可导出的数据")
        clearPendingExportData()
        return
    }

    ioExecutor.execute {
        try {
            writeBytesToTreeUri(treeUri, standardFileName, standardBytes)
            writeBytesToTreeUri(treeUri, fastFileName, fastBytes)
            writeBytesToTreeUri(treeUri, loadingFileName, loadingBytes)

            runOnUiThread {
                toast("型号统计、返厂统计、返厂装车已分别导出")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                toast("导出失败：${e.message ?: "未知错误"}")
            }
        } finally {
            clearPendingExportData()
        }
    }
}

fun MainActivity.clearPendingExportData() {
    pendingExportStandardFileName = null
    pendingExportFastFileName = null
    pendingExportLoadingFileName = null
    pendingExportQualityFileName = null

    pendingExportStandardBytes = null
    pendingExportFastBytes = null
    pendingExportLoadingBytes = null
    pendingExportQualityBytes = null
}



fun MainActivity.writeBytesToTreeUri(treeUri: Uri, fileName: String, bytes: ByteArray) {
    val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        ?: throw Exception("无法打开所选文件夹")

    val existing = pickedDir.findFile(fileName)
    existing?.delete()

    val mimeType = if (fileName.endsWith(".docx", true)) {
        getWordMimeType()
    } else {
        getExcelMimeType(fileName)
    }

    val targetFile = pickedDir.createFile(mimeType, fileName)
        ?: throw Exception("无法创建导出文件：$fileName")

    contentResolver.openOutputStream(targetFile.uri)?.use { output ->
        output.write(bytes)
        output.flush()
    } ?: throw Exception("无法写入导出文件：$fileName")
}


 fun MainActivity.exportCurrentProject() {
    requestExportFolderAndExport()
}
fun MainActivity.shareCurrentModeProject() {
    if (currentProjectId <= 0) {
        toast("请先选择项目")
        return
    }

    ioExecutor.execute {
        try {
            saveScreenDataToCurrentPackage()
            saveLoadingScreenToCurrentTrip()

            if (currentModeType == ModeType.QUALITY_FEEDBACK) {
                val fileName = buildQualityFeedbackWordFileName()
                val wordBytes = buildQualityFeedbackWordBytes()

                val shareDir = File(cacheDir, "share")
                if (!shareDir.exists()) {
                    shareDir.mkdirs()
                }

                val file = File(shareDir, fileName)
                file.writeBytes(wordBytes)

                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                runOnUiThread {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = getWordMimeType()
                            putExtra(Intent.EXTRA_SUBJECT, fileName)
                            putExtra(Intent.EXTRA_TITLE, fileName)
                            putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri(fileName, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        val chooserIntent = Intent.createChooser(shareIntent, "分享到")

                        val resInfoList = packageManager.queryIntentActivities(
                            chooserIntent,
                            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                        )

                        for (resolveInfo in resInfoList) {
                            val targetPackageName = resolveInfo.activityInfo.packageName
                            grantUriPermission(
                                targetPackageName,
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        startActivity(chooserIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast("分享失败：${e.message ?: "未知错误"}")
                    }
                }
                return@execute
            }


            val fileName = when (currentModeType) {
                ModeType.FAST ->
                    buildExcelFileName("${currentProjectName}_${currentBuildingName}_${modeNameFast}")

                ModeType.STANDARD ->
                    buildExcelFileName("${currentProjectName}_${currentBuildingName}_${modeNameStandard}")

                ModeType.RETURN_LOADING ->
                    buildExcelFileName("${currentProjectName}_${currentBuildingName}_返厂装车")

                ModeType.QUALITY_FEEDBACK ->
                    ""
            }

            val excelBytes = when (currentModeType) {
                ModeType.FAST ->
                    buildFastExcelBytes(currentProjectName)

                ModeType.STANDARD ->
                    buildStandardExcelBytes(currentProjectName)

                ModeType.RETURN_LOADING ->
                    buildLoadingExcelBytes(currentProjectName)

                ModeType.QUALITY_FEEDBACK ->
                    ByteArray(0)
            }

            val shareDir = File(cacheDir, "share")
            if (!shareDir.exists()) {
                shareDir.mkdirs()
            }

            val file = File(shareDir, fileName)
            file.writeBytes(excelBytes)

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            runOnUiThread {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = getExcelMimeType(fileName)
                        putExtra(Intent.EXTRA_SUBJECT, fileName)
                        putExtra(Intent.EXTRA_TITLE, fileName)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(fileName, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    val chooserIntent = Intent.createChooser(shareIntent, "分享到")

                    val resInfoList = packageManager.queryIntentActivities(
                        chooserIntent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )

                    for (resolveInfo in resInfoList) {
                        val targetPackageName = resolveInfo.activityInfo.packageName
                        grantUriPermission(
                            targetPackageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    startActivity(chooserIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast("分享失败：${e.message ?: "未知错误"}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                toast("分享失败：${e.message ?: "未知错误"}")
            }
        }
    }
}


fun MainActivity.getExcelMimeType(fileName: String): String {
    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
}


