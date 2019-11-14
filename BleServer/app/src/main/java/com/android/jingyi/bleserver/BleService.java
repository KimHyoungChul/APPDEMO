package com.android.jingyi.bleserver;

import android.app.Service;
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
import android.bluetooth.le.ScanFilter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BleService extends Service {
    private static final String TAG = "BleService";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    //先定几个服务类型的UUID

//    public static UUID BLE_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
//
//    public static UUID READ_CHARACTERISTIC    = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");
//
//    public static UUID WRITE_CHARACTERISTIC = UUID.fromString("00002a0f-0000-1000-8000-00805f9b34fb");
//
//    public static UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID BLE_SERVICE = java.util.UUID.fromString
            ("e89c82e7-aa9d-e7a7-91e6-8a80206d6a83");
    public static final UUID READ_CHARACTERISTIC = java.util.UUID.fromString
            ("e89c82e7-aa9d-e7a7-91e6-8a80206d6a73");
    public static final UUID WRITE_CHARACTERISTIC = java.util.UUID.fromString
            ("e89c82e7-aa9d-e7a7-91e6-8a80206d6a74");
    public static final UUID CONFIG_DESCRIPTOR = java.util.UUID.fromString
            ("e89c82e7-aa9d-e7a7-91e6-8a80206d6a72");

    public BleService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        openAndInitBt();
        if (mBluetoothManager != null && mBluetoothAdapter != null) {
            createGattServer();
            startAdvertising();
        }
    }

    public void updateCharacteristic() {
        if (mBluetoothGattServer == null) {
            return;
        }
        for (BluetoothDevice d : mRegisteredDevices) {
            BluetoothGattCharacteristic readCharacteristic = mBluetoothGattServer
                    .getService(BLE_SERVICE)
                    .getCharacteristic(READ_CHARACTERISTIC);
            byte[] data = "1".getBytes();
            readCharacteristic.setValue("rr");
            mBluetoothGattServer.notifyCharacteristicChanged(d, readCharacteristic, true);
            Log.i(TAG, "updateNotifyValue ... d="+d.toString());

            BluetoothGattCharacteristic writeCharacteristic = mBluetoothGattServer
                    .getService(BLE_SERVICE)
                    .getCharacteristic(WRITE_CHARACTERISTIC);
            writeCharacteristic.setValue("ww");
            mBluetoothGattServer.notifyCharacteristicChanged(d, writeCharacteristic, true);
            Log.i(TAG, "updateNotifyValue ... d="+d.toString());
        }
    }

    //1.初始化并打开蓝牙
    private void openAndInitBt(){
        mBluetoothManager=(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter==null){return;}//不支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return ;
        }
        //.判断蓝牙是否打开
        if (!mBluetoothAdapter.enable()) {
            mBluetoothAdapter.enable();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createGattServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer != null) {
            Log.i(TAG, "mBluetoothGattServer=" + mBluetoothGattServer.toString());

            BluetoothGattService service = new BluetoothGattService(BLE_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

            //1111111
            BluetoothGattCharacteristic readCharater = new BluetoothGattCharacteristic(READ_CHARACTERISTIC,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ);

            BluetoothGattDescriptor configDescriptorRead = new BluetoothGattDescriptor(CONFIG_DESCRIPTOR,
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            configDescriptorRead.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            readCharater.addDescriptor(configDescriptorRead);
            //

            //22222222
            BluetoothGattCharacteristic writeCharater = new BluetoothGattCharacteristic(WRITE_CHARACTERISTIC,
                    BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE);

            BluetoothGattDescriptor configDescriptorWrite = new BluetoothGattDescriptor(CONFIG_DESCRIPTOR,
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            configDescriptorRead.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            writeCharater.addDescriptor(configDescriptorWrite);
            //

            service.addCharacteristic(readCharater);
            service.addCharacteristic(writeCharater);

            mBluetoothGattServer.addService(service);
        }
    }

    private void startAdvertising() {
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser != null) {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();
            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true)
                    .addServiceUuid(new ParcelUuid(BLE_SERVICE))//绑定服务uuid
                    .build();

            mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess.. settingsInEffect="+settingsInEffect.toString());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i(TAG, "onStartFailure.. errorCode="+errorCode);
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange status="+status+" newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mRegisteredDevices.add(device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.i(TAG, "onServiceAdded");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            Log.i(TAG, "onCharacteristicReadRequest device="+device.toString());

            if (READ_CHARACTERISTIC.equals(characteristic.getUuid())) {
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,0,"123".getBytes());
                Log.i(TAG, "onCharacteristicReadRequest sendResponse");
            }
            //super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            Log.i(TAG, "onCharacteristicWriteRequest device="+device.toString());
            if (WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
                Log.i(TAG, "onCharacteristicWriteRequest value=" + new String(value, Charset.forName("UTF-8")));
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,0,"456".getBytes());
            }
            //super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, "456".getBytes());
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.i(TAG, "onDescriptorReadRequest");
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }
 
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i(TAG, "onDescriptorWriteRequest");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.i(TAG, "onExecuteWrite");
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.i(TAG, "onNotificationSent");
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Log.i(TAG, "onMtuChanged");
            super.onMtuChanged(device, mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyUpdate");
            super.onPhyUpdate(device, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            Log.i(TAG, "onPhyRead");
            super.onPhyRead(device, txPhy, rxPhy, status);
        }
    };

    @Override
    public void onDestroy() {
        if (mBluetoothLeAdvertiser!=null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        if(mBluetoothGattServer!=null) {
            mBluetoothGattServer.close();
        }
        super.onDestroy();
    }

    private BleBinder binder = new BleBinder();
    public class BleBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }
}
