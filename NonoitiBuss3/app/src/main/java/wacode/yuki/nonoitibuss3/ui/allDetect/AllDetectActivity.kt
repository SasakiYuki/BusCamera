package wacode.yuki.nonoitibuss3.ui.allDetect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import io.realm.RealmResults
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.detection.DetectionListener
import wacode.yuki.nonoitibuss3.detection.DetectionTask
import wacode.yuki.nonoitibuss3.entity.FaceResult
import wacode.yuki.nonoitibuss3.entity.StrImageRealm
import wacode.yuki.nonoitibuss3.ui.camera.CameraActivity
import wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.detectList.DetectListView
import wacode.yuki.nonoitibuss3.utils.RealmUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class AllDetectActivity : AppCompatActivity(),DetectionListener {
    private var mList:ArrayList<FaceResult>? = null
    private var mCount:Int = 0
    private var mRealmList:RealmResults<StrImageRealm>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_detect)
        mList = ArrayList()
        mRealmList = RealmUtils.getAll(this,CameraActivity.REALM_NAME)
        startDetection()
    }

    private fun startDetection(){
        if(mCount < mRealmList!!.size) {
            val bytes = Base64.decode(mRealmList!![mCount].strBase64, Base64.NO_WRAP);
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size);

            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            val input = ByteArrayInputStream(output.toByteArray())

            DetectionTask(this,this,bitmap).execute(input)
        }else{
            val listView = findViewById(R.id.listView) as DetectListView
            listView.setListView(mList!!)
        }
    }

    override fun detectionFinished(arrayFaceResult: ArrayList<FaceResult>) {
        for(item in arrayFaceResult){
            mList!!.add(item)
        }
        mCount++
        startDetection()
    }

    override fun detectionFailed() {
        mCount++
        startDetection()
    }


}
