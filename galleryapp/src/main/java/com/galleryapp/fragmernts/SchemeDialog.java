package com.galleryapp.fragmernts;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.galleryapp.Logger;
import com.galleryapp.R;
import com.galleryapp.ScanRestService;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ElementData;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBContent.IndexSchemas;
import com.galleryapp.utils.StringUtils;
import com.galleryapp.views.SchemeElementSelector;

import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by pvg on 05.08.14.
 */
public class SchemeDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = SchemeDialog.class.getSimpleName();
    private static final String ARG_CODE = "capchcode";
    private static final String ARG_IMG_ID = "imageID";

    private Activity mActivity;
    private LayoutInflater mInflater;

    private SchemeDialogCallbacks mCallback;
    private GalleryApp mApp;
    private static Handler mHandler = new Handler();
    private String[][] mImageIndexString;

    public interface SchemeDialogCallbacks {
        void onOkClicked(String indexString, int imageId);

        void onCancelClicked();
    }

    public static SchemeDialog newInstance(String capchcode, Integer id) {
        Log.d(TAG, "newInstance() :: ARG_CODE = " + capchcode);
        SchemeDialog dialog = new SchemeDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CODE, capchcode);
        args.putInt(ARG_IMG_ID, id);
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
        mApp = GalleryApp.getInstance();
        mInflater = activity.getLayoutInflater();
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
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.scheme_root, container);
        root.findViewById(R.id.progress).setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated()");
        mImageIndexString = getImageParsedIndexString();
        getLoaderManager().initLoader(R.id.scheme_loader, getArguments(), this);
    }

    private String[][] getImageParsedIndexString() {
        Cursor schemaValues = mActivity.getContentResolver().query(GalleryDBContent.GalleryImages.CONTENT_URI,
                new String[]{GalleryDBContent.GalleryImages.Columns.INDEX_SCHEMA.getName()},
                GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?",
                new String[]{String.valueOf(getArguments().getInt(ARG_IMG_ID))},
                null);

        if (schemaValues != null && schemaValues.getCount() > 0) {
            schemaValues.moveToLast();
            String indexString = schemaValues.getString(schemaValues.getColumnIndex(GalleryDBContent.GalleryImages.Columns.INDEX_SCHEMA.getName()));
            if (!TextUtils.isEmpty(indexString)) {
                return StringUtils.parseIndexElements(indexString);
            }
        }
        return null;
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
        final ViewGroup root = (ViewGroup) getView();
        if (cursor != null && cursor.getCount() > 0) {
            Log.d(TAG, "onLoadFinished() :: Cursor = " + cursor.getCount());

            final List<View> views = new ArrayList<View>();
            final List<String> names = new ArrayList<String>();

            if (root != null) {
                while (cursor.moveToNext()) {

                    String type = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.TYPE.getName()));
                    String code = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.CODE.getName()));
                    final String name = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.NAME.getName()));
                    String ruleCode = cursor.getString(cursor.getColumnIndex(IndexSchemas.Columns.RULECODE.getName()));
                    Log.d(TAG, "onLoadFinished() :: type = " + type + " / code = " + code);

                    if (type.contains("LookupSchemaElement")) {

                        View spinnerView = SchemeElementSelector.getViewByType(mInflater,
                                SchemeElementSelector.TYPE_SPINNER, name, code.hashCode(), root);
                        ((TextView) spinnerView.findViewById(R.id.name_sp)).setText(name);
                        final Spinner spinner = (Spinner) spinnerView.findViewById(R.id.component_sp);
                       /* spinner.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.view_textview,
                                mActivity.getResources().getStringArray(R.array.stub_sp)));*/
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                                Toast.makeText(mActivity, "Clicked: " + adapterView.getSelectedItem(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });
                        //TODO: network request to get Element Items
                        String baseUrl = mApp.getHostName() + ":" + mApp.getPort();
                        Logger.d(TAG, "init():: API_Url = " + baseUrl);
                        ScanRestService serviceEnum = ScanRestService.INSTANCE.initRestAdapter(baseUrl);
                        Logger.d(TAG, "init():: Created scanService = " + serviceEnum.toString());
                        ScanRestService.ScanServices mRestService = serviceEnum.getService();

                        mRestService.getItems(mApp.getDomain(), ruleCode, mApp.getToken(), new Callback<ElementData>() {
                            @Override
                            public void success(ElementData elementData, Response response) {
                                ElementData.RootData rootData = elementData.getDATA();
                                if (rootData != null) {
                                    ElementData.RootObjects rootObjects = rootData.getRoot_retrieve_objects_root_Elements();
                                    if (rootObjects != null) {
                                        List<ElementData.ElementObj> items = rootObjects.getITEMS();
                                        if (items != null && items.size() > 0) {
                                            List<String> elements = new ArrayList<String>();
                                            for (ElementData.ElementObj obj : items) {
                                                elements.add(obj.getNAME());
                                            }
                                            spinner.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.view_textview, elements));
                                            if (mImageIndexString != null) {
                                                int length = mImageIndexString.length;
                                                for (String[] indexString : mImageIndexString) {
                                                    String indexName = indexString[0];
                                                    if (indexName.intern().equals(name)) {
                                                        int selectedPos = elements.indexOf(indexString[1]);
                                                        spinner.setSelection(selectedPos != -1 ? selectedPos : 0);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void failure(RetrofitError error) {

                            }
                        });

                        views.add(spinner);

                    } else if (type.contains("TextSchemaElement")) {

                        View editText = SchemeElementSelector.getViewByType(mInflater,
                                SchemeElementSelector.TYPE_EDITTEXT, name, code.hashCode(), root);
                        ((TextView) editText.findViewById(R.id.name_et)).setText(name);
                        EditText et = (EditText) editText.findViewById(R.id.component_et);

                        if (mImageIndexString != null) {
                            Logger.d(TAG, "mImageIndexString != null");
                            int length = mImageIndexString.length;
                            Logger.d(TAG, "mImageIndexString.length = " + length);
                            for (String[] indexString : mImageIndexString) {
                                String indexName = indexString[0];
                                Logger.d(TAG, "indexName = " + indexName);
                                if (indexName.equals(name)) {
                                    Logger.d(TAG, "indexName.equals(name)::" + indexName + "==" + name);
                                    try {
                                        et.setText(URLDecoder.decode(indexString[1], HTTP.UTF_8));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    Logger.d(TAG, "et.setText = " + indexString[1]);
                                    break;
                                }
                            }
                        }

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
                                    try {
                                        sb.append(URLEncoder.encode((String) ((Spinner) v).getSelectedItem(), HTTP.UTF_8));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                } else if (v instanceof EditText) {
                                    try {
                                        sb.append(URLEncoder.encode(((EditText) v).getText().toString(), HTTP.UTF_8));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (i != (viewSize - 1)) {
                                    sb.append("&");
                                }
                            }
                            String indexString = sb.toString();
                            Logger.d(TAG, "onLoadFinished() :: onOkClicked :: indexString = " + indexString);
                            mCallback.onOkClicked(indexString, getArguments().getInt(ARG_IMG_ID));
                        }
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                });

                buttonBar.findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mCallback != null) {
                            mCallback.onCancelClicked();
                        }
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                });

                root.findViewById(R.id.progress).setVisibility(View.GONE);
            }
        } else {
            Logger.d(TAG, "onLoadFinished() :: Cursor NULL or EMPTY");
//            SyncUtils.triggerRefresh(SyncAdapter.GET_INDEX_SCHEME);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (root != null) {
                        root.findViewById(R.id.progress).setVisibility(View.GONE);
                        Toast.makeText(mActivity, "No Schema available now.", Toast.LENGTH_SHORT).show();
                        if (mCallback != null) {
                            mCallback.onCancelClicked();
                        }
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                }
            }, 2000);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }
}
