package edu.ucla.cs.zyuan.apps.weblatency;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Created by zyuan on 3/22/18.
 */

public class CheckCellIdTask extends AsyncTask<List<CellInfo>, Void, String> {

//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View view = (View) inflater.inflate(R.layout.content_main, null);
//        mCellInfoTextView = (TextView) view.findViewById(R.id.cellinfo_textview);
//    }
    private String message = "";
    private final String LOG_TAG = "WebLatency_Async";

    protected String doInBackground(List<CellInfo>... params) {
        List<CellInfo> curCellInfoList = params[0];
        //This method runs in the same thread as the UI.
        if (curCellInfoList == null) {
            Log.i(LOG_TAG, "curCellInfoList = Null");
        } else {
            Log.d(LOG_TAG, "curCellInfoList =\n" + curCellInfoList);
            for (CellInfo c : curCellInfoList) {
                if (c.isRegistered()) {
                    if (c instanceof CellInfoGsm) {
                        Log.d(LOG_TAG, "11GSM");
                        CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) c).getCellIdentity();
                        message = String.format(Locale.US, "Registered Gsm Cell: Cid-Lac = %d-%d", cellIdentityGsm.getCid(), cellIdentityGsm.getLac());
                    } else if (c instanceof CellInfoWcdma){
                        Log.d(LOG_TAG, "22WCDMA");
                        CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma) c).getCellIdentity();
                        message = String.format(Locale.US, "Registered Wcdma Cell: Cid-Lac = %d-%d", cellIdentityWcdma.getCid(), cellIdentityWcdma.getLac());
                    } else if (c instanceof CellInfoLte) {
                        Log.d(LOG_TAG, "33LTE");
                        CellIdentityLte cellIdentityLte = ((CellInfoLte) c).getCellIdentity();
                        // cast to CellInfoLte and call all the CellInfoLte methods you need
                        // Gets the LTE PCI: (returns Physical Cell Id 0..503, Integer.MAX_VALUE if unknown)
                        message = String.format(Locale.US, "Registered LTE Cell: Pci-Tac = %d-%d", cellIdentityLte.getPci(), cellIdentityLte.getTac());
                    }
                }
            }
        }
        return message;
    }

    protected void onProgressUpdate(Void... progress) {
//        setProgressPercent(progress[0]);
    }

    protected void onPostExecute(String result) {
//        super.onPostExecute(result);
    }
}
