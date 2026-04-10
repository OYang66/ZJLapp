package com.example.datarecorder

import android.text.InputType
import android.widget.EditText

object EditableCellHelper {

    fun bind(
        editText: EditText,
        numericOnly: Boolean,
        onFocused: (() -> Unit)? = null
    ) {
        editText.showSoftInputOnFocus = false

        editText.inputType = if (numericOnly) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_TEXT
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onFocused?.invoke()
            }
        }

        editText.setOnClickListener {
            if (editText.hasFocus()) {
                onFocused?.invoke()
            }
        }
    }
}
