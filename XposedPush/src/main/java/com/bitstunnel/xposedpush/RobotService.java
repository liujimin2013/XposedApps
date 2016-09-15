package com.bitstunnel.xposedpush;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 *
 *
 * Created by Mai on 2016/8/10.
 */
public class RobotService extends AccessibilityService{

    public static final String TAG = "XposedPush";

    private GetuiReceiver getuiReceiver;



    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        getuiReceiver = new GetuiReceiver();
        getuiReceiver.setService(this);
        getuiReceiver.setDbManager(new DataBaseManager(this));
        Log.d(TAG, "RobotService onCreated AND set DBManager");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.PUSH_ACTION);
        filter.addAction(Constants.POST_QR_ACTION);
        filter.addAction(Constants.POST_PAID_ACTION);

        registerReceiver(getuiReceiver, filter);

    }

    @Override
    public void onCreate(){
        super.onCreate();
        PowerManager powerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(powerManager.SCREEN_DIM_WAKE_LOCK, TAG);
        wakeLock.acquire();

    }

    @Override
    public void onDestroy(){
        unregisterReceiver(getuiReceiver);
        super.onDestroy();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "TYPE_WINDOWS_STATE_CHANGE");
            getuiReceiver.changeHold();
            return;
        }

        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> textList = accessibilityEvent.getText();
            if (textList != null && textList.size() > 0) {

                if (textList.get(0).toString().contains("你发起的活动收款") && textList.get(0).toString().contains("钱收齐啦")) {
                    // alipay collection
                    Intent intent = new Intent(Constants.POST_PAID_ACTION);
                    // 暂时不考虑webchat 情况.

                    sendBroadcast(intent);
                    Log.d(TAG, "Successfully collect money event and broadcast");
                }
//                for (int i = 0; i < text.size() ; i++) {
//                    Log.d(TAG, "Notification Text=" + text.get(i).toString());
//                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

}
