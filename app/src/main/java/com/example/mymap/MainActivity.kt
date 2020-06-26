package com.example.mymap

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.sqrt
import kotlin.math.absoluteValue
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    val myDbHelper: MyDBHelper = MyDBHelper(this)
    val myAdminDbHelper: MyAdminDBHelper = MyAdminDBHelper(this)
    lateinit var data:ArrayList<MyData>
    var fusedLocationClient: FusedLocationProviderClient ?= null
    var locationCallback: LocationCallback?= null
    var locationRequest: LocationRequest?= null

    lateinit var savLatLng:LatLng

    lateinit var googleMap: GoogleMap

    var loc = LatLng(37.554752,126.970631)

    lateinit var id:String

    var backKeyPressedTime:Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkTheSetting()
    }

    override fun onRestart() {
        super.onRestart()
        initLocation()
    }

    fun settingTab(){ // 네비게이션 바
        navigationView.setOnNavigationItemSelectedListener {
            when(it.itemId){
                R.id.navigation_home->{
                    menu1.visibility=VISIBLE
                    menu2.visibility= GONE
                    menu3.visibility= GONE
                }
                R.id.navigation_comment->{
                    menu1.visibility=GONE
                    menu2.visibility=VISIBLE
                    menu3.visibility= GONE
                }
                R.id.navigation_setting->{
                    menu1.visibility=GONE
                    menu2.visibility= GONE
                    menu3.visibility= VISIBLE
                }
            }
            return@setOnNavigationItemSelectedListener true
        }

    }

    fun checkTheSetting(){
        init()
        initListener()
        settingTab()
        initLocation()
    }

    fun initListener(){
        button.setOnClickListener {
            AuthUI.getInstance() // 로그아웃
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
                .delete(this) // 계정 탈퇴
                .addOnCompleteListener {
                    val intent = Intent(this, AlarmService::class.java)
                    stopService(intent)
                    val i = Intent(this, StartActivity::class.java)
                    startActivity(i)
                }
        }
        submitbtn.setOnClickListener{
            // db에 넣기위해 0 = false / 1 = true
            var cw = 0
            var tl = 0
            var m2 = 0
            var text = editTextMultiLine.text.toString()
            when(group1.checkedRadioButtonId){
                R.id.yesCW->{cw = 1}
            }
            when(group2.checkedRadioButtonId){
                R.id.yesTL->{tl = 1}
            }
            when(group3.checkedRadioButtonId){
                R.id.yes2dir->{m2 = 1}
            }
            val adminData = adminData(savLatLng.latitude.toString(),savLatLng.longitude.toString(),cw,tl,m2,text)
            myAdminDbHelper.addAdminData(adminData)
            Toast.makeText(this,"제출되었습니다.",Toast.LENGTH_SHORT).show()
        }

        // 토글버튼
        val pref = getSharedPreferences("checkOnOff", Activity.MODE_PRIVATE)
        val check=pref.getBoolean("checkOnOff",false)//(키 값, 디폴트값 : 첫실행때 갖는값)
        val editor = pref.edit()
        OnOffSw.isChecked = check
        OnOffSw.setOnCheckedChangeListener { compoundButton, b ->
            if(b){
                val intent = Intent(this, AlarmService::class.java)
                editor.putBoolean("checkOnOff",true)
                editor.commit()//저장 실행하는 함수
                startService(intent)
                Toast.makeText(this,"Service 시작",Toast.LENGTH_SHORT).show();
            }else{
                val intent = Intent(this, AlarmService::class.java)
                stopService(intent)
                editor.putBoolean("checkOnOff",false)
                editor.commit()//저장 실행하는 함수
                Toast.makeText(this,"Service 끝",Toast.LENGTH_SHORT).show();
            }
        }

    }

    fun setCrossWalkMarker(loc:LatLng){ // 근처 애들만 마커찍기용
        var temp:FloatArray= FloatArray(1)
        for(i in 0..data.size - 1) {
            data.get(i) // MyData(lat,lng);
            Location.distanceBetween(loc.latitude,loc.longitude,data.get(i).lat.toDouble(), data.get(i).long.toDouble(),temp)
                if (temp[0] < 500) { // 인근 정보만 표시되도록
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

    // db갱신
    fun init(){
        val i = intent
        id = i.getStringExtra("ID")
        userId.text="ID : "+id
        data=ArrayList<MyData>()
        data=myDbHelper.loadData()
    }

    fun initLocation(){
        // 권한정보 체크 = checkSelfPermission
        if(ActivityCompat.checkSelfPermission(this, // 이미 허용되어있다면?
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
        } else{ // 허용하지 않았다면 or 맨처음 시작이라면?
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),100)
        }
        getuserlocation() //유저의 최근 현재 위치에 대한 정보 받기
        startLocationUpdates() // 갱신해주기
        initmap() // 맵정보 초기화
    }

    fun getuserlocation(){
        fusedLocationClient=
            LocationServices.getFusedLocationProviderClient(this) 
        fusedLocationClient?.lastLocation?.addOnSuccessListener { 
            loc = LatLng(it.latitude,it.longitude) // 유저 위치 가져오기
        }
    }

    // 권한 체크
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
                    addCircleNearUser(loc)
                    setCrossWalkMarker(loc)
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
    // 유저 입장에서 반경
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

    // 맵 눌렀을때
    fun initMapListener(){
        googleMap.setOnMapClickListener {
            googleMap.clear()
            val options = MarkerOptions()
            val sample = LatLng(it.latitude, it.longitude) // 사용자가 찍은 위치
            options.position(sample)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))

            var geocoder = Geocoder(this)
            var list = geocoder.getFromLocation(it.latitude,it.longitude,1)
            options.title("새로운 횡단보도 제보하기")
            var str = list[0].getAddressLine(0)
            options.snippet(str)

            val mk1 = googleMap.addMarker(options)
            mk1.showInfoWindow()
            googleMap.setOnInfoWindowClickListener {
                menu1.visibility=GONE
                menu2.visibility=VISIBLE
                menu3.visibility= GONE
                val item: MenuItem = navigationView.menu.findItem(R.id.navigation_comment)
                item.setChecked(true)
                savLatLng = sample
            }
        }
    }
}
