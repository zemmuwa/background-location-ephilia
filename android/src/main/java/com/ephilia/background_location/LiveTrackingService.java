package com.ephilia.background_location;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class LiveTrackingService extends Service {
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private final String TAG = "LiveTrackingService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;

    private final int LOCATION_INTERVAL = 6000;
    private final int LOCATION_DISTANCE = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return (IBinder) binder;
    }

    private class LocationListener implements android.location.LocationListener {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
//            mLastLocation = location;
//            Log.d(TAG, "LocationChanged: "+location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + status);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        startForeground(12345678, getNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {
                Log.i(TAG, "fail to remove location listners, ignore", ex);
            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private Timer mTimer1;
    private TimerTask mTt1;
    private Handler mTimerHandler = new Handler();

    private void startTimer() {
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.M)
                    public void run() {
                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Gagal gps");
                            // TODO: Consider calling
                            //    Activity#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for Activity#requestPermissions for more details.
                            return;
                        }

                        final Location tes = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        mLocationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, null);
                        Log.d(TAG, "Location : " + String.valueOf(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)));

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        int defaultValue = 0;
                        final int highScore = sharedPref.getInt("UserID", defaultValue);
                        Log.d(TAG, "shared dpreferences : " + String.valueOf(highScore));

                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                        String url = sharedPref.getString("URL", defaultValue);
                        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>()
                                {
                                    @Override
                                    public void onResponse(String response) {
                                        // response
                                        Log.d("Response", response);
                                    }
                                },
                                new Response.ErrorListener()
                                {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // error
                                        Log.d("Error.Response", String.valueOf(error));
                                    }
                                }
                        ) {
                            @Override
                            protected Map<String, String> getParams()
                            {
                                Map<String, String>  params = new HashMap<String, String>();
                                params.put("id", String.valueOf(highScore));
                                params.put("long", String.valueOf(tes.getLongitude()));
                                params.put("lat", String.valueOf(tes.getLatitude()));

                                return params;
                            }
                        };
                        queue.add(postRequest);
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 300000, 300000);
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);
        Log.d(TAG, "Start Service");

        try {
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 5000, LOCATION_DISTANCE, mLocationListener );
//                mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, null);

        } catch (java.lang.SecurityException ex) {
             Log.d(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
             Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    public void stopTracking() {
        this.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private Notification getNotification() {
        startTracking();
        startTimer();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
            return builder.build();
//        }
    }


    public class LocationServiceBinder extends Binder {
        public LiveTrackingService getService() {
            return LiveTrackingService.this;
        }
    }



}