package com.galleryapp.views;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import com.galleryapp.R;

/**
 * Created by pvg on 05.08.14.
 */
public class SchemeElementSelector {

    public static final int TYPE_SPINNER = 1;
    public static final int TYPE_EDITTEXT = 2;

    private SchemeElementSelector() {
    }

    public static View getViewByType(LayoutInflater inflater, int viewType, String name, int viewId, ViewGroup root) {
        View child = null;
        switch (viewType) {
            case TYPE_EDITTEXT:
                child = inflater.inflate(R.layout.row_et, root, root != null);
                child.setId(viewId);
                break;
            case TYPE_SPINNER:
                child = inflater.inflate(R.layout.row_sp, root, root != null);
                child.setId(viewId);
                break;
            default:
        }
        return child;
    }
}
