package com.galleryapp.utils;

import android.content.Context;
import android.util.TypedValue;

import com.galleryapp.R;

/**
 * Created by pvg on 18.08.14.
 */
public class MetricsHelper {

    private MetricsHelper() {
    }

    public static float getPixelDimen(Context context, float dipDimen) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipDimen, context.getResources().getDisplayMetrics());
    }

    public static int getDisplayWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getThumbHeight(Context context) {
        return getDisplayWidth(context) / context.getResources().getInteger(R.integer.grid_num_columns);
    }
}
