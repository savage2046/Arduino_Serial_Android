package com.hoho.xh;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xh.usbarduino.R;
import xh.usbarduino.SerialConsoleActivity;

/**
 * Created by pc on 2016/2/1.
 * CdcAcmSerialDriver 237行有修改
 * SerialInputOutputManager 改成SerialInputManager+SerialOutputManager
 */
public class ArduinoManagerHandler extends Handler {
    private static final String TAG = ArduinoManagerHandler.class.getSimpleName();
    private ArduinoManager arduinoManager;
    private SerialConsoleActivity activity;

    public ArduinoManagerHandler(SerialConsoleActivity activity) {
        this.activity = activity;
        arduinoManager = new ArduinoManager(activity.getApplicationContext());
        this.postDelayed(runnable, 500);//开始查找
    }
    //隔5秒查找一次arduino，直达找到为止
    private Runnable runnable = new Runnable() {//不能做太“重”工作，会阻塞主线程
        public void run() {
            if (arduinoManager.searchDevice()) {
                Log.d(TAG,"找到设备,连接端口");
                startIoManager();
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
                activity.recieveArduinoData((String) msg.obj);
                break;
            case R.id.send_arduino_data_error:
                Log.w(TAG, "重新连接");
                restart();
                break;
        }
    }
    private SerialOutputManager serialOutputManager;

    public void write(String s) {
        byte[] bytes = s.getBytes();
        serialOutputManager = new SerialOutputManager(arduinoManager.getUsbSerialPort(),bytes,this);
        mExecutor.submit(serialOutputManager);//不能用handle.post，会影响主线程，另开线程池运行
    }

    public void close(){
        stopIoManager();
        arduinoManager.close();
    }
    public void restart(){
        close();
        this.postDelayed(runnable, 500);//开始查找
    }

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    private SerialInputManager mSerialIoManager;
    private final SerialInputManager.Listener mListener =
            new SerialInputManager.Listener() {
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
    public boolean isRuning(){
        return mSerialIoManager!=null;
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
            mSerialIoManager = new SerialInputManager(arduinoManager.getUsbSerialPort(), mListener);
            mExecutor.submit(mSerialIoManager);//不能用handle.post，会影响主线程，另开线程池运行
        }
    }
}
