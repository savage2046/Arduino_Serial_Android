/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package xh.usbarduino;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.xh.ArduinoManager;
import com.hoho.xh.ArduinoManagerHandler;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 *
 * 改进获得权限方式，同时用静态（AndroidManifest.xml）和动态（Broadcast）用两种方式
 * 保证无论何时拔插都能正确获得权限
 *
 */
public class SerialConsoleActivity extends Activity {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    private TextView mTitleTextView;
    private EditText editText;
    private Button button;

    private ListView listView;
    private final int listViewMax = 10;
    private List<String> listViewData = new ArrayList<String>();
    private ArrayAdapter<String> listViewAdapter;

    private ArduinoManager arduinoManager;
    private UsbManager usbManager;

    public PendingIntent permissionIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        editText = (EditText) findViewById(R.id.editText);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(buttonOnClickListener);

        listView = (ListView) findViewById(R.id.listView);
        listViewAdapter = new ArrayAdapter<String>(SerialConsoleActivity.this, android.R.layout.simple_list_item_1, listViewData);
        listView.setAdapter(listViewAdapter);

        usbManager= (UsbManager) getSystemService(Context.USB_SERVICE);

        arduinoManager=new ArduinoManager(this);

        //获取USB权限
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        IntentFilter filter1=new IntentFilter("android.hardware.usb.action.USB_DEVICE_DETACHED");
        registerReceiver(mUsb_Detached_Receiver,filter1);
    }

    Button.OnClickListener buttonOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s = editText.getText().toString();
            if (s.isEmpty()) {
                arduinoManager.write(ArduinoManager.Command.READ_DISTANCE.value());
                return;
            }
            arduinoManager.write(s);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
//        arduinoManager.close();
//        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsb_Detached_Receiver);
        arduinoManager.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void receiveArduinoData(String s) {
        listViewData.add(0, s);
        if (listViewData.size() > listViewMax) {
            listViewData.remove(listViewMax);
        }
        listViewAdapter.notifyDataSetChanged();
    }

    public void getUsbPermission(UsbDevice device){
        Log.d(TAG,"request permission");
        usbManager.requestPermission(device,permissionIntent);
    }

    public static final String ACTION_USB_PERMISSION ="com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"action="+action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG,"尝试连接arduino");
                        arduinoManager.connectDevice();
                    }else{
                        Log.d(TAG, "permission denied for device");
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsb_Detached_Receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"detach");
            String action = intent.getAction();
            if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
                arduinoManager.restart();
            }
        }
    };
}
