package wacode.yuki.nonoitibuss3.ui.camera

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface

import java.util.Arrays
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

import wacode.yuki.nonoitibuss2.View.Camera.CameraListener

class BaseCamera {
    companion object {

        private val TAG = "BasicCamera"

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private val STATE_PREVIEW = 0x00
        private val STATE_WAITING_LOCK = 0x01
        private val STATE_WAITING_PRECAPTURE = 0x02
        private val STATE_WAITING_NON_PRECAPTURE = 0x03
        private val STATE_PICTURE_TAKEN = 0x04
    }
    private var mState = STATE_PREVIEW

    private val mCameraOpenCloseLock = Semaphore(1)
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mPreviewRequest: CaptureRequest? = null
    private var mCaptureSession: CameraCaptureSession? = null

    private var mInterface: CameraListener? = null

    fun setInterface(param: CameraListener) {
        mInterface = param
    }

    val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            Log.d(TAG, "Camera StateCallback onError: Please Reboot Android OS")
        }

    }

    val isLocked: Boolean
        @Throws(InterruptedException::class)
        get() = mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)

    fun close() {
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = mInterface!!.surfaceTextureFromTextureView!!

            val preview = mInterface!!.previewSize
            texture.setDefaultBufferSize(preview!!.width, preview.height)

            val surface = Surface(texture)
            mPreviewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)

            val imageRenderSurface = mInterface!!.imageRenderSurface
            mCameraDevice!!.createCaptureSession(Arrays.asList(surface, imageRenderSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return
                            }

                            mCaptureSession = cameraCaptureSession
                            try {
                                mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

                                mPreviewRequest = mPreviewRequestBuilder!!.build()
                                mCaptureSession!!.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mInterface!!.backgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession onConfigureFailed")
                        }
                    }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }


        private fun process(result: CaptureResult) {
            when (mState) {
                STATE_PREVIEW -> {
                }
                STATE_WAITING_LOCK -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == null) {
                        captureStillPicture()
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState === CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        } else {
                            runPrecaptureSequence()
                        }
                    }
                }
                STATE_WAITING_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                            aeState === CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState === CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState !== CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

    }

    private fun captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return
            }
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mInterface!!.imageRenderSurface)

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            val rotation = mInterface!!.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation))

            val CaptureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult) {
                    Log.e(TAG, "onCaptureCompleted Picture Saved")
                    unlockFocus()
                }
            }

            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.capture(captureBuilder.build(), CaptureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                    mInterface!!.backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    fun takePicture() {
        if (mCaptureSession != null) {
            lockFocus()
        }
    }

    private fun lockFocus() {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            mState = STATE_WAITING_LOCK
            mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                    mInterface!!.backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun unlockFocus() {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                    mInterface!!.backgroundHandler)
            mState = STATE_PREVIEW
            mCaptureSession!!.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mInterface!!.backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

}
