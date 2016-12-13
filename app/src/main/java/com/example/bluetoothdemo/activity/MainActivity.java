package com.example.bluetoothdemo.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.bluetoothdemo.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.list_view)
    ListViewCompat mListView;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter mArrayAdapter;
    private List<String> mData;


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
        mData = new ArrayList<>();
        mArrayAdapter = new ArrayAdapter(MainActivity.this, R.layout.list_item, R.id.list_item_name, mData) {
        };
        mListView.setAdapter(mArrayAdapter);
    }


    private void initBluetooh() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
    }

    @OnClick({R.id.btn_stop_scan, R.id.btn_start_scan, R.id.btn_start_scan_le, R.id.btn_stop_scan_le})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_stop_scan:
                mBluetoothAdapter.cancelDiscovery();
                break;
            case R.id.btn_start_scan:
                mBluetoothAdapter.startDiscovery();
                break;
            case R.id.btn_start_scan_le:
                mBluetoothAdapter.startLeScan(leScanCallback);
                break;
            case R.id.btn_stop_scan_le:
                mBluetoothAdapter.stopLeScan(leScanCallback);
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
                if (mData.indexOf(device.getName() + ":\t" + device.getAddress()) < 0) {
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        mData.add(device.getName() + "\t" + device.getAddress());
                        Log.d("发现设备----", device.getName() + "\t" + device.getAddress());
                        mArrayAdapter.notifyDataSetChanged();
                    } else {
                        mData.add(device.getName() + "\t" + device.getAddress() + "已经绑定过了");
                    }
                }
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


    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int i, byte[] bytes) {
            // If it's already paired, skip it, because it's been listed already
            if (device.getBondState() != BluetoothDevice.BOND_BONDED && mData.indexOf(
                    "LE :  " + device.getName() + ":\t" + device.getAddress()
            ) < 0) {
                mData.add("LE :  " + device.getName() + ":\t" + device.getAddress());
                Log.d("LE发现设备----", device.getName() + ":\t" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };
}