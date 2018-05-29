package com.shsq.allen_meng.tggsimpledemo;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;

import com.shsq.allen_meng.tggsimpledemo.bluetooth.BleBlueTooth;
import com.shsq.allen_meng.tggsimpledemo.callback.BleConnectGattCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.BleScanCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.SendCmdCallback;

import java.util.List;
import java.util.UUID;

/**
 * Created by allen_meng on 2018/5/28.
 * 蓝牙管理类
 * 主要用于初始化 、 开始扫描  、 停止/取消扫描 连接 、断开 、 发送命令 等操作
 * 其分别调用 init()  startScan()  stopScan()  connect()  disConnect()  sendCMD()
 * 其余的一些方法 详见代码
 */

public class BleManager {
    private Application context;
    private BluetoothAdapter mBLEAdapter;
    /**
     * 提供蓝牙操作的管理类
     */
    private BleBlueTooth mBleBlueTooth;
    /**
     * 蓝牙扫描时间 10秒
     */
    public static final int DEFAULT_SCAN_TIME = 10000;


    /**
     * 是否连接的状态
     */
    private boolean isConnected = false;

    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;
    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;
    private static final int DEFAULT_WRITE_DATA_SPLIT_COUNT = 20;

    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;
    private int operateTimeout = DEFAULT_OPERATE_TIME;
    private int splitWriteNum = DEFAULT_WRITE_DATA_SPLIT_COUNT;

    public static BleManager getInstance() {
        return BleManagerHolder.sBleManager;
    }

    private static class BleManagerHolder {
        private static final BleManager sBleManager = new BleManager();
    }

    /**
     * 获取是否处于连接状态
     * @return
     */
    public boolean getIsConnected() {
        if (null != mBleBlueTooth) {
            return mBleBlueTooth.isConnected();
        }
        return false;
    }

    /**
     * 初始化蓝牙管理库
     *
     * @param app
     */
    public void init(Application app) {
        if (context == null && app != null) {
            context = app;
            mBleBlueTooth = new BleBlueTooth(app);
            if (null != mBleBlueTooth) {
                mBLEAdapter = mBleBlueTooth.initBLE(); //获取蓝牙的适配器
            }
        }
    }

    /**
     * Get the Context
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * Get the BluetoothAdapter
     *
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBLEAdapter;
    }

    /**
     * Get the maximum number of connections
     *
     * @return
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     *
     * @param maxCount
     * @return BleManager
     */
    public BleManager setMaxConnectCount(int maxCount) {
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE)
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = maxCount;
        return this;
    }

    /**
     * Get operate timeout
     *
     * @return
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     *
     * @param operateTimeout
     * @return BleManager
     */
    public BleManager setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }

    /**
     * Get operate splitWriteNum
     *
     * @return
     */
    public int getSplitWriteNum() {
        return splitWriteNum;
    }

    /**
     * Set splitWriteNum
     *
     * @param num
     * @return BleManager
     */
    public BleManager setSplitWriteNum(int num) {
        this.splitWriteNum = num;
        return this;
    }


    /**
     * 扫描设备
     *
     * @param callback
     */
    public void startScan(BleScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        } else {
            if (!isBlueEnable()) {
                callback.onScanStarted(false);
            }
        }
        mBleBlueTooth.startScanDevice(callback);
    }

    /**
     * 停止扫描蓝牙设备
     */
    public void stopScan() {
        if (null != mBleBlueTooth) {
            mBleBlueTooth.stopScanDevices();
        }
    }

    /**
     * 连接蓝牙设备
     *
     * @param mac         设备的mac地址
     * @param autoConnect 连接参数 autoConnect
     * @param callback    连接蓝牙设备的回调
     */
    public void connect(String mac, boolean autoConnect, BleConnectGattCallback callback) {
        if (null != mBleBlueTooth) {
            mBleBlueTooth.connectBLEDevice(context, autoConnect, mac, callback);
        }
    }

    /**
     * 断开设备连接
     */
    public void disConnect() {
        BluetoothGatt blueToothGatt = getBlueToothGatt();
        if (null != blueToothGatt && mBleBlueTooth.isConnected()) {
            mBleBlueTooth.disconnectBLEDevice();
        }
    }


    /**
     * 设置扫描时间
     * @param scanTime
     */
   public  void  setScanTime(long scanTime){
        if(mBleBlueTooth!=null){
            mBleBlueTooth.setSCANNING_PERIOD_TIME(scanTime);
        }
   }

    /**
     * 设置自己的写的uuid
     * @param uuid
     */
   public  void setBLE_SHIELD_TX_UUID(String uuid){
       if(mBleBlueTooth!=null){
           mBleBlueTooth.setUUID_BLE_SHIELD_TX(java.util.UUID.fromString(uuid));
       }
   }
    /**
     * 设置自己的uuid 读的uuid
     * @param uuid
     */
   public  void setBLE_SHIELD_RX_UUID(String uuid){
       if(mBleBlueTooth!=null){
           mBleBlueTooth.setUUID_BLE_SHIELD_RX(java.util.UUID.fromString(uuid));
       }
   }
    /**
     * 设置自己的uuid 服务的uuid
     * @param uuid
     */
    public  void setBLE_SHIELD_SERVICE_UUID(String uuid){
        if(mBleBlueTooth!=null){
            mBleBlueTooth.setUUID_BLE_SHIELD_SERVICE(java.util.UUID.fromString(uuid));
        }
    }

    /**
     * 发送命令
     *
     * @param cmd  命令
     * @param callback  发送之后的回调
     */
    public void sendCMD(String cmd, SendCmdCallback callback) {
        if(null != mBleBlueTooth){
            mBleBlueTooth.sendDataToDevice(cmd,callback);
        }

    }

    /**
     * is support ble?
     *
     * @return
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (mBLEAdapter != null) {
            mBLEAdapter.enable();
        }
    }

    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (mBLEAdapter != null) {
            if (mBLEAdapter.isEnabled())
                mBLEAdapter.disable();
        }
    }

    /**
     * judge Bluetooth is enable
     *
     * @return
     */
    public boolean isBlueEnable() {
        return mBLEAdapter != null && mBLEAdapter.isEnabled();
    }


    /**
     * 获取BluetoothGatt 连接设备的Gatt
     *
     * @return
     */
    public BluetoothGatt getBlueToothGatt() {
        if (null != mBleBlueTooth) {
            return mBleBlueTooth.getConnBLEGatt();
        }
        return null;
    }


}
