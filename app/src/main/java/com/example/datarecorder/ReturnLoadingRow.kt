package com.example.datarecorder

data class ReturnLoadingRow(
    var type: ReturnLoadingType = ReturnLoadingType.ALUMINUM,
    var materialName: String = "",
    var packageOrCount: String = "",
    var areaOrWeight: String = "",
    var weight: String = "",
    var remark: String = ""
) {
    fun isEmpty(): Boolean {
        return materialName.isBlank() &&
                packageOrCount.isBlank() &&
                areaOrWeight.isBlank() &&
                weight.isBlank() &&
                remark.isBlank()
    }
}
