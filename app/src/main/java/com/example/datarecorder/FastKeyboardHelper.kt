package com.example.datarecorder

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout

class FastKeyboardHelper(
    private val context: Context,
    private val tailLayout: GridLayout,
    private val widthLayout: GridLayout,
    private val customLayout: GridLayout,
    private val modelLayout: GridLayout,
    private val listener: Listener
) {

    interface Listener {
        fun onTailClick(value: String)
        fun onWidthClick(value: String)
        fun onCustomNumberClick(value: String)
        fun onModelClick(value: String)
        fun onChangeColumn()
        fun onNewRow()
        fun onBackspace()
    }

    fun build() {
        buildSection(
            layout = tailLayout,
            columnCount = 5,
            values = listOf("0", "5", "8", "尾数")
        ) { listener.onTailClick(it) }

        buildSection(
            layout = widthLayout,
            columnCount = 5,
            values = listOf("650", "700", "750", "800", "850", "900", "950", "1000")
        ) { listener.onWidthClick(it) }

        buildSection(
            layout = customLayout,
            columnCount = 5,
            values = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ".", "←")
        ) {
            if (it == "←") listener.onBackspace() else listener.onCustomNumberClick(it)
        }

        buildSection(
            layout = modelLayout,
            columnCount = 3,
            values = listOf("A", "B", "C", "D", "换列", "新行")
        ) {
            when (it) {
                "换列" -> listener.onChangeColumn()
                "新行" -> listener.onNewRow()
                else -> listener.onModelClick(it)
            }
        }
    }

    private fun buildSection(
        layout: GridLayout,
        columnCount: Int,
        values: List<String>,
        click: (String) -> Unit
    ) {
        layout.removeAllViews()
        layout.columnCount = columnCount

        values.forEach { text ->
            val button = Button(context).apply {
                this.text = text
                isAllCaps = false
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                minHeight = dp(42)
                minimumHeight = dp(42)
                setOnClickListener { click(text) }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(42)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }

            layout.addView(button, params)
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
