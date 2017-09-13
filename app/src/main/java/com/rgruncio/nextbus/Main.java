package com.rgruncio.nextbus;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;


public class Main extends AppCompatActivity {

    private BroadcastReceiver gpsBcastRcvr;
    private BroadcastReceiver sqlBcastRcvr;
    private BroadcastReceiver usrBcastRcvr;
    private TextView textView;
    private TextView responseTextView;
    private Context context;
    private Intent loginIntent = null;

    @Override
    protected void onResume() {
        super.onResume();


        //Rejestracja Receivera odbierającego informacje pochodzące z usługi GPS
        if (gpsBcastRcvr == null) {
            gpsBcastRcvr = new BroadcastReceiver() {

                //funkcja wywoływana w momencie odebrania danych pochodzących z usługi GPS
                @Override
                public void onReceive(Context context, Intent intent) {

                    //tymczasowo wypisuję otrzymane współrzędne dla celów testowych
                    String latitude = null, longitude = null;
                    try {
                        latitude = intent.getExtras().get("latitude").toString();
                        longitude = intent.getExtras().get("longitude").toString();
                        textView.setText("Device position:\nlongitude: " + longitude + "\nlatitude: " + latitude);
                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                    }

                    //uruchomienie usługi odpowiedzialnej za odpytywanie bazy danych
                    //i wyszukiwanie najbliższego przystanku
                    Intent sqlIntent = new Intent(getApplicationContext(), SQLService.class);

                    sqlIntent.putExtra(SQLService.LONGITUDE, longitude);
                    sqlIntent.putExtra(SQLService.LATITUDE, latitude);

                    sqlIntent.putExtra(SQLService.QUERY, SQLService.FIND_NEAREST_BUS_STOP);

                    startService(sqlIntent);

                }
            };
        }
        registerReceiver(gpsBcastRcvr, new IntentFilter(GPSService.INTENT_FILTER));

        if (sqlBcastRcvr == null){
            sqlBcastRcvr = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String sqlResponse;

                    sqlResponse = intent.getStringExtra(SQLService.RESPONSE);
                    String text = "DB Response:\n" + sqlResponse;
                    responseTextView.setText(text);
                }
            };
        }
        registerReceiver(sqlBcastRcvr, new IntentFilter(SQLService.QUERY));
        registerReceiver(sqlBcastRcvr, new IntentFilter(SQLService.FIND_USER));


        if (usrBcastRcvr == null){
            usrBcastRcvr = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String login = intent.getStringExtra("login");
                    boolean correct = intent.getBooleanExtra("correct", false);

                    if (correct) {
                        TextView tvLogin = (TextView) findViewById(R.id.loginTextView);
                        tvLogin.setText(login);
                    }
                    else
                    {
                        startActivity(loginIntent);
                    }
                }
            };
        }
        registerReceiver(usrBcastRcvr, new IntentFilter(Login.INTENT_FILTER));


    }


    //Zapobiegamy wyciekowi pamięci przy zamykaniu aplikacji
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gpsBcastRcvr != null) {
            unregisterReceiver(gpsBcastRcvr);
        }
        if (sqlBcastRcvr != null) {
            unregisterReceiver(sqlBcastRcvr);
        }
        if (usrBcastRcvr != null) {
            unregisterReceiver(usrBcastRcvr);
        }
    }

    //Funkcja wywoływana przy uruchamianiu poraz pierwszy aplikacji (tworzenie UI i inicjalizacja obiektów)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        context = this.getApplicationContext();

        textView = (TextView) findViewById(R.id.textView);

        responseTextView = (TextView) findViewById(R.id.resposeTextView);

        loginIntent = new Intent(this, Login.class);
        startActivity(loginIntent);

        //sprawdzamy czy aplikacja ma uprawnienia do korzystania z usługi GPS
        if (!runtime_permissions())
            enable_buttons();

    }

    //aktywacja widgetów
    private void enable_buttons() {
        ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //jeśli przycisk włączony - startujemy usługę GPSService
                if (isChecked) {

                    Intent i = new Intent(getApplicationContext(), GPSService.class);

                    //TODO: nadpisać tymczasowe przekazywanie lokalizacji testowej do usługi GPS

                    i.putExtra("check_distance", "check_distance");
                    i.putExtra("latitude", 1);
                    i.putExtra("longitude", 2);

                    //TODO: uzupełnić przekazywanie danych do usługi
                    startService(i);

                    Intent sqlIntent = new Intent(getApplicationContext(), SQLService.class);

                    sqlIntent.putExtra(SQLService.QUERY, SQLService.DO_NOTHING);

                    startService(sqlIntent);
                } else {
                    stopService(new Intent(getApplicationContext(), GPSService.class));
                }
            }
        });
    }

    //funkcja sprawdzająca uprawnienia do korzystania z GPS
    private boolean runtime_permissions() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }


    //funkcja pobierająca aktualne uprawnienia do usługi GPS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_buttons();
            } else {
                runtime_permissions();
            }
        }
    }


}
