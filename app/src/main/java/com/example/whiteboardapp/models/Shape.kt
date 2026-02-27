package com.example.whiteboardapp.models

sealed class Shape {
    data class Rectangle(var topLeft: Pair<Float, Float>, var bottomRight: Pair<Float, Float>, var color: Int) : Shape(){
        fun getResizeHandle(): Pair<Float, Float> = bottomRight
    }
    data class Circle(var center: Pair<Float, Float>, var radius: Float, var color: Int) : Shape() {
        fun getResizeHandle(): Pair<Float, Float> = Pair(center.first + radius, center.second + radius)
    }
    data class Line(var start: Pair<Float, Float>, var end: Pair<Float, Float>, var color: Int) : Shape()
    data class Polygon(var points: List<Pair<Float, Float>>, var color: Int) : Shape()
}