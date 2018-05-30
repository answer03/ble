 # ble
蓝牙基本操作库的介绍
                        Created by Allen_meng
1、库的引用 导入jar包或者依赖library
2、初始化蓝牙操作统一管理，需要在application里面初始化 ，具体的初始化方法：
 BleManager.getInstance().init(getApplication());
3、具体的方法介绍
A)扫描蓝牙：startScan(BleScanCallback callback)；具体扫描结果回调处理；
B)停止扫描：stopScan()；
C)连接蓝牙设备：connect(String mac, boolean autoConnect, BleConnectGattCallback callback)；通过蓝牙的mac地址连接
D)发送命令： sendCMD(String cmd, SendCmdCallback callback) ；
E)断开连接：disConnect();
F)设置扫描时间：setScanTime(long scanTime)；
G)还有一些蓝牙辅助的一些具体方法，这里只列举一些常用的基本方法；
注意：
1、所有的方法都通过 BleManager.getInstance().xxx()调用；
2、权限判断和状态判断需要根据自己的流程去判断，然后再执行相对应的操作，库里面不执行任何逻辑，只提供蓝牙操作的相关方法。
