package wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.entity.FaceResult
import wacode.yuki.nonoitibuss3.ui.camera.helper.ImageHelper
import wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.detectList.DetectListView
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

/**
 * Created by Yuki on 2016/04/12.
 */
class DetectDetailLayout(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    companion object{
        val TAG ="DetectDetailLayout.kt"
    }
    val imageView: ImageView by bindView(R.id.imageView)
    val textView: TextView by bindView(R.id.textView)
    private var progressDialog: ProgressDialog = ProgressDialog(context)
    private var strBase64:String = ""
    private var bitmap: Bitmap? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    fun bindViews(strBase64:String){
        this.strBase64 = strBase64
        val bytes = Base64.decode(strBase64, Base64.NO_WRAP);
        bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size);
        imageView.setImageBitmap(bitmap)

        val output = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.JPEG,100,output)
        val input = ByteArrayInputStream(output.toByteArray())

        DetectionTask().execute(input)
    }

    private fun Logger(message:String?){
        Log.d(TAG,message)
    }


    private inner class DetectionTask : AsyncTask<InputStream, String, Array<Face>>() {
        private var succeed = true

        override fun doInBackground(vararg params: InputStream): Array<Face>? {
            val faceServiceClient = FaceServiceRestClient(resources.getString(R.string.subscription_key))
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
            Logger("Request: Detecting in image " + strBase64)
        }

        override fun onProgressUpdate(vararg progress: String) {
            progressDialog.setMessage(progress[0])
        }

        override fun onPostExecute(result: Array<Face>?) {
            if (succeed) {
                Logger("Response: Success. Detected " + (if (result == null) 0 else result.size)
                        + " face(s) in " + strBase64)
            }

            // Show the result on screen when detection is done.
            setUiAfterDetection(result, succeed)
        }
    }

    private fun setUiAfterDetection(result:Array<Face>?, succeed:Boolean){
        progressDialog.dismiss()

        if(succeed){
            if(result != null){
                textView.text = result.size.toString() + "faces"

                //
                imageView.setImageBitmap(ImageHelper.drawFaceRectanglesOnBitmap(bitmap!!,result,true))

                var list: ArrayList<FaceResult> = ArrayList()
                for(item in result){
                    var faceResult: FaceResult = FaceResult()
                    faceResult.age = item.faceAttributes.age
                    faceResult.smile = item.faceAttributes.smile
                    faceResult.gender = item.faceAttributes.gender
                    faceResult.bitmap = ImageHelper.generateFaceThumbnail(bitmap!!,item.faceRectangle)
                    list.add(faceResult)
                }
                val listView = findViewById(R.id.ListView) as DetectListView
                listView.setListView(list)
            }
        }
    }
}