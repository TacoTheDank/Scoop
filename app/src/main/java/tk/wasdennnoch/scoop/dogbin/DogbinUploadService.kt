package tk.wasdennnoch.scoop.dogbin

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import tk.wasdennnoch.scoop.R
import tk.wasdennnoch.scoop.XposedHook
import tk.wasdennnoch.scoop.data.crash.Crash
import java.util.*

class DogbinUploadService : Service() {

    private var uploadStarted = false
    private val uploadQueue: Queue<Intent> = LinkedList()

    override fun onBind(intent: Intent): IBinder {
        TODO("not implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val data = intent.getParcelableExtra<Intent>("data")
        val crash = intent.getParcelableExtra<Crash>("crash")
        data?.putExtra("crash", crash)
        data?.putExtra(XposedHook.INTENT_UPDATE, true)
        data?.putExtra(XposedHook.INTENT_HIDE_UPLOAD, true)
        sendBroadcast(data)
        data?.putExtra(XposedHook.INTENT_HIDE_UPLOAD, false)

        uploadQueue.offer(data)
        if (!uploadStarted) {
            uploadNext()
        }
        return START_STICKY
    }

    private fun uploadNext() {
        uploadStarted = true
        val data = uploadQueue.poll()
        if (data == null) {
            stopSelf()
        } else {
            DogbinUtils.upload(
                data.getStringExtra(XposedHook.INTENT_STACKTRACE),
                object : DogbinUtils.UploadResultCallback {

                    override fun onSuccess(url: String) {
                        data.putExtra(XposedHook.INTENT_DOGBIN_LINK, url)
                        next()
                    }

                    override fun onFail(message: String, e: Exception) {
                        data.putExtra(XposedHook.INTENT_UPLOAD_ERROR, true)
                        next()
                    }

                    private fun next() {
                        sendBroadcast(data)
                        uploadNext()
                    }
                })
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("DUS", "onCreate")

        startForeground(
            101, NotificationCompat.Builder(this, "status")
                .setSmallIcon(R.drawable.ic_bug_report)
                .setContentTitle(getString(R.string.dogbin_uploading))
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        )
    }
}
