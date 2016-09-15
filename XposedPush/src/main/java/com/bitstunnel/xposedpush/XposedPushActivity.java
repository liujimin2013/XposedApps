package com.bitstunnel.xposedpush;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import com.igexin.sdk.PushManager;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;


public class XposedPushActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    public static final String TAG = "XposedPush";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_xposed_push);
        PushManager.getInstance().initialize(this.getApplicationContext());
        addPreferencesFromResource(R.xml.preferencescreen);

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        findPreference("sendQRurl").setSummary(defaultSharedPreferences.getString("sendQRurl","Enter for QRCode URL"));
        findPreference("sendPaidurl").setSummary(defaultSharedPreferences.getString("sendPaidurl","Enter for Paid info"));
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, sharedPreferences.getAll().toString() + ":" + key);
        findPreference(key).setSummary(sharedPreferences.getString(key,"Empty"));
    }
}





//        try {
//            JSONObject parseObj = new JSONObject(jsonInput);
//            trace_id = parseObj.getString("trace_id");
//            amount = parseObj.getInt("amount");
//            option = parseObj.getString("option");
//
//            Log.d("XposedPush", trace_id + ":" + amount + ":" + option);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }