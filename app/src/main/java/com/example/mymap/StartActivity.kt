package com.example.mymap

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {

    val RC_SIGN_IN=123
    val myDbHelper: MyDBHelper = MyDBHelper(this)
    val GPS_REQUEST=1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        init()
        // 1. id를 받아오기 2. true false 받아오기
    }
    fun checkInternet():Boolean{
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true
                    }
                }
            } else {
                try {
                    val activeNetworkInfo =
                        connectivityManager.activeNetworkInfo
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                        return true
                    }
                } catch (e: Exception) {
                }
            }
        }
        return false
    }
    fun init(){
        val pref = getSharedPreferences("checkFirst", Activity.MODE_PRIVATE)
        val first=pref.getBoolean("checkFirst",true)//(키 값, 디폴트값 : 첫실행때 갖는값)
        if(first) {
                if(!checkInternet()){
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage("인터넷 연결 상태를 확인해주세요")
                        .setTitle("인터넷 연결 오류")//.setIcon(
                    builder.setPositiveButton("확인") { _, _ ->
                        finishAffinity()
                    }
                    val dialog = builder.create()
                    dialog.show()

                }else {
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage("이 앱을 사용하기 위한 구성요소 다운 (1분이내 소요)")
                        .setTitle("구성요소 다운로드")//.setIcon(
                    builder.setPositiveButton("확인") { _, _ ->
                        val editor = pref.edit()
                        myDbHelper.deleteAll()//일단 다지우고 다시삽입 ( 중간부터 삽입가능하려면 중간지점을 알아야함)
                        myDbHelper.makeData(editor)
                    }
                    val dialog = builder.create()
                    dialog.show()


                }

        }
        start.setOnClickListener {
            login()
        }
    }

    fun login(){
        val user = FirebaseAuth.getInstance().currentUser
        if(user!=null){//로그인된경우 바로 메인화면
            val i = Intent(this, MainActivity::class.java) //IntentB.class : 호출할 class 이름
            i.putExtra("ID",user.email)
            if(user?.email == "admin@konkuk.ac.kr") {
                var admin = Intent(this, AdminActivity::class.java)
                admin.putExtra("ID", user?.email)
                startActivity(admin)
            } else
                startActivity(i)
        } else createSignInIntent()//아니면 로그인화면
    }
    fun createSignInIntent(){
        val provider= arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(provider)
                .setIsSmartLockEnabled(false). //smartLock이 뭘까
                    build(),
            RC_SIGN_IN) // 로그인 하는 파이어베이스 제공 activity 시작
//      .setLogo(R.drawable.my_great_logo) // Set logo drawable
//                .setTheme(R.style.MySuperAppTheme) // Set theme

    }

}
