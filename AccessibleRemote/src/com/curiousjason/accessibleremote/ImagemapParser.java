package com.curiousjason.accessibleremote;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/** 
 * Parses an imagemap
 * @author Jason Friedman
 * Based on the demo in the android training site
 * http://developer.android.com/training/basics/network-ops/xml.html
 * */

public class ImagemapParser {
	private final String TAG = "IMAGEMAPPARSER";

    private static final String ns = null;
    
    public Imagemap parse(InputStream in, Context context) throws XmlPullParserException, IOException {
        Imagemap i = new Imagemap(null,null);
    	try {
        	XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        	factory.setValidating(false);
        	factory.setFeature(Xml.FEATURE_RELAXED, true);
        	XmlPullParser parser = factory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in,null);
            i=readXML(parser,context);
        }
        finally {
        	in.close();
        }
    	return i;
    }
    
    private Imagemap readXML(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
        String imageDetails = "";
    	List<Area> areas = new ArrayList<Area>();
    	
    	Log.v(TAG,"In readXML");
    	
    	int loopcount = 0;
    	int finished=0;

    	while(finished<2 && loopcount<20) {
    		parser.next();
    		loopcount++;
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("img")) {
                imageDetails = readImageDetails(parser,context);
                finished++;
            } else if (name.equals("map")) {
            	areas = readMap(parser,context);
            	finished++;
            }
            else {
            	Toast.makeText(context, "Unknown tag at base level: " + name,Toast.LENGTH_LONG).show();
            	Log.v(TAG,"Unknown tag at base level: " + name);
            	skip(parser);
            }
        }  
        
        Imagemap map = new Imagemap(imageDetails,areas);
        return map;
    }
    
 // Parses the contents of the <img> tag 
 private String readImageDetails(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
     Log.v(TAG,"Parsing image details");
     String src = "";
	 parser.require(XmlPullParser.START_TAG, ns, "img");
     if (parser.next() != XmlPullParser.END_TAG) {
   	 	Log.v(TAG,"There should not be a tag inside an area");
     }
     for(int i=0;i<parser.getAttributeCount();i++) {
    	 if (parser.getAttributeName(i).equalsIgnoreCase("src")) {
    			 src = parser.getAttributeValue(i);
    	 }
     }
     return src;
 }
 
// parse the <map> tag
 private List<Area> readMap(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
     Log.v(TAG,"Parsing map details");
	 List<Area> areas = new ArrayList<Area>();

 	parser.require(XmlPullParser.START_TAG, ns, "map");
 	while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            continue;
        }
    	areas.add(readArea(parser,context));
    }
 	Log.v(TAG,"Finished map");
	return areas;
 }
 
 private Area readArea(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
	 
	 int type = -1;
	 int[] coords = {0};
	 String href = "";
	 String alt = "";
	 String title = "";
	 

     Log.v(TAG,"Parsing area details");
  	 parser.require(XmlPullParser.START_TAG, ns, "area");
  	 if (parser.next() != XmlPullParser.END_TAG) 
  	 	Log.v(TAG,"There should not be a tag inside an area");
  	 for(int i=0;i<parser.getAttributeCount();i++) {
  		 if (parser.getAttributeName(i).equalsIgnoreCase("shape")) {
  			 if (parser.getAttributeValue(i).equalsIgnoreCase("rect"))
  				 type = Area.RECT;
  			 else if (parser.getAttributeValue(i).equalsIgnoreCase("circle"))
  				 type = Area.CIRCLE;
  			 else if (parser.getAttributeValue(i).equalsIgnoreCase("poly"))
  				type = Area.POLY;
  			 else
  				 Log.e(TAG,"Unknown shape: " + parser.getAttributeValue(i));		 	
  		 }
  		 else if (parser.getAttributeName(i).equalsIgnoreCase("coords")) {
  			String items[] = parser.getAttributeValue(i).split("\\s*,\\s*");
  			coords = new int[items.length];

  			for (int j = 0; j < items.length; j++) {
  			    try {
  			        coords[j] = Integer.parseInt(items[j]);
  			    } catch (NumberFormatException nfe) {};
  			}

  		 }
  		 else if (parser.getAttributeName(i).equalsIgnoreCase("href")) {
  			 href = parser.getAttributeValue(i);
  		 }
  		 else if (parser.getAttributeName(i).equalsIgnoreCase("alt")) {
  			 alt = parser.getAttributeValue(i);
  		 }
  		 else if (parser.getAttributeName(i).equalsIgnoreCase("title")) {
  			title = parser.getAttributeValue(i);
  		 }
  	 }
  	return new Area(type,coords,href,alt,title);
 }
 
 private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	    	Log.e(TAG,"Got " + parser.getName() + " with " + parser.getText() + " instead of start tag");
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }

 
 }
