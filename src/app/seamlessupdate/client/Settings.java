package app.seamlessupdate.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_BATTERY_NOT_LOW = "battery_not_low";
    private static final String KEY_IDLE_REBOOT = "idle_reboot";
    static final String KEY_WAITING_FOR_REBOOT = "waiting_for_reboot";

    static SharedPreferences getPreferences(final Context context) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        return PreferenceManager.getDefaultSharedPreferences(deviceContext);
    }

    static String getChannel(final Context context) {
        String def = context.getString(R.string.channel_default);
        return getPreferences(context).getString(KEY_CHANNEL, def);
    }

    static int getNetworkType(final Context context) {
        int def = Integer.valueOf(context.getString(R.string.network_type_default));
        return getPreferences(context).getInt(KEY_NETWORK_TYPE, def);
    }

    static boolean getBatteryNotLow(final Context context) {
        boolean def = Boolean.valueOf(context.getString(R.string.battery_not_low_default));
        return getPreferences(context).getBoolean(KEY_BATTERY_NOT_LOW, def);
    }

    static boolean getIdleReboot(final Context context) {
        boolean def = Boolean.valueOf(context.getString(R.string.idle_reboot_default));
        return getPreferences(context).getBoolean(KEY_IDLE_REBOOT, def);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserManager.get(this).isSystemUser()) {
            throw new SecurityException("system user only");
        }
        getPreferenceManager().setStorageDeviceProtected();
        PreferenceManager.setDefaultValues(createDeviceProtectedStorageContext(), R.xml.settings, false);
        addPreferencesFromResource(R.xml.settings);

        final Preference networkType = findPreference(KEY_NETWORK_TYPE);
        networkType.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            final int value = Integer.parseInt((String) newValue);
            getPreferences(this).edit().putInt(KEY_NETWORK_TYPE, value).apply();
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this);
            }
            return true;
        });

        final Preference batteryNotLow = findPreference(KEY_BATTERY_NOT_LOW);
        batteryNotLow.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            getPreferences(this).edit().putBoolean(KEY_BATTERY_NOT_LOW, (boolean) newValue).apply();
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this);
            }
            return true;
        });

        final Preference idleReboot = findPreference(KEY_IDLE_REBOOT);
        idleReboot.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            final boolean value = (Boolean) newValue;
            if (!value) {
                IdleReboot.cancel(this);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        final ListPreference networkType = (ListPreference) findPreference(KEY_NETWORK_TYPE);
        networkType.setValue(Integer.toString(getNetworkType(this)));
    }
}
