package com.ewoo.runaround;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import net.daum.mf.map.api.MapPoint;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    boolean isGranted = false;

    public static MainService mainService;
    private Intent mainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        while (!isGranted) {
            isGranted = checkPermission();
        }

        startMainService();

        if (mainService != null) {
            mainService.setContext(this);
        }

        changeFragment(CommonDefine.FRAGMENT_MAIN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mainService = null;
    }

    // NOTE : 앱의 KeyHash 를 얻는 함수
    public static String getKeyHash(final Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            if (packageInfo == null)
                return null;

            for (Signature signature : packageInfo.signatures) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    return android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    // NOTE : 앱 사용에 필요한 권한을 체크하는 함수
    public boolean checkPermission() {
        boolean result = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                result = false;
            }
        }

        return result;
    }

    public interface MainCallback {
        void onLocationRoaded(Location location);
        void moveMapPosition(Location location);
        void setCount(int count);
        void stopRun();
    }

    private MainCallback callback;
    public void setCallback(MainCallback callback) {
        this.callback = callback;
    }

    public void onLocationRoaded(Location location) {
        if (callback != null) {
            callback.onLocationRoaded(location);
        }
    }

    public void moveMapPosition(Location location) {
        if (callback != null) {
            callback.moveMapPosition(location);
        }
    }

    public void setCount(int count) {
        if (callback != null) {
            callback.setCount(count);
        }
    }

    public void stopRun() {
        if (callback != null) {
            callback.stopRun();
        }
    }

    public void startMainService() {
        Log.d(TAG, "startMainService !!");

        if (mainIntent == null) {
            mainIntent = new Intent(this, MainService.class);
        }

        ComponentName cName = startService(mainIntent);
        CommonDefine.main_conn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MainService.MainBinder binder = ((MainService.MainBinder) service);
                mainService = binder.getService();
                binder.setContext(MainActivity.this);
                Log.d(TAG, "startMainService onServiceConnected !!");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected" + name);
                mainService = null;
            }

        };

        if (cName != null) { // ServiceConnectionLeaked
            Log.d(TAG, "startMainService bindService !!");
            bindService(mainIntent, CommonDefine.main_conn, Context.BIND_AUTO_CREATE);
        }
    }

    public void stopMainService() {
        Log.d(TAG, "stopMainService!!");

        Log.w("CommonDefine.conn", "" + CommonDefine.main_conn);

        try {
            if (CommonDefine.main_conn != null) {
                unbindService(CommonDefine.main_conn);
            }
            if (check()) {
                stopService(mainIntent);
            }

            mainIntent = null;
            mainService = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean check() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MainService.class.getName().equals(service.service.getClassName())) {
                Log.w("서비스구동중", "MainService 구동중");
                return true;
            }
        }
        return false;
    }

    // NOTE : 프래그먼트 전환 함수
    public void changeFragment(int id)
    {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        switch (id) {
            case CommonDefine.FRAGMENT_MAIN:
                ft.replace(R.id.layout_frame, new MainFragment());
                Toast.makeText(this, "메인이동", Toast.LENGTH_SHORT).show();
                break;
        }

        ft.commit();
        CommonDefine.setFragmentId(id);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                super.onBackPressed();
            } else {
                if (CommonDefine.getFragmentId() == CommonDefine.FRAGMENT_MAIN) {
                    stopMainService();
                    finish();
                } else {
                    changeFragment(CommonDefine.FRAGMENT_MAIN);
                }
            }
        }

        return false;
    }
}