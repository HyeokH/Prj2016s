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

import com.example.prj2016s.R;
import com.example.prj2016s.RtspServer.lib.RtspClient;

import org.w3c.dom.Text;

import java.io.IOException;
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
    private int bwSet = 30000;
    LinkedList<String> downloaded;
    private TextView bitchange;
    private TextView logchange;
    private Button button_auto;
    private Button button_inc;
    private Button button_dec;

    class DownloadTs extends Thread {
        @Override
        public void run() {
            super.run();
            downloaded.add(man.getNext(bwSet, isAuto, ManagerActivity.this));
                }
            }

    public String printTs(){
        String result = "";
        for (int i=0; i<downloaded.size(); i++)
            result = result + downloaded.get(i);
        return result;
    }

    public CallbackEvent callbackEvent = new CallbackEvent(){
        @Override
        public String callbackMethod() {
            // TODO Auto-generated method stub
            DownloadTs hi = new DownloadTs();
            hi.start();
            String result = downloaded.getFirst();
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
        logchange = (TextView)findViewById(R.id.Ts_log);
        button_auto = (Button)findViewById(R.id.auto_bw);
        button_inc = (Button)findViewById(R.id.increase_bw);
        button_dec = (Button)findViewById(R.id.decrease_bw);

        button_auto.setOnClickListener(this);
        button_inc.setOnClickListener(this);
        button_dec.setOnClickListener(this);

        bitchange.setText("30000");
        downloaded = new LinkedList<String>();

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
                downloaded.add(man.getNext(bwSet, isAuto, this));
                downloaded.add(man.getNext(bwSet, isAuto, this));
            } catch (IOException e) {
                e.printStackTrace();
            }

            logchange.setText(printTs());

        }
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.auto_bw:
                isAuto = true;
            case R.id.increase_bw:
                bwSet = bwSet+10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
            case R.id.decrease_bw:
                bwSet = bwSet-10;
                isAuto = false;
                bitchange.setText(Integer.toString(bwSet));
        }
    }
}
