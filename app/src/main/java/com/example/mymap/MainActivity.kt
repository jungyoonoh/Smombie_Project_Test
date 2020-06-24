package com.example.mymap

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Math.pow
import java.lang.Math.sqrt
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.absoluteValue
import kotlin.math.log
import kotlin.math.pow
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    val myDbHelper: MyDBHelper = MyDBHelper(this)
    lateinit var data:ArrayList<MyData>
    var fusedLocationClient: FusedLocationProviderClient ?= null
    var locationCallback: LocationCallback?= null
    var locationRequest: LocationRequest?= null

    lateinit var googleMap: GoogleMap

    var loc = LatLng(37.554752,126.970631)
    val arrLoc = ArrayList<LatLng>()

    lateinit var id:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkTheSetting()

    }

    fun checkTheSetting(){
        init()
        initListener()
        initLocation()

    }

    fun initListener(){
        button.setOnClickListener {
            AuthUI.getInstance()//로그아웃
                .signOut(this)
                .addOnCompleteListener {
                    val intent = Intent(this, AlarmService::class.java)
                    stopService(intent)
                    val i = Intent(this, StartActivity::class.java)
                    startActivity(i)
                }
        }
        button2.setOnClickListener {
            AuthUI.getInstance()
                .delete(this)//계정 탈퇴
                .addOnCompleteListener {
                    val intent = Intent(this, AlarmService::class.java)
                    stopService(intent)
                    val i = Intent(this, StartActivity::class.java)
                    startActivity(i)
                }
        }

        button3.setOnClickListener{
            val intent = Intent(this, AlarmService::class.java)
            startService(intent)
            Toast.makeText(this,"Service 시작",Toast.LENGTH_SHORT).show();


        }
        button4.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java)
            stopService(intent)
            Toast.makeText(this,"Service 끝",Toast.LENGTH_SHORT).show();


        }
    }

    fun search(loc:LatLng){
        var minLatitude:Double = (data.get(0).lat.toDouble() - loc.latitude).absoluteValue
        var minLongitude:Double = (data.get(0).long.toDouble() - loc.longitude).absoluteValue
        var savDistance = sqrt(minLatitude.pow(2.0) + minLongitude.pow(2.0))
        var distance:Double = 0.1
        var savLat:Double = -0.1
        var savLong:Double = -0.1
        for(i in 0..data.size - 1) {
            minLatitude = (loc.latitude - data.get(i).lat.toDouble()).absoluteValue
            minLongitude = (loc.longitude - data.get(i).long.toDouble()).absoluteValue
            distance = sqrt(minLatitude .pow(2.0) + minLongitude.pow(2.0))
            if(savDistance > distance) {
                savDistance = distance
                savLat = data.get(i).lat.toDouble()
                savLong = data.get(i).long.toDouble()
            }
        }
        Log.e("DATA : ", "$savLat, $savLong")
        val options = MarkerOptions()
        val sample = LatLng(savLat, savLong)
        options.position(sample)
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        val mk1 = googleMap.addMarker(options)
        mk1.showInfoWindow()
    }
    fun setCrossWalkMarker(loc:LatLng){ // 근처 애들만 마커찍기 테스트용
        var checkDirection:BooleanArray= booleanArrayOf(false,false,false,false)
        var temp:FloatArray= FloatArray(1)
        for(i in 0..data.size - 1) {
            data.get(i) // MyData(lat,lng);
            Location.distanceBetween(loc.latitude,loc.longitude,data.get(i).lat.toDouble(), data.get(i).long.toDouble(),temp)
            if(temp[0]<1000 ){
                Log.e("DATA : ", data.get(i).lat + ", " + data.get(i).long)
                val options = MarkerOptions()
                val sample = LatLng(data.get(i).lat.toDouble(), data.get(i).long.toDouble())
                options.position(sample)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                val mk1 = googleMap.addMarker(options)
                mk1.showInfoWindow()
            }

        }
    }
    fun init(){
        val i = intent
        id = i.getStringExtra("ID")
        userId.text="ID : "+id
        data=ArrayList<MyData>()
        initListener()
        data=myDbHelper.loadData()
    }
    fun initLocation(){
        // 권한정보 체크 = checkSelfPermission
        if(ActivityCompat.checkSelfPermission(this, // 이미 허용되어있다면?
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getuserlocation() //유저의 최근 현재 위치에 대한 정보 받기
            startLocationUpdates() // 갱신해주기
            initmap() // 맵정보 초기화
        } else{ // 허용하지 않았다면 or 맨처음 시작이라면?
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),100)
        }
    }

    fun getuserlocation(){
        fusedLocationClient=
            LocationServices.getFusedLocationProviderClient(this) 
        fusedLocationClient?.lastLocation?.addOnSuccessListener { 
            loc = LatLng(it.latitude,it.longitude) // 유저 위치 가져오기
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED){
                getuserlocation()
                startLocationUpdates()
                initmap()
            } else{
                Toast.makeText(this,"위치정보 제공을 하셔야 합니다.",Toast.LENGTH_SHORT).show()
                initmap()
            }
        }
    }

    fun startLocationUpdates(){ // 현재 위치 받아오기
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        // 조건을 만족할때 위치정보를 가져오면 이함수 호출
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for(location in locationResult.locations){
                    loc = LatLng(location.latitude,location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,18.0f)) // 위치 바로이동
                    // 비교하는 함수를 넣어서 가장 가까운 녀석 좌표 뽑아보기 할까?
                    addCircleNearUser(loc)
                    setCrossWalkMarker(loc)
                    //search(loc)
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequest, // 이 주기로
            locationCallback, // 호출시 이 함수가
            Looper.getMainLooper() // 메시지 큐에 전달되는건 이 루퍼에서 처리해준다
        )
    }

    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    fun initmap() {
        // map을 초기화를 해줘야..
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            googleMap = it
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,24.0f))
            googleMap.setMinZoomPreference(8.0f)
            googleMap.setMaxZoomPreference(24.0f)
            initMapListener()
        }
    }
    fun addCircleNearUser(loc:LatLng){
        googleMap.clear()
        var nearCircle = CircleOptions().center(loc) // 중심점
            .radius(50.0)   //반지름 단위 : m
            .strokeColor(Color.parseColor("#884169e1"))
            .fillColor(Color.parseColor("#5587cefa")); //배경색
        googleMap.addCircle(nearCircle)
        var user = CircleOptions().center(loc) // 중심점
            .radius(1.0)   //반지름 단위 : m
            .strokeColor(Color.parseColor("#884169e1"))
            .fillColor(Color.parseColor("#884169e1")); //배경색
        googleMap.addCircle(user)
    }
    fun initMapListener(){
        googleMap.setOnMapClickListener {

        }
    }
}
