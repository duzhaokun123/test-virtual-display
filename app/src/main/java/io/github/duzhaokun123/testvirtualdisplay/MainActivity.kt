package io.github.duzhaokun123.testvirtualdisplay

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

val displayList = mutableListOf<VirtualDisplay>()
var mediaProjection: MediaProjection? = null

class MainActivity : Activity() {
    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_init).setOnClickListener {
            if (mediaProjection == null) {
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1)
            } else {
                toast("已经初始化")
            }
        }
        findViewById<Button>(R.id.btn_relist).setOnClickListener {
            relist()
        }
        findViewById<Button>(R.id.btn_new).setOnClickListener {
            val name = findViewById<EditText>(R.id.et_name).text.toString()
            val width = findViewById<EditText>(R.id.et_width).text.toString().toInt()
            val height = findViewById<EditText>(R.id.et_height).text.toString().toInt()
            newVirtualDisplay(name, width, height)
        }
        findViewById<Button>(R.id.btn_releaseAll).setOnClickListener {
            releaseAll()
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            mediaProjection?.stop()
            mediaProjection = null
            displayList.clear()
            relist()
            toast("停止成功")
        }
        relist()
    }

    private fun releaseAll() {
        displayList.forEach {
            it.release()
        }
        displayList.clear()
        relist()
    }

    @SuppressLint("Recycle")
    private fun newVirtualDisplay(name: String, width: Int, height: Int) {
        if (mediaProjection == null) {
            toast("请先初始化")
            return
        }
        val surface = Surface(SurfaceTexture(0))
        val display = mediaProjection!!.createVirtualDisplay(
            name, width, height, DisplayMetrics.DENSITY_DEFAULT,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            surface, null, null
        )
        displayList.add(display)
        relist()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (data == null) return
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            toast("初始化成功")
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun releaseVirtualDisplay(display: VirtualDisplay) {
        display.release()
        displayList.remove(display)
        relist()
    }

    private fun relist() {
        with(findViewById<LinearLayout>(R.id.ll_displays)) {
            removeAllViews()
            displayList.forEach { vd ->
                val btn = Button(this@MainActivity)
                btn.text = vd.toString()
                btn.setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("确认")
                        .setMessage("确认要释放${vd}吗？")
                        .setPositiveButton("确认") { _, _ ->
                            releaseVirtualDisplay(vd)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                addView(btn)
            }
        }
    }
}
