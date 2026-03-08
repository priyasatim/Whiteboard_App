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
    var shapes: MutableStateFlow<List<Shape>> = _shapes

    val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    var texts: MutableStateFlow<List<TextItem>> = _texts


    // Current drawing options
    var currentColor = "#000000".toColorInt()
    var currentWidth = 5f
    var currentStroke: Stroke? = null
    private val redoStack = mutableListOf<Stroke>()


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

    fun addStroke(stroke: Stroke) {
        _strokes.value = _strokes.value + stroke
        redoStack.clear()
    }

    fun startStroke(x: Float, y: Float,width : Float) {

        currentStroke = Stroke(
            points = mutableListOf(),
            color = currentColor,
            width = width
        )

        currentStroke?.points?.add(listOf(x, y))

        addStroke(currentStroke!!)
    }

    fun continueStroke(x: Float, y: Float) {
        currentStroke?.points?.add(listOf(x, y))
    }

    fun undo() {

        val current = _strokes.value

        if (current.isNotEmpty()) {

            val lastStroke = current.last()

            redoStack.add(lastStroke)

            _strokes.value = current.dropLast(1)
        }
    }

    fun redo() {

        if (redoStack.isNotEmpty()) {

            val stroke = redoStack.removeAt(redoStack.lastIndex)

            _strokes.value = _strokes.value + stroke
        }
    }


}