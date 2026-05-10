package com.example.datarecorder

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast

fun MainActivity.createCellBackground(
		fillColor: Int,
		strokeColor: Int,
		strokeWidthDp: Int,
		cornerRadiusDp: Float
): GradientDrawable {
	return GradientDrawable().apply {
		shape = GradientDrawable.RECTANGLE
		setColor(fillColor)
		setStroke(dp(strokeWidthDp), strokeColor)
		cornerRadius = dpF(cornerRadiusDp)
	}
}

fun MainActivity.createTableCell(
		text: String,
		isHeader: Boolean,
		highlight: Boolean = false,
		selected: Boolean = false,
		onClick: (() -> Unit)? = null,
		onLongClick: (() -> Boolean)? = null
): TextView {
	return TextView(this).apply {
		this.text = text
		gravity = Gravity.CENTER
		textAlignment = View.TEXT_ALIGNMENT_CENTER
		setPadding(dp(2), dp(5), dp(2), dp(5))
		textSize = if (isHeader) 10f else 11f
		minHeight = dp(30)

		setTextColor(
			when {
				selected -> 0xFF4E3D91.toInt()
				isHeader -> 0xFF4A4A4A.toInt()
				else -> 0xFF222222.toInt()
			}
		)

		if (selected) {
			setTypeface(typeface, Typeface.BOLD)
		}

		background = when {
			selected -> createCellBackground(
				fillColor = 0xFFEDE7FF.toInt(),
				strokeColor = 0xFF6C56B3.toInt(),
				strokeWidthDp = 2,
				cornerRadiusDp = 6f
			)

			isHeader -> createCellBackground(
				fillColor = 0xFFE9EEF7.toInt(),
				strokeColor = 0xFFD0D8E6.toInt(),
				strokeWidthDp = 1,
				cornerRadiusDp = 4f
			)

			highlight -> createCellBackground(
				fillColor = 0xFFF8FBFF.toInt(),
				strokeColor = 0xFFDCE3EF.toInt(),
				strokeWidthDp = 1,
				cornerRadiusDp = 4f
			)

			else -> createCellBackground(
				fillColor = 0xFFFFFFFF.toInt(),
				strokeColor = 0xFFDCE3EF.toInt(),
				strokeWidthDp = 1,
				cornerRadiusDp = 4f
			)
		}

		layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
			setMargins(dp(1), dp(1), dp(1), dp(1))
		}

		if (!isHeader && onClick != null) {
			isClickable = true
			isFocusable = true
			setOnClickListener { onClick() }
		}

		if (!isHeader && onLongClick != null) {
			isLongClickable = true
			setOnLongClickListener { onLongClick() }
		}
	}
}

fun MainActivity.createSymbolPopupButton(text: String, onClick: () -> Unit): Button {
	return Button(this).apply {
		this.text = text
		textSize = 14f
		isAllCaps = false
		gravity = Gravity.CENTER
		textAlignment = View.TEXT_ALIGNMENT_CENTER
		includeFontPadding = false
		setTextColor(0xFF4E3D91.toInt())
		setPadding(0, 0, 0, 0)
		background = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(0xFFF3EEFF.toInt())
			cornerRadius = dpF(10f)
			setStroke(dp(1), 0xFFD7CCFF.toInt())
		}
		setOnClickListener { onClick() }
	}
}

fun MainActivity.showSymbolPopup(anchor: View, onSymbolSelected: (String) -> Unit) {
	val container = LinearLayout(this).apply {
		orientation = LinearLayout.HORIZONTAL
		setPadding(dp(8), dp(8), dp(8), dp(8))
		background = GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(Color.WHITE)
			cornerRadius = dpF(14f)
			setStroke(dp(1), 0xFFE1E5F0.toInt())
		}
		elevation = dpF(6f)
	}

	val popupWindow = PopupWindow(
		container,
		LinearLayout.LayoutParams.WRAP_CONTENT,
		LinearLayout.LayoutParams.WRAP_CONTENT,
		true
	).apply {
		isOutsideTouchable = true
		setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		elevation = dpF(8f)
	}

	val symbols = listOf("+", "-", "/", "G", "L", "()")

	symbols.forEachIndexed { index, symbol ->
		val button = createSymbolPopupButton(symbol) {
			onSymbolSelected(symbol)
			popupWindow.dismiss()
		}

		val lp = LinearLayout.LayoutParams(dp(48), dp(40)).apply {
			if (index > 0) marginStart = dp(6)
		}
		container.addView(button, lp)
	}

	container.measure(
		View.MeasureSpec.UNSPECIFIED,
		View.MeasureSpec.UNSPECIFIED
	)

	val xOff = (anchor.width - container.measuredWidth) / 2
	popupWindow.showAsDropDown(anchor, xOff, dp(6))
}

fun MainActivity.showAccountCardPopup(
		anchor: View,
		username: String,
		onlineText: String,
		lastActiveText: String,
		onOpenBackend: () -> Unit,
		onLogout: () -> Unit
) {
	fun createInfoRow(label: String, value: String): LinearLayout {
		return LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(12), dp(10), dp(12), dp(10))
			background = createCellBackground(
				fillColor = 0xFFF8F5FF.toInt(),
				strokeColor = 0xFFE4DAFF.toInt(),
				strokeWidthDp = 1,
				cornerRadiusDp = 12f
			)

			addView(TextView(this@showAccountCardPopup).apply {
				text = label
				textSize = 11f
				setTextColor(0xFF8A7AB8.toInt())
			})

			addView(TextView(this@showAccountCardPopup).apply {
				text = value
				textSize = 14f
				setTextColor(0xFF2F2A3D.toInt())
				setTypeface(typeface, Typeface.BOLD)
				maxLines = 1
				ellipsize = android.text.TextUtils.TruncateAt.END
				setPadding(0, dp(2), 0, 0)
			})
		}
	}

	fun createActionButton(
			text: String,
			backgroundRes: Int,
			textColor: Int,
			onClick: () -> Unit
	): Button {
		return Button(this).apply {
			this.text = text
			textSize = 13f
			isAllCaps = false
			gravity = Gravity.CENTER
			setTextColor(textColor)
			background = getDrawable(backgroundRes)
			setPadding(dp(8), 0, dp(8), 0)
			setOnClickListener { onClick() }
		}
	}

	val popupWidth = minOf(dp(280), resources.displayMetrics.widthPixels - dp(24))
	lateinit var popupWindow: PopupWindow
	val avatarText = username.take(1).ifBlank { "用" }.uppercase()
	val isOnline = onlineText == "在线"

	val container = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(dp(14), dp(14), dp(14), dp(14))
		background = getDrawable(R.drawable.bg_card_soft)
		elevation = dpF(10f)

		addView(LinearLayout(this@showAccountCardPopup).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL

			addView(TextView(this@showAccountCardPopup).apply {
				text = avatarText
				gravity = Gravity.CENTER
				textSize = 15f
				setTypeface(typeface, Typeface.BOLD)
				setTextColor(Color.WHITE)
				background = getDrawable(R.drawable.bg_default_avatar)
			}, LinearLayout.LayoutParams(dp(42), dp(42)))

			addView(LinearLayout(this@showAccountCardPopup).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(10), 0, 0, 0)

				addView(TextView(this@showAccountCardPopup).apply {
					text = username
					textSize = 16f
					setTypeface(typeface, Typeface.BOLD)
					setTextColor(0xFF2F2A3D.toInt())
					maxLines = 1
					ellipsize = android.text.TextUtils.TruncateAt.END
				})

				addView(TextView(this@showAccountCardPopup).apply {
					text = if (isOnline) "当前账号在线" else "当前账号离线"
					textSize = 12f
					setTextColor(0xFF8A7AB8.toInt())
					setPadding(0, dp(2), 0, 0)
				})
			}, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

			addView(TextView(this@showAccountCardPopup).apply {
				text = onlineText
				gravity = Gravity.CENTER
				textSize = 12f
				setTypeface(typeface, Typeface.BOLD)
				setTextColor(if (isOnline) 0xFF2E8B57.toInt() else 0xFF6B7280.toInt())
				setPadding(dp(10), dp(5), dp(10), dp(5))
				background = GradientDrawable().apply {
					shape = GradientDrawable.RECTANGLE
					setColor(if (isOnline) 0xFFEAF8F0.toInt() else 0xFFF1F3F5.toInt())
					cornerRadius = dpF(999f)
					setStroke(dp(1), if (isOnline) 0xFFBFE7CF.toInt() else 0xFFD7DCE2.toInt())
				}
			})
		})

		addView(createInfoRow("在线状态", onlineText), LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		).apply {
			topMargin = dp(12)
		})

		addView(createInfoRow("最近活跃时间", lastActiveText), LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
		).apply {
			topMargin = dp(8)
		})

		addView(LinearLayout(this@showAccountCardPopup).apply {
			orientation = LinearLayout.HORIZONTAL

			addView(createActionButton(
				text = "访问后台",
				backgroundRes = R.drawable.bg_auth_button_primary,
				textColor = Color.WHITE
			) {
				popupWindow.dismiss()
				onOpenBackend()
			}, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
				topMargin = dp(14)
				marginEnd = dp(6)
			})

			addView(createActionButton(
				text = "退出登录",
				backgroundRes = R.drawable.bg_auth_button_outline,
				textColor = 0xFF6C56B3.toInt()
			) {
				popupWindow.dismiss()
				onLogout()
			}, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
				topMargin = dp(14)
				marginStart = dp(6)
			})
		})
	}

	container.measure(
		View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
		View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
	)

	popupWindow = PopupWindow(
		container,
		popupWidth,
		LinearLayout.LayoutParams.WRAP_CONTENT,
		true
	).apply {
		isOutsideTouchable = true
		setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		elevation = dpF(10f)
	}

	val anchorLocation = IntArray(2)
	anchor.getLocationOnScreen(anchorLocation)
	val anchorLeft = anchorLocation[0]
	val anchorTop = anchorLocation[1]
	val anchorBottom = anchorTop + anchor.height
	val screenWidth = resources.displayMetrics.widthPixels
	val screenHeight = resources.displayMetrics.heightPixels
	val margin = dp(12)
	val preferredLeft = anchorLeft + (anchor.width - popupWidth) / 2
	val clampedLeft = preferredLeft.coerceIn(margin, screenWidth - popupWidth - margin)
	val xOff = clampedLeft - anchorLeft
	val spaceBelow = screenHeight - anchorBottom - margin
	val spaceAbove = anchorTop - margin

	if (spaceBelow >= container.measuredHeight || spaceBelow >= spaceAbove) {
		popupWindow.showAsDropDown(anchor, xOff, dp(8))
	} else {
		popupWindow.showAsDropDown(anchor, xOff, -(anchor.height + container.measuredHeight + dp(8)))
	}
}

fun MainActivity.dp(value: Int): Int {
	return (value * resources.displayMetrics.density).toInt()
}

fun MainActivity.dpF(value: Float): Float {
	return value * resources.displayMetrics.density
}

fun MainActivity.toast(msg: String) {
	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
