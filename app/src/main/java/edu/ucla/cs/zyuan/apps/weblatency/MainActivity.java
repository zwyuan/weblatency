package edu.ucla.cs.zyuan.apps.weblatency;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final String LOG_TAG = "WebLatency";
    private final String logPath = "weblatency";
    private final String logFileNameBase = "weblog_";

    private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 338;
    private final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 337;

    final protected String json_timing_cmd = "JSON.stringify(window.performance.timing)"; // get Performance Timing
    final protected String json_paint_cmd = "JSON.stringify(window.performance.getEntriesByName('first-contentful-paint'))"; // get First Contentful Painting
    final private Boolean use_paint_cmd = true;

    private final String logFilePath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/" + logPath;
//    Date date = new Date(location.getTime());
//    DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
//    String a = DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date());

    private File logFile = null;
    private OutputStream os;

    protected String info = "";
    protected String defaultUrl = "http://web.cs.ucla.edu/~zyuan/test.html";
    private WebView mWebView;
    private TextView latencyTextView;
    private TextView mCellInfoTextView;
    private boolean mAutoReload = true;

    private Timer mTimer;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;

    private JSONObject timings;

    private int rc = -1;

    private String jsData = "";
    private String carrierName = "";
    private int dataNetworkType = 0;

    // First Contentful Paintings
    private String jsonEntryName = "";
    private String jsonEntryType = "";
    private double jsonEntryStartTime = -1;
    private double jsonEntryDuration = -1;

    // Navigation Timing Data
    private int navigationStart = -1;
    private int unloadEventStart = -1;
    private int unloadEventEnd = -1;
    private int redirectStart = -1;
    private int redirectEnd = -1;
    private int fetchStart = -1;
    private int domainLookupStart = -1;
    private int domainLookupEnd = -1;
    private int connectStart = -1;
    private int connectEnd = -1;
    private int secureConnectionStart = -1;
    private int requestStart = -1;
    private int responseStart = -1;
    private int responseEnd = -1;
    private int domLoading = -1;
    private int domInteractive = -1;
    private int domContentLoadedEventStart = -1;
    private int domContentLoadedEventEnd = -1;
    private int domComplete = -1;
    private int loadEventStart = -1;
    private int loadEventEnd = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(false);
        mTelephonyManager = (TelephonyManager) this.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        logFile = new File(logFilePath, logFileNameBase + getLogMetadata() + ".txt");

        final EditText userUrl = (EditText) findViewById(R.id.url);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

//        @TargetApi(24)
//        int dataNetworkType = mTelephonyManager.getDataNetworkType();

        if (!isExternalStorageWritable() || !isExternalStorageReadable()) {
            Log.w(LOG_TAG, "External storage is not R/W!");
        }

        mWebView = (WebView) findViewById(R.id.my_webview);
        mWebView.getSettings().setJavaScriptEnabled(true); // enable javascript

        latencyTextView = (TextView) findViewById(R.id.latency_textview);
        latencyTextView.setText(R.string.load_latency);

        final Activity activity = this;

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(activity, error.toString(), Toast.LENGTH_SHORT).show();
            }

        });

//        mWebView.loadUrl(userURL);

        final Button mGoButton = (Button) findViewById(R.id.goBtn);
        mGoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.i(LOG_TAG, "userUrl.getText() is: " + userUrl.getText().toString());
                String userURL = buildUrlString(userUrl.getText().toString());
                Log.i(LOG_TAG, "URL I got is: " + userURL);

                if (URLUtil.isHttpUrl(userURL) || URLUtil.isHttpsUrl(userURL)) {
                    mWebView.loadUrl(userURL);
                } else {
                    mWebView.loadUrl(defaultUrl);
                }
//                Log.i(LOG_TAG, "URL I got is: " + userURL);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle reload
                mWebView.reload();
                jsData = "";

                if (use_paint_cmd) {
                    getDataFromJs(json_paint_cmd, mWebView);
                } else {
                    getDataFromJs(json_timing_cmd, mWebView);
                }

                Snackbar.make(view, "Page successfully refreshed", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mTimer = null;

        ToggleButton auto_toggle_button = (ToggleButton) findViewById(R.id.auto_toggle_button);
        auto_toggle_button.setTextOff("Auto Refresh OFF");
        auto_toggle_button.setTextOn("Auto Refresh ON");
        auto_toggle_button.setChecked(false);

        Log.i(LOG_TAG, "Initializing toggle button");
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }
        }, 0, 30000);

        auto_toggle_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    Log.i(LOG_TAG, "Toggle button is checked, automatically refresh web page");

                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                    mAutoReload = true;

                    mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            TimerMethod();
                        }

                    }, 0, 30000);
                } else {
                    // The toggle is disabled
                    Log.i(LOG_TAG, "Toggle button is not checked, disable automatic refreshing");
                    mAutoReload = false;

                    if (mTimer != null) {
                        mTimer.cancel();
                    }
                }
            }
        });
//        getDataFromJs("JSON.stringify(window.performance.timing)", mWebView);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG, "Closing weblatency app, re-enable WiFi");
        mWifiManager.setWifiEnabled(true);
    }

    private void TimerMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        if (mAutoReload) {
            this.runOnUiThread(Timer_Tick);
        }
    }

    public String buildUrlString(String url) {
        if (url.isEmpty()) {
            return defaultUrl;
        } else {
            if (!url.startsWith("www.") && !url.startsWith("http://") && !url.startsWith("https://")){
                url = "www." + url;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")){
                url = "http://" + url;
            }
        }
        return url;
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.

            //Do something to the UI thread here
            mWebView.reload();
            Log.i(LOG_TAG, "Reloaded web view");
            jsData = "";

            if (use_paint_cmd) {
                getDataFromJs(json_paint_cmd, mWebView);
            } else {
                getDataFromJs(json_timing_cmd, mWebView);
            }
//            Snackbar.make((View) findViewById(R.id.activity_main), "Page successfully refreshed", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();

        }
    };

    private void getDataFromJs(String command, WebView webView) {
        // build anonymous function to execute javascript
        String js = String.format("(function() { return %s })();", command);

        webView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                jsData = s.replaceAll("^\"|\"$", "");
                jsData = jsData.replaceAll("\\\\", "");

                Log.d(LOG_TAG, "evaluateJavascript(), jsData = " + jsData.toString());

                if (use_paint_cmd) {
                    parseJsPaintTimingData(jsData);
                } else {
                    parseJsPerformanceTimingData(jsData);
                }

                if (!hasExternalStorageFile(logFile)) {
                    Log.w(LOG_TAG, "Creating " + logFile.toString());
                    createExternalStorageFile(logFilePath);
                }

                writeExternalStorageFile(logFile, jsData);
            }
        });
    }

    private String getLogMetadata() {
        String carrierName = mTelephonyManager.getNetworkOperatorName().replace(" ", "");;
        String timestamp = ((Long) (System.currentTimeMillis() / 1000)).toString();
        //String datetime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new java.util.Date());
        String datetime = DateFormat.format("yyyyMMdd_HHmmss", new java.util.Date()).toString();
        return datetime + "_" + timestamp + "_" + android.os.Build.MANUFACTURER.replace(" ", "")
                + "-" + android.os.Build.MODEL.replace(" ", "") +"_" + carrierName;
    }

    private void parseJsPaintTimingData(String jsString) {

        jsString = jsString.replaceAll("\\[", "").replaceAll("\\]","");
        Log.d(LOG_TAG, "parseJsPaintTimingData(), jsString = " + jsString);

        try{
            timings = new JSONObject(jsString);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Wrong parsing JSON painting data: " + jsString);
        }

        if (timings != null) {
            try {
                jsonEntryName = timings.getString("name");
                jsonEntryType = timings.getString("entryType");
                jsonEntryStartTime = timings.getInt("startTime");
                jsonEntryDuration = timings.getInt("duration");

                StringBuilder latency = new StringBuilder();
                latency.append(String.format(Locale.US, "**["+ mTelephonyManager.getNetworkOperatorName().replaceAll("\\s","") + "], "));
                latency.append(String.format(Locale.US, "jsonEntryName: %s, ", jsonEntryName));
                latency.append(String.format(Locale.US, "entryType: %s, ", jsonEntryType));
                latency.append(String.format(Locale.US, "startTime: %.2f, ", jsonEntryStartTime));
                latency.append(String.format(Locale.US, "duration: %.2f ", jsonEntryDuration));

                latencyTextView.setText(latency.toString().replaceAll(", ", ",\n"));
                writeExternalStorageFile(logFile, latency.toString());
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Exception in parsing json object" + e);
            }
        }
    }

    private void parseJsPerformanceTimingData(String jsString) {
        Log.i(LOG_TAG, "parseJsPerformanceTimingData Received data: " + jsString);
        try{
            timings = new JSONObject(jsString);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Wrong parsing JSON data: " + jsString);
        }

        if (timings != null) {
            try {
                navigationStart = timings.getInt("navigationStart");
                unloadEventStart = timings.getInt("unloadEventStart");
                unloadEventEnd = timings.getInt("unloadEventEnd");
                redirectStart = timings.getInt("redirectStart");
                redirectEnd = timings.getInt("redirectEnd");
                fetchStart = timings.getInt("fetchStart");
                domainLookupStart = timings.getInt("domainLookupStart");
                domainLookupEnd = timings.getInt("domainLookupEnd");
                connectStart = timings.getInt("connectStart");
                connectEnd = timings.getInt("connectEnd");
                secureConnectionStart = timings.getInt("secureConnectionStart");
                requestStart = timings.getInt("requestStart");
                responseStart = timings.getInt("responseStart");
                responseEnd = timings.getInt("responseEnd");
                domLoading = timings.getInt("domLoading");
                domInteractive = timings.getInt("domInteractive");
                domContentLoadedEventStart = timings.getInt("domContentLoadedEventStart");
                domContentLoadedEventEnd = timings.getInt("domContentLoadedEventEnd");
                domComplete = timings.getInt("domComplete");
                loadEventStart = timings.getInt("loadEventStart");
                loadEventEnd = timings.getInt("loadEventEnd");

                calculateLatencies();
            } catch (JSONException e) {
                Log.w(LOG_TAG, "Exception in parsing json object" + e);
            }
        }
    }

    private void calculateLatencies() {
        StringBuilder latency = new StringBuilder();
        latency.append(String.format(Locale.US, "**["+ mTelephonyManager.getNetworkOperatorName() + "], "));
        latency.append(String.format(Locale.US, "pageLoadTime: %d, ", (loadEventStart - navigationStart)));
        latency.append(String.format(Locale.US, "pageRequestTime: %d, ", (loadEventStart - fetchStart)));
        latency.append(String.format(Locale.US, "dnsTime: %d, ", (domainLookupEnd - domainLookupStart)));
        latency.append(String.format(Locale.US, "tcpConnTime: %d, ", (connectEnd - connectStart)));
        latency.append(String.format(Locale.US, "httpRequestLatency: %d, ", (responseStart - requestStart)));
        latency.append(String.format(Locale.US, "httpConnTime: %d, ", (responseEnd - requestStart)));
        latency.append(String.format(Locale.US, "networkTime: %d, ", (responseEnd - domainLookupStart)));
        latency.append(String.format(Locale.US, "renderingTime: %d ", (domComplete - domLoading)));

        latencyTextView.setText(latency.toString().replaceAll(", ", "ms,\n"));
        writeExternalStorageFile(logFile, latency.toString());
    }

    public int showCellInfoStrings() {
        try {
            mTelephonyManager.listen(new PhoneStateListener(){
                @Override
                public void onCellInfoChanged(List<CellInfo> cellInfo) {
                    super.onCellInfoChanged(cellInfo);
                    for (CellInfo ci : cellInfo) {
                        if (ci instanceof CellInfoGsm) {
                            Log.d("TAG", "This has 2G");
                        } else if (ci instanceof CellInfoLte) {
                            Log.d("TAG", "This has 4G");
                        } else {
                            Log.d("TAG", "This has 3G");
                        }
                    }
                }

            }, PhoneStateListener.LISTEN_CELL_INFO);

            mCellInfoTextView.setText(info); // displaying the information in the textView
            return 0;
        } catch (SecurityException se) {
            info = "Sorry, don't have the permission to know";
            mCellInfoTextView.setText(info); // displaying the information in the textView
            return -1;
        }
    }

    public int showPhoneInfoStrings() {
        try {
            // Calling the methods of TelephonyManager the returns the information
            String IMEINumber = mTelephonyManager.getDeviceId();
            String subscriberID = mTelephonyManager.getDeviceId();
            String SIMSerialNumber = mTelephonyManager.getSimSerialNumber();
            String networkCountryISO = mTelephonyManager.getNetworkCountryIso();
            String SIMCountryISO = mTelephonyManager.getSimCountryIso();
            String softwareVersion = mTelephonyManager.getDeviceSoftwareVersion();
            String voiceMailNumber = mTelephonyManager.getVoiceMailNumber();

            // getting information if phone is in roaming
            boolean isRoaming = mTelephonyManager.isNetworkRoaming();

            // Get the phone type
            String phoneTypeStr = "";

            int phoneTypeInt = mTelephonyManager.getPhoneType();

            switch (phoneTypeInt) {
                case (TelephonyManager.PHONE_TYPE_CDMA):
                    phoneTypeStr = "CDMA";
                    break;
                case (TelephonyManager.PHONE_TYPE_GSM):
                    phoneTypeStr = "GSM";
                    break;
                case (TelephonyManager.PHONE_TYPE_NONE):
                    phoneTypeStr = "NONE";
                    break;
            }

            info = "Phone Details:\n";
            info += "\n IMEI Number: " + IMEINumber;
            info += "\n SubscriberID: " + subscriberID;
            info += "\n Sim Serial Number: " + SIMSerialNumber;
            info += "\n Network Country ISO: " + networkCountryISO;
            info += "\n SIM Country ISO: " + SIMCountryISO;
            info += "\n Software Version: " + softwareVersion;
            info += "\n Voice Mail Number: " + voiceMailNumber;
            info += "\n Phone Network Type: " + phoneTypeStr;
            info += "\n In Roaming? : " + isRoaming;

            mCellInfoTextView.setText(info); // displaying the information in the textView
            return 0;
        } catch (SecurityException se) {
            info = "Sorry, don't have the permission to know";
            mCellInfoTextView.setText(info); // displaying the information in the textView
            return -1;
        }
    }

    void createExternalStorageFile(String logFilePath) {
        File dir = new File(logFilePath);
        if (!dir.exists()) {
            Log.d(LOG_TAG, "Creating log file dir: " + dir);
            dir.mkdirs();
        }
    }

    void writeExternalStorageFile(File logFile, String data) {
        Log.d(LOG_TAG, "Received data: " + data);

        try {
            os = new FileOutputStream(logFile, true);
            os.write(data.getBytes());
            os.write("\n".getBytes());
            os.flush();
            os.close();
        } catch (IOException e) {
            // Unable to create file, likely because external storage is not currently mounted.
            Log.w("ExternalStorage", "Error writing " + os, e);
        }
    }

    void deleteExternalStorageFile(File file) {
        if (file != null) {
            file.delete();
        }
    }

    boolean hasExternalStorageFile(File file) {
        return file.exists() && file != null;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
