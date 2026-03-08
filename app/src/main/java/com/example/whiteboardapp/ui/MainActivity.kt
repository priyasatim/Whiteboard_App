package com.example.whiteboardapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
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
import com.example.whiteboardapp.models.ToolType
import com.example.whiteboardapp.services.FileService
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val viewModel: WhiteboardViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var fileService: FileService
    private val gson = Gson()


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        setOnClickListner()
    }


    private fun init() {
        // Observe flows and update canvas
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.strokes.collect {
                        binding.drawingCanvas.strokes = it.toMutableList()
                        binding.drawingCanvas.invalidate()
                    }
                }
                launch {
                    viewModel.shapes.collect {
                        binding.drawingCanvas.shapes = it.toMutableList()
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

        binding.drawingCanvas.viewModel = viewModel
        fileService = FileService(this)


        val canvasView = findViewById<DrawingCanvas>(R.id.drawingCanvas)

        canvasView.textClickListener = { textItem ->
            showEditTextDialog(textItem)
        }

    }
        private fun setOnClickListner(){
        // ----- Color buttons -----
        binding.colorPalette.colorRed.setOnClickListener(clickListener)
        binding.colorPalette.colorBlue.setOnClickListener(clickListener)
        binding.colorPalette.colorGreen.setOnClickListener(clickListener)

        // ----- Stroke width -----
        binding.colorPalette.ivThin.setOnClickListener(clickListener)
        binding.colorPalette.ivMedium.setOnClickListener(clickListener)
        binding.colorPalette.ivThick.setOnClickListener(clickListener)

        binding.ivErase.setOnClickListener(clickListener)
        binding.ivAddShape.setOnClickListener(clickListener)
        binding.ivText.setOnClickListener(clickListener)
        binding.tvPng.setOnClickListener(clickListener)
        binding.ivSave.setOnClickListener(clickListener)
        binding.ivPencil.setOnClickListener(clickListener)
        binding.btnClosePreview.setOnClickListener(clickListener)

//        binding.ivUndo.setOnClickListener {
//            viewModel.undo()
//            binding.drawingCanvas.invalidate()
//        }
//
//
//        binding.ivRedo.setOnClickListener {
//            viewModel.redo()
//            binding.drawingCanvas.invalidate()
//        }
    }

    private val clickListener = View.OnClickListener { view ->

        when (view.id) {

            R.id.colorRed -> viewModel.changeColor("#FF0000".toColorInt())

            R.id.colorBlue -> viewModel.changeColor("#0000FF".toColorInt())

            R.id.colorGreen -> viewModel.changeColor("#00FF00".toColorInt())

            R.id.iv_thin -> viewModel.changeWidth(3f)

            R.id.iv_medium -> viewModel.changeWidth(6f)

            R.id.iv_thick -> viewModel.changeWidth(10f)

            R.id.iv_erase -> {
                viewModel.selectTool(ToolType.ERASER)
                updateToolUI(ToolType.ERASER)

                binding.drawingCanvas.eraserMode = !binding.drawingCanvas.eraserMode
            }

            R.id.iv_add_shape -> {
                viewModel.selectTool(ToolType.SHAPE)
                updateToolUI(ToolType.SHAPE)

                showShapeDialog(this)
            }

            R.id.iv_text -> {
                viewModel.selectTool(ToolType.TEXT)
                updateToolUI(ToolType.TEXT)

                addText()
            }

            R.id.tv_png -> {
                viewModel.selectTool(ToolType.SAVEPNG)
                updateToolUI(ToolType.SAVEPNG)

                val file = saveCanvasAsPNG()
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                binding.ivPreview.setImageBitmap(bitmap)
                binding.framelayoutPng.visibility = View.VISIBLE
            }

            R.id.iv_save -> {
                viewModel.selectTool(ToolType.SAVE)
                updateToolUI(ToolType.SAVE)
                saveCanvas()
            }

            R.id.iv_pencil -> {
                viewModel.currentTool = ToolType.PEN

                viewModel.selectTool(ToolType.PEN)
                updateToolUI(ToolType.PEN)

            }

            R.id.btnClosePreview -> {
                binding.framelayoutPng.visibility = View.GONE
            }
        }
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
                            color = viewModel.currentColor,
                            strokeWidth = viewModel.currentWidth
                        )
                        viewModel.addShape(rect)
                    }
                    1 -> {
                    /* Circle selected */
                        val circle = Shape.Circle(
                            center = listOf(400f, 400f) as MutableList<Float>,
                            radius = 100f,
                            color = viewModel.currentColor,
                            strokeWidth = viewModel.currentWidth
                        )

                        viewModel.addShape(circle)
                    }
                    2 -> {
                    /* Line selected */
                        val line = Shape.Line(
                            start = mutableListOf(50f, 50f),
                            end = mutableListOf(300f, 300f),
                            color = viewModel.currentColor,
                            strokeWidth = viewModel.currentWidth
                        )
                        viewModel.addShape(line)
                    }

                    3 -> {
                    /* Polygon selected */
                        val polygon = Shape.Polygon(
                            points =
                            mutableListOf(
                                mutableListOf(200f, 200f),
                                mutableListOf(300f, 150f),
                                mutableListOf(400f, 250f),
                                mutableListOf(300f, 300f)
                            ),
                            color = viewModel.currentColor,
                            strokeWidth = viewModel.currentWidth
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
            .setTitle("Change Text")
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

    private fun saveCanvasAsPNG(): File {

        val bitmap = Bitmap.createBitmap(
            binding.drawingCanvas.width,
            binding.drawingCanvas.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        binding.drawingCanvas.draw(canvas)

        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "whiteboard.png"
        )

        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        return file
    }

    private fun addText() {
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

    private fun updateToolUI(selectedTool: ToolType) {
        val selectedColor = Color.parseColor("#FFDD00") // Highlight color
        val defaultColor = Color.TRANSPARENT         // Default color

        binding.ivPencil.apply {
            binding.drawingCanvas.eraserMode = false
            isSelected = selectedTool == ToolType.PEN
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
        }

        binding.ivErase.apply {
            binding.drawingCanvas.eraserMode = true
            isSelected = selectedTool == ToolType.ERASER
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
        }

        binding.ivAddShape.apply{
            binding.drawingCanvas.eraserMode = false
            isSelected = selectedTool == ToolType.SHAPE
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
        }

        binding.ivText.apply{
            binding.drawingCanvas.eraserMode = false
            isSelected = selectedTool == ToolType.TEXT
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
        }

        binding.tvPng.apply{
            binding.drawingCanvas.eraserMode = false
            isSelected = selectedTool == ToolType.SAVEPNG
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
        }

        binding.ivSave.apply{
            binding.drawingCanvas.eraserMode = false
            isSelected = selectedTool == ToolType.SAVE
            setBackgroundColor(if (isSelected) selectedColor else defaultColor)
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

        val jsonString = gson.toJson(data)

        showJsonDialog(jsonString)
    }

    fun showJsonDialog(jsonString: String) {
        // Create a TextView inside a ScrollView
        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = jsonString
            setPadding(16, 16, 16, 16)
            setTextIsSelectable(true) // Allow copy
        }
        scrollView.addView(textView)

        // Build and show dialog
        AlertDialog.Builder(this)
            .setTitle("JSON Data")
            .setView(scrollView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}