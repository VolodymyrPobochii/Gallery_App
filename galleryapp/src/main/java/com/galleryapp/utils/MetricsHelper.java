package com.galleryapp.utils;

import android.content.res.Resources;
import android.util.TypedValue;

import com.galleryapp.R;
import com.galleryapp.application.GalleryApp;

/**
 * Created by pvg on 18.08.14.
 */
public class MetricsHelper {

    private static Resources sResources;

    static {
        sResources = GalleryApp.getInstance().getResources();
    }

    private MetricsHelper() {
    }

    public static float getPixelDimen(float dipDimen) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipDimen, sResources.getDisplayMetrics());
    }

    public static int getDisplayWidth() {
        return sResources.getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeight() {
        return sResources.getDisplayMetrics().heightPixels;
    }

    public static int getThumbHeight() {
        return getDisplayWidth() / sResources.getInteger(R.integer.grid_num_columns);
    }
}
