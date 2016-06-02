package com.example.prj2016s.Manager;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
/**
 * Created by Kang on 2016-06-02.
 */
public class M3u8Manager {
    public static int timeLoc;
    //location of saved files(root)
    public static String path = "C:\\Users\\Kang\\Desktop\\공부\\workspace\\Client";

    //get m3u8
    public static void getM3u8(String name){ //name - with ext.
//		public static void main(String[] args){
        String[] lName = name.split("\\.", 0);
        File dest = new File(path+"\\"+lName[0]+"\\");

        if(!dest.exists())
            dest.mkdirs();

        String highURL = lName[0]+"/"+lName[0]+"_high/"+lName[0]+"_high.m3u8";
        String midURL = lName[0]+"/"+lName[0]+"_mid/"+lName[0]+"_mid.m3u8";
        String lowURL = lName[0]+"/"+lName[0]+"_low/"+lName[0]+"_low.m3u8";
        String MasterURL = lName[0]+"/"+lName[0]+".m3u8";

        HttpDownload.httpGet(highURL, path+"\\"+lName[0]+"\\");
        HttpDownload.httpGet(midURL, path+"\\"+lName[0]+"\\");
        HttpDownload.httpGet(lowURL, path+"\\"+lName[0]+"\\");
        HttpDownload.httpGet(MasterURL, path+"\\"+lName[0]+"\\");
    }
    public static void main(String[] args) throws IOException{
        //bandwidth濡� 怨�, 以�, �� �뙋�떒
        //location怨� m3u8�뙆�씪 ts �뙆�씪 留ㅻ땲���뿉寃� �쟾�떖
        String[] name = args[0].split("\\.", 0);
        TsFile test;
        getM3u8(args[0]);

        TsManager tt = new TsManager(path+"\\"+name[0], name[0]);
        tt.findTs((float)15.687);
    }
}