package com.example.prj2016s.Manager;

/**
 * Created by Kang on 2016-06-02.
 */
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpDownload {

    final static int size = 1024;
    /**
     *
     * @param url
     * @param dir
     */

// parameter url -> "http://52.79.138.33/url"
// parameter dir -> a directory in the device(where the file will be saved)
    private static void download(String url, String dir) {
        int sIndex = url.lastIndexOf('/');
        int pIndex = url.lastIndexOf('.');

        String fileName = url.substring(sIndex + 1);

        if (pIndex >= 1 && sIndex >= 0
                && sIndex < url.length() - 1) {

            OutputStream outStream = null;
            URLConnection uCon = null;
            InputStream is = null;
            try {
                URL Url;
                byte[] buf;
                int byteRead;
                int byteWritten = 0;
                Url = new URL(url);
                outStream = new BufferedOutputStream(new FileOutputStream(
                        dir + fileName));
                uCon = Url.openConnection();
                is = uCon.getInputStream();
                buf = new byte[size];
                while ((byteRead = is.read(buf)) != -1) {
                    outStream.write(buf, 0, byteRead);
                    byteWritten += byteRead;
                }
                System.out.println(fileName);
                System.out.println(byteWritten+" bytes");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     *
     * @param url
     * @param dir
     */

    public static void httpGet(String filename, String path){

        String url = "http://52.79.138.33/";
        download(url+filename, path);
    }


}

