package com.galleryapp;

import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;

public interface ProgressiveEntityListener {
    void onFileUploaded(FileUploadObj response, String id, String name, long length);

    void onDocSubmitted(DocSubmittedObj response, String id, String name);
}