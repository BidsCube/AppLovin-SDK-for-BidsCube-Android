package com.bidscube.sdk.models.enums;

public enum NativeDataType {
    SPONSORED(1), DESC(2), RATING(3), LIKES(4), DOWNLOADS(5), PRICE(6),
    SALEPRICE(7), PHONE(8), ADDRESS(9), DESC2(10), DISPLAYURL(11), CTATEXT(12);
    public final int v; NativeDataType(int v){this.v=v;}
    public static NativeDataType from(int v){ for (NativeDataType t:values()) if(t.v==v) return t; return null; }
}