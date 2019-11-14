package com.android.jingyi.btdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
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

public class ComminuteActivity extends AppCompatActivity {
    private String TAG = "ComminuteActivity";

    private BluetoothReceiver receiver;
    private BluetoothAdapter bluetoothAdapter;
    private List<String> devices;
    private List<BluetoothDevice> deviceList;
    private Bluetooth client;
    private final String lockName = "BOLUTEK";
    private String message = "000001";
    private ListView listView;
    private ProgressBar progressBar;
    private Button sendBtn;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comminute);

        mContext = getApplicationContext();

        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        listView = (ListView)findViewById(R.id.device_list);
        sendBtn = (Button)findViewById(R.id.sendmessage);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                client.sendMessage("aaaaa");
            }
        });

        deviceList = new ArrayList<BluetoothDevice>();
        devices = new ArrayList<String>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();
//        bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
//            @Override
//            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
//
//                bluetoothDevice.connectGatt(mContext, false, )
//
//                Log.i(TAG, "onLeScan bluetoothDevice="+bluetoothDevice.getName()+" address="+bluetoothAdapter.getAddress());
//            }
//        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice device = deviceList.get(i);
                client = new Bluetooth(device, handler);
                try {
                    client.connect("0001");
                } catch (Exception e) {
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, intentFilter);
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive action="+action);

            switch (action) {
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.i(TAG, "device="+device.getName()+" address="+device.getAddress());
                    String name = device.getName();
                    if (TextUtils.isEmpty(name)) {
                        name = device.getAddress();
                    }
                    if (!devices.contains(name)) {
                        devices.add(name);
                    }
                    deviceList.add(device);
                    showDevices();
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                }
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                }
                case BluetoothDevice.ACTION_UUID: {
                    ParcelUuid uuid = intent.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
                    if (uuid != null) {
                        Log.i(TAG, "uuid=" + uuid.getUuid().toString());
                    }
                    break;
                }
            }
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handlemessage what="+msg.what);

            switch (msg.what) {

            }
        }
    };

    private boolean isLock(BluetoothDevice device) {
        boolean isLockName = (device.getName()).equals(lockName);
        boolean isSingleDevice = devices.indexOf(device.getName()) == -1;
//        return isLockName && isSingleDevice;
        return isSingleDevice;
    }

    private void showDevices() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                devices);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
