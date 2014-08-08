package com.galleryapp.activities;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.galleryapp.application.GalleryApp;

public abstract class BaseActivity extends Activity {

    protected static Handler sHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected GalleryApp getApp() {
        return (GalleryApp) getApplication();
    }
}
