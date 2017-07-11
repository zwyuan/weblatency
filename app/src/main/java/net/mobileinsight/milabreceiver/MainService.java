package net.mobileinsight.milabreceiver;

import android.app.Service;
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

    private final String MiLogPath = "/sdcard/imc";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MILabCallSender", "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("MILabCallSender", "onBind");
        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i("MILabTask", "onDestroy()");
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
            toSend.setTaskName("Call Sender");
            toSend.setTaskDescription("A call sender");
            toSend.setPathOutputFolder(MiLogPath);
            toSend.setPluginNameMI("NetLoggerCFG");
            return toSend;
        }

        @Override
        public void exit() throws RemoteException {
            Log.i("MainService", "exit");
            stopSelf();
        }

        @Override
        public void basicTypes(
                int anInt, long aLong, boolean aBoolean,
                float aFloat, double aDouble,String aString) throws RemoteException {

        }
    };
}
