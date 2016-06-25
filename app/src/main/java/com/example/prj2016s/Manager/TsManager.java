package com.example.prj2016s.Manager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;


import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by Kang on 2016-06-02.
 */

public class TsManager {
    public String pathInDev; //device 내의 영상 파일이 저장된 path
    public String fileName; //target file - without ext.
    public ArrayList<TsFile> lowList;
    public ArrayList<TsFile> midList;
    public ArrayList<TsFile> highList;
    private int[] bandwidth = new int[3];
    private int tsSize;
    private int curr = 0;

    public TsManager(){
    }

    public String getNext(int bw, boolean auto, Context ctx){
        ArrayList<TsFile> tsList;
        int currBand = 200;
        //get some bandwidth
        //this line will be replaced by WifiManager
        if (auto) {
            ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        currBand = wifiInfo.getLinkSpeed();
                        Log.d("current bandwidth", String.valueOf(currBand));
                    }
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    //I don't know what to do
                }
            } else {
                Toast.makeText(ctx, "internet is not connected", Toast.LENGTH_SHORT).show();
                //we have to handle this
            }
        }
        else
            currBand = bw;
        //whether it is in low bandwidth or ~
        if (currBand <= bandwidth[0]){
            tsList = lowList;
            System.out.println("current bandwidth is in low region");
        }
        else if(currBand >= bandwidth[2]){
            tsList = highList;
            System.out.println("current bandwidth is in high region");
        }
        else{
            tsList = midList;
            System.out.println("current bandwidth is in mid region");
        }
        String tsURL;
        TsFile currTs;
        if (curr>=tsSize)
            return "";
        else {
            currTs = tsList.get(curr);
            tsURL = currTs.getLoc();
            curr++;
        }
        try {
                HttpDownload.httpGet(tsURL, pathInDev);
                return pathInDev+"/"+currTs.getName();
        } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ctx, "error in http requests", Toast.LENGTH_SHORT).show();
            return "";
        }
    }
    /*
    public TsFile findTs(long t, int bw, boolean auto, Context ctx){
        ArrayList<TsFile> tsList;
        int currBand = 30000;
        //get some bandwidth
        //this line will be replaced by WifiManager
        if (auto) {
            ConnectivityManager conMan = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
            if (activeNetwork != null) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        currBand = wifiInfo.getLinkSpeed();
                        Log.d("current bandwidth", String.valueOf(currBand));
                    }
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    //I don't know what to do
                }
            } else {
                Toast.makeText(ctx, "internet is not connected", Toast.LENGTH_SHORT).show();
                //we have to handle this
            }
        }
        else
            currBand = bw;
        //whether it is in low bandwidth or ~
        if (currBand <= bandwidth[0]){
            tsList = lowList;
            System.out.println("current bandwidth is in low region");
        }
        else if(currBand >= bandwidth[2]){
            tsList = highList;
            System.out.println("current bandwidth is in high region");
        }
        else{
            tsList = midList;
            System.out.println("current bandwidth is in mid region");
        }

        TsFile curr = null;
        int i;
        int size = tsList.size();
        String tsURL = "";
        for(i=(int)size/2; i < size && i >= 0; ){
            curr = tsList.get(i);
            if (t <= curr.getTimeEnd()){
                if (t >= curr.getTimeStart()) {
                    tsURL = curr.getLoc();
                    break;
                }
                else
                    i--;
            }
            else
                i++;
        }

        if (tsURL.equals(""))
            return null;
        else {
            try {
                HttpDownload.httpGet(tsURL, pathInDev);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ctx, "error in http requests", Toast.LENGTH_SHORT).show();
            }
            return curr;
        }
    }*/
    public TsManager(String p, String f) throws IOException{
        long lTime, ltemp;

        pathInDev = p;
        fileName = f;
        String line;
        String highURL = fileName+"/"+fileName+"_high/";
        String midURL = fileName+"/"+fileName+"_mid/";
        String lowURL = fileName+"/"+fileName+"_low/";

        try {
            BufferedReader brMaster = new BufferedReader(new FileReader(pathInDev+"/"+f+".m3u8"));
            int bandIndex, bandCount = 0, k, l;
            while(true){
                line = brMaster.readLine();
                if (line==null) break;
                else{
                    bandIndex = line.indexOf("BANDWIDTH=");
                    if (bandIndex != -1){
//	            	if ((bandIndex = line.indexOf("BANDWIDTH="))!=-1){
                        line = line.substring(bandIndex+10, line.length());
                        for(k=0; ;){
                            if (line.charAt(k)>=48 && line.charAt(k)<=57)
                                k++;
                            else{
                                break;
                            }
                        }
                        line = line.substring(0, k);
                        bandwidth[bandCount++] = Integer.parseInt(line);
                        if (bandCount >= 3)
                            break;
                    }
                }
            }
            int swap;
            //Sort the bandwidths
            for(k=0; k<2; k++){
                for(l=0; l<2; l++){
                    if (bandwidth[l] > bandwidth[l+1]){
                        swap = bandwidth[l];
                        bandwidth[l] = bandwidth[l+1];
                        bandwidth[l+1] = swap;
                    }
                }
            }/*
			for(k=0; k<2; k++){
				System.out.println(bandwidth[k]);
			}*/
        }
        catch(Exception e){
            System.out.println("There is no bandwidth definition.");
        }
        //find m3u8 and make array list of each m3u8
        try {
            lTime = 0;
            BufferedReader br = new BufferedReader(new FileReader(pathInDev+"/"+f+"_high.m3u8"));
            highList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + (long)(1000*Float.parseFloat(line.substring(8, line.length()-1)));
                        String loc = br.readLine();
                        int dIndex = loc.lastIndexOf('.');
                        loc = loc.substring(0, dIndex)+".h264";
                        TsFile tsSeg = new TsFile(lTime, ltemp, highURL+loc, loc);
                        highList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
            lTime = 0;
            br = new BufferedReader(new FileReader(pathInDev+"/"+f+"_mid.m3u8"));
            midList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + (long)(1000*Float.parseFloat(line.substring(8, line.length()-1)));
                        String loc = br.readLine();
                        int dIndex = loc.lastIndexOf('.');
                        loc = loc.substring(0, dIndex)+".h264";
                        TsFile tsSeg = new TsFile(lTime, ltemp, midURL+loc, loc);
                        midList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
            lTime = 0;
            br = new BufferedReader(new FileReader(pathInDev+"/"+f+"_low.m3u8"));
            lowList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + (long)(1000*Float.parseFloat(line.substring(8, line.length()-1)));
                        String loc = br.readLine();
                        int dIndex = loc.lastIndexOf('.');
                        loc = loc.substring(0, dIndex)+".h264";
                        TsFile tsSeg = new TsFile(lTime, ltemp, lowURL+loc, loc);
                        lowList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
            tsSize = highList.size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ex){
            System.out.println("M3u8 format error!");
        }
        Log.i("TsManager", "TsManager finished");
    }
}