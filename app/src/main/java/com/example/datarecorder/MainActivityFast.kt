package com.example.datarecorder

import java.util.Locale
import android.view.View


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

     refreshFastVisibleCellsOnly()
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

     refreshFastVisibleCellsOnly()
     triggerAutoSave()

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

        triggerAutoSave()
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

    triggerAutoSave()
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
        triggerAutoSave()
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

        triggerAutoSave()
        return
    }

    currentFastNumericField = FastField.WIDTH
    currentFastActiveField = FastField.WIDTH
    lastFastField = FastField.WIDTH
    refreshFastSelectionOnly(null, true, null, true)
    triggerAutoSave()
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
            listOf(current.width, current.model, current.length, current.quantity).joinToString("\t")
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
                    packageDateMap[packageName] = if (packageDate.isBlank()) getTodayPackageDate() else packageDate
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
                    packageDateMap[packageName] = if (packageDate.isBlank()) getTodayPackageDate() else packageDate
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
        packageDateMap[packageName] = if (packageDate.isBlank()) getTodayPackageDate() else packageDate
    }

    if (currentPackageName.isBlank()) {
        currentPackageName = packageFastRowsMap.keys.firstOrNull().orEmpty()
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

