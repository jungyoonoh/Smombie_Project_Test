package com.example.mymap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*

class AdminActivity : AppCompatActivity() {

    var backKeyPressedTime:Double = 0.0
    var firstcall = true
    val myDbHelper: MyDBHelper = MyDBHelper(this)
    lateinit var data:ArrayList<MyData>
    var fusedLocationClient: FusedLocationProviderClient?= null
    var locationCallback: LocationCallback?= null
    var locationRequest: LocationRequest?= null
    lateinit var id:String
    var loc = LatLng(37.554752,126.970631) // 관리자는 서울역으로 먼저 뜨도록
    lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        checkTheSetting()
    }

    fun checkTheSetting(){
        init()
        initListener()
        initLocation()
    }

    fun init(){
        val i = intent
        id = i.getStringExtra("ID")
        userId.text="ID : "+id+" (관리자 모드로 작동중)"
        data=ArrayList<MyData>()
        initListener()
        data=myDbHelper.loadData()
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
/*
        button3.setOnClickListener{
            val intent = Intent(this, AlarmService::class.java)
            startService(intent)
            Toast.makeText(this,"Service 시작", Toast.LENGTH_SHORT).show();
        }

        button4.setOnClickListener {
            val intent = Intent(this, AlarmService::class.java)
            stopService(intent)
            Toast.makeText(this,"Service 끝", Toast.LENGTH_SHORT).show();
        }*/
    }

    fun initLocation(){
        // 권한정보 체크 = checkSelfPermission
        if(ActivityCompat.checkSelfPermission(this, // 이미 허용되어있다면?
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getuserlocation()
            startLocationUpdates() // 갱신해주기
            initmap() // 맵정보 초기화
        } else{ // 허용하지 않았다면 or 맨처음 시작이라면?
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),100)
        }
    }

    fun initmap() {
        // map을 초기화를 해줘야..
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            googleMap = it
            googleMap.setMaxZoomPreference(24.0f)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,18.0f))
            initMapListener()
        }
    }

    fun setCrossWalkMarker(){ // 근처 애들만 마커찍기 테스트용
        Log.e("size : ",data.size.toString())
        for(i in 0..data.size - 1) {
                val options = MarkerOptions()
                val sample = LatLng(data.get(i).lat.toDouble(), data.get(i).long.toDouble())
                options.position(sample)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                val mk1 = googleMap.addMarker(options)
                mk1.showInfoWindow()
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
                if(firstcall) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,14.0f))
                    setCrossWalkMarker()
                    firstcall = false
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequest, // 이 주기로
            locationCallback, // 호출시 이 함수가
            Looper.getMainLooper() // 메시지 큐에 전달되는건 이 루퍼에서 처리해준다
        )
    }

    fun getuserlocation(){
        fusedLocationClient=
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient?.lastLocation?.addOnSuccessListener {

        }
    }

    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    fun initMapListener(){
        googleMap.setOnMapClickListener {

        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        // 2초 이내로 눌러야함
        if(System.currentTimeMillis()>backKeyPressedTime+2000){
            backKeyPressedTime = System.currentTimeMillis().toDouble();
            Toast.makeText(this, "한 번더 누르면 종료합니다.", Toast.LENGTH_SHORT).show();
        }
        //2번째 백버튼 클릭 (종료)
        else{
            appFinish();
        }
    }
    fun appFinish(){
        finishAffinity();
        System.runFinalization();
        System.exit(0);
    }
}