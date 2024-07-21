package com.example.smsgateway;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button startButton, stopButton;
    private TextView smsCount;
    private Handler handler = new Handler();
    private Runnable runnable;
    private int count = 0;
    private static final String TAG = "SMSGateway";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        smsCount = findViewById(R.id.smsCount);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSending();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSending();
            }
        });
    }

    private void startSending() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        runnable = new Runnable() {
            @Override
            public void run() {
                sendSMS();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(runnable);
    }

    private void stopSending() {
        handler.removeCallbacks(runnable);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void sendSMS() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://10.0.2.2:3000/sms/getSMS");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    if (response.length() > 0) {
                        JSONObject jsonObject = new JSONObject(response.toString());
                        String phone = jsonObject.getString("phone");
                        String msg = jsonObject.getString("msg");
                        int id = jsonObject.getInt("id");
                        Log.d(TAG, "Response from markAsSent: " + phone);

                        // Implementation of the actual SMS sending logic

                        count++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                smsCount.setText("Sent SMS Messages: " + count);
                            }
                        });

                        markAsSent(id);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void markAsSent(int id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://10.0.2.2:3000/sms/markAsSent");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("id", id);
                    Log.d(TAG, "Sending data: " + jsonParam.toString());

                    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                    out.write(jsonParam.toString());
                    out.flush();
                    out.close();

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    Log.d(TAG, "Response from markAsSent: " + response.toString());

                } catch (Exception e) {
                    Log.e(TAG, "Error in markAsSent", e);
                }
            }
        }).start();
    }


}
