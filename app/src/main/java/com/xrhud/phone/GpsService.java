package com.xrhud.phone;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GpsService extends Service {

    private static final String TAG = "XRHUD_GPS";

    public static final String EXTRA_DECK_IP = "deck_ip";

    private static final int PORT = 8676;
    private static final String CHANNEL_ID = "xrhud_gps";

    private LocationManager locationManager;
    private DatagramSocket socket;
    private InetAddress deckAddress;

    private final ExecutorService networkExecutor =
            Executors.newSingleThreadExecutor();

    private volatile int satellites = 0;

    private final LocationListener locationListener =
            new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG,
                    "GPS fix: lat=" + location.getLatitude()
                    + " lon=" + location.getLongitude()
                    + " speed=" + location.getSpeed());

            sendLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS provider enabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "GPS provider disabled: " + provider);
        }
    };

    private final GnssStatus.Callback gnssCallback =
            new GnssStatus.Callback() {

        @Override
        public void onSatelliteStatusChanged(
                GnssStatus status) {

            int count = 0;

            for (int i = 0;
                 i < status.getSatelliteCount();
                 i++) {

                if (status.usedInFix(i)) {
                    count++;
                }
            }

            satellites = count;

            Log.d(TAG, "Satellites used in fix: " + satellites);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "XRHUD GPS service starting");

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("XRHUD GPS")
                        .setContentText("Sending GPS data to HUD")
                        .setSmallIcon(
                                android.R.drawable.ic_menu_mylocation)
                        .setOngoing(true)
                        .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    1,
                    notification,
                    android.content.pm.ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_LOCATION
            );
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        String ip = intent.getStringExtra(EXTRA_DECK_IP);

        Log.d(TAG, "Target IP from UI: " + ip);

        if (ip == null || ip.trim().isEmpty()) {
            Log.e(TAG, "No target IP supplied");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            deckAddress = InetAddress.getByName(ip.trim());

            Log.d(TAG,
                    "Resolved target: "
                    + deckAddress.getHostAddress()
                    + ":" + PORT);

            socket = new DatagramSocket();

            Log.d(TAG,
                    "UDP socket created on local port "
                    + socket.getLocalPort());

        } catch (Exception e) {

            Log.e(TAG,
                    "Failed to create UDP socket/address",
                    e);

            stopSelf();
            return START_NOT_STICKY;
        }

        startLocation();

        return START_STICKY;
    }

    private void startLocation() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            Log.e(TAG, "Fine location permission missing");
            stopSelf();
            return;
        }

        locationManager =
                (LocationManager)
                        getSystemService(
                                Context.LOCATION_SERVICE);

        try {

            locationManager.registerGnssStatusCallback(
                    gnssCallback);

            Log.d(TAG, "GNSS callback registered");

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500,
                    0.0f,
                    locationListener);

            Log.d(TAG, "GPS location updates requested");

        } catch (SecurityException e) {

            Log.e(TAG,
                    "Security exception starting GPS",
                    e);

            stopSelf();

        } catch (Exception e) {

            Log.e(TAG,
                    "Failed to start GPS",
                    e);

            stopSelf();
        }
    }

    private void sendLocation(Location location) {

        if (deckAddress == null) {
            Log.e(TAG, "No destination address");
            return;
        }

        if (socket == null || socket.isClosed()) {
            Log.e(TAG, "UDP socket is unavailable");
            return;
        }

        final double lat = location.getLatitude();
        final double lon = location.getLongitude();

        final float speed =
                location.hasSpeed()
                        ? location.getSpeed()
                        : 0.0f;

        final int sats = satellites;

        networkExecutor.execute(() -> {

            try {

                JSONObject json = new JSONObject();

                json.put("lat", lat);
                json.put("lon", lon);
                json.put("speed", speed);
                json.put("sats", sats);
                json.put("hdop", "-");

                byte[] data =
                        json.toString()
                                .getBytes(StandardCharsets.UTF_8);

                DatagramPacket packet =
                        new DatagramPacket(
                                data,
                                data.length,
                                deckAddress,
                                PORT);

                Log.d(TAG,
                        "UDP SEND -> "
                        + deckAddress.getHostAddress()
                        + ":" + PORT
                        + " "
                        + json);

                socket.send(packet);

                Log.d(TAG,
                        "UDP SEND OK (" + data.length
                        + " bytes)");

            } catch (IOException e) {

                Log.e(TAG,
                        "UDP SEND FAILED",
                        e);

            } catch (Exception e) {

                Log.e(TAG,
                        "Failed creating/sending GPS packet",
                        e);
            }
        });
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "XRHUD GPS service stopping");

        if (locationManager != null) {

            try {
                locationManager.removeUpdates(
                        locationListener);
            } catch (Exception ignored) {}

            try {
                locationManager.unregisterGnssStatusCallback(
                        gnssCallback);
            } catch (Exception ignored) {}
        }

        networkExecutor.shutdownNow();

        if (socket != null) {
            socket.close();
            socket = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "XRHUD GPS",
                        NotificationManager.IMPORTANCE_LOW);

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
