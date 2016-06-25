package com.example.prj2016s.Manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.prj2016s.R;
import com.example.prj2016s.RtspServer.lib.RtspClient;

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
    private M3u8Manager man;
    private boolean isAuto;
    private int bwSet = 300;
    LinkedList<InputStream> downloaded;
    private TextView bitchange;
    private VideoView mVideoView;
    private Button button_auto;
    private Button button_inc;
    private Button button_dec;

    class DownloadTs extends Thread {
        @Override
        public void run() {
            super.run();
            InputStream temp = null;
            String filePath = man.getNext(bwSet, isAuto, ManagerActivity.this);
            if (filePath.equals("")){
                temp = null;
            }
            else {
                try {
                    temp = new FileInputStream(filePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            downloaded.add(temp);
                }
            }

    public CallbackEvent callbackEvent = new CallbackEvent(){
        @Override
        public InputStream callbackMethod() {
            // TODO Auto-generated method stub
            DownloadTs hi = new DownloadTs();
            hi.start();
            InputStream result = downloaded.getFirst();
            downloaded.removeFirst();
            return(result);
        }
    };

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
        downloaded = new LinkedList<InputStream>();

        Intent intent = getIntent();
        String fileName= intent.getStringExtra("fileName");

        //rtsp packet 만들기
//       make_packet(downloaded.getFirst());
//       downloaded.remove(downloaded.getFirst()) ;

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            try {
                man = new M3u8Manager();
                man.prepare(fileName, this);
                downloaded.add(new FileInputStream(man.getNext(bwSet, isAuto, this)));
                downloaded.add(new FileInputStream(man.getNext(bwSet, isAuto, this)));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.auto_bw:
                isAuto = true;
                break;
            case R.id.increase_bw:
                bwSet = bwSet+10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
                Log.i("ManagerActivity", "bit changed: "+Integer.toString(bwSet));
                System.out.println(downloaded.getFirst());
                break;
            case R.id.decrease_bw:
                bwSet = bwSet-10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
                Log.i("ManagerActivity", "bit changed: "+Integer.toString(bwSet));
                break;
        }
    }
}
