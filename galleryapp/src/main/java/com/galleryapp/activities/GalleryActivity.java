package com.galleryapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.galleryapp.R;
import com.galleryapp.application.GalleryApp;
import com.galleryapp.data.model.ImageObj;
import com.galleryapp.fragmernts.GalleryFragment;

import java.text.SimpleDateFormat;
import java.util.Date;


public class GalleryActivity extends Activity implements GalleryFragment.OnFragmentInteractionListener {

    private static final int REQUEST_SETTINGS = 1000;
    private static final int REQUEST_CAMERA_PHOTO = 1100;
    private static final int REQUEST_LOAD_IMAGE = 1200;

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
            Log.d("Image", "URI:" + app.saveImage(imageObj).toString());
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
    public void onFragmentInteraction(Uri uri) {

    }
}
