package com.example.prj2016s;
import java.nio.ByteBuffer;

public class MTSPacket {
	public ByteBuffer payload;
    public boolean payloadStart;
    public int pid;

    public MTSPacket(int guid, boolean payloadStart, ByteBuffer payload) {
        this.pid = guid;
        this.payloadStart = payloadStart;
        this.payload = payload;
    }
}