package com.example.prj2016s.etc;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;


    public interface SeekableByteChannel extends ByteChannel, Channel, Closeable, ReadableByteChannel, WritableByteChannel {
        long position() throws IOException;

        SeekableByteChannel setPosition(long newPosition) throws IOException;

        long size() throws IOException;

        SeekableByteChannel truncate(long size) throws IOException;
    }