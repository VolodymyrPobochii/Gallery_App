package com.galleryapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.galleryapp.R;
import com.galleryapp.fragmernts.GalleryFragment;


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
