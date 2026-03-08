package com.example.whiteboardapp.models

import android.graphics.Path

data class Stroke(
    val path: Path = Path(),
    val points: MutableList<List<Float>> = mutableListOf(),
    val color: Int = 0xFF000000.toInt(),
    val width: Float = 5f,
    val tool: ToolType? = ToolType.PEN
)