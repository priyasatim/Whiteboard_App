package com.example.whiteboardapp.models

sealed class Shape {
    data class Rectangle(val topLeft: Pair<Float, Float>, val bottomRight: Pair<Float, Float>, val color: Int) : Shape()
    data class Circle(val center: Pair<Float, Float>, val radius: Float, val color: Int) : Shape()
    data class Line(val start: Pair<Float, Float>, val end: Pair<Float, Float>, val color: Int) : Shape()
    data class Polygon(val points: List<Pair<Float, Float>>, val color: Int) : Shape()
}