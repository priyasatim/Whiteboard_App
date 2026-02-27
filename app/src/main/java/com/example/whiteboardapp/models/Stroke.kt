package com.example.whiteboardapp.models

data class Stroke(
    val points: MutableList<Pair<Float, Float>> = mutableListOf(),
    val color: Int = 0xFF000000.toInt(),
    val width: Float = 5f
)