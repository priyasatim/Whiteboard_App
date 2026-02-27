package com.example.whiteboardapp.ui

import android.os.Bundle
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
import com.example.whiteboardapp.viewmodels.WhiteboardViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: WhiteboardViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.strokes.collect {
                    binding.drawingCanvas.strokes = it; binding.drawingCanvas.invalidate()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shapes.collect {
                    binding.drawingCanvas.shapes = it; binding.drawingCanvas.invalidate()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.texts.collect {
                    binding.drawingCanvas.texts = it; binding.drawingCanvas.invalidate()
                }
            }
        }

        // Toolbar button example
        binding.btnColorRed.setOnClickListener {
            viewModel.startStroke(color = 0xFFFF0000.toInt(), width = 5f)
        }

    }
}