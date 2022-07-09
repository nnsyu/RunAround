package com.ewoo.runaround;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView;

import java.util.ArrayList;

public class MainFragment extends Fragment implements MainActivity.MainCallback, MapView.MapViewEventListener, View.OnClickListener{
    private final String TAG = "MainFragment";

    MainActivity mainActivity;

    private View mView;
    private MapView mMapView;
    private ViewGroup mapLayout;

    private Button
            btnStart,
            btnDest,
            btnAddCourse,
            btnRemoveCourse,
            btnCurPos,
            btnCheckPoint,
            btnStartRun,
            btnTimeAttack;

    private TextView tvCount;

    private MapPoint curPoint;

    private MapPoint startPoint;
    private MapPoint destPoint;

    private MapPOIItem startMarker;
    private MapPOIItem destMarker;

    private MapPolyline polyline;

    private ArrayList<MapPoint> pointList; // 코스만 포함됨
    private ArrayList<MapPOIItem> courseList;

    private ArrayList<MapPoint> checkPointList;

    boolean isRun = false;

    boolean isTimeAttack = false;

    public MainFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mainActivity = (MainActivity) getContext();
        mainActivity.setCallback(this);

        courseList = new ArrayList<>();
        pointList = new ArrayList<>();
        checkPointList = new ArrayList<>();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container == null) return null;

        mView = inflater.inflate(R.layout.fragment_main, null);

        mMapView = new MapView(getContext());
        mMapView.setMapViewEventListener(this);

        findViewById();

        return mView;
    }

    public void findViewById() {
        mapLayout = mView.findViewById(R.id.map_view);
        mapLayout.addView(mMapView);

        btnStart = mView.findViewById(R.id.f_main_btn_start);
        btnDest = mView.findViewById(R.id.f_main_btn_dest);
        btnAddCourse = mView.findViewById(R.id.f_main_btn_add_course);
        btnRemoveCourse = mView.findViewById(R.id.f_main_btn_remove_course);
        btnCurPos = mView.findViewById(R.id.f_main_btn_cur_pos);
        btnCheckPoint = mView.findViewById(R.id.f_main_btn_check_point);
        btnStartRun = mView.findViewById(R.id.f_main_btn_start_run);
        btnTimeAttack = mView.findViewById(R.id.f_main_btn_time_attack);

        tvCount = mView.findViewById(R.id.f_main_tv_count);

        btnStart.setOnClickListener(this);
        btnDest.setOnClickListener(this);
        btnAddCourse.setOnClickListener(this);
        btnRemoveCourse.setOnClickListener(this);
        btnCurPos.setOnClickListener(this);
        btnCheckPoint.setOnClickListener(this);
        btnStartRun.setOnClickListener(this);
        btnTimeAttack.setOnClickListener(this);

    }

    @Override
    public void onLocationRoaded(Location location) {
        Log.d(TAG, "onLocationRoaded !!");
    }

    @Override
    public void moveMapPosition(Location location) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mMapView.moveCamera(CameraUpdateFactory.newMapPoint(
                        MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude())));
            }
        });

    }

    @Override
    public void setCount(int count) {
        int minute = count / 60;
        final int[] second = {count % 60};

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                if (second[0] < 0) {
                    second[0] *= -1;
                    tvCount.setTextColor(getResources().getColor(R.color.red));
                    tvCount.setText("+" + minute + ":" + String.format("%02d", second[0]));
                } else {
                    tvCount.setTextColor(getResources().getColor(R.color.black));
                    tvCount.setText(minute + ":" + String.format("%02d", second[0]));
                }
            }
        });
    }

    @Override
    public void stopRun() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (isTimeAttack) {
                    btnTimeAttack.performClick();
                } else {
                    btnStartRun.performClick();
                }
            }
        });
        MainActivity.mainService.stopRun();
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {
        Log.d(TAG, "onMapViewInitialized !!");

        if (CommonDefine.lastLocation != null) {
            mMapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(CommonDefine.lastLocation.getLatitude(),
                    CommonDefine.lastLocation.getLongitude()), true);

            MapPOIItem customMarker = new MapPOIItem();
            customMarker.setItemName("출발지");
            customMarker.setTag(1);
            customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(CommonDefine.lastLocation.getLatitude(), CommonDefine.lastLocation.getLongitude()));
            customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
            customMarker.setCustomImageResourceId(R.drawable.pin_start2); // 마커 이미지.
            customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
            customMarker.setCustomImageAnchor(0.0f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

            mapView.addPOIItem(customMarker);
            startMarker = customMarker;
            startPoint = startMarker.getMapPoint();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
            case R.id.f_main_btn_start:
                if (curPoint != null) {
                    if (startMarker != null) {
                        mMapView.removePOIItem(startMarker);
                    }

                    MapPOIItem customMarker = new MapPOIItem();
                    customMarker.setItemName("출발지");
                    customMarker.setTag(1);
                    customMarker.setMapPoint(curPoint);
                    customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                    customMarker.setCustomImageResourceId(R.drawable.pin_start2); // 마커 이미지.
                    customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                    customMarker.setCustomImageAnchor(0.2f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

                    mMapView.addPOIItem(customMarker);

                    startMarker = customMarker;
                    startPoint = curPoint;

                    drawPolyLine(pointList);
                }
                break;

            case R.id.f_main_btn_dest:
                if (curPoint != null) {
                    if (destMarker != null) {
                        mMapView.removePOIItem(destMarker);
                    }

                    MapPOIItem customMarker = new MapPOIItem();
                    customMarker.setItemName("목적지");
                    customMarker.setTag(1);
                    customMarker.setMapPoint(curPoint);
                    customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                    customMarker.setCustomImageResourceId(R.drawable.pin_end); // 마커 이미지.
                    customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                    customMarker.setCustomImageAnchor(0.2f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

                    mMapView.addPOIItem(customMarker);

                    destMarker = customMarker;
                    destPoint = curPoint;

                    drawPolyLine(pointList);

                    MapPointBounds mapPointBounds = new MapPointBounds(polyline.getMapPoints());
                    int padding = 100; // px
                    mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding));
                }
                break;

            case R.id.f_main_btn_add_course: {
                MapPOIItem customMarker = new MapPOIItem();
                customMarker.setItemName("코스" + (courseList.size() + 1));
                customMarker.setTag(1);
                customMarker.setMapPoint(curPoint);
                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                customMarker.setCustomImageResourceId(R.drawable.pin_course); // 마커 이미지.
                customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                customMarker.setCustomImageAnchor(0.5f, 0.5f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

                courseList.add(customMarker);
                pointList.add(curPoint);

                mMapView.addPOIItem(customMarker);

                drawPolyLine(pointList);
                }
                break;

            case R.id.f_main_btn_remove_course:
                if (courseList.size() > 0) {
                    mMapView.removePOIItem(courseList.get(courseList.size()-1));
                    courseList.remove(courseList.size()-1);
                    pointList.remove(pointList.size()-1);

                    drawPolyLine(pointList);
                }
                break;

            case R.id.f_main_btn_cur_pos:
                if (CommonDefine.lastLocation != null) {
                    mMapView.moveCamera(CameraUpdateFactory.newMapPoint(MapPoint.mapPointWithGeoCoord(CommonDefine.lastLocation.getLatitude(),
                            CommonDefine.lastLocation.getLongitude())));
                }
                break;

            case R.id.f_main_btn_check_point: {
                MapPOIItem customMarker = new MapPOIItem();
                customMarker.setItemName("체크포인트" + (checkPointList.size() + 1));
                customMarker.setTag(1);
                customMarker.setMapPoint(curPoint);
                customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
                customMarker.setCustomImageResourceId(R.drawable.pin_check); // 마커 이미지.
                customMarker.setCustomImageAutoscale(false); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
                customMarker.setCustomImageAnchor(0.5f, 0.5f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.

                courseList.add(customMarker);
                pointList.add(curPoint);
                checkPointList.add(curPoint);

                mMapView.addPOIItem(customMarker);

                drawPolyLine(pointList);
                }
                break;

            case R.id.f_main_btn_start_run:
                isTimeAttack = false;

                if (!isRun) {
                    if (destPoint != null) {
                        btnStartRun.setText("STOP RUN");
                        isRun = true;
                        MainActivity.mainService.startRun(startPoint, destPoint, pointList, checkPointList, false);
                        tvCount.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(mainActivity, "목적지를 설정해주세요 !", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    btnStartRun.setText("START RUN");
                    isRun = false;
                    MainActivity.mainService.stopRun();
                    tvCount.setVisibility(View.GONE);
                }
                break;

            case R.id.f_main_btn_time_attack:
                isTimeAttack = true;

                if (!isRun) {
                    if (destPoint != null) {
                        btnTimeAttack.setText("STOP RUN");
                        isRun = true;
                        MainActivity.mainService.startRun(startPoint, destPoint, pointList, checkPointList, true);
                        tvCount.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(mainActivity, "목적지를 설정해주세요 !", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    btnTimeAttack.setText("START TIME ATTACK");
                    isRun = false;
                    MainActivity.mainService.stopRun();
                    tvCount.setVisibility(View.GONE);
                }
                break;
        }
    }

    public void drawPolyLine(ArrayList<MapPoint> mapPoints) {
        mMapView.removeAllPolylines();

        polyline = new MapPolyline();
        //polyline.setTag(1000);
        polyline.setLineColor(Color.argb(128, 91, 235, 51)); // Polyline 컬러 지정.

        // 출발지점은 무조건
        polyline.addPoint(startPoint);

        for (MapPoint point : mapPoints) {
            polyline.addPoint(point);
        }

        if (destMarker != null) {
            polyline.addPoint(destPoint);
        }

        mMapView.addPolyline(polyline);

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewCenterPointMoved !!");
    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {
        Log.d(TAG, "onMapViewZoomLevelChanged !!");
    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewSingleTapped !!");
    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewDoubleTapped !!");

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewLongPressed !!");

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewDragStarted !!");

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewDragEnded !!");

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {
        Log.d(TAG, "onMapViewMoveFinished !!");

        curPoint = mapPoint;
    }
}
