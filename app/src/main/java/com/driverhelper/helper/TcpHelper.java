package com.driverhelper.helper;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.driverhelper.utils.ByteUtil;
import com.jaydenxiao.common.commonutils.ToastUitl;
import com.orhanobut.logger.Logger;
import com.vilyever.socketclient.SocketClient;
import com.vilyever.socketclient.helper.SocketClientDelegate;
import com.vilyever.socketclient.helper.SocketClientReceivingDelegate;
import com.vilyever.socketclient.helper.SocketPacketHelper;
import com.vilyever.socketclient.helper.SocketResponsePacket;
import com.vilyever.socketclient.util.CharsetUtil;

import static com.vilyever.socketclient.helper.SocketPacketHelper.ReadStrategy.AutoReadToTrailer;

/**
 * Created by Administrator on 2017/6/5.
 */

public class TcpHelper {

    final TcpHelper self = this;

    SocketClient socketClient;

    /***
     *
     * @param ip
     * @param port
     * @param timeOut
     */
    public void connect(String ip, String port, int timeOut) {
        if (!TextUtils.isEmpty(ip) && !TextUtils.isEmpty(port)) {
            self.getLocalSocketClient(ip, port, timeOut).connect();
        } else {
            ToastUitl.show("请设置正确的端口号和IP地址", Toast.LENGTH_SHORT);
        }
    }

    public SocketClient getLocalSocketClient(String ip, String port, int timeOut) {
        if (this.socketClient == null) {
            this.socketClient = new SocketClient();
        }
        __i__setupAddress(socketClient, ip, port, timeOut);
//        __i__setupAddress(socketClient, "192.168.0.124", "8098", timeOut);
        __i__setupEncoding(socketClient);
        __i__setupConstantHeartBeat(socketClient);
        __i__setupVariableHeartBeat(socketClient);
        __i__setConnectStateCallBack(socketClient);
        __i__setReceiverCallBack(socketClient);
        return this.socketClient;
    }

    /* Private Methods */

    /**
     * 设置远程端地址信息
     */
    private void __i__setupAddress(SocketClient socketClient, String ip, String port, int timeOut) {
        socketClient.getAddress().setRemoteIP(ip); // 远程端IP地址
        socketClient.getAddress().setRemotePort(port); // 远程端端口号
        socketClient.getAddress().setConnectionTimeout(timeOut); // 连接超时时长，单位毫秒
    }

    /**
     * 设置自动转换String类型到byte[]类型的编码
     * 如未设置（默认为null），将不能使用{@link SocketClient#sendString(String)}发送消息
     * 如设置为非null（如UTF-8），在接受消息时会自动尝试在接收线程（非主线程）将接收的byte[]数据依照编码转换为String，在{@link SocketResponsePacket#getMessage()}读取
     */
    private void __i__setupEncoding(SocketClient socketClient) {
        socketClient.setCharsetName(CharsetUtil.UTF_8); // 设置编码为UTF-8
    }

    private void __i__setupConstantHeartBeat(SocketClient socketClient) {
        socketClient.getHeartBeatHelper().setHeartBeatInterval(10 * 1000); // 设置自动发送心跳包的间隔时长，单位毫秒
        socketClient.getHeartBeatHelper().setSendHeartBeatEnabled(true); // 设置允许自动发送心跳包，此值默认为false
    }

    private void __i__setupVariableHeartBeat(SocketClient socketClient) {
        /**
         * 设置自动发送的心跳包信息
         * 此信息动态生成
         *
         * 每次发送心跳包时自动调用
         */
//        socketClient.getHeartBeatHelper().setSendDataBuilder(new SocketHeartBeatHelper.SendDataBuilder() {
//            @Override
//            public byte[] obtainSendHeartBeatData(SocketHeartBeatHelper helper) {
//                return BodyHelper.makeHeart();              //心跳
//            }
//        });
    }

    private void __i__setReceiverCallBack(SocketClient socketClient) {
        socketClient.getSocketPacketHelper().setReadStrategy(AutoReadToTrailer);
        socketClient.getSocketPacketHelper().setReceiveTrailerData(new byte[]{(byte) 0x7E});
        socketClient.registerSocketClientReceiveDelegate(new SocketClientReceivingDelegate() {
            @Override
            public void onReceivePacketBegin(SocketClient client, SocketResponsePacket packet) {
//                Logger.d("onReceive", "SocketClient: onReceivePacketBegin: " + packet.hashCode());
            }

            /***
             * 接收数据
             * @param client
             * @param packet
             */
            @Override
            public void onReceivePacketEnd(SocketClient client, SocketResponsePacket packet) {
                ByteUtil.printRecvHexString(packet.getData());
                if (packet.getData().length != 0) {
                    BodyHelper.handleReceiveInfo(packet.getData());
                }
            }

            @Override
            public void onReceivePacketCancel(SocketClient client, SocketResponsePacket packet) {
//                Logger.d("onReceive", "SocketClient: onReceivePacketCancel: ");
            }

            @Override
            public void onReceivingPacketInProgress(SocketClient client, SocketResponsePacket packet, float progress, int receivedLength) {
                Logger.d("onReceive", "SocketClient: onReceivingPacketInProgress: " + packet.hashCode() + " : " + progress + " : " + receivedLength);
            }
        });
    }


    private void __i__setConnectStateCallBack(final SocketClient socketClient) {
        socketClient.registerSocketClientDelegate(new SocketClientDelegate() {
            @Override
            public void onConnected(SocketClient client) {
                Logger.d("onConnected", "SocketClient: onConnected");

                if (client.getSocketPacketHelper().getReadStrategy() == SocketPacketHelper.ReadStrategy.Manually) {
                    client.readDataToLength(CharsetUtil.stringToData("Server accepted", CharsetUtil.UTF_8).length);
                }
//                if(){
//                byte[] newDatas = new byte[]{(byte) 0x7E, (byte) 0x80, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x3D, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x58, (byte) 0x68, (byte) 0x82, (byte) 0x59, (byte) 0x93, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x2A, (byte) 0x00, (byte) 0x6F, (byte) 0x48, (byte) 0x5A, (byte) 0x4C, (byte) 0x59, (byte) 0x54, (byte) 0x43, (byte) 0x37, (byte) 0x30, (byte) 0x33, (byte) 0x44, (byte) 0x4D, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x31, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x37, (byte) 0x32, (byte) 0x33, (byte) 0x35, (byte) 0x37, (byte) 0x39, (byte) 0x34, (byte) 0x31, (byte) 0x30, (byte) 0x35, (byte) 0x39, (byte) 0x36, (byte) 0x39, (byte) 0x37, (byte) 0x38, (byte) 0x31, (byte) 0x37, (byte) 0x01, (byte) 0xB6, (byte) 0xF5, (byte) 0x41, (byte) 0x33, (byte) 0x38, (byte) 0x39, (byte) 0x32, (byte) 0xD1, (byte) 0xA7, (byte) 0xA2, (byte) 0x7E};
//                sendData(newDatas);
                sendData(BodyHelper.makeRegist());
//                }

            }

            @Override
            public void onDisconnected(final SocketClient client) {
//                Logger.d("onDisconnected", "SocketClient: onDisconnected");

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        client.connect();

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                    }
                }.execute();
            }

            @Override
            public void onResponse(final SocketClient client, @NonNull SocketResponsePacket responsePacket) {
//                Logger.d("onResponse", "SocketClient: onResponse: " + responsePacket.hashCode() + " 【" + responsePacket.getMessage() + "】 " + " isHeartBeat: " + responsePacket.isHeartBeat() + " " + Arrays.toString(responsePacket.getData()));
                if (responsePacket.isHeartBeat()) {
                    return;
                }
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        client.sendString("client on " + System.currentTimeMillis());

                        try {
                            Thread.sleep(3 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        client.disconnect();

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);

                    }
                }.execute();
            }
        });
    }


    public void disConnect() {
        self.__i__disConnect(socketClient);
    }

    private void __i__disConnect(SocketClient socketClient) {
        if (!socketClient.isDisconnecting()) {
            socketClient.disconnect();
        }
    }

    String TAG = "TcpHelper";

    private void sendData(byte[] data) {
        if (socketClient != null && socketClient.isConnected()) {
            Log.d(TAG, "sendData: ");
            socketClient.sendData(data);
        }
    }


}