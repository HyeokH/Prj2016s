package com.example.prj2016s.Manager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.prj2016s.R;
import com.example.prj2016s.RtspServer.lib.RtspClient;
import com.example.prj2016s.RtspServer.lib.RtspServer;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Kang on 2016-06-03.
 */



public class ManagerActivity extends Activity implements View.OnClickListener {
    private long START_TIME;
    private boolean isAuto;
    private int bwSet = 300;
    private TextView bitchange;
    private VideoView mVideoView;
    private Button button_auto;
    private Button button_inc;
    private Button button_dec;



    //EventRegistration eventRegistration = new EventRegistration(ManagerActivity.callbackEvent);
    //String inputPath = eventRegistration.doWork();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);
        START_TIME = SystemClock.currentThreadTimeMillis();
        bitchange = (TextView)findViewById(R.id.bw);
        mVideoView = (VideoView)findViewById(R.id.tsView);
        button_auto = (Button)findViewById(R.id.auto_bw);
        button_inc = (Button)findViewById(R.id.increase_bw);
        button_dec = (Button)findViewById(R.id.decrease_bw);

        button_auto.setOnClickListener(this);
        button_inc.setOnClickListener(this);
        button_dec.setOnClickListener(this);

        bitchange.setText("300");

        Intent intent = getIntent();
        String fileName= intent.getStringExtra("fileName");

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(RtspServer.KEY_IS_AUTO, false);
        editor.putString(RtspServer.KEY_BW, String.valueOf(310));
        editor.putString(RtspServer.KEY_NAME, fileName);
        editor.commit();
//        String fileName= "test.mp4";

        //rtsp packet 만들기
//       make_packet(downloaded.getFirst());
//       downloaded.remove(downloaded.getFirst()) ;


        this.startService(new Intent(this,RtspServer.class));
        mVideoView.setVideoPath("rtsp://127.0.0.1:8086");
        mVideoView.requestFocus();
        mVideoView.start();
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.auto_bw:
                isAuto = true;
                SharedPreferences.Editor editor3 = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor3.putBoolean(RtspServer.KEY_IS_AUTO, true);
                editor3.commit();
                break;
            case R.id.increase_bw:
                bwSet = bwSet+10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean(RtspServer.KEY_IS_AUTO, false);
                editor.putString(RtspServer.KEY_BW, String.valueOf(bwSet));
                editor.commit();
                Log.i("ManagerActivity", "bit changed: "+Integer.toString(bwSet));
                break;
            case R.id.decrease_bw:
                bwSet = bwSet-10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
                SharedPreferences.Editor editor2 = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor2.putBoolean(RtspServer.KEY_IS_AUTO, false);
                editor2.putString(RtspServer.KEY_BW, String.valueOf(bwSet));
                editor2.commit();
                Log.i("ManagerActivity", "bit changed: "+Integer.toString(bwSet));
                break;
        }
    }
}
