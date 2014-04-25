package com.galleryapp.fragmernts;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import android.widget.Toast;

import com.galleryapp.Config;
import com.galleryapp.R;
import com.galleryapp.adapters.ImageAdapter;
import com.galleryapp.asynctasks.CreateResourceTask;
import com.galleryapp.data.provider.GalleryDBContent;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.http.entity.FileEntity;

import java.io.File;
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
public class GalleryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
//        testThumbs();
    }

    private void testThumbs() {
        Cursor thumbs = getActivity().getContentResolver()
                .query(
                        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        new String[]{
                                MediaStore.Images.Thumbnails._ID,
                                MediaStore.Images.Thumbnails.DATA,
                                MediaStore.Images.Thumbnails.IMAGE_ID,
                        },
                        null, null, null
                );
        assert thumbs != null;
        if (thumbs.getCount() > 0) {
            thumbs.moveToNext();
            Log.d("MediaStore", "THUMBS");
            Log.d("MediaStore", "THUMBS::_ID = " + thumbs.getString(thumbs.getColumnIndex(MediaStore.Images.Thumbnails._ID)));
            Log.d("MediaStore", "THUMBS::DATA = " + thumbs.getString(thumbs.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
            Log.d("MediaStore", "THUMBS::IMAGE_ID = " + thumbs.getString(thumbs.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID)));
            thumbs.close();
        }
        Cursor images = getActivity().getContentResolver()
                .query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{
                                MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.DATA,
                        },
                        null, null, null
                );
        assert images != null;
        if (images.getCount() > 0) {
            images.moveToNext();
            Log.d("MediaStore", "IMAGES");
            Log.d("MediaStore", "IMAGES::_ID = " + images.getString(images.getColumnIndex(MediaStore.Images.Thumbnails._ID)));
            Log.d("MediaStore", "IMAGES::DATA = " + images.getString(images.getColumnIndex(MediaStore.Images.Thumbnails.DATA)));
            images.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection, // Which columns to return
                        null,       // Return all rows
                        null,
                        null);
                assert cursor != null;
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToPosition(position);
                // Get image filename
                String imagePath = cursor.getString(columnIndex);
                cursor.close();
                // Use this path to do further processing, i.e. full screen display
                Toast.makeText(getActivity(), "ImagePath : " + imagePath, Toast.LENGTH_SHORT).show();
                // Use this path to do further processing, i.e. full screen display
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
                switch (selectCount) {
                    case 1:
                        mode.setSubtitle("One item selected");
                        break;
                    default:
                        mode.setSubtitle("" + selectCount + " items selected");
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
                        sendSelectedItems();
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
                mode.setTitle("Select Items");
                mode.setSubtitle("One item selected");
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
        return rootView;
    }

    private void sendSelectedItems() {
        String filePath = null;
        String fileName = null;

        Cursor cursor = ((ImageAdapter) mGridView.getAdapter()).getCursor();
        assert cursor != null;
        if (cursor.getCount() > 0) {
            for (Integer id : mCheckedIds) {
                Log.d("CHECKED_IDS", "ID[] = " + id);
                cursor.moveToPosition(id);
                filePath = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName()));
                fileName = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_NAME.getName()));
            }
            cursor.close();
        }

        String token = "token";
        String cmsBaseUrl = Config.URL_PREFIX + Config.DEFAULT_HOST + ":" + Config.DEFAULT_PORT + Config.DEFAULT_CSM_URL_BODY;
        String domain = Config.DEFAULT_DOMAIN;
        String responseId = "13";
        String url = cmsBaseUrl + Config.MOBILE_CREATE_RESOURCE_RULE + domain;
        String query = String.format("%s=%s&%s=%s", "t", token, "u", Config.AMBUL_DOCUMENTS + fileName);
        url += "?" + query;
        assert filePath != null;
        File file = new File(filePath);
        FileEntity fileEntity = new FileEntity(file, Config.CONTENT_TYPE_IMAGE_JPG);
        CreateResourceTask putTask = new CreateResourceTask(getActivity(), fileEntity, fileName, url, responseId);
        putTask.execute();
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

    private void initThumbLoader() {
//        Bundle b = new Bundle();
//        b.putInt(LIMIT, limit);
        getLoaderManager().restartLoader(R.id.gallery_thumbs_loader, null, this);
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
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    }
}
