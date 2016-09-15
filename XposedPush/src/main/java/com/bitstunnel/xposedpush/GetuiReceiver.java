package com.bitstunnel.xposedpush;

import android.content.*;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.bitstunnel.xposedpush.domain.Transaction;
import com.igexin.sdk.PushConsts;
import com.squareup.okhttp.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
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
    public static final String TRACE_ID = "trace_id";
    public static final String AMOUNT = "amount";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RobotService robotService;
    private DataBaseManager dataBaseManager;
    private Transaction tranItem;

    private static boolean isHold = true;

    public void setService(RobotService robotService){
        this.robotService = robotService;
    }

    public void setDbManager(DataBaseManager dbMgr) {
        this.dataBaseManager = dbMgr;
    }

    public void changeHold() {
        isHold = false;
    }

    private void setPending() {
        isHold = true;
    }

    private void pending() {
        int i = 0;
        while (true == isHold) {
            SystemClock.sleep(200);
            i++;
            if (i > 30) break;
        }
    }


    private void startApp(Context context, String packageName, String activityName) {
        Intent startupIntent = new Intent(Intent.ACTION_MAIN);
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        ComponentName componentName = new ComponentName(packageName, activityName );
        startupIntent.setComponent(componentName);
        startupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startupIntent);
    }


    private void runSmartDialer(Context context, String traceIdMsg, String amountMsg){
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();

        if (rootInActiveWindow == null) {
            Log.e(TAG, "SmartDialer rootWindow return null");
            return;
        }

        if (!rootInActiveWindow.getPackageName().toString().contains("smartdialer")) {
            Log.d(TAG, "The fore windows is smartdialer windows");
            startApp(context,"com.bitstunnel.smartdialer.app.release","com.bitstunnel.smartdialer.app.CommandBoardActivity");
        }

        List<AccessibilityNodeInfo> threeList = rootInActiveWindow.findAccessibilityNodeInfosByText("Three HK");

        if (threeList == null || threeList.size() != 1) {
            Log.e(TAG, "No Three Collection Node found");
            return;
        }

        AccessibilityNodeInfo threeNode = threeList.get(0);
        if (threeNode.isClickable()) {
            boolean b = threeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            threeNode.recycle();
            Log.d(TAG, "three click result=" + b);
            return;
        }
        Log.d(TAG, "three unclick able");

        rootInActiveWindow.recycle();

    }


    private void runAlipayQR(Context context, final String traceIdMsg, final String amountMsg) {

        boolean successStatus = true;

        setPending();
        //Check forewindow is alipay or not
        if (!checkIsAlipay()) {
            Log.d(TAG, "Try to start Alipay App");
            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
            pending();
        }

        successStatus = findAndPerformClick("AA收款", 1);
        if (!successStatus) {
            Log.e(TAG, "Failure on process AA collection");
            return;
        }

        pending();
        touchWebSelect();
        setPending();

        pending();
        successStatus = aliInputQRCode(amountMsg, "1",traceIdMsg);
        if (!successStatus) {
            Log.e(TAG, "Failure on process InputQRCode");
            return;
        }
        setPending();
//        The information including batchno and token can get in last step. aliShowQRPage don't need to called
//        pending();
//        aliShowQRPage();

//        starting rollback to
        pending();

        successStatus = closeDetailActivity();
        if (!successStatus) {
            Log.e(TAG, "Failure on process close detail Activity");
        }
        setPending();

    }

    private void runAlipayBalance(Context context) {
        // check balance is avaialbe or not
        boolean successStatus = true;

        setPending();
        //Check forewindow is alipay or not
        if (!checkIsAlipay()) {
            Log.d(TAG, "Try to start Alipay App");
            startApp(context, "com.eg.android.AlipayGphone", "com.eg.android.AlipayGphone.AlipayLogin");
            pending();
        }

        successStatus = findAndPerformClick("账单", 1);
        if (!successStatus) {
            Log.e(TAG, "Failure on process Balance click");
            return;
        }

        pending();
        successStatus = findAndPerformClick("筛选",1);
        if (!successStatus) {
            Log.e(TAG, "Failure to click on Filter button");
            return;
        }

        pending();
        successStatus = backFromFilter();
        if (!successStatus) {
            Log.e(TAG, "Failure on process back from filter window");
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



    private boolean closeDetailActivity(){
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
        return findAndPerformClick("完成",0);

    }
    private void touchWebSelect() {
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();

        Rect rect = new Rect();
        rootInActiveWindow.getBoundsInScreen(rect);

        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        int x = width / 2;
        int y = (height / 3) + 50;
        Log.d(TAG, x + " : " + y);
        tapOnPosition(x, y);
    }

    private boolean aliInputQRCode(String amountMsg, String number, String traceIdMsg ){
        HashMap<String, String> aliMap = new HashMap<>();
        aliMap.put("最多10,000元", amountMsg);
        aliMap.put("必填", number);
        aliMap.put("小伙伴们速速交钱～", traceIdMsg);

        AccessibilityNodeInfo qrRootNode = robotService.getRootInActiveWindow();

        recursion(qrRootNode, aliMap);

        List<AccessibilityNodeInfo> BtnNodeList = qrRootNode.findAccessibilityNodeInfosByText("确定");
        if (BtnNodeList ==null || BtnNodeList.size() != 1) {
            Log.e(TAG, "find the input node is null or size not equal one");
            return false;
        }

        AccessibilityNodeInfo btnNode = BtnNodeList.get(0);
        btnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        btnNode.recycle();
        return true;
    }

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

    public boolean backFromFilter() {

        AccessibilityNodeInfo filterWindow = robotService.getRootInActiveWindow();
        return findFirstByClass(filterWindow, "ImageButton");
    }


    public boolean findFirstByClass(AccessibilityNodeInfo nodeInfo, final String className) {
        boolean result = false;
        if (nodeInfo.getChildCount() == 0) {
            Log.d(TAG, nodeInfo.getClassName().toString());
            if (nodeInfo.getClassName().toString().contains(className)) {
                result = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return result;
            }
        } else {
            for (int i=0 ; i < nodeInfo.getChildCount() ; i++) {
                if (nodeInfo.getChild(i) != null) {
                    result =  findFirstByClass(nodeInfo.getChild(i), className);
                    if (result) return result;
                }
            }
        }
        return result;
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


    private void tapOnPosition(int x, int y) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            dos.writeBytes("input tap " + x + " " + y + "\n");
            dos.flush();
            dos.close();
            process.waitFor();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            Log.d(TAG,e.toString());
        }
    }


    private AccessibilityNodeInfo ancestor(AccessibilityNodeInfo nodeInfo) {
        return nodeInfo.getParent();
    }

    private boolean findAndPerformClick(final String text, final int level) {
        AccessibilityNodeInfo rootInActiveWindow = robotService.getRootInActiveWindow();
        List<AccessibilityNodeInfo> rootListNode = rootInActiveWindow.findAccessibilityNodeInfosByText(text);

        Log.d(TAG, "size=" + rootListNode.size());

        if (rootListNode == null || rootListNode.size() != 1) {
            Log.e(TAG, "No Node or moe than one node match the text");
            return false;
        }
//        AccessibilityNodeInfo ancestorNode = ancestor(rootListNode.get(0));
        AccessibilityNodeInfo ancestorNode = rootListNode.get(0);

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
        return result;
    }


    private Transaction pushAction(Context context, Intent intent) {

        String payTypeMsg = null;
        String traceIdMsg = null;
        String amountMsg = null;
        Bundle bundle = intent.getExtras();
        Log.d(TAG, "GetuiReceive onReceive");
        Transaction transaction = null;

        if (bundle.getInt(PushConsts.CMD_ACTION) == PushConsts.GET_MSG_DATA ) {
            byte[] payload = bundle.getByteArray("payload");
            if (payload != null)
            {
                String playLoadString = new String(payload);
                Log.d(TAG, "Run TriggerPay value= " + playLoadString);


                try {
                    JSONObject parseObj = new JSONObject(playLoadString);
                    payTypeMsg = parseObj.getString(PAYMENT_TYPE);
                    traceIdMsg = parseObj.getString(TRACE_ID);
                    amountMsg = parseObj.getString(AMOUNT);

                } catch (JSONException e) {
                    Log.e(TAG, "ERR: failure to parse push JSON");
                    return null;
                }

                if (payTypeMsg == null || payTypeMsg.length() < 1
                        || traceIdMsg == null || traceIdMsg.length() < 1
                        || amountMsg == null || amountMsg.length() < 1) {
                    Log.e(TAG, "ERR: JSON attribute is null or empty");
                    return null;
                }

                transaction = new Transaction(payTypeMsg, traceIdMsg, Constants.PAYMENT_STATUS.PUSH.ordinal(), System.currentTimeMillis() / 1000L);

                dataBaseManager.add(transaction);

                switch (payTypeMsg) {
                    case SMART_DIALER:
                        runSmartDialer(context, traceIdMsg, amountMsg);
                        break;
                    case ALIPAY_TYPE:
                        runAlipayQR(context, traceIdMsg, amountMsg);
                        break;
                    case WECHAT_TYPE:
//                        dispatchIntent(context, PUSH_PAY_ACTION + ".WECHAT.GETQR", traceIdMsg, amountMsg, optionMsg);
                }
            }
        }

        return transaction;

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




    private void postQRAction(Context context, Intent intent,Transaction tranItem){
        // get variable from alipay hook or webchat hooker
        // composite json and sent to web app by okhttp

        Bundle bundle = intent.getExtras();
        SerializableMap qrinfo = (SerializableMap) bundle.getSerializable("qrinfo");
        Map<String, String> qrInfoMap = qrinfo.getMap();
        Log.d(TAG,"qrInfoMap=" + qrInfoMap.toString());

        tranItem.setBatch_no(qrInfoMap.get("batch_no"));
        tranItem.setToken(qrInfoMap.get("token"));
        tranItem.setStatus(Constants.PAYMENT_STATUS.WAITING_PAY.ordinal());
        // save batch_no, token, track_no to sqllite DB
        dataBaseManager.updateByTrackNo(tranItem);
        Log.d(TAG, "Finished to save Post QR Information");

        String postJsonStr = buildJsonFromMap(qrInfoMap);

        SharedPreferences appSharePrefrences = PreferenceManager.getDefaultSharedPreferences(robotService);
        String sendQRurl = appSharePrefrences.getString("sendQRurl", "https://bitstunnel.com/api/mobipay/applyQR");
        String authToketn = appSharePrefrences.getString("authToken", "");

        try {
            okhttpAction(sendQRurl,authToketn,postJsonStr);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void postPaidActoin(Context context, Intent intent){
        //get varialbe from alipay hook or webchat hook
        // compostie json and sent to web app by okhttp
    }


    public String buildJsonFromMap(Map<String,String> inputMap) {
        JSONObject jsonObject = new JSONObject(inputMap);
        return jsonObject.toString();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case Constants.PUSH_ACTION:
                Log.d(TAG, "PushAction starting");
                tranItem = pushAction(context,intent);
                break;
            case Constants.POST_QR_ACTION:
                Log.d(TAG, "PostQRAction starting");
                postQRAction(context, intent, tranItem);
                tranItem = null;
                break;
            case Constants.POST_PAID_ACTION:
                Log.d(TAG, "Post Paid Action");
                runAlipayBalance(context);
                break;
        }
    }
}

//    private void dispatchIntent(Context context, String intentAction, String trace_id, String amount, String option) {
//        Intent intent = (new Intent()).setAction(intentAction);
//        intent.putExtra(TRACE_ID, trace_id);
//        intent.putExtra(AMOUNT, amount);
//        intent.putExtra(OPTION, option);
//        context.sendBroadcast(intent);
//    }
//    private void enableScreenOn(Context context) {
//        PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
//        if (powerManager.isScreenOn()) { return;}

//        KeyguardManager keyguardManager = (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(powerManager.SCREEN_DIM_WAKE_LOCK, TAG);
//        wakeLock.acquire();

//        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("");
//        keyguardLock.disableKeyguard();

//    }




