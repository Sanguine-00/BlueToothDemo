package com.example.bluetoothdemo.chatroom;

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
import com.example.bluetoothdemo.activity.MainActivity;
import com.example.bluetoothdemo.adapter.DeviceListAdapter;
import com.example.bluetoothdemo.threads.BlueToothConnectThread;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class DeviceListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

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
        setContentView(R.layout.activity_device_list);
        ButterKnife.inject(this);
        init();
        initBluetooh();
        initBondedList();
        registerReceiver();
    }

    private void init() {
        mList = new ArrayList<>();
        mListBonded = new ArrayList<>();
        mDeviceListAdapter = new DeviceListAdapter(DeviceListActivity.this, mList);
        mListView.setAdapter(mDeviceListAdapter);
        mListView.setOnItemClickListener(this);
    }


    private void initBluetooh() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mSet = mBluetoothAdapter.getBondedDevices();
//        mBluetoothAdapter.setName("有一天我心血来潮骑它去赶集...");
//        mBluetoothAdapter.setName("我有一只小毛驴我从来也不骑...");
//        startServer();
        mBluetoothAdapter.startDiscovery();
    }

    private void initBondedList() {
        Map<Integer, String> map = null;
        for (BluetoothDevice d : mSet) {
            map = new HashMap<>();
            map.put(0, d.getName());
            map.put(1, d.getAddress());
            mListBonded.add(map);
        }
        DeviceListAdapter adapter = new DeviceListAdapter(DeviceListActivity.this, mListBonded);
        mListViewBonded.setAdapter(adapter);
        mListViewBonded.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> view, View view1, int i, long l) {
                Map<Integer, String> map = (Map<Integer, String>) mListBonded.get(i);
                paintAddress = map.get(1);
                Intent intent = new Intent();
                intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, paintAddress);
                setResult(RESULT_OK, intent);
                finish();

            }
        });

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
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(DeviceListActivity.this, "扫描结束", Toast.LENGTH_SHORT).show();
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
        mBluetoothAdapter.cancelDiscovery();
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


    private String paintAddress;


    @Override
    public void onItemClick(AdapterView<?> view, View view1, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();
        Map<Integer, String> map = (Map<Integer, String>) mListView.getAdapter().getItem(i);
        paintAddress = map.get(1);
        Intent intent = new Intent();
        intent.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, paintAddress);
        setResult(RESULT_OK, intent);
        finish();
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


}