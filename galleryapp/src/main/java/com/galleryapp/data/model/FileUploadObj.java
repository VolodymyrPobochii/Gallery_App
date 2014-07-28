package com.galleryapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FileUploadObj implements Parcelable {
    public String Url;

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.Url);
    }

    public FileUploadObj() {
    }

    private FileUploadObj(Parcel in) {
        this.Url = in.readString();
    }

    public static final Parcelable.Creator<FileUploadObj> CREATOR = new Parcelable.Creator<FileUploadObj>() {
        public FileUploadObj createFromParcel(Parcel source) {
            return new FileUploadObj(source);
        }

        public FileUploadObj[] newArray(int size) {
            return new FileUploadObj[size];
        }
    };
}