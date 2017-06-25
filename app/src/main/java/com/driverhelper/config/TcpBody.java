package com.driverhelper.config;

/**
 * Created by Administrator on 2017/6/5.
 */

public final class TcpBody {

    public static final byte[] HEAD = {(byte) 0x7E}; // 协议头
    public static final byte[] VERSION_CODE = {(byte) 0x80}; // 协议版本号
    public static final byte[] END = {(byte) 0x7E}; // 协议尾部

    public static class MessageID {
        public static byte[] register = {(byte) 0x01, (byte) 0x00}; // 注册
        public static byte[] unRegister = {(byte) 0x00, (byte) 0x03}; // 注销
        public static byte[] authentication = {(byte) 0x01, (byte) 0x02}; // 终端鉴权
        public static byte[] login = {(byte) 0x01, (byte) 0xF0}; // 登录
        public static byte[] logout = {(byte) 0x01, (byte) 0xF1}; // 登出
        public static byte[] queryTerminalInfoReq = {(byte) 0x81, (byte) 0x06};    //查询终端参数应答
        public static byte[] heart = {(byte) 0x00, (byte) 0x02};        //心跳包
    }

    public static class ReceiveMessageId {
        public static byte[] currency = {(byte) 0x80, (byte) 0x01};
        public static byte[] setTerminalInfo = {(byte) 0x81, (byte) 0x03}; // 设置终端信息
        public static byte[] queryTerminalInfo = {(byte) 0x81, (byte) 0x06};    //查询终端参数

    }

}