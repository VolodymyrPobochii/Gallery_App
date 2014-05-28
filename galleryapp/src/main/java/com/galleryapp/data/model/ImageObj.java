package com.galleryapp.data.model;

import android.content.ContentValues;

import com.galleryapp.data.provider.GalleryDBContent;

public class ImageObj {
    private Integer id;
    private String imageNmae;
    private String imagePath;
    private String thumbPath;
    private String createDate;
    private String imageTitle;
    private String imageNotes;
    private Integer isSynced;
    private Integer needUpload;
    private String fileUri;
    private String fileId;
    private String status;

    public ImageObj() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getImageNmae() {
        return imageNmae;
    }

    public void setImageName(String imageNmae) {
        this.imageNmae = imageNmae;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getImageTitle() {
        return imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public String getImageNotes() {
        return imageNotes;
    }

    public void setImageNotes(String imageDescription) {
        this.imageNotes = imageDescription;
    }

    public Integer getIsSynced() {
        return isSynced;
    }

    public void setIsSynced(Integer isSynced) {
        this.isSynced = isSynced;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public Integer getNeedUpload() {
        return needUpload;
    }

    public void setNeedUpload(Integer needUpload) {
        this.needUpload = needUpload;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();

        cv.put(GalleryDBContent.GalleryImages.Columns.IMAGE_NAME.getName(), getImageNmae());
        cv.put(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName(), getImagePath());
        cv.put(GalleryDBContent.GalleryImages.Columns.THUMB_PATH.getName(), getThumbPath());
        cv.put(GalleryDBContent.GalleryImages.Columns.IMAGE_TITLE.getName(), getImageTitle());
        cv.put(GalleryDBContent.GalleryImages.Columns.IMAGE_NOTES.getName(), getImageNotes());
        cv.put(GalleryDBContent.GalleryImages.Columns.CREATE_DATE.getName(), getCreateDate());
        cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), getIsSynced());
        cv.put(GalleryDBContent.GalleryImages.Columns.NEED_UPLOAD.getName(), getNeedUpload());
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_URI.getName(), getFileUri());
        cv.put(GalleryDBContent.GalleryImages.Columns.FILE_ID.getName(), getFileId());
        cv.put(GalleryDBContent.GalleryImages.Columns.STATUS.getName(), getStatus());

        return cv;
    }
}
