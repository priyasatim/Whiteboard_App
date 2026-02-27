package com.example.whiteboardapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem

class DrawingCanvas(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    var strokes: List<Stroke> = emptyList()
    var shapes: List<Shape> = emptyList()
    var texts: List<TextItem> = emptyList()

    var currentStroke: Stroke? = null

    // For drag functionality
    var currentShape: Shape? = null
    var lastTouchX = 0f
    var lastTouchY = 0f

    var isResizing = false
    var resizeShape: Shape? = null
    var resizeStartX = 0f
    var resizeStartY = 0f
    val handleRadius = 30f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw strokes
        strokes.forEach { stroke ->
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.first, point.second)
                else path.lineTo(point.first, point.second)
            }
            canvas.drawPath(path, paint)
        }

        // Draw current stroke (during touch)
        currentStroke?.let { stroke ->
            paint.color = stroke.color
            paint.strokeWidth = stroke.width
            val path = Path()
            stroke.points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.first, p.second)
                else path.lineTo(p.first, p.second)
            }
            canvas.drawPath(path, paint)
        }

        // Draw shapes
        shapes.forEach { shape ->
            paint.style = Paint.Style.STROKE
            paint.color = when (shape) {
                is Shape.Rectangle -> shape.color
                is Shape.Circle -> shape.color
                is Shape.Line -> shape.color
                is Shape.Polygon -> shape.color
            }
            paint.strokeWidth = 5f

            when (shape) {
                is Shape.Rectangle -> canvas.drawRect(
                    shape.topLeft.first,
                    shape.topLeft.second,
                    shape.bottomRight.first,
                    shape.bottomRight.second,
                    paint
                )
                is Shape.Circle -> canvas.drawCircle(
                    shape.center.first,
                    shape.center.second,
                    shape.radius,
                    paint
                )
                is Shape.Line -> canvas.drawLine(
                    shape.start.first,
                    shape.start.second,
                    shape.end.first,
                    shape.end.second,
                    paint
                )
                is Shape.Polygon -> {
                    val path = Path()
                    shape.points.forEachIndexed { i, p ->
                        if (i == 0) path.moveTo(p.first, p.second)
                        else path.lineTo(p.first, p.second)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }

        // Draw text
        texts.forEach { text ->
            paint.style = Paint.Style.FILL
            paint.color = text.color
            paint.textSize = text.size
            canvas.drawText(text.text, text.position.first, text.position.second, paint)
        }
    }

    // -------------------------------
    // Touch events for dragging shapes
    // -------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                // Check if touching existing shape
                currentShape = findShapeAt(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentShape?.let { shape ->
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

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

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                currentShape = null
            }
        }
        return true
    }

    // Find the topmost shape under touch
    fun findShapeAt(x: Float, y: Float): Shape? {
        return shapes.reversed().find { shape ->
            when (shape) {
                is Shape.Rectangle -> x >= shape.topLeft.first && x <= shape.bottomRight.first &&
                        y >= shape.topLeft.second && y <= shape.bottomRight.second
                is Shape.Circle -> {
                    val dx = x - shape.center.first
                    val dy = y - shape.center.second
                    dx * dx + dy * dy <= shape.radius * shape.radius
                }
                is Shape.Line -> {
                    // Approximate touch near line
                    val distance = distanceToLine(shape.start, shape.end, Pair(x, y))
                    distance < 20
                }
                is Shape.Polygon -> pointInPolygon(Pair(x, y), shape.points)
            }
        }
    }

    private fun distanceToLine(start: Pair<Float, Float>, end: Pair<Float, Float>, point: Pair<Float, Float>): Float {
        val x0 = point.first
        val y0 = point.second
        val x1 = start.first
        val y1 = start.second
        val x2 = end.first
        val y2 = end.second

        val numerator = Math.abs((y2 - y1)*x0 - (x2 - x1)*y0 + x2*y1 - y2*x1)
        val denominator = Math.hypot((y2 - y1).toDouble(), (x2 - x1).toDouble())
        return (numerator / denominator).toFloat()
    }

    private fun pointInPolygon(point: Pair<Float, Float>, polygon: List<Pair<Float, Float>>): Boolean {
        // Simple ray-casting algorithm
        var intersects = 0
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            if (((polygon[i].second > point.second) != (polygon[j].second > point.second)) &&
                (point.first < (polygon[j].first - polygon[i].first) * (point.second - polygon[i].second) / (polygon[j].second - polygon[i].second) + polygon[i].first)
            ) {
                intersects++
            }
        }
        return intersects % 2 == 1
    }
}