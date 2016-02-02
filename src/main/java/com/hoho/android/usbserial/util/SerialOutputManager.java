package com.hoho.android.usbserial.util;

import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by pc on 2016/2/2.
 */
public class SerialOutputManager implements Runnable {
    private static final String TAG = SerialOutputManager.class.getSimpleName();

    private static final boolean DEBUG = true;

    private static final int WAIT_MILLIS = 200;

    private static final int BUFSIZ = 64;//arduino 缓冲长度
    private final UsbSerialPort mDriver;
    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

    public SerialOutputManager(UsbSerialPort driver,byte[] data) {
        mDriver = driver;
        write(data);
    }
    public void write(byte[] data) {
        synchronized (mWriteBuffer) {
            mWriteBuffer.put(data);
        }
    }
    @Override
    public void run() {
        int len;
        // Handle outgoing data.
        byte[] outBuff = null;
        synchronized (mWriteBuffer) {
            len = mWriteBuffer.position();
            if (len > 0) {
                outBuff = new byte[len];
                mWriteBuffer.rewind();
                mWriteBuffer.get(outBuff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (outBuff != null) {
            if (DEBUG) {
                Log.d(TAG, "Writing data len=" + len);
            }
            try{
                mDriver.write(outBuff, WAIT_MILLIS);
            }catch (IOException e){
                Log.w(TAG, "Writing end,Run ending due to exception: " + e.getMessage(), e);
            }
        }
    }
}
