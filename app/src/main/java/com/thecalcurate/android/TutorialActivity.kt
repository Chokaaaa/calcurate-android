package com.thecalcurate.android

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        val imvClose = findViewById<ImageView>(R.id.imvClose)
        imvClose.setOnClickListener {
            finish()
        }
    }
}