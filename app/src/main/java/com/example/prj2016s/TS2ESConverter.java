//package org.jcodec.samples.transcode;


package com.example.prj2016s;

import com.example.prj2016s.Packet.MTSPacket;
import com.example.prj2016s.etc.SeekableByteChannel;

import org.jcodec.containers.mps.MPSDemuxer;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;

public class TS2ESConverter {
    private MPSDemuxer psDemuxer;
    private SeekableByteChannel tsChannel;
    private SeekableByteChannel src;
    private ByteBuffer data;
//    private int filterGuid;
    
    protected MTSPacket getPacket(ReadableByteChannel channel) throws IOException {
        MTSPacket pkt;

        pkt = readPacket(channel);
        /*do {
            pkt = readPacket(channel);
            if (pkt == null)
                return null;
        } while (pkt.pid <= 0xf || pkt.pid == 0x1fff || pkt.payload == null);

        while (pkt.pid != filterGuid) {
            pkt = readPacket(channel);
            if (pkt == null)
                return null;
        }*/

        return pkt;
    }
    
    public TS2ESConverter(final SeekableByteChannel nSrc) throws IOException {
        src = nSrc;
    }
    
    public int read(ByteBuffer dst) throws IOException {
        while (data == null || !data.hasRemaining()) {
            MTSPacket packet = getPacket(src);
            if (packet == null)
                return -1;
            data = packet.payload;
        }
        int toRead = Math.min(dst.remaining(), data.remaining());
        dst.put(NIOUtils.read(data, toRead));
        return toRead;
    }
    
    public static MTSPacket readPacket(ReadableByteChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(188);
        if (readFromChannel(channel, buffer) != 188)
            return null;
        buffer.flip();
        return parsePacket(buffer);
    }
    
    public static MTSPacket parsePacket(ByteBuffer buffer) {

        int marker = buffer.get() & 0xff;
        assertEquals(0x47, marker);
        int guidFlags = buffer.getShort();
        int guid = (int) guidFlags & 0x1fff;
        int payloadStart = (guidFlags >> 14) & 0x1;
        int b0 = buffer.get() & 0xff;
        int counter = b0 & 0xf;
        if ((b0 & 0x20) != 0) {
            int taken = 0;
            taken = (buffer.get() & 0xff) + 1;
            skip(buffer, taken - 1);
        }
        return new MTSPacket(guid, payloadStart == 1, ((b0 & 0x10) != 0) ? buffer : null);
    }
    
    public static int readFromChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        int rem = buffer.position();
        while (channel.read(buffer) != -1 && buffer.hasRemaining())
            ;
        return buffer.position() - rem;
    }
    
    public static int skip(ByteBuffer buffer, int count) {
        int toSkip = Math.min(buffer.remaining(), count);
        buffer.position(buffer.position() + toSkip);
        return toSkip;
    }
    
    public static void assertEquals(int i, int j) {
        if (i != j)
            throw new AssertionError();
    }
  
}