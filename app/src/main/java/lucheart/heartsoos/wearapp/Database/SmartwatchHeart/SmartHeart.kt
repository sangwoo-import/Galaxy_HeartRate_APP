package lucheart.heartsoos.wearapp.Database.SmartwatchHeart

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement

@Measurement(name = "heart_watch")
data class SmartHeart(
//    @Column(tag = true) val _user: String,
    @Column val value: Int,
    @Column(timestamp = true) val time: Instant
) {
//    @RequiresApi(Build.VERSION_CODES.O)
//    constructor(_user: String, value: Int) : this(_user, value, Instant.now())
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(value: Int) : this(value, Instant.now())
}


