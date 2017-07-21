package com.driverhelper.helper;


import android.util.Log;
import android.widget.Toast;

import com.driverhelper.beans.MessageBean;
import com.driverhelper.config.Config;
import com.driverhelper.config.ConstantInfo;
import com.driverhelper.config.TcpBody;
import com.driverhelper.other.encrypt.Encrypt;
import com.driverhelper.other.jiaminew.TestSignAndVerify;
import com.driverhelper.utils.ByteUtil;
import com.jaydenxiao.common.baserx.RxBus;
import com.jaydenxiao.common.commonutils.ToastUitl;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.driverhelper.config.ConstantInfo.strTerminalSerial;
import static com.driverhelper.config.ConstantInfo.terminalNum;
import static com.driverhelper.config.ConstantInfo.vehicleColor;
import static com.driverhelper.config.ConstantInfo.vehicleNum;
import static com.driverhelper.config.TcpBody.MessageID.register;
import static com.driverhelper.config.TcpBody.VERSION_CODE;

/**
 * Created by Administrator on 2017/6/7.
 */

public class BodyHelper {

    /***
     * 创建消息头的属性
     *
     * @param isDivision
     *            是否分包
     * @param encryption
     *            加密方式 RSA:1 无:0
     * @param length
     *            长度
     * @return
     */
    static private byte[] makeHeadAttribute(boolean isDivision, int encryption,
                                            int length) {
        String attribute = "00";
        if (isDivision) {
            attribute = attribute + "1";
        } else {
            attribute = attribute + "0";
        }
        switch (encryption) {
            case 0:
                attribute = attribute + "000";
                break;
            case 1:
                attribute = attribute + "001";
                break;
            default:
                attribute = attribute + "000";
        }
        String strLength = Integer.toBinaryString(length);
        attribute = attribute + ByteUtil.autoAddZeroByLength(strLength, 10);
        byte[] data = ByteUtil.hexString2BCD(ByteUtil.autoAddZeroByLength(
                Integer.toHexString(Integer.parseInt(attribute, 2)), 4));

        return data;
    }

    /***
     * 构造数据包的头部
     *
     * @param id
     *            消息id
     *            设备号/电话号码 11位，不足16位的左边补0
     * @return
     */
    static private byte[] makeHead(byte[] id,
                                   boolean isDivision, int encryption, int length) {
        byte[] data;
        data = ByteUtil.add(TcpBody.HEAD, VERSION_CODE);
        data = ByteUtil.add(data, id);
        data = ByteUtil.add(data,
                makeHeadAttribute(isDivision, encryption, length));
        data = ByteUtil.add(data, makePhone2BCD(ConstantInfo.terminalPhoneNumber));
        data = ByteUtil.add(data, getWaterCode());
        data = ByteUtil.add(data, getReserve());
        if (isDivision) { // 如果有分包就加这一项
            data = ByteUtil.add(data, getPackageItem(isDivision));
        }
        System.out.println("hand = ");
        ByteUtil.printHexString(data);
        return data;
    }

    /****
     * 把电话号码变成 BCD码
     *
     * @param phoneNum
     * @return
     */
    private static byte[] makePhone2BCD(String phoneNum) {
        int maxLength = 16;
        return ByteUtil.hexString2BCD(ByteUtil.autoAddZeroByLength(phoneNum,
                maxLength));
    }

    /***
     * 获取流水号
     *
     * @return
     */
    public static byte[] getWaterCode() {
        return ByteUtil.hexString2BCD(WaterCodeHelper.getWaterCode());
    }

    /***
     * 预留
     *
     * @return
     */
    public static byte[] getReserve() {
        return new byte[]{(byte) 0x00};
    }

    /****
     * 是否有分包项，和前面的“是否分包”用相同的参数
     *
     * @param isPackage
     * @return
     */
    public static byte[] getPackageItem(boolean isPackage) {
        if (!isPackage) {
            return new byte[0];
        } else {
            return new byte[]{(byte) 0x00, (byte) 0x01};
        }
    }

    /*************************************************************************************/
    /***
     * 各功能的组包,终端注册
     */
    public static byte[] makeRegist() {
        byte[] resultBody = ConstantInfo.province;
        resultBody = ByteUtil.add(resultBody, ConstantInfo.city); // 城市
        resultBody = ByteUtil.add(resultBody, ConstantInfo.makerID); // 制造商id
        resultBody = ByteUtil.add(resultBody, ByteUtil.str2Bcd(ByteUtil
                .autoAddZeroByLengthOnRight(ConstantInfo.strTerminalTYPE, 40))); // 终端型号
        resultBody = ByteUtil.add(resultBody, ByteUtil.str2Word(strTerminalSerial)); // 终端编号
        resultBody = ByteUtil.add(resultBody, ByteUtil.str2Word(ConstantInfo.strIMEI)); // IMEI
        resultBody = ByteUtil.add(resultBody, ByteUtil.str2Bcd(vehicleColor)); // 车身颜色
        resultBody = ByteUtil.add(resultBody, ByteUtil.str2Word(vehicleNum)); // 车牌号
        int bodyLength = resultBody.length;
        System.out.println("bodyLength = " + bodyLength);
        byte[] resultHead = makeHead(register, false, 0, bodyLength); // 包头固定

        byte[] result = ByteUtil.addXor(ByteUtil.add(resultHead, resultBody));
        result = ByteUtil.addEND(result); // 添加尾部
        result = ByteUtil.checkMark(result);

        ByteUtil.printHexString(result);
        return result;
    }

    /***
     * 终端注销
     *
     * @return
     */
    public static byte[] makeUnRegist() {
        int bodyLength = 0;
        byte[] result = makeHead(TcpBody.MessageID.unRegister, false, 0, bodyLength);
        result = ByteUtil.addXor(result);
        result = ByteUtil.addEND(result);
        result = ByteUtil.checkMark(result);
        ByteUtil.printHexString(result);
        return result;
    }

    /***
     * 终端鉴权 加密数据长度有问题
     */
    public static byte[] makeAuthentication() {

        long time = System.currentTimeMillis() / 1000;
//        byte[] timeByte = ByteUtil.str2Bcd(ByteUtil.decString2hexString(time));
        byte[] resultBody = ByteUtil.str2Bcd(ByteUtil
                .decString2hexString(time)); // 时间戳
        try {
            resultBody = ByteUtil.add(resultBody,
                    Encrypt.SHA256(terminalNum,
                            ConstantInfo.terminalCertificate,
                            ByteUtil.getString(ConstantInfo.certificatePassword),
                            time)); // 校验码
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Logger.e(e.getMessage());
        }
        int bodyLength = resultBody.length;
        byte[] resultHead = makeHead(TcpBody.MessageID.authentication, false, 0, bodyLength);
        byte[] result = ByteUtil.addXor(ByteUtil.add(resultHead, resultBody));
        result = ByteUtil.addEND(result);
        result = ByteUtil.checkMark(result);
        ByteUtil.printHexString(result);

        return result;
    }

    /****
     * 终端登录
     *
     * @param terminalNumber
     * @param terminalNumberPwd
     * @param insertCode
     * @return
     */
    public static byte[] makeLogin(String terminalNumber,
                                   String terminalNumberPwd, String insertCode) {
        byte[] resultBody = ByteUtil.hexString2BCD(terminalNumber); // 终端编号
        resultBody = ByteUtil.add(resultBody,
                ByteUtil.hexString2BCD(terminalNumberPwd)); // 终端密码
        resultBody = ByteUtil.add(resultBody,
                ByteUtil.hexString2BCD(insertCode)); // 平台接入码

        int bodyLength = resultBody.length;

        byte[] resultHead = makeHead(TcpBody.MessageID.login, false, 0, bodyLength);
        byte[] result = ByteUtil.addXor(ByteUtil.add(resultHead, resultBody));
        result = ByteUtil.addEND(result);
        result = ByteUtil.checkMark(result);
        ByteUtil.printHexString(result);

        return result;
    }

    /****
     * 终端登出
     *
     * @param terminalNumber
     * @param terminalNumberPwd
     * @return
     */
    public static byte[] makeLogout(String terminalNumber,
                                    String terminalNumberPwd) {
        byte[] resultBody = ByteUtil.hexString2BCD(terminalNumber); // 终端编号
        resultBody = ByteUtil.add(resultBody,
                ByteUtil.hexString2BCD(terminalNumberPwd)); // 终端密码

        int bodyLength = resultBody.length;

        byte[] resultHead = makeHead(TcpBody.MessageID.logout, false, 0, bodyLength);
        byte[] result = ByteUtil.addXor(ByteUtil.add(resultHead, resultBody));
        result = ByteUtil.addEND(result);
        result = ByteUtil.checkMark(result);
        ByteUtil.printHexString(result);

        return result;
    }

    public static byte[] makeHeart() {
        int bodylength = 0;
        byte[] result = makeHead(TcpBody.MessageID.heart, false, 0, bodylength);
        result = ByteUtil.addXor(result);
        result = ByteUtil.addEND(result);
        result = ByteUtil.checkMark(result);
        ByteUtil.printHexString(result);

        return result;
    }

    static String TAG = "ReceiveInfo";

    static List<byte[]> midDatas;
    static int index, totle;

    public static void handleReceiveInfo(byte[] data) {
        if (ByteUtil.checkXOR(data)) {
            ByteUtil.printRecvHexString(data);
            MessageBean messageBean = ByteUtil.handlerInfo(data);
            switch (messageBean.headBean.messageId) {
                case "8100":            //终端注册应答
                    if (messageBean.headBean.bodyLength == 3) {
                        switch (messageBean.bodyBean[2]) {
                            case 0:
                                break;
                            case 1:
                                RxBus.getInstance().post(Config.Config_RxBus.RX_TTS_SPEAK, "车辆已被注册");
                                break;
                            case 2:
                                RxBus.getInstance().post(Config.Config_RxBus.RX_TTS_SPEAK, "数据库中无该车辆");
                                break;
                            case 3:
                                RxBus.getInstance().post(Config.Config_RxBus.RX_TTS_SPEAK, "终端已被注册");
                                break;
                            case 4:
                                RxBus.getInstance().post(Config.Config_RxBus.RX_TTS_SPEAK, "数据库中无该终端");
                                break;
                        }
                    } else {
                        if (midDatas == null) {
                            midDatas = new ArrayList<byte[]>();
                        }
                        index = messageBean.headBean.encapsulationInfo.index;
                        totle = messageBean.headBean.encapsulationInfo.totle;
                        if (index <= totle) {
                            midDatas.add(messageBean.bodyBean);
                            if (messageBean.headBean.encapsulationInfo.index == messageBean.headBean.encapsulationInfo.totle) {
                                int totleLength = 0;
                                int posLength = 0;
                                for (byte[] midData : midDatas) {
                                    totleLength = totleLength + midData.length;
                                }
                                byte[] dataResult = new byte[totleLength];
                                for (byte[] midData : midDatas) {
                                    System.arraycopy(midData, 0, dataResult, posLength, midData.length);
                                    posLength = posLength + midData.length;
//                                    ByteUtil.printHexString(midData);
                                }
                                int posIndex = 0;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.requestWaterCode, 0, ConstantInfo.requestWaterCode.length);       //应答流水号
                                posIndex = ConstantInfo.requestWaterCode.length;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.result, 0, ConstantInfo.result.length);         //结果
                                posIndex = posIndex + ConstantInfo.result.length;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.platformNum, 0, ConstantInfo.platformNum.length);        //平台编号
                                posIndex = posIndex + ConstantInfo.platformNum.length;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.institutionNumber, 0, ConstantInfo.institutionNumber.length);      //培训机构编号
                                posIndex = posIndex + ConstantInfo.institutionNumber.length;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.terminalNum, 0, ConstantInfo.terminalNum.length);          //终端编号
                                ByteUtil.printHexString("终端编号 : ", ConstantInfo.terminalNum);
                                posIndex = posIndex + ConstantInfo.terminalNum.length;
                                System.arraycopy(dataResult, posIndex, ConstantInfo.certificatePassword, 0, ConstantInfo.certificatePassword.length);      //证书口令
                                posIndex = posIndex + ConstantInfo.certificatePassword.length;
                                int passwordLength = dataResult.length - posIndex;
                                byte[] data0 = new byte[passwordLength];
                                System.arraycopy(dataResult, posIndex, data0, 0, data0.length);
                                ConstantInfo.terminalCertificate = ByteUtil.getString(data0);     //终端证书
//                                ByteUtil.printHexString(dataResult);
                                midDatas = null;
                                RxBus.getInstance().post(Config.Config_RxBus.RX_TTS_SPEAK, "终端注册成功");
                                WriteSettingHelper.saveRegistInfo();
                            }
                        }
                    }
                    break;
                case "8001":            //服务端通用应答
                    if (messageBean.headBean.bodyLength == 5) {
                        switch (messageBean.bodyBean[4]) {
                            case 0:
                                ToastUitl.show("成功/确认", Toast.LENGTH_SHORT);
                                break;
                            case 1:
                                ToastUitl.show("失败", Toast.LENGTH_SHORT);
                                break;
                            case 2:
                                ToastUitl.show("消息有误", Toast.LENGTH_SHORT);
                                break;
                            case 3:
                                ToastUitl.show("不支持", Toast.LENGTH_SHORT);
                                break;
                        }
                    }
                    break;
            }
        }
    }

}
