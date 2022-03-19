package io.github.duzhaokun123.testvirtualdisplay

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

val displayList = mutableListOf<VirtualDisplay>()
var mediaProjection: MediaProjection? = null

const val ACTION_RELEASE_ALL = "io.github.duzhaokun123.testvirtualdisplay.action.RELEASE_ALL"
const val ACTION_RELEASE = "io.github.duzhaokun123.testvirtualdisplay.action.RELEASE"
const val ACTION_NEW = "io.github.duzhaokun123.testvirtualdisplay.action.NEW"
const val ACTION_RESIZE = "io.github.duzhaokun123.testvirtualdisplay.action.RESIZE"
const val ACTION_RELIST = "io.github.duzhaokun123.testvirtualdisplay.action.RELIST"

class MainActivity : Activity() {
    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    private val releaseAll = ReleaseAll()
    private val release = Release()
    private val new = New()
    private val resize = Resize()
    private val relist = Relist()

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
            sendBroadcast(Intent(ACTION_NEW).putExtra("name", name).putExtra("width", width).putExtra("height", height))
        }
        findViewById<Button>(R.id.btn_releaseAll).setOnClickListener {
            sendBroadcast(Intent(ACTION_RELEASE_ALL))
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            mediaProjection?.stop()
            mediaProjection = null
            displayList.clear()
            relist()
            toast("停止成功")
        }
        relist()
        registerReceiver(releaseAll, IntentFilter(ACTION_RELEASE_ALL))
        registerReceiver(release, IntentFilter(ACTION_RELEASE))
        registerReceiver(new, IntentFilter(ACTION_NEW))
        registerReceiver(resize, IntentFilter(ACTION_RESIZE))
        registerReceiver(relist, IntentFilter(ACTION_RELIST))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(releaseAll)
        unregisterReceiver(release)
        unregisterReceiver(new)
        unregisterReceiver(resize)
        unregisterReceiver(relist)
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
        startActivityForResult(
            Intent(this, NewDisplayActivity::class.java).putExtra("name", name).putExtra("width", width).putExtra("height", height).putExtra("dpi", resources.displayMetrics.densityDpi),2
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (data == null) return
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            toast("初始化成功")
        }
        if (requestCode == 2) {
            toast("id: ${data?.getIntExtra("id", -1)}")
            relist()
        }
        super.onActivityResult(requestCode, resultCode, data)
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
                            sendBroadcast(Intent(ACTION_RELEASE).putExtra("id", vd.display.displayId))
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                addView(btn)
            }
        }
    }

    private fun resize(display: VirtualDisplay, width: Int, height: Int) {
        display.resize(width, height, DisplayMetrics.DENSITY_DEFAULT)
        relist()
    }

    inner class ReleaseAll : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RELEASE_ALL) {
                releaseAll()
            }
        }
    }
    inner class Release : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RELEASE) {
                val display = intent.getIntExtra("id", 0)
                displayList.find { it.display.displayId == display }
                    ?.let { releaseVirtualDisplay(it) }
            }
        }
    }
    inner class New : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NEW) {
                val name = intent.getStringExtra("name") ?: "VirtualDisplay"
                val width = intent.getIntExtra("width", findViewById<EditText>(R.id.et_width).text.toString().toInt())
                val height = intent.getIntExtra("height", findViewById<EditText>(R.id.et_height).text.toString().toInt())
                newVirtualDisplay(name, width, height)
            }
        }
    }
    inner class Resize : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RESIZE) {
                val display = intent.getIntExtra("id", 0)
                val width = intent.getIntExtra("width", findViewById<EditText>(R.id.et_width).text.toString().toInt())
                val height = intent.getIntExtra("height", findViewById<EditText>(R.id.et_height).text.toString().toInt())
                displayList.find { it.display.displayId == display }
                    ?.let { resize(it, width, height) }
            }
        }
    }
    inner class Relist : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_RELIST) {
                relist()
            }
        }
    }
}

fun toast(msg: String) {
    Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
}
