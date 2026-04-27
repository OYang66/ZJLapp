package com.example.datarecorder

import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.TableRowAlign
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.BitmapFactory
import android.net.Uri
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import java.io.InputStream
import org.apache.xmlbeans.XmlCursor


fun MainActivity.buildQualityFeedbackWordFileName(): String {
    val safeProjectName = currentProjectName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val username = SessionManager.getUsername(this).ifBlank { "未命名用户" }
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val dateText = SimpleDateFormat("yyyy-M-d", Locale.getDefault()).format(Date())
    return "${safeProjectName}质量反馈_${username}_$dateText.docx"
}

fun MainActivity.getWordMimeType(): String {
    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
}

private fun MainActivity.addQualityWordHeader(document: XWPFDocument) {
    val p1 = document.createParagraph()
    p1.alignment = ParagraphAlignment.CENTER
    p1.createRun().apply {
        isBold = true
        fontSize = 20
        setText("中建铝新材料有限公司")
    }

    val p2 = document.createParagraph()
    p2.alignment = ParagraphAlignment.CENTER
    p2.createRun().apply {
        isBold = true
        fontSize = 14
        setText("项目现场质量问题报告单")
    }

    val p3 = document.createParagraph()
    p3.alignment = ParagraphAlignment.RIGHT
    p3.createRun().apply {
        isBold = true
        fontSize = 11
        setText("报告编号：ZJL-GCZ-0")
    }

    document.createParagraph()
}

private fun clearCellContent(cell: XWPFTableCell) {
    while (cell.tables.isNotEmpty()) {
        cell.ctTc.removeTbl(0)
    }
    while (cell.paragraphs.size > 0) {
        cell.removeParagraph(0)
    }
    if (cell.paragraphs.isEmpty()) {
        cell.addParagraph()
    }
}
private fun MainActivity.insertSimpleQualityTableAtCellEnd(
    cell: XWPFTableCell,
    rows: List<QualityFeedbackRow>
) {
    val ctTbl = cell.ctTc.addNewTbl()
    val table = org.apache.poi.xwpf.usermodel.XWPFTable(ctTbl, cell)

    setSafeTableWidth(table)
    table.setTableAlignment(TableRowAlign.CENTER)

    // 不再 createRow() 生成第一行，直接使用现有第一行作为表头
    val header = table.getRow(0) ?: run {
        ctTbl.addNewTr()
        table.getRow(0)
    } ?: return

    while (header.tableCells.size < 6) {
        header.createCell()
    }

    setWordCellText(header.getCell(0), "材料类型", true)
    setWordCellText(header.getCell(1), "安装编号", true)
    setWordCellText(header.getCell(2), "型号", true)
    setWordCellText(header.getCell(3), "质量类型", true)
    setWordCellText(header.getCell(4), "反馈说明", true)
    setWordCellText(header.getCell(5), "附图", true)
    setQualityTableColumnWidths(header)
    setWordTableRowHeight(header)

    if (rows.isEmpty()) {
        val row = table.createRow()
        while (row.tableCells.size < 6) {
            row.createCell()
        }

        repeat(6) { index ->
            setWordCellText(row.getCell(index), "", false)
        }

        setQualityTableColumnWidths(row)
        setWordTableRowHeight(row)
        return
    }

    rows.forEach { rowData ->
        val row = table.createRow()
        while (row.tableCells.size < 6) {
            row.createCell()
        }

        setWordCellText(row.getCell(0), rowData.materialType, false)
        setWordCellText(row.getCell(1), rowData.installNo, false)
        setWordCellText(row.getCell(2), rowData.model, false)
        setWordCellText(row.getCell(3), rowData.qualityType, false)
        setWordCellText(row.getCell(4), rowData.feedbackDesc, false)
        setQualityPhotoCell(row.getCell(5), rowData.photos)

        setQualityTableColumnWidths(row)
        setWordTableRowHeight(row)
    }
}


private fun MainActivity.applyQualityBlocksToTemplate(
    document: XWPFDocument,
    blocks: List<QualityExportBlock>
) {
    if (blocks.isEmpty()) return

    val holder = findCellParagraphByText(document, "\${QUALITY_BLOCKS}") ?: return
    val cell = holder.cell
    val startIndex = holder.paragraphIndex

    while (cell.paragraphs.size <= startIndex) {
        cell.addParagraph()
    }

    val startParagraph = cell.paragraphs[startIndex]
    clearParagraphOnly(startParagraph)

    blocks.forEachIndexed { index, block ->
        val paragraph = if (index == 0) {
            startParagraph
        } else {
            cell.addParagraph()
        }

        paragraph.alignment = ParagraphAlignment.LEFT
        clearParagraphOnly(paragraph)

        val run = paragraph.createRun()
        run.isBold = true
        run.fontSize = 12
        run.setText("质量问题描述：${block.buildingName}在铝模第${block.floorLabel}层施工过程中发现以下问题")

        insertSimpleQualityTableAtCellEnd(
            cell = cell,
            rows = block.rows
        )
    }

    // 所有楼栋数据写完后，再补尾部内容
    val phone = ""
    val username = SessionManager.getUsername(this).ifBlank { "" }
    val exportDate = SimpleDateFormat("yyyy-M-d", Locale.getDefault()).format(Date())

    // 先补足空白，让尾部内容尽量靠近单元格底部
    repeat(8) {
        cell.addParagraph()
    }

    val p1 = cell.addParagraph()
    p1.alignment = ParagraphAlignment.LEFT
    p1.createRun().apply {
        isBold = true
        fontSize = 12
        setText("原因分析：")
    }

    cell.addParagraph()

    val p2 = cell.addParagraph()
    p2.alignment = ParagraphAlignment.LEFT
    p2.createRun().apply {
        fontSize = 12
        setText("联系电话：$phone      陈述人：$username      日期：$exportDate")
    }

    cell.addParagraph()

    val p3 = cell.addParagraph()
    p3.alignment = ParagraphAlignment.LEFT
    p3.createRun().apply {
        isBold = true
        fontSize = 12
        setText("处理方案：")
    }

    cell.addParagraph()

    val p4 = cell.addParagraph()
    p4.alignment = ParagraphAlignment.LEFT
    p4.createRun().apply {
        isBold = true
        fontSize = 12
        setText("处理结果：")
    }

    cell.addParagraph()

    val p5 = cell.addParagraph()
    p5.alignment = ParagraphAlignment.LEFT
    p5.createRun().apply {
        isBold = true
        fontSize = 12
        setText("负责划分：")
    }

}

private data class CellParagraphHolder(
    val cell: XWPFTableCell,
    val paragraphIndex: Int
)

private fun findCellParagraphByText(
    document: XWPFDocument,
    target: String
): CellParagraphHolder? {
    document.tables.forEach { table ->
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEachIndexed { index, paragraph ->
                    val text = paragraph.text
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim()

                    if (text == target) {
                        return CellParagraphHolder(cell, index)
                    }
                }
            }
        }
    }
    return null
}



private fun clearParagraphOnly(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph) {
    while (paragraph.runs.size > 0) {
        paragraph.removeRun(0)
    }
}


private fun ensureFirstParagraph(cell: XWPFTableCell): org.apache.poi.xwpf.usermodel.XWPFParagraph {
    if (cell.paragraphs.isEmpty()) {
        cell.addParagraph()
    }
    return cell.paragraphs[0]
}



fun MainActivity.buildQualityFeedbackWordBytes(): ByteArray {
    saveCurrentBuildingScopeToMemory()
    runCatching {
        kotlinx.coroutines.runBlocking {
            saveCurrentProjectContentNow()
        }
    }

    val exportDate = SimpleDateFormat("yyyy-M-d", Locale.getDefault()).format(Date())
    val username = SessionManager.getUsername(this).ifBlank { "" }
    val phone = ""

    assets.open("quality_feedback_template.docx").use { input ->
        val document = XWPFDocument(input)

        replaceWordText(document, "\${PROJECT_NAME}", currentProjectName)

// 先把模板里原来的这一行清空，避免它提前显示
        replaceWordText(document, "\${PHONE}", "")
        replaceWordText(document, "\${USERNAME}", "")
        replaceWordText(document, "\${EXPORT_DATE}", "")


        val blocks = collectQualityFeedbackExportBlocks()

        if (blocks.isNotEmpty()) {
            applyQualityBlocksToTemplate(document, blocks)
        }

        return ByteArrayOutputStream().use { output ->
            document.write(output)
            document.close()
            output.toByteArray()
        }
    }
}

private fun setCellWidthCm(cell: XWPFTableCell, cm: Double) {
    val dxa = (cm * 567).toInt()   // 1cm ≈ 567 dxa
    val tc = cell.ctTc
    val tcPr = if (tc.isSetTcPr) tc.tcPr else tc.addNewTcPr()
    val tcW = if (tcPr.isSetTcW) tcPr.tcW else tcPr.addNewTcW()
    tcW.w = BigInteger.valueOf(dxa.toLong())
    tcW.type = org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA
}

private fun setQualityTableColumnWidths(row: org.apache.poi.xwpf.usermodel.XWPFTableRow) {
    while (row.tableCells.size < 6) {
        row.createCell()
    }

    row.getCell(0)?.let { setCellWidthCm(it, 2.0) }
    row.getCell(1)?.let { setCellWidthCm(it, 2.0) }
    row.getCell(2)?.let { setCellWidthCm(it, 3.0) }
    row.getCell(3)?.let { setCellWidthCm(it, 3.0) }
    row.getCell(4)?.let { setCellWidthCm(it, 6.0) }
    row.getCell(5)?.let { setCellWidthCm(it, 3.0) }
}

private fun replaceWordText(document: XWPFDocument, oldText: String, newText: String) {
    fun replaceInParagraph(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph) {
        val fullText = paragraph.text ?: return
        if (!fullText.contains(oldText)) return

        val replaced = fullText.replace(oldText, newText)

        while (paragraph.runs.size > 0) {
            paragraph.removeRun(0)
        }

        val run = paragraph.createRun()
        run.fontSize = 12
        run.setText(replaced)
    }

    document.paragraphs.forEach { paragraph ->
        replaceInParagraph(paragraph)
    }

    document.tables.forEach { table ->
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEach { paragraph ->
                    replaceInParagraph(paragraph)
                }
            }
        }
    }
}

private fun findMainQualityOuterTable(document: XWPFDocument): org.apache.poi.xwpf.usermodel.XWPFTable? {
    return document.tables.firstOrNull { table ->
        val row0 = table.getRow(0)
        val row1 = table.getRow(1)

        if (row0 == null || row1 == null) return@firstOrNull false

        val row0Text = row0.tableCells.joinToString("|") { it.text.trim() }
        val row1Text = row1.tableCells.joinToString("|") { it.text.trim() }

        row0Text.contains("投诉单位") &&
                row0Text.contains("项目/产品名称") &&
                row1Text.contains("质量问题描述")
    }
}

private fun setSingleCellParagraphText(
    cell: XWPFTableCell,
    text: String,
    bold: Boolean
) {
    while (cell.paragraphs.size > 0) {
        cell.removeParagraph(0)
    }

    val p = cell.addParagraph()
    p.alignment = ParagraphAlignment.LEFT

    val run = p.createRun()
    run.fontSize = 12
    run.isBold = bold
    run.setText(text)
}





private fun applyCellBorder(cell: XWPFTableCell) {
    val tc = cell.ctTc
    val tcPr = if (tc.isSetTcPr) tc.tcPr else tc.addNewTcPr()
    val borders = if (tcPr.isSetTcBorders) tcPr.tcBorders else tcPr.addNewTcBorders()

    val top = if (borders.isSetTop) borders.top else borders.addNewTop()
    top.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
    top.sz = BigInteger.valueOf(8)
    top.color = "000000"

    val bottom = if (borders.isSetBottom) borders.bottom else borders.addNewBottom()
    bottom.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
    bottom.sz = BigInteger.valueOf(8)
    bottom.color = "000000"

    val left = if (borders.isSetLeft) borders.left else borders.addNewLeft()
    left.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
    left.sz = BigInteger.valueOf(8)
    left.color = "000000"

    val right = if (borders.isSetRight) borders.right else borders.addNewRight()
    right.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
    right.sz = BigInteger.valueOf(8)
    right.color = "000000"
}



private data class QualityExportBlock(
    val buildingName: String,
    val floorLabel: String,
    val rows: List<QualityFeedbackRow>
)


private fun MainActivity.collectQualityFeedbackExportBlocks(): List<QualityExportBlock> {
    val result = mutableListOf<QualityExportBlock>()

    val latestProject = runCatching {
        kotlinx.coroutines.runBlocking {
            repository.getProjectById(currentProjectId)
        }
    }.getOrNull()

    val projectQualityContent = latestProject?.qualityContent.orEmpty()
    if (projectQualityContent.isBlank()) {
        return emptyList()
    }

    val (_, qualityMap) = parseBuildingScopedContent(
        projectQualityContent,
        currentBuildingName.ifBlank { "1号楼" }
    )

    qualityMap.forEach { (buildingName, raw) ->
        if (raw.isBlank()) return@forEach

        val parsed = parseQualityFeedbackExportContent(raw)
        val validRows = parsed.rows.filter { !it.isEmpty() }

        if (validRows.isNotEmpty()) {
            result.add(
                QualityExportBlock(
                    buildingName = buildingName,
                    floorLabel = parsed.floorLabel.ifBlank { "1" },
                    rows = validRows
                )
            )
        }
    }

    return result
}


private data class ParsedQualityExportContent(
    val floorLabel: String,
    val rows: List<QualityFeedbackRow>
)

private fun MainActivity.parseQualityFeedbackExportContent(content: String): ParsedQualityExportContent {
    if (content.isBlank()) {
        return ParsedQualityExportContent(
            floorLabel = "1",
            rows = emptyList()
        )
    }

    return try {
        val root = JSONObject(content)
        val floorLabel = root.optString("floorLabel", "1").ifBlank { "1" }

        val rows = mutableListOf<QualityFeedbackRow>()

        val rowsArray = root.optJSONArray("rows") ?: JSONArray()
        for (i in 0 until rowsArray.length()) {
            val obj = rowsArray.optJSONObject(i) ?: continue
            rows.add(parseQualityFeedbackRowForExport(obj))
        }

        val currentRowObj = root.optJSONObject("currentRow")
        if (currentRowObj != null) {
            val currentRow = parseQualityFeedbackRowForExport(currentRowObj)
            if (!currentRow.isEmpty()) {
                rows.add(currentRow)
            }
        }

        ParsedQualityExportContent(
            floorLabel = floorLabel,
            rows = rows
        )
    } catch (_: Exception) {
        ParsedQualityExportContent(
            floorLabel = "1",
            rows = emptyList()
        )
    }
}

private fun MainActivity.parseQualityFeedbackRowForExport(obj: JSONObject): QualityFeedbackRow {
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

private fun MainActivity.addWordLine(document: XWPFDocument, text: String) {
    val paragraph = document.createParagraph()
    paragraph.alignment = ParagraphAlignment.LEFT
    val run = paragraph.createRun()
    run.fontSize = 12
    run.setText(text)
}

private fun ensureTableHasFirstRow(table: org.apache.poi.xwpf.usermodel.XWPFTable): org.apache.poi.xwpf.usermodel.XWPFTableRow {
    return table.getRow(0) ?: table.createRow()
}

private fun setWordCellText(
    cell: XWPFTableCell,
    text: String,
    bold: Boolean
) {
    while (cell.paragraphs.size > 0) {
        cell.removeParagraph(0)
    }

    val p = cell.addParagraph()
    p.alignment = ParagraphAlignment.CENTER

    val run = p.createRun()
    run.fontSize = 11
    run.isBold = bold
    run.setText(text)

    cell.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER
    applyCellBorder(cell)
}


private fun setWordTableRowHeight(row: org.apache.poi.xwpf.usermodel.XWPFTableRow, height: Int = 567) {
    row.height = height
    val trPr = if (row.ctRow.isSetTrPr) row.ctRow.trPr else row.ctRow.addNewTrPr()
    val trHeight = if (trPr.sizeOfTrHeightArray() > 0) trPr.getTrHeightArray(0) else trPr.addNewTrHeight()
    trHeight.`val` = BigInteger.valueOf(height.toLong())
    trHeight.hRule = org.openxmlformats.schemas.wordprocessingml.x2006.main.STHeightRule.EXACT
}


private fun mergeCellsHorizontal(
    table: org.apache.poi.xwpf.usermodel.XWPFTable,
    row: Int,
    fromCell: Int,
    toCell: Int
) {
    for (cellIndex in fromCell..toCell) {
        val cell = table.getRow(row).getCell(cellIndex)
        val tcPr = cell.ctTc.addNewTcPr()
        if (cellIndex == fromCell) {
            tcPr.addNewHMerge().`val` =
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.RESTART
        } else {
            tcPr.addNewHMerge().`val` =
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.CONTINUE
        }
    }
}


private fun ensureCellCount(row: org.apache.poi.xwpf.usermodel.XWPFTableRow, count: Int) {
    while (row.tableCells.size < count) {
        row.addNewTableCell()
    }
}

private fun setSafeTableWidth(table: org.apache.poi.xwpf.usermodel.XWPFTable) {
    try {
        val ctTbl = table.ctTbl
        val tblPr = if (ctTbl.tblPr != null) ctTbl.tblPr else ctTbl.addNewTblPr()
        val tblW = if (tblPr.tblW != null) tblPr.tblW else tblPr.addNewTblW()
        tblW.w = BigInteger.valueOf(9000)
        tblW.type = org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.DXA
    } catch (_: Exception) {
    }
}


private fun setTableCellText(cell: org.apache.poi.xwpf.usermodel.XWPFTableCell, text: String) {
    cell.removeParagraph(0)
    val p = cell.addParagraph()
    p.alignment = ParagraphAlignment.CENTER
    val run = p.createRun()
    run.fontSize = 11
    run.setText(text)

    cell.verticalAlignment = org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign.CENTER

    val tc = cell.ctTc
    val tcPr = if (tc.isSetTcPr) tc.tcPr else tc.addNewTcPr()

    val width = if (tcPr.isSetTcW) tcPr.tcW else tcPr.addNewTcW()
    width.w = BigInteger.valueOf(2400)

    val vAlign = if (tcPr.isSetVAlign) tcPr.vAlign else tcPr.addNewVAlign()
    vAlign.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc.CENTER
}

private fun MainActivity.setQualityPhotoCell(
    cell: XWPFTableCell,
    photos: List<QualityFeedbackPhotoItem>
) {
    while (cell.paragraphs.size > 0) {
        cell.removeParagraph(0)
    }

    val p = cell.addParagraph()
    p.alignment = ParagraphAlignment.CENTER
    cell.verticalAlignment = XWPFTableCell.XWPFVertAlign.CENTER

    if (photos.isEmpty()) {
        val run = p.createRun()
        run.setText("")
        applyCellBorder(cell)
        return
    }

    photos.forEachIndexed { index, photo ->
        val imageInput = openQualityPhotoInputStream(photo) ?: return@forEachIndexed
        val imageBytes = imageInput.use { it.readBytes() }

        val size = calculateWordImageSize(
            imageBytes = imageBytes,
            maxWidthPx = 18,
            maxHeightPx = 18
        )

        val run = p.createRun()
        try {
            run.addPicture(
                imageBytes.inputStream(),
                getPictureType(photo),
                "quality_${index}.jpg",
                Units.toEMU(size.first.toDouble()),
                Units.toEMU(size.second.toDouble())
            )
        } catch (_: Exception) {
            run.setText("[图]")
        }

        if (index != photos.lastIndex) {
            run.addTab()
        }
    }

    applyCellBorder(cell)
}

private fun ensureCellHasParagraph(cell: XWPFTableCell) {
    if (cell.paragraphs.isEmpty()) {
        cell.addParagraph()
    }
}


private fun MainActivity.openQualityPhotoInputStream(photo: QualityFeedbackPhotoItem): InputStream? {
    return try {
        when {
            photo.uriString.isNotBlank() -> {
                contentResolver.openInputStream(Uri.parse(photo.uriString))
            }
            photo.localPath.isNotBlank() -> {
                val file = java.io.File(photo.localPath)
                if (file.exists()) file.inputStream() else null
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun findCellByExactText(
    document: XWPFDocument,
    target: String
): XWPFTableCell? {
    val expect = target
        .replace("\n", "")
        .replace("\r", "")
        .replace(" ", "")
        .trim()

    document.tables.forEach { table ->
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                val raw = cell.text
                    .replace("\n", "")
                    .replace("\r", "")
                    .replace(" ", "")
                    .trim()

                if (raw.contains(expect)) {
                    return cell
                }
            }
        }
    }
    return null
}

private fun findRowByCellText(
    document: XWPFDocument,
    target: String
): org.apache.poi.xwpf.usermodel.XWPFTableRow? {
    document.tables.forEach { table ->
        table.rows.forEach { row ->
            if (row.tableCells.any { it.text.trim() == target }) {
                return row
            }
        }
    }
    return null
}

private fun MainActivity.calculateWordImageSize(
    imageBytes: ByteArray,
    maxWidthPx: Int,
    maxHeightPx: Int
): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    val srcWidth = options.outWidth.takeIf { it > 0 } ?: maxWidthPx
    val srcHeight = options.outHeight.takeIf { it > 0 } ?: maxHeightPx

    val widthRatio = maxWidthPx.toFloat() / srcWidth.toFloat()
    val heightRatio = maxHeightPx.toFloat() / srcHeight.toFloat()
    val ratio = minOf(widthRatio, heightRatio, 1f)

    val finalWidth = (srcWidth * ratio).toInt().coerceAtLeast(1)
    val finalHeight = (srcHeight * ratio).toInt().coerceAtLeast(1)

    return finalWidth to finalHeight
}
private fun MainActivity.getPictureType(photo: QualityFeedbackPhotoItem): Int {
    val raw = (photo.uriString + photo.localPath).lowercase(Locale.getDefault())
    return when {
        raw.endsWith(".png") -> XWPFDocument.PICTURE_TYPE_PNG
        raw.endsWith(".gif") -> XWPFDocument.PICTURE_TYPE_GIF
        raw.endsWith(".bmp") -> XWPFDocument.PICTURE_TYPE_BMP
        raw.endsWith(".webp") -> XWPFDocument.PICTURE_TYPE_JPEG
        else -> XWPFDocument.PICTURE_TYPE_JPEG
    }
}


private fun MainActivity.insertQualityBlockToCell(
    cell: XWPFTableCell,
    buildingName: String,
    floorLabel: String,
    rows: List<QualityFeedbackRow>,
    showDesc: Boolean
) {
    if (showDesc) {
        val p = cell.addParagraph()
        p.alignment = ParagraphAlignment.LEFT
        p.createRun().apply {
            isBold = true
            fontSize = 12
            setText("质量问题描述：${buildingName}在铝模第${floorLabel}层施工过程中发现以下问题")
        }
    }

    ensureCellHasParagraph(cell)
    val cursor: XmlCursor = cell.paragraphs.last().ctp.newCursor()
    val table = cell.insertNewTbl(cursor)

    setSafeTableWidth(table)
    table.setTableAlignment(TableRowAlign.CENTER)

    val header = ensureTableHasFirstRow(table)
    ensureCellCount(header, 6)
    setWordCellText(header.getCell(0), "材料类型", true)
    setWordCellText(header.getCell(1), "安装编号", true)
    setWordCellText(header.getCell(2), "型号", true)
    setWordCellText(header.getCell(3), "质量类型", true)
    setWordCellText(header.getCell(4), "反馈说明", true)
    setWordCellText(header.getCell(5), "附图", true)

    if (rows.isEmpty()) {
        val row = table.createRow()
        ensureCellCount(row, 6)
        repeat(6) { index ->
            setWordCellText(row.getCell(index), "", false)
        }
        setWordTableRowHeight(row, 900)
        return
    }

    rows.forEachIndexed { index, rowData ->
        android.util.Log.e(
            "QF_EXPORT",
            "row[$index] material=${rowData.materialType}, install=${rowData.installNo}, model=${rowData.model}, quality=${rowData.qualityType}, desc=${rowData.feedbackDesc}, photos=${rowData.photos.size}"
        )

        val row = table.createRow()
        ensureCellCount(row, 6)

        setWordCellText(row.getCell(0), rowData.materialType, false)
        setWordCellText(row.getCell(1), rowData.installNo, false)
        setWordCellText(row.getCell(2), rowData.model, false)
        setWordCellText(row.getCell(3), rowData.qualityType, false)
        setWordCellText(row.getCell(4), rowData.feedbackDesc, false)
        setQualityPhotoCell(row.getCell(5), rowData.photos)

        setWordTableRowHeight(row, 900)
    }

    cell.addParagraph()
}
