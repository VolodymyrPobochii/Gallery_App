package com.galleryapp;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by pvg on 16.07.14.
 */
public class ScanPreff extends Preference {
    private static final String TAG = ScanPreff.class.getSimpleName();
    private Context mContext = null;
    private LayoutInflater mLayoutInflater = null;
    private View myView = null;
    private EditText mSummary;
    private TextView mTitle;

    public ScanPreff(Context context) {
        super(context);
        Logger.d(TAG, "ScanPreff()");
        init(context);
    }

    public ScanPreff(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        Logger.d(TAG, "ScanPreff()");
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.setLayoutResource(R.layout.common_layout);
        myView = mLayoutInflater.inflate(getLayoutResource(), null);
        mTitle = (TextView) myView.findViewById(android.R.id.title);
        mSummary = (EditText) myView.findViewById(android.R.id.summary);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        Logger.d(TAG, "getView()");
        return myView;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Logger.d(TAG, "onBindView()");
//        mSummary = (EditText) view.findViewById(android.R.id.summary);
    }

    @Override
    public CharSequence getSummary() {
        Logger.d(TAG, "getSummary()");
        return mSummary != null ? mSummary.getText().toString() : "null";
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        Logger.d(TAG, "setSummary()");
        if (mSummary != null) mSummary.setText(summary);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        Logger.d(TAG, "setTitle()");
        if (mTitle != null) mTitle.setText(title);
    }

    @Override
    public CharSequence getTitle() {
        Logger.d(TAG, "getTitle()");
        return mTitle != null ? mTitle.getText().toString() : "null";
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        super.setDefaultValue(defaultValue);
        if (mSummary.getText().length() == 0) mSummary.setText(defaultValue.toString());
    }


}
