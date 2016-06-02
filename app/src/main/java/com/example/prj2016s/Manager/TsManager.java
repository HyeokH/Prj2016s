package com.example.prj2016s.Manager;
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

    public TsManager(){
    }
    public String findTs(float t){
        ArrayList<TsFile> tsList;
        //get some bandwidth
        //this line will be replaced by WifiManager
        int currBand = 30000; //temporary

        //whether it is in low bandwidth or ~
        if (currBand <= bandwidth[0])
            tsList = lowList;
        else if(currBand >= bandwidth[2])
            tsList = highList;
        else
            tsList = midList;

        TsFile curr;
        int i;
        int size = tsList.size();
        for(i=(int)size/2; i < size; ){
            curr = tsList.get(i);
            if (t <= curr.getTimeEnd()){
                if (t >= curr.getTimeStart())
                    return curr.getLoc();
                else
                    i--;
            }
            else
                i++;
        }
        return "Can't find ts file - wrong time location";
    }
    public TsManager(String p, String f) throws IOException{
        float lTime, ltemp;

        pathInDev = p;
        fileName = f;
        String line;
        String highURL = fileName+"/"+fileName+"_high/";
        String midURL = fileName+"/"+fileName+"_mid/";
        String lowURL = fileName+"/"+fileName+"_low/";

        try {
            BufferedReader brMaster = new BufferedReader(new FileReader(pathInDev+"\\"+f+".m3u8"));
            int bandIndex, bandCount = 0, k, l;
            while(true){
                line = brMaster.readLine();
                if (line==null) break;
                else{
                    if ((bandIndex = line.indexOf("BANDWIDTH="))!=-1){
                        line = line.substring(bandIndex+10, line.length());
                        for(k=0; ;){
                            if (line.charAt(k)>=80 && line.charAt(k)<=89)
                                k++;
                            else{
                                break;
                            }
                        }
                        line = line.substring(0, k+1);
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
            }
        }
        catch(Exception e){
            System.out.println("There is no bandwidth definition.");
        }
        //find m3u8 and make array list of each m3u8
        try {
            lTime = 0;
            BufferedReader br = new BufferedReader(new FileReader(pathInDev+"\\"+f+"_high.m3u8"));
            highList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + Float.parseFloat(line.substring(8, line.length()-1));
                        String loc = br.readLine();
                        TsFile tsSeg = new TsFile(lTime, ltemp, highURL+loc);
                        highList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
            lTime = 0;
            br = new BufferedReader(new FileReader(pathInDev+"\\"+f+"_mid.m3u8"));
            System.out.println(pathInDev+"\\"+f+"_mid.m3u8");
            midList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + Float.parseFloat(line.substring(8, line.length()-1));
                        String loc = br.readLine();
                        TsFile tsSeg = new TsFile(lTime, ltemp, midURL+loc);
                        midList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
            lTime = 0;
            br = new BufferedReader(new FileReader(pathInDev+"\\"+f+"_low.m3u8"));
            System.out.println(pathInDev+"\\"+f+"_low.m3u8");
            lowList = new ArrayList<TsFile>();
            for(int i=0; i < 5; i++)
                line = br.readLine();
            while(true){
                line = br.readLine();
                if (line==null) break;
                else{
                    if (line.substring(0, 8).compareTo("#EXTINF:")==0){
                        ltemp = lTime + Float.parseFloat(line.substring(8, line.length()-1));
                        String loc = br.readLine();
                        TsFile tsSeg = new TsFile(lTime, ltemp, lowURL+loc);
                        lowList.add(tsSeg);
                        lTime = ltemp;
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception ex){
            System.out.println("M3u8 format error!");
        }
    }
}
