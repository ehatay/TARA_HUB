package com.emhatay.tara_hub;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.content.IntentFilter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import java.util.*;
import android.util.Log;
import android.widget.Button;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;

public class MainActivity extends Activity {
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        listView.removeAllViews();
                        bluetooth_switch.setChecked(false);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        listView.removeAllViews();
                        bluetooth_switch.setChecked(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        RefreshDeviceList(listView);
                        bluetooth_switch.setChecked(true);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        RefreshDeviceList(listView);
                        bluetooth_switch.setChecked(true);
                        break;
                }
            }
        }
    };

    private void RefreshDeviceList(View view)
    {
        listView.removeAllViews();
        Set<BluetoothDevice> pairedDevicees = mBluetoothAdapter.getBondedDevices();
        if (!mBluetoothAdapter.isEnabled()) {
            PrintToast("You need to enable bluetooth first!");
            return;
        }
        if (pairedDevicees.size() > 0) {
            for (BluetoothDevice dev : pairedDevicees)
                CreateDeviceButton(view, dev);
        } else
            PrintToast("No devices paired");
    }
    private void CreateDeviceButton(View view, final BluetoothDevice device)
    {
        Button deviceButton = new Button(view.getContext());
        deviceButton.setText(device.getName() + " : " + device.getAddress());
        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothButtonClick(device);
            }
        });

        listView.addView(deviceButton);
    }

    void BluetoothButtonClick(BluetoothDevice device)
    {
        PrintToast("Attempting to connect to : " + device.getName());
        Intent i = new
                Intent("com.emrehatay.passingdata.SecondActivity");
        i.putExtra("name", device.getName());
        Bundle extras = new Bundle();
        extras.putString("address", device.getAddress());
        i.putExtras(extras);
        startActivityForResult(i, 1);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        bluetooth_switch.setChecked(mBluetoothAdapter.isEnabled());
        RefreshDeviceList(listView);
    }

    Switch bluetooth_switch;
    LinearLayout listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!CheckBluetoothSupport()) {
            PrintToast("Bluetooth not supported on this device!");
            return;
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        bluetooth_switch = findViewById(R.id.bluetooth_switch);
        bluetooth_switch.setOnCheckedChangeListener(switchListener);
        bluetooth_switch.setChecked(mBluetoothAdapter.isEnabled());
        listView = findViewById(R.id.linear_layout);
        RefreshDeviceList(listView);
    }
    public boolean CheckBluetoothSupport()
    {
        return mBluetoothAdapter != null;
    }
    private CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b) {
                if (!mBluetoothAdapter.isEnabled()) {
                    PrintToast("Bluetooth Enabled!");
                    mBluetoothAdapter.enable();
                }
            } else {
                if (mBluetoothAdapter.isEnabled()) {
                    PrintToast("Bluetooth Disabled!");
                    mBluetoothAdapter.disable();
                }
            }
        }
    };

    public void PrintToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//---check if the request code is 1---
        if (requestCode == 1) {
//---if the result is OK---
            if (resultCode == RESULT_OK) {
//---get the result using getIntExtra()---
                Toast.makeText(this, Integer.toString(
                        data.getIntExtra("age3", 0)), Toast.LENGTH_SHORT).show();
//---get the result using getData()---
                Toast.makeText(this, data.getData().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
