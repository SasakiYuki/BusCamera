package wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.detectList

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.entity.FaceResult
import java.util.*

/**
 * Created by Yuki on 2016/04/10.
 */
class DetectListView(context: Context?, attrs: AttributeSet?) : ListView(context, attrs) {
    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    fun setListView(list: ArrayList<FaceResult>){
        val adapter = DetectListAdapter(context, R.layout.listview_detect, list)
        setAdapter(adapter)
    }
}