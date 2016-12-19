package com.example.bluetoothdemo.chatroom;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.bluetoothdemo.R;
import com.example.bluetoothdemo.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ChattingActivity extends AppCompatActivity {

    public static final int CHOOSE_DEVICE = 0X01010;
    @InjectView(R.id.et_msg)
    EditText mEtMsg;
    @InjectView(R.id.btn_send_msg)
    AppCompatButton mBtnSendMsg;
    @InjectView(R.id.listview_msg)
    ListView mListView;
    @InjectView(R.id.ll_edit_area)
    LinearLayout mLlEditArea;
    @InjectView(R.id.activity_chatting)
    RelativeLayout mActivityChatting;
    private List<String> msgList;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private ChattingService mChatService = null;
    private String mAddr;
    private BluetoothDevice mBluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);
        ButterKnife.inject(this);
        checkPermission();
        init();
    }

    private void init() {
        msgList = new ArrayList<>();
        mConversationArrayAdapter = new ArrayAdapter(ChattingActivity.this, R.layout.chatting_list_item, R.id.list_histor, msgList);
        mListView.setAdapter(mConversationArrayAdapter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mOutStringBuffer = new StringBuffer("");
        mChatService = new ChattingService(mBluetoothAdapter, mHandler, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBlueToothState();
        if (mChatService != null) {
            // 状态为空即为可以接收
            if (mChatService.getState() == ChattingService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(MainActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }


    /**
     *
     *
     * @param message
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != ChattingService.STATE_CONNECTED) {
            Toast.makeText(ChattingActivity.this, "没有连接任何设备", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mChatService.write(send);

            mOutStringBuffer.setLength(0);
            mEtMsg.setText(mOutStringBuffer);
        }
    }


    /**
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChattingService.STATE_CONNECTED:
//                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case ChattingService.STATE_CONNECTING:
//                            setStatus(R.string.title_connecting);
                            break;
                        case ChattingService.STATE_LISTEN:
                        case ChattingService.STATE_NONE:
//                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    msgList.add("Me:  " + writeMessage);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    msgList.add(mConnectedDeviceName + ":  " + readMessage);
                    mConversationArrayAdapter.notifyDataSetChanged();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != ChattingActivity.this) {
                        Toast.makeText(ChattingActivity.this, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != ChattingActivity.this) {
                        Toast.makeText(ChattingActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    // Android 6.0 需要动态申请权限
    private void checkPermission() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    0x001);
            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mBluetoothAdapter.enable();
        if (requestCode == CHOOSE_DEVICE) {
            if (resultCode == RESULT_OK) {
                connectDevice(data, false);
            }
        }
        //super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick({R.id.tv_choose_device, R.id.btn_send_msg})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_choose_device:
                Intent intent = new Intent(ChattingActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, CHOOSE_DEVICE);
                break;
            case R.id.btn_send_msg:
                sendMessage(mEtMsg.getText().toString());
                break;
        }
    }

    private void checkBlueToothState() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "该设备不支持蓝牙!", Toast.LENGTH_SHORT).show();
            return;
        } else if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothAdapter.enable();
        }
    }

    private static final int REQUEST_ENABLE_BT = 0x0001;

    private void setEnableDiscory() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }
}
