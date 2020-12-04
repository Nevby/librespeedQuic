package com.group3.speedtest.core.base;

import android.util.Log;

import org.chromium.net.UploadDataSink;

import java.nio.ByteBuffer;

public class ULDataProvider extends org.chromium.net.UploadDataProvider {
    private byte[] garbage;
    private int chunks;

    public ULDataProvider(int chunks, byte[] garbage) {
        this.chunks = chunks;
        this.garbage = garbage;
    };

    @Override
    public long getLength(){
        long sizeI = garbage.length;
        Log.d("MyUploadDataProvider","Length = "+sizeI);
        return sizeI;
    }
    @Override
    public void rewind(UploadDataSink uploadDataSink) {
        Log.d("MyUploadDataProvider","REWIND IS CALLED");
        uploadDataSink.onRewindSucceeded();
    }
    @Override
    public void read(UploadDataSink uploadDataSink, ByteBuffer byteBuffer) {
        Log.d("MyUploadDataProvider","READ IS CALLED. index = " );
    }
}

