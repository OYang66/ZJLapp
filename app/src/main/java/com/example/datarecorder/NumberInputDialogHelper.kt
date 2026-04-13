package com.example.datarecorder

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class NumberInputDialogHelper(
    context: Context,
    private val titleText: String,
    private val initialValue: String = "",
    private val allowDecimal: Boolean = true,
    private val onConfirm: (String) -> Unit
) : Dialog(context) {

    private lateinit var tvTitle: TextView
    private lateinit var tvValue: TextView
    private var currentValue: String = initialValue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_number_input)

        tvTitle = findViewById(R.id.tvDialogTitle)
        tvValue = findViewById(R.id.tvDialogValue)

        tvTitle.text = titleText
        refreshValue()

        val numberButtons = listOf(
            R.id.btnNum0 to "0",
            R.id.btnNum1 to "1",
            R.id.btnNum2 to "2",
            R.id.btnNum3 to "3",
            R.id.btnNum4 to "4",
            R.id.btnNum5 to "5",
            R.id.btnNum6 to "6",
            R.id.btnNum7 to "7",
            R.id.btnNum8 to "8",
            R.id.btnNum9 to "9",
            R.id.btnNum00 to "00"
        )

        numberButtons.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                currentValue += value
                refreshValue()
            }
        }

        findViewById<Button>(R.id.btnNumDot).setOnClickListener {
            if (!allowDecimal) return@setOnClickListener
            if (currentValue.contains(".")) return@setOnClickListener
            currentValue = if (currentValue.isBlank()) "0." else "$currentValue."
            refreshValue()
        }

        findViewById<Button>(R.id.btnNumBackspace).setOnClickListener {
            if (currentValue.isNotBlank()) {
                currentValue = currentValue.dropLast(1)
                refreshValue()
            }
        }

        findViewById<Button>(R.id.btnDialogCancel).setOnClickListener {
            dismiss()
        }

        findViewById<Button>(R.id.btnDialogConfirm).setOnClickListener {
            onConfirm(currentValue)
            dismiss()
        }
    }

    private fun refreshValue() {
        tvValue.text = if (currentValue.isBlank()) "请输入" else currentValue
    }
}
