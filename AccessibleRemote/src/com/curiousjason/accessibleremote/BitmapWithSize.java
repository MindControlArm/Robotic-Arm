package com.curiousjason.accessibleremote;

import android.graphics.Bitmap;

/**
 * Convenience class with a bitmap and its size
 * @author Jason Friedman
 */
public class BitmapWithSize {
	public int width;
	public int height;
	public Bitmap bitmap;
	
	/**
	 * Constructor
	 * @param bitmap Bitmap 
	 * @param width Bitmap width (pixels)
	 * @param height Bitmap height (pixels)
	 */
	BitmapWithSize(Bitmap bitmap,int width,int height) {
		this.bitmap = bitmap;
		this.width = width;
		this.height = height;
	}
}
