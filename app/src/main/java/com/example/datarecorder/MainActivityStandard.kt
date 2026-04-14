package com.example.datarecorder
import android.view.View


 fun MainActivity.handleStandardTokenInput(token: String) {
    if (!ensurePackageSelected()) return

    if (hasStandardEditingTarget()) {
        appendTextToEditingStandardCell(token)
        return
    }

    appendStandardText(token)
}

 fun MainActivity.appendStandardText(text: String) {
    when (currentStandardField) {
        StandardField.INSTALL_NO -> {
            if (!canAppendToInstallNo(text)) {
                toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
                return
            }
            currentStandardRow.installNo += text
            lastStandardField = StandardField.INSTALL_NO
        }

        StandardField.MODEL -> {
            currentStandardRow.model = appendModelToken(currentStandardRow.model, text)
            lastStandardField = StandardField.MODEL
        }

        StandardField.QUANTITY -> {
            if (text.all { it.isDigit() }) {
                currentStandardRow.quantity += text
                lastStandardField = StandardField.QUANTITY
            } else {
                if (containsLetters(text)) {
                    toast("数量内不能输入字母")
                }
                return
            }
        }
    }

     refreshStandardVisibleCellsOnly()
     triggerAutoSave()

 }

 fun MainActivity.appendStandardToLastField(text: String) {
    if (!ensurePackageSelected()) return

    if (hasStandardEditingTarget()) {
        appendTextToEditingStandardCell(text)
        return
    }

    when (lastStandardField) {
        StandardField.INSTALL_NO -> {
            if (!canAppendToInstallNo(text)) {
                toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
                return
            }
            currentStandardRow.installNo += text
        }

        StandardField.MODEL -> {
            currentStandardRow.model += text
        }

        StandardField.QUANTITY -> {
            if (text.all { it.isDigit() }) {
                currentStandardRow.quantity += text
            } else {
                if (containsLetters(text)) {
                    toast("数量内不能输入字母")
                }
                return
            }
        }
    }

     refreshStandardVisibleCellsOnly()
     triggerAutoSave()

 }


 fun MainActivity.canAppendToInstallNo(text: String): Boolean {
    return text.all { it.isDigit() } || text in allowedInstallNoTokens
}


fun MainActivity.moveToNextStandardColumn() {
    if (!ensurePackageSelected()) return

    if (moveEditingStandardCellToNextColumn()) {
        triggerAutoSave()
        return
    }

    val oldField = currentStandardField

    currentStandardField = when (currentStandardField) {
        StandardField.INSTALL_NO -> StandardField.MODEL
        StandardField.MODEL -> StandardField.QUANTITY
        StandardField.QUANTITY -> StandardField.INSTALL_NO
    }
    lastStandardField = currentStandardField

    if (oldField != currentStandardField) {
        refreshStandardSelectionOnly(
            oldSavedRowIndex = null,
            oldWasCurrentRow = true,
            newSavedRowIndex = null,
            newWasCurrentRow = true
        )
    }

    triggerAutoSave()
}



fun MainActivity.finishCurrentStandardRow() {
    if (!ensurePackageSelected()) return

    if (editingStandardRowIndex != null) {
        val oldRowIndex = editingStandardRowIndex
        clearStandardEditingState()
        if (oldRowIndex != null) {
            rebuildStandardRowOnly(oldRowIndex, false)
        }
        triggerAutoSave()
        return
    }

    if (!currentStandardRow.isEmpty()) {
        val savedIndex = savedStandardRows.size
        savedStandardRows.add(currentStandardRow.copy())

        // 把原来的“当前行”改造成“已保存行”，不能再按 current row 重建
        rebuildStandardRowOnly(savedIndex, false)

        currentStandardRow = StandardRow()
        currentStandardField = StandardField.INSTALL_NO
        lastStandardField = StandardField.INSTALL_NO

        addStandardDataRow(
            displayIndex = savedStandardRows.size + 1,
            data = currentStandardRow.copy(),
            isCurrentRow = true,
            savedRowIndex = null
        )

        tvSummaryPrimary.visibility = View.VISIBLE
        tvSummarySecondary.visibility = View.GONE
        tvSummaryPrimary.text = "合计数量：${calculateStandardTotalQty()}"

        bodyVerticalScroll.post {
            bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
        }

        triggerAutoSave()
        return
    }

    currentStandardField = StandardField.INSTALL_NO
    lastStandardField = StandardField.INSTALL_NO
    refreshStandardSelectionOnly(null, true, null, true)
    triggerAutoSave()
}



fun MainActivity.deleteLastStandardInput() {
    if (!ensurePackageSelected()) return
    if (deleteFromEditingStandardCell()) return

    fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

    when (lastStandardField) {
        StandardField.INSTALL_NO -> {
            if (currentStandardRow.installNo.isNotEmpty()) {
                currentStandardRow.installNo = cutLast(currentStandardRow.installNo)
            } else if (currentStandardRow.model.isNotEmpty()) {
                currentStandardRow.model = cutLast(currentStandardRow.model)
                lastStandardField = StandardField.MODEL
            } else if (currentStandardRow.quantity.isNotEmpty()) {
                currentStandardRow.quantity = cutLast(currentStandardRow.quantity)
                lastStandardField = StandardField.QUANTITY
            } else if (savedStandardRows.isNotEmpty()) {
                currentStandardRow = savedStandardRows.removeAt(savedStandardRows.lastIndex)
                lastStandardField = guessLastStandardField(currentStandardRow)
                deleteLastStandardInput()
                return
            }
        }

        StandardField.MODEL -> {
            if (currentStandardRow.model.isNotEmpty()) {
                currentStandardRow.model = cutLast(currentStandardRow.model)
            } else if (currentStandardRow.installNo.isNotEmpty()) {
                currentStandardRow.installNo = cutLast(currentStandardRow.installNo)
                lastStandardField = StandardField.INSTALL_NO
            } else if (currentStandardRow.quantity.isNotEmpty()) {
                currentStandardRow.quantity = cutLast(currentStandardRow.quantity)
                lastStandardField = StandardField.QUANTITY
            } else if (savedStandardRows.isNotEmpty()) {
                currentStandardRow = savedStandardRows.removeAt(savedStandardRows.lastIndex)
                lastStandardField = guessLastStandardField(currentStandardRow)
                deleteLastStandardInput()
                return
            }
        }

        StandardField.QUANTITY -> {
            if (currentStandardRow.quantity.isNotEmpty()) {
                currentStandardRow.quantity = cutLast(currentStandardRow.quantity)
            } else if (currentStandardRow.model.isNotEmpty()) {
                currentStandardRow.model = cutLast(currentStandardRow.model)
                lastStandardField = StandardField.MODEL
            } else if (currentStandardRow.installNo.isNotEmpty()) {
                currentStandardRow.installNo = cutLast(currentStandardRow.installNo)
                lastStandardField = StandardField.INSTALL_NO
            } else if (savedStandardRows.isNotEmpty()) {
                currentStandardRow = savedStandardRows.removeAt(savedStandardRows.lastIndex)
                lastStandardField = guessLastStandardField(currentStandardRow)
                deleteLastStandardInput()
                return
            }
        }
    }

     refreshStandardVisibleCellsOnly()
     triggerAutoSave()

 }


fun MainActivity.serializeStandardContent(): String {
    saveScreenDataToCurrentPackage()

    if (packageStandardRowsMap.isEmpty()) return ""

    val builder = StringBuilder()
    builder.append("#CURRENT_PACKAGE=").append(currentPackageName).append("\n")

    packageStandardRowsMap.forEach { (packageName, rows) ->
        builder.append("#PACKAGE=").append(packageName).append("\n")
        builder.append("#ROWS").append("\n")
        rows.forEach {
            builder.append(
                listOf(it.installNo, it.model, it.quantity).joinToString("\t")
            ).append("\n")
        }
        builder.append("#CURRENT_ROW").append("\n")
        val current = packageCurrentStandardRowMap[packageName] ?: StandardRow()
        builder.append(
            listOf(current.installNo, current.model, current.quantity).joinToString("\t")
        ).append("\n")
        builder.append("#END_PACKAGE").append("\n")
    }

    return builder.toString()
}
 fun MainActivity.deserializePackageStandardContent(content: String) {
    packageStandardRowsMap.clear()
    packageCurrentStandardRowMap.clear()

    if (content.isBlank()) return

    if (!content.contains("#PACKAGE=")) {
        val oldRows = deserializeStandardContentOld(content)
        if (oldRows.isNotEmpty()) {
            val defaultPackage = "第1包"
            packageStandardRowsMap[defaultPackage] = oldRows.toMutableList()
            packageCurrentStandardRowMap[defaultPackage] = StandardRow()
            currentPackageName = defaultPackage
        }
        return
    }

    val lines = content.replace("\r\n", "\n").split("\n")
    var packageName = ""
    var inRows = false
    var inCurrentRow = false
    var rows = mutableListOf<StandardRow>()

    lines.forEach { line ->
        when {
            line.startsWith("#CURRENT_PACKAGE=") -> {
                currentPackageName = line.removePrefix("#CURRENT_PACKAGE=").trim()
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
                    packageStandardRowsMap[packageName] = rows
                    packageCurrentStandardRowMap.putIfAbsent(packageName, StandardRow())
                }
                packageName = ""
                inRows = false
                inCurrentRow = false
            }

            inRows && line.isNotBlank() -> {
                val parts = line.split("\t")
                rows.add(
                    StandardRow(
                        installNo = parts.getOrNull(0).orEmpty(),
                        model = parts.getOrNull(1).orEmpty(),
                        quantity = parts.getOrNull(2).orEmpty()
                    )
                )
            }

            inCurrentRow -> {
                val parts = line.split("\t")
                packageCurrentStandardRowMap[packageName] = StandardRow(
                    installNo = parts.getOrNull(0).orEmpty(),
                    model = parts.getOrNull(1).orEmpty(),
                    quantity = parts.getOrNull(2).orEmpty()
                )
                inCurrentRow = false
            }
        }
    }
}

 fun deserializeStandardContentOld(content: String): List<StandardRow> {
    if (content.isBlank()) return emptyList()

    return content.replace("\r\n", "\n")
        .split("\n")
        .mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split("\t")
            StandardRow(
                installNo = parts.getOrNull(0).orEmpty(),
                model = parts.getOrNull(1).orEmpty(),
                quantity = parts.getOrNull(2).orEmpty()
            )
        }
}
