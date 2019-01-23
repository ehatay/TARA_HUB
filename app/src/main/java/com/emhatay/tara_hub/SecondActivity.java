package com.emhatay.tara_hub;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import io.github.controlwear.virtual.joystick.android.JoystickView;
import android.os.Handler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by emhatay on 1/31/18.
 */

public class SecondActivity extends AppCompatActivity {
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public String DEVICE_ADDRESS, DEVICE_NAME;
    private BluetoothAdapter mBtAdapter;

    static class IncomingHandler extends Handler {
        StringBuilder recDataString = new StringBuilder();
        public void handleMessage(android.os.Message msg)
        {
            if (msg.what == handlerState) {
                String readMessage = (String) msg.obj;
                recDataString.append(readMessage);
                int endOfLineIndex = recDataString.indexOf("\r\n");
                if (endOfLineIndex > 0) {
                    String dataInPrint = recDataString.substring(0, endOfLineIndex);
                    messageListController.AddMessage(dataInPrint, MessageListController.MESSAGE_TYPE.RECEIVED);
                    recDataString.delete(0,endOfLineIndex+1);
                }
            }
        }
    }

    static class MessageListController
    {
        ScrollView scrollView;
        LinearLayout message_list;
        public MessageListController(View messageList, ScrollView scrollView)
        {
            this.scrollView = scrollView;
            message_list = (LinearLayout) messageList;
        }
        public void AddMessage(String msg, MESSAGE_TYPE type)
        {
            TextView text = new TextView(message_list.getContext());
            text.setTextColor(GetColorFromMessageType(type));
            text.setText(msg);
            message_list.addView(text);
            scrollView.fullScroll(View.FOCUS_DOWN);
        }

        private int GetColorFromMessageType(MESSAGE_TYPE type)
        {
            if(type == MESSAGE_TYPE.RECEIVED)
                return Color.GREEN;
            else if(type == MESSAGE_TYPE.INFO)
                return Color.WHITE;
            else if(type == MESSAGE_TYPE.SENT)
                return Color.BLUE;
            else if(type == MESSAGE_TYPE.ERROR)
                return Color.RED;
            else if(type == MESSAGE_TYPE.WARNING)
                return Color.YELLOW;
            else
                return Color.GRAY;
        }
        public enum MESSAGE_TYPE
        {
            INFO,
            RECEIVED,
            SENT,
            WARNING,
            ERROR
        }

    }
    private static MessageListController messageListController;
    private static Handler bluetoothIn = new IncomingHandler();
    BluetoothDevice thisDevice;

    private ConnectedThread mConnectedThread;
    static final int handlerState = 0;                        //used to identify handler message
    private BluetoothSocket btSocket = null;
    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private TextView joystickText;
    private NavigationView nv;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        joystickText = findViewById(R.id.JoyStickText);
        joystickText.setText("Ang:\t0\nLin:\t0");
        DEVICE_NAME = getIntent().getStringExtra("name");
        DEVICE_ADDRESS = getIntent().getExtras().getString("address");        dl = findViewById(R.id.drawer_layout);
        messageListController = new MessageListController(findViewById(R.id.MessageList),(ScrollView) findViewById(R.id.message_view));
        t = new ActionBarDrawerToggle(this, dl, 0, 0);
        dl.addDrawerListener(t);
        t.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        messageListController.AddMessage("Connected to: " + DEVICE_NAME, MessageListController.MESSAGE_TYPE.INFO);
        nv = findViewById(R.id.nv);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        thisDevice = mBtAdapter.getRemoteDevice(DEVICE_ADDRESS);
        CheckBTState();
        ConnectToDevice(thisDevice);
        JoystickView joystickLeft = findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                joystickText.setText("Ang:\t" + angle + "\nLin:\t" + strength);
                mConnectedThread.write( angle + "~" + strength + "\r\n");
            }
        });
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                dl.closeDrawers();
                switch(id)
                {
                    case R.id.clear:
                        PrintToast("Console cleared");
                        messageListController.message_list.removeAllViews();
                        break;
                    case R.id.request_ip:
                        mConnectedThread.write("request_ip\r\n");
                        break;
                    case R.id.send_cmd:
                        PopupBluetoothSend();
                        break;
                    case R.id.connection_status:
                        ConnectionAcknowledgement();
                        break;
                    case R.id.disconnect:
                        PrintToast("Disconnecting from " + thisDevice.getName());
                        try {
                            btSocket.close();
                            CheckBTState();
                        }catch (IOException e)
                        {

                        }
                        break;
                    default:
                        return true;
                }
                return false;
            }
        });
        ConnectionAcknowledgement();
    }

    public void ConnectionAcknowledgement()
    {
        mConnectedThread.write("ack\r\n");
    }

    public void ExitActivity()
    {
        try {
            btSocket.close();
            CheckBTState();
        }catch (IOException e)
        {
            //insert code to deal with this
        }
    }


    public void PopupBluetoothSend()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        builder.setTitle("Input command..");
        /* Set up the input */
        final EditText input = new EditText(this);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.BLACK);

// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT); // | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String msg = input.getText().toString();
                if(msg.length() == 0) return;
                messageListController.AddMessage(msg, MessageListController.MESSAGE_TYPE.SENT);
                mConnectedThread.write(msg + "\r\n");
                PrintToast("Command: " + msg + " sent");
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    private void ConnectToDevice(BluetoothDevice device)
    {
        try {
            btSocket = createBluetoothSocket(device);
        }
        catch (IOException e)
        {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(t.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        CheckBTState();
    }

    @Override
    public void onBackPressed()
    {
        ExitActivity();
    }
    private void CheckBTState()
    {
        mBtAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBtAdapter==null) {
            PrintToast("Device does not support Bluetooth");
        } else {
            if (mBtAdapter.isEnabled()) {
            } else {
                //Prompt user to turn on Bluetooth
                finish();
            }
        }
        CheckIfConnected();
    }
    private void CheckIfConnected()
    {
        if(btSocket != null)
            if(!btSocket.isConnected())
                finish();
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public void PrintToast(String msg)
    {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e)
            {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
