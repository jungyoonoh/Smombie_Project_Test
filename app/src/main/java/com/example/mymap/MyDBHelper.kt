package com.example.mymap

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.AsyncTask
import android.provider.BaseColumns
import android.util.Log
import android.view.WindowManager
import com.example.mymap.MyDBHelper.DataEntry.Companion.TABLE_NAME
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.loading.*
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

class MyDBHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    // 기존 데이터 전용 (클러스터 x)
    val MAX=32133//최대값

    companion object {
        val DATABASE_VERSION = 1
        val DATABASE_NAME = "DataDB.db"
        val SQL_CREATE_ENTRIES = "CREATE TABLE " + DataEntry.TABLE_NAME + " (" +
                BaseColumns._COUNT + " INTEGER PRIMARY KEY," + // string to integer 해주기
                DataEntry.LAT + "  TEXT," +
                DataEntry.LNG + " TEXT )"
    }

    class DataEntry : BaseColumns { // 테이블 형식
        companion object {
            val TABLE_NAME = "Crosswalk"
            val LAT = "lat"
            val LNG = "lng"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //db 버전 바꼈을때 실행
    }

    // 서울시 공공데이터 받아오기
    fun makeData(editor: SharedPreferences.Editor){
        val db= writableDatabase
        var end:Int=0
        var arr:ArrayList<URL> = ArrayList()
        for(start in 1..MAX step 1000){ // until은 끝나는 쪽 포함 하지않음
            end=start+999
            if(end>32000) end=MAX
            arr.add(URL("http://openapi.seoul.go.kr:8088/65485243776b6f653731596e4b7178/json/SdeA004A1W/"+start+"/"+end+"/"))
        } // 파싱

        var task:DataTask= DataTask( object : DataTask.AsyncResponse{
            override fun processFinish() {
                editor.putBoolean("checkFirst", false)
                editor.commit() //저장 실행하는 함수
            }

        },db,context,(MAX/1000)+1)
        task.execute(arr)
    }

    fun deleteAll(){
        val db = writableDatabase
        db.execSQL("delete from " + TABLE_NAME)
    }

    // 사용자용 데이터 로드
    fun loadData(): ArrayList<MyData> {
        val DataArrayList = ArrayList<MyData>()
        val db = readableDatabase

        val projection = arrayOf(
            DataEntry.LAT,
            DataEntry.LNG
        )

        val cursor = db.query(DataEntry.TABLE_NAME,
            projection, null, null, null, null, null)

        while (cursor.moveToNext()) {
            val lat = cursor.getString(cursor.getColumnIndex(DataEntry.LAT))
            val lng = cursor.getString(cursor.getColumnIndex(DataEntry.LNG))
            val data = MyData(lat, lng)
            DataArrayList.add(data)
        }
        return DataArrayList
    }

    // admin용 데이터 로드
    fun loadAdminData() : ArrayList<adminData> {
        val DataArrayList = ArrayList<adminData>()
        val db = readableDatabase

        val projection = arrayOf(
            //   DataEntry.OBJECTID,
            DataEntry.LAT,
            DataEntry.LNG
        )

        val cursor = db.query(DataEntry.TABLE_NAME,
            projection, null, null, null, null, null)

        while (cursor.moveToNext()) {
            val lat = cursor.getString(cursor.getColumnIndex(DataEntry.LAT))
            val lng = cursor.getString(cursor.getColumnIndex(DataEntry.LNG))
            val data = adminData(lat, lng,1,1,1,"")
            DataArrayList.add(data)
        }
        return DataArrayList
    }

    // 작업
    class DataTask (asyncResponse: AsyncResponse,val db:SQLiteDatabase,val context: Context, val MAX:Int): AsyncTask<ArrayList<URL>, Void, Boolean>() {
        var ar: AsyncResponse? = asyncResponse
        lateinit var dlg: Dialog
        var count:Int=0
        interface AsyncResponse {
            fun processFinish()
        }

        fun loading(){
            dlg= Dialog(context) // 시작
            dlg.setContentView(R.layout.loading)
            dlg.setCancelable(false) // 다이얼로그 바깥클릭 or 나가기 불가능

            val lp = WindowManager.LayoutParams()
            lp.copyFrom(dlg.getWindow()?.getAttributes())
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            val window = dlg.getWindow()
            window?.setAttributes(lp)
            // dialog 사이즈
            dlg.show()
            // 중간에 종료시 1) 다 삭제후 다시 2) 중간지점 저장후 다시
        }
        fun parsingJson(params:URL) {
            var conn: HttpURLConnection
            var url: URL = params
            conn = url.openConnection() as HttpURLConnection
            var st: InputStream = BufferedInputStream(conn.inputStream)
            var br = BufferedReader(InputStreamReader(st, "UTF-8"))
            var str: String? = null
            var buffer: StringBuilder = StringBuilder()
            str = br.readLine()
            while ((str) != null) {
                buffer.append(str)
                str = br.readLine()
            }
            var result:String = buffer.toString()
            val parser = JsonParser()
            val json = parser.parse(result).asJsonObject
            val data = json.getAsJsonObject("SdeA004A1W").asJsonObject
            val array = data.getAsJsonArray("row").asJsonArray
            for (j in 0 until array.size()) {
                val lat = array[j].asJsonObject.get("LAT").asString
                val lng = array[j].asJsonObject.get("LNG").asString
                val values = ContentValues()
                values.put(DataEntry.LAT,lat)
                values.put(DataEntry.LNG,lng)

                db.insert(DataEntry.TABLE_NAME, null, values)

            }
            count++
            val percent=(count.toDouble()/MAX.toDouble()) * 100 // 강제 형변환 하면 계산이된 Int를 double 형태로 변환함

            dlg.progressBar.setProgress(percent.toInt())
        }

        override fun onPreExecute() {
            super.onPreExecute()
            loading()
        }

        // 전달된 url을 이용한 작업
        override fun doInBackground(vararg params: ArrayList<URL>):Boolean{
            var bool:Boolean=false
            var end=params.get(0).size
            for(i in 0 until end){
                parsingJson(params.get(0).get(i))
            }
            bool=true
            return bool
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            ar?.processFinish()
            dlg.dismiss()
        }
    }
}
