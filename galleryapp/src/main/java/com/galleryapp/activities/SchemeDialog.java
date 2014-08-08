package com.galleryapp.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.galleryapp.R;
import com.galleryapp.data.provider.GalleryDBContent.IndexSchemas;
import com.galleryapp.views.SchemeElementSelector;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pvg on 05.08.14.
 */
public class SchemeDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = SchemeDialog.class.getSimpleName();
    private static final String ARG_CODE = "capchcode";

    private Activity mActivity;
    private LayoutInflater mInflater;

    private SchemeDialogCallbacks mCallback;

    public interface SchemeDialogCallbacks {
        void onOkClicked(String indexString);

        void onCancelClicked();
    }

    public static SchemeDialog newInstance(String capchcode) {
        Log.d(TAG, "newInstance() :: ARG_CODE = " + capchcode);
        SchemeDialog dialog = new SchemeDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CODE, capchcode);
        dialog.setArguments(args);
        return dialog;
    }

    public SchemeDialog() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "onAttach()");
        if (activity instanceof SchemeDialogCallbacks) {
            mCallback = (SchemeDialogCallbacks) activity;
        }
        mActivity = activity;
        mInflater = activity.getLayoutInflater();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState()");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated()");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "onDismiss()");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(TAG, "onCancel()");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog()");
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        getDialog().setTitle("Scheme data");
//        mRoot = new LinearLayout(mActivity);
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        mRoot.setPadding(10, 10, 10, 10);
//        mRoot.setOrientation(LinearLayout.VERTICAL);
//        mRoot.setShowDividers(LinearLayout.SHOW_DIVIDER_END);
//        mRoot.setDividerDrawable(getResources().getDrawable(android.R.drawable.divider_horizontal_dark));
//        mRoot.setDividerPadding(5);
//        mRoot.setLayoutParams(lp);
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.scheme_root, container);
        root.findViewById(R.id.progress).setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated()");
        getLoaderManager().initLoader(R.id.scheme_loader, getArguments(), this);
    }

    @Override
    public void onDestroy() {
        mCallback = null;
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(mActivity, IndexSchemas.CONTENT_URI, IndexSchemas.PROJECTION,
                IndexSchemas.Columns.CHANNCODE.getName() + "=?", new String[]{bundle.getString(ARG_CODE, "")}, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            Log.d(TAG, "onLoadFinished() :: Cursor = " + cursor.getCount());

            final List<View> views = new ArrayList<View>();
            final List<String> names = new ArrayList<String>();
            ViewGroup root = (ViewGroup) getView();
            if (root != null) {
                while (cursor.moveToNext()) {

                    String type = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.TYPE.getName()));
                    String code = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.CODE.getName()));
                    String name = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.NAME.getName()));
                    Log.d(TAG, "onLoadFinished() :: type = " + type + " / code = " + code);

                    if (type.contains("LookupSchemaElement")) {

                        View spinnerView = SchemeElementSelector.getViewByType(mInflater,
                                SchemeElementSelector.TYPE_SPINNER, name, code.hashCode(), root);
                        ((TextView) spinnerView.findViewById(R.id.name_sp)).setText(name);
                        Spinner spinner = (Spinner) spinnerView.findViewById(R.id.component_sp);
                        spinner.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.view_textview,
                                mActivity.getResources().getStringArray(R.array.stub_sp)));

                        views.add(spinner);

                    } else if (type.contains("TextSchemaElement")) {

                        View editText = SchemeElementSelector.getViewByType(mInflater,
                                SchemeElementSelector.TYPE_EDITTEXT, name, code.hashCode(), root);
                        ((TextView) editText.findViewById(R.id.name_et)).setText(name);
                        EditText et = (EditText) editText.findViewById(R.id.component_et);

                        views.add(et);
                    }

                    names.add(name);
                }
                View buttonBar = mInflater.inflate(R.layout.buttons_bar, root, true);
                buttonBar.findViewById(R.id.ok_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mCallback != null) {
                            StringBuilder sb = new StringBuilder();
                            int viewSize = views.size();
                            for (int i = 0; i < viewSize; i++) {
                                View v = views.get(i);
                                String name = names.get(i);
                                sb.append(name).append("=");
                                if (v instanceof Spinner) {
                                    sb.append(((Spinner) v).getSelectedItem());
                                } else if (v instanceof EditText) {
                                    sb.append(((EditText) v).getText().toString());
                                }
                                if (i != (viewSize - 1)) {
                                    sb.append("&");
                                }
                            }
                            String indexString = sb.toString();
                            Log.d(TAG, "onLoadFinished() :: onOkClicked :: indexString = " + indexString);
                            mCallback.onOkClicked(indexString);
                        }
                        getDialog().dismiss();
                    }
                });

                buttonBar.findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mCallback != null) {
                            mCallback.onCancelClicked();
                        }
                        getDialog().dismiss();
                    }
                });

                root.findViewById(R.id.progress).setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "onLoadFinished() :: Cursor NULL or EMPTY");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
}
