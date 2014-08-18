package com.galleryapp.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.galleryapp.R;
import com.galleryapp.data.provider.GalleryDBContent;
import com.galleryapp.utils.MetricsHelper;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;

/**
 * Adapter for our image files.
 */
public class ImageAdapter extends CursorAdapter {

    private static final String TAG = ImageAdapter.class.getSimpleName();
    private final LayoutInflater mInflator;
    private ImageLoader mImageLoader;
    private DisplayImageOptions mOptions;
    private Context mContext;
    private ViewHolder mHolder;
//    private CheckableLayout mCheckableLayout;

    public ImageAdapter(Context context, ImageLoader imageLoader, DisplayImageOptions options) {
        super(context, null, false);
        mContext = context;
        mImageLoader = imageLoader;
        mOptions = options;
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        View picturesView = mInflator.inflate(R.layout.item_grid_image, parent, false);
        ViewGroup.LayoutParams lp = picturesView.getLayoutParams();
        lp.height = MetricsHelper.getThumbHeight(context);
        Log.d(TAG, "viewHeight = " + lp.height + " viewWidth = " + lp.width);
        picturesView.setLayoutParams(lp);
        mHolder = new ViewHolder();
        mHolder.imageView = (ImageView) picturesView.findViewById(R.id.image);
        mHolder.thumbTitle = (TextView) picturesView.findViewById(R.id.thumb_title);
        mHolder.thumbTitle.setText("");
        mHolder.thumbSyncStatus = (TextView) picturesView.findViewById(R.id.thumb_is_synced);
        mHolder.thumbSyncStatus.setText("");
        mHolder.thumbStatus = (TextView) picturesView.findViewById(R.id.thumb_status);
        mHolder.thumbStatus.setText("");
        mHolder.progressBar = (ProgressBar) picturesView.findViewById(R.id.progress);
        picturesView.setTag(mHolder);
        return picturesView;
    }

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
//        ((ImageView)view).setImageBitmap(null);
        final String imagePath = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_PATH.getName()));
        final String thumbPath = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.THUMB_PATH.getName()));
        Log.d("MediaStore", "bindView::thumbPath = " + thumbPath);
        final String imageTitle = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IMAGE_TITLE.getName()));
        final String imageDate = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.CREATE_DATE.getName()));
        final String imageStatus = cursor.getString(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.STATUS.getName()));
        final int imageSynced = cursor.getInt(cursor.getColumnIndex(GalleryDBContent.GalleryImages.Columns.IS_SYNCED.getName()));
        final ViewHolder holder = (ViewHolder) view.getTag();
        holder.thumbTitle.setText(String.format("%s\n%s", imageTitle, imageDate));
        holder.thumbStatus.setText(imageStatus);
        if (imageSynced == 1) {
            holder.thumbSyncStatus.setText("Synced");
        } else {
            holder.thumbSyncStatus.setText("Not Synced");
        }
//        Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID);
//        ((ImageView) view).setImageURI(imageUri);
//        Log.d("URI", "thumbURI = " + Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, "" + imageID).toString());
        // Set the content of the image based on the provided URI
        mImageLoader.displayImage("file://" + thumbPath, holder.imageView, mOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
//                        holder.progressBar.setProgress(0);
//                        holder.progressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view,
                                                FailReason failReason) {
//                        holder.progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                        holder.progressBar.setVisibility(View.GONE);
                    }
                }, new ImageLoadingProgressListener() {
                    @Override
                    public void onProgressUpdate(String imageUri, View view, int current, int total) {
//                        holder.progressBar.setProgress(Math.round(100.0f * current / total));
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
        TextView thumbSyncStatus;
        ProgressBar progressBar;
        TextView thumbStatus;
    }

    /*public static class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public CheckableLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CheckableLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
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
    }*/
}