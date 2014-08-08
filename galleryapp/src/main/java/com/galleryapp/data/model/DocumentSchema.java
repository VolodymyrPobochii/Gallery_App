package com.galleryapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pvg on 05.08.14.
 */
public class DocumentSchema implements Parcelable {

    private String $type = "";
    private String Code = "";
    private String Name = "";
    private String Description = "";
    private List<SchemeElement> Elements = new ArrayList<SchemeElement>();

    public DocumentSchema() {}

    public String get$type() {
        return $type;
    }

    public void set$type(String $type) {
        this.$type = $type;
    }

    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public List<SchemeElement> getElements() {
        return Elements;
    }

    public void setElements(List<SchemeElement> elements) {
        Elements = elements;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.$type);
        dest.writeString(this.Code);
        dest.writeString(this.Name);
        dest.writeString(this.Description);
        dest.writeTypedList(Elements);
    }

    private DocumentSchema(Parcel in) {
        this.$type = in.readString();
        this.Code = in.readString();
        this.Name = in.readString();
        this.Description = in.readString();
        in.readTypedList(Elements, SchemeElement.CREATOR);
    }

    public static final Parcelable.Creator<DocumentSchema> CREATOR = new Parcelable.Creator<DocumentSchema>() {
        public DocumentSchema createFromParcel(Parcel source) {
            return new DocumentSchema(source);
        }

        public DocumentSchema[] newArray(int size) {
            return new DocumentSchema[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SchemeElement element : Elements){
            sb.append(element.toString()).append(";");
        }
        return sb.toString();
    }
}
