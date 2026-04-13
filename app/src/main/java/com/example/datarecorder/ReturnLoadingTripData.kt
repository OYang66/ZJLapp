package com.example.datarecorder

data class ReturnLoadingTripData(
    var tripName: String = "",
    var aluminumRows: MutableList<ReturnLoadingRow> = mutableListOf(),
    var ironRows: MutableList<ReturnLoadingRow> = mutableListOf(),
    var vehicleInfo: VehicleInfo = VehicleInfo()
)
