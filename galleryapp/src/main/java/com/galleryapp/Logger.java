package com.galleryapp;

import android.util.Log;

/**
 * Created by pvg on 19.08.14.
 */
public class Logger {

    private static boolean logable = true;

    private Logger() {
    }

    public static void d(String tag, String msg) {
        if (logable) {
            Log.d(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (logable) {
            Log.v(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (logable) {
            Log.i(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (logable) {
            Log.e(tag, msg);
        }
    }
}
