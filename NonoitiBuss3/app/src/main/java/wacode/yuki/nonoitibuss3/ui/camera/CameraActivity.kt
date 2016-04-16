package wacode.yuki.nonoitibuss3.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import butterknife.bindView
import wacode.yuki.nonoitibuss2.Utils.BackgroundThreadHelper
import wacode.yuki.nonoitibuss2.Utils.CompareSizesByArea
import wacode.yuki.nonoitibuss2.View.Camera.CameraListener
import wacode.yuki.nonoitibuss2.View.Main.FitTextureView

import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.utils.RealmUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity(),CameraListener {
    companion object{
        private val TAG = "CameraActivity"
        val REALM_NAME = "ImageData.realm"

        private val REQUEST_CODE_CAMERA_PERMISSION =0x001
    }

    private var m_previewSize:Size? = null

    private var m_imageReader:ImageReader? = null
    private var m_thread:BackgroundThreadHelper = BackgroundThreadHelper()
    private var m_camera: BaseCamera = BaseCamera()

    private val fitTextureView:FitTextureView by bindView(R.id.texture)
    private val button:Button by bindView(R.id.button)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        cameraHandler()

        m_camera.setInterface(this)
        button.setOnClickListener { cameraHandler() }
    }

    private fun getCurrentTime():Int{
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        Log.d("sarumaru",(hour*100+minute).toString())
        return (hour*100+minute)
    }

    private fun cameraHandler(){
        val handler = Handler()
        handler.postDelayed(Runnable {
            m_camera.takePicture()
            Log.d("sarusaru",getCurrentTime().toString())
//            when(getCurrentTime()){
//                in 845..855 -> cameraHandler()
//                in 905..915 -> cameraHandler()
//                in 955..1005 -> cameraHandler()
//                in 1015..1023 ->cameraHandler()
//                in 1118.. 1123 -> cameraHandler()
//                in 1240.. 1248 -> cameraHandler()
//                in 1250.. 1258 -> cameraHandler()
//                in 1400.. 1408 -> cameraHandler()
//                in 1515.. 1523 -> cameraHandler()
//                in 1700.. 1708 -> cameraHandler()
//                in 1723.. 1728 -> cameraHandler()
//                in 1815.. 1823 -> cameraHandler()
//                in 1920.. 1928 -> cameraHandler()
//            }
            handler.postDelayed(Runnable {
                m_camera.takePicture()
                Log.d("sarusaru",getCurrentTime().toString())
            },2000)
        },2000)
    }


    override fun onResume() {
        super.onResume()
        m_thread.start()

        if(fitTextureView.isAvailable){
            openCamera(fitTextureView.width,fitTextureView.height)
        }else{
            fitTextureView.surfaceTextureListener = surfaceTextureListener_camera
        }
    }

    override fun onPause() {
        closeCamera()
        m_thread.stop()
        super.onPause()
    }

    // Camera
    private fun openCamera(width:Int,height:Int){
        //Permission Check if user don't allow permission, this Activity finish
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestCameraPermission()
            return
        }

        val cameraId = setUpCamera(width,height)
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try{
            if(m_camera.isLocked){
                throw RuntimeException("Wait to lock camera!!")
            }
            manager.openCamera(cameraId,m_camera.stateCallback,m_thread.getHandler())
        }catch(e:CameraAccessException){
            Log.d(TAG,e.message)
        }catch(e:InterruptedException){
            Log.d(TAG,e.message)
        }
    }

    private fun closeCamera(){
        m_camera.close()
        if(m_imageReader != null){
            m_imageReader!!.close()
            m_imageReader = null
        }
    }

    private fun setUpCamera(width:Int,height:Int):String{
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try{
            for(cameraId in cameraManager.cameraIdList){
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if(facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT){
                    continue
                }

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),CompareSizesByArea())

                setUpPreview(map.getOutputSizes(SurfaceTexture::class.java),width,height,largest)
                configurePreviewTransform(width,height)

                m_imageReader = ImageReader.newInstance(largest.width,largest.height,ImageFormat.JPEG,2)
                m_imageReader!!.setOnImageAvailableListener(
                        { reader ->
                            val image = reader.acquireLatestImage()
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            image.close()
                            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                            RealmUtils.put(this,REALM_NAME,base64)
                        }, m_thread.getHandler())
                return  cameraId
            }
        }catch(e:CameraAccessException){
            Log.d(TAG,e.message)
        }catch(e:NullPointerException){
            Log.d(TAG,e.message)
        }

        return ""
    }

    private fun setUpPreview(choices:Array<Size>,width:Int,height:Int,aspectRatio:Size){
        var bigSize:ArrayList<Size> = ArrayList()
        for(size in choices){
            if(size.height == size.width * aspectRatio.height / aspectRatio.width && size.width >= width && size.height >= height){
                bigSize.add(size)
            }
        }

        if(bigSize.size > 0){
            m_previewSize = Collections.min(bigSize,CompareSizesByArea())
        }else{
            m_previewSize = choices[0]
        }

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            fitTextureView.setAspectRatio(
                    previewSize!!.width, previewSize!!.height)
        } else {
            fitTextureView.setAspectRatio(
                    previewSize!!.height, previewSize!!.width)
        }
    }

    private fun configurePreviewTransform(viewWidth: Int, viewHeight: Int) {
        if (null == fitTextureView || null == m_previewSize) {
            return
        }
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, m_previewSize!!.height.toFloat(), m_previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    viewHeight.toFloat() / m_previewSize!!.height,
                    viewWidth.toFloat() / m_previewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        fitTextureView.setTransform(matrix)
    }

    //Get Camera Permission
    private fun requestCameraPermission(){
        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            AlertDialog.Builder(this)
            .setMessage("Request Camera Permission")
            .setPositiveButton(android.R.string.ok,{dialog,which ->
                requestPermissions(arrayOf(Manifest.permission.CAMERA),REQUEST_CODE_CAMERA_PERMISSION)
            })
            .setNegativeButton(android.R.string.cancel,{ dialog,which ->
                finish()
            })
            .create()
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_CODE_CAMERA_PERMISSION){
            if(grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                // Re request Camera Permission
                AlertDialog.Builder(this)
                .setMessage("Need Camera Permission for Use Camera")
                .setPositiveButton(android.R.string.ok,{ dialog,i ->
                    finish()
                })
                .create()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // CameraListener Overrides
    override val surfaceTextureFromTextureView: SurfaceTexture
        get() = fitTextureView.surfaceTexture
    override val previewSize: Size?
        get() = m_previewSize
    override val backgroundHandler: Handler?
        get() = m_thread.getHandler()
    override val imageRenderSurface: Surface
        get() = m_imageReader!!.surface
    override val rotation: Int
        get() = windowManager.defaultDisplay.rotation

    //Texture's Listener
    private val surfaceTextureListener_camera = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
            openCamera(p1,p2)
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
            configurePreviewTransform(p1,p2)
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
        }
    }

}

