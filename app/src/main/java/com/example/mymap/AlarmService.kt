package com.example.mymap

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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

    val myDbHelper: MyDBHelper = MyDBHelper(this) // 공공데이터
    lateinit var data:ArrayList<MyData>

    var loc = LatLng(37.554752,126.970631) // 초기위치

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스 시작
        data=myDbHelper.loadData()
        makeNotification()
        initLocation()
        return START_REDELIVER_INTENT
    }
    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }
    override fun onDestroy() {
        // 서비스 종료
        super.onDestroy()
        stopLocationUpdates()
    }

    fun makeNotification(){
        // 푸시 알림 설정
        val CHANNELID="notification1"
        val CHANNELNAME = "Start Check Notification"
        if(Build.VERSION.SDK_INT >= 26){
            var notificationChannel = NotificationChannel(
                CHANNELID,CHANNELNAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            var manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(this,CHANNELID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("알림시작")
            .setContentText("지도 알림을 시작합니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)

        // 푸시 누르면 액티비티로 전환 하도록
        val intent = Intent(this,MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // 화면 전환 intent

        val user = FirebaseAuth.getInstance().currentUser
        intent.putExtra("ID",user?.email.toString())
        // id 넘겨줘야함
        val pendingIntent = PendingIntent.getActivity(this,
            1,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        val NOTIFICATION_ID = 1

        startForeground(NOTIFICATION_ID, notification)
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
        var isVibrate=false
        for(i in 0..data.size - 1) {
            data.get(i) // MyData(lat,lng);
            Location.distanceBetween(loc.latitude,loc.longitude,data.get(i).lat.toDouble(), data.get(i).long.toDouble(),temp)
            if(temp[0]<50){
                //알람
                if(data.get(i).isNearby==false) {
                    data.get(i).isNearby=true
                   if(!isVibrate) {
                       val CHANNELID = "notification2"
                       val notificationChannel = NotificationChannel( // 알람등 설정 가능한 channel 설정
                           CHANNELID,
                           "Map Check Notification",
                           NotificationManager.IMPORTANCE_DEFAULT
                       )
                       notificationChannel.enableVibration(true)
                       notificationChannel.vibrationPattern = longArrayOf(100, 200)

                       // 화면꺼진상태에선 on 상태여도 알림 x

                       val builder = Notification.Builder(this, CHANNELID)
                       builder.setContentTitle("위험!")
                       builder.setContentText("주변에 횡단보도 감지 주의하세요!")
                           .setSmallIcon(R.drawable.logo)

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
                       isVibrate=true
                   }
                }

            }else data.get(i).isNearby = false

        } // 확인누르면 알림끝
    }
}