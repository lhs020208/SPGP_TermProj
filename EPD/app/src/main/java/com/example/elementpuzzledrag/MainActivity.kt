package com.example.elementpuzzledrag

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.elementpuzzledrag.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onStartGameClicked(view: View) {
        startGameActivity()
    }

    private fun startGameActivity() {
        Log.d(javaClass.simpleName, "Start Game")
        val intent = Intent(this, ElementPuzzleDrag::class.java)
        startActivity(intent)
    }
}