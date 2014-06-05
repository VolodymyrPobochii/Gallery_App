package com.galleryapp.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.galleryapp.ProgressiveEntityListener;
import com.galleryapp.R;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.DocStatusObj;
import com.galleryapp.data.model.DocSubmittedObj;
import com.galleryapp.data.model.FileUploadObj;
import com.galleryapp.data.model.ImageObj;
import com.galleryapp.fragmernts.GalleryFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class GalleryActivity extends BaseActivity
        implements GalleryFragment.OnFragmentInteractionListener, ProgressiveEntityListener {

    public static final int REQUEST_SETTINGS = 1000;
    private static final int REQUEST_CAMERA_PHOTO = 1100;
    private static final int REQUEST_LOAD_IMAGE = 1200;
    private static final long TIMER_TICK = 100l;
    private int mUploadCount;
    private int mUpdateTimes;
    private int mUpdateFreq;
    private ArrayList<String> mIds;
    private String mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GalleryFragment())
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOAD_IMAGE) {
            if (resultCode == RESULT_OK) {
                Log.d("Image", "GalleryURI:" + data.getData().toString());
                Log.d("Image", "GalleryEncodedPath:" + data.getData().getEncodedPath());
                Log.d("Image", "GalleryPath:" + data.getData().getPath());
                queryImageData(data);
            }
        }
        if (requestCode == REQUEST_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getApp().setUpHost();
            }
        }
    }

    private void queryImageData(Intent data) {
        Cursor c = getContentResolver().query(data.getData(), new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA}, null, null, null);
        assert c != null;
        if (c.getCount() > 0) {
            c.moveToNext();
            String imageId = c.getString(c.getColumnIndex(MediaStore.Images.Media._ID));
            String thumbData = queryImageThumbData(imageId);
            Log.d("Image", "GalleryFilePath:" + c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA)));
            Log.d("Image", "GalleryFileId:" + imageId);

            ImageObj imageObj = new ImageObj();
            imageObj.setImagePath(c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA)));
            imageObj.setThumbPath(thumbData);
            imageObj.setCreateDate(new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss").format(new Date()));
            imageObj.setImageTitle("New Image");
            imageObj.setImageNotes("Image from gallery");
            imageObj.setImageName("Image.jpg");

            GalleryApp app = (GalleryApp) getApplication();
            app.saveImage(imageObj);
            c.close();
        }
    }

    private String queryImageThumbData(String imageId) {
        Cursor c = getContentResolver()
                .query(
                        MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Thumbnails.DATA},
                        MediaStore.Images.Thumbnails.IMAGE_ID + "=?",
                        new String[]{imageId},
                        null);
        String thumbData = null;
        assert c != null;
        if (c.getCount() > 0) {
            c.moveToNext();
            thumbData = c.getString(c.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
            Log.d("Image", "GalleryFilePath:" + thumbData);
            c.close();
        }
        return thumbData;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, PrefActivity.class), REQUEST_SETTINGS);
                return true;
            case R.id.action_add_photo_item:
                startActivityForResult(new Intent(this, PhotoIntentActivity.class), REQUEST_CAMERA_PHOTO);
                return true;
            case R.id.action_add_gallery_item:
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_LOAD_IMAGE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDeleteItemsOperation(ArrayList<String> ids, ArrayList<File> checkedImages, ArrayList<File> checkedThumbs) {
        if (ids != null && ids.size() > 0) {
            for (String id : ids) {
                Log.d("CHECKED_IDS", "ID[delete] = " + id);
            }
            for (File checkedImage : checkedImages) {
                Log.d("CHECKED_IDS", "checkedImage[delete] = " + checkedImage);
            }
            for (File checkedThumb : checkedThumbs) {
                Log.d("CHECKED_IDS", "checkedThumb[delete] = " + checkedThumb);
            }
            getApp().deleteImage(ids, checkedImages, checkedThumbs);
        }
    }

    @Override
    public void onStartUploadImages(int uploadCount) {
        this.mUploadCount = uploadCount;
    }

    @Override
    public void onFileUploaded(FileUploadObj response, String id, String name, long length) {
        Log.d("UPLOAD", "onFileUploaded():: response = " + response.getUrl());
        mUploadCount--;
        if (getApp().updateImageUri(response.getUrl(), id) != 0) {
            Log.d("UPLOAD", "updateImageUri():: imageId = " + id + "\nImageName = " + name + "\n" + "FileURI = " + response.getUrl() +
                    "\nmUploadCount = " + mUploadCount);
            getApp().prepareSubmitDocs(this, response, id, name, length, mUploadCount);
        }
    }

    @Override
    public void onDocSubmitted(DocSubmittedObj response, ArrayList<String> ids) {
        Log.d("UPLOAD", "onDocSubmitted():: response = " + response.getId());
        int updatedCount = getApp().updateImageId(response.getId(), ids);
        Log.d("UPLOAD", "onDocSubmitted():: updatedCount = " + updatedCount);
        if (updatedCount != 0) {
            mUpdateTimes = Integer.parseInt(getApp().getPreff().getString(getString(R.string.updateTimes), "2"));
            mUpdateFreq = Integer.parseInt(getApp().getPreff().getString(getString(R.string.updateFreq), "10")) * 1000;
            getApp().getDocStatus(this, ids, response.getId());
        }
    }

    @Override
    public void onDocStatus(DocStatusObj response, final ArrayList<String> ids, final String docId) {
        Log.d("UPLOAD", "onDocStatus():: response = " + response.getStatus() + " / errorMessage = " + response.getErrorMessage());
        int updatedCount = getApp().updateImageStatus(response.getStatus(), ids, docId);
        Log.d("UPLOAD", "onDocStatus():: updatedCount = " + updatedCount);
        if (!response.getStatus().equals("Completed")) {
            if (mUpdateTimes != 0) {
                mUpdateTimes--;
                new CountDownTimer(mUpdateFreq, TIMER_TICK) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        getApp().getDocStatus(GalleryActivity.this, ids, docId);
                    }
                }.start();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("File status info")
                        .setMessage("Please check document status manually letter")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();

            }
        } else {
            mUpdateTimes = 0;
        }
    }


}
