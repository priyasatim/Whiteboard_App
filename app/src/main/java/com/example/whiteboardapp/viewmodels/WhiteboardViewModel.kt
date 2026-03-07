package com.example.whiteboardapp.viewmodels

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.graphics.toColorInt

class WhiteboardViewModel : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _shapes = MutableStateFlow<List<Shape>>(emptyList())
    val shapes: StateFlow<List<Shape>> = _shapes

    private val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    val texts: StateFlow<List<TextItem>> = _texts


    // Current drawing options
    var currentColor = "#000000".toColorInt()
    var currentWidth = 5f


    // Change brush color
    fun changeColor(color: Int) {
        // Saving JSON
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

    // Update existing text
    fun updateText(updatedText: TextItem) {
        _texts.value = _texts.value.map { text ->
            if (text.position == updatedText.position) updatedText else text
        }
    }

}