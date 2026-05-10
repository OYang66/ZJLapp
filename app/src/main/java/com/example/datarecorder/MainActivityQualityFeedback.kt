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

	lateinit var dialog: android.app.Dialog
	val content = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		items.forEachIndexed { index, value ->
			addView(
				createDialogListItem(
					label = value,
					accent = index < 2,
					onClick = {
						applyQualityMaterialTypeSelection(savedRowIndex, value)
						dialog.dismiss()
					}
				),
				LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					if (index > 0) topMargin = dp(8)
				}
			)
		}
	}

	dialog = createCardDialog(
		title = "选择材料类型",
		subtitle = "点击后写入当前质量反馈行"
	) { dlg ->
		addView(wrapDialogScroll(content, maxHeightDp = 460))
		addView(
			createDialogActionButton("关闭", primary = false) {
				dlg.dismiss()
			},
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(42)
			).apply {
				topMargin = dp(16)
			}
		)
	}
	dialog.show()
}

fun MainActivity.showQualityTypeDialog(savedRowIndex: Int?) {
	lateinit var dialog: android.app.Dialog
	val content = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		QualityTypeOption.items.forEachIndexed { index, value ->
			addView(
				createDialogListItem(
					label = value,
					accent = index == 0,
					onClick = {
						applyQualityTypeSelection(savedRowIndex, value)
						dialog.dismiss()
					}
				),
				LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					if (index > 0) topMargin = dp(8)
				}
			)
		}
	}

	dialog = createCardDialog(
		title = "选择质量类型",
		subtitle = "点击后写入当前质量反馈行"
	) { dlg ->
		addView(wrapDialogScroll(content, maxHeightDp = 360))
		addView(
			createDialogActionButton("关闭", primary = false) {
				dlg.dismiss()
			},
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(42)
			).apply {
				topMargin = dp(16)
			}
		)
	}
	dialog.show()
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

	val dialog = createCardDialog(
		title = "编辑反馈说明",
		subtitle = "支持清空后重新填写"
	) { dlg ->
		addView(createDialogSectionTitle("反馈内容"))
		addView(
			input,
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(180)
			)
		)
		addView(
			createDialogActionButton("清空", primary = false) {
				input.setText("")
			},
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(42)
			).apply {
				topMargin = dp(12)
			}
		)
		addView(
			createDialogActionRow(
				dialog = dlg,
				confirmText = "确认",
				onConfirm = {
					targetRow.feedbackDesc = input.text.toString().trim()
					renderQualityFeedbackTable()
					triggerAutoSaveDebounced()
					dlg.dismiss()
				}
			),
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = dp(12)
			}
		)
	}

	dialog.show()
	input.requestFocus()
}


fun MainActivity.showQualityPhotoMenu(savedRowIndex: Int?) {
	pendingQualityPhotoRowIndex = savedRowIndex

	lateinit var dialog: android.app.Dialog
	val items = listOf(
		Triple("拍照", "打开相机新增附图", false),
		Triple("从相册添加", "从系统相册选择图片", false),
		Triple("删除", "移除当前行已选附图", true)
	)
	val content = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		items.forEachIndexed { index, item ->
			addView(
				createDialogListItem(
					label = item.first,
					subtitle = item.second,
					danger = item.third,
					onClick = {
						when (index) {
							0 -> openQualityCamera()
							1 -> openQualityGallery()
							2 -> deleteQualityPhotos(savedRowIndex)
						}
						dialog.dismiss()
					}
				),
				LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					if (index > 0) topMargin = dp(8)
				}
			)
		}
	}

	dialog = createCardDialog(
		title = "附图",
		subtitle = "选择图片操作方式"
	) { dlg ->
		addView(content)
		addView(
			createDialogActionButton("关闭", primary = false) {
				dlg.dismiss()
			},
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				dp(42)
			).apply {
				topMargin = dp(16)
			}
		)
	}
	dialog.show()
}


fun MainActivity.showQualityFloorInputDialog() {
	val input = EditText(this).apply {
		setText(currentQualityFloorLabel.ifBlank { "1" })
		setSelection(text.length)
		hint = "请输入层数"
		inputType = android.text.InputType.TYPE_CLASS_NUMBER
		setPadding(dp(12), dp(12), dp(12), dp(12))
		textSize = 16f
		background = createCellBackground(0xFFF8F5FF.toInt(), 0xFFE4DAFF.toInt(), 1, 12f)
	}

	createCardDialog(
		title = "输入铝模层数",
		subtitle = "当前质量反馈模式下的楼层标签"
	) { dlg ->
		addView(createDialogSectionTitle("层数"))
		addView(input)
		addView(
			createDialogActionRow(
				dialog = dlg,
				confirmText = "确定",
				onConfirm = {
					val value = input.text.toString().trim()
					if (value.isBlank()) {
						toast("请输入层数")
						return@createDialogActionRow
					}

					currentQualityFloorLabel = value
					updatePackageButtonText()
					triggerAutoSaveDebounced()
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
	triggerAutoSaveDebounced()
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

	updateDisplayTable()
	triggerAutoSaveDebounced()
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

	updateDisplayTable()
	triggerAutoSaveDebounced()
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

	updateDisplayTable()
	triggerAutoSaveDebounced()
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

	updateDisplayTable()
	triggerAutoSaveDebounced()
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

	updateDisplayTable()
	triggerAutoSaveDebounced()

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

	updateDisplayTable()
	triggerAutoSaveDebounced()
}

private fun MainActivity.deleteQualityPhotos(rowIndex: Int?) {
	val targetRow = getTargetQualityRow(rowIndex) ?: return
	targetRow.photos.clear()
	updateDisplayTable()
	triggerAutoSaveDebounced()
}
