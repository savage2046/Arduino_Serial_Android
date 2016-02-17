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
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.xh.ArduinoManager;
import com.hoho.xh.ArduinoManagerHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
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
        arduinoManager.close();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arduinoManager == null) {
            arduinoManager = new ArduinoManager(this);
        }
    }

    public void receiveArduinoData(String s) {
        listViewData.add(0, s);
        if (listViewData.size() > listViewMax) {
            listViewData.remove(listViewMax);
        }
        listViewAdapter.notifyDataSetChanged();
    }
}
