package com.example.bluetoothdemo.threads;

/**
 * Created by 张高强 on 2016/12/13.
 * 邮箱: zhang.gaoqiang@mobcb.com
 */

public class BlueToothConnectThread extends Thread {

    public BlueToothConnectThread(String address) {
        this.address = address;
    }

    private String address;


    @Override
    public void run() {


    }

    public void cancel() {

    }
}
