package com.bitstunnel.xposedpush;

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
    public static final String  POST_QR_ACTION = "com.bitstunnel.payment.action.POST_QR_ACTION";
    public static final String  POST_PAID_ACTION = "com.bitstunnel.payment.action.POST_PAID_ACTION";

    public enum PAYMENT_STATUS {
        PUSH, WAITING_PAY, PAID
    }

}
