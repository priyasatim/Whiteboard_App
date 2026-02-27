package com.example.whiteboardapp.models

data class TextItem(
    var text: String,
    var position: Pair<Float, Float>,
    var color: Int = 0xFF000000.toInt(),
    var size: Float = 24f
)