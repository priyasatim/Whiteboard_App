package com.example.whiteboardapp.ui

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.whiteboardapp.R
import com.example.whiteboardapp.databinding.ActivityMainBinding
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem
import com.example.whiteboardapp.viewmodels.WhiteboardViewModel
import com.example.whiteboardapp.views.DrawingCanvas
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: WhiteboardViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe flows and update canvas
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.strokes.collect {
                        binding.drawingCanvas.strokes = it
                        binding.drawingCanvas.invalidate()
                    }
                }
                launch {
                    viewModel.shapes.collect {
                        binding.drawingCanvas.shapes = it
                        binding.drawingCanvas.invalidate()
                    }
                }
                launch {
                    viewModel.texts.collect {
                        binding.drawingCanvas.texts = it
                        binding.drawingCanvas.invalidate()
                    }
                }
            }
        }

        // ----- Color buttons -----
        binding.btnColorRed.setOnClickListener { viewModel.changeColor(0xFFFF0000.toInt()) }
        binding.btnColorBlue.setOnClickListener { viewModel.changeColor(0xFF0000FF.toInt()) }
        binding.btnColorGreen.setOnClickListener { viewModel.changeColor(0xFF00FF00.toInt()) }

        // ----- Stroke width -----
        binding.btnThin.setOnClickListener { viewModel.changeWidth(3f) }
        binding.btnMedium.setOnClickListener { viewModel.changeWidth(6f) }
        binding.btnThick.setOnClickListener { viewModel.changeWidth(10f) }

        // ----- Shapes -----
        binding.btnRectangle.setOnClickListener {
            val rect = Shape.Rectangle(Pair(100f, 100f), Pair(300f, 200f), viewModel.currentColor)
            viewModel.addShape(rect)
        }
        binding.btnCircle.setOnClickListener {
            val circle = Shape.Circle(Pair(400f, 400f), 100f, viewModel.currentColor)
            viewModel.addShape(circle)
        }
        binding.btnLine.setOnClickListener {
            val line = Shape.Line(Pair(50f, 50f), Pair(300f, 300f), viewModel.currentColor)
            viewModel.addShape(line)
        }

        // ----- Text -----
        binding.btnText.setOnClickListener {
            val text = TextItem("Hello!", Pair(200f, 500f), viewModel.currentColor, 48f)
            viewModel.addText(text)
        }

        // ----- Save -----
        binding.btnSave.setOnClickListener {
            saveCanvas()
        }

        // ----- Touch on canvas for freehand -----
        binding.drawingCanvas.setOnTouchListener { v, event ->
            val canvas = v as DrawingCanvas
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touching resize handle
                    canvas.resizeShape = canvas.shapes.find { shape ->
                        when(shape) {
                            is Shape.Rectangle -> {
                                val handle = shape.getResizeHandle()
                                val dx = handle.first - event.x
                                val dy = handle.second - event.y
                                dx*dx + dy*dy <= canvas.handleRadius*canvas.handleRadius
                            }
                            is Shape.Circle -> {
                                val handle = shape.getResizeHandle()
                                val dx = handle.first - event.x
                                val dy = handle.second - event.y
                                dx*dx + dy*dy <= canvas.handleRadius*canvas.handleRadius
                            }
                            else -> false
                        }
                    }

                    if(canvas.resizeShape != null) {
                        canvas.isResizing = true
                        canvas.resizeStartX = event.x
                        canvas.resizeStartY = event.y
                        true
                    } else {
                        // Normal drag or stroke logic
                        canvas.currentShape = canvas.findShapeAt(event.x, event.y)
                        if (canvas.currentShape == null) {
                            val stroke = Stroke(
                                mutableListOf(Pair(event.x, event.y)),
                                viewModel.currentColor,
                                viewModel.currentWidth
                            )
                            canvas.currentStroke = stroke
                        }
                        canvas.lastTouchX = event.x
                        canvas.lastTouchY = event.y
                        true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if(canvas.isResizing) {
                        val dx = event.x - canvas.resizeStartX
                        val dy = event.y - canvas.resizeStartY
                        when(val shape = canvas.resizeShape) {
                            is Shape.Rectangle -> {
                                shape.bottomRight = Pair(shape.bottomRight.first + dx, shape.bottomRight.second + dy)
                            }
                            is Shape.Circle -> {
                                val newRadius = shape.radius + maxOf(dx, dy)
                                shape.radius = newRadius.coerceAtLeast(10f)
                            }

                            else -> {}
                        }
                        canvas.resizeStartX = event.x
                        canvas.resizeStartY = event.y
                        canvas.invalidate()
                        true
                    } else {
                        // Existing drag / stroke logic
                        canvas.currentShape?.let { shape ->
                            val dx = event.x - canvas.lastTouchX
                            val dy = event.y - canvas.lastTouchY
                            when (shape) {
                                is Shape.Rectangle -> {
                                    shape.topLeft = Pair(shape.topLeft.first + dx, shape.topLeft.second + dy)
                                    shape.bottomRight = Pair(shape.bottomRight.first + dx, shape.bottomRight.second + dy)
                                }
                                is Shape.Circle -> {
                                    shape.center = Pair(shape.center.first + dx, shape.center.second + dy)
                                }
                                is Shape.Line -> {
                                    shape.start = Pair(shape.start.first + dx, shape.start.second + dy)
                                    shape.end = Pair(shape.end.first + dx, shape.end.second + dy)
                                }
                                is Shape.Polygon -> {
                                    shape.points = shape.points.map { Pair(it.first + dx, it.second + dy) }
                                }
                            }
                            canvas.lastTouchX = event.x
                            canvas.lastTouchY = event.y
                        }

                        canvas.currentShape ?: canvas.currentStroke?.points?.add(Pair(event.x, event.y))
                        canvas.invalidate()
                        true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    canvas.currentStroke?.let { viewModel.addStroke(it) }
                    canvas.currentStroke = null
                    canvas.currentShape = null
                    canvas.isResizing = false
                    canvas.resizeShape = null
                    true
                }

                else -> false
            }
        }
    }

    private fun saveCanvas() {
        // Convert strokes, shapes, texts to JSON (use Gson)
        val data = mapOf(
            "strokes" to viewModel.strokes.value,
            "shapes" to viewModel.shapes.value,
            "texts" to viewModel.texts.value
        )
        val json = Gson().toJson(data)
        // Save to file (internal storage)
        val filename = "whiteboard_${System.currentTimeMillis()}.json"
        openFileOutput(filename, MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
        Toast.makeText(this, "Saved $filename", Toast.LENGTH_SHORT).show()
    }
}