package edu.ucla.cs.zyuan.apps.weblatency;

import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.ViewDebug;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class CellStateListener extends PhoneStateListener {

    //--------------------------------------------------
    // Constants
    //--------------------------------------------------

    public static final String LOG_TAG = "WebLatency_CSL";

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    private final TextView mTextView;
    private List<CellInfo> mCellinfo;

    //--------------------------------------------------
    // Constructor
    //--------------------------------------------------

    public CellStateListener(TextView textView, List<CellInfo> cellInfo) {
        mTextView = textView;
        mCellinfo = cellInfo;
    }

    //--------------------------------------------------
    // Methods
    //--------------------------------------------------

    private String serviceStateToString(int serviceState) {
        switch (serviceState) {
            case ServiceState.STATE_IN_SERVICE:
                return "STATE_IN_SERVICE";
            case ServiceState.STATE_OUT_OF_SERVICE:
                return "STATE_OUT_OF_SERVICE";
            case ServiceState.STATE_EMERGENCY_ONLY:
                return "STATE_EMERGENCY_ONLY";
            case ServiceState.STATE_POWER_OFF:
                return "STATE_POWER_OFF";
            default:
                return "UNKNOWN_STATE";
        }
    }

    private String callStateToString(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                return "\nonCallStateChanged: CALL_STATE_IDLE, ";
            case TelephonyManager.CALL_STATE_RINGING:
                return "\nonCallStateChanged: CALL_STATE_RINGING, ";
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return "\nonCallStateChanged: CALL_STATE_OFFHOOK, ";
            default:
                return "\nUNKNOWN_STATE: " + state + ", ";
        }
    }

    //--------------------------------------------------
    // PhoneStateListener
    //--------------------------------------------------

    @Override
    public void onCellInfoChanged(List<CellInfo> cellInfo) {

        super.onCellInfoChanged(cellInfo);
        mCellinfo = cellInfo;
        Log.i(LOG_TAG, "CellStateListener-onCellInfoChanged: " + cellInfo);
        if (cellInfo == null) {

        } else {
            for (CellInfo c : cellInfo) {
                if (c instanceof CellInfoLte) {
                    // cast to CellInfoLte and call all the CellInfoLte methods you need
                    // Gets the LTE PCI: (returns Physical Cell Id 0..503, Integer.MAX_VALUE if unknown)
                    int cellPci = ((CellInfoLte) c).getCellIdentity().getPci();
                    Log.i(LOG_TAG, "onCellInfo - cellPci = " + cellPci);
                    mTextView.setText("onCellInfoChanged - cellPci = " + Integer.toString(cellPci));

                }
            }
            Log.i(LOG_TAG, "onCellInfoChanged: " + cellInfo);
        }
    }

    @Override
    public void onDataActivity(int direction) {
        super.onDataActivity(direction);
        switch (direction) {
            case TelephonyManager.DATA_ACTIVITY_NONE:
                Log.i(LOG_TAG, "onDataActivity: DATA_ACTIVITY_NONE");
                break;
            case TelephonyManager.DATA_ACTIVITY_IN:
                Log.i(LOG_TAG, "onDataActivity: DATA_ACTIVITY_IN");
                break;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                Log.i(LOG_TAG, "onDataActivity: DATA_ACTIVITY_OUT");
                break;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                Log.i(LOG_TAG, "onDataActivity: DATA_ACTIVITY_INOUT");
                break;
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                Log.i(LOG_TAG, "onDataActivity: DATA_ACTIVITY_DORMANT");
                break;
            default:
                Log.w(LOG_TAG, "onDataActivity: UNKNOWN " + direction);
                break;
        }
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        String message = "onServiceStateChanged: " + serviceState + "\n";
        message += "onServiceStateChanged: getOperatorAlphaLong " + serviceState.getOperatorAlphaLong() + "\n";
        message += "onServiceStateChanged: getOperatorAlphaShort " + serviceState.getOperatorAlphaShort() + "\n";
        message += "onServiceStateChanged: getOperatorNumeric " + serviceState.getOperatorNumeric() + "\n";
        message += "onServiceStateChanged: getIsManualSelection " + serviceState.getIsManualSelection() + "\n";
        message += "onServiceStateChanged: getRoaming " + serviceState.getRoaming() + "\n";
        message += "onServiceStateChanged: " + serviceStateToString(serviceState.getState());
        Log.i(LOG_TAG, message);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        callStateToString(state);
        String message = callStateToString(state) + "incomingNumber: " + incomingNumber;
//        mTextView.setText(message);
    }

    @Override
    public void onCellLocationChanged(CellLocation location) {
        super.onCellLocationChanged(location);
        String message = "";
        if (location instanceof GsmCellLocation) {
            GsmCellLocation gcLoc = (GsmCellLocation) location;
            message += "onCellLocationChanged: GsmCellLocation " + gcLoc + "\n";
            message += "onCellLocationChanged: GsmCellLocation getCid " + gcLoc.getCid() + "\n";
            message += "onCellLocationChanged: GsmCellLocation getLac " + gcLoc.getLac() + "\n";
            message += "onCellLocationChanged: GsmCellLocation getPsc" + gcLoc.getPsc(); // Requires min API 9
            Log.i(LOG_TAG, message);
//            mTextView.setText(String.format(Locale.US, "onCellLocationChanged (GSM):\n %d-%d-%d", gcLoc.getCid(), gcLoc.getLac(), gcLoc.getPsc()));
        } else if (location instanceof CdmaCellLocation) {
            CdmaCellLocation ccLoc = (CdmaCellLocation) location;
            message += "onCellLocationChanged: CdmaCellLocation " + ccLoc + "\n";;
            message += "onCellLocationChanged: CdmaCellLocation getBaseStationId " + ccLoc.getBaseStationId() + "\n";;
            message += "onCellLocationChanged: CdmaCellLocation getBaseStationLatitude " + ccLoc.getBaseStationLatitude() + "\n";;
            message += "onCellLocationChanged: CdmaCellLocation getBaseStationLongitude" + ccLoc.getBaseStationLongitude() + "\n";;
            message += "onCellLocationChanged: CdmaCellLocation getNetworkId " + ccLoc.getNetworkId() + "\n";;
            message += "onCellLocationChanged: CdmaCellLocation getSystemId " + ccLoc.getSystemId();
            Log.i(LOG_TAG, message);
//            mTextView.setText(String.format(Locale.US, "onCellLocationChanged (CDMA):\n %d-%d", ccLoc.getBaseStationId(), ccLoc.getNetworkId()));
        } else {
            Log.i(LOG_TAG, "onCellLocationChanged: " + location);
        }
    }

    @Override
    public void onCallForwardingIndicatorChanged(boolean changed) {
        super.onCallForwardingIndicatorChanged(changed);
    }

    @Override
    public void onMessageWaitingIndicatorChanged(boolean changed) {
        super.onMessageWaitingIndicatorChanged(changed);
    }
}
