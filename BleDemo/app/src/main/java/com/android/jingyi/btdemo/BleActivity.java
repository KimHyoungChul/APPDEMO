package com.android.jingyi.btdemo;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class BleActivity extends AppCompatActivity {
    private String TAG = "BleActivity";

    BleUtil bleUtil;

    ListView listView;
    ProgressBar progressBar;
    Button writebutton;
    Button readbutton;

    private List<String> devices;
    private List<BluetoothDevice> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        listView = (ListView)findViewById(R.id.ble_device_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                bleUtil.connectLeDevice(position);
            }
        });

        writebutton = (Button)findViewById(R.id.sendbtn);
        writebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleUtil.sendStrengWrite(100);
            }
        });

        readbutton = (Button)findViewById(R.id.readBtn);
        readbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleUtil.connectLeDevice(1);
//                bleUtil.sendStrengthRead(100);
            }
        });

        devices = new ArrayList<String>();

        bleUtil = BleUtil.getInstance();
        bleUtil.setContext(this);

        bleUtil.setBTUtilListener(btUtilListener);
        bleUtil.scanLeDevice(true);
    }

    BleUtil.BTUtilListener btUtilListener = new BleUtil.BTUtilListener() {
        @Override
        public void onLeScanStart() {
            Log.i(TAG, "onLeScanStart");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onLeScanStop() {
            Log.i(TAG, "onLeScanStop");
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onLeScanDevices(List<BluetoothDevice> listDevice) {
            Log.i(TAG, "onLeScanDevices");
            deviceList = listDevice;
            devices.clear();
            for (BluetoothDevice device: listDevice) {
                devices.add(TextUtils.isEmpty(device.getName()) ? device.getAddress() : device.getName());
            }
            showDevices();
        }

        @Override
        public void onConnected(BluetoothDevice mCurDevice) {
            Log.i(TAG, "onConnected");

        }

        @Override
        public void onDisConnected(BluetoothDevice mCurDevice) {
            Log.i(TAG, "onDisConnected");

        }

        @Override
        public void onConnecting(BluetoothDevice mCurDevice) {
            Log.i(TAG, "onConnecting");

        }

        @Override
        public void onDisConnecting(BluetoothDevice mCurDevice) {
            Log.i(TAG, "onDisConnecting");

        }

        @Override
        public void onStrength(int strength) {
            Log.i(TAG, "onStrength");

        }

        @Override
        public void onModel(int model) {
            Log.i(TAG, "onModel");

        }
    };

    private void showDevices() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                devices);
        listView.setAdapter(adapter);
    }
}
