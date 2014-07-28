package com.galleryapp.fragmernts;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.galleryapp.R;
import com.galleryapp.adapters.ImageAdapter;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.data.provider.GalleryDBProvider;
import com.galleryapp.syncadapter.SyncAdapter;
import com.galleryapp.syncadapter.SyncBaseFragment;
import com.galleryapp.syncadapter.SyncUtils;
import com.google.common.io.Files;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GalleryFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryFragment extends SyncBaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = GalleryFragment.class.getSimpleName();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String LIMIT = "limit";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private ImageAdapter mGalleryAdapter;
    private int mCurrentLimit = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;
    private int previousTotal = 0;
    private int currentPage = 0;
    private int mConstantLimit = 100;
    private DisplayImageOptions mOptions;
    private ImageLoader mImageLoader;
    private List<Integer> mCheckedIds = new ArrayList<Integer>();
    private GridView mGridView;
    private SimpleCursorAdapter mChannelsAdapter;
    private LoaderManager.LoaderCallbacks<Cursor> mChannelsLoader = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), GalleryDBContent.Channels.CONTENT_URI, GalleryDBContent.Channels.PROJECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.getCount() > 0) {
                mChannelsAdapter.changeCursor(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mChannelsAdapter.changeCursor(null);
        }
    };
    private Spinner mChannels;

    public ImageAdapter getGalleryAdapter() {
        return mGalleryAdapter;
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GalleryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GalleryFragment newInstance(String param1, String param2) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public GalleryFragment() {
        super();
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mImageLoader = ImageLoader.getInstance();
        mOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        initThumbLoader();
        initChannelsLoader();
    }

    private void initThumbLoader() {
//        Bundle b = new Bundle();
//        b.putInt(LIMIT, limit);
        getLoaderManager().restartLoader(R.id.gallery_thumbs_loader, null, this);
    }

    private void initChannelsLoader() {
        getLoaderManager().restartLoader(R.id.channels_loader, null, mChannelsLoader);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final SharedPreferences preff = GalleryApp.getInstance().getPreff();
        final View rootView = inflater.inflate(R.layout.fragment_gallery, container, false);
        // Inflate the layout for this fragment
        // Set up an array of the Thumbnail Image ID column we want
        assert rootView != null;

        mGridView = (GridView) rootView.findViewById(R.id.gallery_gv);
        mGalleryAdapter = new ImageAdapter(getActivity(), mImageLoader, mOptions);
        mGridView.setAdapter(mGalleryAdapter);
        // Set up a click listener
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // Get the data location of the image
            }
        });
        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                Log.d("CHECKED_IDS", "onItemCheckedStateChanged");
                // Here you can do something when items are selected/de-selected,
                // such as update the title in the CAB
                if (checked) {
                    mCheckedIds.add(position);
                } else {
                    mCheckedIds.remove((Integer) position);
                }
                int selectCount = mGridView.getCheckedItemCount();
                TextView subtitle = (TextView) mode.getCustomView().findViewById(R.id.cab_subtitle);
                switch (selectCount) {
                    case 1:
//                        mode.setSubtitle("One item selected");
                        subtitle.setText("One item selected");
                        break;
                    default:
//                        mode.setSubtitle("" + selectCount + " items selected");
                        subtitle.setText("" + selectCount + " items selected");
                        break;
                }
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d("CHECKED_IDS", "onActionItemClicked");
                // Respond to clicks on the actions in the CAB
                switch (item.getItemId()) {
                    case R.id.action_delete_photo_item:
                        deleteSelectedItems();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.action_send_photo_item:
                        mode.finish(); // Action picked, so close the CAB
                        prepareFilesForSync();
//                        sendSelectedItems();
                        return true;
                    case R.id.action_status_item:
                        mode.finish(); // Action picked, so close the CAB
                        getSelectedItemsStatus();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate the menu for the CAB
                Log.d("CHECKED_IDS", "onCreateActionMode");
                MenuInflater inflater = mode.getMenuInflater();
                assert inflater != null;
                inflater.inflate(R.menu.context, menu);
                mode.setCustomView(getActivity().getLayoutInflater().inflate(R.layout.cab_layout, null));
                ((TextView) mode.getCustomView().findViewById(R.id.cab_title)).setText("Select Items");
                ((TextView) mode.getCustomView().findViewById(R.id.cab_subtitle)).setText("One item selected");
                mChannels = (Spinner) mode.getCustomView().findViewById(R.id.cab_channels);
                mChannels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        ((TextView) view).setTextColor(Color.WHITE);
                        Cursor channelCursor = (Cursor) mChannels.getSelectedItem();
                        preff.edit()
                                .putString("domain", channelCursor.getString(channelCursor.getColumnIndex(GalleryDBContent.Channels.Columns.DOMAIN.getName())))
                                .putString("capturechannelcode", channelCursor.getString(channelCursor.getColumnIndex(GalleryDBContent.Channels.Columns.CODE.getName())))
                                .apply();
                        GalleryApp.getInstance().setUpHost();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                mChannels.setAdapter(mChannelsAdapter);
                Cursor data = mChannelsAdapter.getCursor();
                if (data != null) {
                    while (data.moveToNext()) {
                        if (data.getString(data.getColumnIndex(GalleryDBContent.Channels.Columns.DOMAIN.getName())).intern()
                                .equals(preff.getString("domain", getString(R.string.default_value_domain_preference)))) {
                            if (mChannels != null) {
                                mChannels.setSelection(data.getPosition());
                            }
                        }
                    }
                }
//                mode.setTitle("Select Items");
//                mode.setSubtitle("One item selected");
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Here you can make any necessary updates to the activity when
                // the CAB is removed. By default, selected items are deselected/unchecked.
                Log.d("CHECKED_IDS", "onDestroyActionMode");
//                mCheckedIds.clear();
                initThumbLoader();
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Here you can perform updates to the CAB due to
                // an invalidate() request
                mCheckedIds.clear();
                return true;
            }
        });
        mChannelsAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_spinner_item, null,
                new String[]{GalleryDBContent.Channels.Columns.NAME.getName()}, new int[]{android.R.id.text1}, 0);
        // Specify the layout to use when the list of choices appears
        mChannelsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return rootView;
    }

    @Override
    protected void setRefreshActionButtonState(boolean refreshing) {
        Log.d(TAG, "setRefreshActionButtonState() = " + refreshing);
        getActivity().setProgressBarIndeterminate(refreshing);
        getActivity().setProgressBarIndeterminateVisibility(refreshing);
    }

    private void getSelectedItemsStatus() {
        ArrayList<Integer> fileIds = new ArrayList<Integer>();
        ArrayList<String> fileDocIds = new ArrayList<String>();

        Cursor cursor = ((ImageAdapter) mGridView.getAdapter()).getCursor();
        assert cursor != null;
        if (cursor.getCount() > 0) {
            for (Integer id : mCheckedIds) {
                Log.d("UPLOAD", "ID[" + id + "] = " + id);
                cursor.moveToPosition(id);
                fileIds.add(cursor.getInt(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.ID.getName())));
                fileDocIds.add(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.FILE_ID.getName())));
                Log.d("UPLOAD", "filePath = " + fileIds + "\nfileName = " + fileDocIds);
            }
            cursor.close();
        }
        if (GalleryApp.getInstance().isNetworkConnected()) {
            for (Integer fileId : fileIds) {
                String docId = fileDocIds.get(fileIds.indexOf(fileId));
                GalleryApp.getInstance().getDocStatus(getActivity().getApplicationContext(), String.valueOf(fileId), docId);
            }
        } else {
            Toast.makeText(getActivity(), "No Connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareFilesForSync() {
        ContentValues cv = new ContentValues();
        cv.put(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName(), 0);
        cv.put(GalleryDBContent.GalleryImages.Columns.NEED_UPLOAD.getName(), 1);

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (Integer id : mCheckedIds) {
            operations.add(ContentProviderOperation.newUpdate(GalleryDBContent.GalleryImages.CONTENT_URI)
                    .withValues(cv)
                    .withSelection(GalleryDBContent.GalleryImages.Columns.ID.getName() + "=?", new String[]{String.valueOf(id + 1)})
                    .build());
        }
        if (operations.size() > 0) {
            try {
                int updated = getActivity().getContentResolver().applyBatch(GalleryDBProvider.AUTHORITY, operations).length;
                Log.d(TAG, "prepareFilesForSync()::applyBatch()::" + updated);
                SyncUtils.TriggerRefresh(SyncAdapter.UPLOAD_FILES);
                Log.d(TAG, "prepareFilesForSync()::SyncUtils.TriggerRefresh(UPLOAD_FILES)");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendSelectedItems() {
        List<String> filePaths = new ArrayList<String>();
        List<String> thumbPaths = new ArrayList<String>();
        List<String> fileNames = new ArrayList<String>();
        List<Integer> fileIds = new ArrayList<Integer>();

        Cursor cursor = ((ImageAdapter) mGridView.getAdapter()).getCursor();
        assert cursor != null;
        if (cursor.getCount() > 0) {
            for (Integer id : mCheckedIds) {
                Log.d("UPLOAD", "ID[" + id + "] = " + id);
                cursor.moveToPosition(id);
                fileIds.add(cursor.getInt(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.ID.getName())));
                filePaths.add(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName())));
                thumbPaths.add(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.THUMB_PATH.getName())));
                fileNames.add(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_NAME.getName())));
                Log.d("UPLOAD", "filePath = " + filePaths + "\nfileName = " + fileNames);
            }
            cursor.close();
        }

        File uploadFile = null;
//        ArrayList<TypedInput> files = new ArrayList<TypedInput>();
        ArrayList<byte[]> fileBytes = new ArrayList<byte[]>();
        if (!filePaths.isEmpty()) {
            for (String filePath : filePaths) {
                uploadFile = new File(filePath);
                try {
                    fileBytes.add(Files.toByteArray(uploadFile));
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                files.add(new TypedFile("application/binary", uploadFile));
            }
//            mListener.onStartUploadImages(filePaths.size());
            GalleryApp.getInstance().uploadFile(getActivity(), fileBytes, filePaths, thumbPaths, fileNames, fileIds);
        }
    }

    private void deleteSelectedItems() {
        ArrayList<String> checkedCursorIds = new ArrayList<String>();
        ArrayList<File> checkedImages = new ArrayList<File>();
        ArrayList<File> checkedThumbs = new ArrayList<File>();
        Cursor cursor = ((ImageAdapter) mGridView.getAdapter()).getCursor();
        assert cursor != null;
        if (cursor.getCount() > 0) {
            for (Integer id : mCheckedIds) {
                Log.d("CHECKED_IDS", "ID[] = " + id);
                cursor.moveToPosition(id);
                checkedCursorIds.add(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.ID.getName())));
                checkedImages.add(new File(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName()))));
                checkedThumbs.add(new File(cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.THUMB_PATH.getName()))));
            }
            cursor.close();
        }
        mListener.onDeleteItemsOperation(checkedCursorIds, checkedImages, checkedThumbs);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
//            mListener.onDeleteItemsOperation(null, null, null);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity.getApplication();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri queryUri = GalleryDBContent.GalleryImages.CONTENT_URI;
//        int limit = args.getInt(LIMIT, 100);
//        queryUri = queryUri.buildUpon().appendQueryParameter(LIMIT, String.valueOf(mConstantLimit)).build();
        String[] projection = GalleryDBContent.GalleryImages.PROJECTION;
//        String selection = MediaStore.Images.Thumbnails._ID + ">=?";
//        String[] selectionArgs = {String.valueOf(mCurrentLimit)};
        return new CursorLoader(getActivity(),
                queryUri,
                projection, // Which columns to return
                null,       // Return all rows
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mGalleryAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mGalleryAdapter.changeCursor(null);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onDeleteItemsOperation(ArrayList<String> ids, ArrayList<File> checkedImages, ArrayList<File> checkedThumbs);

        public void onStartUploadImages(int uploadCount);
    }
}
