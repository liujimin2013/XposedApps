package com.bitstunnel.xposedpush.utils;

/**
 *
 *
 * Created by Mai on 2016/9/9.
 */
public class Constants {

    //    public static final String SMARTDIALER_PACKAGENAME = "com.bitstunnel.smartdialer.app.release";
//    public static final String ALIPAY_PACKAGENAME = "com.eg.android.AlipayGphone";
//    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";

    public static final String PUSH_ACTION = "com.igexin.sdk.action.2mfmZrRFkp93ax9VcdqUA5";
    public static final String ALIPAY_SETMONEY = "com.bitstunnel.xposedpay.action.startAliPaySetMoney";

    public static final String NOTIFY_QRURL = "com.bitstunnel.xposedPush.action.notifyQRcode";
    public static final String AFTER_PAID_ACTION = "com.bitstunnel.payment.action.afterPaidAction";

    public enum PAYMENT_STATUS {
        PUSH, WAITING_PAY, PAID
    }

}
