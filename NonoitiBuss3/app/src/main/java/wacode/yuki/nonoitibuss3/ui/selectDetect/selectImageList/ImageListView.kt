package wacode.yuki.nonoitibuss3.ui.selectDetect.selectImageList

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.widget.ListView
import io.realm.RealmResults
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.entity.SelectListEntity
import wacode.yuki.nonoitibuss3.entity.StrImageRealm
import wacode.yuki.nonoitibuss3.ui.camera.CameraActivity
import wacode.yuki.nonoitibuss3.utils.RealmUtils
import java.util.*

/**
 * Created by Yuki on 2016/04/10.
 */
class ImageListView(context: Context?, attrs: AttributeSet?) : ListView(context, attrs) {

    override fun onFinishInflate() {
        super.onFinishInflate()

        val adapter = ImageListAdapter(context, R.layout.listview_image, parseRealmObject(RealmUtils.getAll(context, CameraActivity.REALM_NAME)))
        setAdapter(adapter)
    }

    fun setListView(){
        val adapter = ImageListAdapter(context,R.layout.listview_image,parseRealmObject(RealmUtils.getAll(context,CameraActivity.REALM_NAME)))
        setAdapter(adapter)
    }

    private fun parseRealmObject(list: RealmResults<StrImageRealm>): ArrayList<String> {
        val stringList: ArrayList<String> = ArrayList()
        for(item in list){
            stringList.add(item.strBase64)
        }
        return stringList
    }

}
