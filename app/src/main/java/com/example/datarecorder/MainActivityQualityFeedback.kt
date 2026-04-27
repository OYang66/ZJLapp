package com.example.datarecorder

import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog
import java.io.File

// =========================
// 质量反馈
// =========================
fun MainActivity.renderQualityFeedbackTable() {
    tableHeader.removeAllViews()
    tableBody.removeAllViews()

    addQualityFeedbackTableHeader()


    if (qualityRows.isEmpty() && currentQualityRow.isEmpty()) {
        addQualityFeedbackDataRow(
            data = QualityFeedbackRow(),
            isCurrentRow = true,
            savedRowIndex = null
        )
    } else {
        qualityRows.forEachIndexed { index, row ->
            addQualityFeedbackDataRow(
                data = row,
                isCurrentRow = false,
                savedRowIndex = index
            )
        }

        addQualityFeedbackDataRow(
            data = currentQualityRow,
            isCurrentRow = true,
            savedRowIndex = null
        )
    }

    tvSummaryPrimary.visibility = View.GONE
    tvSummarySecondary.visibility = View.GONE
}

fun MainActivity.addQualityFeedbackDataRow(
    data: QualityFeedbackRow,
    isCurrentRow: Boolean,
    savedRowIndex: Int? = null
) {
    tableBody.addView(
        buildQualityFeedbackRowView(
            data = data,
            isCurrentRow = isCurrentRow,
            savedRowIndex = savedRowIndex
        )
    )
}

private fun MainActivity.buildQualityFeedbackRowView(
    data: QualityFeedbackRow,
    isCurrentRow: Boolean,
    savedRowIndex: Int? = null
): TableRow {
    val row = TableRow(this)

    val materialTypeCell = createTableCell(
        text = data.materialType,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.MATERIAL_TYPE),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.MATERIAL_TYPE)
            showQualityMaterialTypeDialog(savedRowIndex)
        }
    ).apply {
        maxLines = 1
        minLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }


    row.addView(materialTypeCell)

    val installNoCell = createTableCell(
        text = data.installNo,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.INSTALL_NO),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.INSTALL_NO)
        }
    )
    row.addView(installNoCell)

    val modelCell = createTableCell(
        text = data.model,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.MODEL),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.MODEL)
        }
    )
    row.addView(modelCell)

    val qualityTypeCell = createTableCell(
        text = data.qualityType,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.QUALITY_TYPE),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.QUALITY_TYPE)
            showQualityTypeDialog(savedRowIndex)
        }
    ).apply {
        maxLines = 1
        minLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }


    row.addView(qualityTypeCell)

    val feedbackDescCell = createTableCell(
        text = data.feedbackDesc,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.FEEDBACK_DESC),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.FEEDBACK_DESC)
            showQualityDescEditorDialog(savedRowIndex)
        }
    ).apply {
        gravity = Gravity.CENTER_VERTICAL
        textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        setPadding(dp(6), dp(5), dp(6), dp(5))
        maxLines = 1
        minLines = 1
        minHeight = dp(30)
        ellipsize = android.text.TextUtils.TruncateAt.END
    }


    row.addView(feedbackDescCell)

    val photoCountText = if (data.photos.isEmpty()) "" else "${data.photos.size}张"

    val photosCell = createTableCell(
        text = photoCountText,
        isHeader = false,
        highlight = isCurrentRow,
        selected = isQualityFieldSelected(savedRowIndex, QualityFeedbackField.PHOTOS),
        onClick = {
            selectQualityField(savedRowIndex, QualityFeedbackField.PHOTOS)
            showQualityPhotoMenu(savedRowIndex)
        }
    )
    row.addView(photosCell)

    return row
}

private fun MainActivity.isQualityFieldSelected(
    savedRowIndex: Int?,
    field: QualityFeedbackField
): Boolean {
    return if (savedRowIndex != null) {
        editingQualityRowIndex == savedRowIndex && editingQualityField == field
    } else {
        editingQualityRowIndex == null && editingQualityField == field
    }
}

private fun MainActivity.selectQualityField(
    savedRowIndex: Int?,
    field: QualityFeedbackField
) {
    editingQualityRowIndex = savedRowIndex
    editingQualityField = field
    renderQualityFeedbackTable()
}

fun MainActivity.showQualityMaterialTypeDialog(savedRowIndex: Int?) {
    val items = listOf(
        "铝模板",
        "钢模板",
        "背楞", "吊架", "单支撑", "销钉", "销片", "销钉销片", "四方垫片",
        "对拉螺母", "对拉螺杆", "斜撑", "圆管", "调节底座", "码仔", "K板螺丝",
        "T字螺杆", "放线盒", "上料箱", "泵管盒", "方通扣", "回型钩", "凳子",
        "拉片小斜撑", "背楞接头", "铁钩铁锤", "其他铁件"
    )

    AlertDialog.Builder(this)
        .setTitle("选择材料类型")
        .setItems(items.toTypedArray()) { _, which ->
            val value = items[which]
            applyQualityMaterialTypeSelection(savedRowIndex, value)
        }
        .setNegativeButton("取消", null)
        .show()
}

fun MainActivity.showQualityTypeDialog(savedRowIndex: Int?) {
    AlertDialog.Builder(this)
        .setTitle("选择质量类型")
        .setItems(QualityTypeOption.items.toTypedArray()) { _, which ->
            val value = QualityTypeOption.items[which]
            applyQualityTypeSelection(savedRowIndex, value)
        }
        .setNegativeButton("取消", null)
        .show()
}


fun MainActivity.showQualityDescEditorDialog(savedRowIndex: Int?) {
    val targetRow = if (savedRowIndex != null) {
        qualityRows.getOrNull(savedRowIndex) ?: return
    } else {
        currentQualityRow
    }

    val input = EditText(this).apply {
        setText(targetRow.feedbackDesc)
        setSelection(text.length)
        minLines = 6
        gravity = Gravity.TOP or Gravity.START
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setTextColor(0xFF222222.toInt())
        textSize = 15f
        background = createCellBackground(
            fillColor = 0xFFFFFFFF.toInt(),
            strokeColor = 0xFFDCE3EF.toInt(),
            strokeWidthDp = 1,
            cornerRadiusDp = 8f
        )
    }

    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(4))
        addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(180)
            )
        )
    }

    val dialog = AlertDialog.Builder(this)
        .setTitle("编辑反馈说明")
        .setView(container)
        .setPositiveButton("确认", null)
        .setNeutralButton("清空", null)
        .setNegativeButton("取消", null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            targetRow.feedbackDesc = input.text.toString().trim()
            renderQualityFeedbackTable()
            triggerAutoSave()
            dialog.dismiss()
        }

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            input.setText("")
        }

        input.requestFocus()
    }

    dialog.show()
}


fun MainActivity.showQualityPhotoMenu(savedRowIndex: Int?) {
    pendingQualityPhotoRowIndex = savedRowIndex

    val items = arrayOf("拍照", "从相册添加", "删除")

    AlertDialog.Builder(this)
        .setTitle("附图")
        .setItems(items) { _, which ->
            when (which) {
                0 -> openQualityCamera()
                1 -> openQualityGallery()
                2 -> deleteQualityPhotos(savedRowIndex)
            }
        }
        .setNegativeButton("取消", null)
        .show()
}


fun MainActivity.showQualityFloorInputDialog() {
    val input = EditText(this).apply {
        setText(currentQualityFloorLabel.ifBlank { "1" })
        setSelection(text.length)
        hint = "请输入层数"
        inputType = android.text.InputType.TYPE_CLASS_NUMBER
        setPadding(dp(12), dp(12), dp(12), dp(12))
        textSize = 16f
    }

    AlertDialog.Builder(this)
        .setTitle("输入铝模层数")
        .setView(input)
        .setNegativeButton("取消", null)
        .setPositiveButton("确定") { _, _ ->
            val value = input.text.toString().trim()
            if (value.isBlank()) {
                toast("请输入层数")
                return@setPositiveButton
            }

            currentQualityFloorLabel = value
            updatePackageButtonText()
            triggerAutoSave()
        }
        .show()
}


fun MainActivity.initQualityModeArea() {
    val textSize = getStandardGroupButtonTextSize()

    bindAdaptiveFixedColumnButtons(layoutQualityWall, qualityWallKeys, 4, textSize) { value ->
        appendQualityText(value)
    }

    bindAdaptiveFixedColumnButtons(layoutQualityBeam, qualityBeamKeys, 4, textSize) { value ->
        appendQualityText(value)
    }

    bindAdaptiveFixedColumnButtons(layoutQualitySlab, qualitySlabKeys, 4, textSize) { value ->
        appendQualityText(value)
    }

    bindAdaptiveFixedColumnButtons(layoutQualityStair, qualityStairKeys, 4, textSize) { value ->
        appendQualityText(value)
    }
}

private fun MainActivity.addQualityFeedbackTableHeader() {
    val row = TableRow(this)
    row.addView(createTableCell("材料类型", true))
    row.addView(createTableCell("安装编号", true))
    row.addView(createTableCell("型号", true))
    row.addView(createTableCell("质量类型", true))
    row.addView(createTableCell("反馈说明", true))
    row.addView(createTableCell("附图", true))
    tableHeader.addView(row)
}
private fun MainActivity.applyQualityMaterialTypeSelection(
    savedRowIndex: Int?,
    value: String
) {
    val targetRow = if (savedRowIndex != null) {
        qualityRows.getOrNull(savedRowIndex) ?: return
    } else {
        currentQualityRow
    }

    targetRow.materialType = value

    if (targetRow.qualityType.isNotBlank()) {
        targetRow.feedbackDesc = buildQualityFeedbackDesc(
            materialType = targetRow.materialType,
            qualityType = targetRow.qualityType
        )
    }

    renderQualityFeedbackTable()
    triggerAutoSave()
}

private fun MainActivity.applyQualityTypeSelection(
    savedRowIndex: Int?,
    value: String
) {
    val targetRow = if (savedRowIndex != null) {
        qualityRows.getOrNull(savedRowIndex) ?: return
    } else {
        currentQualityRow
    }

    targetRow.qualityType = value
    targetRow.feedbackDesc = buildQualityFeedbackDesc(
        materialType = targetRow.materialType,
        qualityType = value
    )

    renderQualityFeedbackTable()
    triggerAutoSave()
}

private fun buildQualityFeedbackDesc(
    materialType: String,
    qualityType: String
): String {
    return when (qualityType) {
        "材料变形" -> "材料施工中变形，影响成型质量，需更换。"
        "材料脱焊" -> "材料施工中脱焊，影响成型质量，需更换。"
        "材料缺失" -> "材料拆包未找到，影响拼装进度，需补发。"
        "尺寸错误" -> "材料实际尺寸与图纸不符，影响拼装进度，需补发。"
        "材料增补" -> "材料数量不满足现场施工需求，“${materialType.ifBlank { "材料" }}”根据现场施工统计需增加***。"
        "设计错误" -> "材料设计错误，影响拼装进度，需补发。"
        "深化错误" -> "深化错误，影响拼装进度，需补发。"
        "生产错误" -> "材料生产错误，影响拼装进度，需补发。"
        else -> ""
    }
}

fun MainActivity.initQualityInputButtons() {

    findViewById<Button>(R.id.btnQualityPlus).setOnClickListener {
        showSymbolPopup(it) { symbol ->
            appendQualityText(symbol)
        }
    }





    findViewById<Button>(R.id.btnQualityMultiply).setOnClickListener {
        appendQualityText("L")
    }

    findViewById<Button>(R.id.btnQualityBrackets).setOnClickListener {
        appendQualityText("Y")
    }

    findViewById<Button>(R.id.btnQualitySpace).setOnClickListener {
        appendQualityText(" ")
    }

    findViewById<Button>(R.id.btnQualityBackspace).setOnClickListener {
        deleteLastQualityInput()
    }

    findViewById<Button>(R.id.btnQualityA).setOnClickListener { appendQualityText("A") }
    findViewById<Button>(R.id.btnQualityB).setOnClickListener { appendQualityText("B") }
    findViewById<Button>(R.id.btnQualityC).setOnClickListener { appendQualityText("C") }
    findViewById<Button>(R.id.btnQualityD).setOnClickListener { appendQualityText("D") }
    findViewById<Button>(R.id.btnQualityE).setOnClickListener { appendQualityText("E") }
    findViewById<Button>(R.id.btnQualityF).setOnClickListener { appendQualityText("F") }

    findViewById<Button>(R.id.btnQuality0).setOnClickListener { appendQualityText("0") }
    findViewById<Button>(R.id.btnQuality1).setOnClickListener { appendQualityText("1") }
    findViewById<Button>(R.id.btnQuality2).setOnClickListener { appendQualityText("2") }
    findViewById<Button>(R.id.btnQuality3).setOnClickListener { appendQualityText("3") }
    findViewById<Button>(R.id.btnQuality4).setOnClickListener { appendQualityText("4") }
    findViewById<Button>(R.id.btnQuality5).setOnClickListener { appendQualityText("5") }
    findViewById<Button>(R.id.btnQuality6).setOnClickListener { appendQualityText("6") }
    findViewById<Button>(R.id.btnQuality7).setOnClickListener { appendQualityText("7") }
    findViewById<Button>(R.id.btnQuality8).setOnClickListener { appendQualityText("8") }
    findViewById<Button>(R.id.btnQuality9).setOnClickListener { appendQualityText("9") }
    findViewById<Button>(R.id.btnQuality00).setOnClickListener { appendQualityText("00") }
    findViewById<Button>(R.id.btnQualityDot).setOnClickListener { appendQualityText(".") }

    findViewById<Button>(R.id.btnQualityNextColumn).setOnClickListener {
        moveToNextQualityColumn()
    }

    findViewById<Button>(R.id.btnQualityNextRow).setOnClickListener {
        finishCurrentQualityRow()
    }
}

fun MainActivity.appendQualityText(text: String) {
    val targetRow = if (editingQualityRowIndex != null) {
        qualityRows.getOrNull(editingQualityRowIndex ?: -1) ?: return
    } else {
        currentQualityRow
    }

    when (editingQualityField) {
        QualityFeedbackField.INSTALL_NO -> {
            if (!canAppendToInstallNo(text) && text != " ") {
                toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
                return
            }
            targetRow.installNo += text
        }

        QualityFeedbackField.MODEL -> {
            targetRow.model = appendModelToken(targetRow.model, text)
        }

        else -> return
    }

    renderQualityFeedbackTable()
    triggerAutoSave()
}

fun MainActivity.deleteLastQualityInput() {
    val targetRow = if (editingQualityRowIndex != null) {
        qualityRows.getOrNull(editingQualityRowIndex ?: -1) ?: return
    } else {
        currentQualityRow
    }

    fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

    when (editingQualityField) {
        QualityFeedbackField.INSTALL_NO -> {
            targetRow.installNo = cutLast(targetRow.installNo)
        }

        QualityFeedbackField.MODEL -> {
            targetRow.model = cutLast(targetRow.model)
        }

        else -> return
    }

    renderQualityFeedbackTable()
    triggerAutoSave()
}

fun MainActivity.moveToNextQualityColumn() {
    editingQualityField = when (editingQualityField) {
        QualityFeedbackField.MATERIAL_TYPE -> QualityFeedbackField.INSTALL_NO
        QualityFeedbackField.INSTALL_NO -> QualityFeedbackField.MODEL
        QualityFeedbackField.MODEL -> QualityFeedbackField.QUALITY_TYPE
        QualityFeedbackField.QUALITY_TYPE -> QualityFeedbackField.FEEDBACK_DESC
        QualityFeedbackField.FEEDBACK_DESC -> QualityFeedbackField.PHOTOS
        QualityFeedbackField.PHOTOS -> QualityFeedbackField.MATERIAL_TYPE
    }

    renderQualityFeedbackTable()
    triggerAutoSave()
}


fun MainActivity.finishCurrentQualityRow() {
    if (!currentQualityRow.isEmpty()) {
        qualityRows.add(
            currentQualityRow.copy(
                photos = currentQualityRow.photos.map { it.copy() }.toMutableList()
            )
        )
        currentQualityRow = QualityFeedbackRow()
    }

    editingQualityRowIndex = null
    editingQualityField = QualityFeedbackField.MATERIAL_TYPE

    renderQualityFeedbackTable()
    triggerAutoSave()

    bodyVerticalScroll.post {
        bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
    }
}

private fun MainActivity.getTargetQualityRow(rowIndex: Int?): QualityFeedbackRow? {
    return if (rowIndex != null) {
        qualityRows.getOrNull(rowIndex)
    } else {
        currentQualityRow
    }
}

private fun MainActivity.openQualityCamera() {
    val imageFile = createQualityTempImageFile()
    val uri = androidx.core.content.FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        imageFile
    )
    pendingQualityPhotoUri = uri
    qualityPhotoCameraLauncher.launch(uri)
}

private fun MainActivity.openQualityGallery() {
    qualityPhotoGalleryLauncher.launch(arrayOf("image/*"))
}

private fun MainActivity.createQualityTempImageFile(): File {
    val dir = File(cacheDir, "quality_feedback_images")
    if (!dir.exists()) {
        dir.mkdirs()
    }

    val fileName = "qf_${System.currentTimeMillis()}.jpg"
    return File(dir, fileName)
}

fun MainActivity.addPhotoToQualityRow(rowIndex: Int?, uri: Uri) {
    val targetRow = getTargetQualityRow(rowIndex) ?: return

    targetRow.photos.add(
        QualityFeedbackPhotoItem(
            uriString = uri.toString(),
            localPath = "",
            createTime = System.currentTimeMillis()
        )
    )

    renderQualityFeedbackTable()
    triggerAutoSave()
}

private fun MainActivity.deleteQualityPhotos(rowIndex: Int?) {
    val targetRow = getTargetQualityRow(rowIndex) ?: return
    targetRow.photos.clear()
    renderQualityFeedbackTable()
    triggerAutoSave()
}
