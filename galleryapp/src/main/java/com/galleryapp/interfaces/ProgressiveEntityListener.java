package com.galleryapp.interfaces;

import org.apache.http.entity.FileEntity;

public interface ProgressiveEntityListener {
    void onFileUpload(String response, String fileName, FileEntity fileEntity, String responseId);
}
