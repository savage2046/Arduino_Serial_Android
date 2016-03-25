package com.hoho.xh;

import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xh.usbarduino.R;


/**
 * Created by pc on 2016/2/1.
 * CdcAcmSerialDriver 237行有修改
 * SerialInputOutputManager 改成SerialInputManager+SerialOutputManager
 */
public class ArduinoManagerHandler extends Handler {
    private static final String TAG = "ArduinoManagerHandler";
    private ArduinoManager arduinoManager;
    private int searchCount = 0;

    public ArduinoManagerHandler(ArduinoManager manager) {
        arduinoManager = manager;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.arduino_receive_data:
                arduinoManager.receiveData((String) msg.obj);
                break;
            case R.id.arduino_send_data_error:
                Log.w(TAG, "重新连接");
                arduinoManager.restart();
                break;
            case R.id.arduino_search_device:
                searchCount++;
                Log.d(TAG, "search device,count=" + searchCount);
                UsbDevice device = arduinoManager.searchDevice();
                if (device != null) {
                    arduinoManager.getUsbPermission(device);
                } else {
                    this.sendEmptyMessageDelayed(R.id.arduino_search_device, 3000);
                }
                break;
        }
    }
}
