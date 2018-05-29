package com.shsq.allen_meng.tggsimpledemo.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Build;

/**
 * Created by allen_meng on 2018/5/28.
 * 蓝牙连接时候的回调
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleConnectGattCallback {

    /**
     * 开始连接
     */
    public abstract void onStartConnect();

    /**
     * 连接错误信息
     * @param errMsg
     */
    public abstract void onConnectFail(String errMsg);

    /**
     * 连接成功后的操作
     * @param bleDevice
     * @param gatt
     * @param status
     */
    public abstract void onConnectSuccess(BluetoothDevice bleDevice, BluetoothGatt gatt, int status);

}
