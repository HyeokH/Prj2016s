package com.example.prj2016s.RtspPlayer;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.VideoView;
import android.widget.MediaController;

import com.example.prj2016s.R;



/**
 * Created by 성혁화 on 2016-06-06.
 */
public class Example_RtspPlayer extends Activity {
    private static final String TAG = "Example_RtspPlayer";

    private RtspPlayerService mMmsPlayerService;
    private VideoView mVideoView;
    private MediaController mMediaController;
    boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_rtsp_player);

        Log.d(TAG, "doBindService()");

        mVideoView = (VideoView)findViewById(R.id.videoView);

        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);

        mVideoView.setVideoPath("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        //mVideoView.setVideoPath("rtsp://127.0.0.1/rtp_test1");
        //mVideoView.setVideoPath("rtsp://ebsonairandaod.ebs.co.kr/fmradiobandiaod/bandiappaac");
        mVideoView.requestFocus();
        mVideoView.start();

        //doBindService();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mMmsPlayerService != null){
            Log.d(TAG, "mMmsPlayerService.start()");
            mMmsPlayerService.start();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        Log.d(TAG, "doUnbindService()");
        //doUnbindService();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            RtspPlayerService.LocalBinder binder = (RtspPlayerService.LocalBinder) service;
            mMmsPlayerService = binder.getService();
            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mIsBound = false;
        }
    };

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Intent intent = new Intent(this, RtspPlayerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
}
