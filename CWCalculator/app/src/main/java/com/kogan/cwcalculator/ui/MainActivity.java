package com.kogan.cwcalculator.ui;


import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kogan.cwcalculator.R;
import com.kogan.cwcalculator.model.AirConditioner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button triggerApi;
    private TextView label;
    private TextView resultView;
    private Button back;
    private ProgressDialog progressDialog;

    private String baseUrl;
    private HashMap<Integer,AirConditioner> airConditioners;
    private double conversionFactor;
    private String productName;

    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUi();
    }

    /**
     * Initialize the User Interface
     */
    private void initializeUi(){
        Toolbar toolbar = (Toolbar)findViewById(R.id.action_menu_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        boolean networkStatus = NetworkStatusChecker.getNetworkConnectivity(this);

        if(networkStatus){
            triggerApi = (Button)findViewById(R.id.btn_trigger_api);
            back = (Button)findViewById(R.id.btn_back);
            label = (TextView)findViewById(R.id.tv_label);
            resultView = (TextView)findViewById(R.id.tv_avgcw);

            airConditioners = new HashMap<>();
            random = new Random();
            conversionFactor = 250;
            productName = "Air Conditioners";
            baseUrl = "http://wp8m3he1wt.s3-website-ap-southeast-2.amazonaws.com";

            triggerApi.setOnClickListener(apiTriggerListener);
            back.setOnClickListener(backListener);
        }
        else
            Toast.makeText(this, "Please connect to internet", Toast.LENGTH_SHORT).show();
    }

    /**
     * On Click listener for api trigger button. Connects to API and fetch data.
     */
    private View.OnClickListener apiTriggerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String requestEndPoint = "/api/products/1";
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle("Loading");
            progressDialog.setMessage("Loading "+productName + " details!");
            progressDialog.show();
            triggerAPIRequest(requestEndPoint);
        }
    };

    /**
     * Triggers API GET request through Android - Volley.
     * @param endPoint: URL end point, concatenated with base URL to get complete URL.
     */
    private void triggerAPIRequest(String endPoint){

        final RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + endPoint, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                extractDataFromResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(response !=null && response.data != null){
                        switch(response.statusCode){
                            case 500:
                                Toast.makeText(MainActivity.this,"Something went wrong, try again later",Toast.LENGTH_LONG).show();
                                break;
                            case 404:
                                Toast.makeText(MainActivity.this,"Requested Page Not Found",Toast.LENGTH_LONG).show();
                                break;
                        }
                }
            }
        });

        requestQueue.add(jsonObjectRequest);
    }


    /**
     * Parse the JSON response, invoke triggerAPIRequest method if the next end point is not null.
     * If the next end point is null, invokes average cubic weight calculation.
     * @param response: JSON response from the GET request.
     */
    private void extractDataFromResponse(JSONObject response){
        try {
            if(response.has("objects")){
                JSONArray objectsReceived = (JSONArray) response.get("objects");
                for(int i =0; i < objectsReceived.length(); i++){
                    JSONObject productJson  = (JSONObject) objectsReceived.get(i);

                    if(productJson.getString("category").equals(productName)){
                        AirConditioner airConditioner = new AirConditioner();
                        JSONObject size = (JSONObject)productJson.get("size");
                        airConditioner.setHeight(Double.parseDouble(size.getString("height")));
                        airConditioner.setWidth(Double.parseDouble(size.getString("width")));
                        airConditioner.setLength(Double.parseDouble(size.getString("length")));
                        double cubicWeight = (airConditioner.getLength()*airConditioner.getHeight()*airConditioner.getWidth()*conversionFactor)/1000000;
                        airConditioner.setCubicWeight(cubicWeight);
                        int key = random.nextInt();
                        if(!airConditioners.containsKey(key))
                            airConditioners.put(key,airConditioner);
                    }

                }
            }

            if(!response.getString("next").equals("null")){
                triggerAPIRequest(response.getString("next"));
            }
            else{
                calculateAverageCubicWeight();
            }



        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the average cubic weight from Hashmap.
     * Update the user interface with the result.
     */
    private void calculateAverageCubicWeight(){

        if(airConditioners.size()>0){
            double sum = 0;
            for(Map.Entry<Integer,AirConditioner> entry : airConditioners.entrySet()){
                AirConditioner tempAirConditioner = (AirConditioner)entry.getValue();
                sum += tempAirConditioner.getCubicWeight();
            }

            resultView.setText(String.format("%.2f",sum/airConditioners.size())+"kg");
            progressDialog.dismiss();
            resultView.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
            triggerApi.setVisibility(View.INVISIBLE);
            back.setVisibility(View.VISIBLE);
        }
        else
            Toast.makeText(MainActivity.this, "Sorry, No "+productName+" available", Toast.LENGTH_SHORT).show();

    }

    /**
     * On click listener for back button.
     */
    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            resultView.setVisibility(View.INVISIBLE);
            label.setVisibility(View.INVISIBLE);
            back.setVisibility(View.INVISIBLE);
            triggerApi.setVisibility(View.VISIBLE);
            airConditioners.clear();
        }
    };
}