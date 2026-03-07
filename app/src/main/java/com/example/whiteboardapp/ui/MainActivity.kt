package com.example.whiteboardapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {
    private val viewModel: WhiteboardViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ClickableViewAccessibility")
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
                    viewModel.texts.collect { texts ->
                        binding.drawingCanvas.texts = texts.toMutableList()
                        binding.drawingCanvas.invalidate()
                    }
                }
            }
        }

        // ----- Color buttons -----
        binding.btnColorRed.setOnClickListener { viewModel.changeColor("#FF0000".toColorInt()) }
        binding.btnColorBlue.setOnClickListener { viewModel.changeColor("#0000FF".toColorInt()) }
        binding.btnColorGreen.setOnClickListener { viewModel.changeColor("#00FF00".toColorInt()) }

        // ----- Stroke width -----
        binding.btnThin.setOnClickListener { viewModel.changeWidth(3f) }
        binding.btnMedium.setOnClickListener { viewModel.changeWidth(6f) }
        binding.btnThick.setOnClickListener { viewModel.changeWidth(10f) }

        // ----- Shapes -----
        binding.ivAddShape.setOnClickListener {
            showShapeDialog(this)
        }

        // ----- Text -----
        binding.btnText.setOnClickListener {
            val lastY = viewModel.texts.value.lastOrNull()?.position?.second ?: 500f
            val text = TextItem(
                "Hello!",
                Pair(200f, lastY + 60f), // offset new text
                viewModel.currentColor,
                48f
            )
            viewModel.addText(text)
            binding.drawingCanvas.invalidate() // redraw canvas
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
                                val dx = handle[0] - event.x
                                val dy = handle[1] - event.y
                                dx*dx + dy*dy <= canvas.handleRadius*canvas.handleRadius
                            }

                            is Shape.Circle -> {

                                val handle = shape.getResizeHandle()

                                val dx = handle.first - event.x
                                val dy = handle.second - event.y

                                dx * dx + dy * dy <= canvas.handleRadius * canvas.handleRadius
                            }

                            is Shape.Line -> {

                                val start = shape.start
                                val end = shape.end

                                val dxStart = start[0] - event.x
                                val dyStart = start[1] - event.y

                                val dxEnd = end[0] - event.x
                                val dyEnd = end[1] - event.y

                                val startTouched =
                                    dxStart * dxStart + dyStart * dyStart <= canvas.handleRadius * canvas.handleRadius

                                val endTouched =
                                    dxEnd * dxEnd + dyEnd * dyEnd <= canvas.handleRadius * canvas.handleRadius

                                startTouched || endTouched
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
                        val tappedText = viewModel.texts.value.find { textItem ->
                            val textPaint = Paint()
                            textPaint.textSize = textItem.size
                            textPaint.color = textItem.color
                            textPaint.style = Paint.Style.FILL

                            val bounds = Rect()
                            textPaint.getTextBounds(textItem.text, 0, textItem.text.length, bounds)

                            val left = textItem.position.first
                            val top = textItem.position.second - bounds.height() // baseline - height
                            val right = left + bounds.width()
                            val bottom = textItem.position.second

                            val padding = 20f // optional, make tapping easier
                            event.x in (left - padding)..(right + padding) &&
                                    event.y in (top - padding)..(bottom + padding)
                        }

                        if (tappedText != null) {
                            // User tapped an existing text → open edit dialog
                            showEditTextDialog(tappedText)
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
                                shape.bottomRight = listOf(
                                    shape.bottomRight[0] + dx,
                                    shape.bottomRight[1] + dy
                                ) as MutableList<Float>
                            }
                            is Shape.Circle -> {
                                val handle = shape.getResizeHandle()

                                val dx = handle.first - event.x
                                val dy = handle.second - event.y

                                val touchRadius = canvas.handleRadius * 3

                                dx * dx + dy * dy <= touchRadius * touchRadius
                            }
                            is Shape.Line -> {

                                shape.end = mutableListOf(event.x, event.y)
                            }
                            is Shape.Polygon -> {

                                val index = canvas.selectedPolygonPointIndex

                                if (index != -1) {
                                    shape.points[index] = mutableListOf(event.x, event.y)
                                }
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
                                    shape.topLeft = listOf(
                                        shape.topLeft[0] + dx,
                                        shape.topLeft[1] + dy
                                    ) as MutableList<Float>
                                    shape.bottomRight = listOf(
                                        shape.bottomRight[0] + dx,
                                        shape.bottomRight[1] + dy
                                    ) as MutableList<Float>
                                }
                                is Shape.Circle -> {
                                    val newX = shape.center[0] + dx
                                    val newY = shape.center[1] + dy

                                    shape.center = listOf(newX, newY) as MutableList<Float>
                                }
                                is Shape.Line -> {
                                    shape.start = mutableListOf(shape.start[0] + dx, shape.start[1] + dy)
                                    shape.end = mutableListOf(shape.end[0] + dx, shape.end[1] + dy)
                                }

                                is Shape.Polygon -> {
                                    shape.points = shape.points.map { point ->
                                        mutableListOf(point[0] + dx, point[1] + dy)
                                    }.toMutableList()
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

    fun showShapeDialog(context: Context) {

        val shapeNames = listOf("Rectangle", "Circle", "Line", "Polygon")

        val shapeIcons = listOf(
            R.drawable.ic_rectangle,
            R.drawable.ic_circle,
            R.drawable.ic_line,
            R.drawable.ic_polgon
        )

        val adapter = object : ArrayAdapter<String>(
            context,
            R.layout.dialog_shape_item,
            R.id.textShape,
            shapeNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                val icon = view.findViewById<ImageView>(R.id.iconShape)
                val text = view.findViewById<TextView>(R.id.textShape)

                text.text = shapeNames[position]
                icon.setImageResource(shapeIcons[position])

                return view
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Insert Shape")
            .setAdapter(adapter) { _, which ->

                when (which) {
                    0 -> {
                    /* Rectangle selected */
                        val rect = Shape.Rectangle(
                            topLeft = mutableListOf(100f, 100f),
                            bottomRight = mutableListOf(300f, 200f),
                            color = viewModel.currentColor
                        )
                        viewModel.addShape(rect)
                    }
                    1 -> {
                    /* Circle selected */
                        val circle = Shape.Circle(
                            center = listOf(400f, 400f) as MutableList<Float>,
                            radius = 100f,
                            color = viewModel.currentColor
                        )

                        viewModel.addShape(circle)
                    }
                    2 -> {
                    /* Line selected */
                        val line = Shape.Line(
                            start = mutableListOf(50f, 50f),
                            end = mutableListOf(300f, 300f),
                            color = viewModel.currentColor
                        )
                        viewModel.addShape(line)
                    }

                    3 -> {
                    /* Polygon selected */
                        val polygon = Shape.Polygon(
                            points =
                            mutableListOf(
                                listOf(200f, 200f),
                                listOf(300f, 150f),
                                listOf(400f, 250f),
                                listOf(300f, 300f)
                            ),
                            color = viewModel.currentColor
                        )

                        viewModel.addShape(polygon)
                    }
                }

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditTextDialog(textItem: TextItem) {
        // Create an EditText pre-filled with the current text
        val editText = EditText(this)
        editText.setText(textItem.text)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        val canvasView = findViewById<DrawingCanvas>(R.id.drawingCanvas)

        // 2️⃣ Build and show AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Edit Text")
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                // Update the text in the TextItem
                textItem.text = editText.text.toString()
                // If using StateFlow, update the ViewModel
                viewModel.updateText(textItem)
                canvasView.invalidate()  // Forces redraw
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}