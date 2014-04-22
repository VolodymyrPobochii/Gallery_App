package com.galleryapp.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.galleryapp.R;
import com.galleryapp.data.provider.GalleryDBContent;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

/**
 * Adapter for our image files.
 */
public class ImageAdapter extends CursorAdapter {

    private ImageLoader mImageLoader;
    private DisplayImageOptions mOptions;
    private Context mContext;
    private ViewHolder mHolder;
    private CheckableLayout mCheckableLayout;

    public ImageAdapter(Context context, ImageLoader imageLoader, DisplayImageOptions options) {
        super(context, null, false);
        mContext = context;
        mImageLoader = imageLoader;
        mOptions = options;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View picturesView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.item_grid_image, parent, false);
        mHolder = new ViewHolder();
        assert picturesView != null;
        mHolder.imageView = (ImageView) picturesView.findViewById(R.id.image);
        mHolder.thumbTitle = (TextView) picturesView.findViewById(R.id.thumb_title);
        mHolder.progressBar = (ProgressBar) picturesView.findViewById(R.id.progress);
//        picturesView.setTag(mHolder);

//        ImageView picturesView = new ImageView(context);
//        picturesView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//        picturesView.setPadding(5, 5, 5, 5);
//        GridView.LayoutParams lp = new GridView.LayoutParams(
//                (int) context.getResources().getDimension(R.dimen.grid_item),
//                (int) context.getResources().getDimension(R.dimen.grid_item));
//        picturesView.setLayoutParams(lp);
        //        picturesView.setImageBitmap(null);
        mCheckableLayout = new CheckableLayout(context);
        mCheckableLayout.setPadding(8, 8, 8, 8);
        mCheckableLayout.setLayoutParams(new GridView.LayoutParams(
                GridView.LayoutParams.WRAP_CONTENT,
                GridView.LayoutParams.WRAP_CONTENT));
        mCheckableLayout.setForegroundGravity(Gravity.CENTER);
        mCheckableLayout.addView(picturesView);
        mCheckableLayout.setTag(mHolder);
        return mCheckableLayout;
    }

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
//        ((ImageView)view).setImageBitmap(null);
        final String imagePath = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName()));
        final String imageTitle = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_TITLE.getName()));
        final String imageDate = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.CREATE_DATE.getName()));
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.thumbTitle.setText(String.format("%s\n%s", imageTitle, imageDate));
//        Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID);
//        ((ImageView) view).setImageURI(imageUri);
//        Log.d("URI", "thumbURI = " + Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID).toString());
        // Set the content of the image based on the provided URI
        mImageLoader.displayImage("file://" + imagePath, holder.imageView, mOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
//                        mHolder.progressBar.setProgress(0);
//                        mHolder.progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view,
                                                FailReason failReason) {
//                        mHolder.progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                        mHolder.progressBar.setVisibility(View.GONE);
                    }
                }, new ImageLoadingProgressListener() {
                    @Override
                    public void onProgressUpdate(String imageUri, View view, int current,
                                                 int total) {
//                        mHolder.progressBar.setProgress(Math.round(100.0f * current / total));
                    }
                }
        );

        /*new AsyncTask<String, Void, Bitmap>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                ((ImageView) ((CheckableLayout) view).getChildAt(0)).setImageBitmap(null);
            }

            @Override
            protected Bitmap doInBackground(String... params) {
//                Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + params[0]);
                //                    bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageUri);
                return BitmapFactory.decodeFile(params[0]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                ((ImageView) ((CheckableLayout) view).getChildAt(0)).setImageBitmap(bitmap);
            }
        }.execute(imagePath);*/
    }

    private class ViewHolder {
        ImageView imageView;
        TextView thumbTitle;
        ProgressBar progressBar;
    }

    private class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        @SuppressWarnings("deprecation")
        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundColor(checked ? getResources().getColor(android.R.color.holo_blue_bright) :
                    getResources().getColor(android.R.color.transparent));
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }
}