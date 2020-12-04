package com.group3.speedtest.core.worker;

import android.app.Activity;
import android.util.Log;

import com.group3.speedtest.core.base.ULDataProvider;
import com.group3.speedtest.core.base.UrlRequestCallback;
import com.group3.speedtest.core.base.Utils;
import com.group3.speedtest.core.config.SpeedtestConfig;
import com.group3.speedtest.core.config.TelemetryConfig;
import com.group3.speedtest.core.log.Logger;
import com.group3.speedtest.core.ping.PingerQuic;
import com.group3.speedtest.core.serverSelector.TestPoint;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UploadDataSink;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public abstract class SpeedtestWorkerQuic extends Thread{
    private final Object MainActivity = null;
    private TestPoint backend;
    private SpeedtestConfig config;
    private TelemetryConfig telemetryConfig;
    private boolean stopASAP=false;
    private double dl=-1, ul=-1, ping=-1;
    private String ipIsp="";
    private Logger log=new Logger();
    CronetEngine.Builder myBuilder;
    CronetEngine engine;
    Executor executor;
    UrlRequestCallback callbackDL;
    UrlRequestCallback callbackPing;
    UrlRequestCallback callbackUL;
    ULDataProvider uploadProvider;
    int time = 10;

    long startDl = 0;
    long stopDl = 0;
    long progressDL = 0;
    long bonusTDL = 0;
    long receivedByteCount = 0;

    long startPing=0;
    long stopPing=0;

    long startUl=0;
    long stopUl=0;

    private byte[] garbage;
    private static final int BUFFER_SIZE=16384;
    int index = 0;
    int chunks = 20;
    double speedUl = -1;
    double progressUl = -1;

    private static final int MODE_BEGIN=0, MODE_IN_PROGRESS=1, MODE_ENDED=2;
    int mode = MODE_BEGIN;
    private static final int TEST_DL = 0, TEST_UL = 1, TEST_PING = 2;
    int test = TEST_DL;
    long[] pingArray;
    int nrOfPings = 0;
    int totPing = 0;
    long totalJitter;
    double jitter;
    PingerQuic pingerQuic;

    public void run(){
        Log.d("SpeedTestWorkerQuic", "Quic Test started");
        try {
            for (char t : config.getTest_order().toCharArray()) {
                if(stopASAP) break;
                if (t == '_') Utils.sleep(1000);
                //if (t == 'I') getIP();
                if (t == 'D') dlTest();
                if (t == 'U') ulTest();
                if (t == 'P') pingTest();
            }
        }catch (Throwable t){
            onCriticalFailure(t.toString());
        }
        try{
            //sendTelemetry();
        }catch (Throwable t){}
        onEnd();
    }

    public SpeedtestWorkerQuic(Activity mainActivity, TestPoint backend, SpeedtestConfig config, TelemetryConfig telemetryConfig){
        this.backend=backend;
        this.config=config==null?new SpeedtestConfig():config;
        this.telemetryConfig=telemetryConfig==null?new TelemetryConfig():telemetryConfig;
        myBuilder = new CronetEngine.Builder(mainActivity);
        engine = myBuilder.enableHttp2(true).enableQuic(true).build();
        executor = Executors.newSingleThreadExecutor();

        garbage=new byte[chunks*16384];
        Random r=new Random(System.nanoTime());
        r.nextBytes(garbage);

        callbackDL = new UrlRequestCallback() {
            @Override
            public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
                stopDl=System.currentTimeMillis();
                mode = MODE_ENDED;
                super.onSucceeded(request, info);
            }
            @Override
            public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
                //Log.d("SpeedtestWorkerQuic", "" + byteBuffer);
                //Log.d("SpeedtestWorkerQuic", "" + info.getReceivedByteCount());
                receivedByteCount = info.getReceivedByteCount();
                super.onReadCompleted(request, info, byteBuffer);
            }
            @Override
            public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
                Log.d("SpeedtestWorkerQuic", "onFailed: error: " + error);
                super.onFailed(request, info, error);
            }
            @Override
            public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
                mode = MODE_BEGIN;
                dlUpdate();
                super.onResponseStarted(request, info);
            }
        };
        callbackUL = new UrlRequestCallback() {
            @Override
            public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
                super.onSucceeded(request, info);
            }
            @Override
            public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
                super.onReadCompleted(request, info, byteBuffer);
            }
            @Override
            public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
                Log.d("SpeedtestWorkerQuic", "onFailed: error: " + error);
                super.onFailed(request, info, error);
            }
            @Override
            public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
                super.onResponseStarted(request, info);
            }
        };
        uploadProvider = new ULDataProvider(chunks, garbage) {
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
                Log.d("MyUploadDataProvider","READ IS CALLED. index = " + index);
                if (index>=chunks) {
                    index = 0;
                    stopUl = System.currentTimeMillis();
                    uploadDataSink.onReadSucceeded(true);
                } else {
                    byteBuffer.put(garbage, BUFFER_SIZE * index, BUFFER_SIZE);
                    index++;
                    Utils.sleep(200);
                    uploadDataSink.onReadSucceeded(false);
                }
                ulUpdate(index);
            }
        };
        start();
    }

    private void ulUpdate(int index){

        speedUl = 8*((double)index*(double)BUFFER_SIZE) / (double)((System.currentTimeMillis() - startUl - 200*index)*1000);
        progressUl = (double) index / 20;
        //ul = (speed * 8 * config.getOverheadCompensationFactor()) / (config.getUseMebibits() ? 1048576.0 : 1000000.0);
        onUploadUpdate(speedUl, progressUl);
        Log.e("ulUpdate", ": " + index + "/" + chunks + " : " + (index*BUFFER_SIZE) + "/" + chunks*BUFFER_SIZE + ", speed = " + speedUl + ", progress = " + progressUl + ", Time = " + (System.currentTimeMillis() - startUl));
        if (index>=chunks - 1) {
            stopDl = System.currentTimeMillis();
            speedUl = 8*((double)chunks*(double)BUFFER_SIZE) / ((System.currentTimeMillis() - startUl -200*index)*1000);
            Log.e("ulUpdate", "done. Time = " + (System.currentTimeMillis() - startUl) + ", speed = " + speedUl);
            onUploadUpdate(speedUl, 1);
        }
    };
    private void dlUpdate(){
        startDl = System.currentTimeMillis();
        receivedByteCount = 0;
        new Thread(new Runnable() {
            public void run() {
                for(;;) {
                    double t=System.currentTimeMillis()-startDl;
                    if (mode == MODE_BEGIN) {
                        Log.d("dlTest: ","MODE_BEGIN");
                        onDownloadUpdate(0, 0);
                        mode = MODE_IN_PROGRESS;
                        startDl = startDl + 100;
                    } else if (mode == MODE_IN_PROGRESS) {
                        double speed = (receivedByteCount) / ((t<100?100:t) / 1000.0);
                        if (config.getTime_auto()) {
                            double b = (2.5 * speed) / 100000.0;
                            bonusTDL += b > 200 ? 200 : b;
                        }
                        double progress = (t + bonusTDL) / (double) (config.getTime_dl_max() * 1000);
                        speed = (speed * 8 * config.getOverheadCompensationFactor()) / (config.getUseMebibits() ? 1048576.0 : 1000000.0);
                        dl = speed;
                        //onDownloadUpdate(dl,0.0001*(System.currentTimeMillis()-start));
                        onDownloadUpdate(dl, progress>1?1:progress);
                        Log.d("dlTest: ","MODE_IN_PROGRESS progress = " +  0.00016*(System.currentTimeMillis()-startDl));
                    } else if (mode == MODE_ENDED) {
                        Log.d("dlTest: ","MODE_ENDED");
                        stopDl = System.currentTimeMillis();
                        onDownloadUpdate(dl,1);
                        Log.d("dlTest: ","Download: "+ "TO-DO"+ " (took "+(System.currentTimeMillis()-startDl)+"ms)");
                        break;
                    }
                    Utils.sleep(100);
                }
            }
        }).start();
    }

    private void pingUpdate() {
        nrOfPings++;
        totPing += (System.currentTimeMillis() - startPing - ((nrOfPings-1)*1000) - 200);
        Log.d("SpeedTestWorkerQuic", "Ping done. ping = " + (System.currentTimeMillis() - startPing - ((nrOfPings-1)*1000)) + ", nrOfPings =" + nrOfPings);
    }

    private boolean getIPCalled=false;
    private void getIP(){

    }

    private boolean dlCalled=false;
    private void dlTest(){
        Log.d("SpeedTestWorkerQuic", "Quic dlTest");
        Log.d("SpeedTestWorkerQuic", "Quic dlTest. url: " + backend.getServer() + backend.getDlURL());
        if(dlCalled) return; else dlCalled=true;
        UrlRequest.Builder requestBuilder = engine.newUrlRequestBuilder(backend.getServer() + backend.getDlURL(), callbackDL, executor);
        UrlRequest request = requestBuilder.build();
        request.start();
    }

    private boolean ulCalled=false;
    private void ulTest(){
        Log.d("SpeedTestWorkerQuic", "Quic ulTest");
        Log.d("SpeedTestWorkerQuic", "Quic ulTest. url: " + backend.getServer() + backend.getUlURL());
        if(ulCalled) return; else ulCalled=true;
        onUploadUpdate(0,1);
        UrlRequest.Builder requestBuilder = engine.newUrlRequestBuilder(backend.getServer() + backend.getUlURL(), callbackUL, executor);
        requestBuilder.addHeader("Content-Type","application/json; charset=UTF-8");
        requestBuilder.setHttpMethod("POST");
        requestBuilder.setUploadDataProvider(uploadProvider,executor);
        UrlRequest request = requestBuilder.build();
        startUl=System.currentTimeMillis();
        request.start();
    }

    private boolean pingCalled=false;
    private void pingTest(){
        Log.d("SpeedTestWorkerQuic", "Quic pingTest");
        Log.d("SpeedTestWorkerQuic", "Quic pingTest. url: " + backend.getServer() + backend.getPingURL());
        new Thread(new Runnable() {
            public void run() {
                totalJitter = 0;
                jitter = 0;
                pingArray = new long[6];
                if (pingCalled) return;
                else pingCalled = true;
                for (nrOfPings = 0; nrOfPings < 6; ) {
                    pingerQuic = new PingerQuic(engine, backend.getServer() + backend.getPingURL()) {
                        @Override
                        public void onPong(long ms) {
                            if (nrOfPings != 0) {
                                Log.e("SpeedTestWorkerQuic", "Quic pingTest. nrOfPings = " + nrOfPings);
                                totPing += ms;
                                pingArray[nrOfPings] = ms;
                                Log.e("SpeedTestWorkerQuic", "Quic pingTest. onPong. ms = " + ms);
                                onPingJitterUpdate(ms, 0, 1);
                            }
                            if (nrOfPings >= 5) {
                                totalJitter = 0;
                                for (int i = 1; i < 6; i++) {
                                    if (i != 0) {
                                        totalJitter += Math.abs(pingArray[i] - pingArray[i - 1]);
                                    }
                                };

                                jitter = (double) totalJitter / 4;
                                onPingJitterUpdate(totPing / 5, jitter, 1);
                                Log.e("SpeedTestWorkerQuic", "Quic pingTest done");
                            }
                            nrOfPings++;
                        }
                    };
                    Utils.sleep(1000);
                }
            }
        }).start();;
    }
/*
    private void sendTelemetry(){
        if(telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) return;
        if(stopASAP&&telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_BASIC)) return;
        try{
            ConnectionQuic c=new ConnectionQuic(telemetryConfig.getServer(),-1,-1,-1,-1);
            Telemetry t=new Telemetry(c,telemetryConfig.getPath(),telemetryConfig.getTelemetryLevel(),ipIsp,config.getTelemetry_extra(),dl==-1?"":String.format(Locale.ENGLISH,"%.2f",dl),ul==-1?"":String.format(Locale.ENGLISH,"%.2f",ul),ping==-1?"":String.format(Locale.ENGLISH,"%.2f",ping),jitter==-1?"":String.format(Locale.ENGLISH,"%.2f",jitter),log.getLog()) {
                @Override
                public void onDataReceived(String data) {
                    if(data.startsWith("id")){
                        onTestIDReceived(data.split(" ")[1]);
                    }
                }

                @Override
                public void onError(String err) {
                    System.err.println("Telemetry error: "+ err);
                }
            };
            t.join();
        }catch (Throwable t){
            System.err.println("Failed to send telemetry: "+ t.toString());
            t.printStackTrace(System.err);
        }
    }
*/

    public void abort(){
        if(stopASAP) return;
        log.l("Manually aborted");
        stopASAP=true;
    }

    public abstract void onDownloadUpdate(double dl, double progress);
    public abstract void onUploadUpdate(double ul, double progress);
    public abstract void onPingJitterUpdate(double ping, double jitter, double progress);
    public abstract void onIPInfoUpdate(String ipInfo);
    public abstract void onTestIDReceived(String id);
    public abstract void onEnd();

    public abstract void onCriticalFailure(String err);

}
