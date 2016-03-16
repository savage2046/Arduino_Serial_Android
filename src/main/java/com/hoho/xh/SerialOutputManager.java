package com.hoho.xh;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;

import xh.usbarduino.R;


/**
 * Created by pc on 2016/2/2.
 */
public class SerialOutputManager implements Runnable {
    private static final String TAG = SerialOutputManager.class.getSimpleName();

    private final boolean DEBUG = true;

    private final int TIMEOUT_MILLIS = 200;

    private final int BUFSIZ = 64;//arduino 缓冲长度
    private UsbSerialPort mDriver;
    private final Handler handler;

    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

    public SerialOutputManager(Handler handler) {
        this.handler = handler;
    }
    public void setPort(UsbSerialPort port) {
        this.mDriver = port;
    }

    public void setData(byte[] data) {
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
            if (writeAct(outBuff) < 0) {//出错，尝试第二次
                SystemClock.sleep(100);
                if (writeAct(outBuff) < 0) {
                    Log.w(TAG, "Writing unknow error");
                    handler.sendEmptyMessage(R.id.send_arduino_data_error);
                }
            }
        }
    }

    private int writeAct(byte[] outBuff) {
        try {
            return mDriver.write(outBuff, TIMEOUT_MILLIS);
        } catch (IOException e) {
            Log.w(TAG, "Writing end,Run ending due to exception: " + e.getMessage(), e);
        }
        return -1;
    }
}
