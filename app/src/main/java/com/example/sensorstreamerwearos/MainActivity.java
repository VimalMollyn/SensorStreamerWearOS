package com.example.sensorstreamerwearos;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.sensorstreamerwearos.databinding.ActivityMainBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends WearableActivity implements SensorEventListener {

    private static final String LOG_TAG = "log tag";
    private Button startButton,stopButton;

    private TextView ipAddr;
    private TextView unixTimeText;
    private ActivityMainBinding binding;
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private int port = 5005;

    // audio stuff
    private AudioRecord audioRecorder;
    int audioBufferSize = 160*2;
    int audioSamplingRate = 16000;
    byte[] audioBuffer = new byte[audioBufferSize];
    int minBufSize = AudioRecord.getMinBufferSize(audioSamplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    // random
    private String TAG = "UDP";
    private String SERVER = "192.168.0.175"; // tplink5g
    private int PORT = 5005;
    private InetAddress serverAddress;
    private DatagramSocket socket;
    private boolean status = true;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;
    private SensorManager mSensorManager;
    private HashMap<String, Sensor> mSensors = new HashMap<>();


    private final static int REQUEST_CODE_ANDROID = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    private static boolean hasPermissions(Context context, String... permissions) {

        // check Android hardware permissions
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        // nullify back button when recording starts
        if (!mIsRecording.get()) {
            super.onBackPressed();
            wakeLockRelease();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startButton = (Button) findViewById (R.id.StartStreaming);
        stopButton = (Button) findViewById (R.id.StopStreaming);

        ipAddr = (TextView) findViewById(R.id.ipAddr);

        unixTimeText = (TextView) findViewById(R.id.unixTime);

        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);
//        Thread udpConnect = new Thread(new UDPClient(port, ip));
//        udpConnect.start();

        // get required permissions
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_ANDROID);
        }

        // acquire wakelock
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLockAcquire();
        setAmbientEnabled();

        // sensor stuff

        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        // setup and register various sensors
        mSensors.put("acce", mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        // mSensors.put("gyro", mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        // mSensors.put("rotvec", mSensorManager.getDefaultSensor((Sensor.TYPE_ROTATION_VECTOR)));
        // mSensors.put("mag", mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

        // Used to get all the available microphone sampling rates
        // getSamplingRates();
    }

    public void registerSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.registerListener((SensorEventListener) this, eachSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void unregisterSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.unregisterListener((SensorEventListener) this, eachSensor);
        }
    }
    private final View.OnClickListener stopListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = false;
            audioRecorder.release();
            socket.close();
//            mIsRecording.set(false);
            Log.d("VS","Recorder released");
            unregisterSensors();
        }

    };

    private final View.OnClickListener startListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            status = true;
            SERVER = ipAddr.getText().toString();
            try {
                socket = new DatagramSocket();
                Log.d("VS", "Socket Created");
            } catch (SocketException e) {
                e.printStackTrace();
            }
            registerSensors();
            startStreaming();
        }

    };

    public void startStreaming() {

        // set mIsRecording to true
//        mIsRecording.set(true);

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

//                    DatagramSocket socket = new DatagramSocket();
                    byte[] buffer = new byte[minBufSize];
//                    ByteBuffer[] buffer = new ByteBuffer[minBufSize];
//                    short[] buffer = new short[minBufSize];

//                    HashMap<String, byte[]> to_send = new HashMap<String, byte[]>();
                    HashMap<String, String> to_send = new HashMap<String, String>();
//                    JSONObject to_send = new JSONObject();

                    Log.d("VS","Buffer created of size " + minBufSize);
                    DatagramPacket packet;


                    Log.d("VS", "Address retrieved");

                    final InetAddress destination = InetAddress.getByName(SERVER);
                    Log.d("VS", "Address retrieved");


                    audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,audioSamplingRate,
                            AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,
                            minBufSize*10);
                    Log.d("VS", "Recorder initialized");

                    audioRecorder.startRecording();


                    while(status) {


                        //reading data from MIC into buffer
                        int n_read;
                        n_read = audioRecorder.read(buffer, 0, buffer.length);
//                        n_read = audioRecorder.read(buffer, buffer.length);
                        // convert buffer to string
                        // byte[] audio_encoded = Base64.getEncoder().encode(buffer);
                        // get current timestamp and send that as well
                        long unixTime = System.currentTimeMillis();
//                        to_send.put("unixTime_send", unixTime);
                        String unixString = String.valueOf(unixTime) + ",";

                        String audio_encoded = unixString + "audio," + Base64.getEncoder().encodeToString(buffer);

                        byte[] audio_buf = audio_encoded.getBytes(StandardCharsets.UTF_8);
                        packet = new DatagramPacket(audio_buf, audio_buf.length, destination, port);
                        socket.send(packet);
                        System.out.println("MinBufferSize: " +minBufSize + " ,new buff size " + audio_buf.length + ", nread, " + n_read);
                    }


                } catch(UnknownHostException e) {
                    Log.e("VS", "UnknownHostException",e);


                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", ""+ e);
                }
            }

        });
        streamThread.start();
    }

    private void wakeLockAcquire(){
        if (mWakeLock != null){
            Log.e(LOG_TAG, "WakeLock already acquired!");
            return;
        }
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sensors_data_logger:wakelocktag");
        mWakeLock.acquire(600*60*1000L /*10 hours*/);
        Log.e(LOG_TAG, "WakeLock acquired!");
    }

    private void wakeLockRelease(){
        if (mWakeLock != null && mWakeLock.isHeld()){
            mWakeLock.release();
            Log.e(LOG_TAG, "WakeLock released!");
            mWakeLock = null;
        }
        else{
            Log.e(LOG_TAG, "No wakeLock acquired!");
        }
    }

    private void sendSensorValues(String type, long sensorTimestamp, long unixTimestamp, float[] values) throws IOException {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // inputs are a float of values
                long unixTime = System.currentTimeMillis();
//                        to_send.put("unixTime_send", unixTime);
                String unixString = String.valueOf(unixTime) + ",";
                Log.d("UVM", unixString);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        // Stuff that updates the UI
                        unixTimeText.setText(unixString);

                    }
                });
                StringBuilder to_send = new StringBuilder(unixString + type + "," + String.valueOf(sensorTimestamp) + "," + String.valueOf(unixTimestamp) + ",");
                for (int i = 0; i < values.length; i++) {
                    if (i < values.length - 1) {
                        to_send.append(String.valueOf(values[i])).append(",");
                    } else {
                        to_send.append(String.valueOf(values[i]));
                    }
                }

                byte[] buf = to_send.toString().getBytes(StandardCharsets.UTF_8);
                final InetAddress destination;
                try {
                    destination = InetAddress.getByName(SERVER);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, destination, port);
                socket.send(packet);
                Log.d("VM", "Sending data");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        streamThread.start();
    }
    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {

        // update each sensor measurements
        // NOTE: This is the place to change the timestamp to unix if we want to
        long sensorTimestamp = sensorEvent.timestamp;
        long unixTimestamp = System.currentTimeMillis();
//        long timeInMillis = System.currentTimeMillis() + (sensorEvent.timestamp - System.nanoTime()) / 1000000L;
//        Log.d("Timestamps", "SystemTime: " + String.valueOf(timestamp) + ", Corrected: " +
//                String.valueOf(timeInMillis) + ", SensorTime: " + String.valueOf(sensorEvent.timestamp));
        Sensor eachSensor = sensorEvent.sensor;
        switch (eachSensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                try {
                    sendSensorValues("acce", sensorTimestamp, unixTimestamp, sensorEvent.values);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(sensorTimestamp, "acce", 3, sensorEvent.values, unixTimestamp);
//                    }
                break;

            case Sensor.TYPE_GYROSCOPE:
                try {
                    sendSensorValues("gyro", sensorTimestamp, unixTimestamp, sensorEvent.values);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(sensorTimestamp, "gyro", 3, sensorEvent.values, unixTimestamp);
//                    }
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                try {
                    sendSensorValues("mag", sensorTimestamp, unixTimestamp, sensorEvent.values);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(sensorTimestamp, "magnet", 3, sensorEvent.values, unixTimestamp);
//                    }
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                try {
                    sendSensorValues("rotvec", sensorTimestamp, unixTimestamp, sensorEvent.values);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(sensorTimestamp, "rotvec", 4, sensorEvent.values, unixTimestamp);
//                    }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void getSamplingRates(){
        for (int rate : new int[] {125, 250, 500, 1000, 2000, 4000, 8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported
                Log.d("Sampling Rates", rate + " Hz is allowed!, buff size " + bufferSize);

            }
        }
    }
}
