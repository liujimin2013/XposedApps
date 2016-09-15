package com.bitstunnel.xposedpush;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.bitstunnel.xposedpush.domain.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * Created by Mai on 2016/9/12.
 */
public class DataBaseManager {

    private SqlDBHelper sqlDBHelper;
    private SQLiteDatabase db;

    public DataBaseManager(Context context) {
        sqlDBHelper = SqlDBHelper.getInstance(context);
        db = sqlDBHelper.getWritableDatabase();
    }

    public void add(Transaction transaction) {

        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO transaction(batch_no, token, track_no, status, update_time) VALUES ( ?, ? , ?, ?, ?",
                    new Object[]{transaction.getBatch_no(), transaction.getToken(), transaction.getTrack_no(), transaction.getStatus(), transaction.getUpdateTime()});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

    }

    public void addList(List<Transaction> transactions) {

        db.beginTransaction();

        try {
            for (Transaction transaction : transactions) {
                db.execSQL("INSERT INTO transaction(batch_no, token, track_no, status, update_time) VALUES ( ?, ? , ?, ?, ?",
                        new Object[]{transaction.getBatch_no(), transaction.getToken(), transaction.getTrack_no(), transaction.getStatus(), transaction.getUpdateTime()});
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }


    }

    public void updateByTrackNo(Transaction transaction){
        ContentValues cv = new ContentValues();
        cv.put("batch_no", transaction.getBatch_no());
        cv.put("token", transaction.getToken());
        cv.put("status", transaction.getStatus());
        cv.put("update_time", transaction.getUpdateTime());

        db.update("transactions", cv, "track_no= ?", new String[] {transaction.getTrack_no()});
    }

    public void updateByBatchNo(Transaction transaction) {
        ContentValues cv = new ContentValues();
        cv.put("status", transaction.getStatus());
        cv.put("update_time", transaction.getUpdateTime());
        db.update("transactions", cv, "batch_no= ?", new String[]{transaction.getBatch_no()});
    }

    public List<Transaction> queryWaitingPay(final String status) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        Cursor cursor = queryCursorWaitingPay(status);
        while (cursor.moveToNext()) {
            Transaction transaction = new Transaction();
            transaction.setBatch_no(cursor.getString(cursor.getColumnIndex("batch_no")));
            transaction.setToken(cursor.getString(cursor.getColumnIndex("token")));
            transaction.setTrack_no(cursor.getString(cursor.getColumnIndex("track_no")));
            transaction.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            transaction.setUpdateTime(cursor.getInt(cursor.getColumnIndex("update_time")));
            transactions.add(transaction);
        }
            return transactions;
    }

    private Cursor queryCursorWaitingPay(final String status) {
        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE status = ?", new String[]{status});
        return cursor;
    }

    public void delRecordsByTime(Integer before) {
        db.delete("transactions", "update_time <= " + before, null);
    }

    public void closeDB() {
        db.close();
    }





}
