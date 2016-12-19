package com.example.bluetoothdemo.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.bluetoothdemo.R;
import com.example.bluetoothdemo.adapter.DeviceListAdapter;
import com.example.bluetoothdemo.chatroom.ChattingActivity;
import com.example.bluetoothdemo.chatroom.ChattingService;
import com.example.bluetoothdemo.chatroom.Constants;
import com.example.bluetoothdemo.threads.BlueToothConnectThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final int REQUEST_ENABLE_BT = 0x0001;
    private static final int MESSAGE_READ = 0x002;
    private static String NAME;
    @InjectView(R.id.list_view)
    ListViewCompat mListView;
    @InjectView(R.id.list_bonded)
    ListViewCompat mListViewBonded;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter mArrayAdapter;
    private List<String> mData;
    private List<Map<Integer, String>> mList;
    private List<Map<Integer, String>> mListBonded;
    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private String TAG = "蓝牙Demo";
    private Set<BluetoothDevice> mSet;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        init();
        initBluetooh();
        initBondedList();
        registerReceiver();
    }

    private void init() {
        mList = new ArrayList<>();
        mListBonded = new ArrayList<>();
        mDeviceListAdapter = new DeviceListAdapter(MainActivity.this, mList);
        mListView.setAdapter(mDeviceListAdapter);
        mListView.setOnItemClickListener(this);
    }


    private void initBluetooh() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBlueToothState();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mSet = mBluetoothAdapter.getBondedDevices();
//        mBluetoothAdapter.setName("有一天我心血来潮骑它去赶集...");
//        mBluetoothAdapter.setName("我有一只小毛驴我从来也不骑...");
        startServer();
    }

    private void initBondedList() {
        Map<Integer, String> map = null;
        for (BluetoothDevice d : mSet) {
            map = new HashMap<>();
            map.put(0, d.getName());
            map.put(1, d.getAddress());
            mListBonded.add(map);
        }
        DeviceListAdapter adapter = new DeviceListAdapter(MainActivity.this, mListBonded);
        mListViewBonded.setAdapter(adapter);

    }

    @OnClick({R.id.btn_stop_scan, R.id.btn_start_scan, R.id.btn_start_scan_le,
            R.id.btn_stop_scan_le, R.id.btn_stop_scan_advertiser, R.id.btn_start_scan_advertiser})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_stop_scan:
                checkBlueToothState();
                mBluetoothAdapter.cancelDiscovery();
                break;
            case R.id.btn_start_scan:
                checkBlueToothState();
                mBluetoothAdapter.startDiscovery();
                break;
            case R.id.btn_start_scan_le:
                checkBlueToothState();
                startLeScan();
                break;
            case R.id.btn_stop_scan_le:
                checkBlueToothState();
                stopLeScan();
                break;
            case R.id.btn_start_scan_advertiser:
                checkBlueToothState();
                startLeAdvertiser();
                break;
            case R.id.btn_stop_scan_advertiser:
                checkBlueToothState();
                stopLeAdvertiser();
                break;
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void unRegisterReceiver() {
        this.unregisterReceiver(mReceiver);
    }


    // 广播接收器,接收扫描的结果或者扫描结束的通知
    // 如果扫描到设备,Intent对象中会有设备实例
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //发现设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                从Intent中获取设备对象
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 已经配对的和没有配对的分开显示  不重复添加
                Map<Integer, String> map = new HashMap<>();
                map.put(0, device.getName());
                map.put(1, device.getAddress());
                mList.add(map);
                mDeviceListAdapter.notifyDataSetChanged();
//                if (mData.indexOf(device.getName() + ":\t" + device.getAddress()) < 0) {
//                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                        mData.add(device.getName() + "\t" + device.getAddress());
//                        Log.d("发现设备----", device.getName() + "\t" + device.getAddress());
//                        mArrayAdapter.notifyDataSetChanged();
//                    } else {
//                        mData.add(device.getName() + "\t" + device.getAddress() + "已经绑定过了");
//                    }
//                }
                //扫描结束或者手动cancel的话 会执行
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "扫描结束", Toast.LENGTH_SHORT).show();
                Log.d("未发现蓝牙设备", "未发现蓝牙设备");
            }
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClient();
        stopServer();
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.disable();
        unRegisterReceiver();
    }


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


    android.bluetooth.le.ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            Map<Integer, String> map = new HashMap<>();
            map.put(0, "LE :  " + device.getName());
            map.put(1, device.getAddress());
            mList.add(map);
//            mData.add("LE :  " + device.getName() + ":\t" + device.getAddress());
//            mArrayAdapter.notifyDataSetChanged();
            mDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this, "扫描出错" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    public static final UUID uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private BluetoothDevice device;
    private String paintAddress;

    private void doConnect() {
        new BlueToothConnectThread(paintAddress).start();
    }

    private void startLeScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mBluetoothLeScanner == null) {
                if (mBluetoothAdapter == null) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            mBluetoothLeScanner.startScan(scanCallback);
        } else {
            Toast.makeText(this, "Android版本数太低,请升级后重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLeScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (mBluetoothLeScanner == null) {
                if (mBluetoothAdapter == null) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                }
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            mBluetoothLeScanner.stopScan(scanCallback);
        } else {
            Toast.makeText(this, "Android版本数太低,请升级后重试", Toast.LENGTH_SHORT).show();
        }
    }


    private AdvertiseCallback advertiseCallback;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private void startLeAdvertiser() {
        if (mBluetoothLeAdvertiser == null) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setTimeout(12000);
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Toast.makeText(MainActivity.this, "LE 启动成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Toast.makeText(MainActivity.this, "LE 启动失败", Toast.LENGTH_SHORT).show();
            }
        };

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        mBluetoothLeAdvertiser.startAdvertising(builder.build(), dataBuilder.build(), advertiseCallback);
    }

    private void stopLeAdvertiser() {
        if (mBluetoothLeAdvertiser == null) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        }
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
    }


    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    public void onItemClick(AdapterView<?> view, View view1, int i, long l) {
        Map<Integer, String> map = (Map<Integer, String>) mListView.getAdapter().getItem(i);
        paintAddress = map.get(1);
        startClient(mBluetoothAdapter.getRemoteDevice(paintAddress));
    }


    private BluetoothSocket bluetoothSocket;
    private boolean isConnecting = false;
    private boolean isConnected = false;

    private int connetTime = 0;

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙打开成功!", Toast.LENGTH_SHORT).show();
                initBluetooh();
                initBondedList();
                registerReceiver();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkBlueToothState() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "该设备不支持蓝牙!", Toast.LENGTH_SHORT).show();
            return;
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void setEnableDiscory() {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }


    private Handler mHandler = new Handler() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        @Override
        public void handleMessage(Message msg) {

            if (msg.what == MESSAGE_READ) {
                int len = msg.arg1;
                byte[] bys = (byte[]) msg.obj;
                if (len != -1) {
                    output.write(bys, 0, len);

                } else {
                    String result = String.valueOf(output.toByteArray());
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            }
            super.handleMessage(msg);
        }
    };


    private void manageConnectedSocket(BluetoothSocket socket) {
        Intent intent = new Intent(MainActivity.this, ChattingActivity.class);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, paintAddress);
        startActivity(intent);
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private void startServer() {
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    private void stopServer() {
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }
    }

    private void startClient(BluetoothDevice bluetoothDevice) {
        mConnectThread = new ConnectThread(bluetoothDevice);
        mConnectThread.start();
    }

    private void stopClient() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }
    }
}