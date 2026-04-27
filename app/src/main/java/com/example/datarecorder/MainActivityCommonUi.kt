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

    val symbols = listOf("+", "-", "/", "G", "()")

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

fun MainActivity.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

fun MainActivity.dpF(value: Float): Float {
    return value * resources.displayMetrics.density
}

fun MainActivity.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
