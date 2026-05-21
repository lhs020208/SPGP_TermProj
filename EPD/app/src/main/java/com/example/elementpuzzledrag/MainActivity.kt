package com.example.elementpuzzledrag

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.elementpuzzledrag.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedLevel = 1
    private lateinit var levelButtons: Map<Int, TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLevelButtons()
        selectLevel(1)
    }

    private fun setupLevelButtons() {
        levelButtons = mapOf(
            1 to binding.levelButton1,
            2 to binding.levelButton2,
            3 to binding.levelButton3,
            4 to binding.levelButton4,
            5 to binding.levelButton5,
            6 to binding.levelButton6,
            7 to binding.levelButton7,
            8 to binding.levelButton8,
            9 to binding.levelButton9,
            10 to binding.levelButton10,
        )

        for ((level, button) in levelButtons) {
            button.setOnClickListener {
                selectLevel(level)
            }
        }
    }

    private fun selectLevel(level: Int) {
        selectedLevel = level.coerceIn(1, 10)

        for ((buttonLevel, button) in levelButtons) {
            button.isActivated = buttonLevel == selectedLevel
        }
    }

    fun onStartGameClicked(view: View) {
        startGameActivity()
    }

    private fun startGameActivity() {
        Log.d(javaClass.simpleName, "Start Game Level=$selectedLevel")

        val intent = Intent(this, ElementPuzzleDrag::class.java).apply {
            putExtra(
                ElementPuzzleDrag.EXTRA_INITIAL_STAGE_INDEX,
                selectedLevel - 1,
            )
        }

        startActivity(intent)
    }
}