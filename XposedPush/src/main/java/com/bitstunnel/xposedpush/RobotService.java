package com.bitstunnel.xposedpush;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import com.bitstunnel.xposedpush.utils.Constants;

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
        Log.d(TAG, "start setting DataBaseManager");
        getuiReceiver.setDbManager(new DataBaseManager(this));

        Log.d(TAG, "RobotService onCreated AND set DBManager");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.PUSH_ACTION);
        filter.addAction(Constants.NOTIFY_QRURL);
        filter.addAction("com.bitstunnel.xposedPush.action.typeInput");
        filter.addAction(Constants.AFTER_PAID_ACTION);
        filter.addAction("com.bitstunnel.alipay.action.postPaidList");

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

        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            List<CharSequence> textList = accessibilityEvent.getText();
            if (textList != null && textList.size() > 0) {

                if (textList.get(0).toString().contains("通过扫码向你付款")) {
                    // alipay collection
                    Intent intent = new Intent(Constants.AFTER_PAID_ACTION);
                    // 暂时不考虑webchat 情况.

                    sendBroadcast(intent);
                    Log.d(TAG, "Successfully collect money event and broadcast");
                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

}
