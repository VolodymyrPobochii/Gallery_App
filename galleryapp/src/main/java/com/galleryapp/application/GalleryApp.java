package com.galleryapp.application;

import android.app.Application;
import android.net.Uri;

import com.galleryapp.data.model.ImageObj;
import com.galleryapp.data.provider.GalleryDBContent;

/**
 * Created by Ksu on 22.04.2014.
 */
public class GalleryApp extends Application {
    public GalleryApp() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public Uri saveImage(ImageObj image) {
        return getContentResolver().insert(GalleryDBContent.GalleryImages.CONTENT_URI, image.toContentValues());
    }
}
