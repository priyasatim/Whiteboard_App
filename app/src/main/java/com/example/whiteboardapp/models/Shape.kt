package com.example.whiteboardapp.models

sealed class Shape {
    data class Rectangle(val type: String = "rectangle",var topLeft: MutableList<Float>, var bottomRight: MutableList<Float>, var color: Int, var strokeWidth: Float) : Shape(){
        fun getResizeHandle(): List<Float> = bottomRight
    }
    data class Circle(
        val type: String = "circle",
        var center: MutableList<Float>,
        var radius: Float,
        val color: Int,
        var strokeWidth: Float
    ): Shape() {
        fun getResizeHandle(): Pair<Float, Float> {
            return Pair(center[0] + radius, center[1])
        }
    }

    data class Line(
        val type: String = "line",
        var start: MutableList<Float>,
        var end: MutableList<Float>,
        var color: Int,
        var strokeWidth: Float
    ) : Shape() {
    }


    data class Polygon(
        val type: String = "polygon",
        var points: MutableList<MutableList<Float>>,
        var color: Int,
        var strokeWidth: Float
    ) : Shape() {
    }
}