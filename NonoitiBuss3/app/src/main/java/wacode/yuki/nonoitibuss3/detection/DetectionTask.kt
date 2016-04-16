package wacode.yuki.nonoitibuss3.detection

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.entity.FaceResult
import wacode.yuki.nonoitibuss3.ui.camera.helper.ImageHelper
import java.io.InputStream
import java.util.*

/**
 * Created by Yuki on 2016/04/12.
 */
class DetectionTask(context: Context,listener: DetectionListener,bitmap: Bitmap) : AsyncTask<InputStream, String, Array<Face>>() {
    companion object{
        private val TAG = "DetectionTask"
    }
    private var progressDialog:ProgressDialog
    private var succeed = true
    private val context: Context
    private val listener:DetectionListener
    private val sourceBitmap:Bitmap

    init {
        this.context = context
        progressDialog = ProgressDialog(context)
        this.listener = listener
        this.sourceBitmap = bitmap
    }

    override fun doInBackground(vararg params: InputStream): Array<Face>? {
        val faceServiceClient = FaceServiceRestClient(context.resources.getString(R.string.subscription_key))
        try {
            publishProgress("Detecting...")

            // Start detection.
            return faceServiceClient.detect(
                    params[0],
                    true,
                    true,
                    arrayOf(FaceServiceClient.FaceAttributeType.Age, FaceServiceClient.FaceAttributeType.Gender, FaceServiceClient.FaceAttributeType.Glasses, FaceServiceClient.FaceAttributeType.Smile, FaceServiceClient.FaceAttributeType.HeadPose))
        } catch (e: Exception) {
            succeed = false
            publishProgress(e.message)
            Logger(e.message)
            return null
        }

    }

    override fun onPreExecute() {
        progressDialog.show()
    }

    override fun onProgressUpdate(vararg progress: String) {
        progressDialog.setMessage(progress[0])
    }

    override fun onPostExecute(result: Array<Face>?) {
        if (succeed) {
            Logger("Response: Success. Detected " + (if (result == null) 0 else result.size)
                    + " face(s) ")
        }
        // Show the result on screen when detection is done.
        setUiAfterDetection(result, succeed)
    }

    private fun Logger(message:String?){
        Log.d(TAG,message)
    }

    private fun setUiAfterDetection(result:Array<Face>?, succeed:Boolean){
        progressDialog.dismiss()

        if(succeed){
            if(result != null){

                var list: ArrayList<FaceResult> = ArrayList()
                for(item in result){
                    var faceResult: FaceResult = FaceResult()
                    faceResult.age = item.faceAttributes.age
                    faceResult.smile = item.faceAttributes.smile
                    faceResult.gender = item.faceAttributes.gender
                    faceResult.bitmap = ImageHelper.generateFaceThumbnail(sourceBitmap,item.faceRectangle)
                    list.add(faceResult)
                }

                listener.detectionFinished(list)
            }else{
                listener.detectionFailed()
            }
        }else{
            listener.detectionFailed()
        }
    }
}
