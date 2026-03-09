package com.example.whiteboardapp.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem
import com.example.whiteboardapp.models.ToolType
import com.example.whiteboardapp.viewmodels.WhiteboardViewModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

class DrawingCanvas(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    var strokes: MutableList<Stroke> = mutableListOf()
    var shapes: MutableList<Shape> = mutableListOf()
    var texts: MutableList<TextItem> = mutableListOf()

    // For drag functionality
    var currentShape: Shape? = null
    var lastTouchX = 0f
    var lastTouchY = 0f
    var isResizing = false
    var resizeShape: Shape? = null
    val handleRadius = 30f

    private var selectedVertexIndex = -1
    var textClickListener: ((TextItem) -> Unit)? = null

    private var currentText: TextItem? = null

    private var lastTapTime = 0L
    private val doubleTapDelay = 300
    var eraserMode = false

    private var eraserX = 0f
    private var eraserY = 0f
    private val eraserRadius = 50f

    lateinit var viewModel: WhiteboardViewModel


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw all strokes
        for (stroke in strokes) {
            if (stroke.tool == ToolType.ERASER) {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                paint.xfermode = null
                paint.color = stroke.color
            }
            paint.strokeWidth = stroke.width
            paint.style = Paint.Style.STROKE

            val path = Path()
            stroke.points.firstOrNull()?.let { first ->
                path.moveTo(first[0], first[1])
            }
            for (i in 1 until stroke.points.size) {
                val p = stroke.points[i]
                path.lineTo(p[0], p[1])
            }
            canvas.drawPath(path, paint)
        }

        // 2. Draw shapes
        shapes.forEach { shape ->
            paint.style = Paint.Style.STROKE
            paint.color = when (shape) {
                is Shape.Rectangle -> shape.color
                is Shape.Circle -> shape.color
                is Shape.Line -> shape.color
                is Shape.Polygon -> shape.color
            }
            paint.strokeWidth = when (shape) {
                is Shape.Rectangle -> shape.strokeWidth
                is Shape.Circle -> shape.strokeWidth
                is Shape.Line -> shape.strokeWidth
                is Shape.Polygon -> shape.strokeWidth
            }
            when (shape) {
                is Shape.Rectangle -> canvas.drawRect(
                    shape.topLeft[0], shape.topLeft[1],
                    shape.bottomRight[0], shape.bottomRight[1], paint
                )
                is Shape.Circle -> canvas.drawCircle(shape.center[0], shape.center[1], shape.radius, paint)
                is Shape.Line -> canvas.drawLine(shape.start[0], shape.start[1], shape.end[0], shape.end[1], paint)
                is Shape.Polygon -> {
                    val path = Path()
                    shape.points.forEachIndexed { index, point ->
                        if (index == 0) path.moveTo(point[0], point[1])
                        else path.lineTo(point[0], point[1])
                    }
                    if (shape.points.size > 2) path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }

        // 3. Draw texts
        texts.forEach { text ->
            paint.style = Paint.Style.FILL
            paint.color = text.color
            paint.textSize = text.size
            canvas.drawText(text.text, text.position.first, text.position.second, paint)
        }

        // 4 Draw eraser if active
        if (eraserMode) {
            val highlightPaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.GRAY
                strokeWidth = 2f
                alpha = 150
            }
            val fillPaint = Paint().apply {
                style = Paint.Style.FILL
                color = Color.LTGRAY
                alpha = 50
            }
            canvas.drawCircle(eraserX, eraserY, eraserRadius, fillPaint)
            canvas.drawCircle(eraserX, eraserY, eraserRadius, highlightPaint)
        }
    }
    // -------------------------------
    // Touch events for dragging shapes
    // -------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (eraserMode) {

            eraserX = event.x
            eraserY = event.y

            eraseAt(event.x, event.y, eraserRadius)
            invalidate()
            return true
        }
        val toolType = event.getToolType(0)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event,toolType)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP -> handleActionUp()
        }
        return true
    }
    // Find the topmost shape under touch
    fun findShapeAt(x: Float, y: Float): Shape? {
        return shapes.reversed().find { shape ->
            when (shape) {
                is Shape.Rectangle ->
                    x >= shape.topLeft[0] && x <= shape.bottomRight[0] &&
                            y >= shape.topLeft[1] && y <= shape.bottomRight[1]

                is Shape.Circle -> {
                    val dx = x - shape.center[0]
                    val dy = y - shape.center[1]
                    dx * dx + dy * dy <= shape.radius * shape.radius
                    }
                is Shape.Line -> {

                    // Approximate touch near line
                    val distance = distanceToLine(shape.start, shape.end, listOf(x, y))
                    distance < 20
                }
                is Shape.Polygon ->
                    pointInPolygon(listOf(x, y), shape.points)

            }
        }
    }

    private fun distanceToLine(
        start: List<Float>,
        end: List<Float>,
        point: List<Float>
    ): Float {

        val x0 = point[0]
        val y0 = point[1]

        val x1 = start[0]
        val y1 = start[1]

        val x2 = end[0]
        val y2 = end[1]

        val numerator = kotlin.math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1)

        val denominator = kotlin.math.hypot(
            (y2 - y1).toDouble(),
            (x2 - x1).toDouble()
        )

        return (numerator / denominator).toFloat()
    }

    private fun pointInPolygon(point: List<Float>, polygon: List<List<Float>>): Boolean {

        val x = point[0]
        val y = point[1]

        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {

            val xi = polygon[i][0]
            val yi = polygon[i][1]

            val xj = polygon[j][0]
            val yj = polygon[j][1]

            val intersect =
                ((yi > y) != (yj > y)) &&
                        (x < (xj - xi) * (y - yi) / (yj - yi + 0.000001f) + xi)

            if (intersect) inside = !inside

            j = i
        }

        return inside
    }
    private fun handleActionDown(event: MotionEvent,toolType : Int) {

        lastTouchX = event.x
        lastTouchY = event.y
        var width = viewModel.currentWidth

        if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            Log.d("INPUT", "Stylus DOWN")
            val pressure = event.pressure
            width = 3f + pressure * 15f
        }

        if (viewModel.currentTool == ToolType.PEN) {
            viewModel.startStroke(event.x, event.y,width)
            }

        // detect which shape is touched
        currentShape = findShapeAt(event.x, event.y)

        // check resize handle
        resizeShape = currentShape?.takeIf { shape ->

            when (shape) {

                is Shape.Rectangle -> {
                    val handle = shape.getResizeHandle()
                    val dx = handle[0] - event.x
                    val dy = handle[1] - event.y
                    dx * dx + dy * dy <= handleRadius * handleRadius
                }

                is Shape.Circle -> {

                    val dx = event.x - shape.center[0]
                    val dy = event.y - shape.center[1]
                    val distance = hypot(dx, dy)
                    kotlin.math.abs(distance - shape.radius) <= handleRadius
                }

                is Shape.Line -> {
                    val dx = shape.end[0] - event.x
                    val dy = shape.end[1] - event.y
                    dx * dx + dy * dy <= handleRadius * handleRadius
                }

                is Shape.Polygon -> {

                    val vertexIndex = findPolygonVertex(shape, event.x, event.y)

                    if (vertexIndex != -1) {

                        selectedVertexIndex = vertexIndex
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        isResizing = resizeShape != null

        val clickedText = findTextAt(event.x, event.y)

        if (clickedText != null) {

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastTapTime < doubleTapDelay) {
                // double tap detected
                textClickListener?.invoke(clickedText)
            }

            lastTapTime = currentTime

            currentText = clickedText

            return
        }
    }
    private fun findPolygonVertex(shape: Shape.Polygon, x: Float, y: Float): Int {

        shape.points.forEachIndexed { index, point ->

            val dx = point[0] - x
            val dy = point[1] - y

            if (dx * dx + dy * dy <= handleRadius * handleRadius) {
                return index
            }
        }

        return -1
    }
    private fun handleActionMove(event: MotionEvent) {

        val dx = event.x - lastTouchX
        val dy = event.y - lastTouchY

        if (viewModel.currentTool == ToolType.PEN && !isResizing && currentShape == null && currentText == null) {
            viewModel.continueStroke(event.x, event.y)
        }

        currentText?.let { text ->

            val dx = event.x - lastTouchX
            val dy = event.y - lastTouchY

            text.position = Pair(
                text.position.first + dx,
                text.position.second + dy
            )

            lastTouchX = event.x
            lastTouchY = event.y

            invalidate()
            return
        }

        if (isResizing && resizeShape != null) {

            when (val shape = resizeShape!!) {

                is Shape.Rectangle -> {
                    shape.bottomRight[0] = event.x
                    shape.bottomRight[1] = event.y
                }

                is Shape.Circle -> {
                    val dx = event.x - shape.center[0]
                    val dy = event.y - shape.center[1]
                    val newRadius = hypot(dx, dy)
                    shape.radius = newRadius
                }

                is Shape.Line -> {
                    shape.end[0] = event.x
                    shape.end[1] = event.y
                }
                is Shape.Polygon -> {

                    if (selectedVertexIndex != -1) {

                        val vertex = shape.points[selectedVertexIndex]

                        vertex[0] = event.x
                        vertex[1] = event.y
                    }
                }

                else -> {}
            }

        } else if (currentShape != null) {

            when (val shape = currentShape!!) {

                is Shape.Rectangle -> {
                    shape.topLeft[0] += dx
                    shape.topLeft[1] += dy
                    shape.bottomRight[0] += dx
                    shape.bottomRight[1] += dy
                }

                is Shape.Circle -> {
                    shape.center[0] += dx
                    shape.center[1] += dy
                }

                is Shape.Line -> {
                    shape.start[0] += dx
                    shape.start[1] += dy
                    shape.end[0] += dx
                    shape.end[1] += dy
                }

                is Shape.Polygon -> {
                    shape.points.forEach {

                        it[0] += dx
                        it[1] += dy
                    }
                }
                else -> {}
            }
        }

        lastTouchX = event.x
        lastTouchY = event.y

        invalidate()
    }
    private fun handleActionUp() {
        isResizing = false
        resizeShape = null
        currentShape = null
        selectedVertexIndex = -1
        currentText = null
    }


    fun findTextAt(x: Float, y: Float): TextItem? {

        val textPaint = Paint()
            texts.forEach { text ->

                textPaint.textSize = text.size

                val bounds = Rect()
                textPaint.getTextBounds(text.text, 0, text.text.length, bounds)

                val left = text.position.first
                val top = text.position.second - bounds.height()
                val right = left + bounds.width()
                val bottom = text.position.second

                if (x >= left && x <= right && y >= top && y <= bottom) {
                    return text
                }
            }

        return null
    }


    // Helper for line distance
    fun distanceToLineSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return Math.hypot((px - x1).toDouble(), (py - y1).toDouble()).toFloat()
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0f, 1f)
        val nearestX = x1 + clampedT * dx
        val nearestY = y1 + clampedT * dy
        return Math.hypot((px - nearestX).toDouble(), (py - nearestY).toDouble()).toFloat()
    }

    // Helper for polygon
    private fun pointInPolygon(x: Float, y: Float, polygon: List<List<Float>>): Boolean {
        var intersects = 0
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            if (((polygon[i][1] > y) != (polygon[j][1] > y)) &&
                (x < (polygon[j][0] - polygon[i][0]) * (y - polygon[i][1]) / (polygon[j][1] - polygon[i][1]) + polygon[i][0])
            ) {
                intersects++
            }
        }
        return intersects % 2 == 1
    }

    fun findTopShapeAt(x: Float, y: Float, eraserRadius: Float): Shape? {
        return shapes.reversed().find { shape ->
            when (shape) {
                is Shape.Rectangle -> x in shape.topLeft[0]..shape.bottomRight[0] &&
                        y in shape.topLeft[1]..shape.bottomRight[1]
                is Shape.Circle -> {
                    val dx = x - shape.center[0]
                    val dy = y - shape.center[1]
                    dx * dx + dy * dy <= shape.radius * shape.radius
                }
                is Shape.Line -> {
                    val distance = distanceToLineSegment(x, y, shape.start[0], shape.start[1], shape.end[0], shape.end[1])
                    distance <= shape.strokeWidth / 2 + eraserRadius
                }
                is Shape.Polygon -> pointInPolygon(x, y, shape.points)
                else -> false
            }
        }
    }

    fun eraseAt(x: Float, y: Float, eraserRadius: Float) {

        // Remove stroke points inside eraser
        val updatedStrokes = strokes.map { stroke ->
            val newPoints = stroke.points.filter { point ->
                val dx = point.get(0) - x
                val dy = point.get(1) - y
                dx * dx + dy * dy >= eraserRadius * eraserRadius
            }
            // Also update ViewModel
            viewModel.addStroke(stroke)

            stroke.copy(points = newPoints.toMutableList())
        }.filter { it.points.isNotEmpty() }
        strokes = updatedStrokes.toMutableList()

        // Remove topmost shape only
        val shapeToRemove = findTopShapeAt(x, y, eraserRadius)
        shapeToRemove?.let {
            shapes = shapes.toMutableList().apply { remove(it) }
            // Update ViewModel
            viewModel.shapes.value = shapes
        }

        // Remove text inside eraser
        val updatedTexts = texts.filterNot { text ->
            val dx = text.position.first - x
            val dy = text.position.second - y
            dx * dx + dy * dy < eraserRadius * eraserRadius
        }
        texts = updatedTexts.toMutableList()
        viewModel._texts.value = texts

        invalidate()
    }
}