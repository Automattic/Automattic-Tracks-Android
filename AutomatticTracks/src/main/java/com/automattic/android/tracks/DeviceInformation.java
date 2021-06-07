package com.automattic.android.tracks;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
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
    private static final String ORIENTATION_PORTRAIT = "portrait";
    private static final String ORIENTATION_LANDSCAPE = "landscape";

    private static final int DISPLAY_SIZE_LARGE_THRESHOLD = 7;

    private final Context mContext;

    // Unchanging facts
    private final Boolean mHasNFC;
    private final Boolean mHasTelephony;
    private final DisplayMetrics mDisplayMetrics;
    private final String mAppName;
    private final String mAppVersionName;
    private final Integer mAppVersionCode;
    private final Locale mLocale;
    private final String mDeviceLanguage;
    private int mWidthPixels;
    private int mHeightPixels;

    private final JSONObject mImmutableDeviceInfoJSON;
    private final boolean mIsPortraitDefault;

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

        mAppName = applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo).toString() : "Unknown";
        mAppVersionName = foundAppVersionName;
        mAppVersionCode = foundAppVersionCode;
        // We're caching device's language here, even if the user can change it while the app is running.
        mLocale = Locale.getDefault();
        mDeviceLanguage = mLocale.toString();
        mIsPortraitDefault = isPortraitDefault();

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
        if (display != null) {
            display.getMetrics(mDisplayMetrics);
            // since SDK_INT = 1; This doesn't include window decorations
            mWidthPixels = mDisplayMetrics.widthPixels;
            mHeightPixels = mDisplayMetrics.heightPixels;
            // Try to load the real screen size now - This does include window decorations (statusbar bar/menu bar)
            try {
                Point realSize = new Point();
                display.getRealSize(realSize);
                mWidthPixels = realSize.x;
                mHeightPixels = realSize.y;
            } catch (final Exception exception) {
                Log.w(LOGTAG, "Unable to call getRealSize: " + exception.getMessage());
            }
        }

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
            int densityDpi = getDisplayMetrics().densityDpi;
            mImmutableDeviceInfoJSON.put("display_density_dpi", densityDpi);
            if (densityDpi > 0) {
                double height = mHeightPixels / (double) densityDpi;
                double width = mWidthPixels / (double) densityDpi;
                double size = Math.hypot(width, height);
                // Format it now
                size = Math.round(size * 10d) / 10d;
                mImmutableDeviceInfoJSON.put("display_size", size);
                mImmutableDeviceInfoJSON.put("is_large_display", size >= DISPLAY_SIZE_LARGE_THRESHOLD);
            }
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing display_density_dpi value in JSON object", e);
        }
        try {
            // Width and height depend on device orientation - to be consistent, always
            // report the shorter dimension as the width.
            // These values represent the 'real' dimensions of the screen, ignoring navigation
            // and window decoration.
            if (mHeightPixels > mWidthPixels) {
                mImmutableDeviceInfoJSON.put("display_height", mHeightPixels);
                mImmutableDeviceInfoJSON.put("display_width", mWidthPixels);
            } else {
                mImmutableDeviceInfoJSON.put("display_height", mWidthPixels);
                mImmutableDeviceInfoJSON.put("display_width", mHeightPixels);
            }
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing display_height and width value in JSON object", e);
        }
        try {
            // Width and height depend on device orientation - to be consistent, always
            // report the shorter dimension as the width.
            // These values represent the usable dimensions of the screen - whatever is left after
            // navigation and window decoration.
            if (mDisplayMetrics.heightPixels > mDisplayMetrics.widthPixels) {
                mImmutableDeviceInfoJSON.put("display_usable_height", mDisplayMetrics.heightPixels);
                mImmutableDeviceInfoJSON.put("display_usable_width", mDisplayMetrics.widthPixels);
            } else {
                mImmutableDeviceInfoJSON.put("display_usable_height", mDisplayMetrics.widthPixels);
                mImmutableDeviceInfoJSON.put("display_usable_width", mDisplayMetrics.heightPixels);
            }
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing display_usable_height and width value in JSON object", e);
        }
        try {
            mImmutableDeviceInfoJSON.put("bluetooth_version", getBluetoothVersion());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing bluetooth info values in JSON object", e);
        }
        try {
            mImmutableDeviceInfoJSON.put("is_rtl_language", isRtlLanguage());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing is_rtl_language value in JSON object", e);
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
            mutableDeviceInfo.put("is_online", isOnline());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing network info values in JSON object", e);
        }

        try {
            mutableDeviceInfo.put("device_orientation", getDeviceOrientation());
        } catch (final JSONException e) {
            Log.e(LOGTAG, "Exception writing device orientation info value in JSON object", e);
        }

        return mutableDeviceInfo;
    }

    private String getDeviceOrientation() {
        int currentRotation = getCurrentRotation();

        if (currentRotation == Surface.ROTATION_0 || currentRotation == Surface.ROTATION_180) {
            return mIsPortraitDefault ? ORIENTATION_PORTRAIT : ORIENTATION_LANDSCAPE;
        } else {
            return mIsPortraitDefault ? ORIENTATION_LANDSCAPE : ORIENTATION_PORTRAIT;
        }
    }

    private boolean isPortraitDefault() {
        int currentRotation = getCurrentRotation();
        return (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && (currentRotation == Surface.ROTATION_0 || currentRotation == Surface.ROTATION_180))
               || (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                   && ((currentRotation == Surface.ROTATION_90 || currentRotation == Surface.ROTATION_270)));
    }

    private int getCurrentRotation() {
        return ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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
        Boolean ret = Boolean.FALSE;

        if (PackageManager.PERMISSION_GRANTED == mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager != null) {
                try {
                    NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    ret = wifiInfo != null && wifiInfo.isConnected();
                } catch (NullPointerException e) {
                    Log.e(LOGTAG, "Cannot access WI-FI status", e);
                    // See: https://github.com/Automattic/Automattic-Tracks-Android/issues/29
                }
            }
        }

        return ret;
    }

    private Boolean isOnline() {
        if (PackageManager.PERMISSION_GRANTED != mContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            return false;
        }

        ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission")
        NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public Boolean isBluetoothEnabled() {
        Boolean isBluetoothEnabled = null;
        if (PackageManager.PERMISSION_GRANTED == mContext.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH)) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                isBluetoothEnabled = bluetoothAdapter.isEnabled();
            }
        }
        return isBluetoothEnabled;
    }

    public String getBluetoothVersion() {
        String bluetoothVersion = "none";
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothVersion = "ble";
        } else if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothVersion = "classic";
        }
        return bluetoothVersion;
    }

    /**
     * @return True if the default locale is Right-to-left, false otherwise.
     */
    public boolean isRtlLanguage() {
        return TextUtils.getLayoutDirectionFromLocale(mLocale) == View.LAYOUT_DIRECTION_RTL;
    }

    public String getDeviceLanguage() {
        return mDeviceLanguage;
    }
}
