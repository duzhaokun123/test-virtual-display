package io.github.duzhaokun123.testvirtualdisplay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Surface

class NewDisplayActivity: Activity() {
    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (mediaProjection == null) {
            toast("请先初始化")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val intent = intent
        val name = intent.getStringExtra("name") ?: "DisplayActivity"
        val height = intent.getIntExtra("height", 800)
        val width = intent.getIntExtra("width", 400)
        val dpi = intent.getIntExtra("dpi", 1)

        val surface = Surface(SurfaceTexture(0))
        val display = mediaProjection?.createVirtualDisplay(
            name, width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
            surface, null, null
        ) ?: run {
            finish()
            return
        }
        displayList.add(display)
        sendBroadcast(Intent(ACTION_RELIST))
        setResult(RESULT_OK, Intent().putExtra("id", display.display.displayId))
        finish()
    }
}