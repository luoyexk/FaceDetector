package com.facedetector

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.facedetector.adapter.DemosAdapter
import com.facedetector.camerax.CameraXActivity
import com.facedetector.databinding.ActivityDemoListBinding

class DemoListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoListBinding
    private val dataset: Array<DemosAdapter.Demo> = arrayOf(
        DemosAdapter.Demo("Camera2", "", MainActivity::class.java),
        DemosAdapter.Demo("CameraX", "", CameraXActivity::class.java),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_demo_list)
        initViews()
    }

    private fun initViews() {
        binding.apply {
            recyclerView.apply {
                adapter = DemosAdapter(dataset)
            }
        }
    }

    fun start(activity: Class<*>, layoutFileId: Int) {
        val intent = Intent(this, activity).apply {
            putExtra("layout_file_id", layoutFileId)
        }
        startActivity(intent)
    }

}