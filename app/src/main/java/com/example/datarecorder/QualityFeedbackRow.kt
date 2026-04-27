package com.example.datarecorder

data class QualityFeedbackRow(
    var materialType: String = "",
    var installNo: String = "",
    var model: String = "",
    var qualityType: String = "",
    var feedbackDesc: String = "",
    var photos: MutableList<QualityFeedbackPhotoItem> = mutableListOf()
) {
    fun isEmpty(): Boolean {
        return materialType.isBlank() &&
                installNo.isBlank() &&
                model.isBlank() &&
                qualityType.isBlank() &&
                feedbackDesc.isBlank() &&
                photos.isEmpty()
    }
}
