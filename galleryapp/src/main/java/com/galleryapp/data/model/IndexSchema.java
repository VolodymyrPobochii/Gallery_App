package com.galleryapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pvg on 05.08.14.
 */
public class IndexSchema implements Parcelable {
    private String $type = "";
    private Schema Schema = new Schema();
    private Integer ErrorCode = 0;
    private String ErrorMessage = "";

    public IndexSchema() {
    }

    public String get$type() {
        return $type;
    }

    public void set$type(String $type) {
        this.$type = $type;
    }

    public Schema getSchema() {
        return Schema;
    }

    public void setSchema(Schema schema) {
        Schema = schema;
    }

    public Integer getErrorCode() {
        return ErrorCode;
    }

    public void setErrorCode(Integer errorCode) {
        ErrorCode = errorCode;
    }

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ErrorMessage = errorMessage;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.$type);
        dest.writeParcelable(this.Schema, 0);
        dest.writeValue(this.ErrorCode);
        dest.writeString(this.ErrorMessage);
    }

    private IndexSchema(Parcel in) {
        this.$type = in.readString();
        this.Schema = in.readParcelable(Schema.class.getClassLoader());
        this.ErrorCode = (Integer) in.readValue(Integer.class.getClassLoader());
        this.ErrorMessage = in.readString();
    }

    public static final Parcelable.Creator<IndexSchema> CREATOR = new Parcelable.Creator<IndexSchema>() {
        public IndexSchema createFromParcel(Parcel source) {
            return new IndexSchema(source);
        }

        public IndexSchema[] newArray(int size) {
            return new IndexSchema[size];
        }
    };

    @Override
    public String toString() {
        return "IndexScheme{Error{" + ErrorMessage + "}";
    }
}
