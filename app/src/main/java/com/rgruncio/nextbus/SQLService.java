package com.rgruncio.nextbus;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SQLService extends Service {

    public final static String QUERY = "query";

    public final static String FIND_USER = "FIND_USER";

    public final static String DO_NOTHING = "SELECT * FROM Przystanki";

    public final static String EMAIL = "email";

    public final static String PASSWORD = "password";

    public final static String RESPONSE = "response";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Odpytujemy bazę w zależności od żadania jakie przyszło w parametrze QUERY

        String _query = intent.getStringExtra(QUERY);

        if (_query.equals(FIND_USER)){
            String mEmail = intent.getStringExtra(EMAIL);

            String query = "SELECT * FROM USERS WHERE email='" + mEmail + "';";

            new _SQLQuery().execute(FIND_USER, query);
        }

        else {

            String sqlQuery = intent.getStringExtra("query");

            new _SQLQuery().execute(QUERY, sqlQuery);
        }

        return flags;
    }

    private class _SQLQuery extends AsyncTask<String, Void, String> {

        private String WHATIMDOING = null;

        //Adres pliku PHP po stronie serwera bazy danych, który odpytuje bazę
        private final String phpFile = "http://sundiamore.pl/nextbus/init.php";

        //funkcja działająca asynchronicznie (w tle), która tworzy połączenie z plikiem PHP i wysyła do niego zapytanie SQL
        //przekazywane jako parametr

        @Override
        protected String doInBackground(String... params) {

            WHATIMDOING = params[0];

            try {
                URL url = new URL(phpFile);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.connect();

                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                HashMap<String,String> sql = new HashMap<String, String>();
                sql.put("query", params[1]);
                out.write(getPostDataString(sql));
                out.close();

                int HttpResult = connection.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));

                    String line = null;
                    //sb - StringBuilder w którym zapisujemy wynik zapytania zwrócony przez serwer bazy danych
                    StringBuilder sb = new StringBuilder();

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }

                    br.close();

                    return sb.toString();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Funkcja realizująca zamianę par parametrów na jeden łańcuch znaków zgodny z formatem przesyłania
         * metodą POST
         *
         * @param params ciąg par łańcuchów
         * @return przetworzony łańcuch parametrów
         * @throws UnsupportedEncodingException
         */
        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for(Map.Entry<String, String> entry : params.entrySet()){
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {

            //Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
            sendIntent(WHATIMDOING, s);
        }

        /**
         * Funkcja rozgłaszająca Intent z zadanym parametrem, z filtrem <i>type</i>
         *
         * @param type rodzaj wykonanego zapytania
         * @param response łańcuch znaków do przesłania przez Intent
         */
        private void sendIntent(String type, String response){

            Intent i = new Intent(type);
            i.putExtra(RESPONSE, response);

            sendBroadcast(i);

        }
    }
}
