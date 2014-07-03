package com.curiousjason.accessibleremote;

import android.util.Log;

/**
 * This class represents an area in an imagemap (rectangle, circle or poly)
 * @author Jason Friedman
 */
public class Area {
	private final String TAG = "AREA";

	static final int RECT = 1;
	static final int CIRCLE = 2;
	static final int POLY = 3;

	int type;
	int[] coords;
	String href;
	String alt;
	String title;

	/**
	 * Constructor
	 * @param type Type of shape (rectangle, circle, or polygon)
	 * @param coords The coordinates describing the shape
	 * @param href The action to perform
	 * @param alt Alt text (currently unused)
	 * @param title Title (currently unused)
	 */
	public Area(int type,int[] coords,String href,String alt,String title) {
		this.type = type;
		this.coords = coords;
		this.href = href;
		this.alt = alt;
		this.title = title;
		Log.v(TAG,"Area created with type " + type + ", first two coords " + coords[0] + "," + coords[1] + ", href " + href + ",alt " + alt + ",title " + title);
	}

	/**
	 * Is this point inside the area?
	 * @param x x coordinate
	 * @param y y coordinate
	 * @return whether the point is inside the area
	 */
	public boolean contains(float x, float y) {
		switch (type) {
		case Area.RECT:
			if (x >= coords[0] && x <= coords[2] && y >= coords[1] && y <= coords[3]) {
				return true;
			}
			break;
		case Area.CIRCLE:
			if (Math.sqrt(Math.pow(x - coords[0],2) + Math.pow(y - coords[1],2)) <= coords[2]) {
				return true;
			}
			break;
			// Check whether it lies in the polygon using the ray casting algorithm
			// Take a ray (line) that starts at any point that goes through the point they touched
			// Count how many times this line crosses the edges of the polygon
			// If it is even if outside, odd if inside
			// See http://en.wikipedia.org/wiki/Point_in_polygon
			// Based on code from http://stackoverflow.com/questions/7044838/finding-points-contained-in-a-path-in-android
			// which is based on http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
		case Area.POLY:
			int numSides = coords.length/2;
			boolean in = false;
			int i, j = 0;
			for (i = 0, j = numSides - 1; i < numSides; j = i++) {
				if (((coords[i*2+1] > y) != (coords[j*2+1] > y))
						&& (x < (coords[j*2] - coords[i*2]) * (y - coords[i*2+1]) / (coords[j*2+1] - coords[i*2+1]) + coords[i*2]))
					in = !in;
			}
			return in;
		}
		// If none matched, return false
		return false;
	}
}