package com.example.luontopeli.viewmodel

// 📁 viewmodel/CameraViewModel.kt


import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.luontopeli.data.local.entity.NatureSpot
import com.example.luontopeli.data.repository.NatureSpotRepository
import com.example.luontopeli.ml.ClassificationResult
import com.example.luontopeli.ml.PlantClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*



class CameraViewModel(
    private val repository: NatureSpotRepository
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _capturedImagePath = MutableStateFlow<String?>(null)
    val capturedImagePath: StateFlow<String?> = _capturedImagePath.asStateFlow()

    var currentLatitude: Double? = null
    var currentLongitude: Double? = null
    private val classifier = PlantClassifier()


    private val _classificationResult = MutableStateFlow<ClassificationResult?>(null)
    val classificationResult: StateFlow<ClassificationResult?> = _classificationResult.asStateFlow()


    fun takePhotoAndClassify(context: Context, imageCapture: ImageCapture) {
        _isLoading.value = true
        viewModelScope.launch {

            val imagePath = takePhotoSuspend(context, imageCapture)
            if (imagePath == null) { _isLoading.value = false; return@launch }

            _capturedImagePath.value = imagePath as String?


            try {
                val uri = Uri.fromFile(File(imagePath))
                val result = classifier.classify(uri, context)
                _classificationResult.value = result
            } catch (e: Exception) {
                _classificationResult.value = ClassificationResult.Error(e.message ?: "unknow error")
            }

            _isLoading.value = false
        }
    }

    private suspend fun takePhotoSuspend(
        context: Context,
        imageCapture: ImageCapture
    ): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->

        val file = File(
            context.cacheDir,
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    continuation.resume(file.absolutePath, null)
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resume(null, null)
                }
            }
        )
    }

    fun saveCurrentSpot() {
        val imagePath = _capturedImagePath.value ?: return
        viewModelScope.launch {
            val result = _classificationResult.value

            val spot = NatureSpot(
                name = when (result) {
                    is ClassificationResult.Success -> result.label
                    else -> "Luontolöytö"
                },
                latitude = currentLatitude,
                longitude = currentLongitude,
                imageLocalPath = imagePath,
                plantLabel = (result as? ClassificationResult.Success)?.label,
                confidence = (result as? ClassificationResult.Success)?.confidence
            )
            repository.insertSpot(spot)
            clearCapturedImage()
            _classificationResult.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        classifier.close()
    }
    fun clearCapturedImage() {
        _capturedImagePath.value = null
    }
}

