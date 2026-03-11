package com.bidscube.sdk.models.enums;

public enum ImageType { ICON(1), MAIN(3); public final int v; ImageType(int v){this.v=v;}
    public static ImageType from(int v){ for (ImageType t:values()) if(t.v==v) return t; return null; }
}