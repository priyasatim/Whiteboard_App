package com.example.whiteboardapp.services

import android.content.Context
import com.example.whiteboardapp.models.Shape
import com.example.whiteboardapp.models.Stroke
import com.example.whiteboardapp.models.TextItem
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileService(private val context: Context) {

    private val gson = Gson()

    fun saveCanvas(strokes: StateFlow<List<Stroke>>, shapes: MutableStateFlow<List<Shape>>, texts: MutableStateFlow<List<TextItem>>) {
        val data = mapOf(
            "strokes" to strokes,
            "shapes" to shapes,
            "texts" to texts
        )
        val json = gson.toJson(data)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.filesDir, "whiteboard_$timestamp.json")
        file.writeText(json)
    }

    fun loadWhiteboard(file: File): Triple<List<Stroke>, List<Shape>, List<TextItem>> {
        val json = file.readText()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = gson.fromJson(json, type)
        val strokes = gson.fromJson(gson.toJson(data["strokes"]), Array<Stroke>::class.java).toList()
        val shapes = gson.fromJson(gson.toJson(data["shapes"]), Array<Shape>::class.java).toList()
        val texts = gson.fromJson(gson.toJson(data["texts"]), Array<TextItem>::class.java).toList()
        return Triple(strokes, shapes, texts)
    }
}