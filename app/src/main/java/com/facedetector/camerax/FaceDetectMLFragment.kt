package com.facedetector.camerax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import androidx.fragment.app.viewModels
import com.facedetector.R
import com.facedetector.base.CameraFragment
import com.facedetector.databinding.FragmentFaceDetectMlBinding

/**
 * Face detection by Google ML
 * see https://developers.google.com/ml-kit/vision/face-detection/android?authuser=0
 */
class FaceDetectMLFragment : CameraFragment() {

    private lateinit var binding: FragmentFaceDetectMlBinding
    private val mViewModel by viewModels<MLFaceDetectViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentFaceDetectMlBinding.inflate(inflater, container, false).let {
            it.lifecycleOwner = viewLifecycleOwner
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        previewView = binding.previewView
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnTransferFacing.setOnClickListener {
            toggleCameraFacing()
            (it as ImageView).setImageResource(
                if (isFacingFront())
                    R.drawable.ic_baseline_tag_faces_24
                else
                    R.drawable.ic_baseline_linked_camera_24
            )
        }
    }

    override fun initObserver() {
    }

    override fun analyze(image: ImageProxy) {
        mViewModel.receiveImage(image)
    }
}