package com.bitstunnel.xposedpush;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 *
 * Created by Mai on 2016/9/12.
 */
public class SqlDBHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "transactions.dfb";
    private static final int DATABASE_VERSION = 0;

    private static SqlDBHelper instance = null;

    private static final String CREATE_TRANSACTIONS_TABLE =
            "CREATE TABLE IF NOT EXISTS transactions ( " +
            "id VARCHAR PRIMARY KEY, " +
            "paymentType VARCHAR," +
            "batch_no VARCHAR, " +
            "token VARCHAR, " +
            "track_no VARCHAR, " +
            "status INTEGER, " +
            "update_time INTEGER DEFAULT CURRENT_TIMESTAMP " +
            ");";

    public SqlDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
