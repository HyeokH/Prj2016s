package com.example.prj2016s.Manager;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
/**
 * Created by Kang on 2016-06-02.
 */

//M3u8Manager how to use
    //만약 M3u8Manager 생성하면, high, mid, low의 m3u8을 이용하여 ts 테이블 구성
    //findTS에 시간 넣어주면 52.79.138.33/(해당 ts파일 위치)에서 해당 ts 파일 위치 return
public class M3u8Manager {
    public static int timeLoc;
    //location of saved files(root)
    public String dir;
    private TsManager tt;
    private String vName;

    //get m3u8
    public void getM3u8(String name, Context ctx) { //name - with ext.
        File rootFolder = ctx.getFilesDir();

        String[] lName = name.split("\\.", 0);
        dir = rootFolder.getAbsolutePath() + "/" + lName[0];
        boolean hi = false;

        File dest = new File(dir);
        if (!dest.exists())
            hi = dest.mkdirs();
        else
            hi = true;
        Log.d("folder", String.valueOf(hi));

        String highURL = lName[0] + "/" + lName[0] + "_high/" + lName[0] + "_high.m3u8";
        String midURL = lName[0] + "/" + lName[0] + "_mid/" + lName[0] + "_mid.m3u8";
        String lowURL = lName[0] + "/" + lName[0] + "_low/" + lName[0] + "_low.m3u8";
        String MasterURL = lName[0] + "/" + lName[0] + ".m3u8";
        try {
            HttpDownload.httpGet(highURL, dir);
            HttpDownload.httpGet(midURL, dir);
            HttpDownload.httpGet(lowURL, dir);
            HttpDownload.httpGet(MasterURL, dir);
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(ctx, "error in http requests", Toast.LENGTH_SHORT).show();
        }
    }
    //스트리밍할 준비
    public void prepare(String nameTemp, Context ctx) throws IOException{
        //bandwidth濡� 怨�, 以�, �� �뙋�떒
        //location怨� m3u8�뙆�씪 ts �뙆�씪 留ㅻ땲���뿉寃� �쟾�떖
        String[] name = nameTemp.split("\\.", 0);
        getM3u8(nameTemp, ctx);
        vName = name[0];

        tt = new TsManager(dir, vName);
    }

    public String getNext(int bw, boolean auto, Context ctx){
        return tt.getNext(bw, auto, ctx);
    }
}