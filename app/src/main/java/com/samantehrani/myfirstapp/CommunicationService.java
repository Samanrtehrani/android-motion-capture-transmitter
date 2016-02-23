package com.samantehrani.myfirstapp;


/**
 * Created by Sk on 9/10/15.
 */


import android.app.Service;import java.lang.Math;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.location.*;
import android.os.Bundle;

import java.math.BigDecimal;
import android.os.IBinder;
import android.util.Log;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import android.content.SharedPreferences;
import java.lang.InterruptedException;
import java.io.IOException;
import android.os.Binder;
import android.net.*;
import android.content.Context;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import android.bluetooth.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.samantehrani.myfirstapp.RazorAHRS;
import com.samantehrani.myfirstapp.RazorListener;
import com.samantehrani.myfirstapp.DeclinationHelper;
import android.widget.ArrayAdapter;


public class CommunicationService extends Service implements SensorEventListener{
    public MyActivity myActivity = null;
    private static final String LOG_TAG = "CommunicationService";


    private int PORT;
    private String ipAddress;
    DatagramSocket socket;
    DatagramPacket packet;
    Thread udpThread;
    private boolean stopUDPThread = true;

    private IBinder mBinder = new CommunicationBinder();

    private BluetoothAdapter mBluetoothAdapter;
    private RazorAHRS razor;
    private SensorManager mSensorManager;
    private Sensor mOrientation;

    private String networkState = Constants.INTERNET_STATE.INTERNET_DOWN;
    public String razorState = Constants.BLUETOOTH_STATE.BL_DOWN;

    //sensor orientation
    private float yaw = 0;
    private float pitch = 0;
    private float roll = 0;

    //phone orientation
    private  float y_angle = 0;
    private float p_angle = 0;
    private float r_angle = 0;
    private float offsetX=0;
    private float offsetY=0;
    private float offsetZ=0;
    private float diff = 0;

    private double packetCounter = 0;

    Thread calibrationThread;
    private boolean sensorCalibrationStarted = false;
    Map<Integer , Integer> sensorsMap = new HashMap<Integer, Integer>();

    LocationManager locationManager =null;
    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.NETWORK_PROVIDER),
            new LocationListener(LocationManager.GPS_PROVIDER)

    };

    public void saveCalibrationData(){
        if( sensorsMap.size() >=360 ) {
            SharedPreferences sharedPref = getSharedPreferences(
                    "calibration_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();

            for (Map.Entry<Integer, Integer> entry : sensorsMap.entrySet()) {
                editor.putInt(String.valueOf(entry.getKey()),entry.getValue());
            }
            editor.commit();
            myActivity.errorView.setText("Data Saved to Preferences!");
        }else{
            myActivity.errorView.setText("Please Do Calibration First!");
        }
    }
    public void calibrateSensors(){
        sensorsMap.clear();
        myActivity.calibrateSensorButton.setText("CALIBRATING");
        myActivity.calibrateSensorButton.setBackgroundColor(Color.BLUE);
        sensorCalibrationStarted = true;
    }
    @Override
    public void onSensorChanged(SensorEvent event){
        y_angle = event.values[0] ;
        p_angle = event.values[1];
        r_angle = event.values[2];
        try{
            myActivity.yawPhoneTextView.setText(String.format("%.1f",  y_angle  ));
            myActivity.pitchPhoneTextView.setText(String.format("%.1f", p_angle));
            myActivity.rollPhoneTextView.setText(String.format("%.1f", r_angle));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    public void onCreate(){
        super.onCreate();
        initializeLocationManager();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);

        checkInternetConnection();
        //////////////////////
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0,0,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
//            Log.i(LOG_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
  //          Log.d(LOG_TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
     //       Log.i(LOG_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
      //      Log.d(LOG_TAG, "gps provider does not exist " + ex.getMessage());
        }
        //////////////////////


    }
    public boolean connectBluetooth(BluetoothDevice device){
        razor = new RazorAHRS(device, new RazorListener() {

            @Override
            public void onConnectAttempt(int attempt, int maxAttempts) {
                myActivity.connectBluetoothButton.setText("CONNECTING...");
                myActivity.statusText.setText("Connecting Attempt #"+ String.valueOf(attempt) );
            }

            @Override
            public void onConnectOk() {
                razorState = Constants.BLUETOOTH_STATE.BL_UP;
                myActivity.connectBluetoothButton.setText("DISCONNECT");
                myActivity.statusText.setText("Connected!");
                SharedPreferences sharedPref = getSharedPreferences(
                        "calibration_data", Context.MODE_PRIVATE);
                Map<String, ?> allEntries = sharedPref.getAll();
                for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                    sensorsMap.put(Integer.valueOf(entry.getKey()) , (Integer)entry.getValue()) ;
                }
                if( sensorsMap.size() >= 360 ){
                    myActivity.errorView.setText("Data Pulled From Preferences!");
                }else{
                    myActivity.errorView.setText("Data either not Pulled From Preferences or broken!");
                }
            }

            public void onConnectFail(Exception e) {
                razorState = Constants.BLUETOOTH_STATE.BL_DOWN;
                myActivity.connectBluetoothButton.setText("CONNECT");
                myActivity.statusText.setText("Connecting failed: " + e.getMessage() + ".");
            }

            @Override
            public void onAnglesUpdate(float y, float r, float p) {
               /* if( y < 0){
                    y=360 + y;
                }*/

                pitch = p;

                roll = r;

                yaw = y ;

                ///////
                /*if(sensorsMap.size() >=360 ){
                    diff = sensorsMap.get((int)y_angle)  - yaw ;

                    myActivity.yawTextView.setText(String.format("%.1f", diff));
                    myActivity.pitchTextView.setText(String.format("%.1f", pitch));
                    myActivity.rollTe*/

             //   }
                myActivity.yawTextView.setText(String.format("%.1f", yaw));
                myActivity.pitchTextView.setText(String.format("%.1f", pitch));
                myActivity.rollTextView.setText(String.format("%.1f", roll));

                if( sensorCalibrationStarted ){
                    sensorsMap.put((int) y_angle, (int) yaw);
                    myActivity.calibCount.setText(String.valueOf(sensorsMap.size() ) );
                    if( sensorsMap.size() >= 360){
                        sensorCalibrationStarted = false;
                        myActivity.calibrateSensorButton.setTextColor(Color.BLACK);
                        myActivity.calibrateSensorButton.setText("CALIBRATED");
                        myActivity.calibrateSensorButton.setBackgroundColor(Color.GREEN);
                        for(Map.Entry<Integer,Integer> entry : sensorsMap.entrySet()){
                            Log.v(LOG_TAG,entry.getKey() + "*"+ entry.getValue().toString());
                        }
                    }

                }
            }

            @Override
            public void onIOExceptionAndDisconnect(IOException e) {
                razorState = Constants.BLUETOOTH_STATE.BL_DOWN;
                myActivity.connectBluetoothButton.setText("CONNECT");
                myActivity.statusText.setText("Disconnected, an error occured: " + e.getMessage() + ".");

            }
            @Override
            public void onSensorsUpdate(float accX, float accY, float accZ, float magX, float magY, float magZ,
                                 float gyrX, float gyrY, float gyrZ){
            }
        });

        razor.asyncConnect(5);
        return false;
    }
    private void checkInternetConnection(){
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connMgr.getActiveNetworkInfo();
        if( info != null && info.isConnected() ){
            networkState = Constants.INTERNET_STATE.INTERNET_UP;
        }else{
            networkState = Constants.INTERNET_STATE.INTERNET_DOWN;
        }
    }
    public String getNetworkState(){
       return networkState;
    }
    public boolean startLocationTransmission(int port, String ip){
        setupUDPSocket(port,ip);
        packetCounter = 0;
        this.udpThread.start();
        return true;
    }
    public boolean startTransmission(int port, String ip){
        if( razorState == Constants.BLUETOOTH_STATE.BL_UP) {
                setupUDPSocket(port,ip);
                packetCounter = 0;
                this.udpThread.start();
                return true;
        }
        return false;
    }
    public boolean setupUDPSocket(int port, String ip){
        //stopCommunication();

        this.PORT = port;
        this.ipAddress = ip;

        stopUDPThread = false;
        this.udpThread = new Thread() {

            @Override
            public void run() {

                while(true) {
                    if(stopUDPThread) return;
                    try {

                        //Float xValue = diff* 8 / 10;
                        Float xValue = yaw;
                        Float yValue = pitch ;
                        Float zValue = roll;



                            sleep(20);
                            InetAddress serverAddress = Inet4Address.getByName(ipAddress);


                            DatagramSocket socket = new DatagramSocket();
                            socket.connect(serverAddress, PORT);
                            socket.setBroadcast(false);

                            Character delimiter = ':';
                            long currentTime=System.currentTimeMillis();

                            Double latValue = new BigDecimal(mLocationListeners[1].mLastLocation.getLatitude()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                            Double lonValue = new BigDecimal(mLocationListeners[1].mLastLocation.getLongitude()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                            String msg =  xValue.toString() + delimiter.toString() + yValue.toString() + delimiter.toString() + zValue.toString() ;

                            final byte[] buf = (msg).getBytes();

                            DatagramPacket packet = new DatagramPacket(msg.getBytes(), buf.length);
                            packet.setAddress(serverAddress);
                            packet.setPort(PORT);


                            socket.send(packet);

                            socket.close();

                    } catch (final UnknownHostException e) {
                        e.printStackTrace();
                    } catch (final SocketException e) {

                        e.printStackTrace();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        return true;
    }
    public void stopCommunication(){
        try{
            if(udpThread != null) {
                this.stopUDPThread = true;
                this.udpThread.join();
                this.udpThread = null;
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }

    }
    @Override
    public IBinder onBind(Intent intent) {
        //Log.v(LOG_TAG, "in onBind");


        return mBinder;
    }
    @Override
    public void onRebind(Intent intent) {
       // Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
      //  Log.v(LOG_TAG, "in onUnbind");
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
       // Log.v(LOG_TAG, "in onDestroy");

        if (locationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    locationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                   // Log.i(LOG_TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
        mSensorManager.unregisterListener(this);

        try{
            this.stopUDPThread = true;
            this.udpThread.join();
            this.udpThread = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    public class CommunicationBinder extends Binder {
        public CommunicationService getService() {
            return CommunicationService.this;
        }

    }
    private void initializeLocationManager() {
        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener{
        public Location mLastLocation;
        public LocationListener(String provider) {
          //  Log.e(LOG_TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        @Override
        public void onLocationChanged(Location location) {
           // Log.e(LOG_TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

        }
        @Override
        public void onProviderDisabled(String provider) {
         //   Log.e(LOG_TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider) {
           // Log.e(LOG_TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

}


