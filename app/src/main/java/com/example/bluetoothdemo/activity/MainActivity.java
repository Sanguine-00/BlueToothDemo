package com.example.bluetoothdemo.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.os.Bundle;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @InjectView(R.id.list_view)
    ListViewCompat mListView;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter mArrayAdapter;
    private List<String> mData;
    private List<Map<Integer, String>> mList;
    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private String TAG = "蓝牙Demo";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        initBluetooh();
        init();
        initBluetooh();
        registerReceiver();
    }

    private void init() {
//        mData = new ArrayList<>();
//        mArrayAdapter = new ArrayAdapter(MainActivity.this, R.layout.list_item, R.id.list_item_name, mData) {
//        };
        mList = new ArrayList<>();
        mDeviceListAdapter = new DeviceListAdapter(MainActivity.this, mList);
        mListView.setAdapter(mDeviceListAdapter);
        mListView.setOnItemClickListener(this);
    }


    private void initBluetooh() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothAdapter.setName("我有一只小毛驴我从来也不骑...");
        mBluetoothAdapter.enable();
    }

    @OnClick({R.id.btn_stop_scan, R.id.btn_start_scan, R.id.btn_start_scan_le,
            R.id.btn_stop_scan_le, R.id.btn_stop_scan_advertiser, R.id.btn_start_scan_advertiser})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_stop_scan:
                mBluetoothAdapter.cancelDiscovery();
                break;
            case R.id.btn_start_scan:
                mBluetoothAdapter.startDiscovery();
                break;
            case R.id.btn_start_scan_le:
//                mBluetoothAdapter.startLeScan(leScanCallback);
                startLeScan();
                break;
            case R.id.btn_stop_scan_le:
                stopLeScan();
                break;
            case R.id.btn_start_scan_advertiser:
                startLeAdvertiser();
                break;

            case R.id.btn_stop_scan_advertiser:
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.disable();
        unRegisterReceiver();
    }


    // Android 6.0 需要动态申请权限
    private void checkPermission() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    public static final UUID uuid = UUID.randomUUID();
    private BluetoothDevice device;
    private String paintAddress;

    private void doConnect() {
        new BlueToothConnectThread(paintAddress).start();
    }

    private void startLeScan() {
        if (mBluetoothLeScanner == null) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        mBluetoothLeScanner.startScan(scanCallback);
    }

    private void stopLeScan() {
        if (mBluetoothLeScanner == null) {
            if (mBluetoothAdapter == null) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        mBluetoothLeScanner.stopScan(scanCallback);
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

    @Override
    public void onItemClick(AdapterView<?> view, View view1, int i, long l) {
        Map<Integer, String> map = (Map<Integer, String>) mListView.getAdapter().getItem(i);
        paintAddress = map.get(1);
        doConnect();
    }


    private BluetoothSocket bluetoothSocket;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private int connetTime = 0;

    private class BlueToothConnectThread extends Thread {

        public BlueToothConnectThread(String address) {
            this.address = address;
        }

        private String address;


        @Override
        public void run() {
            isConnecting = true;
            isConnected = false;
            if (mBluetoothAdapter != null) {
                device = mBluetoothAdapter.getRemoteDevice(address);
                mBluetoothAdapter.cancelDiscovery();
                while (!isConnected && connetTime < 10) {
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    connectDevice();
                }
            }


        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void connectDevice() {
        try {
            // 连接建立之前的先配对
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Method creMethod = BluetoothDevice.class
                        .getMethod("createBond");
                Log.e("TAG", "开始配对");
                creMethod.invoke(device);
            } else {
            }
        } catch (Exception e) {
            // TODO: handle exception
            //DisplayMessage("无法配对！");
            e.printStackTrace();
        }
        mBluetoothAdapter.cancelDiscovery();
        try {
            bluetoothSocket.connect();
            //DisplayMessage("连接成功!");
            //connetTime++;
            isConnected = true;
        } catch (Exception e) {
            // TODO: handle exception
            //DisplayMessage("连接失败！");
            connetTime++;
            isConnected = false;
            try {
                bluetoothSocket.close();
                bluetoothSocket = null;
            } catch (Exception e2) {
                // TODO: handle exception
                Log.e(TAG, "Cannot close connection when connection failed");
            }
        } finally {
            isConnecting = false;
        }
    }

}