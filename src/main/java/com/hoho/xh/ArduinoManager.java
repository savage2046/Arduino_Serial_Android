package com.hoho.xh;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

import xh.usbarduino.SerialConsoleActivity;

/**
 * Created by pc on 2016/2/1.
 */
public class ArduinoManager {
    private final String TAG = ArduinoManager.class.getSimpleName();
    private final int BAUD_RATE = 115200;//波特率
    private UsbSerialPort usbSerialPort;
    private Context context;
    private ArduinoManagerHandler handler;
    private SerialConsoleActivity activity;

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
        this.context =activity.getApplicationContext();
        this.activity=activity;
        handler=new ArduinoManagerHandler(this);
    }

    public void receiveData(String s){
        activity.receiveArduinoData(s);
    }
    public void write(String s){
        handler.write(s);
    }

    public boolean searchDevice() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Log.d(TAG, "开始查找arduino");
        final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        usbSerialPort = null;
        if (!drivers.isEmpty()) {
            List<UsbSerialPort> ports = drivers.get(0).getPorts();
            if (!ports.isEmpty()) {
                usbSerialPort = ports.get(0);
                Log.d(TAG, "找到端口,尝试连接");
            }
        }
        if (usbSerialPort != null) {
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
            Log.i(TAG, "打开端口");
            return true;
        }
        return false;
    }

    public void close() {
        Log.i(TAG, "关闭arduino端口");
        try {
            usbSerialPort.close();
        } catch (IOException e2) {
            // Ignore.
        }
        usbSerialPort = null;
    }

    public UsbSerialPort getUsbSerialPort() {
        return this.usbSerialPort;
    }

    public void setUsbSerialPort(UsbSerialPort port) {
        this.usbSerialPort = port;
    }
}
