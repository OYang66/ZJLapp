package com.example.datarecorder

import android.view.View
import android.widget.PopupMenu

fun MainActivity.showModeSwitchMenu(anchor: View) {
    val popup = PopupMenu(this, anchor)
    popup.menu.add(0, 1, 0, "返厂统计")
    popup.menu.add(0, 2, 1, "型号统计")
    popup.menu.add(0, 3, 2, "返厂装车")
    popup.menu.add(0, 4, 3, "质量反馈")

    popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
            1 -> {
                switchMode(ModeType.FAST)
                true
            }
            2 -> {
                switchMode(ModeType.STANDARD)
                true
            }
            3 -> {
                switchMode(ModeType.RETURN_LOADING)
                true
            }
            4 -> {
                switchMode(ModeType.QUALITY_FEEDBACK)
                true
            }
            else -> false
        }
    }
    popup.show()
}

fun MainActivity.switchMode(mode: ModeType) {
    currentModeType = mode
    isFastMode = mode == ModeType.FAST

    getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString("last_mode_type", mode.name)
        .putBoolean("is_fast_mode", isFastMode)
        .apply()

    standardModeContainer.visibility = if (mode == ModeType.STANDARD) View.VISIBLE else View.GONE
    fastModeContainer.visibility = if (mode == ModeType.FAST) View.VISIBLE else View.GONE
    loadingModeContainer.visibility = if (mode == ModeType.RETURN_LOADING) View.VISIBLE else View.GONE
    qualityModeContainer.visibility = if (mode == ModeType.QUALITY_FEEDBACK) View.VISIBLE else View.GONE

    btnModeToggle.text = "切换模式"

    when (mode) {
        ModeType.STANDARD -> {
            if (currentPackageName.isNotBlank()) {
                loadPackageToScreen(currentPackageName)
            } else {
                updatePackageButtonText()
                updateDisplayTable()
            }
        }

        ModeType.FAST -> {
            if (currentPackageName.isNotBlank()) {
                loadPackageToScreen(currentPackageName)
            } else {
                updatePackageButtonText()
                updateDisplayTable()
            }
        }

        ModeType.RETURN_LOADING -> {
            btnPackageMenu.text = if (currentLoadingTripName.isBlank()) "车次" else currentLoadingTripName
            renderLoadingTable()
        }

        ModeType.QUALITY_FEEDBACK -> {
            updatePackageButtonText()
            renderQualityFeedbackTable()
        }
    }
}


