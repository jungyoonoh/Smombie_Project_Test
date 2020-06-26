package com.example.mymap

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

// 위도,경도,횡단보도여부,신호등여부,2차선이상도로,코멘트 순
class adminData(var lat:String, var long:String, var checkCW:Int, var checkTL:Int, var checkmore2:Int, var comment:String):ClusterItem {
    override fun getPosition(): LatLng {
        var temp = LatLng(lat.toDouble(),long.toDouble())
        return temp
    }

}