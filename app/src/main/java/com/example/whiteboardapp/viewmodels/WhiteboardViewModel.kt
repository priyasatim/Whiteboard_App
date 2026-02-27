package com.example.whiteboardapp.viewmodels

import androidx.lifecycle.ViewModel
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WhiteboardViewModel : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _shapes = MutableStateFlow<List<Shape>>(emptyList())
    val shapes: StateFlow<List<Shape>> = _shapes

    private val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    val texts: StateFlow<List<TextItem>> = _texts

    private var currentStroke: Stroke? = null

    // Start new stroke
    fun startStroke(color: Int, width: Float) {
        currentStroke = Stroke(color = color, width = width)
        _strokes.value = _strokes.value + currentStroke!!
    }

    fun addPointToStroke(x: Float, y: Float) {
        currentStroke?.points?.add(x to y)
        _strokes.value = _strokes.value.toList()
    }

    fun endStroke() {
        currentStroke = null
    }

    // Add shapes or texts
    fun addShape(shape: Shape) {
        _shapes.value = _shapes.value + shape
    }

    fun addText(text: TextItem) {
        _texts.value = _texts.value + text
    }
}