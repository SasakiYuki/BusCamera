package wacode.yuki.nonoitibuss3.ui.selectDetect.detailLayout.detectList

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import wacode.yuki.nonoitibuss3.entity.FaceResult

/**
 * Created by Yuki on 2016/04/10.
 */
class DetectListAdapter(context: Context, resource: Int, objects: MutableList<FaceResult>?)
    : ArrayAdapter<FaceResult>(context, resource, objects) {
    private val m_layoutInflater: LayoutInflater
    private val m_resource:Int

    init {
        m_layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        m_resource= resource
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

        val view = if(convertView == null){
            m_layoutInflater.inflate(m_resource,null) as DetectListLayout
        }else{
            convertView as DetectListLayout
        }

        view.bindViews(getItem(position))

        return view
    }
}