package com.ewoo.runaround;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.daum.mf.map.api.MapPoint;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service implements TextToSpeech.OnInitListener{
    private final String TAG = "MainService";

    private final MainBinder mainBinder = new MainBinder();

    private Context mContext;
    private MainActivity mainActivity;

    private SharedPreferenceManager pref;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    public TextToSpeech mTTS;

    private Location startLocation;
    private Location destLocation;
    private ArrayList<Location> courseList;
    private ArrayList<Location> checkPointList;

    private Timer runTimer;
    private TimerTask runTimerTask;

    private Timer timeAttackTimer;
    private TimerTask timeAttackTimerTask;

    private Location curLocation;

    private final int DEFAULT_COUNT = 10;
    private int nCountDown = DEFAULT_COUNT;

    int cusCourse = 0;
    int cusCheckPoint = 0;

    boolean isDest = false;
    boolean isNoti = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate !!");

        courseList = new ArrayList<>();
        checkPointList = new ArrayList<>();

        initTTS();
        initLocationManager();
    }

    public void setContext(Context context) {
        Log.d(TAG, "setContext !!");
        mContext = context;
        mainActivity = (MainActivity)mContext;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind !!");
        return mainBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand !!");

        registerLocationUpdates();

        return Service.START_STICKY;
    }

    public class MainBinder extends Binder {
        public MainService getService() { return MainService.this; }
        public void setContext(Context context) {
            mContext = context;
            mainActivity = (MainActivity) mContext;

            // ???????????? ????????? ????????????
            pref = SharedPreferenceManager.getInstance(mContext);
        }
    }

    @Override
    public void onInit(int i) {

    }

    // NOTE : TTS ?????????
    public void initTTS() {
        mTTS = new TextToSpeech(getApplicationContext(), this);
    }

    // NOTE : TTS ?????? ??????
    public void speech(String msg, int type) {
        // TODO Auto-generated method stub
        Log.d(TAG, "TTS speech msg : " + msg);

        if (mTTS != null) {
            mTTS.setLanguage(Locale.KOREA);

            if (type == 0) {
                Bundle bundle = new Bundle();
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                mTTS.speak(msg, TextToSpeech.QUEUE_FLUSH, bundle, null);
            } else {
                Bundle bundle = new Bundle();
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0");
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
                mTTS.speak(msg, TextToSpeech.QUEUE_ADD, bundle, null);
            }
        } else {
            return;
        }
    }

    // NOTE : ???????????? ????????? ?????????
    public void initLocationManager() {
        Log.d(TAG, "initLocationManager !!");
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Log.d(TAG, "gps ?????? ?????? " + "provider [" + location.getProvider() + "] lon [" + location.getLongitude() +"] lat [" + location.getLatitude() + "]");
                curLocation = location;
                CommonDefine.lastLocation = location;
            }

            public void onProviderDisabled (String provider){
                Log.d(TAG, "onProviderDisabled [" + provider + "] gps ?????? ??????");
            }

            public void onProviderEnabled (String provider){
                registerLocationUpdates();
                Log.d(TAG, "onProviderEnabled [" + provider + "] gps ?????? ??????");
            }

            public void onStatusChanged (String provider,int status, Bundle extras){
                Log.d(TAG, "onStatusChanged [" + provider + "]");
            }
        };
    }

    // NOTE : ?????? ???????????? ??????
    public void registerLocationUpdates(){
        if (mLocationManager == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);

            String locationProvider = LocationManager.NETWORK_PROVIDER;
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation != null) {

                if ((lastKnownLocation.getLongitude() < 130.0 && lastKnownLocation.getLongitude() > 124.0)
                        && (lastKnownLocation.getLatitude() < 38.0 && lastKnownLocation.getLatitude() > 33.0)) {
                    //TODO : ????????? Location ?????? ??????????????? ????????????
                    CommonDefine.lastLocation = lastKnownLocation;
                }
            }

            Log.d(TAG,  "registerLocationUpdates()");
        } else {
            Log.d(TAG, "not registerLocationUpdates()");
        }
    }

    public void startRun(MapPoint startPoint,
                         MapPoint destPoint,
                         ArrayList<MapPoint> courseList,
                         ArrayList<MapPoint> checkPointList,
                         boolean isTimeAttack) {
        startLocation = new Location(LocationManager.GPS_PROVIDER);
        startLocation.setLatitude(startPoint.getMapPointGeoCoord().latitude);
        startLocation.setLongitude(startPoint.getMapPointGeoCoord().longitude);

        destLocation = new Location(LocationManager.GPS_PROVIDER);
        destLocation.setLatitude(destPoint.getMapPointGeoCoord().latitude);
        destLocation.setLongitude(destPoint.getMapPointGeoCoord().longitude);

        for (MapPoint mapPoint : courseList) {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(mapPoint.getMapPointGeoCoord().latitude);
            location.setLongitude(mapPoint.getMapPointGeoCoord().longitude);
            this.courseList.add(location);
        }

        for (MapPoint mapPoint : checkPointList) {
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(mapPoint.getMapPointGeoCoord().latitude);
            location.setLongitude(mapPoint.getMapPointGeoCoord().longitude);
            this.checkPointList.add(location);
        }

        speech("???????????? ???????????????", TextToSpeech.QUEUE_FLUSH);

        if (isTimeAttack) {
            startTimeAttackTimer();
        } else {
            startRunTimer();
        }


        //guideNextCourse();
    }

    public void stopRun() {
        KillTimer(runTimer, runTimerTask);
        KillTimer(timeAttackTimer, timeAttackTimerTask);
        nCountDown = DEFAULT_COUNT;

        isDest = false;
        isNoti = false;

        courseList.clear();
        checkPointList.clear();
        cusCheckPoint = 0;
        cusCourse = 0;
    }

    public void startRunTimer() {
        timeAttackTimer = new Timer();
        timeAttackTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (curLocation != null) {
                    nCountDown = 0;
                    mainActivity.moveMapPosition(curLocation);
                    mainActivity.setCount(++nCountDown);

                    if (!isDest) {
                        if (checkPointList.isEmpty()) {
                            isDest = true;
                        } else {
                            if (curLocation.distanceTo(checkPointList.get(cusCheckPoint)) < 20.0f) {
                                if (!isNoti) {
                                    speech("?????????????????? ?????????????????????. " + nCountDown + "??? ?????????????????????.", TextToSpeech.QUEUE_FLUSH);
                                    isNoti = true;

                                    if (cusCheckPoint + 1 == checkPointList.size()) {
                                        // ?????????
                                        isDest = true;
                                    } else {
                                        // ?????? ???????????????
                                        cusCheckPoint++;
                                    }
                                    isNoti = false;
                                }
                            }
                        }
                    } else {
                        if (curLocation.distanceTo(destLocation) < 20.0f) {
                            if (!isNoti) {
                                speech("???????????? ?????????????????????. ?????????????????????. ??? " + nCountDown + "??? ?????????????????????.", TextToSpeech.QUEUE_FLUSH);
                                mainActivity.stopRun();
                            }
                        }
                    }

                }
            }
        };
        timeAttackTimer.schedule(timeAttackTimerTask, 0, 1000);
    }

    public void startTimeAttackTimer() {
        runTimer = new Timer();
        runTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (curLocation != null) {
                    nCountDown = DEFAULT_COUNT;
                    mainActivity.moveMapPosition(curLocation);
                    mainActivity.setCount(--nCountDown);

                    if (!isDest) {
                        if (checkPointList.isEmpty()) {
                            isDest = true;
                        } else {
                            if (curLocation.distanceTo(checkPointList.get(cusCheckPoint)) > 20.0f) {
                                if (nCountDown < 5) {
                                    if (!isNoti) {
                                        speech("5??? ???????????????.", TextToSpeech.QUEUE_FLUSH);
                                        isNoti = true;
                                    }
                                }
                            } else {
                                speech("?????????????????? ?????????????????????.", TextToSpeech.QUEUE_FLUSH);
                                nCountDown = DEFAULT_COUNT;
                                isNoti = false;

                                if (cusCheckPoint + 1 == checkPointList.size()) {
                                    // ?????????
                                    isDest = true;
                                } else {
                                    // ?????? ???????????????
                                    cusCheckPoint++;
                                }
                            }
                        }
                    } else {
                        if (curLocation.distanceTo(destLocation) > 20.0f) {
                            if (nCountDown < 5) {
                                if (!isNoti) {
                                    speech("5??? ???????????????.", TextToSpeech.QUEUE_FLUSH);
                                    isNoti = true;
                                }
                            }
                        } else {
                            speech("???????????? ?????????????????????. ?????????????????????.", TextToSpeech.QUEUE_FLUSH);
                            mainActivity.stopRun();
                        }
                    }

                }
            }
        };
        runTimer.schedule(runTimerTask, 0, 1000);
    }

    private void KillTimer(Timer t, TimerTask tk) {
        if (t != null) {
            t.cancel();
        }

        if (tk != null) {
            tk.cancel();
        }
    }

//    public void guideNextCourse() {
//        String uri = "kakaomap://route?sp=" +
//                startPoint.getMapPointGeoCoord().latitude + "," +
//                startPoint.getMapPointGeoCoord().longitude + "&ep=" +
//                destPoint.getMapPointGeoCoord().latitude + "," +
//                destPoint.getMapPointGeoCoord().longitude + "&by=FOOT";
//        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//        if (intent != null) {
//            startActivity(intent);
//        } else {
//
//        }
//    }
}
