package com.example.datarecorder

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

enum class FastVoiceUnit(
	val millimeterFactor: Double
) {
	MM(millimeterFactor = 1.0),
	CM(millimeterFactor = 10.0),
	M(millimeterFactor = 1000.0)
}

private data class FastVoiceParsedDimension(
	val valueInMillimeters: Double
)

private val fastVoiceDimensionFormatter = DecimalFormat("0.#")

// =========================
// 返厂统计
// =========================
fun MainActivity.initFastModeArea() {
	val presetTextSize = getFastPresetButtonTextSize()
	val customTextSize = getFastCustomButtonTextSize()
	val modelTextSize = getFastModelButtonTextSize()

	bindAdaptiveFixedColumnButtons(layoutFastTail, fastTailKeys, 5, presetTextSize) { value ->
		appendFastNumericToken(value)
	}

	bindAdaptiveFixedColumnButtons(layoutFastWidth, fastWidthKeys, 5, presetTextSize) { value ->
		appendFastNumericToken(value)
	}

	bindAdaptiveFixedColumnButtons(layoutFastLength, fastLengthKeys, 5, presetTextSize) { value ->
		appendFastNumericToken(value)
	}

	bindFastCustomArea(customTextSize)
	bindFastModelButtons(modelTextSize)
}

fun MainActivity.bindFastModelButtons(textSizeSp: Float) {
	stopFastVoiceButtonRecordingEffects(resetView = false, clearButtonReference = true)
	layoutFastModel.removeAllViews()
	layoutFastModel.columnCount = 4
	layoutFastModel.rowCount = 1

	fastModelKeys.forEachIndexed { index, value ->
		layoutFastModel.addView(
			createFastModelActionButton(
				text = value,
				row = 0,
				column = index,
				textSizeSp = textSizeSp
			) {
				appendFastModelToken(value)
			}
		)
	}

	layoutFastModel.addView(
		createFastVoiceHoldButton(
			row = 0,
			column = fastModelKeys.size,
			textSizeSp = textSizeSp
		)
	)
}

private fun MainActivity.createFastModelActionButton(
	text: String,
	row: Int,
	column: Int,
	textSizeSp: Float,
	onClick: () -> Unit
): Button {
	return Button(this).apply {
		this.text = text
		isAllCaps = false
		gravity = Gravity.CENTER
		textAlignment = View.TEXT_ALIGNMENT_CENTER
		includeFontPadding = false
		setPadding(dp(1), 0, dp(1), 0)
		minWidth = 0
		minHeight = 0
		minimumWidth = 0
		minimumHeight = 0
		setBackgroundResource(R.drawable.bg_key)
		setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
		maxLines = 1

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			setAutoSizeTextTypeUniformWithConfiguration(
				6,
				textSizeSp.toInt().coerceAtLeast(8),
				1,
				android.util.TypedValue.COMPLEX_UNIT_SP
			)
		}

		layoutParams = GridLayout.LayoutParams(
			GridLayout.spec(row, 1, 1f),
			GridLayout.spec(column, 1, 1f)
		).apply {
			width = 0
			height = 0
			setGravity(Gravity.FILL)
			setMargins(dp(1), dp(1), dp(1), dp(1))
		}

		setOnClickListener { onClick() }
	}
}

private fun MainActivity.createFastVoiceHoldButton(
	row: Int,
	column: Int,
	textSizeSp: Float
): Button {
	return createFastModelActionButton(
		text = "",
		row = row,
		column = column,
		textSizeSp = textSizeSp,
		onClick = {}
	).apply {
		contentDescription = "麦克风"
		setBackgroundResource(R.drawable.bg_fast_voice_button)
		setPadding(0, 0, 0, 0)
		elevation = dpF(8f)
		fastVoiceHoldButton = this
		if (fastVoiceListening || fastVoiceWaitingResult) {
			post { startFastVoiceButtonRecordingEffects(this) }
		}
		setOnTouchListener { view, event ->
			fastVoiceHoldButton = view
			when (event.actionMasked) {
				MotionEvent.ACTION_DOWN -> {
					isPressed = true
					animateFastVoiceHoldButton(view, pressed = true)
					startFastVoiceHoldListening()
					true
				}

				MotionEvent.ACTION_MOVE -> {
					if (!isFastVoiceTouchInside(view, event)) {
						isPressed = false
						animateFastVoiceHoldButton(view, pressed = false)
						finishFastVoiceHoldListening(cancelRecognition = true, showTooShortToast = false)
					}
					true
				}

				MotionEvent.ACTION_UP -> {
					isPressed = false
					animateFastVoiceHoldButton(view, pressed = false)
					finishFastVoiceHoldListening(cancelRecognition = false, showTooShortToast = true)
					view.performClick()
					true
				}

				MotionEvent.ACTION_CANCEL -> {
					isPressed = false
					animateFastVoiceHoldButton(view, pressed = false)
					finishFastVoiceHoldListening(cancelRecognition = true, showTooShortToast = false)
					true
				}

				else -> false
			}
		}
	}
}

private fun MainActivity.animateFastVoiceHoldButton(view: View, pressed: Boolean) {
	view.animate()
		.scaleX(if (pressed) 0.92f else 1f)
		.scaleY(if (pressed) 0.92f else 1f)
		.alpha(if (pressed) 0.97f else 1f)
		.translationZ(if (pressed) dpF(3f) else dpF(8f))
		.setDuration(110L)
		.start()
}

fun MainActivity.startFastVoiceButtonRecordingEffects(resolvedView: View? = fastVoiceHoldButton) {
	val resolvedView = resolvedView ?: return
	val overlayHost = resolvedView.parent as? ViewGroup ?: return
	if (overlayHost.width == 0 || overlayHost.height == 0 || resolvedView.width == 0 || resolvedView.height == 0) {
		resolvedView.post { startFastVoiceButtonRecordingEffects(resolvedView) }
		return
	}
	if (fastVoiceButtonOverlayHost === overlayHost && fastVoiceButtonEffectDrawables.isNotEmpty() && fastVoiceHoldButton === resolvedView) {
		return
	}

	stopFastVoiceButtonRecordingEffects(resetView = false)
	fastVoiceHoldButton = resolvedView
	fastVoiceButtonOverlayHost = overlayHost

	val pulseDrawable = FastVoiceButtonPulseDrawable(resolvedView)
	val glowDrawable = FastVoiceButtonGlowDrawable(resolvedView)
	pulseDrawable.setBounds(0, 0, overlayHost.width, overlayHost.height)
	glowDrawable.setBounds(0, 0, overlayHost.width, overlayHost.height)
	overlayHost.overlay.add(pulseDrawable)
	overlayHost.overlay.add(glowDrawable)
	fastVoiceButtonEffectDrawables += pulseDrawable
	fastVoiceButtonEffectDrawables += glowDrawable

	val primaryPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
		duration = 1380L
		repeatCount = ValueAnimator.INFINITE
		interpolator = LinearInterpolator()
		addUpdateListener {
			pulseDrawable.primaryProgress = it.animatedValue as Float
		}
		start()
	}
	val secondaryPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
		duration = 1380L
		startDelay = 460L
		repeatCount = ValueAnimator.INFINITE
		interpolator = LinearInterpolator()
		addUpdateListener {
			pulseDrawable.secondaryProgress = it.animatedValue as Float
		}
		start()
	}
	val glowAnimator = ValueAnimator.ofFloat(0.38f, 1f).apply {
		duration = 920L
		repeatCount = ValueAnimator.INFINITE
		repeatMode = ValueAnimator.REVERSE
		interpolator = AccelerateDecelerateInterpolator()
		addUpdateListener {
			glowDrawable.intensity = it.animatedValue as Float
		}
		start()
	}
	fastVoiceButtonEffectAnimators += primaryPulseAnimator
	fastVoiceButtonEffectAnimators += secondaryPulseAnimator
	fastVoiceButtonEffectAnimators += glowAnimator
}

fun MainActivity.stopFastVoiceButtonRecordingEffects(
	resetView: Boolean = true,
	clearButtonReference: Boolean = false
) {
	fastVoiceButtonEffectAnimators.forEach { it.cancel() }
	fastVoiceButtonEffectAnimators.clear()
	fastVoiceButtonOverlayHost?.let { overlayHost ->
		fastVoiceButtonEffectDrawables.forEach { drawable ->
			overlayHost.overlay.remove(drawable)
		}
	}
	fastVoiceButtonEffectDrawables.clear()
	fastVoiceButtonOverlayHost = null
	if (resetView) {
		fastVoiceHoldButton?.invalidate()
	}
	if (clearButtonReference) {
		fastVoiceHoldButton = null
	}
}

private fun MainActivity.isFastVoiceTouchInside(view: View, event: MotionEvent): Boolean {
	return event.x >= 0f && event.x <= view.width.toFloat() && event.y >= 0f && event.y <= view.height.toFloat()
}

private class FastVoiceButtonPulseDrawable(
	private val hostView: View
) : Drawable() {
	private val density = hostView.resources.displayMetrics.density
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
	}

	var primaryProgress: Float = 0f
		set(value) {
			field = value
			invalidateSelf()
		}

	var secondaryProgress: Float = 0.5f
		set(value) {
			field = value
			invalidateSelf()
		}

	override fun draw(canvas: Canvas) {
		if (!hostView.isAttachedToWindow) return
		val overlayHost = hostView.parent as? ViewGroup ?: return
		if (bounds.width() != overlayHost.width || bounds.height() != overlayHost.height) {
			setBounds(0, 0, overlayHost.width, overlayHost.height)
		}
		val centerX = hostView.left + hostView.translationX + hostView.width / 2f
		val centerY = hostView.top + hostView.translationY + hostView.height / 2f
		drawRing(canvas, centerX, centerY, primaryProgress)
		drawRing(canvas, centerX, centerY, secondaryProgress)
	}

	private fun drawRing(canvas: Canvas, centerX: Float, centerY: Float, progress: Float) {
		val clamped = progress.coerceIn(0f, 1f)
		val baseRadius = max(hostView.width, hostView.height) * 0.48f
		val radius = baseRadius + (12f * density) + (18f * density * clamped)
		paint.color = Color.argb((92f * (1f - clamped)).roundToInt(), 154, 127, 255)
		paint.strokeWidth = (2.4f * density) - (0.8f * density * clamped)
		canvas.drawCircle(centerX, centerY, radius, paint)
	}

	override fun setAlpha(alpha: Int) = Unit

	override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

private class FastVoiceButtonGlowDrawable(
	private val hostView: View
) : Drawable() {
	private val density = hostView.resources.displayMetrics.density
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	var intensity: Float = 0.38f
		set(value) {
			field = value
			invalidateSelf()
		}

	override fun draw(canvas: Canvas) {
		if (!hostView.isAttachedToWindow) return
		val overlayHost = hostView.parent as? ViewGroup ?: return
		if (bounds.width() != overlayHost.width || bounds.height() != overlayHost.height) {
			setBounds(0, 0, overlayHost.width, overlayHost.height)
		}
		val centerX = hostView.left + hostView.translationX + hostView.width / 2f
		val centerY = hostView.top + hostView.translationY + hostView.height / 2f
		val baseRadius = max(hostView.width, hostView.height) * 0.54f
		val glowRadius = baseRadius + (10f * density) + (8f * density * intensity)
		val outerAlpha = (60 + 50 * intensity).roundToInt().coerceIn(0, 255)
		val middleAlpha = (22 + 28 * intensity).roundToInt().coerceIn(0, 255)
		paint.shader = RadialGradient(
			centerX,
			centerY,
			glowRadius,
			intArrayOf(
				Color.argb(outerAlpha, 197, 176, 255),
				Color.argb(middleAlpha, 124, 101, 235),
				Color.TRANSPARENT
			),
			floatArrayOf(0f, 0.58f, 1f),
			Shader.TileMode.CLAMP
		)
		canvas.drawCircle(centerX, centerY, glowRadius, paint)
	}

	override fun setAlpha(alpha: Int) = Unit

	override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit

	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

fun MainActivity.appendFastNumericToken(value: String) {
	if (!ensurePackageSelected()) return

	if (hasFastEditingTarget()) {
		val field = editingFastField ?: return

		if (field == FastField.QUANTITY && value == ".") {
			return
		}

		appendTextToEditingFastCell(value)
		return
	}

	if (currentFastActiveField == FastField.MODEL) {
		currentFastNumericField = resolveNextFastNumericFieldAfterModel(currentFastRow)
		currentFastActiveField = currentFastNumericField
		lastFastField = currentFastNumericField
	}

	when (currentFastNumericField) {
		FastField.WIDTH -> {
			val newValue = currentFastRow.width + value
			if (!isFastWidthValid(newValue)) {
				if (handleFastPresetReplaceOnLimit(FastField.WIDTH, value) {
						currentFastRow.width = it
					}) {
					return
				}
				showFastInputWarning()
				return
			}
			currentFastRow.width = newValue
			currentFastNumericField = FastField.WIDTH
			currentFastActiveField = FastField.WIDTH
			lastFastField = FastField.WIDTH
		}

		FastField.LENGTH -> {
			val newValue = currentFastRow.length + value
			if (!isFastLengthValid(newValue)) {
				if (handleFastPresetReplaceOnLimit(
						FastField.LENGTH,
						value
					) { currentFastRow.length = it }
				) {
					return
				}
				showFastInputWarning()
				return
			}
			currentFastRow.length = newValue
			currentFastNumericField = FastField.LENGTH
			currentFastActiveField = FastField.LENGTH
			lastFastField = FastField.LENGTH
		}

		FastField.QUANTITY -> {
			if (value == ".") return
			val newValue = currentFastRow.quantity + value
			if (!isFastQuantityValid(newValue)) {
				if (handleFastPresetReplaceOnLimit(
						FastField.QUANTITY,
						value
					) { currentFastRow.quantity = it }
				) {
					return
				}
				showFastInputWarning()
				return
			}
			currentFastRow.quantity = newValue
			currentFastNumericField = FastField.QUANTITY
			currentFastActiveField = FastField.QUANTITY
			lastFastField = FastField.QUANTITY
		}

		FastField.MODEL -> {
			currentFastNumericField = FastField.LENGTH
			currentFastActiveField = FastField.LENGTH
			lastFastField = FastField.LENGTH

			val newValue = currentFastRow.length + value
			if (!isFastLengthValid(newValue)) {
				if (handleFastPresetReplaceOnLimit(
						FastField.LENGTH,
						value
					) { currentFastRow.length = it }
				) {
					return
				}
				showFastInputWarning()
				return
			}
			currentFastRow.length = newValue
		}
	}

	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()

}

fun MainActivity.appendFastModelToken(value: String) {
	if (!ensurePackageSelected()) return

	if (hasFastEditingTarget()) {
		editingFastField = FastField.MODEL
		currentFastActiveField = FastField.MODEL
		lastFastField = FastField.MODEL
		appendTextToEditingFastCell(value)
		return
	}

	currentFastActiveField = FastField.MODEL
	lastFastField = FastField.MODEL

	currentFastRow.model = value
	pendingReplaceCurrentFastModel = false
	currentFastNumericField = resolveNextFastNumericFieldAfterModel(currentFastRow)

	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()

}

fun MainActivity.startFastVoiceInputWithPermissionCheck() {
	if (!ensurePackageSelected()) return

	if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
		startFastVoiceListening()
	} else {
		fastVoiceAwaitingPermission = true
		fastVoicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
	}
}

fun MainActivity.startFastVoiceListening() {
	if (!ensurePackageSelected()) return
	if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
		toast("未授予录音权限，无法使用语音识别")
		return
	}
	if (fastVoiceListening || fastVoiceWaitingResult) {
		toast("语音识别正在进行中")
		return
	}

	beginFastSparkVoiceListening(showListeningDialog = false)
}

fun MainActivity.parseFastVoiceSizePair(rawText: String): Pair<String, String>? {
	val normalized = normalizeFastVoiceText(rawText)
	val parts = splitFastVoiceSizeTokens(normalized) ?: return null
	val width = parseFastVoiceDimension(parts.first) ?: return null
	val length = parseFastVoiceDimension(parts.second) ?: return null
	return formatFastVoiceDimension(width.valueInMillimeters) to formatFastVoiceDimension(length.valueInMillimeters)
}

private fun MainActivity.normalizeFastVoiceText(rawText: String): String {
	return rawText
		.trim()
		.lowercase(Locale.getDefault())
		.replace(" ", "")
		.replace("　", "")
		.replace("乘以", "乘")
		.replace("乘号", "乘")
		.replace("成", "乘")
		.replace("＊", "*")
		.replace("×", "乘")
		.replace("*", "乘")
		.replace("x", "乘")
		.replace("公分", "厘米")
		.replace("cm", "厘米")
		.replace("mm", "毫米")
		.replace("m", "米")
}

private fun MainActivity.splitFastVoiceSizeTokens(normalized: String): Pair<String, String>? {
	val parts = normalized.split("乘").filter { it.isNotBlank() }
	if (parts.size != 2) {
		return null
	}
	return parts[0] to parts[1]
}

private fun MainActivity.parseFastVoiceDimension(token: String): FastVoiceParsedDimension? {
	return when {
		token.endsWith("毫米") -> {
			parseNumberToken(token.removeSuffix("毫米"))?.let { value ->
				FastVoiceParsedDimension(valueInMillimeters = value)
			}
		}

		token.endsWith("厘米") -> {
			parseNumberToken(token.removeSuffix("厘米"))?.let { value ->
				FastVoiceParsedDimension(valueInMillimeters = value * FastVoiceUnit.CM.millimeterFactor)
			}
		}

		token.contains("米") -> parseFastVoiceMeterToken(token)

		else -> {
			parseNumberToken(token)?.let { value ->
				FastVoiceParsedDimension(valueInMillimeters = value)
			}
		}
	}
}

private fun MainActivity.parseFastVoiceMeterToken(token: String): FastVoiceParsedDimension? {
	val meterParts = token.split("米")
	if (meterParts.size != 2) {
		return null
	}
	val majorPart = meterParts[0]
	val tailPart = meterParts[1]
	if (majorPart.isBlank()) {
		return null
	}
	if (tailPart.isNotBlank() && majorPart.contains('.')) {
		return null
	}

	val majorValue = parseNumberToken(majorPart) ?: return null
	val meterValue = if (tailPart.isBlank()) {
		majorValue
	} else {
		val decimalValue = parseNumberToken("0.$tailPart") ?: return null
		majorValue + decimalValue
	}
	return FastVoiceParsedDimension(
		valueInMillimeters = meterValue * FastVoiceUnit.M.millimeterFactor
	)
}

private fun MainActivity.parseNumberToken(token: String): Double? {
	if (token.isBlank() || token == ".") {
		return null
	}
	return token.toDoubleOrNull()
}

private fun MainActivity.formatFastVoiceDimension(valueInMillimeters: Double): String {
	val roundedValue = round(valueInMillimeters * 10.0) / 10.0
	return if (roundedValue % 1.0 == 0.0) {
		roundedValue.toInt().toString()
	} else {
		fastVoiceDimensionFormatter.format(roundedValue)
	}
}

fun MainActivity.applyFastVoiceSizeToCurrentRow(width: String, length: String) {
	if (hasFastEditingTarget()) {
		val oldRowIndex = editingFastRowIndex
		clearFastEditingState()
		currentFastNumericField = FastField.WIDTH
		currentFastActiveField = FastField.WIDTH
		lastFastField = FastField.WIDTH
		if (oldRowIndex != null) {
			rebuildFastRowOnly(oldRowIndex, false)
		}
	}

	if (!isFastWidthValid(width) || !isFastLengthValid(length)) {
		showFastInputWarning("识别成功，但尺寸超出当前 FAST 可录入范围")
		return
	}

	currentFastRow.width = width
	currentFastRow.length = length
	currentFastNumericField = resolveNextFastNumericFieldAfterModel(currentFastRow)
	currentFastActiveField = currentFastNumericField
	lastFastField = currentFastActiveField

	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()
	finishCurrentFastRow()
}

fun MainActivity.moveToNextFastColumn() {
	if (!ensurePackageSelected()) return

	if (hasFastEditingTarget()) {
		val oldRowIndex = editingFastRowIndex ?: return
		val oldField = editingFastField ?: return

		editingFastField = when (oldField) {
			FastField.WIDTH -> FastField.MODEL
			FastField.MODEL -> FastField.LENGTH
			FastField.LENGTH -> FastField.QUANTITY
			FastField.QUANTITY -> FastField.WIDTH
		}

		currentFastActiveField = editingFastField!!
		currentFastNumericField = when (editingFastField!!) {
			FastField.WIDTH -> FastField.WIDTH
			FastField.MODEL -> FastField.WIDTH
			FastField.LENGTH -> FastField.LENGTH
			FastField.QUANTITY -> FastField.QUANTITY
		}
		lastFastField = editingFastField!!
		pendingReplaceFastEditing = true

		refreshFastSelectionOnly(
			oldSavedRowIndex = oldRowIndex,
			oldWasCurrentRow = false,
			newSavedRowIndex = oldRowIndex,
			newWasCurrentRow = false
		)

		triggerAutoSaveDebounced()
		return
	}

	val oldField = currentFastActiveField

	currentFastNumericField = when (currentFastNumericField) {
		FastField.WIDTH -> FastField.LENGTH
		FastField.LENGTH -> FastField.QUANTITY
		FastField.QUANTITY -> FastField.WIDTH
		FastField.MODEL -> FastField.LENGTH
	}
	currentFastActiveField = currentFastNumericField
	lastFastField = currentFastNumericField

	if (oldField != currentFastActiveField) {
		refreshFastSelectionOnly(
			oldSavedRowIndex = null,
			oldWasCurrentRow = true,
			newSavedRowIndex = null,
			newWasCurrentRow = true
		)
	}

	triggerAutoSaveDebounced()
}


fun MainActivity.finishCurrentFastRow() {
	if (!ensurePackageSelected()) return

	if (hasFastEditingTarget()) {
		val oldRowIndex = editingFastRowIndex
		clearFastEditingState()
		currentFastNumericField = FastField.WIDTH
		currentFastActiveField = FastField.WIDTH
		lastFastField = FastField.WIDTH
		if (oldRowIndex != null) {
			rebuildFastRowOnly(oldRowIndex, false)
		}
		triggerAutoSaveDebounced()
		return
	}

	if (!currentFastRow.isEmpty()) {
		val savedIndex = savedFastRows.size
		savedFastRows.add(currentFastRow.copy())

		// 把原来的“当前行”改造成“已保存行”，不能再按 current row 重建
		rebuildFastRowOnly(savedIndex, false)

		currentFastRow = FastRow()
		currentFastNumericField = FastField.WIDTH
		currentFastActiveField = FastField.WIDTH
		lastFastField = FastField.WIDTH

		addFastDataRow(
			displayIndex = savedFastRows.size + 1,
			data = currentFastRow.copy(),
			isCurrentRow = true,
			savedRowIndex = null
		)

		tvSummaryPrimary.visibility = View.VISIBLE
		tvSummarySecondary.visibility = View.VISIBLE
		tvSummaryPrimary.text = "合计面积：${formatAreaSquareMeter(calculateFastTotalArea())}"
		tvSummarySecondary.text = "合计数量：${calculateFastTotalQty()}"

		bodyVerticalScroll.post {
			bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
		}

		triggerAutoSaveDebounced()
		return
	}

	currentFastNumericField = FastField.WIDTH
	currentFastActiveField = FastField.WIDTH
	lastFastField = FastField.WIDTH
	refreshFastSelectionOnly(null, true, null, true)
	triggerAutoSaveDebounced()
}

fun MainActivity.deleteLastFastInput() {
	if (!ensurePackageSelected()) return

	if (hasFastEditingTarget()) {
		deleteFromEditingFastCell()
		return
	}

	fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

	when (currentFastActiveField) {
		FastField.WIDTH -> {
			if (currentFastRow.width.isNotEmpty()) {
				currentFastRow.width = cutLast(currentFastRow.width)
				currentFastNumericField = FastField.WIDTH
				currentFastActiveField = FastField.WIDTH
				lastFastField = FastField.WIDTH
			}
		}

		FastField.MODEL -> {
			if (currentFastRow.model.isNotEmpty()) {
				currentFastRow.model = cutLast(currentFastRow.model)
				currentFastActiveField = FastField.MODEL
				lastFastField = FastField.MODEL
			}
		}

		FastField.LENGTH -> {
			if (currentFastRow.length.isNotEmpty()) {
				currentFastRow.length = cutLast(currentFastRow.length)
				currentFastNumericField = FastField.LENGTH
				currentFastActiveField = FastField.LENGTH
				lastFastField = FastField.LENGTH
			}
		}

		FastField.QUANTITY -> {
			if (currentFastRow.quantity.isNotEmpty()) {
				currentFastRow.quantity = cutLast(currentFastRow.quantity)
				currentFastNumericField = FastField.QUANTITY
				currentFastActiveField = FastField.QUANTITY
				lastFastField = FastField.QUANTITY
			}
		}
	}

	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()
}


fun MainActivity.clearFastRows() {
	clearFastEditingState()
	pendingReplaceFastEditing = false
	savedFastRows.clear()
	currentFastRow = FastRow()
	currentFastNumericField = FastField.WIDTH
	currentFastActiveField = FastField.WIDTH
	lastFastField = FastField.WIDTH

	if (currentPackageName.isNotBlank()) {
		packageFastRowsMap[currentPackageName] = mutableListOf()
		packageCurrentFastRowMap[currentPackageName] = FastRow()
	}

	updateDisplayTable()
	triggerAutoSaveDebounced()
}

fun MainActivity.calculateFastTotalQty(): Int {
	val rows = mutableListOf<FastRow>()
	rows.addAll(savedFastRows)
	if (!currentFastRow.isEmpty()) rows.add(currentFastRow.copy())
	return rows.sumOf { it.quantity.toIntOrNull() ?: 1 }
}

fun MainActivity.calculateFastTotalArea(): Double {
	val rows = mutableListOf<FastRow>()
	rows.addAll(savedFastRows)
	if (!currentFastRow.isEmpty()) rows.add(currentFastRow.copy())

	return rows.sumOf { row ->
		val model = row.model.trim().uppercase(Locale.getDefault())
		val qty = row.quantity.toDoubleOrNull() ?: 1.0

		when {
			model.contains("SP") -> {
				0.02 * qty * 1_000_000.0
			}

			model.contains("E") || model.contains("F") -> {
				0.0
			}

			else -> {
				val width = row.width.toDoubleOrNull() ?: 0.0
				val length = row.length.toDoubleOrNull() ?: 0.0
				width * length * qty
			}
		}
	}
}

fun MainActivity.serializeFastContent(): String {
	saveScreenDataToCurrentPackage()

	val allPackageNames = linkedSetOf<String>()
	allPackageNames.addAll(packageFastRowsMap.keys)
	allPackageNames.addAll(packageCurrentFastRowMap.keys)

	if (currentPackageName.isNotBlank()) {
		allPackageNames.add(currentPackageName)
	}

	if (allPackageNames.isEmpty()) return ""

	val packageBlocks = StringBuilder()

	allPackageNames.forEach { packageName ->
		val rows = packageFastRowsMap[packageName] ?: mutableListOf()
		val current = packageCurrentFastRowMap[packageName] ?: FastRow()

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
				listOf(it.width, it.model, it.length, it.quantity).joinToString("\t")
			).append("\n")
		}

		packageBlocks.append("#CURRENT_ROW").append("\n")
		packageBlocks.append(
			listOf(
				current.width,
				current.model,
				current.length,
				current.quantity
			).joinToString("\t")
		).append("\n")

		packageBlocks.append("#END_PACKAGE").append("\n")
	}

	if (packageBlocks.isEmpty()) return ""

	return buildString {
		append("#CURRENT_PACKAGE=").append(currentPackageName).append("\n")
		append(packageBlocks)
	}
}

fun MainActivity.deserializePackageFastContent(content: String) {
	packageFastRowsMap.clear()
	packageCurrentFastRowMap.clear()

	if (content.isBlank()) return

	try {
		if (!content.contains("#PACKAGE=")) {
			if (content.trim().startsWith("#")) {
				return
			}

			val oldRows = deserializeFastContentOld(content)
			if (oldRows.isNotEmpty()) {
				val defaultPackage = "第1包"
				packageFastRowsMap[defaultPackage] = oldRows.toMutableList()
				packageCurrentFastRowMap[defaultPackage] = FastRow()
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
		var rows = mutableListOf<FastRow>()

		lines.forEach { line ->
			when {
				line.startsWith("#CURRENT_PACKAGE=") -> {
					currentPackageName = line.removePrefix("#CURRENT_PACKAGE=").trim()
				}

				line.startsWith("#PACKAGE=") -> {
					if (packageName.isNotBlank()) {
						packageFastRowsMap[packageName] = rows
						packageCurrentFastRowMap.putIfAbsent(packageName, FastRow())
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
						packageFastRowsMap[packageName] = rows
						packageCurrentFastRowMap.putIfAbsent(packageName, FastRow())
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
						FastRow(
							width = parts.getOrNull(0).orEmpty(),
							model = parts.getOrNull(1).orEmpty(),
							length = parts.getOrNull(2).orEmpty(),
							quantity = parts.getOrNull(3).orEmpty()
						)
					)
				}

				inCurrentRow -> {
					val parts = line.split("\t")
					packageCurrentFastRowMap[packageName] = FastRow(
						width = parts.getOrNull(0).orEmpty(),
						model = parts.getOrNull(1).orEmpty(),
						length = parts.getOrNull(2).orEmpty(),
						quantity = parts.getOrNull(3).orEmpty()
					)
					inCurrentRow = false
				}
			}
		}

		if (packageName.isNotBlank()) {
			packageFastRowsMap[packageName] = rows
			packageCurrentFastRowMap.putIfAbsent(packageName, FastRow())
			packageDateMap[packageName] =
				if (packageDate.isBlank()) getTodayPackageDate() else packageDate
		}

		if (currentPackageName.isBlank()) {
			currentPackageName = packageFastRowsMap.keys.firstOrNull().orEmpty()
		}
	} catch (e: Exception) {
		e.printStackTrace()
		packageFastRowsMap.clear()
		packageCurrentFastRowMap.clear()
		if (currentBuildingName.isNotBlank()) {
			buildingFastContentMap[currentBuildingName] = ""
		}
	}
}


fun MainActivity.deserializeFastContentOld(content: String): List<FastRow> {
	if (content.isBlank()) return emptyList()

	return content.replace("\r\n", "\n")
		.split("\n")
		.mapNotNull { line ->
			if (line.isBlank()) return@mapNotNull null
			val parts = line.split("\t")
			FastRow(
				width = parts.getOrNull(0).orEmpty(),
				model = parts.getOrNull(1).orEmpty(),
				length = parts.getOrNull(2).orEmpty(),
				quantity = parts.getOrNull(3).orEmpty()
			)
		}
}

fun MainActivity.buildFastExportRows(): List<FastRow> {
	val rows = mutableListOf<FastRow>()
	rows.addAll(savedFastRows.map { it.copy() })
	if (!currentFastRow.isEmpty()) {
		rows.add(currentFastRow.copy())
	}
	return rows
}

fun MainActivity.calcFastUnitAreaRaw(row: FastRow): Double {
	val model = row.model.trim().uppercase(Locale.getDefault())
	return when {
		model.contains("SP") -> 0.02 * 1_000_000.0
		model.contains("E") || model.contains("F") -> 0.0
		else -> {
			val width = row.width.toDoubleOrNull() ?: 0.0
			val length = row.length.toDoubleOrNull() ?: 0.0
			width * length
		}
	}
}

fun MainActivity.calcFastQty(row: FastRow): Int {
	return row.quantity.toIntOrNull() ?: 1
}

fun MainActivity.calcFastTotalAreaRaw(row: FastRow): Double {
	return calcFastUnitAreaRaw(row) * calcFastQty(row)
}


fun MainActivity.areaToSquareMeterText(area: Double): String {
	return dfArea.format(area / 1_000_000.0)
}

fun MainActivity.renderFastTable() {
	clearFastCellRefs()

	addTableHeader("序号", "宽度", "型号", "长度", "数量")

	if (savedFastRows.isEmpty() && currentFastRow.isEmpty()) {
		addFastDataRow(
			displayIndex = 1,
			data = FastRow(),
			isCurrentRow = true,
			savedRowIndex = null
		)
		return
	}

	savedFastRows.forEachIndexed { index, row ->
		addFastDataRow(
			displayIndex = index + 1,
			data = row,
			isCurrentRow = false,
			savedRowIndex = index
		)
	}

	addFastDataRow(
		displayIndex = savedFastRows.size + 1,
		data = currentFastRow.copy(),
		isCurrentRow = true,
		savedRowIndex = null
	)
}

fun MainActivity.clearFastCellRefs() {
	fastWidthCellMap.clear()
	fastModelCellMap.clear()
	fastLengthCellMap.clear()
	fastQuantityCellMap.clear()
	currentFastWidthCell = null
	currentFastModelCell = null
	currentFastLengthCell = null
	currentFastQuantityCell = null
	fastRowViewMap.clear()
	currentFastRowView = null
}

fun MainActivity.buildFastDataRowView(
		displayIndex: Int,
		data: FastRow,
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
				showFastRowDeleteOptions(savedRowIndex, isCurrentRow)
				true
			}
		)
	)

	val widthCell = createTableCell(
		text = data.width,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingFastRowIndex == savedRowIndex && editingFastField == FastField.WIDTH
		} else {
			editingFastRowIndex == null && currentFastActiveField == FastField.WIDTH
		},
		onClick = {
			if (savedRowIndex != null) {
				selectFastSavedCell(savedRowIndex, FastField.WIDTH)
			} else {
				val oldField = currentFastActiveField
				clearFastEditingState()
				pendingReplaceFastEditing = false
				currentFastNumericField = FastField.WIDTH
				currentFastActiveField = FastField.WIDTH
				lastFastField = FastField.WIDTH
				if (oldField != currentFastActiveField) {
					refreshFastSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(widthCell)

	val modelCell = createTableCell(
		text = data.model,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingFastRowIndex == savedRowIndex && editingFastField == FastField.MODEL
		} else {
			editingFastRowIndex == null && currentFastActiveField == FastField.MODEL
		},
		onClick = {
			if (savedRowIndex != null) {
				selectFastSavedCell(savedRowIndex, FastField.MODEL)
			} else {
				val oldField = currentFastActiveField
				clearFastEditingState()
				pendingReplaceFastEditing = false
				currentFastActiveField = FastField.MODEL
				lastFastField = FastField.MODEL
				if (oldField != currentFastActiveField) {
					refreshFastSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(modelCell)

	val lengthCell = createTableCell(
		text = data.length,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingFastRowIndex == savedRowIndex && editingFastField == FastField.LENGTH
		} else {
			editingFastRowIndex == null && currentFastActiveField == FastField.LENGTH
		},
		onClick = {
			if (savedRowIndex != null) {
				selectFastSavedCell(savedRowIndex, FastField.LENGTH)
			} else {
				val oldField = currentFastActiveField
				clearFastEditingState()
				pendingReplaceFastEditing = false
				currentFastNumericField = FastField.LENGTH
				currentFastActiveField = FastField.LENGTH
				lastFastField = FastField.LENGTH
				if (oldField != currentFastActiveField) {
					refreshFastSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(lengthCell)

	val quantityCell = createTableCell(
		text = data.quantity,
		isHeader = false,
		highlight = isCurrentRow,
		selected = if (savedRowIndex != null) {
			editingFastRowIndex == savedRowIndex && editingFastField == FastField.QUANTITY
		} else {
			editingFastRowIndex == null && currentFastActiveField == FastField.QUANTITY
		},
		onClick = {
			if (savedRowIndex != null) {
				selectFastSavedCell(savedRowIndex, FastField.QUANTITY)
			} else {
				val oldField = currentFastActiveField
				clearFastEditingState()
				pendingReplaceFastEditing = false
				currentFastNumericField = FastField.QUANTITY
				currentFastActiveField = FastField.QUANTITY
				lastFastField = FastField.QUANTITY
				if (oldField != currentFastActiveField) {
					refreshFastSelectionOnly(null, true, null, true)
				}
			}
		}
	)
	row.addView(quantityCell)

	if (savedRowIndex != null) {
		fastWidthCellMap[savedRowIndex] = widthCell
		fastModelCellMap[savedRowIndex] = modelCell
		fastLengthCellMap[savedRowIndex] = lengthCell
		fastQuantityCellMap[savedRowIndex] = quantityCell
		fastRowViewMap[savedRowIndex] = row
	} else {
		currentFastWidthCell = widthCell
		currentFastModelCell = modelCell
		currentFastLengthCell = lengthCell
		currentFastQuantityCell = quantityCell
		currentFastRowView = row
	}

	return row
}

fun MainActivity.addFastDataRow(
		displayIndex: Int,
		data: FastRow,
		isCurrentRow: Boolean,
		savedRowIndex: Int? = null
) {
	tableBody.addView(
		buildFastDataRowView(
			displayIndex = displayIndex,
			data = data,
			isCurrentRow = isCurrentRow,
			savedRowIndex = savedRowIndex
		)
	)
}


fun MainActivity.refreshFastVisibleCellsOnly() {
	if (currentModeType != ModeType.FAST) return

	if (editingFastRowIndex != null) {
		val rowIndex = editingFastRowIndex ?: return
		val row = savedFastRows.getOrNull(rowIndex) ?: return
		fastWidthCellMap[rowIndex]?.text = row.width
		fastModelCellMap[rowIndex]?.text = row.model
		fastLengthCellMap[rowIndex]?.text = row.length
		fastQuantityCellMap[rowIndex]?.text = row.quantity
	} else {
		currentFastWidthCell?.text = currentFastRow.width
		currentFastModelCell?.text = currentFastRow.model
		currentFastLengthCell?.text = currentFastRow.length
		currentFastQuantityCell?.text = currentFastRow.quantity
	}

	tvSummaryPrimary.visibility = View.VISIBLE
	tvSummarySecondary.visibility = View.VISIBLE
	tvSummaryPrimary.text = "合计面积：${formatAreaSquareMeter(calculateFastTotalArea())}"
	tvSummarySecondary.text = "合计数量：${calculateFastTotalQty()}"
}


fun MainActivity.refreshFastSelectionOnly(
		oldSavedRowIndex: Int?,
		oldWasCurrentRow: Boolean,
		newSavedRowIndex: Int?,
		newWasCurrentRow: Boolean
) {
	if (currentModeType != ModeType.FAST) return
	rebuildFastRowOnly(oldSavedRowIndex, oldWasCurrentRow)
	if (oldSavedRowIndex != newSavedRowIndex || oldWasCurrentRow != newWasCurrentRow) {
		rebuildFastRowOnly(newSavedRowIndex, newWasCurrentRow)
	}
}


fun MainActivity.rebuildFastRowOnly(savedRowIndex: Int?, isCurrentRow: Boolean) {
	if (currentModeType != ModeType.FAST) return

	val displayIndex = if (savedRowIndex != null) savedRowIndex + 1 else savedFastRows.size + 1
	val data = if (savedRowIndex != null) {
		savedFastRows.getOrNull(savedRowIndex)?.copy() ?: return
	} else {
		currentFastRow.copy()
	}

	val newRow = buildFastDataRowView(
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


fun MainActivity.appendTextToEditingFastCell(text: String): Boolean {
	val rowIndex = editingFastRowIndex ?: return false
	val field = editingFastField ?: return false
	val row = savedFastRows.getOrNull(rowIndex) ?: return false

	when (field) {
		FastField.WIDTH -> {
			val newValue = if (pendingReplaceFastEditing) text else row.width + text
			if (!isFastWidthValid(newValue)) {
				if (text in fastPresetNumericTokens) {
					showFastInputWarning()
					row.width = text
					pendingReplaceFastEditing = false
					currentFastNumericField = FastField.WIDTH
					currentFastActiveField = FastField.WIDTH
					lastFastField = FastField.WIDTH
					updateDisplayTable()
					triggerAutoSaveDebounced()
					return true
				}
				showFastInputWarning()
				return true
			}
			row.width = newValue
			currentFastNumericField = FastField.WIDTH
		}

		FastField.MODEL -> {
			row.model = text
		}

		FastField.LENGTH -> {
			val newValue = if (pendingReplaceFastEditing) text else row.length + text
			if (!isFastLengthValid(newValue)) {
				if (text in fastPresetNumericTokens) {
					showFastInputWarning()
					row.length = text
					pendingReplaceFastEditing = false
					currentFastNumericField = FastField.LENGTH
					currentFastActiveField = FastField.LENGTH
					lastFastField = FastField.LENGTH
					updateDisplayTable()
					triggerAutoSaveDebounced()
					return true
				}
				showFastInputWarning()
				return true
			}
			row.length = newValue
			currentFastNumericField = FastField.LENGTH
		}

		FastField.QUANTITY -> {
			if (text == "." || !text.all { it.isDigit() }) {
				if (containsLetters(text)) {
					toast("数量内不能输入字母")
				}
				return true
			}
			val newValue = if (pendingReplaceFastEditing) text else row.quantity + text
			if (!isFastQuantityValid(newValue)) {
				if (text in fastPresetNumericTokens) {
					showFastInputWarning()
					row.quantity = text
					pendingReplaceFastEditing = false
					currentFastNumericField = FastField.QUANTITY
					currentFastActiveField = FastField.QUANTITY
					lastFastField = FastField.QUANTITY
					updateDisplayTable()
					triggerAutoSaveDebounced()
					return true
				}
				showFastInputWarning()
				return true
			}
			row.quantity = newValue
			currentFastNumericField = FastField.QUANTITY
		}
	}

	pendingReplaceFastEditing = false
	currentFastActiveField = field
	lastFastField = field
	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()
	return true

}


fun MainActivity.deleteFromEditingFastCell(): Boolean {
	val rowIndex = editingFastRowIndex ?: return false
	val field = editingFastField ?: return false
	val row = savedFastRows.getOrNull(rowIndex) ?: return false

	fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

	when (field) {
		FastField.WIDTH -> {
			row.width = cutLast(row.width)
			currentFastNumericField = FastField.WIDTH
		}

		FastField.MODEL -> {
			row.model = cutLast(row.model)
		}

		FastField.LENGTH -> {
			row.length = cutLast(row.length)
			currentFastNumericField = FastField.LENGTH
		}

		FastField.QUANTITY -> {
			row.quantity = cutLast(row.quantity)
			currentFastNumericField = FastField.QUANTITY
		}
	}

	pendingReplaceFastEditing = false
	currentFastActiveField = field
	lastFastField = field
	refreshFastVisibleCellsOnly()
	triggerAutoSaveDebounced()
	return true

}

fun MainActivity.selectFastSavedCell(rowIndex: Int, field: FastField) {
	if (rowIndex !in savedFastRows.indices) return
	editingFastRowIndex = rowIndex
	editingFastField = field
	currentFastActiveField = field
	currentFastNumericField = when (field) {
		FastField.WIDTH -> FastField.WIDTH
		FastField.MODEL -> FastField.WIDTH
		FastField.LENGTH -> FastField.LENGTH
		FastField.QUANTITY -> FastField.QUANTITY
	}
	lastFastField = field
	pendingReplaceFastEditing = true
	updateDisplayTable()
}


fun MainActivity.showFastRowDeleteOptions(savedRowIndex: Int?, isCurrentRow: Boolean) {
	if (savedRowIndex == null && (!isCurrentRow || currentFastRow.isEmpty())) return

	confirmDeleteFastRow(savedRowIndex, isCurrentRow)
}


fun guessLastFastField(row: FastRow): FastField {
	return when {
		row.quantity.isNotEmpty() -> FastField.QUANTITY
		row.length.isNotEmpty() -> FastField.LENGTH
		row.model.isNotEmpty() -> FastField.MODEL
		row.width.isNotEmpty() -> FastField.WIDTH
		else -> FastField.WIDTH
	}
}


fun MainActivity.hasFastEditingTarget(): Boolean {
	return editingFastRowIndex != null && editingFastField != null
}


fun MainActivity.clearFastEditingState() {
	editingFastRowIndex = null
	editingFastField = null
	pendingReplaceFastEditing = false
}


fun MainActivity.resolveNextFastNumericFieldAfterModel(row: FastRow): FastField {
	return when (row.model.trim().uppercase(Locale.getDefault())) {
		"SP" -> FastField.QUANTITY
		"E", "F" -> FastField.LENGTH
		else -> {
			when {
				row.width.isBlank() -> FastField.WIDTH
				row.length.isBlank() -> FastField.LENGTH
				else -> FastField.QUANTITY
			}
		}
	}
}


fun MainActivity.isFastWidthValid(value: String): Boolean {
	if (value.isBlank() || value == ".") return true
	val number = value.toDoubleOrNull() ?: return true
	return number <= 600.0
}

fun MainActivity.isFastLengthValid(value: String): Boolean {
	if (value.isBlank() || value == ".") return true
	val number = value.toDoubleOrNull() ?: return true
	return number <= 4500.0
}


fun MainActivity.isFastQuantityValid(value: String): Boolean {
	if (value.isBlank()) return true
	val number = value.toIntOrNull() ?: return true
	return number <= 500
}


fun MainActivity.handleFastPresetReplaceOnLimit(
		field: FastField,
		token: String,
		applyValue: (String) -> Unit
): Boolean {
	if (token !in fastPresetNumericTokens) return false

	showFastInputWarning()
	applyValue(token)

	when (field) {
		FastField.WIDTH -> {
			currentFastNumericField = FastField.WIDTH
			currentFastActiveField = FastField.WIDTH
			lastFastField = FastField.WIDTH
		}

		FastField.LENGTH -> {
			currentFastNumericField = FastField.LENGTH
			currentFastActiveField = FastField.LENGTH
			lastFastField = FastField.LENGTH
		}

		FastField.QUANTITY -> {
			currentFastNumericField = FastField.QUANTITY
			currentFastActiveField = FastField.QUANTITY
			lastFastField = FastField.QUANTITY
		}

		FastField.MODEL -> {}
	}

	updateDisplayTable()
	triggerAutoSaveDebounced()
	return true
}


fun MainActivity.showFastInputWarning(message: String = "警告！系统检测到错误输入，请检查输入数据是否有误！") {
	Toast.makeText(
		this,
		message,
		Toast.LENGTH_SHORT
	).show()
}



