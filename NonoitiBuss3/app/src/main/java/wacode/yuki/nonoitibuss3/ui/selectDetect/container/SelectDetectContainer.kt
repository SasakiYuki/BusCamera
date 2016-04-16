package wacode.yuki.nonoitibuss3.ui.selectDetect.container

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import butterknife.bindView
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.DetectDetailLayout
import wacode.yuki.nonoitibuss3.ui.selectDetect.selectImageList.ImageListView

/**
 * Created by Yuki on 2016/04/10.
 */
class SelectDetectContainer(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs), SelectDetectContainerLayout {
    val top: ImageListView by bindView(R.id.listView)
    val detect: DetectDetailLayout by bindView(R.id.detailLayout)
    override fun dispTop() {
        top.visibility = VISIBLE
        detect.visibility = GONE
        top.setListView()
    }

    override fun dispImageDetail(strBase64: String) {
        top.visibility = GONE
        detect.visibility = VISIBLE
        val layout = findViewById(R.id.detailLayout) as DetectDetailLayout
        layout.bindViews(strBase64)
    }

}