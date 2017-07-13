package com.bitstunnel.xposedpay.alipay;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import com.bitstunnel.xposedpay.utils.Constants;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 *
 *
 * Created by Mai on 2016/7/28.
 */
public class AlipayHook  {

    private void checkTransactions(Activity currActivity, ClassLoader classLoader) throws Throwable {

        HashMap<String, String> bListMap = new HashMap<>();

        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_BILLLIST_ACTIVITY_)) {

            Class superClz = currActivity.getClass().getSuperclass();

            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_BILLLIST_ACTIVITY)) {
                Field blAdapter_field = XposedHelpers.findField(superClz, "v");
                Object blAdapter_obj = blAdapter_field.get(currActivity);

                XposedBridge.log("blAdapter_obj class name: " + blAdapter_obj.getClass().getName());
                Field blist_field = XposedHelpers.findField(blAdapter_obj.getClass(), "f");

                List<Object> bList_obj = (List)blist_field.get(blAdapter_obj);

                if (bList_obj != null && bList_obj.size() > 0) {
                    for (Object obj : bList_obj) {
                        XposedBridge.log("SingleItemList String= " + obj.toString());
                        addPaidItemToMap(bListMap, obj);
                    }

                Intent intent = new Intent("com.bitstunnel.alipay.action.postPaidList");
                XposedBridge.log("broadcast paidMap=" + bListMap.toString());
                intent.putExtra("paidMap", bListMap);
                currActivity.sendBroadcast(intent);


                SystemClock.sleep(500);
                currActivity.finish();
                } else {
                    Class searchClz= XposedHelpers.findClass("com.alipay.mobile.bill.list.ui.BillSearchActivity_", classLoader);
                    if (searchClz != null) {
                        Intent intent = new Intent(currActivity, searchClz);
                        currActivity.startActivity(intent);
                    } else {
                        XposedBridge.log("Failure to get BillSearchActivity Class");
                    }
                }
            }
        }
    }

    private String getStringField(final Object obj, final String fieldName) throws IllegalAccessException {
        Field sField = XposedHelpers.findField(obj.getClass(), fieldName);
        if (sField == null) {
            XposedBridge.log("Failure to find specific fieldname: " + fieldName + " with class: " + obj.getClass());
            return null;
        }

        return String.valueOf(sField.get(obj));

    }

    private void addPaidItemToMap(HashMap<String, String> bListMap, Object obj) throws IllegalAccessException {

        String bizNo = getStringField(obj, "bizInNo");

        String bizType = getStringField(obj, "bizType");
        if (bizType == null || !bizType.equalsIgnoreCase("D_TRANSFER")) {
//            XposedBridge.log("bizType=" + bizType);
            return;
        }

        String bizSubType = getStringField(obj, "bizSubType");
        if (bizSubType == null || !bizSubType.equalsIgnoreCase("1135")){
//            XposedBridge.log("bizSubType=" + bizSubType);
            return;
        }

        String custTitleTmp = getStringField(obj, "consumeTitle");
        String customerTitle;
        String amount;
        if (custTitleTmp ==null || !custTitleTmp.contains("-")) {
//            XposedBridge.log("custTitleTmp=" + custTitleTmp);
           return;
        }
        customerTitle = custTitleTmp.substring(custTitleTmp.indexOf("-") + 1);

        String amountTmp = getStringField(obj, "consumeFee");
        if (amountTmp == null || !amountTmp.startsWith("+")) {
//            XposedBridge.log("amountTmp=" + amountTmp);
            return;
        }
        amount = amountTmp.substring(1, amountTmp.length());

        Integer statusNum = Integer.valueOf(getStringField(obj, "consumeStatus"));
        if (statusNum != 2) {
            return;
        }

        String gmtCreate = getStringField(obj, "gmtCreate");
        XposedBridge.log("=====>  " + bizNo + ":" + customerTitle + ":" + amount + ":" + gmtCreate );
        bListMap.put(customerTitle, bizNo);
    }

    private void closeSearchActivity(Activity currActivity) {
        if (currActivity.getClass().getName().equalsIgnoreCase("com.alipay.mobile.bill.list.ui.BillSearchActivity_")) {
            SystemClock.sleep(500);
            currActivity.finish();
            XposedBridge.log("com.alipay.mobile.bill.list.ui.BillSearchActivity_  finished");
        }
    }

    public void hook(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Activity thisActvity = (Activity) param.thisObject;

                if (thisActvity.getClass().getName().equalsIgnoreCase("com.eg.android.AlipayGphone.AlipayLogin")) {
                    XposedBridge.log("Hook Receiver to AlipayLogin Activity");
                    AliPayReceiver aliPayReceiver = new AliPayReceiver();
                    aliPayReceiver.setClassLoader(classLoader);
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("com.bitstunnel.xposedpay.action.startAliPaySetMoney");
                    thisActvity.registerReceiver(aliPayReceiver, intentFilter);
                }


                if (thisActvity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_SETMONEY_ACTIVITY_)) {
                    Class superClz = thisActvity.getClass().getSuperclass();
                    if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_SETMONEY_ACTIVITY)) {
                        Method bMethod = XposedHelpers.findMethodExact(superClz, "b");
                        if (bMethod !=null) {
                            XposedBridge.log("Found b method in " + superClz.getName());
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Activity currActivity = (Activity) param.thisObject;
                XposedBridge.log("Alipay Activity Name: " + currActivity.getClass().getName() + "after resumed");
//                getPayeeQRString(currActivity);
                checkTransactions(currActivity, classLoader);
                closeSearchActivity(currActivity);
            }

        });


        XposedHelpers.findAndHookMethod(Activity.class, "onActivityResult", int.class, int.class, Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity curActivity = (Activity) param.thisObject;

                if (curActivity.getClass().getName().equalsIgnoreCase("com.eg.android.AlipayGphone.AlipayLogin")) {
                    XposedBridge.log("com.eg.android.AlipayGphone.AlipayLogin before OnActiivtyResult meothod");
                    int requestCode = (int)param.args[0];
                    int resultCode = (int) param.args[1];
                    Intent resultIntent = (Intent) param.args[2];
                    String amount = resultIntent.getStringExtra("qr_money");
                    String trackNo = resultIntent.getStringExtra("beiZhu");
                    String qrCodeUrl = resultIntent.getStringExtra("qrCodeUrl");
                    String codeId  = resultIntent.getStringExtra("codeId");
                    XposedBridge.log("requstCode=" + requestCode + "resultCode=" + resultCode + "amount=" + amount + " reason=" + trackNo + " qrCodeUrl=" + qrCodeUrl + " codeId=" + codeId);
                    Bundle qrBundle = new Bundle();
                    qrBundle.putString("paymentType", "alipay");
                    qrBundle.putString("amount", amount);
                    qrBundle.putString("track_no", trackNo);
                    qrBundle.putString("qrUrl", qrCodeUrl);
                    qrBundle.putString("codeId", codeId);

                    Intent qrIntent = new Intent("com.bitstunnel.xposedPush.action.notifyQRcode");
                    qrIntent.putExtras(qrBundle);
                    curActivity.sendBroadcast(qrIntent);
                }
            }
        });

    }
}

// No more need snippet

//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                super.beforeHookedMethod(param);
//
//                Activity currActivity = (Activity) param.thisObject;
//                XposedBridge.log("Alipay Activity Name: " + currActivity.getClass().getName() + " created");
//
//
//                if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_EMPTY_ACTIVITY_)) {
//                    Class superClz = currActivity.getClass().getSuperclass();
//
//                    if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_EMPTY_ACTIVITY)) {
//
//                        XposedBridge.log("Found: " + Constants.ALIPAY_EMPTY_ACTIVITY);
//                        Field field_a = XposedHelpers.findField(superClz, "a");
//                        Field field_b = XposedHelpers.findField(superClz, "b");
//                        Field field_c = XposedHelpers.findField(superClz, "c");
//
//                        String str_a = (String) field_a.get(currActivity);
//                        String str_b = (String) field_b.get(currActivity);
//                        long long_c = field_c.getLong(currActivity);
//
//                        XposedBridge.log("tradeNO=" + str_a + ";" + "bizType=" + str_b + ";" + "gmtCreate=" + long_c);
//                    }
//                }
//
//                if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_TRADE_DETAIL_ACTIVITY_)) {
//
//                    Class superClz = currActivity.getClass().getSuperclass();
//
//                    if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_TRADE_DETAIL_ACTIVITY)) {
//                        XposedBridge.log("Found :" + Constants.ALIPAY_TRADE_DETAIL_ACTIVITY);
//                        Bundle bundle =  currActivity.getIntent().getExtras();
//                        if (bundle == null) {
//                            XposedBridge.log("bundle is null");
//                            return;
//                        }
//
//                        String trade_no = null;
//                        trade_no = bundle.getString("tradeNO");
//                        if (trade_no == null ) {
//                            trade_no = bundle.getString("no");
//                        }
//
//                        String bizType = null;
//                        bizType = bundle.getString("bizType");
//                        if (bizType == null ) {
//                            bizType = bundle.getString("biz");
//                        }
//
//                        long gmt_create = bundle.getLong("bizType");
//
//                        XposedBridge.log("tradeNo=" + trade_no + " bizType=" + bizType + "gmtCreate=" + gmt_create);
//                    }
//                }
////
////                if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_QR_ACTIVITY_)) {
////                    XposedBridge.log("Found: " + Constants.ALIPAY_PAYEE_QR_ACTIVITY_ );
////                    Class superClz = currActivity.getClass().getSuperclass();
////                   if  (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_QR_ACTIVITY)) {
////                       XposedBridge.log("Found: " + Constants.ALIPAY_PAYEE_QR_ACTIVITY );
////                       Field field_B = XposedHelpers.findField(superClz, "B");
////                       Field field_C = XposedHelpers.findField(superClz, "C");
////                       int int_B = field_B.getInt(currActivity);
////                       int int_C = field_C.getInt(currActivity);
////                       XposedBridge.log("int_b= " + int_B + " int_c=" + int_C);
////                    }
////                }
//            }


//                        for (Field field : obj.getClass().getDeclaredFields()) {
//                            XposedBridge.log(obj.getClass().getName() + ": fields=" + field.getName() );
//                        }
//
//                        for (Method method : obj.getClass().getDeclaredMethods()) {
//                            XposedBridge.log(obj.getClass().getName() + ": method=" + method.getName());
//                        }


//    private void getPayeeQRString(Activity currActivity) throws Throwable {
//        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_QR_ACTIVITY_)) {
//            Class superClz = currActivity.getClass().getSuperclass();
//            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_PAYEE_QR_ACTIVITY)) {
//                Field field_r = XposedHelpers.findField(superClz, "r");
//                Field field_s = XposedHelpers.findField(superClz, "s");
//                Field field_t = XposedHelpers.findField(superClz, "t");
//                Field field_u = XposedHelpers.findField(superClz, "u");
//                Field field_v = XposedHelpers.findField(superClz, "v");
//                Field field_G = XposedHelpers.findField(superClz, "G");
//
//                String amount = (String) field_r.get(currActivity);
//                String track_no = (String) field_s.get(currActivity);
//                String customer_id = (String) field_t.get(currActivity);
//                String str_u = (String) field_u.get(currActivity);
//                String str_v = (String) field_v.get(currActivity);
//                String qr_str = (String) field_G.get(currActivity);
//
//                XposedBridge.log("(comment)track_no=" + track_no + " amount=" + amount  + " customer_id=" + customer_id + " QRstring=" + qr_str + ">>" + str_u + ":" + str_v);
//                HashMap<String,String> aliQRMap = new HashMap<>();
//                aliQRMap.put(Constants.PAYMENT_TYPE, "alipay");
//                aliQRMap.put(Constants.ALIPAY_TRACK_NO, track_no);
//                aliQRMap.put(Constants.ALIPAY_QR_AMOUNT, amount);
//                aliQRMap.put(Constants.ALIPAY_QR_URL, qr_str);
//
//
//                SerializableMap serializableMap = new SerializableMap();
//                serializableMap.setMap(aliQRMap);
//
//                Bundle bundle = new Bundle();
//                bundle.putSerializable("qrinfo", serializableMap);
//                Intent intent = new Intent(Constants.POST_QR_ACTION);
//
//                intent.putExtras(bundle);
//                currActivity.sendBroadcast(intent);
//            }
//        }
//    }



//    @Deprecated
//    private void collectDetail(Activity currActivity) throws Throwable{
//
//        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY_)) {
////            SharedPreferences sharedPreferences = currActivity.getSharedPreferences("XposedPay", Context.MODE_PRIVATE);
////            SharedPreferences.Editor editor = sharedPreferences.edit();
//
//            Class superClz = currActivity.getClass().getSuperclass();
//            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY)) {
////                XposedBridge.log("Found: " + Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY);
//                Field field_F = XposedHelpers.findField(superClz, "F");
//                Field field_G = XposedHelpers.findField(superClz, "G");
////                Field field_H = XposedHelpers.findField(superClz, "H");
////                Field field_I = XposedHelpers.findField(superClz, "I");
////                Field field_J = XposedHelpers.findField(superClz, "J");1
////                Field field_K = XposedHelpers.findField(superClz, "K");
////                Field field_L = XposedHelpers.findField(superClz, "L");
////                Field field_M = XposedHelpers.findField(superClz, "M");
////                Field field_N = XposedHelpers.findField(superClz, "N");
//
//                String batch_no = (String)field_F.get(currActivity);
//                String token = (String)field_G.get(currActivity);
////                String str_H = (String)field_H.get(currActivity);
////                String str_I = (String)field_I.get(currActivity);
////                String str_J = (String)field_J.get(currActivity);
////                String str_K = (String)field_K.get(currActivity);
////                boolean bool_L = field_L.getBoolean(currActivity);
////                boolean bool_M = field_M.getBoolean(currActivity);
////                boolean bool_N = field_N.getBoolean(currActivity);
//
////                XposedBridge.log(str_F + ":" + str_G + ":" + str_H + ":" + str_I + ":" + str_J + ":" + str_K + ":" + bool_L + ":" + bool_M + ":" + bool_N);
////                XposedBridge.log(str_F + ":" + str_G);
////                String qr_generate_string = "alipays://platformapi/startapp?appId=20000215&sourceId=qrcode&actionType=detail&batchNo= " + batch_no + "&shareObjId=null&shareObjType=null&token=" + token;
//
//                XposedBridge.log("batch_no=" + batch_no + " " + "token=" + token);
//                HashMap<String,String> aliQRMap = new HashMap<String,String>();
//                aliQRMap.put(Constants.PAYMENT_TYPE, "alipay");
//                aliQRMap.put(Constants.BATCH_NO,batch_no);
//                aliQRMap.put(Constants.TOKEN, token);
//
//                SerializableMap serializableMap = new SerializableMap();
//                serializableMap.setMap(aliQRMap);
//
//                Bundle bundle = new Bundle();
//                bundle.putSerializable("qrinfo", serializableMap);
//                Intent intent = new Intent(Constants.POST_QR_ACTION);
//
//                intent.putExtras(bundle);
//                currActivity.sendBroadcast(intent);
//
//            }
//        }
//    }


//    @Deprecated
//    private String getQRString(Activity currActivity) throws Throwable{
//
//        String qrString = null;
//
//        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_QRACTIVITY_)) {
//            Class superClz = currActivity.getClass().getSuperclass();
//
//            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_QRACTIVITY)) {
//                Field field_g = XposedHelpers.findField(superClz, "g");
//                 qrString = (String)field_g.get(currActivity);
//                XposedBridge.log("Field g= " + qrString);
//            }
//        }
//        return qrString;
//    }




//        Deprecated code
//        XposedHelpers.findAndHookMethod(NotificationManager.class, "notify", String.class, int.class, Notification.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                super.beforeHookedMethod(param);
//
//                String str_value = (String) param.args[0];
//                Integer int_value = (Integer)param.args[1];
//                Notification notification = (Notification) param.args[2];
//                XposedBridge.log("Notify: " + str_value + " : " + int_value + " : " + notification.toString());
//
//                String NotifyPackageName = notification.contentView.getPackage();
//                if (NotifyPackageName.contains("AlipayGphone")) {
//
//                }
//            }
//
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//                XposedBridge.log("NotificationManager notify after-hooked");
//            }
//        });