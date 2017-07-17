package net.mobileinsight.milabreceiver;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.content.Intent;
import android.util.Log;

import edu.ucla.cs.zyuan.apps.weblatency.MainActivity;

import net.mobileinsight.milab.TaskObject;
import net.mobileinsight.milab.ITask;

/**
 * Created by Zengwen on 7/8/17.
 */

public class MainService extends Service {

    private final String LOG_TAG = "WebLatencyExp";
    private final String taskName = "WebLatencyExp";
    private final String taskDescription = "Collect LTE latency in accessing a webpage";
    private final String pluginName = "NetLoggerLatency";
    private final String relativeLogPath = "imc";
    //    private final String logFileName = "res.txt";
    private final String absoluteLogPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/" + relativeLogPath;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind");
        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy()");
        Log.w(LOG_TAG, "Closing weblatency app, re-enable WiFi");
        WifiManager mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(true);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private final ITask.Stub mBinder = new ITask.Stub() {
        @Override
        public int getPid() throws RemoteException {
            return Process.myPid();
        }

        @Override
        public TaskObject getOutput() throws RemoteException {
            TaskObject toSend = new TaskObject();
            toSend.setTaskName(taskName);
            toSend.setTaskDescription(taskDescription);
            toSend.setPathOutputFolder(absoluteLogPath);
            toSend.setPluginNameMI(pluginName);
            return toSend;
        }

        @Override
        public void exit() throws RemoteException {
            Log.i(LOG_TAG, "exit");
            stopSelf();
        }

        @Override
        public void basicTypes(
            int anInt, long aLong, boolean aBoolean,
            float aFloat, double aDouble,String aString) throws RemoteException {

        }
    };
}
