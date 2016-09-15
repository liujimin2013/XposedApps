package com.bitstunnel.xposedpay.utils;

import java.io.Serializable;
import java.util.Map;

/**
 *
 *
 * Created by Mai on 2016/9/10.
 */
public class SerializableMap implements Serializable{

    private Map<String, String> map;

    public Map<String,String> getMap(){
        return map;
    }

    public void setMap(Map<String,String> map) {
        this.map = map;
    }
}
