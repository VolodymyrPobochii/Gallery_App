package com.galleryapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by pvg on 13.08.14.
 */
public class ElementData {

    private RootData DATA;

    public RootData getDATA() {
        return DATA;
    }

    public void setDATA(RootData DATA) {
        this.DATA = DATA;
    }

    public static class RootData {
        private RootObjects root_retrieve_objects_root_Elements;

        public RootObjects getRoot_retrieve_objects_root_Elements() {
            return root_retrieve_objects_root_Elements;
        }

        public void setRoot_retrieve_objects_root_Elements(RootObjects root_retrieve_objects_root_Elements) {
            this.root_retrieve_objects_root_Elements = root_retrieve_objects_root_Elements;
        }
    }

    public static class RootObjects {
        private List<ElementObj> ITEMS;
        private Integer TotalCount;

        public List<ElementObj> getITEMS() {
            return ITEMS;
        }

        public void setITEMS(List<ElementObj> ITEMS) {
            this.ITEMS = ITEMS;
        }

        public Integer getTotalCount() {
            return TotalCount;
        }

        public void setTotalCount(Integer totalCount) {
            TotalCount = totalCount;
        }
    }

    public static class ElementObj implements Parcelable {

        private Integer ID;
        private String NAME;
        private Integer RN;

        public Integer getID() {
            return ID;
        }

        public void setID(Integer ID) {
            this.ID = ID;
        }

        public String getNAME() {
            return NAME;
        }

        public void setNAME(String NAME) {
            this.NAME = NAME;
        }

        public Integer getRN() {
            return RN;
        }

        public void setRN(Integer RN) {
            this.RN = RN;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeValue(this.ID);
            dest.writeString(this.NAME);
            dest.writeValue(this.RN);
        }

        public ElementObj() {
        }

        private ElementObj(Parcel in) {
            this.ID = (Integer) in.readValue(Integer.class.getClassLoader());
            this.NAME = in.readString();
            this.RN = (Integer) in.readValue(Integer.class.getClassLoader());
        }

        public static final Parcelable.Creator<ElementObj> CREATOR = new Parcelable.Creator<ElementObj>() {
            public ElementObj createFromParcel(Parcel source) {
                return new ElementObj(source);
            }

            public ElementObj[] newArray(int size) {
                return new ElementObj[size];
            }
        };
    }
}
