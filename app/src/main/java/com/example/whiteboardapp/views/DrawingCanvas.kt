package com.example.whiteboardapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem

class DrawingCanvas(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var selectedPolygonPointIndex = -1

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
                    shape.topLeft[0],
                    shape.topLeft[1],
                    shape.bottomRight[0],
                    shape.bottomRight[1],
                    paint
                )
                is Shape.Circle -> {

                    canvas.drawCircle(
                        shape.center[0],
                        shape.center[1],
                        shape.radius,
                        paint
                    )
                }
                is Shape.Line -> canvas.drawLine(
                    shape.start[0],
                    shape.start[1],
                    shape.end[0],
                    shape.end[1],
                    paint
                )
                is Shape.Polygon -> {

                    if (shape.points.size >= 2) {

                        val path = Path()
                        path.reset()

                        shape.points.forEachIndexed { index, point ->
                            val x = point[0]
                            val y = point[1]

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // close shape if 3+ points
                        if (shape.points.size > 2) {
                            path.close()
                        }

                        canvas.drawPath(path, paint)
                    }
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
                            shape.topLeft = listOf(
                                shape.topLeft[0] + dx,
                                shape.topLeft[1] + dy
                            )

                            shape.bottomRight = listOf(
                                shape.bottomRight[0] + dx,
                                shape.bottomRight[1] + dy
                            )
                        }
                        is Shape.Circle -> {

                            val newX = shape.center[0] + dx
                            val newY = shape.center[1] + dy

                            shape.center = listOf(newX, newY) as MutableList<Float>
                        }
                        is Shape.Line -> {
                            shape.start = shape.start
                            shape.end = shape.end
                        }
                        is Shape.Polygon -> {

                            // Move all points by dx, dy
                            shape.points = shape.points.map { point ->
                                listOf(point[0] + dx, point[1] + dy)
                            }.toMutableList()
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

        var intersects = 0

        for (i in polygon.indices) {

            val j = (i + 1) % polygon.size

            if (
                ((polygon[i][1] > point[1]) != (polygon[j][1] > point[1])) &&
                (point[0] < (polygon[j][0] - polygon[i][0]) *
                        (point[1] - polygon[i][1]) /
                        (polygon[j][1] - polygon[i][1]) + polygon[i][0])
            ) {
                intersects++
            }
        }

        return intersects % 2 == 1
    }
}