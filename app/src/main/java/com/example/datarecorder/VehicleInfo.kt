package com.example.datarecorder

data class VehicleInfo(
    var grossWeight: String = "",
    var tareWeight: String = "",
    var middleAluminumWeight: String = "",
    var middleIronWeight: String = "",
    var woodEstimate: String = "",
    var vehiclePlateNumber: String = "",
    var loadingDate: String = ""
) {
    fun netWeight(): Double {
        val gross = grossWeight.toDoubleOrNull() ?: 0.0
        val tare = tareWeight.toDoubleOrNull() ?: 0.0
        val wood = woodEstimate.toDoubleOrNull() ?: 0.0
        return gross - tare - wood
    }

    fun aluminumWeight(): Double {
        val gross = grossWeight.toDoubleOrNull() ?: 0.0
        val tare = tareWeight.toDoubleOrNull() ?: 0.0
        val middleAl = middleAluminumWeight.toDoubleOrNull()
        val middleIron = middleIronWeight.toDoubleOrNull()

        return when {
            middleAl != null -> middleAl - tare
            middleIron != null -> gross - middleIron
            else -> 0.0
        }
    }

    fun ironWeight(): Double {
        val gross = grossWeight.toDoubleOrNull() ?: 0.0
        val tare = tareWeight.toDoubleOrNull() ?: 0.0
        val middleAl = middleAluminumWeight.toDoubleOrNull()
        val middleIron = middleIronWeight.toDoubleOrNull()

        return when {
            middleAl != null -> gross - middleAl
            middleIron != null -> middleIron - tare
            else -> 0.0
        }
    }
}
