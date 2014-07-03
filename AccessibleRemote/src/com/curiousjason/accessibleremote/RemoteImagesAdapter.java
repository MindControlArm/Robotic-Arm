package com.curiousjason.accessibleremote;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

// TODO: Maintain aspect ratio of thumbnails

/**
 * Extended base adapter for thumbnails of the remotes
 * @author Jason Friedman
 *
 */
public class RemoteImagesAdapter extends BaseAdapter {

	Context context;
	Bitmap[] bitmaps; 
	public final String TAG="REMOTEIMAGESADAPTER";

	/**
	 * Constructor
	 * @param c context of the activity
	 * @param filenames Filenames of the images
	 */
	public RemoteImagesAdapter(Context c,String[] filenames) {
		context = c;
		bitmaps = new Bitmap[filenames.length];
		for (int k=0;k<filenames.length;k++) {
			// TODO - fix height and width
			Log.v(TAG,"Trying to load image "+filenames[k]);
			BitmapWithSize b = MainScreen.loadBitmap(context, filenames[k], 500, 200);
			bitmaps[k] = b.bitmap;
		}
	}

	@Override
	public int getCount() {
		return bitmaps.length;
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageview;
		if (convertView == null) {  // if it's not recycled, initialize some attributes
			imageview = new ImageView(context);
			imageview.setScaleType(ImageView.ScaleType.FIT_XY);
			imageview.setPadding(1, 1, 1, 1);
			imageview.setImageBitmap(bitmaps[position]);
		} else {
			imageview = (ImageView) convertView;
		}
		return imageview;
	}

}
