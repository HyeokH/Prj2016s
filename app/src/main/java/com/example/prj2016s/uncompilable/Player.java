package com.example.prj2016s.uncompilable;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import com.example.prj2016s.R;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 성혁화 on 2016-06-03.
 */
public class Player extends Activity implements  OnClickListener {
    public final static String TAG = "Player";

    private SurfaceView mSurfaceView;
    private RtspClient mClient;
    private Session mSession;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.player);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.setOnClickListener(this);

        mSession = SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                .setAudioQuality(new AudioQuality(8000, 16000))
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(0)
//                .setCallback(this)
                .build();

        mClient = new RtspClient();
        mClient.setSession(mSession);
//        mClient.setCallback(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mClient.release();
        mSession.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.surface:
                toggleStream();
                break;
        }
    }

    public void toggleStream() {
        if (!mClient.isStreaming()) {
            String ip, port, path;

            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(Player.this);

            Pattern uri = Pattern.compile("rtsp://(.+):(\\d*)/(.+)");
            Matcher m = uri.matcher(mPrefs.getString("uri", ""));
            m.find();
            ip = m.group(1);
            port = m.group(2);
            path = m.group(3);

            mClient.setServerAddress(ip, Integer.parseInt(port));
            mClient.setStreamPath("/" + path);
            mClient.startStream();
        }
        else {
            mClient.stopStream();
        }
    }
}
