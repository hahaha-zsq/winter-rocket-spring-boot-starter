package com.zsq.winter.rocketmq.entity;

public interface DelayConstant {

    //1==1s、2==5s、3==10s、4==30s、5==1m、6==2m、7==3m、8==4m、9==5m
    //10 ==》6m、11 ==》7m、12 ==》8m、13 ==》9m、14 ==》10m、15 ==》20m、16 ==》30m、17 ==》1h、18 ==》2h
    int ONE_SECOND = 1;
    int FIVE_SECOND = 2;
    int TEN_SECOND=3;
    int THIRTY_SECOND=4;
    int ONE_MINUTES=5;
    int TWO_MINUTES=6;
    int THIRD_MINUTES=7;
    int FOUR_MINUTES=8;
    int FIVE_MINUTES=9;
    int SIX_MINUTES=10;
    int SEVEN_MINUTES=11;
    int EIGHT_MINUTES=12;
    int NINE_MINUTES=13;
    int TEN_MINUTES=14;
    int TWENTY_MINUTES=15;
    int THIRTY_MINUTES=16;
    int ONE_HOURS=17;
    int TWO_HOURS=18;
}
