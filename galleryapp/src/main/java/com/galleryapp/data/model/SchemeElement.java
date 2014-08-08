package com.galleryapp.data.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

import com.galleryapp.data.provider.GalleryDBContent.IndexSchemas.Columns;

/**
 * Created by pvg on 05.08.14.
 */
public class SchemeElement implements Parcelable {
    private String ChannCode = "";
    private String $type = "";
    private String Uri = "";
    private String DataRoot = "";
    private String ValueField = "";
    private String DisplayField = "";
    private String ValueFieldName = "";
    private String DisplayFieldName = "";
    private Boolean AddNewAllowed = false;
    private String RuleCode = "";
    private String Code = "";
    private String Name = "";
    private String Description = "";
    private String ParameterName = "";

    public SchemeElement() {
    }

    public String get$type() {
        return $type;
    }

    public void set$type(String $type) {
        this.$type = $type;
    }

    public String getUri() {
        return Uri;
    }

    public void setUri(String uri) {
        Uri = uri;
    }

    public String getDataRoot() {
        return DataRoot;
    }

    public void setDataRoot(String dataRoot) {
        DataRoot = dataRoot;
    }

    public String getValueField() {
        return ValueField;
    }

    public void setValueField(String valueField) {
        ValueField = valueField;
    }

    public String getDisplayField() {
        return DisplayField;
    }

    public void setDisplayField(String displayField) {
        DisplayField = displayField;
    }

    public String getValueFieldName() {
        return ValueFieldName;
    }

    public void setValueFieldName(String valueFieldName) {
        ValueFieldName = valueFieldName;
    }

    public String getDisplayFieldName() {
        return DisplayFieldName;
    }

    public void setDisplayFieldName(String displayFieldName) {
        DisplayFieldName = displayFieldName;
    }

    public Boolean getAddNewAllowed() {
        return AddNewAllowed;
    }

    public void setAddNewAllowed(Boolean addNewAllowed) {
        AddNewAllowed = addNewAllowed;
    }

    public String getRuleCode() {
        return RuleCode;
    }

    public void setRuleCode(String ruleCode) {
        RuleCode = ruleCode;
    }

    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    public String getChannCode() {
        return ChannCode;
    }

    public void setChannCode(String channCode) {
        ChannCode = channCode;
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

    public String getParameterName() {
        return ParameterName;
    }

    public void setParameterName(String parameterName) {
        ParameterName = parameterName;
    }


    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();

        cv.put(Columns.CHANNCODE.getName(), getChannCode());
        cv.put(Columns.TYPE.getName(), get$type());
        cv.put(Columns.URI.getName(), getUri());
        cv.put(Columns.DATAROOT.getName(), getDataRoot());
        cv.put(Columns.VALUEFIELD.getName(), getValueField());
        cv.put(Columns.DISPLAYFIELD.getName(), getDisplayField());
        cv.put(Columns.VALUEFIELDNAME.getName(), getValueFieldName());
        cv.put(Columns.DISPLAYFIELDNAME.getName(), getDisplayFieldName());
        cv.put(Columns.ADDNEWALLOWED.getName(), getAddNewAllowed() ? 1 : 0);
        cv.put(Columns.RULECODE.getName(), getRuleCode());
        cv.put(Columns.CODE.getName(), getCode());
        cv.put(Columns.NAME.getName(), getName());
        cv.put(Columns.DESCRIPTION.getName(), getDescription());
        cv.put(Columns.PARAMETERNAME.getName(), getParameterName());

        return cv;
    }

    @Override
    public String toString() {
        return "SchemeElement{Name:" + Name + ", Type:" + $type + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.ChannCode);
        dest.writeString(this.$type);
        dest.writeString(this.Uri);
        dest.writeString(this.DataRoot);
        dest.writeString(this.ValueField);
        dest.writeString(this.DisplayField);
        dest.writeString(this.ValueFieldName);
        dest.writeString(this.DisplayFieldName);
        dest.writeValue(this.AddNewAllowed);
        dest.writeString(this.RuleCode);
        dest.writeString(this.Code);
        dest.writeString(this.Name);
        dest.writeString(this.Description);
        dest.writeString(this.ParameterName);
    }

    private SchemeElement(Parcel in) {
        this.ChannCode = in.readString();
        this.$type = in.readString();
        this.Uri = in.readString();
        this.DataRoot = in.readString();
        this.ValueField = in.readString();
        this.DisplayField = in.readString();
        this.ValueFieldName = in.readString();
        this.DisplayFieldName = in.readString();
        this.AddNewAllowed = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.RuleCode = in.readString();
        this.Code = in.readString();
        this.Name = in.readString();
        this.Description = in.readString();
        this.ParameterName = in.readString();
    }

    public static final Parcelable.Creator<SchemeElement> CREATOR = new Parcelable.Creator<SchemeElement>() {
        public SchemeElement createFromParcel(Parcel source) {
            return new SchemeElement(source);
        }

        public SchemeElement[] newArray(int size) {
            return new SchemeElement[size];
        }
    };
}
