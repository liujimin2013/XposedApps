package com.bitstunnel.xposedpay.alipay;

import android.app.Activity;
import android.app.LauncherActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.bitstunnel.xposedpay.utils.Constants;
import com.bitstunnel.xposedpay.utils.SerializableMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

/**
 *
 *
 * Created by Mai on 2016/7/28.
 */
public class AlipayHook  {


    private void checkTransactions(Activity currActivity) throws Throwable {

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
                    }
                }
            }
        }
        //              need to compare local sqllite DB to find out the paid transaction
//             Okhttppost post the data to server.

    }

    @Deprecated
    private String getQRString(Activity currActivity) throws Throwable{

        String qrString = null;

        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_QRACTIVITY_)) {
            Class superClz = currActivity.getClass().getSuperclass();

            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_QRACTIVITY)) {
                Field field_g = XposedHelpers.findField(superClz, "g");
                 qrString = (String)field_g.get(currActivity);
                XposedBridge.log("Field g= " + qrString);
            }
        }
        return qrString;
    }

    private void collectDetail(Activity currActivity) throws Throwable{

        if (currActivity.getClass().getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY_)) {
            SharedPreferences sharedPreferences = currActivity.getSharedPreferences("XposedPay", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Class superClz = currActivity.getClass().getSuperclass();
            if (superClz.getName().equalsIgnoreCase(Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY)) {
//                XposedBridge.log("Found: " + Constants.ALIPAY_SOCIAL_DETAIL_ACTIVITY);
                Field field_F = XposedHelpers.findField(superClz, "F");
                Field field_G = XposedHelpers.findField(superClz, "G");
//                Field field_H = XposedHelpers.findField(superClz, "H");
//                Field field_I = XposedHelpers.findField(superClz, "I");
//                Field field_J = XposedHelpers.findField(superClz, "J");1
//                Field field_K = XposedHelpers.findField(superClz, "K");
//                Field field_L = XposedHelpers.findField(superClz, "L");
//                Field field_M = XposedHelpers.findField(superClz, "M");
//                Field field_N = XposedHelpers.findField(superClz, "N");

                String batch_no = (String)field_F.get(currActivity);
                String token = (String)field_G.get(currActivity);
//                String str_H = (String)field_H.get(currActivity);
//                String str_I = (String)field_I.get(currActivity);
//                String str_J = (String)field_J.get(currActivity);
//                String str_K = (String)field_K.get(currActivity);
//                boolean bool_L = field_L.getBoolean(currActivity);
//                boolean bool_M = field_M.getBoolean(currActivity);
//                boolean bool_N = field_N.getBoolean(currActivity);

//                XposedBridge.log(str_F + ":" + str_G + ":" + str_H + ":" + str_I + ":" + str_J + ":" + str_K + ":" + bool_L + ":" + bool_M + ":" + bool_N);
//                XposedBridge.log(str_F + ":" + str_G);
//                String qr_generate_string = "alipays://platformapi/startapp?appId=20000215&sourceId=qrcode&actionType=detail&batchNo= " + batch_no + "&shareObjId=null&shareObjType=null&token=" + token;

                XposedBridge.log("batch_no=" + batch_no + " " + "token=" + token);
                HashMap<String,String> aliQRMap = new HashMap<>();
                aliQRMap.put(Constants.PAYMENT_TYPE, "alipay");
                aliQRMap.put(Constants.BATCH_NO,batch_no);
                aliQRMap.put(Constants.TOKEN, token);

                SerializableMap serializableMap = new SerializableMap();
                serializableMap.setMap(aliQRMap);

                Bundle bundle = new Bundle();
                bundle.putSerializable("qrinfo", serializableMap);
                Intent intent = new Intent(Constants.POST_QR_ACTION);

                intent.putExtras(bundle);
                currActivity.sendBroadcast(intent);

            }

        }
    }


    public void hook(ClassLoader classLoader) {

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Activity currActivity = (Activity) param.thisObject;
                XposedBridge.log("Alipay Activity Name: " + currActivity.getClass().getName() + " created");

                collectDetail(currActivity);
                checkTransactions(currActivity);

            }

        });

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