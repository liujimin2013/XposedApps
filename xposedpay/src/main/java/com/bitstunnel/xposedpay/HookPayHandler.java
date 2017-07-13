package com.bitstunnel.xposedpay;

import com.bitstunnel.xposedpay.alipay.AlipayHook;
import com.bitstunnel.xposedpay.utils.Constants;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 *
 * Created by Mai on 2016/7/28.
 */
public class HookPayHandler implements IXposedHookLoadPackage {

    AlipayHook alipayHook = new AlipayHook();

    @Override
         public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

//        XposedBridge.log("Loaded App: " + lpparam.packageName);

        if (lpparam.packageName.equalsIgnoreCase(Constants.ALIPAY_PACKAGENAME)) {

            alipayHook.hook(lpparam.classLoader);
//            new AlipayHook().hook(lpparam.classLoader);

        }
    }
}
