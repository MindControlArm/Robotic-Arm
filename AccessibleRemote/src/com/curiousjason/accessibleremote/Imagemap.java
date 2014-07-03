package com.curiousjason.accessibleremote;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/** 
 * Class representing an imagemap
 * @author Jason Friedman
 */
public class Imagemap {
	String filename;
	List<Area> areas;

	Imagemap(String filename,List<Area> areas) {
		this.filename = filename;
		this.areas = areas;
	}

	/**
	 * Check if a point is contained in any of the shapes
	 * @param x x coordinate (pixels)
	 * @param y y coordinate (pixels)
	 * @return Which area it is contained in (otherwise null)
	 */
	public Area contains(float x,float y) {
		Area matchedArea = null;

		for (Area a:areas) {
			if(a.contains(x,y))
				matchedArea = a;
		}
		return matchedArea;
	}

	/** 
	 * Get the protocols used in the href tags. 
	 * @return String array of the protocols used (e.g. android, log, etc)
	 */
	public String[] getProtocols() {
		// Use a treeset to prevent duplication
		TreeSet<String> protocolsTree = new TreeSet<String>();
		for (Area a : areas) {
			Pattern p = Pattern.compile("(.*)://(.*)");
			Matcher m = p.matcher(a.href);
			if (m.matches()) {
				String protocol = m.group(1);
				protocolsTree.add(protocol);
			}
			else
			{
				Log.v("IMAGEMAP","href not in correct format: " + a.href);
			}
		}
		return protocolsTree.toArray(new String[protocolsTree.size()]);
	}
}
