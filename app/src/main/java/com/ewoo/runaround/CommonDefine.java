package com.ewoo.runaround;

import android.content.ServiceConnection;
import android.location.Location;
import android.util.Log;

import net.daum.mf.map.api.MapPoint;

public class CommonDefine {

    private static int      CURRENT_FRAGMENT_ID        = 0;
    public static final int FRAGMENT_MAIN              = 1;

    public static int getFragmentId() {
        return CURRENT_FRAGMENT_ID;
    }
    public static void setFragmentId(int id) {
        CURRENT_FRAGMENT_ID = id;
        Log.d("Fragment", "fragment id = " + id);
    }

    public static ServiceConnection main_conn;

    public static boolean firstLocationRoaded = false;

    public static Location lastLocation = null;
//    public static Location curLocation = null;
}
