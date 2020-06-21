package com.example.mymap

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth


class AlarmService :Service (){
    var fusedLocationClient: FusedLocationProviderClient?= null
    var locationCallback: LocationCallback?= null
    var locationRequest: LocationRequest?= null

    val myDbHelper: MyDBHelper = MyDBHelper(this)
    lateinit var data:ArrayList<MyData>

    var loc = LatLng(37.554752,126.970631)

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //TODO : 서비스 처음 시작시 할 동작 정의.
        data=myDbHelper.loadData()
        makeNotification()
        initLocation()
      //

        return START_REDELIVER_INTENT
    }
    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }
    override fun onDestroy() {
        //TODO : 서비스 종료시 할 것들
        super.onDestroy()
        stopLocationUpdates()
    }

    fun makeNotification(){
        val CHANNELID="notification1"
        val notificationChannel = NotificationChannel(// 알람등 설정 가능한 channel 설정
            CHANNELID,
            "Start Check Notification",
            NotificationManager. IMPORTANCE_DEFAULT
        )
        val builder=Notification.Builder(this,CHANNELID)
        builder.setContentTitle ("알림시작 ")
        builder.setContentText("지도 알림을 시작합니다")
            .setSmallIcon( R.mipmap. ic_launcher)

                val intent = Intent(this,MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP //화면 전환 intent

        val user = FirebaseAuth.getInstance().currentUser
        intent.putExtra("ID",user?.email.toString())
        // id 넘겨줘야함
        val pendingIntent = PendingIntent.getActivity(this,
            1,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingIntent)

        val notification = builder.build()

        startForeground(1, notification)
    }
    fun initLocation(){

            getuserlocation() //유저의 최근 현재 위치에 대한 정보 받기
            startLocationUpdates() // 갱신해주기

    }

    fun getuserlocation(){
        fusedLocationClient=
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient?.lastLocation?.addOnSuccessListener {
            loc = LatLng(it.latitude,it.longitude) // 유저 위치 가져오기
        }
    }

    fun startLocationUpdates(){
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
                    makeLocation()
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(
            locationRequest, // 이 주기로
            locationCallback, // 호출시 이 함수가
            Looper.getMainLooper() // 메시지 큐에 전달되는건 이 루퍼에서 처리해준다
        )
    }

    fun makeLocation(){
        //현재위치 갱신후 거리계산 후 있으면 알림
        var temp:FloatArray= FloatArray(1)
        for(i in 0..data.size - 1) {
            data.get(i) // MyData(lat,lng);
            Location.distanceBetween(loc.latitude,loc.longitude,data.get(i).lat.toDouble(), data.get(i).long.toDouble(),temp)
            Log.e("거릿값",temp[0].toString())
            if(temp[0]<1000 ){
                //알람
                if(data.get(i).isNearby==false) {
                    data.get(i).isNearby=true
                    val CHANNELID = "notification2"
                    val notificationChannel = NotificationChannel(// 알람등 설정 가능한 channel 설정
                        CHANNELID,
                        "Map Check Notification",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationChannel.enableVibration(true)
                    notificationChannel.vibrationPattern = longArrayOf(100, 200)

                    //화면꺼진상태에선 on 상태여도 알림 x

                    val builder = Notification.Builder(this, CHANNELID)
                    builder.setContentTitle("위험!")
                    builder.setContentText("주변에 횡단보도 감지 주의하세요!")
                        .setSmallIcon(R.mipmap.ic_launcher)

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP //화면 전환 intent

                    val user = FirebaseAuth.getInstance().currentUser
                    intent.putExtra("ID", user?.email.toString())

                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        1, intent, PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    builder.setContentIntent(pendingIntent)

                    val notification = builder.build()
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                            as NotificationManager
                    manager.createNotificationChannel(notificationChannel)

                    manager.notify(10, notification)
                }

            }else data.get(i).isNearby = false

        }// 확인누르면 알림끝내고
    }

    //지도기능
}