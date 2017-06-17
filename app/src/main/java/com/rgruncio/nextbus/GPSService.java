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

    public final static String INTENT_FILTER = "location_update";


    private LocationListener listener;
    private LocationManager locationManager;
    private Location givenLocation;

    private boolean CHECKING_DISTANCE_WANTED = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //funkcja pobierająca parametry przekazywane przy wywoływaniu usługi
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //jeśli usługa otrzymuje parametr "check_distance"
        //co oznacza, że chcemy sprawdzić odległość między aktualną
        //pozycją urządzenia a zadaną lokalizacją:
        if (intent.getExtras().containsKey("check_distance")){
            CHECKING_DISTANCE_WANTED = true;
            givenLocation = new Location("");
            givenLocation.setLatitude(intent.getDoubleExtra("latitude", 1000));
            givenLocation.setLongitude(intent.getDoubleExtra("longitude", 1000));
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //w momencie wykrycia zmiany lokalizacji urządzenia, usługa wysyła Intent z aktualnymi
                //danymi geolokalizacyjnymi
                Intent i = new Intent(INTENT_FILTER);
                i.putExtra("longitude", location.getLongitude());
                i.putExtra("latitude", location.getLatitude());

                //jeśli żądano sprawdzenia odległości od danej lokalizacji:
                /*if (CHECKING_DISTANCE_WANTED){
                    float distance = location.distanceTo(givenLocation);
                    i.putExtra("distance", distance);
                }*/

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
