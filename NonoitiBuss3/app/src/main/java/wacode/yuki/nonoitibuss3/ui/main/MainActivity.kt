package wacode.yuki.nonoitibuss3.ui.main

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import butterknife.bindView
import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.ui.allDetect.AllDetectActivity
import wacode.yuki.nonoitibuss3.ui.camera.CameraActivity
import wacode.yuki.nonoitibuss3.ui.camera.jCameraActivity
import wacode.yuki.nonoitibuss3.ui.selectDetect.SelectDetectActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    val button_camera: Button by bindView(R.id.button_camera)
    val button_select_image:Button by bindView(R.id.button_select_image)
    val button_select_all:Button by bindView(R.id.button_select_all)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setAllClickListener()
    }

    private fun setAllClickListener(){
        button_camera.setOnClickListener {
            val intent = Intent(this, jCameraActivity::class.java)
            startActivity(intent)
        }
        button_select_image.setOnClickListener {
           val intent = Intent(this,SelectDetectActivity::class.java)
            startActivity(intent)
        }
        button_select_all.setOnClickListener {
            intent = Intent(this,AllDetectActivity::class.java)
            startActivity(intent)
        }
    }

}
