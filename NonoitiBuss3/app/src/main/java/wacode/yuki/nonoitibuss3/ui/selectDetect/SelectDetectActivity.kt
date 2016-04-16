package wacode.yuki.nonoitibuss3.ui.selectDetect

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import wacode.yuki.nonoitibuss3.R
import wacode.yuki.nonoitibuss3.ui.selectDetect.container.SelectDetectContainer

class SelectDetectActivity : AppCompatActivity() {
    private var container :SelectDetectContainer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_detect)
        setContainer()
    }

    private fun setContainer(){
        container = findViewById(R.id.container) as SelectDetectContainer
    }

    fun getContainer() :SelectDetectContainer? = container
}
