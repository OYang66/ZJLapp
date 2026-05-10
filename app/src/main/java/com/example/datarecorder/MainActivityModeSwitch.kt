package com.example.datarecorder

import android.view.View

// =========================
// 模式切换
// =========================
fun MainActivity.showModeSwitchMenu(anchor: View) {
	showMenuCardPopup(
		anchor = anchor,
		title = "切换模式",
		subtitle = "快速切换当前录入方式",
		sections = listOf(
			MenuCardSection(
				items = listOf(
					MenuCardItem("返厂统计", onClick = { switchMode(ModeType.FAST) }, selected = currentModeType == ModeType.FAST),
					MenuCardItem("型号统计", onClick = { switchMode(ModeType.STANDARD) }, selected = currentModeType == ModeType.STANDARD),
					MenuCardItem("返厂装车", onClick = { switchMode(ModeType.RETURN_LOADING) }, selected = currentModeType == ModeType.RETURN_LOADING),
					MenuCardItem("质量反馈", onClick = { switchMode(ModeType.QUALITY_FEEDBACK) }, selected = currentModeType == ModeType.QUALITY_FEEDBACK)
				)
			)
		)
	)
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


