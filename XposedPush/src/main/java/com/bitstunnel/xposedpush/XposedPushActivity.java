package com.bitstunnel.xposedpush;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.bitstunnel.xposedpush.domain.Transaction;
import com.igexin.sdk.PushManager;

import java.util.List;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;


public class XposedPushActivity extends Activity implements OnClickListener{

    public static final String TAG = "XposedPush";
    protected Button displayDbBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_push);
        PushManager.getInstance().initialize(this.getApplicationContext());

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PreFragment preFragment = new PreFragment();
        fragmentTransaction.replace(R.id.xpreference, preFragment);
        fragmentTransaction.commit();

        displayDbBtn = (Button)findViewById(R.id.btn_list_db);
        displayDbBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_list_db:
                Log.i(TAG, "DislplayDB onClick");
                DataBaseManager dataBaseManager = new DataBaseManager(this);

                List<Transaction> transactions = dataBaseManager.queryAll();
                for (Transaction transaction : transactions) {
                    Log.i(TAG, "Display All transaction=" + transaction.getTrack_no() + ":" + transaction.getAmount() + ":" + transaction.getQr_string() +":" + transaction.getStatus());
                }
                break;
        }
    }
}

