package com.example.prj2016s;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.io.DataOutputStream;
import java.io.File;
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

    private void upload(String url, String path){
        int sIndex = url.lastIndexOf('/');
        int pIndex = url.lastIndexOf('.');

        String fileName = url.substring(sIndex + 1);

        if (pIndex >= 1 && sIndex >= 0 && sIndex < url.length() - 1) {

            FileOutputStream outStream = null;
            URLConnection uCon = null;
            InputStream is = null;
            try {
                URL Url = new URL(url.substring(0,sIndex + 1) + "video_output0.mp4");
                System.out.println(Url);
                byte[] buf;
                int byteRead;
                int byteWritten = 0;
                String path1 = "android.resource://" + getPackageName() + "/" + R.raw.kitty;
                File filePointer = new File(path1);
                System.out.println("asdfasdfasdfasdf"+path1);
//                outStream = new FileOutputStream();
//                uCon = Url.openConnection();
//                is = uCon.getInputStream();
//                is = new FileInputStream(filePointer);
                buf = new byte[size];
                HttpURLConnection urlConnection = (HttpURLConnection) Url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestMethod("POST");
                    DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
                    out.write(buf);

//                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                }
                finally {
                    urlConnection.disconnect();
                }
//                while ((byteRead = is.read(buf)) != -1) {
//                    outStream.write(buf, 0, byteRead);
//                    byteWritten += byteRead;
//                }
                System.out.println(fileName);
                System.out.println(byteWritten+" bytes");
                is.close();
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

/* filename_at_server is the name for the file would be saved at server (for example, video_output0 video_output1 video_output2) */
/* filepath_at_client is a path including filename in the client (for example, "android.resource://" + getPackageName() + "/" + R.raw.kitty) */

    public void httpPost(String filename_at_server, String filepath_at_client) throws IOException {


        String url = "http://52.79.138.33/video_upload/";
        upload(url+filename_at_server, filepath_at_client);
    }

}
