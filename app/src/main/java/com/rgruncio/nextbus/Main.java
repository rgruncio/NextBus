package com.rgruncio.nextbus;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;


public class Main extends AppCompatActivity {

    private BroadcastReceiver gpsBcastRcvr;
    private BroadcastReceiver sqlBcastRcvr;
    private BroadcastReceiver usrBcastRcvr;
    private BroadcastReceiver busStopRcvr;
    private BroadcastReceiver busInfoRcvr;
    private TextView textView;
    private TextView busStopNameTextView;
    private TextView lineNumberTextView;
    private TextView busTimeTextView;
    private LinearLayout infoLayout;
    private Context context;
    private Intent loginIntent = null;
    private Location actual;

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
                        actual.setLatitude(Double.parseDouble(latitude));
                        actual.setLongitude(Double.parseDouble(longitude));
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

        if (sqlBcastRcvr == null) {
            sqlBcastRcvr = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String sqlResponse;

                    sqlResponse = intent.getStringExtra(SQLService.RESPONSE);
                    String text = "DB Response:\n" + sqlResponse;
                    //responseTextView.setText(text);
                }
            };
        }
        registerReceiver(sqlBcastRcvr, new IntentFilter(SQLService.QUERY));
        registerReceiver(sqlBcastRcvr, new IntentFilter(SQLService.FIND_USER));

        if (busStopRcvr == null) {
            busStopRcvr = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String sqlResponse;

                    sqlResponse = intent.getStringExtra(SQLService.RESPONSE);

                    //busTextView.setText(sqlResponse);
                    try {
                        JSONArray json = new JSONArray(sqlResponse);

                        String id = json.getJSONObject(0).getString("ID");
                        String lat = json.getJSONObject(0).getString("Latitude");
                        String lon = json.getJSONObject(0).getString("Longitude");

                        Location busStopLocation = new Location(LOCATION_SERVICE);
                        busStopLocation.setLatitude(Double.parseDouble(lat));
                        busStopLocation.setLongitude(Double.parseDouble(lon));

                        if (actual.distanceTo(busStopLocation) < 15) {

                            textView.setVisibility(View.INVISIBLE);

                            DateFormat df = new SimpleDateFormat("EEEyyyyMMdd");
                            df.setTimeZone(TimeZone.getTimeZone("Europe/Warsaw"));
                            String date = df.format(Calendar.getInstance().getTime());
                            int typeOfDay = whichDay(date);

                            Intent _intent = new Intent(getApplicationContext(), SQLService.class);
                            _intent.putExtra("ID", id);
                            _intent.putExtra("TypeOfDay", typeOfDay);
                            _intent.putExtra(SQLService.QUERY, SQLService.GET_BUSSTOP_INFO);

                            startService(_intent);
                        }
                        else
                        {
                            infoLayout.setVisibility(View.INVISIBLE);
                            textView.setVisibility(View.VISIBLE);
                            textView.setText("Brak przystanku w odległości 15 metrów.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
        }
        registerReceiver(busStopRcvr, new IntentFilter(SQLService.FIND_NEAREST_BUS_STOP));

        if (busInfoRcvr == null) {
            busInfoRcvr = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String response = intent.getExtras().getString(SQLService.RESPONSE);

                    try {
                        JSONArray json = new JSONArray(response);
                        String busStopName = json.getJSONObject(0).getString("Name");
                        String lineNumber = json.getJSONObject(0).getString("Line_Number");
                        String time = json.getJSONObject(0).getString("TIME");

                        busStopNameTextView.setText(busStopName);
                        lineNumberTextView.setText(lineNumber);
                        busTimeTextView.setText(time);

                        infoLayout.setVisibility(View.VISIBLE);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            };
        }
        registerReceiver(busInfoRcvr, new IntentFilter(SQLService.GET_BUSSTOP_INFO));

        if (usrBcastRcvr == null) {
            usrBcastRcvr = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String login = intent.getStringExtra("login");
                    boolean correct = intent.getBooleanExtra("correct", false);

                    if (correct) {
                        TextView tvLogin = (TextView) findViewById(R.id.loginTextView);
                        tvLogin.setText(login);
                    } else {
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
        if (busStopRcvr != null) {
            unregisterReceiver(busStopRcvr);
        }
    }

    //Funkcja wywoływana przy uruchamianiu poraz pierwszy aplikacji (tworzenie UI i inicjalizacja obiektów)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        actual = new Location(LOCATION_SERVICE);
        context = this.getApplicationContext();

        textView = (TextView) findViewById(R.id.textView);

        busStopNameTextView = (TextView) findViewById(R.id.busNameTextView);
        lineNumberTextView = (TextView) findViewById(R.id.lineNumberTextView);
        busTimeTextView = (TextView) findViewById(R.id.busTimeTextView);
        infoLayout = (LinearLayout) findViewById(R.id.infoLayout);
        infoLayout.setVisibility(View.INVISIBLE);



        //loginIntent = new Intent(this, Login.class);
        //startActivity(loginIntent);

        //sprawdzamy czy aplikacja ma uprawnienia do korzystania z usługi GPS
        if (!runtime_permissions())
            enable_buttons();

    }

    //sprawdzanie jaki jest dzień: powszedni, sobota, czy święto.
    // 0 - dzień powszedni
    // 1 - sobota
    // 2 - święto

    // date - format: EEEyyyyMMdd
    private int whichDay(String date) {

        String dayName = date.substring(0, 3);
        int year = Integer.parseInt(date.substring(3, 7));
        int month = Integer.parseInt(date.substring(7, 9));
        int day = Integer.parseInt(date.substring(9, 11));

        switch (dayName) {
            case "Sun":
                return 2;
            default:
                switch (month) {
                    case 1:
                        if (day == 1 || day == 6) return 2;
                    case 5:
                        if (day == 1 || day == 3) return 2;
                    case 8:
                        if (day == 15) return 2;
                    case 11:
                        if (day == 1 || day == 11) return 2;
                    case 12:
                        if (day == 25 || day == 26) return 2;
                    default:
                        if (dayName.equals("Sat")) return 1;
                        else {
                            int A = 24;
                            int B = 5;
                            int a = year % 19;
                            int b = year % 4;
                            int c = year % 7;
                            int d = (a * 19 + A) % 30;
                            int e = (2 * b + 4 * c + 6 * d + B) % 7;
                            if (d == 29 && e == 6) d -= 7;
                            if (d == 28 && e == 6) d -= 7;
                            int easter = 22 + d + e;
                            int easterMonth = 3;
                            if (easter > 31) {
                                easterMonth++;
                                easter = easter % 31;
                            }
                            //Wielkanoc
                            if (day == easter && month == easterMonth) return 2;
                            //Poniedziałek Wielkanocny
                            if (day - 1 == easter && month == easterMonth) return 2;
                            //Zesłanie Ducha Swiętego (49 dni po Wielkanocy) to zawsze niedziela
                            //ten przypadek jest sprawdzany na początku
                            //Sprawdzamy datę Bożego Ciała (60 dni po Wielkanocy)
                            {
                                int dd = 0;
                                int lim;
                                while (dd < 60) {
                                    dd++;
                                    easter++;
                                    if (easterMonth == 4) lim = 30;
                                    else lim = 31;
                                    if (easter > lim) {
                                        easterMonth++;
                                        easter -= lim;
                                    }
                                }

                                if (day == easter && month == easterMonth)
                                    return 2;
                                else
                                    return 0;
                            }
                        }
                }
        }
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
