package com.rgruncio.nextbus;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Main extends AppCompatActivity {

    private BroadcastReceiver broadcastReceiver;
    private TextView textView;
    private ToggleButton button;


    @Override
    protected void onResume() {
        super.onResume();

        //Rejestracja Receivera odbierającego informacje pochodzące z usługi GPS
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {

                //funkcja wywoływana w momencie odebrania danych pochodzących z usługi GPS
                @Override
                public void onReceive(Context context, Intent intent) {


                    textView.setText("Współrzędne:\nlongitude: " + intent.getExtras().get("longitude").toString()
                            + "\nlatitude: " + intent.getExtras().get("latitude").toString());


                    //TODO: w tym miejscu będzie algorytm obsługujący wyszukiwanie najbliższego przystanku

                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }


    //Zapobiegamy wyciekowi pamięci przy zamykaniu aplikacji
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    //Funkcja wywoływana przy uruchamianiu poraz pierwszy aplikacji (tworzenie UI i inicjalizacja obiektów)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        textView = (TextView) findViewById(R.id.textView);

        //sprawdzamy czy aplikacja ma uprawnienia do korzystania z usługi GPS
        if(!runtime_permissions())
            enable_buttons();

    }

    //aktywacja widgetów
    private void enable_buttons() {
        button = (ToggleButton) findViewById(R.id.toggleButton);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //jeśli przycisk włączony - startujemy usługę GPSService
                if (isChecked){
                    Intent i = new Intent(getApplicationContext(), GPSService.class);
                    //TODO: uzupełnić przekazywanie danych do usługi
                    startService(i);
                }
                else {
                    stopService(new Intent(getApplicationContext(), GPSService.class));
                }
            }
        });
    }

    //funkcja sprawdzająca uprawnienia do korzystania z GPS
    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


    //funkcja pobierająca aktualne uprawnienia do usługi GPS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }
}
