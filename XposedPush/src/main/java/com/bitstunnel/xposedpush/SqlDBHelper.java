package com.bitstunnel.xposedpush;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 *
 * Created by Mai on 2016/9/12.
 */
public class SqlDBHelper extends SQLiteOpenHelper{

    public static final String TAG = "XposedPush";

    private static final String DATABASE_NAME = "transactions.db";
    private static final int DATABASE_VERSION = 1;

    private static SqlDBHelper instance = null;

    private static final String CREATE_TRANSACTIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS transactions ( " +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "paymentType VARCHAR," +
            "track_no VARCHAR, " +
            "qr_string VARCHAR, " +
            "amount VARCHAR, " +
            "status INTEGER, " +
            "update_time INTEGER DEFAULT CURRENT_TIMESTAMP " +
            ");";

    public SqlDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "completed init SqlDBHelper");

    }

    public synchronized static SqlDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SqlDBHelper(context);
        }
        return instance;
    }


    @Override
    public void onCreate(SQLiteDatabase sqLitedb) {
        sqLitedb.execSQL(CREATE_TRANSACTIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLitedb, int oldVersion, int newVersion) {

    }
}
