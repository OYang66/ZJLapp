package com.example.datarecorder

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FastRowAdapter(
    private val list: MutableList<FastRow>,
    private val onChanged: () -> Unit,
    private val onCellFocused: (row: Int, columnKey: String, editText: EditText) -> Unit
) : RecyclerView.Adapter<FastRowAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndex: TextView = view.findViewById(R.id.tvIndex)
        val etWidth: EditText = view.findViewById(R.id.etWidth)
        val etModel: EditText = view.findViewById(R.id.etModel)
        val etLength: EditText = view.findViewById(R.id.etLength)
        val etQuantity: EditText = view.findViewById(R.id.etQuantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fast_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = list[position]

        holder.tvIndex.text = (position + 1).toString()

        bindEdit(
            holder = holder,
            editText = holder.etWidth,
            value = row.width,
            numericOnly = true,
            columnKey = "width"
        ) {
            row.width = it
            onChanged()
        }

        bindEdit(
            holder = holder,
            editText = holder.etModel,
            value = row.model,
            numericOnly = false,
            columnKey = "model"
        ) {
            row.model = it
            onChanged()
        }

        bindEdit(
            holder = holder,
            editText = holder.etLength,
            value = row.length,
            numericOnly = true,
            columnKey = "length"
        ) {
            row.length = it
            onChanged()
        }

        bindEdit(
            holder = holder,
            editText = holder.etQuantity,
            value = row.quantity,
            numericOnly = true,
            columnKey = "quantity"
        ) {
            row.quantity = it
            onChanged()
        }
    }

    override fun getItemCount(): Int = list.size

    fun addRow() {
        list.add(FastRow())
        notifyItemInserted(list.lastIndex)
        onChanged()
    }

    fun removeLastRow() {
        if (list.isEmpty()) return
        val index = list.lastIndex
        list.removeAt(index)
        notifyItemRemoved(index)
        onChanged()
    }

    fun getRows(): MutableList<FastRow> = list

    private fun bindEdit(
        holder: VH,
        editText: EditText,
        value: String,
        numericOnly: Boolean,
        columnKey: String,
        afterChanged: (String) -> Unit
    ) {
        val oldWatcher = editText.getTag(R.id.tag_text_watcher) as? TextWatcher
        if (oldWatcher != null) {
            editText.removeTextChangedListener(oldWatcher)
        }

        if (editText.text.toString() != value) {
            editText.setText(value)
            editText.setSelection(editText.text.length)
        }

        EditableCellHelper.bind(
            editText = editText,
            numericOnly = numericOnly,
            onFocused = {
                val realPos = holder.bindingAdapterPosition
                if (realPos != RecyclerView.NO_POSITION) {
                    onCellFocused(realPos, columnKey, editText)
                }
            }
        )

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                afterChanged(s?.toString().orEmpty())
            }
        }

        editText.addTextChangedListener(watcher)
        editText.setTag(R.id.tag_text_watcher, watcher)
    }
}
