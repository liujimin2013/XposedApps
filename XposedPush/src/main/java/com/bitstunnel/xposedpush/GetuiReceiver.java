package com.bitstunnel.xposedpush;

import android.content.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.bitstunnel.xposedpush.domain.Transaction;
import com.bitstunnel.xposedpush.utils.Constants;
import com.bitstunnel.xposedpush.utils.JsonConvert;
import com.igexin.sdk.PushConsts;
import com.squareup.okhttp.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by Mai on 2016/8/3.
 *
 */

// TODO:
        /* 第三方应用需要将ClientID上传到第三方服务器，并且将当前用户帐号和ClientID进行关联，
        以便以后通过用户帐号查找ClientID进行消息推送。有些情况下ClientID可能会发生变化，为保证获取最新的ClientID，
        请应用程序在每次获取ClientID广播后，都能进行一次关联绑定 */


public class GetuiReceiver extends BroadcastReceiver{
    public static final String TAG = "XposedPush";
    public static final String ALIPAY_TYPE = "alipay";
    public static final String WECHAT_TYPE = "wechat";
    public static final String SMART_DIALER = "smartdialer";

    public static final String PAYMENT_TYPE = "paymentType";
    public static final String TRACK_NO = "track_no";
    public static final String AMOUNT = "amount";
    public static final String QRURL = "qrUrl";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int holdDuration = 500;
    private static final int maxHoldTimes = 6;

    private RobotService robotService;
    private DataBaseManager dataBaseManager;

    public void setService(RobotService robotService){
        this.robotService = robotService;
    }

    public void setDbManager(DataBaseManager dbMgr) {
        this.dataBaseManager = dbMgr;
    }


    private void startApp(Context context, String packageName, String activityName) {
        Intent startupIntent = new Intent(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        ComponentName componentName = new ComponentName(packageName, activityName );
        startupIntent.setComponent(componentName);
        startupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startupIntent);
    }

    private void runAlipayBalance(Context context) {
        // check balance is avaialbe or not

        boolean resultStatus = false;

        if (!checkIsAlipay()) {
            Log.d(TAG, "Try to start Alipay App");
            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
            SystemClock.sleep(2000);
        }
//        timeCounter = 0;
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("账单", 1);
//            Log.d(TAG, "账单 resultStatus=" + resultStatus);
//            timeCounter ++;
//            if (timeCounter >= maxHoldTimes) break;
//
//        }
        SystemClock.sleep(holdDuration);
        resultStatus = findAndPerformClick("账单", 1);
        if (!resultStatus) {
            Log.e(TAG, "Failure on process Balance click");
            return;
        }
    }

    private boolean checkIsAlipay() {
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
        Log.d(TAG, rootInActiveWindow.getPackageName().toString());
        if (rootInActiveWindow.getPackageName().toString().contains("Alipay")) {
            return true;
        }
        return false;
    }

    public static boolean isNum(String str){
        return str.matches("^[-+]?(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)$");
    }

    private boolean aliInputPayeeQRCode(String amountStr, String trackNoStr) {
        HashMap<String,String> aliMap = new HashMap<>();
        aliMap.put("请填写收款金额",amountStr);
        aliMap.put("收款理由", trackNoStr);

        if (amountStr == null || !isNum(amountStr)) {
            Log.e(TAG, "Amount input is null or Not number");
            return false;
        }
        if (trackNoStr == null || trackNoStr.length() > 12) {
            Log.e(TAG, "Track Number is null or larger than 12 ");
            return false;
        }

        AccessibilityNodeInfo qrRootNode = robotService.getRootInActiveWindow();
        recursion(qrRootNode, aliMap);
        List<AccessibilityNodeInfo> BtnNodeList = qrRootNode.findAccessibilityNodeInfosByText("确定");
        if (BtnNodeList ==null || BtnNodeList.size() != 1) {
            Log.e(TAG, "find the input node is null or size not equal one");
            qrRootNode.recycle();
            return false;
        }

        AccessibilityNodeInfo btnNode = BtnNodeList.get(0);
        if (!btnNode.isEnabled()) {
            Log.d(TAG, "Confirmation button is not enable to click");
            return false;
        }
        btnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        btnNode.recycle();
        return true;
    }



    public void recursion(AccessibilityNodeInfo nodeInfo, HashMap<String, String> maps) {
        if (nodeInfo.getChildCount() == 0) {
//            Log.d(TAG, "child widget=" + nodeInfo.getClassName());
//            Log.d(TAG, "text=" + nodeInfo.getText());

            if (maps.containsKey(nodeInfo.getText())) {
                String inputStr = maps.get(nodeInfo.getText());
                Log.d(TAG, "inputStr=" + inputStr);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                inputOnFocus(inputStr);
            }
        } else {
            for (int i = 0; i < nodeInfo.getChildCount() ; i++) {
                if (nodeInfo.getChild(i) != null) {
                    recursion(nodeInfo.getChild(i), maps);
                }

            }
        }
    }

    private void inputOnFocus(String text){

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("input text " + text + "\n");
            dos.flush();
            dos.close();
            process.waitFor();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    private AccessibilityNodeInfo ancestor(AccessibilityNodeInfo nodeInfo) {
        return nodeInfo.getParent();
    }

    private boolean findAndPerformClick(final String text, final int level) {
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
        List<AccessibilityNodeInfo> rootListNode = rootInActiveWindow.findAccessibilityNodeInfosByText(text);

//        Log.d(TAG, "size=" + rootListNode.size());
        if (rootListNode == null || rootListNode.size() < 1) {
            Log.e(TAG, "No Node or moe than one node match the text");
            rootInActiveWindow.recycle();
            return false;
        }

        AccessibilityNodeInfo ancestorNode = null;
        if (rootListNode.size() > 1) {
            for (AccessibilityNodeInfo nodeInfo : rootListNode) {
                if (nodeInfo.getText().toString().equalsIgnoreCase(text)){
                    Log.d(TAG, "accessNode Text=" + nodeInfo.getText());
                    ancestorNode = nodeInfo;
                }
            }
         } else {
               ancestorNode = rootListNode.get(0);
        }
//        AccessibilityNodeInfo ancestorNode = ancestor(rootListNode.get(0));
        if (ancestorNode == null)  return  false;
        for (int i =0; i < level; i++) {
            ancestorNode = ancestor(ancestorNode);
        }
        if (!ancestorNode.isClickable()) {
            Log.e(TAG, "The node is not clickable");
            ancestorNode.recycle();
            return false;
        }

        boolean result = ancestorNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        ancestorNode.recycle();
        rootInActiveWindow.recycle();
        return result;
    }

    private void pushAction(Context context, Intent intent) {

        String payTypeMsg = null;
        String trackNoMsg = null;
        String amountMsg = null;
        Bundle bundle = intent.getExtras();
        Log.d(TAG, "PushCode=" + bundle.getInt(PushConsts.CMD_ACTION) + "?=" + PushConsts.GET_MSG_DATA);
        if (bundle.getInt(PushConsts.CMD_ACTION) == PushConsts.GET_MSG_DATA ) {
            byte[] payload = bundle.getByteArray("payload");
            if (payload != null)
            {
                String playLoadString = new String(payload);
                Log.d(TAG, "Run TriggerPay value= " + playLoadString);
                try {
                    JSONObject parseObj = new JSONObject(playLoadString);
                    payTypeMsg = parseObj.getString(PAYMENT_TYPE);
                    trackNoMsg = parseObj.getString(TRACK_NO);
                    amountMsg = parseObj.getString(AMOUNT);

                } catch (JSONException e) {
                    Log.e(TAG, "ERR: failure to parse push JSON");
                    return;
                }

                if (payTypeMsg == null || payTypeMsg.length() < 1
                        || trackNoMsg == null || trackNoMsg.length() < 1
                        || amountMsg == null || amountMsg.length() < 1) {
                    Log.e(TAG, "ERR: JSON attribute is null or empty");
                    return;
                }

//                transaction = new Transaction(payTypeMsg, trackNoMsg, Constants.PAYMENT_STATUS.PUSH.ordinal(), System.currentTimeMillis() / 1000L);
//                dataBaseManager.add(transaction);
                Log.d(TAG,"Completed to Log data on sqlite");

                switch (payTypeMsg) {
                    case ALIPAY_TYPE:
                        Log.d(TAG, "alipay type push task starting WITH " + payTypeMsg + ":" + trackNoMsg + ":" + amountMsg);
                        Intent aliIntent = new Intent(Constants.ALIPAY_SETMONEY);
                        Bundle paymentBundle = new Bundle();

                        paymentBundle.putString(PAYMENT_TYPE, payTypeMsg);
                        paymentBundle.putString(TRACK_NO, trackNoMsg);
                        paymentBundle.putString(AMOUNT, amountMsg);
                        aliIntent.putExtras(paymentBundle);
                        context.sendBroadcast(aliIntent);
                        break;
                    case WECHAT_TYPE:
//                        dispatchIntent(context, PUSH_PAY_ACTION + ".WECHAT.GETQR", trackNoMsg, amountMsg, optionMsg);
                }
            }
        } else {
            Log.d(TAG, "Push CMD_ACTION no equal GET_MSG_DATA. Exiting this push");
        }
    }

    private Response okhttpAction(final String webUrl, final String base64String, final String postJson) throws IOException {
        RequestBody requestBody = RequestBody.create(JSON, postJson);
        Request request = new Request.Builder().url(webUrl).addHeader("authorization","Basic " + base64String)
                .post(requestBody).build();
        OkHttpClient okHttpClient = new OkHttpClient();
        Response response = okHttpClient.newCall(request).execute();
        Log.d(TAG, "Response message=" + response.body().string());
        return response;
    }

//    private void postQRAction(Context context, Intent intent,Transaction tranItem){
//        // get variable from alipay hook or webchat hooker
//        // composite json and sent to web app by okhttp
//
//        Bundle bundle = intent.getExtras();
//        SerializableMap qrinfo = (SerializableMap) bundle.getSerializable("qrinfo");
//        Map<String, String> qrInfoMap = qrinfo.getMap();
//
//        Log.d(TAG,"qrInfoMap=" + qrInfoMap.toString());
//        tranItem.setTrack_no(qrInfoMap.get(TRACK_NO));
//        tranItem.setAmount(qrInfoMap.get(AMOUNT));
//        tranItem.setQr_string(qrInfoMap.get(QRURL));
//        tranItem.setStatus(Constants.PAYMENT_STATUS.WAITING_PAY.ordinal());
//
////        tranItem.setBatch_no(qrInfoMap.get("batch_no"));
////        tranItem.setToken(qrInfoMap.get("token"));
//
//        // save batch_no, token, track_no to sqllite DB
//        dataBaseManager.updateByTrackNo(tranItem);
//        Log.d(TAG, "Finished to save Post QR Information");
//
//        String postJsonStr = buildJsonFromMap(qrInfoMap);
//
//        SharedPreferences appSharePrefrences = PreferenceManager.getDefaultSharedPreferences(robotService);
//        String sendQRurl = appSharePrefrences.getString("sendQRurl", "https://bitstunnel.com/api/mobipay/applyQR");
//        String authToketn = appSharePrefrences.getString("authToken", "");
//
//        try {
//            okhttpAction(sendQRurl,authToketn,postJsonStr);
//        } catch (IOException e) {
//            Log.e(TAG, e.toString());
//        }
//    }


    private void notifyQRUrl(Intent intent) {
        Bundle qrUrlBundle = intent.getExtras();
        String paymentType = qrUrlBundle.getString(PAYMENT_TYPE);
        String trackNo = qrUrlBundle.getString(TRACK_NO);
        String amount = qrUrlBundle.getString(AMOUNT);
        String qrUrl = qrUrlBundle.getString(QRURL);

        Log.d(TAG, "Bundle string=" + paymentType +":"+ trackNo  + ":"
                + amount + ":" + qrUrl + ":" + qrUrlBundle.getString("codeId"));
        dataBaseManager.add(paymentType, trackNo, amount, qrUrl, System.currentTimeMillis()/1000L, Constants.PAYMENT_STATUS.WAITING_PAY.ordinal());
        // save data to database
        // okhttpPost to webapp
    }

//    public String buildJsonFromMap(Map<String,String> inputMap) {
//        JSONObject jsonObject = new JSONObject(inputMap);
//        return jsonObject.toString();
//    }

    private void setMoneyTask(final String paymentType, final String amount, final String track_no) {

        boolean resultStatus = false;
        int count = 0;
        while (!resultStatus) {
            SystemClock.sleep(holdDuration);
            resultStatus = findAndPerformClick("添加收款理由",0);
            count++;
            if (count >= maxHoldTimes ) break;
        }
        if (!resultStatus) {
            Log.e(TAG, "Failure on click setting reason");
            return;
        }
        // try to input message
        SystemClock.sleep(300);
        resultStatus = aliInputPayeeQRCode(amount, track_no);
        if (!resultStatus) {
            Log.e(TAG, "Failure on process InputPayeeQRCode");
        }
    }

    private void updatePaidStatus(Intent intent) {

        HashMap<String, String> paidMap = (HashMap<String,String>) intent.getSerializableExtra("paidMap");
        Log.d(TAG, "paidMap in pushapp=" + paidMap.toString());
        List<Transaction> trackNoList = dataBaseManager.queryWaitingPay(Constants.PAYMENT_STATUS.WAITING_PAY.ordinal());
        List<Object> mapList = new ArrayList<>();
        Log.d(TAG, trackNoList.toString());
        for (Transaction transaction : trackNoList) {
            Map<String, String> postMap = new HashMap<>();
            String bizTranNo = paidMap.get(transaction.getTrack_no());
            if (bizTranNo != null) {
                dataBaseManager.updateByTrackNo("alipay", transaction.getTrack_no(), Constants.PAYMENT_STATUS.PAID.ordinal());

                postMap.put("paymentType", "alipay");
                postMap.put("bizNo", bizTranNo);
                postMap.put("trackNo", transaction.getTrack_no());
                mapList.add(postMap);
            }
        }

// make mapList to json
        JSONArray jsonArray = JsonConvert.toJson(mapList);


        SharedPreferences appSharePrefrences = PreferenceManager.getDefaultSharedPreferences(robotService);
        String sendQRurl = appSharePrefrences.getString("sendQRurl", "https://bitstunnel.com/api/mobipay/applyQR");
        String authToketn = appSharePrefrences.getString("authToken", "");
        Log.d(TAG, "okhttp: sendQRUrl To=" + sendQRurl + " authToken=" + authToketn + " with JsonString=" + jsonArray.toString());
//
//        try {
//            okhttpAction(sendQRurl,authToketn,postJsonStr);
//        } catch (IOException e) {
//            Log.e(TAG, e.toString());
//        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case Constants.PUSH_ACTION:
                Log.d(TAG, "PushAction starting");
                pushAction(context,intent);
                break;

            case "com.bitstunnel.xposedPush.action.typeInput":
                Log.d(TAG, "XposedPush input alipay setMoney activty starting");
                Bundle setBundle = intent.getExtras();
                setMoneyTask(setBundle.getString(PAYMENT_TYPE), setBundle.getString(AMOUNT), setBundle.getString(TRACK_NO));
                break;

            case Constants.NOTIFY_QRURL:
                Log.d(TAG, "Start Get QRUrl Bundle from XposedPay.");
                notifyQRUrl(intent);
                break;

            case Constants.AFTER_PAID_ACTION:
                Log.d(TAG, "Starting After Paid Action");
                runAlipayBalance(context);
                break;
            case "com.bitstunnel.alipay.action.postPaidList":
                Log.d(TAG, "Starting Post Paid List");
                updatePaidStatus(intent);
                break;
        }
    }

//    @Deprecated
//    public boolean findFirstByClass(AccessibilityNodeInfo nodeInfo, final String className) {
//        boolean result = false;
//        if (nodeInfo.getChildCount() == 0) {
//            Log.d(TAG, nodeInfo.getClassName().toString() + ":" + className);
//            if (nodeInfo.getClassName().toString().contains(className)) {
//                result = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                Log.d(TAG, "Return from findFirstByClass result=" + result);
//                return result;
//            }
//        } else {
//            for (int i=0 ; i < nodeInfo.getChildCount() ; i++) {
//                if (nodeInfo.getChild(i) != null) {
//                    result =  findFirstByClass(nodeInfo.getChild(i), className);
//                    if (result) return result;
//                }
//            }
//        }
//        return result;
//    }
}



//
//    private void runAlipayBalance(Context context) {
//        // check balance is avaialbe or not
//
//        boolean resultStatus = false;
//        int timeCounter = 0;
////        setPending();
//        //Check forewindow is alipay or not
//        if (!checkIsAlipay()) {
//            Log.d(TAG, "Try to start Alipay App");
//            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
//            SystemClock.sleep(2000);
////            pending();
//        }
//
//
//        timeCounter = 0;
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("账单", 1);
//            Log.d(TAG, "账单 resultStatus=" + resultStatus);
//            timeCounter ++;
//            if (timeCounter >= maxHoldTimes) break;
//
//        }
////        successStatus = findAndPerformClick("账单", 1);
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process Balance click");
//            return;
//        }
//
//        resultStatus = false;
//        timeCounter = 0;
//        while ( !resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("筛选", 0);
//            Log.d(TAG, "筛选  resultStatus=" + resultStatus);
//            timeCounter++;
//            if (timeCounter >= maxHoldTimes) break;
//        }
//
//        if (!resultStatus) {
//            Log.e(TAG, "Failure to click on Filter button");
//            return;
//        }
//
//        resultStatus = false;
//        timeCounter = 0;
//        while(!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = backFromFilter();
//            Log.d(TAG, "backFilter counter resultStatus=" + resultStatus);
//            timeCounter++;
//            if (timeCounter >= maxHoldTimes) break;
//        }
//
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process back from filter window");
//        }
//
//        resultStatus = false;
//        timeCounter = 0;
//        while ( !resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = backFromBalToMain();
//            Log.d(TAG, "BackToMain   resultStatus=" + resultStatus);
//            timeCounter++;
//            if (timeCounter >= maxHoldTimes) break;
//        }
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process back from Balance window to main window");
//        }
//
//        pending();
//        successStatus = findAndPerformClick("筛选", 0);
//        if (!successStatus) {
//            Log.e(TAG, "Failure to click on Filter button");
//            return;
//        }
//        setPending();
//
//        pending();
//        successStatus = backFromFilter();
//        if (!successStatus) {
//            Log.e(TAG, "Failure on process back from filter window");
//        }
//        setPending();
//
//        pending();
//        successStatus = backFromBalToMain();
//        if (!successStatus) {
//            Log.e(TAG, "Failure on process back from Balance window to main window");
//        }
//        setPending();
//
//    }


//    private boolean findOnly(final String text) {
//        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
//        List<AccessibilityNodeInfo> rootListNode = rootInActiveWindow.findAccessibilityNodeInfosByText(text);
//        if (rootListNode == null || rootListNode.size() != 1) {
//            Log.e(TAG, "No Node or moe than one node match the text in findOnly method");
//            rootInActiveWindow.recycle();
//            return false;
//        }
//        return true;
//    }



//    private void tapOnPosition(int x, int y) {
//        try {
//            Process process = Runtime.getRuntime().exec("su");
//            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
//            dos.writeBytes("input tap " + x + " " + y + "\n");
//            dos.flush();
//            dos.close();
//            process.waitFor();
//        } catch (IOException e) {
//            Log.e(TAG, e.toString());
//        } catch (InterruptedException e) {
//            Log.d(TAG,e.toString());
//        }
//    }

//    private void runSmartDialer(Context context, String traceIdMsg, String amountMsg){
//        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
//
//        if (rootInActiveWindow == null) {
//            Log.e(TAG, "SmartDialer rootWindow return null");
//            return;
//        }
//
//        if (!rootInActiveWindow.getPackageName().toString().contains("smartdialer")) {
//            Log.d(TAG, "The fore windows is smartdialer windows");
//            startApp(context,"com.bitstunnel.smartdialer.app.release","com.bitstunnel.smartdialer.app.CommandBoardActivity");
//        }
//
//        List<AccessibilityNodeInfo> threeList = rootInActiveWindow.findAccessibilityNodeInfosByText("Three HK");
//
//        if (threeList == null || threeList.size() != 1) {
//            Log.e(TAG, "No Three Collection Node found");
//            return;
//        }
//
//        AccessibilityNodeInfo threeNode = threeList.get(0);
//        if (threeNode.isClickable()) {
//            boolean b = threeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            threeNode.recycle();
//            Log.d(TAG, "three click result=" + b);
//            return;
//        }
//        Log.d(TAG, "three unclick able");
//
//        rootInActiveWindow.recycle();
//    }


//    private void runAlipayQR(Context context, final String traceNoStr, final String amountStr){
//        //收款 1
//        //设置金额 0
//        //收款理由 0
//        boolean resultStatus = false;
//        int count = 0;
//        if (!checkIsAlipay()) {
//            Log.d(TAG, "Try to start Alipay App");
//            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
//            SystemClock.sleep(2000);
//        }
//
//
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("收款", 1);
//            Log.d(TAG, "收款 resultStatus=" + resultStatus);
//            count++;
//            if (count >= maxHoldTimes ) break;
//        }
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process payment collection");
//            return;
//        }
//
//        //
//        SystemClock.sleep(1000);
//        boolean findResult = findOnly("设置金额");
//        if (!findResult) {
//            // if it is last payment screen. back to main screen
//            boolean status = backFromPayeeSreen();
//            if (!status) {
//                Log.d(TAG, "Failure to back From Paid screen to Main screen ");
//                return;
//            }
//
//            while (!resultStatus) {
//                SystemClock.sleep(holdDuration);
//                resultStatus = findAndPerformClick("收款", 1);
//                Log.d(TAG, "收款 resultStatus=" + resultStatus);
//                count++;
//                if (count >= maxHoldTimes ) break;
//            }
//            if (!resultStatus) {
//                Log.e(TAG, "Failure on process payment collection again");
//                return;
//            }
//        }
//
//
//        resultStatus = false;
//        count=0;
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("设置金额", 0);
//            Log.d(TAG, "设置金额 resultStatus=" + resultStatus);
//            count++;
//            if (count >= maxHoldTimes ) break;
//        }
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process setting amount");
//            return;
//        }
//
//        resultStatus = false;
//        SystemClock.sleep(500);
//        resultStatus = findAndPerformClick("添加收款理由",0);
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process setting amount");
//            return;
//        }
//
//        // try to input message
//        resultStatus = false;
//        SystemClock.sleep(500);
//        resultStatus = aliInputPayeeQRCode(amountStr, traceNoStr);
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process InputPayeeQRCode");
//            return;
//        }
//
//
//
//        resultStatus = false;
//        SystemClock.sleep(500);
//        resultStatus = backFromPayeeSreen();
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on final back from Payee QR screen");
//            return;
//        }
//
//    }


//    private void runAlipayQR(Context context, final String traceIdMsg, final String amountMsg) {
//
//        boolean resultStatus = false;
//        int count = 0;
//
//        if (!checkIsAlipay()) {
//            Log.d(TAG, "Try to start Alipay App");
//            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
//            SystemClock.sleep(2000);
//        }
//
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus = findAndPerformClick("AA收款", 1);
//            Log.d(TAG, "2-AA收款 resultStatus=" + resultStatus);
//            count++;
//            if (count >= maxHoldTimes ) break;
//        }
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process AA collection");
//            return;
//        }
//
//        SystemClock.sleep(1500);
//        touchWebSelect();
//
//        SystemClock.sleep(1000);
//        resultStatus = aliInputQRCode(amountMsg, "1",traceIdMsg);
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process InputQRCode");
//            return;
//        }
//
//        resultStatus = false;
//        while (!resultStatus) {
//            SystemClock.sleep(holdDuration);
//            resultStatus =  closeDetailActivity();
//            Log.d(TAG, "2-Close resultStatus=" + resultStatus);
//            count++;
//            if (count >= maxHoldTimes) break;
//        }
//        if (!resultStatus) {
//            Log.e(TAG, "Failure on process close detail Activity");
//        }
//    }

//    private boolean closeDetailActivity(){
//        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
//        return findAndPerformClick("完成",0);
//    }

//    private void touchWebSelect() {
//        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
//
//        Rect rect = new Rect();
//        rootInActiveWindow.getBoundsInScreen(rect);
//
//        int width = rect.right - rect.left;
//        int height = rect.bottom - rect.top;
//
//        int x = width / 2;
//        int y = (height / 3) + 50;
//        Log.d(TAG, x + " : " + y);
//        tapOnPosition(x, y);
//        rootInActiveWindow.recycle();
//    }



//    private boolean aliInputQRCode(String amountMsg, String number, String traceIdMsg ){
//        HashMap<String, String> aliMap = new HashMap<>();
//        aliMap.put("最多10,000元", amountMsg);
//        aliMap.put("必填", number);
//        aliMap.put("小伙伴们速速交钱～", traceIdMsg);
//
//        AccessibilityNodeInfo qrRootNode = robotService.getRootInActiveWindow();
//
//        recursion(qrRootNode, aliMap);
//
//        List<AccessibilityNodeInfo> BtnNodeList = qrRootNode.findAccessibilityNodeInfosByText("确定");
//        if (BtnNodeList ==null || BtnNodeList.size() != 1) {
//            Log.e(TAG, "find the input node is null or size not equal one");
//            qrRootNode.recycle();
//            return false;
//        }
//
//        AccessibilityNodeInfo btnNode = BtnNodeList.get(0);
//        btnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        btnNode.recycle();
//        return true;
//    }

//    public void aliShowQRPage(){
//        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
//        List<AccessibilityNodeInfo> jumpQRPage = rootInActiveWindow.findAccessibilityNodeInfosByText("扫一扫收款");
//        if (jumpQRPage == null || jumpQRPage.size() != 1) {
//            Log.e(TAG, "Can't find node that jump to QRCode");
//        }
//        AccessibilityNodeInfo jumpNode = jumpQRPage.get(0);
//        jumpNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        jumpNode.recycle();
//    }


//    private AccessibilityNodeInfo findAndFocus(AccessibilityNodeInfo rootNodeInfo, String findText) {
//        List<AccessibilityNodeInfo> nodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText(findText);
//        if (nodeInfoList == null) {
//            Log.d(TAG, "nodeInfolist is null" );
//        }
//        Log.d(TAG, findText + ":" + nodeInfoList.size() );
//
//        if (nodeInfoList ==null || nodeInfoList.size() != 1) {
//            Log.e(TAG, "find the input node is null or size not equal one");
//            return null;
//        }
//
//        AccessibilityNodeInfo nodeInfo = nodeInfoList.get(0);
//        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
//        return nodeInfo;
//    }

//    private boolean backFromPayeeSreen() {
//        AccessibilityNodeInfo paidWindow = null;
//        int counter = 0;
//
//        while (paidWindow == null) {
//            SystemClock.sleep(holdDuration);
//            paidWindow = robotService.getRootInActiveWindow();
//            counter++;
//            if (counter >= maxHoldTimes) break;
//        }
//
//        boolean result = findFirstByClass(paidWindow, "ImageButton");
//        paidWindow.recycle();
//        return result;
//    }



//
//    public boolean backFromFilter() {
//
//        AccessibilityNodeInfo filterWindow = robotService.getRootInActiveWindow();
//        return findFirstByClass(filterWindow, "ImageButton");
//    }

//    public boolean backFromBalToMain() {
//        AccessibilityNodeInfo balWindow = robotService.getRootInActiveWindow();
//        return findFirstByClass(balWindow, "ImageButton");
//    }