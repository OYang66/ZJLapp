package com.example.datarecorder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val buildingName: String = "",
    val standardContent: String = "[]",
    val fastContent: String = "[]",
    val loadingContent: String = ""
)
