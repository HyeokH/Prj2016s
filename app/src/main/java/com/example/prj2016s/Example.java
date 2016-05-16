package com.example.prj2016s;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;

import java.util.Set;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// http://lists.live555.com/pipermail/live-devel/2013-November/017694.html
// RTSP (RTP) -> H.264ES -> MPEG2-TS muxing problem

// http://stackoverflow.com/questions/9082952/rtp-rtsp-library-usable-in-java
// stackoverflow RTP/RTSP library usable in Java

// https://github.com/fyhertz/libstreaming
// A solution for streaming H.264, H.263, AMR, AAC using RTP on Android


public class Example {
	public static void main(String[] args) throws Exception {
        SeekableByteChannel source = null;
		
        Cmd cmd = MainUtils.parseArguments(args);
        source = new FileChannelWrapper(new FileInputStream(cmd.getArg(0)).getChannel());

        ByteBuffer data = ByteBuffer.allocate(200000);

        MTSDemuxer demuxer = new MTSDemuxer(source);
        for (int i=0; demuxer.read(data) != -1; i++) {}
        
        File file = new File("out.txt");
        boolean append = false;
        FileChannel channel = new FileOutputStream(file, append).getChannel();
     	data.flip();
     	channel.write(data);
     	channel.close();
	}
}
