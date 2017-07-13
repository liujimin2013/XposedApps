package com.bitstunnel.xposedpush.domain;

/**
 *
 * Created by Mai on 2016/9/12.
 */
public class Transaction {

    private String paymentType;
    private String track_no;
//    private String batch_no;
//    private String token;
    private String qr_string;
    private String amount;
    private Integer status;
    private long updateTime;

    public Transaction() {}

    public Transaction(final String paymentType, final String track_no, final Integer status, final long updateTime) {
        this.paymentType = paymentType;
        this.track_no = track_no;
        this.status = status;
        this.updateTime = updateTime;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getTrack_no() {
        return track_no;
    }

    public void setTrack_no(String track_no) {
        this.track_no = track_no;
    }

    public String getQr_string() {
        return qr_string;
    }

    public void setQr_string(String qr_string) {
        this.qr_string = qr_string;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    //    public String getBatch_no() {
//        return batch_no;
//    }
//
//    public void setBatch_no(String batch_no) {
//        this.batch_no = batch_no;
//    }
//
//    public String getToken() {
//        return token;
//    }
//
//    public void setToken(String token) {
//        this.token = token;
//    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
