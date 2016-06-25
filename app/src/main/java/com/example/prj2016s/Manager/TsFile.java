package com.example.prj2016s.Manager;

/**
 * Created by Kang on 2016-06-02.
 */
//time component(start location of TsFile, end location, file location in server)
public class TsFile {
    private final long timeStart;
    private final long timeEnd;
    private final String name;
    private final String loc;

    public TsFile(long ts, long te, String l, String n) {
        timeStart = ts;
        timeEnd = te;
        loc = l;
        name = n;
    }

    public long getTimeStart() {
        return timeStart;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public String getLoc() {
        return loc;
    }
    public String getName() {
        return name;
    }
}