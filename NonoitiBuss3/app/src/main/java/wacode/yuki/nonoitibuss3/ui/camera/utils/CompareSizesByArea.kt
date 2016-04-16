package wacode.yuki.nonoitibuss2.Utils

import android.util.Size
import java.util.*

/**
 * Created by Yuki on 2016/04/09.
 */
internal class CompareSizesByArea : Comparator<Size>{
    override fun compare(p0: Size, p1: Size): Int {
        val long =((p0.width * p0.height) as Long)-((p1.width * p1.height)as Long)
        return if(long > 0) 1 else if(long < 0) -1 else 0
    }
}