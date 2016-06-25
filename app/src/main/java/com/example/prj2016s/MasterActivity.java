package com.example.prj2016s;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;

import com.example.prj2016s.Manager.M3u8Manager;
import com.example.prj2016s.Manager.ManagerActivity;

import java.io.IOException;

/**
 * Created by Kang on 2016-06-25.
 */
public class MasterActivity extends Activity implements View.OnClickListener {
    private Button buttonMp4;
    private Button buttonRec;
    private Button buttonStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        buttonMp4 = (Button) findViewById(R.id.button_mp4);
        buttonRec = (Button) findViewById(R.id.button_record);
        buttonStream = (Button) findViewById(R.id.button_stream);
        buttonMp4.setOnClickListener(this);
        buttonRec.setOnClickListener(this);
        buttonStream.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            case R.id.button_mp4:
                String Value = "test.mp4";
                Intent intent0 = new Intent(MasterActivity.this, ManagerActivity.class);
                intent0.putExtra("fileName", Value);
                startActivity(intent0);
                break;
            case R.id.button_record:
                Intent intent1 = new Intent(MasterActivity.this, VideoSave.class);
                startActivity(intent1);
                break;
            case R.id.button_stream:
                String ValueS = "streaming.mp4";
                Intent intent2 = new Intent(MasterActivity.this, ManagerActivity.class);
                intent2.putExtra("fileName", ValueS);
                startActivity(intent2);
                break;
        }
    }
}
