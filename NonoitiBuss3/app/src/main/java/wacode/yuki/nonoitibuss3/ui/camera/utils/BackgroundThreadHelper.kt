package wacode.yuki.nonoitibuss2.Utils

import android.os.Handler
import android.os.HandlerThread
import android.util.Log

internal class BackgroundThreadHelper {

    companion object {
        private val TAG = "BackgroundThreadHelper"
    }

    private var thread: HandlerThread? = null
    private var handler: Handler? = null


    fun start() {
        thread = HandlerThread("CameraBackground")
        thread!!.start()
        handler = Handler(thread!!.looper)
    }

    fun stop() {
        thread!!.quitSafely()
        try {
            thread!!.join()
            thread = null
            handler = null
        } catch (e: InterruptedException) {
            Log.d(TAG,e.message);
        }
    }

    fun getHandler(): Handler? {
        if (handler == null) {
            Log.e(TAG, "BackgroundThread Error handler null")
        }
        return handler
    }



}
