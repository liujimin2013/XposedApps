package com.bitstunnel.xposedpush;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.bitstunnel.xposedpush.domain.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * Created by Mai on 2016/9/12.
 */
public class DataBaseManager {
    public static final String TAG = "XposedPush";

    private static final String TABLE_TRANS = "transactions";

    private static final String TRANS_COLUMN_PAYMENTTYPE = "paymentType";
    private static final String TRANS_COLUMN_TRACKNO = "track_no";
    private static final String TRANS_COLUMN_AMOUNT = "amount";
    private static final String TRANS_COLUMN_QRURL = "qr_string";
    private static final String TRANS_COLUMN_UPDATETIME = "update_time";
    private static final String TRANS_COLUMN_STATUS = "status";

    private SqlDBHelper sqlDBHelper;
    private SQLiteDatabase db;

    public DataBaseManager(Context context) {
        sqlDBHelper = SqlDBHelper.getInstance(context);
        db = sqlDBHelper.getWritableDatabase();
        Log.d(TAG, "complete init SqlDBHelper and DB");
    }

    public void add(final String paymentType, final String trackNo, final String amount, final String qrUrl, final long updateTime, final Integer status) {
        ContentValues values = new ContentValues();
        values.put(TRANS_COLUMN_PAYMENTTYPE, paymentType);
        values.put(TRANS_COLUMN_TRACKNO, trackNo);
        values.put(TRANS_COLUMN_AMOUNT, amount);
        values.put(TRANS_COLUMN_QRURL, qrUrl);
        values.put(TRANS_COLUMN_UPDATETIME, updateTime);
        values.put(TRANS_COLUMN_STATUS, status);

        long rowId = db.insert("transactions", null, values);
    }

    private Cursor queryCursorWaitingPay(final Integer status) {
        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE status = ?", new String[]{String.valueOf(status)});
        return cursor;
    }

    public List<Transaction> queryWaitingPay(final Integer status) {

        ArrayList<Transaction> paidTrackNoList = new ArrayList<>();
        Cursor cursor = queryCursorWaitingPay(status);
        while (cursor.moveToNext()) {
            Transaction transaction = new Transaction();
            transaction.setTrack_no(cursor.getString(cursor.getColumnIndex("track_no")));
            transaction.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            paidTrackNoList.add(transaction);
        }
            return paidTrackNoList;
    }

    public List<Transaction> queryAll() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM transactions", null);
        while (cursor.moveToNext()) {
            Transaction transaction = new Transaction();

            transaction.setPaymentType(cursor.getString(cursor.getColumnIndex("paymentType")));
            transaction.setQr_string(cursor.getString(cursor.getColumnIndex("qr_string")));
            transaction.setTrack_no(cursor.getString(cursor.getColumnIndex("track_no")));
            transaction.setAmount(cursor.getString(cursor.getColumnIndex("amount")));
            transaction.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
            transaction.setUpdateTime(cursor.getInt(cursor.getColumnIndex("update_time")));
            transactions.add(transaction);
        }
            return transactions;
    }


    public Integer updateByTrackNo(final String paymentType, final String trackNo, final Integer status) {
        ContentValues cv = new ContentValues();
        cv.put("status", status);

        int count = db.update(TABLE_TRANS, cv, "paymentType = ? AND track_no = ?", new String[]{paymentType, trackNo});
        return count;
    }

    public void delRecordsByTime(Integer before) {
        db.delete("transactions", "update_time <= " + before, null);
    }

    public void closeDB() {
        db.close();
    }



//    public void add(Transaction transaction) {
//
//        db.beginTransaction();
//        try {
//            db.execSQL("INSERT INTO transactions(paymentType, track_no, qr_string, amount, status, update_time) VALUES (?, ? , ?, ?, ?, ?)",
//                    new Object[]{transaction.getPaymentType(), transaction.getTrack_no(), transaction.getQr_string(), transaction.getAmount(), transaction.getStatus(), transaction.getUpdateTime()});
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//
//    }
//

//    @Deprecated
//    public void addList(List<Transaction> transactions) {
//
//        db.beginTransaction();
//
//        try {
//            for (Transaction transaction : transactions) {
//                db.execSQL("INSERT INTO transactions(paymentType, track_no, qr_string, amount, status, update_time) VALUES (?, ? , ?, ?, ?, ?)",
//                        new Object[]{transaction.getPaymentType(), transaction.getTrack_no(), transaction.getQr_string(), transaction.getAmount(), transaction.getStatus(), transaction.getUpdateTime()});
//                db.setTransactionSuccessful();
//            }
//        } finally {
//            db.endTransaction();
//        }
//
//
//    }
//
//    @Deprecated
//    public void updateByTrackNo(Transaction transaction){
//        ContentValues cv = new ContentValues();
//        cv.put("qr_string", transaction.getQr_string());
//        cv.put("status", transaction.getStatus());
//        cv.put("update_time", transaction.getUpdateTime());
//        cv.put("amount", transaction.getAmount());
//        db.update("transactions", cv, "track_no= ?", new String[] {transaction.getTrack_no()});
//    }
//
//
//
//    @Deprecated
//    public List<Transaction> queryWaitingPay(final String status) {
//        ArrayList<Transaction> transactions = new ArrayList<>();
//        Cursor cursor = queryCursorWaitingPay(status);
//        while (cursor.moveToNext()) {
//            Transaction transaction = new Transaction();
//
//            transaction.setPaymentType(cursor.getString(cursor.getColumnIndex("paymentType")));
//            transaction.setQr_string(cursor.getString(cursor.getColumnIndex("qr_string")));
//            transaction.setTrack_no(cursor.getString(cursor.getColumnIndex("track_no")));
//            transaction.setStatus(cursor.getInt(cursor.getColumnIndex("status")));
//            transaction.setUpdateTime(cursor.getInt(cursor.getColumnIndex("update_time")));
//            transactions.add(transaction);
//        }
//            return transactions;
//    }
//



}
