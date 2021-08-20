package com.facedetector.base

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Size
import android.view.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import com.facedetector.R
import com.facedetector.extension.activityResultLauncher
import com.facedetector.extension.finish
import com.facedetector.extension.hasPermissions
import com.facedetector.extension.requestPermissionsLauncher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors


private val PERMISSIONS_TAKE_PICTURES = arrayOf(
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
)

abstract class BaseCameraXFragment : Fragment(), ImageAnalysis.Analyzer {

    private val requestPermissions = requestPermissionsLauncher { result ->
        val isAllPermissionsGranted = result.all { it.value == true }
        if (isAllPermissionsGranted) {
            onPermissionsGranted()
        } else {
            showPermissionNoteDialog()
        }
    }

    private fun showPermissionNoteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.need_permissions_to_take_pictures)
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setPositiveButton(android.R.string.ok) { _, _ -> navigateToAppSettingScreen() }
            .setCancelable(false)
            .show()
    }

    private val appSettingResult = activityResultLauncher {
        checkPermissions()
    }

    private fun navigateToAppSettingScreen() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireActivity().packageName, null)
        ).let { appSettingResult.launch(it) }
    }

    private val mMainExecutor by lazy { ContextCompat.getMainExecutor(requireContext()) }
    private val mAnalysisExecutor = Executors.newSingleThreadExecutor()
    private val mMetadata = ImageCapture.Metadata()
    private var mImageCapture: ImageCapture? = null
    private var mCameraInfo: CameraInfo? = null
    private var mCameraControl: CameraControl? = null
    private var mFocusFuture: ListenableFuture<FocusMeteringResult>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    protected val torchStatus = MediatorLiveData<Int>()

    protected lateinit var previewView: PreviewView

    var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    abstract fun onManualFocusStart(x: Float, y: Float)
    abstract fun onManualFocusEnd()

    private val orientationEventListener by lazy {
        object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                mImageCapture?.targetRotation = rotation
            }
        }
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        checkPermissions()
    }

    private fun checkPermissions() {
        if (hasPermissions(PERMISSIONS_TAKE_PICTURES)) {
            onPermissionsGranted()
        } else {
            requestPermissions()
        }
    }

    private fun onPermissionsGranted() {
        initListener()
        initCamera()
        initObserver()
    }

    private fun requestPermissions() {
        requestPermissions.launch(PERMISSIONS_TAKE_PICTURES)
    }

    // 创建一个名为 listener 的回调函数，当手势事件发生时会调用这个回调函数
    private val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // 获取当前的摄像头的缩放比例
            val currentZoomRatio: Float = mCameraInfo?.zoomState?.value?.zoomRatio ?: 1f

            // 获取用户捏拉手势所更改的缩放比例
            val delta = detector.scaleFactor

            // 更新摄像头的缩放比例
            mCameraControl?.setZoomRatio(currentZoomRatio * delta)
            return true
        }
    }

    // 将 PreviewView 的触摸监听器绑定到缩放手势监听器上
    private val scaleGestureDetector by lazy { ScaleGestureDetector(requireContext(), listener) }
    private val customGestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                tabToFocus(e.x, e.y)
                return true
            }
        })
    }

    private fun tabToFocus(x: Float, y: Float) {
        val cameraController = mCameraControl ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        // Note: 目前的CameraX版本调整AE无效，调整AF有效
        val action = FocusMeteringAction.Builder(point).build()

        if (mFocusFuture?.isDone == false) {
            mFocusFuture?.cancel(true)
            onManualFocusEnd()
        }
        val future = cameraController.startFocusAndMetering(action)
        future.addListener({
            if (future.isDone) {
                onManualFocusEnd()
            }
        }, mMainExecutor)
        mFocusFuture = future
        onManualFocusStart(x, y)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        // 将 PreviewView 的触摸事件传递给缩放手势监听器上
        previewView.setOnTouchListener { _, event ->
            customGestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startCamera()
        }, mMainExecutor)

    }

    abstract fun initObserver()

    protected open fun getPreviewResolution(): Size {
        return Size(3648, 2736)
    }

    protected open fun getCaptureResolution(): Size {
        return Size(3648, 2736)
    }

    fun toggleCameraFacing() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        startCamera()
    }

    fun isFacingFront(): Boolean {
        return lensFacing == CameraSelector.LENS_FACING_FRONT
    }

    private fun startCamera() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // Preview.
        val preview: Preview = Preview.Builder()
            .setTargetResolution(getPreviewResolution())
            .build()

        // Image Capture.
        mImageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(getCaptureResolution())
            .build()

        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(getPreviewResolution())
            .build()
        analysis.setAnalyzer(mAnalysisExecutor, this)

        // Select back camera.
        val cameraSelector: CameraSelector = getCameraSelector()

        // Bind use cases to camera.
        try {
            // Used to bind the lifecycle of cameras to the lifecycle owner.
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                mImageCapture,
                analysis,
            )
            mCameraInfo?.torchState?.removeObservers(viewLifecycleOwner)
            mCameraInfo = camera.cameraInfo
            mCameraControl = camera.cameraControl
            preview.setSurfaceProvider(previewView.surfaceProvider)
            listenTorchStatus(camera.cameraInfo)
            onCameraStarted()
            onCameraHasFlashUnit(camera.cameraInfo.hasFlashUnit())
        } catch (e: Exception) {
            Timber.e("Use case binding failed $e")
        }
    }

    private fun getCameraSelector(): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    abstract fun onPlayTakePhotoAnimation()
    abstract fun onPlaySound()
    protected open fun onCameraStarted() {}
    protected open fun onCameraHasFlashUnit(hasFlashUnit: Boolean) {}
    protected open fun onSuccess(tempFileUri: Uri) {}
    protected open fun onFailure(ex: ImageCaptureException) {}
    override fun analyze(image: ImageProxy) {}

    private fun listenTorchStatus(cameraInfo: CameraInfo) {
        cameraInfo.torchState.observe(viewLifecycleOwner) {
            torchStatus.postValue(it)
        }
    }

    fun takePhoto() {
        mImageCapture?.let {
            onPlaySound()
            onPlayTakePhotoAnimation()
            takePhotoInternal()
        }
    }

    private fun takePhotoInternal() {
        val tmp = File.createTempFile("", "")
        val options = ImageCapture.OutputFileOptions.Builder(tmp).setMetadata(mMetadata).build()
        val imageSavedCallback = ImageSavedCallback(tmp)
        mImageCapture?.takePicture(options, mMainExecutor, imageSavedCallback)
    }

    protected fun toggleTorch() {
        val control = mCameraControl ?: return
        val info = mCameraInfo ?: return
        if (info.hasFlashUnit()) {
            val state = info.torchState.value
            control.enableTorch(TorchState.OFF == state)
        }
    }

    protected fun enableTorch() {
        val control = mCameraControl ?: return
        val info = mCameraInfo ?: return
        if (info.hasFlashUnit()) {
            control.enableTorch(true)
        }
    }

    protected fun hasFlashUnit(): Boolean {
        return mCameraInfo?.hasFlashUnit() == true
    }

    inner class ImageSavedCallback(
        private val savedFile: File,
    ) : ImageCapture.OnImageSavedCallback {

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = savedFile.toUri()
            onSuccess(savedUri)
        }

        override fun onError(exception: ImageCaptureException) {
            onFailure(exception)
        }
    }

}