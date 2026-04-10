package com.example.datarecorder

fun FastRow.isEmpty(): Boolean {
    return width.isBlank() && model.isBlank() && length.isBlank() && quantity.isBlank()
}
