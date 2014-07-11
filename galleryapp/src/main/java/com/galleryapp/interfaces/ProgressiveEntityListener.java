package com.galleryapp.interfaces;

import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;

import java.util.ArrayList;

public interface ProgressiveEntityListener {
    void onFileUploaded(FileUploadObj response, String id, String name, long length);

    void onDocSubmitted(DocSubmittedObj response, ArrayList<String> ids);

    void onDocStatus(DocStatusObj response, ArrayList<String> ids, String docId);
}