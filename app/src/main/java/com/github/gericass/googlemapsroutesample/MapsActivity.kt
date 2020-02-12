package com.github.gericass.googlemapsroutesample

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.facebook.stetho.okhttp3.StethoInterceptor

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, CoroutineScope {

    private lateinit var map: GoogleMap

    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    private val retrofit by lazy {
        val okhttp = OkHttpClient()
                .newBuilder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
        val moshi = Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        Retrofit.Builder()
                .client(okhttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl("https://maps.googleapis.com")
                .build()
    }

    private val client = retrofit.create(RouteClient::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val from = LatLng(41.841155, 140.768203)
        val to = LatLng(41.801190, 140.756835)
        map.addMarker(MarkerOptions().position(from).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLng(from))

        val fromString = "${from.latitude},${from.longitude}"
        val toString = "${to.latitude},${to.longitude}"
        renderRoute(fromString, toString)
    }

    // 経路を取得するAPIを叩く
    private fun renderRoute(from: String, to: String) {
        launch {
            try {
                val resp = client.getRoute(from, to, getString(R.string.google_maps_key))
                val steps = resp.routes?.firstOrNull()?.legs?.firstOrNull()?.steps
                steps?.map { step ->
                    val points = step?.polyline?.points
                    PolyUtil.decode(points)
                }?.forEach {
                    // 線を描画
                    map.addPolyline(PolylineOptions().addAll(it).color(Color.RED))
                }
            } catch (e: Exception) {
                Log.e("MapsActivity", e.toString())
            }
        }
    }

    interface RouteClient {
        @GET("/maps/api/directions/json")
        suspend fun getRoute(
                @Query("origin") origin: String,
                @Query("destination") destination: String,
                @Query("key") key: String
        ): Route
    }
}
