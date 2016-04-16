package wacode.yuki.nonoitibuss3.ui.selectDetect.selectImageList

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.ui.camera.CameraActivity
import wacode.yuki.nonoitibuss3.ui.selectDetect.SelectDetectActivity
import wacode.yuki.nonoitibuss3.utils.RealmUtils

/**
 * Created by Yuki on 2016/04/10.
 */
class ImageListLayout(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val dialogBuilder:AlertDialog.Builder

    init {
        dialogBuilder = AlertDialog.Builder(context)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    fun bindViews(strBase64:String){
        try {
            val imageView: ImageView = findViewById(R.id.imageView) as ImageView
            val decodedString = Base64.decode(strBase64, Base64.NO_WRAP)
            val decodedByte: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size);
            imageView.setImageBitmap(decodedByte);
            setOnClickListener({
                val alertDialog = setAlertDialog(strBase64)
                alertDialog.show()
            })
        }catch(e:OutOfMemoryError){
            Log.d("oom","oom")
        }
    }

    private fun setAlertDialog(strBase64: String):AlertDialog{
        dialogBuilder.setPositiveButton("Delete",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    val result = RealmUtils.query(context,CameraActivity.REALM_NAME,strBase64)
                    RealmUtils.delete(context,CameraActivity.REALM_NAME,result)
                    val activity = context as SelectDetectActivity
                    val container = activity.getContainer()
                    container!!.dispTop()
                });
        dialogBuilder.setNegativeButton("Detect",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    val activity = context as SelectDetectActivity
                    val container = activity.getContainer()
                    container!!.dispImageDetail(strBase64)
                })
        dialogBuilder.setCancelable(true)
        return dialogBuilder.create()
    }






}