package com.example.prj2016s;

/**
 * Created by Kang on 2016-06-13.
 */
import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.media.*;
import android.view.*;
import android.util.*;
import android.os.*;
import android.widget.Toast;

import java.io.*;
public class VideoSave extends Activity
        implements SurfaceHolder.Callback, Handler.Callback
{
    final private String TAG = "VideoSave";

    //handler command
    final private int START_RECORDING = 1;
    final private int STOP_RECORDING = 2;
    final private int INIT_RECORDER  = 3;
    final private int RELEASE_RECORDER = 4;
    final private int START_INTERVAL_RECORD = 5;
    private SurfaceHolder mSurfaceHolder = null;
    private MediaRecorder mMediaRecorder = null;
    private Handler   mHandler;
    private Handler mToaster;
    private CountDownTimer mTimer = null;
    private String OUTPUT_FILE;
    private Camera mCamera = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_buffer);

        //preview surface
        SurfaceView surView = (SurfaceView)findViewById(R.id.surface);
//        surView.setAspectRatio(320.0f/240.0f);
        SurfaceHolder holder = surView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //camera!
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        //handler
        mHandler = new Handler(this);
        mToaster = new Handler(this);
//        OUTPUT_FILE = this.getFilesDir().getAbsolutePath()+"/recorded";
        OUTPUT_FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video_output";
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( mTimer!=null ) {
            mTimer.cancel();
            mTimer = null;
        }
        stopMediaRecorder();
        releaseMediaRecorder();
    }

    int mIth = 0;

    protected void startIntervalRecording() {
        mTimer = new CountDownTimer(10000, 1000) {
            boolean recordStart = false;

            public void onTick(long millisUntilFinished) {
                if ( !recordStart) {
                    recordStart = true;
                    mHandler.sendEmptyMessage(START_RECORDING);
                }
            }
            public void onFinish() {
                mHandler.sendEmptyMessage(STOP_RECORDING);
                mHandler.sendEmptyMessage(RELEASE_RECORDER);
                mHandler.sendEmptyMessage(INIT_RECORDER);
                mHandler.sendEmptyMessage(START_INTERVAL_RECORD);
            }
        };
        mTimer.start();
    }

    protected void initMediaRecorder() {

        if ( mSurfaceHolder==null ) {
            Log.e(TAG, "No Surface Holder");
            return;
        }
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//     mMediaRecorder.setMaxDuration(200000);
        mMediaRecorder.setOutputFile(OUTPUT_FILE + mIth + ".mp4");
//        Toast.makeText(this, "중계중 "+ Integer.toString(mIth*10) +"초" ,Toast.LENGTH_SHORT);
        mToaster.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), "중계중("+Integer.toString(mIth*10)+"초)", Toast.LENGTH_SHORT).show();
            }
        });
        mIth++;
        mMediaRecorder.setVideoFrameRate(16);
        mMediaRecorder.setVideoSize(1920,1080);
        mMediaRecorder.setOrientationHint(90);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        try {
            mMediaRecorder.prepare();
        } catch (IOException exception) {
            releaseMediaRecorder();
            return;
        }
    }

    protected void releaseMediaRecorder() {
        if ( mMediaRecorder==null )
            return;
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
//        mCamera.lock();
    }

    protected void startMediaRecorder() {
        if ( mMediaRecorder!=null ) {
            Log.v(TAG, "Before Record Start");
            mMediaRecorder.start();
            Log.v(TAG, "Record Started");
        }
    }

    protected void stopMediaRecorder() {
        if ( mMediaRecorder!=null ) {
            Log.v(TAG, "Before Record Stop");
            mMediaRecorder.stop();
//            mCamera.lock();
            Log.v(TAG, "Record Stopped");
        }
    }


    //--------------------------------------------------------------------
    // SurfaceHolder.Callback Implementation
    @Override
    public void  surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    @Override
    public void  surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        if (mCamera != null){
            Camera.Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
//            Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG).show();
            finish();
        }
        //init video
        initMediaRecorder();
        startIntervalRecording();
    }
    @Override
    public void  surfaceDestroyed(SurfaceHolder holder) {
//        if (mMediaRecorder != null) mMediaRecorder.release();
        mSurfaceHolder = null;
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    //--------------------------------------------------------------------
    // Handler.Callback Implementation
    public boolean  handleMessage(Message msg) {
        switch (msg.what ) {
            case START_RECORDING:
                startMediaRecorder();
                return true;
            case STOP_RECORDING:
                stopMediaRecorder();
                return true;
            case INIT_RECORDER:
                initMediaRecorder();
                return true;
            case RELEASE_RECORDER:
                releaseMediaRecorder();
                return true;
            case START_INTERVAL_RECORD:
                startIntervalRecording();
                return true;
        }
        return false;
    }
}