package com.galleryapp.activities;

import android.app.Activity;
import android.os.Bundle;

import com.galleryapp.application.GalleryApp;

public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected GalleryApp getApp() {
        return (GalleryApp) getApplication();
    }
}
