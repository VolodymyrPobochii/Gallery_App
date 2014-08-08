package com.galleryapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by pvg on 05.08.14.
 */
public class Schema implements Parcelable {
    private String $type = "";
    private DocumentSchema DocuemntSchema = new DocumentSchema();
    private String FolderSchema = "";
    private String BatchSchema = "";

    public Schema() {
    }

    public String get$type() {
        return $type;
    }

    public void set$type(String $type) {
        this.$type = $type;
    }

    public DocumentSchema getDocuemntSchema() {
        return DocuemntSchema;
    }

    public void setDocuemntSchema(DocumentSchema docuemntSchema) {
        DocuemntSchema = docuemntSchema;
    }

    public String getFolderSchema() {
        return FolderSchema;
    }

    public void setFolderSchema(String folderSchema) {
        FolderSchema = folderSchema;
    }

    public String getBatchSchema() {
        return BatchSchema;
    }

    public void setBatchSchema(String batchSchema) {
        BatchSchema = batchSchema;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.$type);
        dest.writeParcelable(this.DocuemntSchema, 0);
        dest.writeString(this.FolderSchema);
        dest.writeString(this.BatchSchema);
    }

    private Schema(Parcel in) {
        this.$type = in.readString();
        this.DocuemntSchema = in.readParcelable(DocumentSchema.class.getClassLoader());
        this.FolderSchema = in.readString();
        this.BatchSchema = in.readString();
    }

    public static final Parcelable.Creator<Schema> CREATOR = new Parcelable.Creator<Schema>() {
        public Schema createFromParcel(Parcel source) {
            return new Schema(source);
        }

        public Schema[] newArray(int size) {
            return new Schema[size];
        }
    };

    @Override
    public String toString() {
        return "DocumentSchema{" + DocuemntSchema.toString() + "}";
    }
}
