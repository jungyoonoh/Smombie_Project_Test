package com.example.mymap

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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

    fun init(){
        val pref = getSharedPreferences("checkFirst", Activity.MODE_PRIVATE)
        val first=pref.getBoolean("checkFirst",true)//(키 값, 디폴트값 : 첫실행때 갖는값)
        if(first) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("이 앱을 사용하기 위한 구성요소 다운 (1분이내 소요)")
                .setTitle("구성요소 다운로드")//.setIcon(
            builder.setPositiveButton("확인") { _, _ ->
                val editor = pref.edit()
                myDbHelper.deleteAll()//일단 다지우고 다시삽입 ( 중간부터 삽입가능하려면 중간지점을 알아야함)
                myDbHelper.makeData(editor)
            }
            val dialog=builder.create()
            dialog.show()
        }
        initPermisson()
        start.setOnClickListener {
            login()
        }
    }

    fun login(){
        val user = FirebaseAuth.getInstance().currentUser
        if(user!=null){//로그인된경우 바로 메인화면
            val i = Intent(this, MainActivity::class.java) //IntentB.class : 호출할 class 이름
            i.putExtra("ID",user.email)
            if(user?.email == "xkakak142@gmail.com") {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {//로그인 최초 성공시
                val user = FirebaseAuth.getInstance().currentUser
                val i = Intent(this, MainActivity::class.java)
                i.putExtra("ID",user?.email)
                if(user?.email == "xkakak142@gmail.com"){
                    var admin = Intent(this,AdminActivity::class.java)
                    admin.putExtra("ID",user?.email)
                    startActivity(admin)
                } else
                    startActivity(i)
            } else {
                Toast.makeText(this,"로그인 실패", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun askPermisson(requestPermission:Array<String>,REQ_PERMISSON:Int){//요청하는 함수
        ActivityCompat.requestPermissions(this,requestPermission,REQ_PERMISSON)
    }
    fun checkAppPermission(requestPermission: Array<String>): Boolean { //false인 권한있는지 확인하는 함수

        val requestResult = BooleanArray(requestPermission. size)
        for (i in requestResult. indices ) {
            requestResult[i] = ContextCompat.checkSelfPermission(
                this,
                requestPermission[i]
            ) == PackageManager. PERMISSION_GRANTED
            if (!requestResult[i]) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GPS_REQUEST->{
                if(!checkAppPermission(permissions)){
                    Toast.makeText(applicationContext,"권한 승인안됨", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }


    fun initPermisson(){
        if(checkAppPermission(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION))){//권한 허용이 이미 돼있는 경우
            login()
        }else{//권한이없는경우
            val builder= AlertDialog.Builder(this)
            builder.setMessage("이 앱은 위치정보 권한이 반드시 필요합니다")
                .setTitle("권한 요청")//.setIcon(
            builder.setPositiveButton("확인"){ _,_->
                askPermisson(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),GPS_REQUEST)
            }
            val dialog=builder.create()
            dialog.show()

        }
    }


}
