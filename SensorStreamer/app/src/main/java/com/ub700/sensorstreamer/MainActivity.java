package com.ub700.sensorstreamer;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import static android.content.ContentValues.TAG;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Socket socket;
    private PrintWriter out;
    private ClientTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText editText = findViewById(R.id.editText);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        findViewById(R.id.button).setOnClickListener((View v) -> {
            Log.e(TAG, "Button Clicked");
            String address = editText.getText().toString();
            task = new ClientTask();
            try {
                task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, address).get();
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        });
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    protected void onPause() {
        super.onPause();
//        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (out != null) {
            String sensorData = "";
//            Log.e("Sensor ", ""+event.sensor.getType());
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
                sensorData = "acceleration "+event.values[0]+":"+event.values[1]+":"+event.values[2];
            } else {
                sensorData = "gyroscope "+event.values[0]+":"+event.values[1]+":"+event.values[2];
            }
//            task.execute(sensorData);
            out.println(sensorData);
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            /* Setup connection with other AVDS for the first time. */
            if (socket == null) {
                    try {
                        Log.e(TAG, "Before connecting to to "+msgs[0]);
                        socket = new Socket();
                        String[] address = msgs[0].split(":");
                        socket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 5000);
                        socket.setSoTimeout(0);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        Log.e(TAG, "Connected to "+msgs[0]);
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
            } else {
                try {
                    out.println(msgs[0]);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
            return null;
        }
    }
}
