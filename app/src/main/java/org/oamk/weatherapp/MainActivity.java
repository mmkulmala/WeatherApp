package org.oamk.weatherapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ramotion.foldingcell.FoldingCell;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    EditText editTextCityName;
    Button btnByCityName;
    TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextCityName = (EditText)findViewById(R.id.cityname);
        btnByCityName = (Button)findViewById(R.id.bycityname);
        textViewResult = (TextView)findViewById(R.id.result);

        // foldaaja solu
        final FoldingCell fc = (FoldingCell) findViewById(R.id.folding_cell);

        // liitetään kuuntelija soluun
        fc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // siivotaan tekstikentät
                editTextCityName.setText("");
                textViewResult.setText("");
                fc.toggle(false);
            }
        });

        // kuuntelija solun pienentävään nappiin
        final Button foldBtn = (Button) findViewById(R.id.fold_btn);
        foldBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fc.fold(false);
            }
        });

        // kuuntelija hakunappiin, jolla haetaan OpenWeatherMapistä tiedot
        btnByCityName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new OpenWeatherMapTask(
                        editTextCityName.getText().toString(),
                        textViewResult).execute();
            }
        });
    }

    private class OpenWeatherMapTask extends AsyncTask<Void, Void, String> {

        String cityName;
        TextView tvResult;

        // applikaatioavain, jota REST-pyynnöt tarvitsevat hakuihin OpenWeatherMapistä
        String appid = "some_appid_get_own_from_openweathermap";
        String queryWeather = "http://api.openweathermap.org/data/2.5/weather?q=";
        String queryDummyKey = "&appid=" + appid;

        OpenWeatherMapTask(String cityName, TextView tvResult){
            this.cityName = cityName;
            this.tvResult = tvResult;
        }

        @Override
        protected String doInBackground(Void... params) {
            String result = "";
            String queryReturn;

            String query = null;
            try {
                // itse kysely. Viimeinen parametri kertoo, että käytetään metristä järjestelmää
                query = queryWeather + URLEncoder.encode(cityName, "UTF-8") + queryDummyKey + "&units=metric";
                queryReturn = sendQuery(query);
                result += ParseJSON(queryReturn);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            tvResult.setText(s);
        }

        private String sendQuery(String query) throws IOException {
            String result = "";

            URL searchURL = new URL(query);

            HttpURLConnection httpURLConnection = (HttpURLConnection)searchURL.openConnection();
            if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(
                        inputStreamReader,
                        8192);

                String line = null;
                while((line = bufferedReader.readLine()) != null){
                    result += line;
                }

                bufferedReader.close();
            }

            return result;
        }

        private String ParseJSON(String json){
            String jsonResult = "";

            try {
                JSONObject JsonObject = new JSONObject(json);
                String cod = jsonHelperGetString(JsonObject, "cod");

                if(cod != null){
                    if(cod.equals("200")){
                        JSONObject sys = jsonHelperGetJSONObject(JsonObject, "sys");
                        if(sys != null){
                            jsonResult += jsonHelperGetString(JsonObject, "name") + ", " +jsonHelperGetString(sys, "country") + "\n";
                        } else {
                            jsonResult += jsonHelperGetString(JsonObject, "name") + "\n";
                        }
                        jsonResult += "\n";

                        JSONObject coord = jsonHelperGetJSONObject(JsonObject, "coord");
                        if(coord != null){
                            String lon = jsonHelperGetString(coord, "lon");
                            String lat = jsonHelperGetString(coord, "lat");
                            jsonResult += "longituudi: " + lon + " latituudi: " + lat + "\n";
                        }
                        jsonResult += "\n";

                        JSONArray weather = jsonHelperGetJSONArray(JsonObject, "weather");
                        jsonResult += "Yleissää:\n";
                        if(weather != null){
                            for(int i=0; i<weather.length(); i++){
                                JSONObject thisWeather = weather.getJSONObject(i);
                                jsonResult += " " + jsonHelperGetString(thisWeather, "main") + "\n";
                                jsonResult += " " + jsonHelperGetString(thisWeather, "description") + "\n";
                                jsonResult += "\n";
                            }
                        }

                        JSONObject main = jsonHelperGetJSONObject(JsonObject, "main");
                        if(main != null){
                            jsonResult += "Lämpötila: " + jsonHelperGetString(main, "temp") + " astetta\n";
                            jsonResult += "Kosteus: " + jsonHelperGetString(main, "humidity") + " %\n";
                            jsonResult += "\n";
                        }

                        jsonResult += "Näkyvyys: " + jsonHelperGetString(JsonObject, "visibility") + " metriä\n";
                        jsonResult += "\n";

                        JSONObject wind = jsonHelperGetJSONObject(JsonObject, "wind");
                        if(wind != null){
                            jsonResult += "Tuuli:\n";
                            jsonResult += " Nopeus: " + jsonHelperGetString(wind, "speed") + " metriä sekunnissa\n";
                            jsonResult += " Suunta: " + jsonHelperGetString(wind, "deg") + "\n";
                            jsonResult += "\n";
                        }

                    }else if(cod.equals("404")){
                        String message = jsonHelperGetString(JsonObject, "message");
                        jsonResult += "cod 404: " + message;
                    }
                }else{
                    jsonResult += "cod == null\n";
                }

            } catch (JSONException e) {
                e.printStackTrace();
                jsonResult += e.getMessage();
            }

            return jsonResult;
        }

        private String jsonHelperGetString(JSONObject obj, String k){
            String v = null;
            try {
                v = obj.getString(k);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return v;
        }

        private JSONObject jsonHelperGetJSONObject(JSONObject obj, String k){
            JSONObject o = null;

            try {
                o = obj.getJSONObject(k);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return o;
        }

        private JSONArray jsonHelperGetJSONArray(JSONObject obj, String k){
            JSONArray a = null;

            try {
                a = obj.getJSONArray(k);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return a;
        }
    }
}
