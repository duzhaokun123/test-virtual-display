package io.github.duzhaokun123.testvirtualdisplay

lateinit var application: android.app.Application

class Application: android.app.Application() {
    override fun onCreate() {
        application = this
        super.onCreate()
    }
}