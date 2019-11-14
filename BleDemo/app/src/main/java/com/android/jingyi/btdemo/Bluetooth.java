package com.android.jingyi.btdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class Bluetooth {
    private String TAG = "Bluetoothjingyi";

    public static int METHOD_FAILED = 109;
    public static int CONNECT_FAILED = 110;
    public static int CONNECT_SUCESS = 111;
    public static int READ_FAILED = 112;
    public static int WRITE_FAILED = 113;
    public static int DATA = 114;

    BluetoothDevice device;
    Handler handler;
    BluetoothSocket socket;
    boolean isConnect = false;

    public Bluetooth(BluetoothDevice device, Handler handler) {
        this.device = device;
        this.handler = handler;
    }

    public void setState(int message) {
        handler.sendEmptyMessage(message);
    }

    public void sendMessage(String message) {
        if (isConnect) {
            try {
                Log.i(TAG, "sendMessage message="+message);
                OutputStream outStream = socket.getOutputStream();
                outStream.write(getHexBytes(message));
            } catch (IOException e) {
                setState(WRITE_FAILED);
                Log.e("TAG", e.toString());
            }
            try {
                InputStream inputStream = socket.getInputStream();
                int data;
                while (true) {
                    try {
                        data = inputStream.read();
                        Message msg = handler.obtainMessage();
                        msg.what = DATA;
                        msg.arg1 = data;
                        Log.i(TAG, "inputStream data="+data);
                        handler.sendMessage(msg);
                    } catch (IOException e) {
                        setState(READ_FAILED);
                        Log.e("TAG", e.toString());
                        break;
                    }
                }
            } catch (IOException e) {
                setState(WRITE_FAILED);
                Log.e("TAG", e.toString());
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("TAG", e.toString());
            }
        }
    }

    private byte[] getHexBytes(String message) {
        int len = message.length() / 2;
        char[] chars = message.toCharArray();
        String[] hexStr = new String[len];
        byte[] bytes = new byte[len];
        for (int i = 0, j = 0; j < len; i += 2, j++) {
            hexStr[j] = "" + chars[i] + chars[i + 1];
            bytes[j] = (byte) Integer.parseInt(hexStr[j], 16);
        }
        return bytes;
    }

    public void connect(String message) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothSocket tmp = null;
                Method method;
                try {
                    method = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                    tmp = (BluetoothSocket)method.invoke(device, 1);
                }catch (Exception e) {
                    setState(METHOD_FAILED);
                    Log.i(TAG, "getmethod e="+e.getMessage());
                }
                socket = tmp;
                try {
                    socket.connect();
                    isConnect = true;
                    Log.i(TAG, "connect isConnect="+isConnect);
                } catch (Exception e) {
                    setState(CONNECT_FAILED);
                    Log.i(TAG, "connect e="+e.getMessage());
                }
            }
        });
        thread.start();
    }

}
