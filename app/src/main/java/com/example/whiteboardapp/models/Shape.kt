package com.example.whiteboardapp.models

sealed class Shape {
    data class Rectangle(val type: String = "rectangle",var topLeft: List<Float>, var bottomRight: List<Float>, var color: Int) : Shape(){
        fun getResizeHandle(): List<Float> = bottomRight
    }
    data class Circle(
        val type: String = "circle",
        var center: MutableList<Float>,
        val radius: Float,
        val color: Int
    ): Shape() {
        fun getResizeHandle(): Pair<Float, Float> {
            return Pair(center[0] + radius, center[1])
        }
    }

    data class Line(
        val type: String = "line",
        var start: MutableList<Float>,
        var end: MutableList<Float>,
        var color: Int
    ) : Shape() {
    }


    data class Polygon(
        val type: String = "polygon",
        var points: MutableList<List<Float>>,
        var color: Int
    ) : Shape() {
    }
}