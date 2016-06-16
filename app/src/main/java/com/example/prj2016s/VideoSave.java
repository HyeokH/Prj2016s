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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
    private int mIth = 0;
    private String OUTPUT_PATH;
    private AsyncTask task = null;

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
        OUTPUT_FILE = this.getFilesDir().getAbsolutePath()+"/recorded";
//        OUTPUT_FILE = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video_output";
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

    protected void startIntervalRecording() {
        mTimer = new CountDownTimer(20000, 1000) {
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
//        mMediaRecorder.setMaxDuration(200000);
        if (mIth == 0){}
        else{
            if (mIth%2 == 1){
                //저장된 파일은 0번
                OUTPUT_PATH = OUTPUT_FILE + "0.mp4";
            }
            else {
                //저장된 파일은 1번
                OUTPUT_PATH = OUTPUT_FILE + "1.mp4";
            }
            new AsyncCallWS().execute();
        }
        mMediaRecorder.setOutputFile(OUTPUT_FILE+Integer.toString(mIth%2)+".mp4");
        mToaster.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), "중계중("+Integer.toString(mIth++*20)+"초)", Toast.LENGTH_SHORT).show();
            }
        });
/*        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(16);
        mMediaRecorder.setVideoSize(1280,720); */
        mMediaRecorder.setOrientationHint(90);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        CamcorderProfile profile = null;
/*
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            Log.d("profile", "1");
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        }
        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            Log.d("profile", "2");
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        }
        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            Log.d("profile", "3");
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        }
        else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH)) {
            Log.d("profile", "4");
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        }*/

        Log.d("profile", "QUALITY LOW");
        profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        mMediaRecorder.setProfile(profile);

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

private class AsyncCallWS extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {
        Log.i(TAG, "doInBackground");
        try {
            uploadVideo(OUTPUT_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.i(TAG, "onPostExecute");
    }

    @Override
    protected void onPreExecute() {
        Log.i(TAG, "onPreExecute");
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        Log.i(TAG, "onProgressUpdate");
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

    private void uploadVideo(String videoPath) throws ParseException, IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://52.79.138.33/video_upload");
        String fileName = "streaming.mp4";

        FileBody filebodyVideo = new FileBody(new File(videoPath));
        StringBody title = new StringBody(fileName);
        StringBody description = new StringBody("This is a video of the agent");
//        StringBody code = new StringBody(realtorCodeStr);
        int currNum = (mIth-1);

        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("videoFile", filebodyVideo);
        reqEntity.addPart("title", title);
        reqEntity.addPart("description", description);
        //      reqEntity.addPart("code", code);
        httppost.setEntity(reqEntity);

        // DEBUG
        System.out.println( "executing request " + httppost.getRequestLine( ) );
        HttpResponse response = httpclient.execute( httppost );
        Log.i(TAG, Integer.toString(currNum)+"번째 영상 업로드!");
        HttpEntity resEntity = response.getEntity( );

        // DEBUG
        System.out.println( response.getStatusLine( ) );
        if (resEntity != null) {
            System.out.println( EntityUtils.toString( resEntity ) );
        } // end if

        if (resEntity != null) {
            resEntity.consumeContent( );
        } // end if

        httpclient.getConnectionManager( ).shutdown( );
    }
}