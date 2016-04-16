package wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.detectList

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import butterknife.bindView
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.entity.FaceResult

/**
 * Created by Yuki on 2016/04/10.
 */
class DetectListLayout(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    val textView_age: TextView by bindView(R.id.textView_age)
    val textView_gender: TextView by bindView(R.id.textView_gender)
    val textView_smile: TextView by bindView(R.id.textView_smile)
    val imageView:ImageView by bindView(R.id.imageView)

    override fun onFinishInflate() {
        super.onFinishInflate()
    }

    fun bindViews(entity: FaceResult){
        textView_age.text = entity.age.toString()
        textView_smile.text = entity.smile.toString()
        textView_gender.text = entity.gender
        imageView.setImageBitmap(entity.bitmap)
    }
}