package com.example.datarecorder

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

object JsonHelper {

    private val gson = Gson()

    data class StandardPackageData(
        val currentPackageName: String = "",
        val packages: MutableList<StandardPackageItem> = mutableListOf()
    )

    data class StandardPackageItem(
        val packageName: String = "",
        val savedRows: MutableList<StandardRow> = mutableListOf(),
        val currentRow: StandardRow = StandardRow()
    )

    data class FastPackageData(
        val currentPackageName: String = "",
        val packages: MutableList<FastPackageItem> = mutableListOf()
    )

    data class FastPackageItem(
        val packageName: String = "",
        val savedRows: MutableList<FastRow> = mutableListOf(),
        val currentRow: FastRow = FastRow()
    )

    fun standardToJson(list: List<StandardRow>): String {
        return gson.toJson(list)
    }

    fun fastToJson(list: List<FastRow>): String {
        return gson.toJson(list)
    }

    fun jsonToStandard(json: String?): MutableList<StandardRow> {
        if (json.isNullOrBlank()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<StandardRow>>() {}.type
            gson.fromJson<MutableList<StandardRow>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun jsonToFast(json: String?): MutableList<FastRow> {
        if (json.isNullOrBlank()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<FastRow>>() {}.type
            gson.fromJson<MutableList<FastRow>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun standardPackageToJson(data: StandardPackageData): String {
        return gson.toJson(data)
    }

    fun fastPackageToJson(data: FastPackageData): String {
        return gson.toJson(data)
    }

    fun jsonToStandardPackage(json: String?): StandardPackageData {
        if (json.isNullOrBlank()) return StandardPackageData()
        return try {
            val type = object : TypeToken<StandardPackageData>() {}.type
            gson.fromJson<StandardPackageData>(json, type) ?: StandardPackageData()
        } catch (_: Exception) {
            StandardPackageData()
        }
    }

    fun jsonToFastPackage(json: String?): FastPackageData {
        if (json.isNullOrBlank()) return FastPackageData()
        return try {
            val type = object : TypeToken<FastPackageData>() {}.type
            gson.fromJson<FastPackageData>(json, type) ?: FastPackageData()
        } catch (_: Exception) {
            FastPackageData()
        }
    }

    fun isPackageStandardJson(json: String?): Boolean {
        return try {
            if (json.isNullOrBlank()) false else JSONObject(json).has("packages")
        } catch (_: Exception) {
            false
        }
    }

    fun isPackageFastJson(json: String?): Boolean {
        return try {
            if (json.isNullOrBlank()) false else JSONObject(json).has("packages")
        } catch (_: Exception) {
            false
        }
    }
}
