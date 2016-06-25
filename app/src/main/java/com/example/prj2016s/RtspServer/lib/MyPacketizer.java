package com.example.prj2016s.RtspServer.lib;

import android.util.Log;

import com.example.prj2016s.RtspServer.lib_spy.AbstractPacketizer;
import com.example.prj2016s.RtspServer.lib_spy.RtpSocket;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Created by 성혁화 on 2016-06-24.
 */
public class MyPacketizer implements Runnable {

    public final static String TAG = "MyPacketizer";

    protected static final int rtphl = RtpSocket.RTP_HEADER_LENGTH;
    private int nal_length = 0;
    protected InputStream is = null;
    private Thread t = null;
    protected byte[] buffer;
    protected RtpSocket socket = null;
    protected long ts = 0;
    private long delay = 0, oldtime = 0;
    private AbstractPacketizer.Statistics stats = new AbstractPacketizer.Statistics();

    public void setInputStream(InputStream is) {
        this.is = is;
    }

    public MyPacketizer() throws IOException {
        socket = new RtpSocket();
        ts = new Random().nextInt();
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    public void stop() {
        if (t != null) {
            try {
                is.close();
            } catch (IOException e) {}
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {}
            t = null;
        }
    }

    @Override
    public void run() {
        Log.d(TAG,"H264 packetizer started !");
        stats.reset();

        byte[] header = new byte[5];
        long duration = 0;
        boolean sw = false;
        try {
            fill(header,0,5);
        } catch (IOException e) {
            Log.e(TAG, "Exception : " + e.getMessage());
        }
        try {
            while (!Thread.interrupted()) {
                //Log.d(TAG, String.valueOf(header[0]) + String.valueOf(header[1]) + String.valueOf(header[2]) + String.valueOf(header[3]) + String.valueOf(header[4]));
                if ((int)(header[0]&0xFF) == 0 && (int)(header[1]&0xFF) == 0 && (int)(header[2]&0xFF) == 0 && (int)(header[3]&0xFF) == 1) {
                    Log.d(TAG, "length : " + String.valueOf(nal_length));
                    Log.d(TAG, String.valueOf(header[4]));
                    if (sw) {
                        ts += delay;
                        socket.updateTimestamp(ts);
                        socket.markNextPacket();
                        socket.commitBuffer(nal_length+rtphl);

                        duration = System.nanoTime() - oldtime;
                        stats.push(duration);
                        delay = stats.average();
                    }
                    sw = true;
                    nal_length = 0;
                    buffer = socket.requestBuffer();
                    oldtime = System.nanoTime();
                }
                if (sw && nal_length >= 4) {
                    buffer[rtphl+(nal_length-4)] = header[0];
                }
                header[0] = header[1];
                header[1] = header[2];
                header[2] = header[3];
                header[3] = header[4];
                header[4] = (byte) is.read();
                nal_length++;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Exception : " + e.getMessage());
        }
        Log.d(TAG,"H264 packetizer stopped !");
    }


    private int fill(byte[] buffer, int offset,int length) throws IOException {
        int sum = 0, len;
        while (sum<length) {
            len = is.read(buffer, offset+sum, length-sum);
            if (len<0) {
                throw new IOException("End of stream");
            }
            else sum+=len;
        }
        return sum;
    }
}
