package com.ub700.arstreamingclient;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback, SensorEventListener
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int PORT = 8080;
    private static String ipAddress = "";

    private Camera camera;
    private SurfaceHolder previewHolder;
    private boolean isSessionActive = false;
    private boolean isStreaming = false;

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private byte[] frame;
    private static int sensorBufferSize = 0;
    private static int MJPEGBufferSize = 0;
    private LinkedList<String> sensorBuffer = new LinkedList<>();
    private LinkedList<byte[]> MJPEGBuffer = new LinkedList<>();

    private Handler mSensorHandler;
    private Handler mMJPEGHandler;

    private Socket sessionSocket;
    private DataOutputStream session;

    private Socket streamSocket;
    private DataOutputStream stream;
    private String boundary = "gc0p4Jq0M2Yt08jU534c0p";
    private String sensorData;

    private long time = 0;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private static TextView delayText;
    private LinearLayout delayView;

    Runnable streamMJPEG = new Runnable() {
        @Override
        public void run() {
            try {
                buffer.reset();
                new YuvImage(frame, ImageFormat.NV21, 640, 480, null).compressToJpeg(new Rect(0, 0, 640, 480), 50, buffer);
                buffer.flush();

                stream.write(("Content-type: image/jpeg\r\n" +
                        "Content-Length: " + buffer.size() + "\r\n" +
                        "X-Timestamp:" + (time++) + "\r\n" +
                        "\r\n").getBytes());
                buffer.writeTo(stream);
                stream.write(("\r\n--" + boundary + "\r\n").getBytes());
                stream.flush();
            } catch (IOException e) {
                isStreaming = false;
                streamSocket = null;
                new StreamHeaderTask().execute();
            }
        }
    };

    Runnable streamSensor = new Runnable() {
        @Override
        public void run() {
            try {
                session.write(sensorData.getBytes());
            } catch (IOException e) {
                isSessionActive = false;
                sessionSocket = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_main);

        SurfaceView svCameraPreview = this.findViewById(R.id.svCameraPreview);
        this.previewHolder = svCameraPreview.getHolder();
        this.previewHolder.addCallback(this);

        HandlerThread handlerThread = new HandlerThread("MJPEGstreamer");
        handlerThread.start();
        mMJPEGHandler = new Handler(handlerThread.getLooper());

        handlerThread = new HandlerThread("sesnorStreamer");
        handlerThread.start();
        mSensorHandler = new Handler(handlerThread.getLooper());

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);

        EditText ipText = findViewById(R.id.ipText);
        delayText = findViewById(R.id.delayText);
        delayView = findViewById(R.id.delayView);
        SeekBar delayBar = findViewById(R.id.delayBar);
        delayBar.setOnSeekBarChangeListener(new DelayBarListener());

        ipText.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    ipAddress = ipText.getText().toString();
                    new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                    return true;
                }
                return false;
            });
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isStreaming && isSessionActive) {
            frame = data;
            if (MJPEGBufferSize == 0)
                mMJPEGHandler.post(streamMJPEG);
            else {
                MJPEGBuffer.offer(frame);
                if (MJPEGBuffer.size() > MJPEGBufferSize) {
                    while(MJPEGBuffer.size() > MJPEGBufferSize)
                        frame = MJPEGBuffer.poll();
                    mMJPEGHandler.post(streamMJPEG);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera();
        new StreamHeaderTask().execute();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isStreaming = false;
        stopCamera();
    }

    private void startCamera() {
        int width = 640;
        int height = 480;
        this.previewHolder.setFixedSize(1080, 1440);
        try
        {
            camera = Camera.open();
            camera.setPreviewDisplay(this.previewHolder);
            Camera.Parameters params = camera.getParameters();
            params.setPreviewSize(width, height);
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewFrameRate(30);
            camera.setDisplayOrientation(90);
            camera.setParameters(params);
            camera.setPreviewCallback(this);
            camera.startPreview();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (camera != null)
        {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isStreaming && isSessionActive) {
            sensorData = "";
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
                sensorData = "acceleration "+event.values[0]+":"+event.values[1]+":"+event.values[2]+"\n";
            } else {
                sensorData = "gyroscope "+event.values[0]+":"+event.values[1]+":"+event.values[2]+"\n";
            }
            if (sensorBufferSize == 0)
                mSensorHandler.post(streamSensor);
            else {
                sensorBuffer.offer(sensorData);
                if (sensorBuffer.size() > sensorBufferSize) {
                    while(sensorBuffer.size() > sensorBufferSize)
                        sensorData = sensorBuffer.poll();
                    mSensorHandler.post(streamSensor);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            if (sessionSocket == null) {
                try {
                    sessionSocket = new Socket();
                    String[] address = ipAddress.split(" ");
                    sessionSocket.connect(new InetSocketAddress(address[0], Integer.parseInt(address[1])), 10000);
                    session = new DataOutputStream(sessionSocket.getOutputStream());
                    Log.i(TAG, "Connected to :"+ipAddress);
                    isSessionActive = true;
                } catch (IOException e) {
                    sessionSocket = null;
                    Log.e(TAG, e.toString());
                }
            }
            return null;
        }
    }

    private class StreamHeaderTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                if (streamSocket == null) {
                    ServerSocket server = new ServerSocket(PORT);
                    streamSocket = server.accept();
                    while (!streamSocket.getInetAddress().toString().equals("/"+ipAddress.split(" ")[0]))
                        streamSocket = server.accept();
                    server.close();
                    Log.i(TAG, "Streaming to :" + streamSocket.getInetAddress());
                    stream = new DataOutputStream(streamSocket.getOutputStream());
                    stream.write(("HTTP/1.1 200 OK\r\n" +
                            "Server: ARStreamer\r\n" +
                            "Connection: keep-alive\r\n" +
                            "Max-Age: 0\r\n" +
                            "Expires: 0\r\n" +
                            "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: multipart/x-mixed-replace; " +
                            "boundary=" + boundary + "\r\n" +
                            "\r\n" +
                            "--" + boundary + "\r\n").getBytes());
                    stream.flush();
                }
                isStreaming = true;
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            delayView.setVisibility(View.VISIBLE);
        }
    }

    private static class DelayBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            int delay = i < 500 ? i-500 : ((i-500)*30)/1000;
            if (delay < 0) {
                sensorBufferSize = -delay;
                MJPEGBufferSize = 0;
            } else {
                MJPEGBufferSize = delay;
                sensorBufferSize = 0;
            }
            delayText.setText(String.valueOf(delay));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    }
}
