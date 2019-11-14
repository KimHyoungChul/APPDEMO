package com.android.jingyi.btdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleUtil {
    private static final String TAG = "BleUtil";
    private static final long SCAN_PERIOD = 10000;

    public static String characterUUID1 = "00002a2b-0000-1000-8000-00805f9b34fb";//APP发送命令
    public static String characterUUID2 = "00002a0f-0000-1000-8000-00805f9b34fb";//BLE用于回复命令
    private static String descriptorUUID = "00001805-0000-1000-8000-00805f9b34fb";//BLE设备特性的UUID
//
//   public static String characterUUID1 = "00002af0-0000-1000-8000-00805f9b34fb";//APP发送命令
//    public static String characterUUID2 = "00002af1-0000-1000-8000-00805f9b34fb";//BLE用于回复命令
//    private static String descriptorUUID = "0000fee7-0000-1000-8000-00805f9b34fb";//BLE设备特性的UUID

    public static byte[] workModel = {0x02, 0x01};

    private Context mContext;
    private static BleUtil mInstance;

    //作为中央来使用和处理数据；
    private BluetoothGatt mGatt;

    private BluetoothManager manager;
    private BTUtilListener mListener;
    private BluetoothDevice mCurDevice;
    private BluetoothAdapter mBtAdapter;
    private List<BluetoothDevice> listDevice;
    private List<BluetoothGattService> serviceList;//服务
    private List<BluetoothGattCharacteristic> characterList;//特征

    private BluetoothGattService service;
    private BluetoothGattCharacteristic character1;
    private BluetoothGattCharacteristic character2;

    public static synchronized BleUtil getInstance() {
        if (mInstance == null) {
            mInstance = new BleUtil();
        }
        return mInstance;
    }

    public void setContext(Context context) {
        mContext = context;
        init();
    }

    public void init() {
        listDevice = new ArrayList<>();
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast("BLE不支持此设备");
        }
        manager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBtAdapter = manager.getAdapter();
        }
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            mBtAdapter.enable();
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, final int i, final byte[] bytes) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!listDevice.contains(bluetoothDevice)) {
                        listDevice.add(bluetoothDevice);
                        mListener.onLeScanDevices(listDevice);
                        Log.i(TAG, "device="+bluetoothDevice.toString()
                                +" name="+bluetoothDevice.getName()
                                +" type="+bluetoothDevice.getType()
                                +" uuid="+bluetoothDevice.getUuids()
                                +" i="+i
                                //+" bytes="+new String(bytes, Charset.forName("UTF-8"))
                        );
                    }
                }
            });
        }
    };

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                    Log.i(TAG, "run:stop");
                }
            }, SCAN_PERIOD);
            startScan();
            Log.i(TAG, "start");
        } else {
            stopScan();
            Log.i(TAG, "stop");
        }
    }


    //开始扫描BLE设备
    private void startScan() {
        //mBtAdapter.startLeScan(mLeScanCallback);


//        UUID[] uuids = {UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")};
//        mBtAdapter.startLeScan(uuids,mLeScanCallback);

//        mBtAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
//            @Override
//            public void onScanResult(int callbackType, ScanResult result) {
//                BluetoothDevice device = result.getDevice();
//                Log.i(TAG, "device="+device.toString()+" name="+result.getDevice().getName());
//            }
//
//            @Override
//            public void onBatchScanResults(List<ScanResult> results) {
//                Log.i(TAG, "results="+results.size());
//            }
//
//            @Override
//            public void onScanFailed(int errorCode) {
//                Log.i(TAG, "errorCode="+errorCode);
//            }
//        });

        mListener.onLeScanStart();
    }

    //停止扫描BLE设备
    private void stopScan() {
        mBtAdapter.stopLeScan(mLeScanCallback);
        mListener.onLeScanStop();
    }

    //返回中央的状态和周边提供的数据
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.i(TAG, "onConnectionStateChange status="+status+" newState="+newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "STATE_CONNECTED");
                    mListener.onConnected(mCurDevice);
                    gatt.discoverServices(); //搜索连接设备所支持的service
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mListener.onDisConnected(mCurDevice);
                    disConnGatt();
                    Log.i(TAG, "STATE_DISCONNECTED");
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    mListener.onConnecting(mCurDevice);
                    Log.i(TAG, "STATE_CONNECTING");
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    mListener.onDisConnecting(mCurDevice);
                    Log.i(TAG, "STATE_DISCONNECTING");
                    break;
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                serviceList = gatt.getServices();
                for (int i = 0; i < serviceList.size(); i++) {
                    BluetoothGattService theService = serviceList.get(i);

                    Log.i(TAG, "ServiceName:" + theService.getUuid());
                    characterList = theService.getCharacteristics();
                    for (int j = 0; j < characterList.size(); j++) {
                        String uuid = characterList.get(j).getUuid().toString();
                        Log.i(TAG, "---CharacterName:" + uuid);
                        if (uuid.equals(characterUUID1)) {
                            Log.i(TAG, "character1 characterUUID1:" + characterUUID1);
                            character1 = characterList.get(j);
                        } else if (uuid.equals(characterUUID2)) {
                            Log.i(TAG, "character2 characterUUID2:" + characterUUID2);
                            character2 = characterList.get(j);
                        }
                    }
                    if (character1 != null) {
                        setNotification1();
                    }
                    if (character2 != null) {
                        setNotification2();
                    }
                }
            }
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead status="+status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                Log.i(TAG, "onCharacteristicRead success value="+new String(value, Charset.forName("UTF-8")));
            }

            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite status="+status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String value = new String(characteristic.getValue(), Charset.forName("UTF-8"));
                Log.i(TAG, "onCharacteristicWrite success value="+value);
            }

            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.i(TAG, "onCharacteristicChanged value="+new String(value, Charset.forName("UTF-8")));

            receiveData(characteristic);
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    //获取设备指定的特征中的特性,其中对其进行监听, setCharacteristicNotification与上面的回调onCharacteristicChanged进行一一搭配
    private void setNotification1() {
        mGatt.setCharacteristicNotification(character1, true);
//        BluetoothGattDescriptor descriptor1 = character1.getDescriptor(UUID.fromString(descriptorUUID));
//        descriptor1.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//
//        mGatt.writeDescriptor(descriptor1);
    }

    private void setNotification2() {
        mGatt.setCharacteristicNotification(character2, true);
//        BluetoothGattDescriptor descriptor = character2.getDescriptor(UUID.fromString(descriptorUUID));
//        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        mGatt.writeDescriptor(descriptor);
    }

    //接收数据,对其进行处理
    private void receiveData(BluetoothGattCharacteristic ch) {
        Log.i(TAG, "receiveData ch="+ch);
        byte[] bytes = ch.getValue();
        int cmd = bytes[0];
        int agree = bytes[1];
        switch (cmd) {
            case 1:
                mListener.onStrength(agree);
                Log.i(TAG, "手机通知BLE设备强度:" + agree);
                break;
            case 2:
                mListener.onModel(agree);
                Log.i(TAG, "工作模式:" + agree);
                break;
            case 3:
                mListener.onStrength(agree);
                Log.i(TAG, "设备自身通知改变强度:" + agree);
                break;
        }
    }

    //连接设备
    public void connectLeDevice(int devicePos) {
        Log.i(TAG, "connectLeDevice pos="+devicePos);
        //mBtAdapter.stopLeScan(mLeScanCallback);
       // mCurDevice = listDevice.get(devicePos);

        mCurDevice = mBtAdapter.getRemoteDevice("11:22:33:44:55:74");

        checkConnGatt();
    }

    //发送进入工作模式请求
    public void sendWorkModel() {
        if (character1 != null) {
            character1.setValue(workModel);
            mGatt.writeCharacteristic(character1);
        }
    }

    //发送强度
    public void sendStrength(int strength) {
        byte[] strengthModel = {0x01, (byte) strength};
        Log.i(TAG, "sendStrength character1="+character1);
        if (character1 != null) {
            character1.setValue(strengthModel);
            mGatt.writeCharacteristic(character1);
        }
    }

    //发送强度
    public void sendStrengthRead(int strength) {
        Log.i(TAG, "sendStrength character1="+character1);
        if (character1 != null) {
            mGatt.readCharacteristic(character1);
        }
    }

    //发送强度
    public void sendStrengWrite(int strength) {
        byte[] strengthModel = "jingyi123456789".getBytes();
        Log.i(TAG, "send length="+strengthModel.length);
        Log.i(TAG, "sendStrength character2="+character2);
        if (character2 != null) {
            character2.setValue(strengthModel);
            mGatt.writeCharacteristic(character2);
        }
    }

    //检查设备是否连接了
    private void checkConnGatt() {
        Log.i(TAG, "checkConnGatt mGatt="+mGatt+" mCurDeviceName="+mCurDevice.getName()+" mCurDeviceAddress="+mCurDevice.getAddress());
        if (mGatt == null) {
            mGatt = mCurDevice.connectGatt(mContext, false, mGattCallback);
            mGatt.connect();
            mListener.onConnecting(mCurDevice);
        } else {
            mGatt.connect();
            mGatt.discoverServices();
        }
    }

    //  断开设备连接
    private void disConnGatt() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
            listDevice = new ArrayList<>();
            mListener.onLeScanDevices(listDevice);
        }
    }

    private void showToast(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void setBTUtilListener(BTUtilListener listener) {
        mListener = listener;
    }


    public interface BTUtilListener {
        void onLeScanStart(); // 扫描开始

        void onLeScanStop();  // 扫描停止

        void onLeScanDevices(List<BluetoothDevice> listDevice); //扫描得到的设备

        void onConnected(BluetoothDevice mCurDevice); //设备的连接

        void onDisConnected(BluetoothDevice mCurDevice); //设备断开连接

        void onConnecting(BluetoothDevice mCurDevice); //设备连接中

        void onDisConnecting(BluetoothDevice mCurDevice); //设备连接失败

        void onStrength(int strength); //给设备设置强度

        void onModel(int model); //设备模式
    }

}
