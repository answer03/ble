package com.shsq.allen_meng.tggsimpledemo.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.text.TextUtils;
import android.util.Log;

import com.shsq.allen_meng.tggsimpledemo.callback.BleConnectGattCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.BleScanCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.SendCmdCallback;
import com.shsq.allen_meng.tggsimpledemo.data.BLEGattAttrs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by allen_meng on 2018/5/28.
 * 蓝牙的一些操作管理类
 */

public class BleBlueTooth {

    private final static String TAG = BleBlueTooth.class.getSimpleName();
    // 扫描设备时长 3s
    private  long SCANNING_PERIOD_TIME = 5000;
    //rssi读取次数，取平均值判断rssi
    private static final int RSSI_READ_TIME = 3;
    // ****************** Broadcast Action **********************
    public static final int BLUE_GATT_CONNECT_REQUEST = 1;
    public static final int BLUE_GATT_DISCONNECT_REQUEST = 2;
    public static final int BLUE_GATT_DISCOVER_SERVICE_REQUEST = 3;
    public static final int BLUE_GATT_RSSI_VALUE = -85;

    private  UUID UUID_BLE_SHIELD_TX = UUID.fromString(BLEGattAttrs.BLE_SHIELD_TX);
    private  UUID UUID_BLE_SHIELD_RX = UUID.fromString(BLEGattAttrs.BLE_SHIELD_RX);
    private  UUID UUID_BLE_SHIELD_SERVICE = UUID.fromString(BLEGattAttrs.BLE_SHIELD_SERVICE);
    // BLE是否初始化完成
    private boolean isBLEAvailable = false;
    // 蓝牙适配器
    private BluetoothAdapter mBLEAdapter;
    // 上次连接成功的设备
    private BluetoothDevice connBLEDevice;
    // 上次连接成功的设备GATT
    private volatile BluetoothGatt connBLEGatt;
    // 上次连接成功的设备Characteristic
    private BluetoothGattCharacteristic connBLEGattCharstic;
    private List<BluetoothGatt> mConnGattList = new ArrayList<BluetoothGatt>();
    private AutoDiscoverThread autoDiscoverThread;

    public BluetoothGatt getConnBLEGatt() {
        return connBLEGatt;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setSCANNING_PERIOD_TIME(long SCANNING_PERIOD_TIME) {
        this.SCANNING_PERIOD_TIME = SCANNING_PERIOD_TIME;
    }

    public void setUUID_BLE_SHIELD_TX(UUID UUID_BLE_SHIELD_TX) {
        this.UUID_BLE_SHIELD_TX = UUID_BLE_SHIELD_TX;
    }

    public void setUUID_BLE_SHIELD_RX(UUID UUID_BLE_SHIELD_RX) {
        this.UUID_BLE_SHIELD_RX = UUID_BLE_SHIELD_RX;
    }

    public void setUUID_BLE_SHIELD_SERVICE(UUID UUID_BLE_SHIELD_SERVICE) {
        this.UUID_BLE_SHIELD_SERVICE = UUID_BLE_SHIELD_SERVICE;
    }

    /**
     * 上下文
     */
    private Context context;
    private BluetoothGattCallback mGattCallback = null;
    private AutoReadRssiThread autoReadRssiThread;//读取rssi线程
    private int[] rssiCache = new int[RSSI_READ_TIME + 1];    //缓存读取到的rssi值.留一个占位值，表示当前读取到的是第几个
    private boolean isConnected = false;  //蓝牙设备是否处于已经连接的状态
    private BleScanCallback bleScanCallback;  //扫描设备的回调
    private  BleConnectGattCallback bleConnectGattCallback ; //连接设备的回调
    private SendCmdCallback sendCmdCallback; //发送命令的回调

    /**
     * 为避免handler造成的内存泄漏
     * 1、使用静态的handler，对外部类不保持对象的引用
     * 2、但Handler需要与Activity通信，所以需要增加一个对Activity的弱引用
     */
    private Handler myHandle = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "handle message :" + msg.what + "-->" + msg.obj);
            switch (msg.what) {    //获取消息
                case BLUE_GATT_CONNECT_REQUEST://连接设备
                    String mac = (String) msg.obj;
//                    autoReconnect(mac);
                    removeCallbacksAndMessages(null);
                    break;

                case BLUE_GATT_DISCONNECT_REQUEST://断开连接
                    if (null != connBLEGatt) {
                        connBLEGatt.disconnect();
                    }
                    removeCallbacksAndMessages(null);
                    break;

                case BLUE_GATT_DISCOVER_SERVICE_REQUEST://发现服务
                    boolean isOK = connBLEGatt != null && connBLEGatt.discoverServices();
                    Log.d(TAG, "=====> start discover service : " + isOK);
                    removeCallbacksAndMessages(null);
                    break;
            }
        }
    };


    public BleBlueTooth(Application app) {
        if (null != app) {
            context = app;
        }
    }

    private BluetoothGattCallback bleGattCallback() {
        if (null == mGattCallback) {
            mGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    Log.e(TAG, "===>> Connect State Changed: GATT=" + gatt + "; Status=" + status + "; NewState=" + newState);
                    switch (newState) {
                        case BluetoothProfile.STATE_CONNECTING:
                            Log.e(TAG, "****** Device Connecting *******");
                            if(null != bleConnectGattCallback){
                                bleConnectGattCallback.onConnectSuccess(connBLEDevice,connBLEGatt,status);
                            }
                            break;
                        case BluetoothProfile.STATE_CONNECTED:
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                isConnected = true;
                                Log.e(TAG, "****** Device Connected *******");
                                connBLEGatt = gatt;
                                rssiCache[RSSI_READ_TIME] = 0;//第一个读取的rssi放在数组第一位
                                startAutoReadRssi();
                                //连接成功
                                if(null != bleConnectGattCallback){
                                    bleConnectGattCallback.onConnectSuccess(connBLEDevice,connBLEGatt,status);
                                }
                            } else {
                                disconnectBLEDevice();
                            }
                            break;
                        case BluetoothProfile.STATE_DISCONNECTING:
                            if(null != bleConnectGattCallback){
                                bleConnectGattCallback.onConnectSuccess(connBLEDevice,connBLEGatt,status);
                            }
                            Log.e(TAG, "****** Device Disconnecting *******");
//                            close(); // 防止出现status 133
                            break;
                        case BluetoothProfile.STATE_DISCONNECTED:
                            if(null != bleConnectGattCallback){
                                bleConnectGattCallback.onConnectSuccess(connBLEDevice,connBLEGatt,status);
                            }
                            Log.e(TAG, "****** Device Disconnected *******");
                            //连接成功后由于信号弱断开连接时需要停止信号弱提示音
                            stopAutoReadRssi();
                            close(gatt);//关闭连接，释放资源
                            //手动扫描断开连接的时候不进行以下操作
//                            sendBroadcast(ACTION_GATT_DISCONNECTED);
                            //重新获取蓝牙适配器，判断是否是由于蓝牙断开导致的连接断开
                            initBLE();
                            if (!isBLEAvailable) {
//                                sendBroadcast(ACTION_BLE_DISABLED);
                            }
                            break;
                        default:
                            Log.e(TAG, "****** wp Device Unknow Status *******");
                            break;
                    }
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    Log.d(TAG, "===>> On Read Remote Rssi: GATT=" + gatt + "; Rssi=" + rssi + "; Status=" + status);
//                    sendBroadcast(ACTION_GATT_RSSI, rssi);
                    onReadRssi(rssi, status, gatt);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.e(TAG, "===>> On Services Discovered: GATT=" + gatt + "; Status=" + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connBLEGatt = gatt;
//                        SessionManager.getInstance().setState(SessionManager.BLEDeviceState.DISCOVERED);
                        Log.i(TAG, "Services Discovered size: " + getSupportedGattServices().size());
                        connBLEGattCharstic = connBLEGatt.getService(UUID_BLE_SHIELD_SERVICE).getCharacteristic(UUID_BLE_SHIELD_TX);
                        if (null != connBLEGattCharstic) {
                            /* Here sleep to wait system */
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // set notification is true
                            int charaProp = connBLEGattCharstic.getProperties();
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                setCharacteristicNotification(connBLEGattCharstic, true);
                            }
//                            sendBroadcast(ACTION_GATT_SERVICES_DISCOVERED);
                            //发现服务后，主动向设备发送密码
//                            sendCmdDelayed(PASSWORD_PASS);
//                            sendCmdDelayed(200);
                            // 读取设备版本号
                            // writeCharacteristic(ConstantValues.GET_DEV_VERSION.getBytes());
                        }
                    } else {
                        Log.e(TAG, "ServicesDiscovered Failed: " + status);
                        disconnectBLEDevice();
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.d(TAG, "===>> On Characteristic Read: UUID: " + characteristic.getUuid() + "; Value: "
                            + new String(characteristic.getValue()) + "; Status: " + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
//                        sendBroadcast(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.d(TAG, "===>> On Characteristic Write: UUID: " + characteristic.getUuid() + "; Value: "
                            + new String(characteristic.getValue()) + "; Status: " + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
//                        sendBroadcast(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }

                // 读取从机发来的信息,这里通过接口回调的方法暴露出去
                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic charstic) {
                    super.onCharacteristicChanged(gatt, charstic);
                    String reply = new String(charstic.getValue());
                    Log.e(TAG, "===>> Receive Value: " + reply);
                    sendCmdCallback.onSendCmdResponse(true, reply, "send cmd  is ok ");

                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                    Log.d(TAG, "===>> On Descriptor Write, Status= " + status);
                }

                @Override
                public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                    super.onReliableWriteCompleted(gatt, status);
                    Log.d(TAG, "===>> On Reliable Write Completed, Status= " + status);
                }

            };
        }

        return mGattCallback;
    }

//    /**
//     * 发送校验密码
//     */
//    public void sendVerifyPwd(String pwd) {
////        String pwd = SessionManager.getInstance().getDevicePIN(connBLEDevice.getAddress());
//        sendDataToDevice(pwd.getBytes());
//        Log.e(TAG, ">>>>> " + "Send verify pwd: " + pwd);
//    }

    /**
     * 开始读取rssi
     */
    private synchronized void startAutoReadRssi() {
        stopAutoReadRssi();
        autoReadRssiThread = new AutoReadRssiThread();
        autoReadRssiThread.start();
    }

    /**
     * 停止读取rssi
     */
    private synchronized void stopAutoReadRssi() {
        if (autoReadRssiThread == null) {
            return;
        }
        autoReadRssiThread.stopAutoPass();
        autoReadRssiThread = null;
    }

    /**
     * 设备连接成功后，延迟发送命令
     * 高哥，这个用着好像有点问题，发送NetVersion命令在onServicesDiscovered()方法中，好像发送不过去
     * <p>
     * 我这边几个手机测试是可以的，之前的版本可能是发送Password和NetVersion中间没加延时，现在加了300ms延时
     *
     * @param delayMillis
     * @deprecated {use sendCmdDelayed(@PassFlag int flag) }
     */
    private void sendCmdDelayed(final long delayMillis) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, ">>>>> Send CMD to device delayed <<<<<");
                // 1、发送校验密码
//                sendVerifyPwd(Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 2、发送设备类型 判断是否为联网版
//                sendNetVersion(Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                // 3、发送读取电池电量
//                sendReadPower(Thread.currentThread().getName());
            }
        }).start();
    }

    public void onDestroy() {
        if (connBLEGatt != null) {
            connBLEGatt.disconnect();
            closeBLEGatt();
        }
//        sendBroadcast(ACTION_GATT_DISCONNECTED);
        Log.e(TAG, "Service is destroyed, restart it");
        Log.e(TAG, "service is destroyed");
    }


    /**
     * 处理rssi读取回调
     *
     * @param rssi
     * @param status
     * @param gatt
     */
    private void onReadRssi(int rssi, int status, BluetoothGatt gatt) {
        stopAutoReadRssi();//停止自动读取rssi线程
        rssiCache[rssiCache[RSSI_READ_TIME]] = rssi;
        if (rssiCache[RSSI_READ_TIME] == RSSI_READ_TIME - 1) {
            rssiCache[RSSI_READ_TIME] = 0;//存放达到上限，重新从零开始存放
        } else {
            rssiCache[RSSI_READ_TIME] = rssiCache[RSSI_READ_TIME] + 1;//存放坐标向下一位移动
        }
        if (status == BluetoothGatt.GATT_SUCCESS && rssiCache[RSSI_READ_TIME - 1] != 0) {
            //计算rssi平均值
            int totalRssi = 0;
            for (int i = 0; i < RSSI_READ_TIME; i++) {
                totalRssi += rssiCache[i];
            }
            int avgRssi = totalRssi / RSSI_READ_TIME;
            //在未连接成功情况下读取强度
            if (avgRssi > BLUE_GATT_RSSI_VALUE) {
                Log.d(TAG, "#### RSSI value ==>" + Arrays.toString(rssiCache));
                Log.d(TAG, "#### RSSI ok, enable discover service...");
                //重置数据
                rssiCache = new int[RSSI_READ_TIME + 1];
                rssiCache[RSSI_READ_TIME] = 0;
                startAutoDiscover();//发现服务
            } else {
                Log.w(TAG, "#### RSSI weak, read it 100ms later...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startAutoReadRssi();
            }
        } else {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startAutoReadRssi();
        }
    }

    /**
     * 打开 自动发现线程
     */
    public synchronized void startAutoDiscover() {
        if (null != autoDiscoverThread) {
            autoDiscoverThread.stopAutoDiscover();
            autoDiscoverThread = null;
        }
        autoDiscoverThread = new AutoDiscoverThread();
        autoDiscoverThread.setPriority(Thread.MAX_PRIORITY);
        autoDiscoverThread.start();
        Log.e(TAG, "*********** start auto discover with gatt : " + connBLEGatt + " ***********");
    }

    private void addConnBLEGatt(BluetoothGatt connGatt) {
        for (BluetoothGatt gatt : mConnGattList) {
            if (connGatt.getDevice().getAddress().equals(gatt.getDevice().getAddress())) {
                return;
            }
        }

        mConnGattList.add(connGatt);
    }

    private class AutoDiscoverThread extends Thread {
        int discoverTryTime = 0;
        boolean discovered = false; // discoverServices() 是否启动成功
        boolean stop = false;

        AutoDiscoverThread() {
            super("AutoDiscoverThread");
        }

        @Override
        public void run() {
            while (!discovered) {
                if (stop) {
                    break;//end thread
                }
                // 尝试5次，每次延时1s，尝试发现服务
                if (discoverTryTime > 5) {
                    Log.e(TAG, ">>>> Discover service failed, close connect and reconnect!");
                    disconnectBLEDevice();
                    break;//end thread
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                connBLEGatt.discoverServices();
                //发送发现服务消息到主线程
                myHandle.sendEmptyMessage(BLUE_GATT_DISCOVER_SERVICE_REQUEST);
                discoverTryTime++;
                Log.d(TAG, "### Try to discovering services: " + discoverTryTime);

            }
        }

        public void stopAutoDiscover() {
            stop = true;
            if (isAlive()) {
                this.interrupt();
            }
        }
    }

    public void removeConnBLEGatt(BluetoothGatt connGatt) {
        for (int i = 0; i < mConnGattList.size(); i++) {
            BluetoothGatt gatt = mConnGattList.get(i);
            if (connGatt.getDevice().getAddress().equals(gatt.getDevice().getAddress())) {
                mConnGattList.remove(i);
                return;
            }
        }
    }

    public BluetoothGatt findBluetoothGatt(String address) {
        for (BluetoothGatt gatt : mConnGattList) {
            if (address.equals(gatt.getDevice().getAddress())) {
                return gatt;
            }
        }
        return null;
    }

    public List<BluetoothGatt> getConnBLEGattList() {
        return mConnGattList;
    }

    /**
     * 初始化BLE
     */
    public BluetoothAdapter initBLE() {
        if (null == context) {
            Log.e(TAG, "Context is null ...");
            return null;
        }
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }
        BluetoothManager bleManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (null != bleManager) {
            mBLEAdapter = bleManager.getAdapter();
            if (null != mBLEAdapter && mBLEAdapter.isEnabled()) {
                Log.d(TAG, ">>>> Init BluetoothAdapter success: " + mBLEAdapter);
                isBLEAvailable = true;
                return mBLEAdapter;
            }
        }
        return null;
    }

    /**
     * 清理本地的BluetoothGatt 的缓存，以保证在蓝牙连接设备的时候，设备的服务、特征是最新的
     *
     * @param gatt
     * @return
     */
    public boolean refreshDeviceCache(BluetoothGatt gatt) {
        if (null != gatt) {
            try {
                BluetoothGatt localBluetoothGatt = gatt;
                Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
                if (localMethod != null) {
                    boolean ret = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                    Log.i(TAG, "Refresh GATT: " + ret);
                    return ret;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 通过蓝牙地址连接蓝牙设备
     *
     * @param mac 蓝牙地址
     * @return 是否连接成功
     */
    @MainThread
    public synchronized boolean connectBLEDevice(Context context, boolean autoConnect, final String mac, BleConnectGattCallback callback) {
        bleConnectGattCallback = callback;
        mBLEAdapter.cancelDiscovery();
        if (mac == null) {
            Log.e(TAG, "Device mac can't be null!");
            callback.onConnectFail("mac地址不能为空！");
            return false;
        }
        if (!isBLEAvailable) {
            Log.e(TAG, "BLE is not available!");
            callback.onConnectFail("蓝牙不可用！");
            return false;
        }
        if (null != connBLEGatt) {
            if (mac.equals(connBLEDevice.getAddress())) {
                Log.d(TAG, "Target device is connected");
                callback.onConnectFail("此设备已连接");
                return false;
            } else {
                //断开上一次的连接
                Log.d(TAG, "Disconnect old device before connect another");
                disconnectBLEDevice();
            }
        }
        if (connBLEDevice == null || !mac.equals(connBLEDevice.getAddress())) {
            connBLEDevice = mBLEAdapter.getRemoteDevice(mac);
        }
        //connBLEDevice = mBLEAdapter.getRemoteDevice(mac);
        if (connBLEDevice != null) {
            connBLEGatt = connBLEDevice.connectGatt(context, autoConnect, bleGattCallback());
            Log.d(TAG, "=== Start try to connect device: " + mac);
            callback.onStartConnect();
            if (null != connBLEGatt) {
                if (android.os.Build.VERSION.SDK_INT > 21) {
                    connBLEGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
                Log.d(TAG, "Connecting device , wait BluetoothGattCallback");
                return connBLEGatt.connect();
            } else {
                callback.onConnectFail("蓝牙连接失败");
                Log.e(TAG, "Connecting device failed");
            }
        } else {
            callback.onConnectFail("蓝牙连接异常");
            Log.e(TAG, "wp getRemoteDevice fail" + mac);
        }
        return false;
    }


    public synchronized void disconnectBLEDevice() {
        if (connBLEGatt == null) {
            return;
        }
        myHandle.sendEmptyMessage(BLUE_GATT_DISCONNECT_REQUEST);
    }

    //解决status 133问题
    private synchronized void close(BluetoothGatt gatt) {
        if (gatt == null) {
            connBLEGatt = null;
            return;
        }
        if (android.os.Build.VERSION.SDK_INT > 21) {
            gatt.requestConnectionPriority(connBLEGatt.CONNECTION_PRIORITY_BALANCED);
        }
        gatt.disconnect();
        refreshDeviceCache(gatt);
        gatt.close();
        Log.w(TAG, "mBluetoothGatt closed");
        connBLEGatt = null;
    }

    /**
     * 关闭当前连接的设备，释放资源
     */
    private void closeBLEGatt() {
        if (connBLEGatt == null) {
            return;
        }
        refreshDeviceCache(connBLEGatt);
        connBLEGatt.close();
        connBLEGatt = null;
        connBLEDevice = null;
        connBLEGattCharstic = null;
    }

    /**
     * 发送命令  回调
     * @param cmd
     * @return
     */
    public boolean sendDataToDevice(String cmd, SendCmdCallback callback) {
        sendCmdCallback = callback;
        if(TextUtils.isEmpty(cmd)){
            callback.onSendCmdResponse(false,null,"Send Data is null");
            return false;
        }
        byte[] data = cmd.getBytes();
        if (null == data) {
            Log.e(TAG, "Write data is NULL");
            callback.onSendCmdResponse(false,null,"Write data is NULL");
            return false;
        }
        if (connBLEGattCharstic == null || connBLEGatt == null) {
            Log.e(TAG, "WriteCharacteristic is NULL");
            callback.onSendCmdResponse(false,null,"WriteCharacteristic is NULL");
            return false;
        }
        if (isConnected) {
            String s = new String(data);
            Log.e(TAG, "发送的内容是：" + s);
            connBLEGattCharstic.setValue(data);
            Log.d(TAG, "+++++ Send to dev: " + new String(data));
            Log.d(TAG, "Send to dev: " + new String(data));
            return connBLEGatt.writeCharacteristic(this.connBLEGattCharstic);
        } else {
            callback.onSendCmdResponse(false,null,"DEVICE IS DISCONNECTED");
            Log.e(TAG, "Device do not connect");
        }
        return false;
    }

    private ArrayList<BluetoothDevice> mBLEDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG, ">>> Scan Device: Address=" + device.getAddress());
            if (!mBLEDeviceList.contains(device)) {
                mBLEDeviceList.add(device);
            }
            if (null != bleScanCallback) {
                bleScanCallback.onScanning(device);
            }
        }
    };

    /**
     * 扫描可用设备
     */
    @MainThread
    public void startScanDevice(final BleScanCallback callback) {
        bleScanCallback = callback;
        if (null != callback) {
            disconnectBLEDevice(); //扫描之前关闭连接
            initBLE();
            if (!isBLEAvailable) {
                Log.e(TAG, "BLE is not avaliable !");
                callback.onScanFailed("BLE is not avaliable !");
                return;
            }
            mBLEDeviceList.clear();
            //开始进入扫描
            callback.onScanStarted(true);
            mBLEAdapter.startLeScan(mLeScanCallback);
            Log.e(TAG, "start scan ble");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBLEAdapter.stopLeScan(mLeScanCallback);
                    if (null != callback) {
                        callback.onScanFinished(mBLEDeviceList);
                    }
                }
            }, SCANNING_PERIOD_TIME);
        }
    }

    @MainThread
    public void stopScanDevices() {
        if (!isBLEAvailable) {
            Log.e(TAG, "BLE is not avaliable !");
            return;
        }
        if (null != mBLEAdapter) {
            mBLEAdapter.stopLeScan(null);
            mBLEAdapter.cancelDiscovery();
//            if (null != bleScanCallback) {
//                bleScanCallback.onScanFinished(mBLEDeviceList);
//            }
        }
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBLEAdapter == null || connBLEGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        connBLEGatt.readCharacteristic(characteristic);
    }

    public boolean readRssi() {
        if (!isBLEAvailable || connBLEGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return connBLEGatt.readRemoteRssi();
    }

    public synchronized boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!isBLEAvailable || connBLEGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        Log.d(TAG, "+++++ Send to dev: " + new String(characteristic.getValue()));
        Log.d(TAG, "Send to dev: " + new String(characteristic.getValue()));
        return connBLEGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    private boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (!isBLEAvailable || connBLEGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        connBLEGatt.setCharacteristicNotification(characteristic, enabled);
        if (UUID_BLE_SHIELD_RX.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(BLEGattAttrs.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                connBLEGatt.writeDescriptor(descriptor);
            }
        }
        return true;
    }

    public void getCharacteristicDescriptor(BluetoothGattDescriptor descriptor) {
        if (!isBLEAvailable || connBLEGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }

        connBLEGatt.readDescriptor(descriptor);
    }

    public BluetoothGattService getSupportedGattService() {
        if (connBLEGatt == null) {
            Log.w(TAG, "BluetoothGatt is null");
            return null;
        }

        BluetoothGattService mBluetoothGattService = connBLEGatt.getService(UUID_BLE_SHIELD_SERVICE);

        return mBluetoothGattService;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (connBLEGatt == null)
            return null;
        return connBLEGatt.getServices();
    }


    /**
     * 读取rssi线程
     */
    private class AutoReadRssiThread extends Thread {

        private volatile boolean isStart = true;

        @Override
        public void run() {
            // TODO 读取RSSI可能会失败，需要检查返回结果并容错，否则连接状态会卡死在 '正在连接'
//            int retry = 0;
            while (isStart) {
//                if (retry > 10 || connBLEGatt == null) {
                if (connBLEGatt == null) {
                    disconnectBLEDevice();
                    break;
                }
                if (connBLEGatt.readRemoteRssi()) { //查看强度
                    Log.d(TAG, "$$$$ Request read remote RSSI success");
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 停止线程，线程退出
         */
        public void stopAutoPass() {
            Log.e(TAG, ">>>>> auto read rssi destroy <<<<<");
            if (isAlive()) {
                interrupt();
            }
            isStart = false;
        }
    }
}
