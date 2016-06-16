package com.example.prj2016s;

/**
 * Created by Kang on 2016-06-11.
 */
import android.app.Activity;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.prj2016s.R;


public class MediaBuffer extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Camera camera = null;
    SurfaceHolder  holder = null;
    VideoView videoView = null;
    TextView textView = null;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        textView.setText("DATA: " + data.toString());
        textView.setHighlightColor(Color.BLACK);
        textView.setTextColor(Color.WHITE);
        textView.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_buffer);

        videoView = (VideoView)findViewById(R.id.videoView2);
        textView = (TextView)findViewById(R.id.textView2);

        camera = Camera.open();
        Camera.Parameters params = camera.getParameters();
        params.setPreviewFpsRange(15000, 30000);
        params.setPreviewSize(1280, 720);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//        params.setPictureFormat(ImageFormat.NV21);
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewFrameRate(30);
        camera.setDisplayOrientation(90);
        camera.setParameters(params);

        holder = videoView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(this);
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.unlock();
            camera.reconnect();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}


