package edu.ucla.cs.zyuan.apps.weblatency;

/**
 * Created by zyuan on 3/22/18.
 */

import java.util.Locale;

public class CellData {

    public enum Type {
        GSM,
        CDMA,
        WCDMA,
        LTE
    }

    public Type type;
    public int dbm = 0;
    public String code = "";
    public Double latitude;
    public Double longitude;

    public CellData(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        String commonInfo = String.format(Locale.US, "[%s] %s (%d dbm)", type.toString(), code, dbm);
        String position = latitude != null && longitude != null
                ? String.format(Locale.US, "lat: %.06f, long: %.06f", latitude, longitude)
                : "";
        return String.format("%s%s", commonInfo, position);
    }
}
