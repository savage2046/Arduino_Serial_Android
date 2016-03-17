package com.hoho.xh;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xh.usbarduino.R;
import xh.usbarduino.SerialConsoleActivity;

/**
 * Created by pc on 2016/2/1.
 */
public class ArduinoManager {
    private final String TAG = "ArduinoManager";
    private final int BAUD_RATE = 115200;//波特率
    private UsbSerialPort usbSerialPort;
    private Context context;
    private ArduinoManagerHandler handler;
    private SerialConsoleActivity activity;
    private final UsbManager usbManager;

    public enum Command {
        READ_DISTANCE("read distance");
        private String string;

        Command(String s) {
            string = s;
        }

        public String value() {
            return string;
        }
    }

    public ArduinoManager(SerialConsoleActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        handler = new ArduinoManagerHandler(this);
        start();
    }

    public void start() {
        handler.sendEmptyMessageDelayed(R.id.arduino_search_device, 500);//开始查找usb设备
    }

    private UsbSerialDriver usbSerialDriver;

    public UsbDevice searchDevice() {
        final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        Log.d(TAG, "driver number=" + drivers.size());
        UsbDevice device = null;
        usbSerialDriver = null;
        if (!drivers.isEmpty()) {
            usbSerialDriver = drivers.get(0);
            device = usbSerialDriver.getDevice();
        }
        return device;
    }

    public void getUsbPermission(UsbDevice device) {
        activity.getUsbPermission(device);
    }

    public boolean connectDevice() {
        if (usbSerialDriver != null) {
            usbSerialPort = null;
            List<UsbSerialPort> ports = usbSerialDriver.getPorts();
            if (ports.isEmpty()) {
                Log.w(TAG, "no port for this driver");
                return false;
            }
            usbSerialPort = ports.get(0);
            Log.d(TAG, "找到端口,尝试连接");
            UsbDeviceConnection connection = usbManager.openDevice(usbSerialPort.getDriver().getDevice());
            if (connection == null) {
                Log.w(TAG, "Opening device failed");
                return false;
            }
            try {
                usbSerialPort.open(connection);
                usbSerialPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    usbSerialPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                usbSerialPort = null;
                return false;
            }
            startIoManager();
            Log.d(TAG, "打开端口");
            return true;
        }
        return false;
    }

    public void receiveData(String s) {
        activity.receiveArduinoData(s);
    }

    public void close() {
        Log.d(TAG, "关闭arduino端口");
        handler.removeMessages(R.id.arduino_search_device);//停止查找
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
        }
        usbSerialPort = null;
        stopIoManager();
    }

    public void restart() {
        close();
        Log.d(TAG, "restart search");
        start();
    }

    private final ExecutorService mExecutor = Executors.newCachedThreadPool();

    private SerialOutputManager serialOutputManager = new SerialOutputManager(handler);
    public void write(String s) {
        byte[] bytes = s.getBytes();
        UsbSerialPort port = usbSerialPort;
        if (port == null) {
            Log.i(TAG, "还未连接arduino,不能发送");
            return;
        }
        serialOutputManager.setPort(port);
        serialOutputManager.setData(bytes);
        mExecutor.submit(serialOutputManager);//不能用handle.post，会影响主线程，另开线程池运行
    }

    private SerialInputManager serialInputManager;
    private final SerialInputManager.Listener mListener =
            new SerialInputManager.Listener() {
                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    serialReadBuffer.append(data);
                    StringBuffer stringBuffer = new StringBuffer();
                    while (serialReadBuffer.size() > 0) {
                        stringBuffer.append(serialReadBuffer.read());
                    }
                    if (stringBuffer.length() > 0) {
                        Message.obtain(handler, R.id.arduino_receive_data, stringBuffer.toString()).sendToTarget();
                    }
                }
            };

    private SerialReadBuffer serialReadBuffer = new SerialReadBuffer();

    private void stopIoManager() {
        if (serialInputManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            serialInputManager.stop();
            serialInputManager = null;
        }
    }

    private void startIoManager() {
        if (usbSerialPort != null) {
            Log.i(TAG, "Starting io manager ..");
            serialInputManager = new SerialInputManager(usbSerialPort, mListener);
            mExecutor.submit(serialInputManager);//不能用handle.post，会影响主线程，另开线程池运行
        }
    }
}
