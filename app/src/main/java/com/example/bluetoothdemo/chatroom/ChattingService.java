package com.example.bluetoothdemo.chatroom;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by 张高强 on 2016/12/14.
 * 邮箱: zhang.gaoqiang@mobcb.com
 */

public class ChattingService {


    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String TAG = "ChattingService";


    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    public static final int STATE_NONE = 0;       // 什么都不做
    public static final int STATE_LISTEN = 1;     // 正在监听
    public static final int STATE_CONNECTING = 2; // 初始化连接
    public static final int STATE_CONNECTED = 3;  // 已经连接到另一设备
    private Context mContext;

    public ChattingService(BluetoothAdapter mAdapter, Handler mHandler, Context context) {
        this.mAdapter = mAdapter;
        this.mHandler = mHandler;
        this.mContext = context;
        this.mState = STATE_NONE;
    }

    private void connectionFailed() {
        // 通知UI  连接失败
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // 初始化监听
        ChattingService.this.start();
    }


    /**
     * 设置状态
     *
     * @param state 要设置的状态
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // 通知UI
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * 返回当前状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 启动监听线程  在Activity的onResume中调用
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // 关闭连接
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 关闭连接
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // 启动监听
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

//    private ProgressDialog mProgressDialog;

//    private void showDialog() {
//        mProgressDialog = new ProgressDialog(mContext.getApplicationContext());
//        mProgressDialog.setCancelable(true);
//        mProgressDialog.show();
//    }

//    private void hideDialog() {
//        if (mProgressDialog != null) {
//            mProgressDialog.hide();
//            mProgressDialog = null;
//        }
//    }

    /**
     * 初始化远程连接
     *
     * @param device 远程设备
     * @param secure 安全性
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // 清空连接
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // 清空连接
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 连接设备
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }


    public void write(byte[] data) {
        ConnectedThread thread;
        synchronized (this) {
//            if (mState != STATE_CONNECTED) {
//                return;
//            }
            thread = mConnectedThread;
        }

        thread.write(data);
    }


    /**
     * 启动Connected线程管理
     *
     * @param socket
     * @param device
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);
        Log.d(TAG, "connected, Socket status:" + socket.isConnected());

        // 清空正在连接的
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }


        // 清空已经连接的
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 停止监听,不在接收其他设备连接
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // 启动线程管理
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // 通知UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
//        hideDialog();
        setState(STATE_CONNECTED);
    }


    /**
     * 停止所有线程
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;


            try {
                // UUID从网上申请  此处用的是Demo中的UUID
                int sdk = Integer.parseInt(Build.VERSION.SDK);
                if (sdk >= 10) {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                } else {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // 取消搜索, 防止阻塞
            mAdapter.cancelDiscovery();

            try {
                // 阻塞线程直到成功或者抛出异常
                mmSocket.connect();
            } catch (IOException connectException) {
                // 抛出异常  关闭连接 退出
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    connectionFailed();
                }
                return;
            }

            synchronized (this) {
                mConnectThread = null;
            }

            // 连接成功
            connected(mmSocket, mmDevice, null);
        }


        //手动关闭连接
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;

            Log.d(TAG, "connected, Socket status:" + socket.isConnected());
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "connected, Socket status:" + mmSocket.isConnected());
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
//            if (!mmSocket.isConnected()) {
//                try {
//                    mmSocket.connect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            // 当状态是连接的时.死循环监听
            while (mState == STATE_CONNECTED) {
                try {
                    //读取流
                    bytes = mmInStream.read(buffer);

                    // 与UI交互
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // 重启监听
                    ChattingService.this.start();
                    break;
                }
            }
        }

        /**
         * 连接已经断开,通知UI
         */
        private void connectionLost() {
            // 发送信息到UI
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, "Device connection was lost");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
            // 打开服务端  可以接受连接
            ChattingService.this.start();
        }

        // 向流中写数据,成功了 通知UI
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, bytes)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        // 手动关闭
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class AcceptThread extends Thread {
        // 本地
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // 创建监听
            try {
                int sdk = Integer.parseInt(Build.VERSION.SDK);
                if (sdk < 10) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // 在没有连接的情况下保持监听  (死循环)
            while (mState != STATE_CONNECTED) {
                try {
                    // 阻塞线程  直到有连接或者抛出异常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // 连接接收
                if (socket != null) {
                    synchronized (ChattingService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // 启动连接线程
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // 不连接
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


}
