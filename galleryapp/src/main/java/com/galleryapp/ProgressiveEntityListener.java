package com.galleryapp;

import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;

public interface ProgressiveEntityListener {
    void onFileUploaded(FileUploadObj response, String id, String name, long length);

    void onDocSubmitted(DocSubmittedObj response, String id, String name);

    void onDocStatus(DocStatusObj response, String id, String docId);
}