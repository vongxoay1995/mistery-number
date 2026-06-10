package com.example.sortorder

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class PolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_policy)
        FullScreenHelper.apply(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }
}
