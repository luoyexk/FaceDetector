package com.facedetector.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.facedetector.R

class CameraXActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_xactivity)
        initViews()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, FaceDetectMLFragment())
            }
        }
    }

    private fun initViews() {
        findViewById<RecyclerView>(R.id.recycler_view)
    }
}