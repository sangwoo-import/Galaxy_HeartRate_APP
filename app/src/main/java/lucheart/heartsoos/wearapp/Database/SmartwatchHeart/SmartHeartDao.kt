package lucheart.heartsoos.wearapp.Database.SmartwatchHeart

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.exceptions.InfluxException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import lucheart.heartsoos.wearapp.BUCKET
import lucheart.heartsoos.wearapp.ORGANIZATION
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class SmartHeartDao {


    companion object {
        const val bucket = BUCKET
        const val org = ORGANIZATION
        const val measurement = "heart_watch"
    }
//    user: String,
    suspend fun getByUserAndPeriod(
         start: Instant, stop: Instant
    ): ArrayList<SmartHeart> {
        val heartRates = ArrayList<SmartHeart>()

        val fluxQuery = ("from(bucket: \"$bucket\")"
                + " |> range(start: $start, stop: $stop)"
                + " |> filter(fn: (r) => (r[\"_measurement\"] == \"$measurement\")")


        val client = InfluxDBClientKotlinFactory.create()
        client.use {
            val results = client.getQueryKotlinApi().query(fluxQuery)
            results
                .consumeAsFlow()
                .filter { measurement == it.measurement }
                .catch {
                    Log.e("InfluxException", "Query: ${it.cause}")
                }
                .collect {
                    val heartRate =  SmartHeart(

                        it.values["hr"] as Int,
                        it.time!!)
                    heartRates.add(heartRate)
                }
        }
        return heartRates
    }


    suspend fun insert(heartRate: SmartHeart): Boolean {
        val client = InfluxDBClientKotlinFactory.create()

        client.use {
            val writeApi = client.getWriteKotlinApi()
            try {
                writeApi.writeMeasurement(heartRate, WritePrecision.NS)
            } catch (ie: InfluxException) {
                Log.e("InfluxException", "Insert: ${ie.cause}")
                return false
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteAllByUser(uid: String): Boolean {
        val client = InfluxDBClientFactory.create()

        val start = OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.of("UTC"))
        val stop = OffsetDateTime.now()

        client.use {
            try {
                val deleteApi = client.deleteApi
                deleteApi.delete(
                    start, stop, "_measurement=\"$measurement\" AND _user=\"$uid\"", bucket, org
                )
            } catch (ie: InfluxException) {
                Log.e("InfluxException", "Delete: ${ie.cause}")
                return false
            }
        }
        return true
    }
}