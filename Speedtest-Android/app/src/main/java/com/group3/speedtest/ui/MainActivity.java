package com.group3.speedtest.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.group3.speedtest.core.SpeedTestQuic;
import com.group3.speedtest.core.Speedtest;
import com.group3.speedtest.core.config.SpeedtestConfig;
import com.group3.speedtest.core.config.TelemetryConfig;
import com.group3.speedtest.core.congestionControlSelector.CCA;
import com.group3.speedtest.core.serverSelector.TestPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

import com.caddy.speedtest.R;
import com.group3.speedtest.core.transportProtocolSelector.TP;

public class MainActivity extends Activity {
    final Activity main = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        transition(R.id.page_splash,0);
        new Thread(){
            public void run(){
                try{sleep(1500);}catch (Throwable t){}
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    final ImageView v = (ImageView) findViewById(R.id.testBackground);
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options);
                    int ih = options.outHeight, iw = options.outWidth;
                    if(4*ih*iw>16*1024*1024) throw new Exception("Too big");
                    options.inJustDecodeBounds = false;
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int vh = displayMetrics.heightPixels, vw = displayMetrics.widthPixels;
                    double desired=Math.max(vw,vh) * 0.7;
                    double scale=desired/Math.max(iw,ih);
                    final Bitmap b = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options),(int)(iw*scale), (int)(ih*scale), true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            v.setImageBitmap(b);
                        }
                    });
                }catch (Throwable t){
                    System.err.println("Failed to load testbackground ("+t.getMessage()+")");
                }

                page_init(main);
            }
        }.start();
    }

    private static Speedtest st=null;
    private static SpeedTestQuic stQuic=null;

    private void page_init(final Activity main){
        new Thread(){
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        transition(R.id.page_init,TRANSITION_LENGTH);
                    }
                });
                final TextView t=((TextView)findViewById(R.id.init_text));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        t.setText(R.string.init_init);
                    }
                });
                SpeedtestConfig config=null;
                TelemetryConfig telemetryConfig=null;
                TestPoint[] servers=null;
                TP[] transportProtocols=null;
                CCA[] congestionControls=null;
                String[] pickerVals=null;
                try{
                    String c=readFileFromAssets("SpeedtestConfig.json");
                    JSONObject o=new JSONObject(c);
                    config=new SpeedtestConfig(o);
                    c=readFileFromAssets("TelemetryConfig.json");
                    o=new JSONObject(c);
                    telemetryConfig=new TelemetryConfig(o);
                    if(telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideView(R.id.privacy_open);
                            }
                        });
                    }
                    if(st!=null){
                        try{st.abort();}catch (Throwable e){}
                    }
                    st=new Speedtest();
                    stQuic=new SpeedTestQuic(main);
                    st.setSpeedtestConfig(config);
                    st.setTelemetryConfig(telemetryConfig);
                    c=readFileFromAssets("ServerList.json");
                    if(c.startsWith("\"")||c.startsWith("'")){ //fetch server list from URL
                        if(!st.loadServerList(c.subSequence(1,c.length()-1).toString())){
                            throw new Exception("Failed to load server list");
                        }
                    }else{ //use provided server list
                        JSONArray a=new JSONArray(c);
                        if(a.length()==0) throw new Exception("No test points");
                        ArrayList<TestPoint> s=new ArrayList<>();
                        for(int i=0;i<a.length();i++) s.add(new TestPoint(a.getJSONObject(i)));
                        servers=s.toArray(new TestPoint[0]);
                        st.addTestPoints(servers);
                    }

                    /* Modified to add transport protocol selection */
                    c=readFileFromAssets("TransportProtocolList.json");
                    JSONArray b=new JSONArray(c);
                    ArrayList<TP> s=new ArrayList<>();
                    for(int i=0;i<b.length();i++) s.add(new TP(b.getJSONObject(i)));
                    transportProtocols=s.toArray(new TP[0]);
                    st.addTransportProtocols(transportProtocols);
                    /* Modified to add transport protocol selection */

                    /* Modified to add congestion control selection */
                    c=readFileFromAssets("CongestionList.json");
                    JSONArray d=new JSONArray(c);
                    ArrayList<CCA> u=new ArrayList<>();
                    for(int i=0;i<d.length();i++) u.add(new CCA(d.getJSONObject(i)));
                    congestionControls=u.toArray(new CCA[0]);
                    st.addCongestionControlAlgorithms(congestionControls);
                    /* Modified to add congestion control selection */

                    final String testOrder=config.getTest_order();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!testOrder.contains("D")){
                                hideView(R.id.dlArea);
                            }
                            if(!testOrder.contains("U")){
                                hideView(R.id.ulArea);
                            }
                            if(!testOrder.contains("P")){
                                hideView(R.id.pingArea);
                            }
                            if(!testOrder.contains("I")){
                                hideView(R.id.ipInfo);
                            }
                        }
                    });
                }catch (final Throwable e){
                    System.err.println(e);
                    st=null;
                    stQuic=null;
                    transition(R.id.page_fail,TRANSITION_LENGTH);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.fail_text)).setText(getString(R.string.initFail_configError)+": "+e.getMessage());
                            final Button b=(Button)findViewById(R.id.fail_button);
                            b.setText(R.string.initFail_retry);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init(main);
                                    b.setOnClickListener(null);
                                }
                            });
                        }
                    });
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        t.setText(R.string.init_selecting);
                    }
                });
                st.selectServer(new Speedtest.ServerSelectedHandler() {
                    @Override
                    public void onServerSelected(final TestPoint server) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(server==null){
                                    transition(R.id.page_fail,TRANSITION_LENGTH);
                                    ((TextView)findViewById(R.id.fail_text)).setText(getString(R.string.initFail_noServers));
                                    final Button b=(Button)findViewById(R.id.fail_button);
                                    b.setText(R.string.initFail_retry);
                                    b.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            page_init(main);
                                            b.setOnClickListener(null);
                                        }
                                    });
                                }else{
                                    page_serverSelect(server,st.getTestPoints(), st.getTransportProtocols(), st.getCongestionControlAlgorithms());
                                }
                            }
                        });
                    }
                });
            }
        }.start();
    }

    private void page_serverSelect(TestPoint selected, TestPoint[] servers, TP[] transportProtocols, CCA[] congestionControlAlgorithms){
        transition(R.id.page_serverSelect,TRANSITION_LENGTH);
        reinitOnResume=true;

        /* Server selection */

        final ArrayList<TestPoint> availableServers=new ArrayList<>();
        for(TestPoint t:servers) {
            //if (t.getPing() != -1)
            availableServers.add(t);
        }
        int selectedServerId=availableServers.indexOf(selected);
        final Spinner spinnerServer=(Spinner)findViewById(R.id.serverList);
        ArrayList<String> options=new ArrayList<String>();
        for(TestPoint t:availableServers){
            options.add(t.getName());
        }
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options.toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerServer.setAdapter(adapter);
        spinnerServer.setSelection(selectedServerId);

        /* TransportProtocol selection */

        final ArrayList<TP> availableTPs=new ArrayList<>();
        for(TP u:transportProtocols) {
            availableTPs.add(u);
        }
        int selectedTPId=availableTPs.indexOf(selected);
        final Spinner spinnerTransportProtocol=(Spinner)findViewById(R.id.transportProtocolList);
        final Spinner spinnerCCA=(Spinner)findViewById(R.id.congestionControl);

        ArrayList<String> optionsTransportProtocol=new ArrayList<String>();
        for(TP u:availableTPs){
            optionsTransportProtocol.add(u.getName());
        }
        final ArrayAdapter<String> adapterTP=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,optionsTransportProtocol.toArray(new String[0]));
        adapterTP.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransportProtocol.setEnabled(false);
        spinnerTransportProtocol.setClickable(false);
        spinnerTransportProtocol.setAdapter(adapterTP);
        spinnerTransportProtocol.setSelection(selectedTPId);

        /* Congestion Control selection */

        final ArrayAdapter<String> adapterCCA=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,st.getOptionsCCA());
        final ArrayList<CCA> availableCCAs=new ArrayList<>();
        for(CCA u:congestionControlAlgorithms) {
            availableCCAs.add(u);
        }
        int selectedCCAId=availableCCAs.indexOf(selected);

        adapterCCA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCCA.setAdapter(adapterCCA);
        spinnerCCA.setSelection(selectedCCAId);

        /* Parallel connections selection */
        final String[] pickerVals;
        final NumberPicker pCPicker;
        st.setParallelConnections(1);
        pCPicker = findViewById(R.id.parallelConnectionsPicker);
        pCPicker.setMaxValue(12);
        pCPicker.setMinValue(1);
        pickerVals  = new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
        pCPicker.setDisplayedValues(pickerVals);


        spinnerServer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (availableServers.get(position).getTP().equals("QUIC")) {
                    Log.d("onItemSelected", "selected: " + st.getSelectedServer().getTP());
                    spinnerTransportProtocol.setSelection(0);
                    pCPicker.setEnabled(false);
                    pCPicker.setValue(1);
                } else if (availableServers.get(position).getTP().equals("TCP")) {
                    Log.d("onItemSelected", "selected: " + st.getSelectedServer().getTP());
                    spinnerTransportProtocol.setSelection(1);
                    pCPicker.setEnabled(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // TODO Auto-generated method stub
            }
        });

        spinnerTransportProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                int position, long id) {
                st.setSelectedTP(st.getTransportProtocols()[position]);
                Toast.makeText(MainActivity.this, st.getTransportProtocols()[position].getName(), Toast.LENGTH_SHORT).show();
                if (st.getTransportProtocols()[position].getName().equals("UDP")) {
                    //st.setOptionsCCA(null);
                }
                spinnerCCA.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,st.getOptionsCCA()));
                adapterCCA.notifyDataSetChanged();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // TODO Auto-generated method stub
            }
        });
        /*Modified - Spinner for selection of Transport protocol */

        /*Modified - Spinner for selection of Congestion Control Algorithm */

        pCPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                int valuePicker = pCPicker.getValue();
                st.setParallelConnections(valuePicker);
            }
        });
        /*Modified - Spinner for selection of Parallel Connections */

        final Button b=(Button)findViewById(R.id.start);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reinitOnResume=false;
                if (st.getSelectedTP().getName().equals("QUIC")) {
                    Log.d("QUIC", "QUIC __________________________________________________________________________________________");
                    page_test(availableServers.get(spinnerServer.getSelectedItemPosition()), availableTPs.get(spinnerTransportProtocol.getSelectedItemPosition()), availableCCAs.get(spinnerCCA.getSelectedItemPosition()));
                } else {
                    Log.d("TCP", "TCP __________________________________________________________________________________________");
                    page_test(availableServers.get(spinnerServer.getSelectedItemPosition()), availableTPs.get(spinnerTransportProtocol.getSelectedItemPosition()), availableCCAs.get(spinnerCCA.getSelectedItemPosition()));
                }
                //b.setOnClickListener(null);
            }
        });
        TextView t=(TextView)findViewById(R.id.privacy_open);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                page_privacy();
            }
        });
    }

    private void page_privacy(){
        transition(R.id.page_privacy,TRANSITION_LENGTH);
        reinitOnResume=false;
        ((WebView)findViewById(R.id.privacy_policy)).loadUrl(getString(R.string.privacy_policy));
        TextView t=(TextView)findViewById(R.id.privacy_close);
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transition(R.id.page_serverSelect,TRANSITION_LENGTH);
                reinitOnResume=true;
            }
        });
    }

    private void page_test(final TestPoint selectedServer, final TP selectedTP, final CCA selectedCCA){
        transition(R.id.page_test,TRANSITION_LENGTH);
        st.setSelectedServer(selectedServer);
        ((TextView)findViewById(R.id.serverName)).setText(
            selectedTP.getName().equals("QUIC") ?
                selectedServer.getName() + "\n\n" +
                selectedTP.getName() + "\n\n" +
                selectedCCA.getName() + "\n\nParallel: " + "Quic Multiplex"
            :
                selectedServer.getName() + "\n\n" +
                selectedTP.getName() + "\n\n" +
                selectedCCA.getName() + "\n\n Parallel: " + st.getParallelConnections()
        );
        ((TextView)findViewById(R.id.dlText)).setText(format(0));
        ((TextView)findViewById(R.id.ulText)).setText(format(0));
        ((TextView)findViewById(R.id.pingText)).setText(format(0));
        ((TextView)findViewById(R.id.jitterText)).setText(format(0));
        ((ProgressBar)findViewById(R.id.dlProgress)).setProgress(0);
        ((ProgressBar)findViewById(R.id.ulProgress)).setProgress(0);
        ((GaugeView)findViewById(R.id.dlGauge)).setValue(0);
        ((GaugeView)findViewById(R.id.ulGauge)).setValue(0);
        ((TextView)findViewById(R.id.ipInfo)).setText("");
        ((ImageView)findViewById(R.id.logo_inapp)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url=getString(R.string.logo_inapp_link);
                if(url.isEmpty()) return;
                Intent i=new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        final View endTestArea=findViewById(R.id.endTestArea);
        final int endTestAreaHeight=endTestArea.getHeight();
        ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
        p.height=0;
        endTestArea.setLayoutParams(p);
        findViewById(R.id.shareButton).setVisibility(View.GONE);
        if (selectedTP.getName().equals("QUIC")) {
            stQuic.setParallelConnections(st.getParallelConnections());
            stQuic.setSelectedServer(st.getSelectedServer());
            stQuic.setSelectedTP(st.getSelectedTP());
            //stQuic.setSpeedtestConfig(st.getSpeedTestConfig());
            stQuic.start(new SpeedTestQuic.SpeedtestHandler() {
                @Override
                public void onDownloadUpdate(final double dl, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.dlText)).setText(progress==0?"...": format(dl));
                            ((GaugeView)findViewById(R.id.dlGauge)).setValue(progress==0?0:mbpsToGauge(dl));
                            ((ProgressBar)findViewById(R.id.dlProgress)).setProgress((int)(100*progress));
                        }
                    });
                }

                @Override
                public void onUploadUpdate(final double ul, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.ulText)).setText(progress==0?"...": format(ul));
                            ((GaugeView)findViewById(R.id.ulGauge)).setValue(progress==0?0:mbpsToGauge(ul));
                            ((ProgressBar)findViewById(R.id.ulProgress)).setProgress((int)(100*progress));
                        }
                    });

                }

                @Override
                public void onPingJitterUpdate(final double ping, final double jitter, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.pingText)).setText(progress==0?"...": format(ping));
                            ((TextView)findViewById(R.id.jitterText)).setText(progress==0?"...": format(jitter));
                        }
                    });
                }

                @Override
                public void onIPInfoUpdate(final String ipInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.ipInfo)).setText(ipInfo);
                        }
                    });
                }

                @Override
                public void onTestIDReceived(final String id, final String shareURL) {
                    if(shareURL==null||shareURL.isEmpty()||id==null||id.isEmpty()) return;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button shareButton=(Button)findViewById(R.id.shareButton);
                            shareButton.setVisibility(View.VISIBLE);
                            shareButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent share = new Intent(android.content.Intent.ACTION_SEND);
                                    share.setType("text/plain");
                                    share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                    share.putExtra(Intent.EXTRA_TEXT, shareURL);
                                    startActivity(Intent.createChooser(share, getString(R.string.test_share)));
                                }
                            });
                        }
                    });
                }

                @Override
                public void onEnd() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Button restartButton=(Button)findViewById(R.id.restartButton);
                            restartButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init(main);
                                    restartButton.setOnClickListener(null);
                                }
                            });
                        }
                    });
                    final long startT=System.currentTimeMillis(), endT=startT+TRANSITION_LENGTH;
                    new Thread(){
                        public void run(){
                            while(System.currentTimeMillis()<endT){
                                final double f=(double)(System.currentTimeMillis()-startT)/(double)(endT-startT);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
                                        p.height=(int)(endTestAreaHeight*f);
                                        endTestArea.setLayoutParams(p);
                                    }
                                });
                                try{sleep(10);}catch (Throwable t){}
                            }
                        }
                    }.start();
                }

                @Override
                public void onCriticalFailure(String err) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transition(R.id.page_fail,TRANSITION_LENGTH);
                            ((TextView)findViewById(R.id.fail_text)).setText(getString(R.string.testFail_err));
                            final Button b=(Button)findViewById(R.id.fail_button);
                            b.setText(R.string.testFail_retry);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init(main);
                                    b.setOnClickListener(null);
                                }
                            });
                        }
                    });
                }
            });
        } else {
            st.start(new Speedtest.SpeedtestHandler() {
                @Override
                public void onDownloadUpdate(final double dl, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.dlText)).setText(progress==0?"...": format(dl));
                            ((GaugeView)findViewById(R.id.dlGauge)).setValue(progress==0?0:mbpsToGauge(dl));
                            ((ProgressBar)findViewById(R.id.dlProgress)).setProgress((int)(100*progress));
                        }
                    });
                }

                @Override
                public void onUploadUpdate(final double ul, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.ulText)).setText(progress==0?"...": format(ul));
                            ((GaugeView)findViewById(R.id.ulGauge)).setValue(progress==0?0:mbpsToGauge(ul));
                            ((ProgressBar)findViewById(R.id.ulProgress)).setProgress((int)(100*progress));
                        }
                    });

                }

                @Override
                public void onPingJitterUpdate(final double ping, final double jitter, final double progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.pingText)).setText(progress==0?"...": format(ping));
                            ((TextView)findViewById(R.id.jitterText)).setText(progress==0?"...": format(jitter));
                        }
                    });
                }

                @Override
                public void onIPInfoUpdate(final String ipInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.ipInfo)).setText(ipInfo);
                        }
                    });
                }

                @Override
                public void onTestIDReceived(final String id, final String shareURL) {
                    if(shareURL==null||shareURL.isEmpty()||id==null||id.isEmpty()) return;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Button shareButton=(Button)findViewById(R.id.shareButton);
                            shareButton.setVisibility(View.VISIBLE);
                            shareButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent share = new Intent(android.content.Intent.ACTION_SEND);
                                    share.setType("text/plain");
                                    share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                    share.putExtra(Intent.EXTRA_TEXT, shareURL);
                                    startActivity(Intent.createChooser(share, getString(R.string.test_share)));
                                }
                            });
                        }
                    });
                }

                @Override
                public void onEnd() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Button restartButton=(Button)findViewById(R.id.restartButton);
                            restartButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init(main);
                                    restartButton.setOnClickListener(null);
                                }
                            });
                        }
                    });
                    final long startT=System.currentTimeMillis(), endT=startT+TRANSITION_LENGTH;
                    new Thread(){
                        public void run(){
                            while(System.currentTimeMillis()<endT){
                                final double f=(double)(System.currentTimeMillis()-startT)/(double)(endT-startT);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
                                        p.height=(int)(endTestAreaHeight*f);
                                        endTestArea.setLayoutParams(p);
                                    }
                                });
                                try{sleep(10);}catch (Throwable t){}
                            }
                        }
                    }.start();
                }

                @Override
                public void onCriticalFailure(String err) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transition(R.id.page_fail,TRANSITION_LENGTH);
                            ((TextView)findViewById(R.id.fail_text)).setText(getString(R.string.testFail_err));
                            final Button b=(Button)findViewById(R.id.fail_button);
                            b.setText(R.string.testFail_retry);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init(main);
                                    b.setOnClickListener(null);
                                }
                            });
                        }
                    });
                }
            });
        }

    }

    private String format(double d){
        Locale l=null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) {
            l = getResources().getConfiguration().getLocales().get(0);
        }else{
            l=getResources().getConfiguration().locale;
        }
        if(d<10) return String.format(l,"%.2f",d);
        if(d<100) return String.format(l,"%.1f",d);
        return ""+Math.round(d);
    }

    private int mbpsToGauge(double s){
        return (int)(1000*(1-(1/(Math.pow(1.3,Math.sqrt(s))))));
    }

    private String readFileFromAssets(String name) throws Exception{
        BufferedReader b=new BufferedReader(new InputStreamReader(getAssets().open(name)));
        String ret="";
        try{
            for(;;){
                String s=b.readLine();
                if(s==null) break;
                ret+=s;
            }
        }catch(EOFException e){}
        return ret;
    }

    private void hideView(int id){
        View v=findViewById(id);
        if(v!=null) v.setVisibility(View.GONE);
    }

    private boolean reinitOnResume=false;
    @Override
    protected void onResume() {
        super.onResume();
        if(reinitOnResume){
            reinitOnResume=false;
            page_init(main);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{st.abort();}catch (Throwable t){}
    }

    @Override
    public void onBackPressed() {
        if(currentPage==R.id.page_privacy)
            transition(R.id.page_serverSelect,TRANSITION_LENGTH);
        else super.onBackPressed();
    }

    //PAGE TRANSITION SYSTEM

    private int currentPage=-1;
    private boolean transitionBusy=false; //TODO: improve mutex
    private int TRANSITION_LENGTH=300;

    private void transition(final int page, final int duration){
        if(transitionBusy){
            new Thread(){
                public void run(){
                    try{sleep(10);}catch (Throwable t){}
                    transition(page,duration);
                }
            }.start();
        }else transitionBusy=true;
        if(page==currentPage) return;
        final ViewGroup oldPage=currentPage==-1?null:(ViewGroup)findViewById(currentPage),
                newPage=page==-1?null:(ViewGroup)findViewById(page);
        new Thread(){
            public void run(){
                long t=System.currentTimeMillis(), endT=t+duration;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(newPage!=null){
                            newPage.setAlpha(0);
                            newPage.setVisibility(View.VISIBLE);
                        }
                        if(oldPage!=null){
                            oldPage.setAlpha(1);
                        }
                    }
                });
                while(t<endT){
                    t=System.currentTimeMillis();
                    final float f=(float)(endT-t)/(float)duration;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(newPage!=null) newPage.setAlpha(1-f);
                            if(oldPage!=null) oldPage.setAlpha(f);
                        }
                    });
                    try{sleep(10);}catch (Throwable e){}
                }
                currentPage=page;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(oldPage!=null){
                            oldPage.setAlpha(0);
                            oldPage.setVisibility(View.INVISIBLE);
                        }
                        if(newPage!=null){
                            newPage.setAlpha(1);
                        }
                        transitionBusy=false;
                    }
                });
            }
        }.start();
    }

}
