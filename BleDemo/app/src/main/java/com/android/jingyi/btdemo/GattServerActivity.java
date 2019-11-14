package com.android.jingyi.btdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GattServerActivity extends AppCompatActivity {
    private String TAG = "GattServerActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    //先定几个服务类型的UUID
    /* Current Time Service UUID */
    public static UUID TIME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    /* Mandatory Current Time Information Characteristic */
    public static UUID CURRENT_TIME    = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
    /* Optional Local Time Information Characteristic */
    public static UUID LOCAL_TIME_INFO = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");
    /* Mandatory Client Characteristic Config Descriptor */
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    Button notifyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gatt_server);

        openAndInitBt();//初始化需要一定时间
        createGattServer();//所以以下这两个方法在这里直接运行是错误的，一定要在蓝牙正确开启，并且支持BLE在执行
        startAdvertising();//我卸载这里只是为了展示调用顺序。切记切记！！

        notifyBtn = (Button)findViewById(R.id.notifybtn);
        notifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateCharacteristic();
            }
        });
    }

    //1.初始化并打开蓝牙
    private void openAndInitBt(){
        mBluetoothManager=(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter==null){return;}//不支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return ;//不支持ble蓝牙
        }
        //.判断蓝牙是否打开
        if (!mBluetoothAdapter.enable()) {
            //没打开请求打开
            Intent btEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btEnable, 100);
        }
    }
    //2.创建GATT服务
    private void createGattServer() {

        //2.5 打开外围设备  注意这个services和server的区别，别记错了
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.i(TAG, "mBluetoothGattServer="+mBluetoothGattServer.toString());
            return;
        }

        //2.1.新建一个服务
        BluetoothGattService service = new BluetoothGattService(TIME_SERVICE,BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //2.2 新建一个Characteristic
        BluetoothGattCharacteristic currentTime = new BluetoothGattCharacteristic(CURRENT_TIME,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

//        BluetoothGattCharacteristic currentTime = new BluetoothGattCharacteristic(CURRENT_TIME,
//                //Read-only characteristic, supports notifications
//                255,
//                511);

        //2.3 新建特性描述并配置--这一步非必需
//        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
//                //Read/write descriptor
//                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
//        currentTime.addDescriptor(configDescriptor);

//         Local Time Information characteristic
        BluetoothGattCharacteristic localTime = new BluetoothGattCharacteristic(LOCAL_TIME_INFO,
                //Read-only characteristic
                BluetoothGattCharacteristic.PROPERTY_WRITE| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        //2.4 将特性配置到服务
        service.addCharacteristic(currentTime);
        service.addCharacteristic(localTime);

        mBluetoothGattServer.addService(service);
    }

    //3.通知服务开启
    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startAdvertising() {
        mBluetoothLeAdvertiser= mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            //创建失败
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder() 
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        Log.i(TAG, "startAdvertising settings="+settings.toString());

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceData(new ParcelUuid(TIME_SERVICE), "j".getBytes())
                .addServiceUuid(new ParcelUuid(TIME_SERVICE))//绑定服务uuid
                .build();
        Log.i(TAG, "startAdvertising data="+data.toString());

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }
        @Override
        public void onStartFailure(int errorCode) {
            Log.i(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    //4.模拟数据更新
    private void updateCharacteristic() {
            if (mBluetoothGattServer == null) {
                return;
            }
        final Handler updateHandler = new Handler();
        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (BluetoothDevice d : mRegisteredDevices) {
                    BluetoothGattCharacteristic newCharacteristic = mBluetoothGattServer
                            .getService(TIME_SERVICE)
                            .getCharacteristic(CURRENT_TIME);
//                    byte[] data = ("数据更新" + System.currentTimeMillis()).getBytes();
                    byte[] data = "11".getBytes();
                    newCharacteristic.setValue(data);
                    Log.i(TAG, "updateNotifyValue ... d="+d.toString());
                    mBluetoothGattServer.notifyCharacteristicChanged(d, newCharacteristic, true);
                }
                updateHandler.postDelayed(this, 5000);

            }
        }, 5000);//5s更新一次
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * 所有characteristics 和 descriptors 的读写请求都在这里处理
     * 这里我忽略了处理逻辑，这个根据实际需求写
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange status=" + status+" newState="+newState);
            //连接状态改变
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                mRegisteredDevices.add(device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onDescriptorReadRequest device=" + device.toString());
            //请求读特征 如果包含有多个服务，就要区分请求读的是什么，这里我只有一个服务
//            if(CURRENT_TIME.equals(characteristic.getUuid())){
                //回应
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,0,"123".getBytes());
//            }

        }

        //这个实际可以用于反向写数据
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i(TAG, "onCharacteristicWriteRequest value=" + new String(value, Charset.forName("UTF-8")));
            Log.i(TAG, "onCharacteristicWriteRequest device=" + device.toString());
            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            Log.i(TAG, "onDescriptorReadRequest device=" + device.toString());
            if( CLIENT_CONFIG.equals(descriptor.getUuid())){
                Log.i(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue);
            } else {
                Log.i(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,BluetoothGattDescriptor descriptor,boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {

            Log.i(TAG, "onDescriptorWriteRequest device=" + device.toString());

            if (CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.i(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.i(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.i(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }
    };

    @Override
    protected void onDestroy() {
        if (mBluetoothLeAdvertiser!=null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        if(mBluetoothGattServer!=null) {
            mBluetoothGattServer.close();
        }
        super.onDestroy();
    }
}
