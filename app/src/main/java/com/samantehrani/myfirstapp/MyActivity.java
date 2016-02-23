package com.samantehrani.myfirstapp;

import android.content.ServiceConnection;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.os.IBinder;
import android.content.ComponentName;
import android.view.MenuItem;
import android.content.Context;
import android.net.*;
import android.content.ServiceConnection;
import android.view.Window;
import android.widget.*;
import android.view.View;
import android.content.Intent;
import android.util.Log;
import android.bluetooth.*;
import java.util.Set;
import com.samantehrani.myfirstapp.CommunicationService.CommunicationBinder;
import com.samantehrani.myfirstapp.CommunicationService;

import org.w3c.dom.Text;

import java.util.Objects;


public class MyActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MyActivity";
    private static final String TAG =  MyActivity.class.getSimpleName();
    CommunicationService mCommunicationService;
    boolean mServiceBound = false;

    private BluetoothAdapter bluetoothAdapter;
    private RazorAHRS razor;
    private RadioGroup deviceListRadioGroup;
    public Button refreshBlutoothButton;
    public Button connectBluetoothButton;

    public Button calibrateSensorButton;
    public Button startButton;
    public TextView statusText;
    public TextView yawTextView;
    public TextView pitchTextView;
    public TextView rollTextView;
    public TextView yawPhoneTextView;
    public TextView pitchPhoneTextView;
    public TextView rollPhoneTextView;
    public TextView errorView;

    public TextView calibCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);


        statusText = (TextView)findViewById(R.id.bluetoothStatus);
        statusText.setText("Service not bound!");

        deviceListRadioGroup = (RadioGroup) findViewById(R.id.devicesRadioGroup);

        refreshBlutoothButton = (Button) findViewById(R.id.refreshButton);
        connectBluetoothButton = (Button) findViewById(R.id.connectBluetooth);
        calibrateSensorButton = (Button) findViewById(R.id.calibrateButton);
        startButton = (Button) findViewById(R.id.startButton);
        calibCount = (TextView) findViewById(R.id.textViewCalibCount);
        yawTextView = (TextView) findViewById(R.id.yawValue);
        pitchTextView = (TextView) findViewById(R.id.pitchValue);
        rollTextView = (TextView) findViewById(R.id.rollValue);
        yawPhoneTextView = (TextView) findViewById(R.id.yawPhoneValue);
        pitchPhoneTextView = (TextView) findViewById(R.id.pitchPhoneValue);
        rollPhoneTextView = (TextView) findViewById(R.id.rollPhoneValue);
        errorView = (TextView) findViewById(R.id.errorTextView);
        errorView.setText("-");
        findBLTDevices();

        if ( !mServiceBound ) {
            Intent intent = new Intent(this, CommunicationService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void onConnectBluetoothButtonClicked(View view){
        final Button  startButton = (Button)view;
        String state = startButton.getText().toString();
        if (state.equals("CONNECT")) {
            final Button refresh = (Button) view;
            RadioButton rb = (RadioButton) findViewById(deviceListRadioGroup.getCheckedRadioButtonId());
            if (rb == null) {
                statusText.setText("You have select a device first.");
                return;
            }
            BluetoothDevice razorDevice = (BluetoothDevice) rb.getTag();
            mCommunicationService.connectBluetooth(razorDevice);
        }

    }
    public void onSetZeroButtonClicked(){

    }
    public void onSaveCalibrationData(View view){
        mCommunicationService.saveCalibrationData();
    }
    public void onCalibrateButtonClicked(View view){


        if( startButton.getText().equals("Start!") ){
            if(connectBluetoothButton.getText().equals("DISCONNECT") ){
                mCommunicationService.calibrateSensors();
            }
        }

    }
    public void onRefreshBluetoothButtonClicked(View view){
        final Button  refresh = (Button)view;
        findBLTDevices();
    }
    public void onStartLocationButtonClicked(View view){
        final Button  startButton = (Button)view;
        String state = startButton.getText().toString();
        if (state.equals( "Start Location")){
            if( mServiceBound ) {

                String ip = readIP();
                if ( mCommunicationService.startLocationTransmission(Integer.valueOf("7777"), ip) )
                    startButton.setText("Stop Location");
                else{
                    startButton.setText("Start Location");
                }
            }else{
                startButton.setText("Service Not Yet Bound!!");
            }
        }else if ( state.equals("Stop Location")){
            if (mServiceBound) {
                mCommunicationService.stopCommunication();
                startButton.setText("Start Location");
            }else{
                startButton.setText("Service Not Yet Bound!!");
            }

        }
    }
    public void onStartButtonClicked ( View view){
        final Button  startButton = (Button)view;
        String state = startButton.getText().toString();
        if (state.equals( "Start!")){
            if( mServiceBound ) {
                TextView statusText = (TextView) findViewById(R.id.textedit_port);
                String ip = readIP();
                if ( mCommunicationService.startTransmission(Integer.valueOf(statusText.getText().toString()), ip) )
                    startButton.setText("Stop!");
                else{
                    startButton.setText("Start!");
                }
            }else{
                startButton.setText("Service Not Yet Bound!!");
            }
        }else if ( state.equals("Stop!")){
            if (mServiceBound) {
                mCommunicationService.stopCommunication();
                startButton.setText("Start!");
            }else{
                startButton.setText("Service Not Yet Bound!!");
            }

        }

    }

    protected void onStart(){
        super.onStart();
    };

    protected void onRestart(){
        super.onRestart();
    };

    protected void onResume(){
        super.onResume();
    };

    protected void onPause(){
        super.onPause();
    };

    protected void onStop(){
        super.onStop();
        // Unbind from the service
    };

    protected void onDestroy(){

        super.onDestroy();
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
        }
    }
    private String readIP(){
        TextView ip1 = (TextView)findViewById(R.id.textedit_ip_1);
        TextView ip2 = (TextView)findViewById(R.id.textedit_ip_2);
        TextView ip3 = (TextView)findViewById(R.id.textedit_ip_3);
        TextView ip4 = (TextView)findViewById(R.id.textedit_ip_4);

        return ip1.getText().toString()+'.'+ip2.getText().toString() +'.'+ip3.getText().toString() + '.'+ip4.getText().toString();
    }
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CommunicationBinder binder = (CommunicationBinder) service;
            mCommunicationService = binder.getService();
            mCommunicationService.myActivity = MyActivity.this;

            mServiceBound = true;
            Log.v(LOG_TAG, "Service Bounded!");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mServiceBound = false;
        }
    };


    private void findBLTDevices(){
        deviceListRadioGroup.removeAllViews();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {	// Ooops
            statusText.setText("Your device does not seem to have Bluetooth, sorry.");
            Log.v(LOG_TAG,"Your device does not seem to have Bluetooth, sorry.");

            return;
        }

        // Check whether Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            statusText.setText("Bluetooth not enabled. Please enable and try again!");
            Log.v(LOG_TAG, "Bluetooth not enabled. Please enable and try again!");

            return;
        }

        // Get list of paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // Add devices to radio group
        for (BluetoothDevice device : pairedDevices) {
            RadioButton rb = new RadioButton(this);
            rb.setText(" " + device.getName());
            rb.setTag(device);
            deviceListRadioGroup.addView(rb);
        }
        if (pairedDevices.size() == 0) {
            statusText.setText("No paired Bluetooth devices found." );


        } else {
            ((RadioButton) deviceListRadioGroup.getChildAt(0)).setChecked(true);

        }
    }

}
