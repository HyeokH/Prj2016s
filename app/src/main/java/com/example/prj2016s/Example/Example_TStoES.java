package com.example.prj2016s.Example;
import com.example.prj2016s.etc.FileChannelWrapper;
import com.example.prj2016s.TS2ESConverter;
import com.example.prj2016s.etc.SeekableByteChannel;

import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.File;
import java.io.FileOutputStream;

// http://lists.live555.com/pipermail/live-devel/2013-November/017694.html
// RTSP (RTP) -> H.264ES -> MPEG2-TS muxing problem

// http://stackoverflow.com/questions/9082952/rtp-rtsp-library-usable-in-java
// stackoverflow RTP/RTSP library usable in Java

// https://github.com/fyhertz/libstreaming
// A solution for streaming H.264, H.263, AMR, AAC using RTP on Android


public class Example_TStoES {
	public static void main(String[] args) throws Exception {
        SeekableByteChannel source = null;
		
        Cmd cmd = MainUtils.parseArguments(args);
        source = new FileChannelWrapper(new FileInputStream(cmd.getArg(0)).getChannel());

        ByteBuffer data = ByteBuffer.allocate(200000);

        TS2ESConverter demuxer = new TS2ESConverter(source);
        for (int i=0; demuxer.read(data) != -1; i++) {}
        
        File file = new File("out.txt");
        boolean append = false;
        FileChannel channel = new FileOutputStream(file, append).getChannel();
     	data.flip();
     	channel.write(data);
     	channel.close();
	}
}
