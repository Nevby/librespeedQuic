package com.group3.speedtest.core.base;

import android.util.Log;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.nio.ByteBuffer;

public class UrlRequestCallback extends UrlRequest.Callback {
    private static final String TAG = "MyUrlRequestCallback";

    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        Log.d("ConnectionQUIC", "onRedirectReceived method called.");
        // You should call the request.followRedirect() method to continue
        // processing the request.
        request.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        Log.d("ConnectionQUIC", "onResponseStarted method called.");
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes to the read() method.
        request.read(ByteBuffer.allocateDirect(102400));
    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
        Log.d("ConnectionQUIC", "onReadCompleted method called.");
        // You should keep reading the request until there's no more data.
        request.read(ByteBuffer.allocateDirect(102400));
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        Log.d("ConnectionQUIC", "onSucceeded method called.");
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        Log.d("ConnectionQUIC", "onFailed method called.");
        /*
        Log.d("ConnectionQUIC", "onFailed request: " + request);
        Log.d("ConnectionQUIC", "onFailed info: " + info);
        Log.d("ConnectionQUIC", "onFailed error: " + error);
         */
    }
}