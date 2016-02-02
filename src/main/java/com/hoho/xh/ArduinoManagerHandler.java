package com.hoho.xh;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xh.usbarduino.R;
import xh.usbarduino.SerialConsoleActivity;

/**
 * Created by pc on 2016/2/1.
 * CdcAcmSerialDriver 237行有修改
 * SerialInputOutputManager 44行有修改
 */
public class ArduinoManagerHandler extends Handler {
    private static final String TAG = ArduinoManagerHandler.class.getSimpleName();
    private ArduinoManager arduinoManager;
    private SerialConsoleActivity activity;

    public ArduinoManagerHandler(SerialConsoleActivity activity) {
        this.activity = activity;
        arduinoManager = new ArduinoManager(activity.getApplicationContext());
        this.postDelayed(runnable, 1000);//开始查找
    }

    //隔5秒查找一次arduino，直达找到为止
    private Runnable runnable = new Runnable() {
        public void run() {
            if (arduinoManager.searchDevice()) {
                onDeviceStateChange();
            } else {
                ArduinoManagerHandler.this.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.receive_arduino_data:
                // Log.w(TAG, "收到信息，正在处理");
                activity.updateList((String) msg.obj);
                break;
        }
    }

    public void write(String s) {
        byte[] bytes = s.getBytes();
        try {
            mSerialIoManager.writeAsync(bytes);
        } catch (Exception e) {
            Log.w(TAG, "串口发送失败");
        }
    }
    public void close(){
        stopIoManager();
        if (arduinoManager.getUsbSerialPort() != null) {
            try {
                arduinoManager.getUsbSerialPort().close();
            } catch (IOException e) {
                // Ignore.
            }
            arduinoManager.setUsbSerialPort(null);
        }
    }
    public void restart(){
        close();
        this.postDelayed(runnable, 1000);//开始查找
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    serialReadBuffer.append(data);
                    if (serialReadBuffer.size() > 0) {
                        Message.obtain(ArduinoManagerHandler.this, R.id.receive_arduino_data, serialReadBuffer.read()).sendToTarget();
                    }
                }
            };

    private SerialReadBuffer serialReadBuffer = new SerialReadBuffer();

    public void onDeviceStateChange() {
        serialReadBuffer.clean();
        stopIoManager();
        startIoManager();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (arduinoManager.getUsbSerialPort() != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(arduinoManager.getUsbSerialPort(), mListener);
            mExecutor.submit(mSerialIoManager);//不能用handle.post，会影响主线程，另开线程池运行
        }
    }
}