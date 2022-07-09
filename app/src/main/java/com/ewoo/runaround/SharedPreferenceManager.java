package com.ewoo.runaround;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceManager {

    private static SharedPreferenceManager instance;

    private static SharedPreferences pref;

    private static final Object sLock = new Object();

    private SharedPreferenceManager() {}

    public static SharedPreferenceManager getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = new SharedPreferenceManager();
            }

            if (pref == null) {
                pref = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE);
            }
        }

        return instance;
    }

    public String getString(String key, String defValue) {
        return pref.getString(key, defValue);
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor prefsEditor = pref.edit();
        prefsEditor.putString(key, value);
        prefsEditor.apply();
    }

    public int getInt(String key, int defValue) {
        return pref.getInt(key, defValue);
    }

    public void putInt(String key, int value) {
        SharedPreferences.Editor prefsEditor = pref.edit();
        prefsEditor.putInt(key, value);
        prefsEditor.apply();
    }

    public boolean getBoolean(String key, boolean defValue) {
        return pref.getBoolean(key, defValue);
    }

    public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor prefsEditor = pref.edit();
        prefsEditor.putBoolean(key, value);
        prefsEditor.apply();
    }
}
