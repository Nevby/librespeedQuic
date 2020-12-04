package com.group3.speedtest.core.ping;

import android.util.Log;

import com.group3.speedtest.core.base.UrlRequestCallback;

import org.chromium.net.CronetEngine;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class PingerQuic {

    long startPing=0;
    long stopPing=0;
    UrlRequestCallback callbackPing;
    Executor executor;
    UrlRequest request;

    public PingerQuic(CronetEngine engine, String path){
        Log.e("PingerQuic", "Quic pingTest");
        callbackPing = new UrlRequestCallback() {
            @Override
            public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
                stopPing=System.currentTimeMillis();
                Log.d("SpeedtestWorkerQuic", "callbackPing() onResponseStarted: " + request);
                onPong(stopPing - startPing);
                super.onResponseStarted(request, info);
            }
        };
        executor = Executors.newSingleThreadExecutor();
        UrlRequest.Builder requestBuilder = engine.newUrlRequestBuilder( path, callbackPing, executor);
        request = requestBuilder.build();
        init();
    }

    private void init(){
        Log.e("PingerQuic", "Quic pingTest");
        startPing=System.currentTimeMillis();
        request.start();
    }
    public abstract void onPong(long ms);
}
