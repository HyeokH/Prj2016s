package com.example.prj2016s;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

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
// parameter url -> "http://52.79.138.33/video_upload/url"
// parameter path -> a path including filename in the client (for example, "android.resource://" + getPackageName() + "/" + R.raw.kitty)

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        try {
            httpPost("asdf","asdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* url is terminated by / */
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
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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

/* filename_at_server is the name for the file would be saved at server (for example, video_output0 video_output1 video_output2) */
/* filepath_at_client is a path including filename in the client (for example, "android.resource://" + getPackageName() + "/" + R.raw.kitty) */

    public void httpPost(String filename_at_server, Integer mIth, String filepath_at_client) throws IOException {

        String url = "http://52.79.138.33/video_upload/";
        upload(url, filename_at_server, mIth, filepath_at_client);
    }

}
