package lucheart.heartsoos.wearapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.neovisionaries.ws.client.WebSocketState
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import lucheart.heartsoos.wearapp.Database.SmartwatchHeart.SmartHeart
import lucheart.heartsoos.wearapp.Database.SmartwatchHeart.SmartHeartDao
import java.lang.IndexOutOfBoundsException


@OptIn(DelicateCoroutinesApi::class)
class MainActivity :Activity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String
    private var service: HeartRateService? = null
    private var hrs : String  = ""

    private var hr  : Int =0

    private val toneVolue = ToneGenerator(AudioManager.STREAM_SYSTEM, 500)
    private var standard = 100

    private var broadcastReceiver = object : BroadcastReceiver() {

        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {
                    "updateHR" -> if (intent != null) {
                        textBPM.text= "${intent.extras!!.get("bpm")} bpm"
                    }
                    "sendHR" -> if (intent != null) {
                        hrs = "${intent.extras!!.get("bpm1")}"
                        hr = hrs.toInt()

                    }
                    "updateState" -> if (intent != null) {
                        updateWebSocketState(intent.extras!!.get("state") as WebSocketState)
                    }
                }
            }

            /* influxDB에 저장하는 함수*/
            fun saveHeartRate() {
                val dao = SmartHeartDao()
                val heartRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SmartHeart(hr)
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                 GlobalScope.launch(Dispatchers.IO){
                    while(isActive){
                        dao.insert(heartRate)
                        delay(1000)

                    }

                 }
            }
            saveHeartRate()

/* 진동 및 소리 기능 */
            fun goWarining(){
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))

//                toneVolue.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
            }

            if(hr <=standard *1 && hr>=standard*0.9){
                goWarining()
            }
        }



    }












    @RequiresApi(Build.VERSION_CODES.O)
    override  fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission(Manifest.permission.BODY_SENSORS, 100);

        val filter = IntentFilter()
        filter.addAction("updateHR")
        filter.addAction("sendHR")
        filter.addAction("updateState")
        registerReceiver(broadcastReceiver, filter)
    }



    private fun updateWebSocketState(state: WebSocketState) {
        val color = when(state) {
            WebSocketState.OPEN -> Color.GREEN
            WebSocketState.CLOSED, WebSocketState.CLOSING -> Color.RED
            WebSocketState.CREATED -> Color.LTGRAY
            WebSocketState.CONNECTING -> Color.argb(255, 204, 105, 0)
        }

        heartButton.setColorFilter(color)

    }


    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_DENIED) return
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
    }

    private var mConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, ser: IBinder) {
            Toast.makeText(this@MainActivity, "Service is connected", Toast.LENGTH_SHORT).show()
            val mLocalBinder = ser as HeartRateService.LocalBinder
            service = mLocalBinder.getServerInstance()

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Toast.makeText(this@MainActivity, "Service is disconnected", Toast.LENGTH_SHORT).show()
            service = null

        }

    }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart();


        Intent(this, HeartRateService::class.java).also { intent ->
            bindService(intent, mConnection, 0)
            startForegroundService(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if(service != null) {
            unbindService(mConnection)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val stopIntent = Intent()
        stopIntent.action = "STOP_ACTION";
        var pendingIntentStopAction =
            PendingIntent.getBroadcast(this, 12345, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        pendingIntentStopAction.send()
    }

}


