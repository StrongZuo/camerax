package com.zuoz.camera

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent.ACTION_DOWN
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class CameraActivity : AppCompatActivity() {

    companion object {
        private val TAG: String = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

    var CURRENT_FLASH = 0

    private lateinit var imageCapture: ImageCapture
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mCameraSelector: CameraSelector
    private lateinit var mCameraControl: CameraControl

    private var executor: Executor? = null
    private lateinit var mCameraInfo: CameraInfo

    private var flashMode by Delegates.observable(ImageCapture.FLASH_MODE_OFF) { _, _, new ->
        buttonFlash.setImageResource(
            when (new) {
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
                else -> R.drawable.ic_flash_off
            }
        )
    }

    /**
     * 摄像头（前/后）
     */
    private var mCameraSelectorInt = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        btn_take_photo.setOnClickListener { takePhoto() }
        buttonFlash.setOnClickListener { closeFlashAndSelect() }
        initCamera()
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
        executor = Executors.newSingleThreadExecutor()
    }

    fun initCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                var camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                mCameraInfo = camera.cameraInfo
                mCameraControl = camera.cameraControl

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
        initCameraSelector()

        // 旋转监听
        val orientationEventListener: OrientationEventListener =
            object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    val rotation: Int

                    // Monitors orientation values to determine the target rotation value
                    rotation = if (orientation >= 45 && orientation < 135) {
                        Surface.ROTATION_270
                    } else if (orientation >= 135 && orientation < 225) {
                        Surface.ROTATION_180
                    } else if (orientation >= 225 && orientation < 315) {
                        Surface.ROTATION_90
                    } else {
                        Surface.ROTATION_0
                    }
                    imageCapture.targetRotation = rotation
                }
            }

        orientationEventListener.enable()
        viewFinder.setOnTouchListener { v, e ->
            when (e.action) {
                ACTION_DOWN -> {
                    cameraFocus(e.x, e.y)

                }
                else-> true
            }

        }
    }

    /**
     * 选择摄像头
     */
    private fun initCameraSelector() {
        mCameraSelector = CameraSelector.Builder()
            .requireLensFacing(mCameraSelectorInt)
            .build()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }


    private fun closeFlashAndSelect() {
        CURRENT_FLASH++
        flashMode = CURRENT_FLASH % 3
        imageCapture.flashMode =flashMode
    }

    private fun cameraFocus(x: Float, y: Float): Boolean {
        val factory = viewFinder.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            // auto calling cancelFocusAndMetering in 3 seconds
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        img_focus.startFocus(Point(x.toInt(), y.toInt()))
        val future: ListenableFuture<*> = mCameraControl.startFocusAndMetering(action)
        future.addListener(Runnable {
            try {
                val result = future.get() as FocusMeteringResult
                if (result.isFocusSuccessful) {
                    img_focus.onFocusSuccess()
                } else {
                    img_focus.onFocusFailed()
                }
            } catch (e: Exception) {

            }
        }, executor)

        return true
    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}