package com.company;

public class Tuple{
    long time;
    boolean result;
    Tuple setTime(long time){
        this.time = time;
        return this;
    }
    Tuple setResult (boolean result){
        this.result = result;
        return this;
    }
}