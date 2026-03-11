package com.bidscube.sdk.network;

/**
 * Response model for Bidscube ad requests
 * Contains adm content and position information
 */
public class BidscubeResponse {
    private final String adm;
    private final int position;
    
    public BidscubeResponse(String adm, int position) {
        this.adm = adm;
        this.position = position;
    }

    public String getAdm() { return adm; }
    public int getPosition() { return position; }
    
    @Override
    public String toString() {
        return "BidscubeResponse{position=" + position + ", admLength=" + (adm != null ? adm.length() : 0) + "}";
    }
}

