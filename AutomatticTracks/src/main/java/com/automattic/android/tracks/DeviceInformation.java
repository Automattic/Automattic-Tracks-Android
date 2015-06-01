package com.automattic.android.tracks;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Some of the code below is from Mixpanel's SystemInformation class, but it's heavily modified to
 * adapt it to our needs. Mostly performance improvements.
 *
 * Original code available here:
 * https://github.com/mixpanel/mixpanel-android/blob/4c3405f33e483fd0d184e9cef524f6aee1f36a1f/src/main/java/com/mixpanel/android/mpmetrics/SystemInformation.java
 *
 */


/* package */ class DeviceInformation {
    public static final String LOGTAG = "NosaraDeviceInformation";

    private final Context mContext;

    // Unchanging facts
    private final Boolean mHasNFC;
    private final Boolean mHasTelephony;
    private final DisplayMetrics mDisplayMetrics;
    private final String mAppName;
    private final String mAppVersionName;
    private final Integer mAppVersionCode;
    private final String mDeviceLanguage;

    private final JSONObject mImmutableDeviceInfoJSON;

    public DeviceInformation(Context context) {
        mContext = context;

        final String mOs = "Android";
        final String mOSVersion = Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE;
        final String mManufacturer = Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER;
        final String mBrand = Build.BRAND == null ? "UNKNOWN" : Build.BRAND;
        final String mModel = Build.MODEL == null ? "UNKNOWN" : Build.MODEL;

        PackageManager packageManager = mContext.getPackageManager();

        String foundAppVersionName = null;
        Integer foundAppVersionCode = null;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            foundAppVersionName = packageInfo.versionName;
            foundAppVersionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            Log.w(LOGTAG, "System information constructed with a context that apparently doesn't exist.");
        }

        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(mContext.getApplicationInfo().packageName, 0);
        } catch (final NameNotFoundException e) {
            Log.w(LOGTAG, "System information constructed with a context that apparently doesn't exist.");
        }

        mAppName =  (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo).toString() : "Unknown");
        mAppVersionName = foundAppVersionName;
        mAppVersionCode = foundAppVersionCode;
        // We're caching device's language here, even if the user can change it while the app is running.
        mDeviceLanguage =  Locale.getDefault().toString();

        // We can't count on these features being available, since we need to
        // run on old devices. Thus, the reflection fandango below...
        Class<? extends PackageManager> packageManagerClass = packageManager.getClass();

        Method hasSystemFeatureMethod = null;
        try {
            hasSystemFeatureMethod = packageManagerClass.getMethod("hasSystemFeature", String.class);
        } catch (NoSuchMethodException e) {
            // Nothing, this is an expected outcome
        }

        Boolean foundNFC = null;
        Boolean foundTelephony = null;
        if (null != hasSystemFeatureMethod) {
            try {
                foundNFC = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.nfc");
                foundTelephony = (Boolean) hasSystemFeatureMethod.invoke(packageManager, "android.hardware.telephony");
            } catch (InvocationTargetException e) {
                Log.w(LOGTAG, "System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            } catch (IllegalAccessException e) {
                Log.w(LOGTAG, "System version appeared to support PackageManager.hasSystemFeature, but we were unable to call it.");
            }
        }

        mHasNFC = foundNFC;
        mHasTelephony = foundTelephony;
        mDisplayMetrics = new DisplayMetrics();

        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getMetrics(mDisplayMetrics);

        // pre-populate the JSON version with immutable info here for performance reasons
        mImmutableDeviceInfoJSON = new JSONObject();
        try {
            mImmutableDeviceInfoJSON.put("os", mOs);
            mImmutableDeviceInfoJSON.put("os_version", mOSVersion);
            mImmutableDeviceInfoJSON.put("manufacturer", mManufacturer);
            mImmutableDeviceInfoJSON.put("brand", mBrand);
            mImmutableDeviceInfoJSON.put("model", mModel);
            mImmutableDeviceInfoJSON.put("app_name", getAppName());
            mImmutableDeviceInfoJSON.put("app_version", getAppVersionName());
            mImmutableDeviceInfoJSON.put("app_version_code", Integer.toString(getAppVersionCode()));
            mImmutableDeviceInfoJSON.put("language", getDeviceLanguage());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing basic device info values in JSON object", e);
        }
        try {
            mImmutableDeviceInfoJSON.put("has_NFC", hasNFC());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing has_NFS value in JSON object", e);
        }
        try {
            mImmutableDeviceInfoJSON.put("has_telephony", hasTelephony());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing has_telephony value in JSON object", e);
        }
        try {
            DisplayMetrics dMetrics = getDisplayMetrics();
            mImmutableDeviceInfoJSON.put("display_density_dpi", dMetrics.densityDpi);
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing display_density_dpi value in JSON object", e);
        }
        try {
            mImmutableDeviceInfoJSON.put("bluetooth_version", getBluetoothVersion());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing bluetooth info values in JSON object", e);
        }
    }


    // Returns those system info that could change upon time.
    public JSONObject getMutableDeviceInfo() {
        JSONObject mutableDeviceInfo = new JSONObject();
        try {
            mutableDeviceInfo.put("bluetooth_enabled", isBluetoothEnabled());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing bluetooth info values in JSON object", e);
        }

        try {
            mutableDeviceInfo.put("current_network_operator", getCurrentNetworkOperator());
            mutableDeviceInfo.put("phone_radio_type", getPhoneRadioType()); // NONE - GMS - CDMA - SIP
            mutableDeviceInfo.put("wifi_connected", isWifiConnected());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing network info values in JSON object", e);
        }

        return mutableDeviceInfo;
    }

    public JSONObject getImmutableDeviceInfo() {
        return mImmutableDeviceInfoJSON;
    }

    public String getAppName() { return mAppName; }

    public String getAppVersionName() { return mAppVersionName; }

    public Integer getAppVersionCode() { return mAppVersionCode; }

    public boolean hasNFC() { return mHasNFC; }

    public boolean hasTelephony() { return mHasTelephony; }

    public DisplayMetrics getDisplayMetrics() { return mDisplayMetrics; }

    public String getPhoneRadioType() {
        String ret = null;

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager) {
            switch(telephonyManager.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_NONE:
                ret = "none";
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                ret = "gsm";
                break;
            case TelephonyManager.PHONE_TYPE_CDMA:
                ret = "cdma";
                break;
                case TelephonyManager.PHONE_TYPE_SIP:
                ret = "sip";
                break;
            default:
                ret = null;
            }
        }

        return ret;
    }

    // Note this is the *current*, not the canonical network, because it
    // doesn't require special permissions to access. Unreliable for CDMA phones,
    //
    public String getCurrentNetworkOperator() {
        String ret = null;

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager)
            ret = telephonyManager.getNetworkOperatorName();

        return ret;
    }


    public Boolean isWifiConnected() {
        Boolean ret = null;

        if (PackageManager.PERMISSION_GRANTED == mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            ret = wifiInfo.isConnected();
        }

        return ret;
    }

    public Boolean isBluetoothEnabled() {
        Boolean isBluetoothEnabled = null;
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                isBluetoothEnabled = bluetoothAdapter.isEnabled();
            }
        } catch (SecurityException e) {
            // do nothing since we don't have permissions
        }
        return isBluetoothEnabled;
    }

    public String getBluetoothVersion() {
        String bluetoothVersion = null;
        if (Build.VERSION.SDK_INT >= 8) {
            bluetoothVersion = "none";
            if(Build.VERSION.SDK_INT >= 18 &&
                    mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble";
            } else if(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic";
            }
        }
        return bluetoothVersion;
    }

    public int getDeviceWidthPixels() {
        return getDisplayMetrics().widthPixels;
    }

    public int getDeviceHeightPixels() {
        return getDisplayMetrics().heightPixels;
    }

    public String getDeviceLanguage() {
        return mDeviceLanguage;
    }
}
