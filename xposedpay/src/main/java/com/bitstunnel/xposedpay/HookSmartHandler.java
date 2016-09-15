package com.bitstunnel.xposedpay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.EditText;
import com.bitstunnel.xposedpay.utils.Constants;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 *
 * Created by Mai on 2016/8/4.
 */
public class HookSmartHandler implements IXposedHookLoadPackage {

//    public final static String CMD_ACTION = "action";
//    public static final int GET_MSG_DATA = 10001;

    public static final String TRACE_ID = "trace_id";
    public static final String AMOUNT = "amount";
    public static final String OPTION = "option";

    final static int nameEdit_id = 2131492951;
    final static int codeEdit_id = 0x7f0c0058;

    View confirm_btn = null;
    String trace_id = null;
    String amount = null;
    String option = null;

    Class addActivityClz = null;

    class PushReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            trace_id = intent.getStringExtra(TRACE_ID);
            amount = intent.getStringExtra(AMOUNT);
            option = intent.getStringExtra(OPTION);

        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("Loaded app: " + lpparam.packageName);

        if (!lpparam.packageName.equalsIgnoreCase( Constants.SMARTDIALER_PACKAGENAME)) {
            return;
        }

        addActivityClz = XposedHelpers.findClass(Constants.SMARTDIALER_ADDCOMMAND, lpparam.classLoader);

        XposedHelpers.findAndHookMethod(Constants.SMARTDIALER_MAINACTIVITY, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("CommandBoardActivity onCreate on-hook");

                PushReceiver pushReceiver  = new PushReceiver();
                Activity activity = (Activity) param.thisObject;
                activity.registerReceiver(pushReceiver, new IntentFilter("com.igexin.sdk.action.2mfmZrRFkp93ax9VcdqUA5"));

            }
        });

        XposedHelpers.findAndHookMethod(View.class, "setOnClickListener", View.OnClickListener.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                View view = (View) param.thisObject;
                if (view.getId() == 0x7f0c005a) {
                    confirm_btn = view;
                }
            }
        });


        XposedHelpers.findAndHookMethod(EditText.class, "getText", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                EditText editor = (EditText) param.thisObject;
                if (editor.getId() == nameEdit_id) {
                    param.setResult(new SpannableStringBuilder(trace_id + amount));
                }

                if (editor.getId() == codeEdit_id) {
                    param.setResult(new SpannableStringBuilder(option));
                }
            }
        });

        XposedHelpers.findAndHookMethod(Constants.SMARTDIALER_ADDCOMMAND, lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
//                XposedBridge.log("ADD COMMAND oncreate Hook done");
                if (confirm_btn == null) {
                    XposedBridge.log("confirm_btn is null");
                }
                confirm_btn.performClick();
            }
        });

        // 应该还要考虑到 Receiver绑定到 Activty后, Activity在onResume, onStop等状态时，重绑或者解绑Receiver
//
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//                EditText nameEdit = (EditText) param.thisObject;
//                if (nameEdit.getId() == 2131492951) {
//                    XposedBridge.log("Located nameEdit and Hint: " + nameEdit.getHint());
//                    nameEdit.setHint("Xposed Name");
//                }
//            }
//        };
//
//        XposedHelpers.findAndHookConstructor(EditText.class, Context.class, edit_MethodHook );
//
//        XposedHelpers.findAndHookConstructor(EditText.class, Context.class, AttributeSet.class, edit_MethodHook);
//
//        XposedHelpers.findAndHookConstructor(EditText.class, Context.class, AttributeSet.class, int.class, edit_MethodHook);
    }
}



//Bundle bundle = intent.getExtras();
//if (bundle.getInt(CMD_ACTION) == GET_MSG_DATA ) {
//        byte[] payload = bundle.getByteArray("payload");
//        if (payload != null)
//        {
//        String playLoadString = new String(payload);
//        XposedBridge.log("Run TriggerPay value= " + playLoadString);
//
//        if (addActivityClz == null) {
//        XposedBridge.log("Add Commomd Activty class is null");
//        return;
//        }
//
//        Intent addIntent = new Intent(context, addActivityClz);
//        ((Activity) context).startActivityForResult(addIntent, 2001);
//
//        try {
//        JSONObject parseObj = new JSONObject(playLoadString);
//        nameInput = parseObj.getString("trace_id");
//        codeInput = parseObj.getString("amount");
//
//        } catch (JSONException e) {
//        XposedBridge.log("Failed to parse json string" + e.toString());
//        nameInput = "AFTER FAIL TO PARSE";
//        codeInput = "AFTER FAIL TO PARSE";
//        }
//        }
//        }