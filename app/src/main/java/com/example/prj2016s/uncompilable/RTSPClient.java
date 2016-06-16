package com.example.prj2016s.uncompilable;
/**
 * Created by 성혁화 on 2016-06-03.
 */
/*

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import net.majorkernelpanic.streaming.Session;


public class RTSPClient {

    public final static String TAG = "RtspClient";

    private final static int STATE_STARTED = 0x00;
    private final static int STATE_STARTING = 0x01;
    private final static int STATE_STOPPING = 0x02;
    private final static int STATE_STOPPED = 0x03;
    private int mState = 0;
    private Handler mHandler;

    private class Parameters {
        public String host;
        public String path;
        public Session session;
        public int port;
        public int transport;

        public Parameters clone() {
            Parameters params = new Parameters();
            params.host = host;
            params.path = path;
            params.session = session;
            params.port = port;
            params.transport = transport;
            return params;
        }
    }

    private Parameters mTmpParameters;
    private Parameters mParameters;

    public RTSPClient() {
        mState = STATE_STOPPED;

        new HandlerThread("net.majorkernelpanic.streaming.RtspClient"){
            @Override
            protected void onLooperPrepared() {
                mHandler = new Handler();
            }
        }.start();
    }


    public void setServerAddress(String host, int port) {
        mTmpParameters.port = port;
        mTmpParameters.host = host;
    }

    public void setStreamPath(String path) {
        mTmpParameters.path = path;
    }

    public boolean isStreaming() {
        return mState==STATE_STARTED|mState==STATE_STARTING;
    }


    public void startStream() {
        if (mTmpParameters.host == null)
            throw new IllegalStateException("setServerAddress(String,int) has not been called !");
        if (mTmpParameters.session == null)
            throw new IllegalStateException("setSession() has not been called !");
        mHandler.post(new Runnable () {
            @Override
            public void run() {
                if (mState != STATE_STOPPED) return;
                mState = STATE_STARTING;

                Log.d(TAG,"Connecting to RTSP server...");

                mParameters = mTmpParameters.clone();
                mParameters.session.setDestination(mTmpParameters.host);

                try {
                    mParameters.session.syncConfigure();
                } catch (Exception e) {
                    mParameters.session = null;
                    mState = STATE_STOPPED;
                    return;
                }

                try {
                    tryConnection();
                } catch (Exception e) {
                    postError(ERROR_CONNECTION_FAILED, e);
                    abort();
                    return;
                }

                try {
                    mParameters.session.syncStart();
                    mState = STATE_STARTED;
                    if (mParameters.transport == TRANSPORT_UDP) {
                        mHandler.post(mConnectionMonitor);
                    }
                } catch (Exception e) {
                    abort();
                }

            }
        });
    }
}
*/