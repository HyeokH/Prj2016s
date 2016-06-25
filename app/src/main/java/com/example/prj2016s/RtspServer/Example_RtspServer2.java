package com.example.prj2016s.RtspServer;

/**
 * Created by Kang on 2016-06-23.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import com.example.prj2016s.R;
import com.example.prj2016s.RtspServer.lib.RtspServer;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by 성혁화 on 2016-06-06.
 */
public class Example_RtspServer2 extends Activity implements
        SurfaceHolder.Callback,
        View.OnClickListener {
    private static final String TAG = "Example_RtspServer2";
 //   private VideoView mVideoView;
    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;
    private SurfaceHolder mSurfaceHolder;
 //   private MediaController mMediaController;
    private Button mButtonVideo;
    private RtspClient mClient;
    private String ipaddress;
    //   private Session mSession;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.example_rtsp_player);

 //       mVideoView = (VideoView)findViewById(R.id.videoView);
        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mButtonVideo = (Button) findViewById(R.id.video);
        mButtonVideo.setOnClickListener(this);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
/*
        mMediaController = new MediaController(this);
        mMediaController.setAnchorView(mVideoView);
        mVideoView.setMediaController(mMediaController);
*/

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(8086));
        editor.commit();
/*
        mSession = SessionBuilder.getInstance()
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(0)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .build();
*/
    /*
            mClient = new RtspClient();
            mClient.setSession(mSession);
            mClient.setServerAddress("rtsp://10.0.2.15/", 8086);

            mSurfaceView.getHolder().addCallback(this);*/
        this.startService(new Intent(this,RtspServer.class));
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder){

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int a, int b ,int c){

    }
    @Override
    public void  surfaceDestroyed(SurfaceHolder holder) {
//        if (mMediaRecorder != null) mMediaRecorder.release();
        mMediaPlayer.release();
        mSurfaceHolder = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video:
                Log.d(TAG, "onClick() video button");
                try {

                    WifiManager wManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo info = wManager.getConnectionInfo();
                    ipaddress = Formatter.formatIpAddress(info.getIpAddress());


/*
                    mMediaPlayer = MediaPlayer.create(this, Uri.parse("rtsp://"+ipaddress+":8086"));
//                    mMediaPlayer = MediaPlayer.create(this, Uri.parse("rtsp://ebsonairandaod.ebs.co.kr/fmradiobandiaod/bandiappaac"));
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                    try {
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mMediaPlayer.start();
                    //Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sample1);

                    //Log.d(TAG, "rtsp://" + getLocalHost() + ":8086");
                    //mVideoView.setVideoURI(uri);
                    //mVideoView.setVideoPath("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
                    //mVideoView.setVideoPath("rtsp://127.0.0.1/rtp_test1");
                    //mVideoView.setVideoPath("rtsp://ebsonairandaod.ebs.co.kr/fmradiobandiaod/bandiappaac");


                    //mVideoView.setVideoPath("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");

                    //mClient.startStream();

                    //                   getLocalIpAddress();

                    //                   mVideoView.setVideoPath("rtsp://10.0.2.15:8086");
                    /*
                    mVideoView.setVideoPath("rtsp://127.0.0.1:8086");
                    //mVideoView.setVideoPath("rtsp://127.0.0.1:8086");
                    mVideoView.requestFocus();
                    mVideoView.start();
*/
                } catch (Exception e) {
                    Log.d(TAG, String.valueOf(e));
                }
                break;
        }
    }

    /*
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mSession.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mClient.stopStream();
        }*/
}

