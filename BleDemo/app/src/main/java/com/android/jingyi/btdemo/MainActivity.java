package com.android.jingyi.btdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button button;
    Button bleButton;
    Button serverBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
            adapter.enable();
        }
//        Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//        enable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600); //3600为蓝牙设备可见时间
//        startActivity(enable);


        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search();
            }
        });

        bleButton = (Button)findViewById(R.id.scan_ble);
        bleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BleActivity.class);
                startActivity(intent);
            }
        });

        serverBtn = (Button)findViewById(R.id.gatt_server);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GattServerActivity.class);
                startActivity(intent);
            }
        });

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED); {
               requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
        }
    }

    private void search() {
        Intent searchIntent = new Intent(this, ComminuteActivity.class);
        startActivity(searchIntent);
    }

}
