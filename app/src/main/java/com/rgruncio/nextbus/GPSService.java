package com.rgruncio.nextbus;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;

public class GPSService extends Service {



    private LocationListener listener;
    private LocationManager locationManager;

    private String value;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //funkcja pobierająca parametry przekazywane przy wywoływaniu usługi
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //TODO: do uzupełnienia !!!
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //w momencie wykrycia zmiany lokalizacji urządzenia, usługa wysyła Intent z aktualnymi
                //danymi geolokalizacyjnymi
                Intent i = new Intent("location_update");
                i.putExtra("longitude", location.getLongitude());
                i.putExtra("latitude", location.getLatitude());
                sendBroadcast(i);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                //jeśli GPS w urządzeniu jest wyłączony, przenosimy użytkownika do menu systemowego,
                // w którym ma on możliwość włączyć usługę GPS
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //noinspection MissingPermission
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,0,listener);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null){
            //noinspection MissingPermission
            locationManager.removeUpdates(listener);
        }
    }
}
