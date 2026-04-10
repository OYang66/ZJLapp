package com.example.datarecorder

fun StandardRow.isEmpty(): Boolean {
    return installNo.isBlank() && model.isBlank() && quantity.isBlank()
}
