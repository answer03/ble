package com.shsq.allen_meng.tggsimpledemo.callback;

import android.bluetooth.BluetoothDevice;

import java.util.List;

/**
 * Created by allen_meng on 2018/5/28.
 * 扫描设备的回调
 *
 */

public abstract class BleScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(BluetoothDevice result);

    public abstract void onScanFinished(List<BluetoothDevice> scanResultList);

    public  abstract  void onScanFailed(String errMsg);




}
