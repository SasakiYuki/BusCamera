package wacode.yuki.nonoitibuss3.ui.camera.utils

/**
 * Created by Yuki on 2016/04/16.
 */
object TimeSwitcher{
    fun checkBusStop(currentTime:Int):Boolean{
        when(currentTime){
            in 845..855 -> return true
            in 905..915 -> return true
            in 955..1005 -> return true
            in 1015..1023 ->return true
            in 1118.. 1123 -> return true
            in 1240.. 1248 -> return true
            in 1250.. 1258 -> return true
            in 1400.. 1408 -> return true
            in 1515.. 1523 -> return true
            in 1700.. 1708 -> return true
            in 1723.. 1728 -> return true
            in 1815.. 1823 -> return true
            in 1920.. 1928 -> return true
        }
        return false
    }
}