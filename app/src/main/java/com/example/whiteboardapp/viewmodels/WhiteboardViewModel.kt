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


    // Current drawing options
    var currentColor = 0xFF000000.toInt()
    var currentWidth = 5f

    // Add a new stroke
    fun addStroke(stroke: Stroke) {
        _strokes.value = _strokes.value + stroke
    }

    // Change brush color
    fun changeColor(color: Int) {
        currentColor = color
    }

    // Change brush width
    fun changeWidth(width: Float) {
        currentWidth = width
    }

    // Add shapes or texts
    fun addShape(shape: Shape) {
        _shapes.value = _shapes.value + shape
    }

    fun addText(text: TextItem) {
        _texts.value = _texts.value + text
    }
}