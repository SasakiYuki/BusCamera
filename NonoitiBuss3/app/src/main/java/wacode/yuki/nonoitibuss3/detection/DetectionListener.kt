package wacode.yuki.nonoitibuss3.detection

import wacode.yuki.nonoitibuss3.entity.FaceResult
import java.util.*

/**
 * Created by Yuki on 2016/04/12.
 */
interface DetectionListener {
    fun detectionFinished(arrayFaceResult:ArrayList<FaceResult>)
    fun detectionFailed()
}