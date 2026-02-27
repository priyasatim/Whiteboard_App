package com.example.whiteboardapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
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
}