package wacode.yuki.nonoitibuss2.View.Camera

import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Size
import android.view.Surface

interface CameraListener {
    val surfaceTextureFromTextureView: SurfaceTexture
    val previewSize: Size?
    val backgroundHandler: Handler?
    val imageRenderSurface: Surface
    val rotation: Int
}
