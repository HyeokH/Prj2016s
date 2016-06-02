package com.example.prj2016s.Manager;

/**
 * Created by Kang on 2016-06-02.
 */
//time component(start location of TsFile, end location, file location in server)
public class TsFile {
    private final float timeStart;
    private final float timeEnd;
    private final String loc;

    public TsFile(float ts, float te, String l) {
        timeStart = ts;
        timeEnd = te;
        loc = l;
    }

    public float getTimeStart() {
        return timeStart;
    }

    public float getTimeEnd() {
        return timeEnd;
    }

    public String getLoc() {
        return loc;
    }
}