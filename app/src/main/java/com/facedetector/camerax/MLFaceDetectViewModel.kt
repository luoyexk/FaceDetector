package com.facedetector.camerax

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class MLFaceDetectViewModel : ViewModel() {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .enableTracking()
            .build()
    )
    private val _facePath = MutableLiveData<String>()

    val facePath: LiveData<String> get() = _facePath

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun receiveImage(image: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val mediaImage = image.image
            if (mediaImage != null) {
                val img = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                detect(img)
                    .catch { ex ->
                        Timber.e("detect face error ${ex.stackTraceToString()}")
                    }
                    .collect {
                        Timber.d("face count ${it.size}")
                    }
                image.close()
            }
        }
    }

    private fun saveBitmap(bitmapInternal: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            val tmp = File.createTempFile("", "")
            val os = BufferedOutputStream(FileOutputStream(tmp))
            bitmapInternal?.compress(Bitmap.CompressFormat.JPEG, 20, os)
            _facePath.postValue(tmp.absolutePath)
        }
    }

    @ExperimentalCoroutinesApi
    private fun detect(image: InputImage): Flow<MutableList<Face>> {
        return callbackFlow {
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (isActive) {
                        trySend(faces)
                    }
                    close()
                }
                .addOnFailureListener { e ->
                    close(e)
                }
            awaitClose()
        }
    }
}