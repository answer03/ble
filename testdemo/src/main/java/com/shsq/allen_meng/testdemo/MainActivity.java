package com.shsq.allen_meng.testdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.shsq.allen_meng.tggsimpledemo.BleManager;
import com.shsq.allen_meng.tggsimpledemo.callback.BleConnectGattCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.BleScanCallback;
import com.shsq.allen_meng.tggsimpledemo.callback.SendCmdCallback;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private Button tv_scan;
    private Button btn_send;
    private Button btn_on;
    private TextView tv_status;
    private ListView lv_search;
    private BlueSearchListAdapter adapter = null;
    private List<BluetoothDevice> datas = new ArrayList<>();
    private boolean isConnected = false;
    private String connectAddress = "unknow";
    private boolean isLock = false;

    private BleScanCallback bleScanCallback = new BleScanCallback() {
        @Override
        public void onScanStarted(boolean success) {

            Log.e("TAG", "" + success);
            tv_status.setText("开始扫描");

        }

        @Override
        public void onScanning(BluetoothDevice result) {
            tv_status.setText("扫描中");
        }

        @Override
        public void onScanFinished(List<BluetoothDevice> scanResultList) {
            tv_status.setText("扫描完成");
            datas.clear();
            datas.addAll(scanResultList);

            if (null == adapter) {
                adapter = new BlueSearchListAdapter();
                lv_search.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
//            if(scanResultList!=null && scanResultList.size()>0){
//                for(int i = 0;i<scanResultList.size();i++){
//                    if(scanResultList.get(i).getAddress().equals("0C:B2:B7:02:39:E4")){
//                        BleManager.getInstance().connect("0C:B2:B7:02:39:E4",false,callback);
//                        break;
//                    }
//                    Log.e("TAG", "扫描到的设备"+i+"====="+scanResultList.get(i).getAddress()+"_____"+scanResultList.get(i).getName());
//                }
//            }
        }

        @Override
        public void onScanFailed(String errMsg) {
            Log.e("TAG", "" + errMsg);
        }
    };
    private BleConnectGattCallback callback = new BleConnectGattCallback() {
        @Override
        public void onStartConnect() {
            Log.e("TAG", "111111111111111111");
        }

        @Override
        public void onConnectFail(String errMsg) {
            Log.e("TAG", "连接失败的原因" + errMsg);
        }

        @Override
        public void onConnectSuccess(final BluetoothDevice bleDevice, BluetoothGatt gatt, final int status) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_status.setText("已连接");
                    connectAddress = bleDevice.getAddress();
                    Toast.makeText(MainActivity.this, "连接成功" + status + bleDevice.getName(), Toast.LENGTH_SHORT).show();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_scan = findViewById(R.id.tv_scan);
        btn_send = findViewById(R.id.btn_send);
        btn_on = findViewById(R.id.btn_on);
        lv_search = findViewById(R.id.lv_search);
        tv_status = findViewById(R.id.tv_status);
        initData();
        initListener();
    }

    private void initListener() {
        tv_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("TAG", "111111111111");
                BleManager.getInstance().startScan(bleScanCallback);
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BleManager.getInstance().sendCMD("Password131670", new SendCmdCallback() {
                    @Override
                    public void onSendCmdResponse(boolean isOK, String reply, String des) {
                        Log.e("TAG", "isOK" + isOK + "reply====" + reply + " des ====" + des);
                        if (isOK) {
                            //NetVersion=1
                            BleManager.getInstance().sendCMD("NetVersion=1", new SendCmdCallback() {
                                @Override
                                public void onSendCmdResponse(boolean isOK, String reply, String des) {
                                    Toast.makeText(MainActivity.this, "isOK" + isOK + "reply====" + reply + " des ====" + des, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        });


        btn_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isLock) {
                    BleManager.getInstance().sendCMD("Button3Down", new SendCmdCallback() {
                        @Override
                        public void onSendCmdResponse(boolean isOK, String reply, String des) {
                            Log.e("TAG", "isOK" + isOK + "reply====" + reply + " des ====" + des);
                            BleManager.getInstance().sendCMD("Button3Up", new SendCmdCallback() {
                                @Override
                                public void onSendCmdResponse(boolean isOK, String reply, String des) {
                                    Log.e("TAG", "isOK" + isOK + "reply====" + reply + " des ====" + des);
                                    isLock = !isLock;
                                }
                            });
                        }
                    });
                } else {

                    BleManager.getInstance().sendCMD("Button1Down", new SendCmdCallback() {
                        @Override
                        public void onSendCmdResponse(boolean isOK, String reply, String des) {
                            Log.e("TAG", "isOK" + isOK + "reply====" + reply + " des ====" + des);
                            BleManager.getInstance().sendCMD("Button1Up", new SendCmdCallback() {
                                @Override
                                public void onSendCmdResponse(boolean isOK, String reply, String des) {
                                    Log.e("TAG", "isOK" + isOK + "reply====" + reply + " des ====" + des);
                                    isLock = !isLock;
                                }
                            });
                        }
                    });
                }


            }
        });

    }

    private void initData() {
        /**
         * 初始化蓝牙管理类
         */
        BleManager.getInstance().init(getApplication());
    }

    class BlueSearchListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return datas.size();
        }

        @Override
        public Object getItem(int i) {
            return datas.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            viewHolder holder = null;
            if (view == null) {
                view = View.inflate(MainActivity.this, R.layout.layout_item_list, null);
                holder = new viewHolder();
                holder.tv_name = view.findViewById(R.id.tv_name);
                holder.btn_connect = view.findViewById(R.id.btn_connect);
                view.setTag(holder);
            } else {
                holder = (viewHolder) view.getTag();
            }

            final BluetoothDevice bluetoothDevice = datas.get(i);

            holder.tv_name.setText(bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress());
            isConnected = BleManager.getInstance().getIsConnected();
            if (isConnected && bluetoothDevice.getAddress().equals(connectAddress)) {
                holder.btn_connect.setText("断开连接");
            }
            final viewHolder finalHolder = holder;
            holder.btn_connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isConnected && bluetoothDevice.getAddress().equals(connectAddress)) {
                        //断开连接
                        BleManager.getInstance().disConnect();
                        connectAddress = "unknow";
                        finalHolder.btn_connect.setText("连接");
                        tv_status.setText("未连接");
                    } else {
                        //连接
                        BleManager.getInstance().connect(bluetoothDevice.getAddress(), false, callback);
                    }
                }
            });
            return view;
        }

        class viewHolder {
            TextView tv_name;
            Button btn_connect;

        }
    }


}
