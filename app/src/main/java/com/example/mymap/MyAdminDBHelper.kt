package com.example.mymap

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import com.example.mymap.MyAdminDBHelper.DataEntry.Companion.TABLE_NAME


class MyAdminDBHelper(val context: Context) : SQLiteOpenHelper(context,
    MyDBHelper.DATABASE_NAME, null,
    MyDBHelper.DATABASE_VERSION
)  {
    var list:ArrayList<MyData> = ArrayList<MyData>()
    val MAX=32133//최대값

    companion object {
        val DATABASE_VERSION = 1
        val DATABASE_NAME = "DataDB.db"
        val SQL_CREATE_ENTRIES = "CREATE TABLE " + DataEntry.TABLE_NAME + " (" +
                BaseColumns._COUNT + " INTEGER PRIMARY KEY," + //string to integer 해주기
                DataEntry.LAT + " TEXT," +
                DataEntry.LNG + " TEXT," +
                DataEntry.CHECK_CW + " INTEGER," +
                DataEntry.CHECK_TL + " INTEGER," +
                DataEntry.CHECK_MORE2 + " INTEGER," +
                DataEntry.COMMENT + " TEXT )"
    }

    class DataEntry : BaseColumns { //테이블 형식
        companion object {
            val TABLE_NAME = "UserFeedbackCrosswalk"
            val LAT = "lat"
            val LNG = "lng"
            val CHECK_CW = "checkCW"
            val CHECK_TL = "checkTL"
            val CHECK_MORE2 = "checkMore2"
            val COMMENT = "comment"
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //db 버전 바꼈을때 실행
    }

    fun addAdminData(data:adminData){
        val db= writableDatabase
        val values = ContentValues()
        values.put(DataEntry.LAT,data.lat)
        values.put(DataEntry.LNG,data.long)
        values.put(DataEntry.CHECK_CW,data.checkCW)
        values.put(DataEntry.CHECK_TL,data.checkTL)
        values.put(DataEntry.CHECK_MORE2,data.checkmore2)
        values.put(DataEntry.COMMENT,data.comment)
        db.insert(DataEntry.TABLE_NAME, null, values)
    }

    fun deleteAll(){
        val db = writableDatabase
        db.execSQL("delete from " + TABLE_NAME)
    }

    fun loadDataAdmin(): ArrayList<adminData> {
        val DataArrayList = ArrayList<adminData>()
        val db = readableDatabase

        val projection = arrayOf(
            //   DataEntry.OBJECTID,
            DataEntry.LAT,
            DataEntry.LNG,
            DataEntry.CHECK_CW,
            DataEntry.CHECK_TL,
            DataEntry.CHECK_MORE2,
            DataEntry.COMMENT
        )

        val cursor = db.query(DataEntry.TABLE_NAME,
            projection, null, null, null, null, null)

        while (cursor.moveToNext()) {
            val lat = cursor.getString(cursor.getColumnIndex(DataEntry.LAT))
            val lng = cursor.getString(cursor.getColumnIndex(DataEntry.LNG))
            val checkCW = cursor.getInt(cursor.getColumnIndex(DataEntry.CHECK_CW))
            val checkTL = cursor.getInt(cursor.getColumnIndex(DataEntry.CHECK_TL))
            val checkMore2 = cursor.getInt(cursor.getColumnIndex(DataEntry.CHECK_MORE2))
            val comment = cursor.getString(cursor.getColumnIndex(DataEntry.COMMENT))
            val data = adminData(lat, lng, checkCW,checkTL,checkMore2,comment)
            DataArrayList.add(data)
        }
        Log.e("총 개수",cursor.count.toString())
        return DataArrayList
    }
}