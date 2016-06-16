package com.example.prj2016s;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by NAM on 2016. 6. 15..
 */
public class VideoUploader extends Activity {
    final static int size = 1024;
    /**
     *
     * @param url
     * @param path
     */
    private static Context context;
    private final String TAG = "VideoUploader";
// parameter url -> "http://52.79.138.33/video_upload/url"
// parameter path -> a path including filename in the client (for example, "android.resource://" + getPackageName() + "/" + R.raw.kitty)
    private String testStr = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video_output0.mp4";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        AsyncCallWS task = new AsyncCallWS();
        task.execute();
    }

    private class AsyncCallWS extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "doInBackground");
            try {
                uploadVideo(testStr);
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

    /* url is terminated by / */
    /*
    private void upload(String url, String filename, Integer mIth, String path){
        String crlf = "\r\n";
        FileOutputStream outStream = new FileOutputStream();
        try {
            URL Url = new URL(url + filename + '/' + mIth);
            System.out.println(Url);
            byte[] buf = new byte[size];;
            Integer byteRead;
            Integer byteWritten = 0;
            HttpURLConnection urlConnection = (HttpURLConnection) Url.openConnection();
            InputStream is = urlConnection.getInputStream();
            try {
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                urlConnection.setRequestProperty("Content-Type", "application/form-data");
                urlConnection.setRequestMethod("POST");
                DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                out.writeBytes(crlf);
                out.write(buf);
                out.writeBytes(crlf);
                out.flush();
                out.close();

//                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            }
            finally {
                urlConnection.disconnect();
            }
//                while ((byteRead = is.read(buf)) != -1) {
//                    outStream.write(buf, 0, byteRead);
//                    byteWritten += byteRead;
//                }
            is.close();
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    private void uploadVideo(String videoPath) throws ParseException, IOException {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://52.79.138.33/video_upload/video_output/1");
        int sIndex = videoPath.lastIndexOf('/');
        String fileName = videoPath.substring(sIndex + 1);

        FileBody filebodyVideo = new FileBody(new File(videoPath));
        StringBody title = new StringBody(fileName);
        StringBody description = new StringBody("This is a video of the agent");
//        StringBody code = new StringBody(realtorCodeStr);

        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("videoFile", filebodyVideo);
        reqEntity.addPart("title", title);
        reqEntity.addPart("description", description);
  //      reqEntity.addPart("code", code);
        httppost.setEntity(reqEntity);

        // DEBUG
        System.out.println( "executing request " + httppost.getRequestLine( ) );
        HttpResponse response = httpclient.execute( httppost );
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
    } // end of uploadVideo( )
/* filename_at_server is the name for the file would be saved at server (for example, video_output0 video_output1 video_output2) */
/* filepath_at_client is a path including filename in the client (for example, "android.resource://" + getPackageName() + "/" + R.raw.kitty) */
/*
    public void httpPost(String filename_at_server, Integer mIth, String filepath_at_client) throws IOException {

        String url = "http://52.79.138.33/video_upload/";
        upload(url, filename_at_server, mIth, filepath_at_client);
    }
*/
}
