package com.shsq.allen_meng.tggsimpledemo.callback;

/**
 * Created by allen_meng on 2018/5/29.
 * 用于发送命令的回调
 */

public  abstract  class SendCmdCallback {
    /**
     * @param isOK  是否发送命令成功
     * @param reply   从设备给的应答消息
     */
    public  abstract  void onSendCmdResponse(boolean isOK ,String reply,String des);
}
