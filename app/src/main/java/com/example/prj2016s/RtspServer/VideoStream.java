package com.example.prj2016s.RtspServer;

/**
 * Created by 성혁화 on 2016-06-15.
 * Read video data from disk
 */
import com.example.prj2016s.R;

import java.io.*;

public class VideoStream {

    //FileInputStream fis; //video file
    InputStream fis; //video file
    int frame_nb; //current frame nb

    //-----------------------------------
    //constructor
    //-----------------------------------
    public VideoStream(String filename) throws Exception{
        //init variables
        fis = new FileInputStream(filename);
        frame_nb = 0;
    }
    public VideoStream(InputStream inputStream) throws Exception{
        //init variables
        fis = inputStream;
        frame_nb = 0;
    }

    //-----------------------------------
    // getnextframe
    //returns the next frame as an array of byte and the size of the frame
    //-----------------------------------
    public int getnextframe(byte[] frame) throws Exception
    {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        //read current frame length
        fis.read(frame_length,0,5);

        //transform frame_length to integer
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);

        return(fis.read(frame,0,length));
    }
}
