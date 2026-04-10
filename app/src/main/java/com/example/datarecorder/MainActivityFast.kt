package com.example.datarecorder

import java.util.Locale


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

    bindAdaptiveFixedColumnButtons(layoutFastModel, fastModelKeys, 3, modelTextSize) { value ->
        appendFastModelToken(value)
    }
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
                if (handleFastPresetReplaceOnLimit(FastField.WIDTH, value) { currentFastRow.width = it }) {
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
                if (handleFastPresetReplaceOnLimit(FastField.LENGTH, value) { currentFastRow.length = it }) {
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
                if (handleFastPresetReplaceOnLimit(FastField.QUANTITY, value) { currentFastRow.quantity = it }) {
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
                if (handleFastPresetReplaceOnLimit(FastField.LENGTH, value) { currentFastRow.length = it }) {
                    return
                }
                showFastInputWarning()
                return
            }
            currentFastRow.length = newValue
        }
    }

    updateDisplayTable()
    triggerAutoSave()
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

    updateDisplayTable()
    triggerAutoSave()
}
 fun MainActivity.moveToNextFastColumn() {
    if (!ensurePackageSelected()) return

    if (hasFastEditingTarget()) {
        editingFastField = when (editingFastField!!) {
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
        updateDisplayTable()
        triggerAutoSave()
        return
    }

    currentFastNumericField = when (currentFastNumericField) {
        FastField.WIDTH -> FastField.LENGTH
        FastField.LENGTH -> FastField.QUANTITY
        FastField.QUANTITY -> FastField.WIDTH
        FastField.MODEL -> FastField.LENGTH
    }
    currentFastActiveField = currentFastNumericField
    lastFastField = currentFastNumericField
    updateDisplayTable()
    triggerAutoSave()
}

 fun MainActivity.finishCurrentFastRow() {
    if (!ensurePackageSelected()) return

    if (hasFastEditingTarget()) {
        clearFastEditingState()
        currentFastNumericField = FastField.WIDTH
        currentFastActiveField = FastField.WIDTH
        lastFastField = FastField.WIDTH
        updateDisplayTable()
        triggerAutoSave()
        return
    }

    if (!currentFastRow.isEmpty()) {
        savedFastRows.add(currentFastRow.copy())
    }

    currentFastRow = FastRow()
    currentFastNumericField = FastField.WIDTH
    currentFastActiveField = FastField.WIDTH
    lastFastField = FastField.WIDTH
    updateDisplayTable()
    triggerAutoSave()
}

 fun MainActivity.deleteLastFastInput() {
    if (!ensurePackageSelected()) return

    if (hasFastEditingTarget()) {
        deleteFromEditingFastCell()
        return
    }

    fun MainActivity.cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

    when (lastFastField) {
        FastField.WIDTH -> {
            if (currentFastRow.width.isNotEmpty()) {
                currentFastRow.width = cutLast(currentFastRow.width)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.WIDTH
                lastFastField = FastField.WIDTH
            } else if (currentFastRow.model.isNotEmpty()) {
                currentFastRow.model = cutLast(currentFastRow.model)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.MODEL
                lastFastField = FastField.MODEL
            } else if (currentFastRow.length.isNotEmpty()) {
                currentFastRow.length = cutLast(currentFastRow.length)
                currentFastNumericField = FastField.LENGTH
                currentFastActiveField = FastField.LENGTH
                lastFastField = FastField.LENGTH
            } else if (currentFastRow.quantity.isNotEmpty()) {
                currentFastRow.quantity = cutLast(currentFastRow.quantity)
                currentFastNumericField = FastField.QUANTITY
                currentFastActiveField = FastField.QUANTITY
                lastFastField = FastField.QUANTITY
            } else if (savedFastRows.isNotEmpty()) {
                currentFastRow = savedFastRows.removeAt(savedFastRows.lastIndex)
                lastFastField = guessLastFastField(currentFastRow)
                currentFastNumericField = when (lastFastField) {
                    FastField.WIDTH -> FastField.WIDTH
                    FastField.MODEL -> FastField.WIDTH
                    FastField.LENGTH -> FastField.LENGTH
                    FastField.QUANTITY -> FastField.QUANTITY
                }
                currentFastActiveField = lastFastField
                deleteLastFastInput()
                return
            }
        }

        FastField.MODEL -> {
            if (currentFastRow.model.isNotEmpty()) {
                currentFastRow.model = cutLast(currentFastRow.model)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.MODEL
                lastFastField = FastField.MODEL
            } else if (currentFastRow.width.isNotEmpty()) {
                currentFastRow.width = cutLast(currentFastRow.width)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.WIDTH
                lastFastField = FastField.WIDTH
            } else if (currentFastRow.length.isNotEmpty()) {
                currentFastRow.length = cutLast(currentFastRow.length)
                currentFastNumericField = FastField.LENGTH
                currentFastActiveField = FastField.LENGTH
                lastFastField = FastField.LENGTH
            } else if (currentFastRow.quantity.isNotEmpty()) {
                currentFastRow.quantity = cutLast(currentFastRow.quantity)
                currentFastNumericField = FastField.QUANTITY
                currentFastActiveField = FastField.QUANTITY
                lastFastField = FastField.QUANTITY
            } else if (savedFastRows.isNotEmpty()) {
                currentFastRow = savedFastRows.removeAt(savedFastRows.lastIndex)
                lastFastField = guessLastFastField(currentFastRow)
                currentFastNumericField = when (lastFastField) {
                    FastField.WIDTH -> FastField.WIDTH
                    FastField.MODEL -> FastField.WIDTH
                    FastField.LENGTH -> FastField.LENGTH
                    FastField.QUANTITY -> FastField.QUANTITY
                }
                currentFastActiveField = lastFastField
                deleteLastFastInput()
                return
            }
        }

        FastField.LENGTH -> {
            if (currentFastRow.length.isNotEmpty()) {
                currentFastRow.length = cutLast(currentFastRow.length)
                currentFastNumericField = FastField.LENGTH
                currentFastActiveField = FastField.LENGTH
                lastFastField = FastField.LENGTH
            } else if (currentFastRow.model.isNotEmpty()) {
                currentFastRow.model = cutLast(currentFastRow.model)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.MODEL
                lastFastField = FastField.MODEL
            } else if (currentFastRow.width.isNotEmpty()) {
                currentFastRow.width = cutLast(currentFastRow.width)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.WIDTH
                lastFastField = FastField.WIDTH
            } else if (currentFastRow.quantity.isNotEmpty()) {
                currentFastRow.quantity = cutLast(currentFastRow.quantity)
                currentFastNumericField = FastField.QUANTITY
                currentFastActiveField = FastField.QUANTITY
                lastFastField = FastField.QUANTITY
            } else if (savedFastRows.isNotEmpty()) {
                currentFastRow = savedFastRows.removeAt(savedFastRows.lastIndex)
                lastFastField = guessLastFastField(currentFastRow)
                currentFastNumericField = when (lastFastField) {
                    FastField.WIDTH -> FastField.WIDTH
                    FastField.MODEL -> FastField.WIDTH
                    FastField.LENGTH -> FastField.LENGTH
                    FastField.QUANTITY -> FastField.QUANTITY
                }
                currentFastActiveField = lastFastField
                deleteLastFastInput()
                return
            }
        }

        FastField.QUANTITY -> {
            if (currentFastRow.quantity.isNotEmpty()) {
                currentFastRow.quantity = cutLast(currentFastRow.quantity)
                currentFastNumericField = FastField.QUANTITY
                currentFastActiveField = FastField.QUANTITY
                lastFastField = FastField.QUANTITY
            } else if (currentFastRow.length.isNotEmpty()) {
                currentFastRow.length = cutLast(currentFastRow.length)
                currentFastNumericField = FastField.LENGTH
                currentFastActiveField = FastField.LENGTH
                lastFastField = FastField.LENGTH
            } else if (currentFastRow.model.isNotEmpty()) {
                currentFastRow.model = cutLast(currentFastRow.model)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.MODEL
                lastFastField = FastField.MODEL
            } else if (currentFastRow.width.isNotEmpty()) {
                currentFastRow.width = cutLast(currentFastRow.width)
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.WIDTH
                lastFastField = FastField.WIDTH
            } else if (savedFastRows.isNotEmpty()) {
                currentFastRow = savedFastRows.removeAt(savedFastRows.lastIndex)
                lastFastField = guessLastFastField(currentFastRow)
                currentFastNumericField = when (lastFastField) {
                    FastField.WIDTH -> FastField.WIDTH
                    FastField.MODEL -> FastField.WIDTH
                    FastField.LENGTH -> FastField.LENGTH
                    FastField.QUANTITY -> FastField.QUANTITY
                }
                currentFastActiveField = lastFastField
                deleteLastFastInput()
                return
            }
        }
    }

    updateDisplayTable()
    triggerAutoSave()
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
    triggerAutoSave()
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

    if (packageFastRowsMap.isEmpty()) return ""

    val builder = StringBuilder()
    builder.append("#CURRENT_PACKAGE=").append(currentPackageName).append("\n")

    packageFastRowsMap.forEach { (packageName, rows) ->
        builder.append("#PACKAGE=").append(packageName).append("\n")
        builder.append("#ROWS").append("\n")
        rows.forEach {
            builder.append(
                listOf(it.width, it.model, it.length, it.quantity).joinToString("\t")
            ).append("\n")
        }
        builder.append("#CURRENT_ROW").append("\n")
        val current = packageCurrentFastRowMap[packageName] ?: FastRow()
        builder.append(
            listOf(current.width, current.model, current.length, current.quantity).joinToString("\t")
        ).append("\n")
        builder.append("#END_PACKAGE").append("\n")
    }

    return builder.toString()
}

 fun MainActivity.deserializePackageFastContent(content: String) {
    packageFastRowsMap.clear()
    packageCurrentFastRowMap.clear()

    if (content.isBlank()) return

    if (!content.contains("#PACKAGE=")) {
        val oldRows = deserializeFastContentOld(content)
        if (oldRows.isNotEmpty()) {
            val defaultPackage = if (currentPackageName.isBlank()) "第1包" else currentPackageName
            packageFastRowsMap[defaultPackage] = oldRows.toMutableList()
            packageCurrentFastRowMap[defaultPackage] = FastRow()
            if (currentPackageName.isBlank()) currentPackageName = defaultPackage
        }
        return
    }

    val lines = content.replace("\r\n", "\n").split("\n")
    var packageName = ""
    var inRows = false
    var inCurrentRow = false
    var rows = mutableListOf<FastRow>()

    lines.forEach { line ->
        when {
            line.startsWith("#CURRENT_PACKAGE=") -> {
                if (currentPackageName.isBlank()) {
                    currentPackageName = line.removePrefix("#CURRENT_PACKAGE=").trim()
                }
            }

            line.startsWith("#PACKAGE=") -> {
                packageName = line.removePrefix("#PACKAGE=").trim()
                rows = mutableListOf()
                inRows = false
                inCurrentRow = false
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
                }
                packageName = ""
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

