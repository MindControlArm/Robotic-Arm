package com.curiousjason.accessibleremote;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Application;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

public class RemoteControl extends Application {

	private Context context;

	public enum InputTypes {
		IMAGEMAP, // Input is an standard imagemap, the href contains 
		//what to do when the relevant area is selected
		NETWORKSERVER; // Input is over a network connection
	}


	public enum OutputTypes {
		TOAST, // Show the button presses as "toasts" (brief messages), mainly for testing
		LOG,  // Show the button presses in the Android log, mainly for testing
		ARDUINO, // Send the button presses to a connected Arduino
		INPUTSTICK, // Send the button presses via the "InputStick" to simulate a USB keyboard
		HTTP, // Request a URL
		ERROR; // Something is wrong

		public static OutputTypes fromString(String s) {
			try {
				return OutputTypes.valueOf(s.trim().toUpperCase(Locale.US));
			} catch (IllegalArgumentException e1) {
				return ERROR;
			}

		}
	}

	InputTypes[] inputTypes;
	//OutputTypes outputType;

	String errorMessage = null;

	Imagemap imagemap = null;
	String inputFilename = null;

	String[] pressedTitles = null;
	String[] pressedFilenames = null;
	Point[] pressedLocations = null;


	ArduinoCommands arduinoCommands = null;

	public boolean usesArduino = false;
	public boolean usesInputStick = false;

	String thumbnailFilename;

	final String TAG = "REMOTECONTROL";


	public RemoteControl(Context context, String filename) {
		this(context,filename,false);
	}

	// Quick just loads and get the thumbnail
	public RemoteControl(Context context, String filename,boolean quick) {
		this.context = context;

		String fn = context.getExternalFilesDir(null) + "/maps/" + filename;
		File file = new File(fn);
		Log.v(TAG,"Opening file: " + fn);
		InputStream in = null;		   
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			Toast.makeText(context, e1.toString(), Toast.LENGTH_LONG).show();
			return;
		}

		// Put the JSON into a string
		int size;
		byte[] buffer = null;
		try {
			size = in.available();
			buffer = new byte[size];
			in.read(buffer);
			in.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			Toast.makeText(context, e1.toString(), Toast.LENGTH_LONG).show();
			return;
		}

		String remoteJSON = null;
		try {
			remoteJSON = new String(buffer,"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return;
		}
		try {
			// It should be just a simple JSON Object, not an array
			JSONObject result = new JSONObject(remoteJSON);
			String name = result.getString("name");
			Log.v(TAG,"The name from the JSON is " + name);
			thumbnailFilename = result.getString("thumbnail");
			Log.v(TAG,"The thumbname from the JSON is " + thumbnailFilename);

			if (!quick) {
				parseInputs(result);
			
				if (!result.isNull("pressedImages")) {
					parsePressedImages(result);
				} else {
					Log.v(TAG,"There are no pressedImages in the JSON file");
				}

				if (!result.isNull("arduino")) {
					parseArduino(result);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void parseInputs(JSONObject result) throws JSONException {

		JSONArray inputObjects = result.getJSONArray("input");
		inputTypes = new InputTypes[inputObjects.length()];
		for (int i=0;i<inputObjects.length();i++) {
			JSONObject inputObject = inputObjects.getJSONObject(i);
			String inputType = inputObject.getString("type");
			Log.v(TAG,"The input type from the JSON is " + inputType);
			if (inputType.equals("imagemap")) {
				inputTypes[i] = parseImageMap(inputObject);
			} else if (inputType.equals("networkserver")) { 
				inputTypes[i] = parseNetworkServer(inputObject);
			} else {
				errorMessage = "Unknown input type: " + inputType.toString();
			}
			Log.v(TAG,"Input type is " + inputTypes[i]);
		}
	}


	private InputTypes parseImageMap(JSONObject inputObject) throws JSONException{
		InputTypes inputType = InputTypes.IMAGEMAP;
		inputFilename = inputObject.getString("filename");
		Log.v(TAG,"The imagemap filename from the JSON is " + inputFilename);

		ImagemapParser parser = new ImagemapParser();
		String fn = context.getExternalFilesDir(null) + "/maps/" + inputFilename;
		File file = new File(fn);
		Log.e(TAG,"Opening file: " + fn);
		InputStream in = null;		   
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return inputType;
		}

		try {
			imagemap = parser.parse(in,context);

		} catch (XmlPullParserException e1) {
			e1.printStackTrace();
			Toast.makeText(context, e1.toString(), Toast.LENGTH_LONG).show();
			return inputType;
		} catch (IOException e1) {
			e1.printStackTrace();
			return inputType;
		}

		String[] protocolsList = imagemap.getProtocols();

		for (int i = 0;i<protocolsList.length;i++) {
			switch (OutputTypes.fromString(protocolsList[i])) {

			case HTTP:
				Log.v(TAG,"There are href links in the imagemap");
				break;
			case ARDUINO:
				Log.v(TAG,"There are arduino links in the imagemap");
				usesArduino = true;
				break;
			case INPUTSTICK:
				Log.v(TAG,"There are inputstick links in the imagemap");
				usesInputStick = true;
				break;
			case LOG:
				Log.v(TAG,"There are log links in the imagemap");
				break;					
			case TOAST:
				Log.v(TAG,"There are toast links in the imagemap");
				break;
			case ERROR:
			default:
				Log.v(TAG,"Unknown output protocol "+ protocolsList[i]);
			} 
		}
		return inputType;

	}
	
	private InputTypes parseNetworkServer(JSONObject inputObject){
		return InputTypes.NETWORKSERVER;
	}


	private void parsePressedImages(JSONObject inputObject) {
		try {
			JSONArray pressedImagesArray = inputObject.getJSONArray("pressedImages");
			int numPressed = pressedImagesArray.length();
			Log.v(TAG,"There are " + numPressed + " pressedImages");
			pressedTitles = new String[numPressed];
			pressedFilenames = new String[numPressed];
			pressedLocations = new Point[numPressed];


			for (int i=0; i < numPressed; i++) {
				JSONObject thisPressedImage = pressedImagesArray.getJSONObject(i);
				pressedTitles[i] = thisPressedImage.getString("title");
				pressedFilenames[i] = thisPressedImage.getString("filename");
				if (thisPressedImage.isNull("location")) 
					pressedLocations[i] = new Point(0,0);
				else {
					String location = thisPressedImage.getString("location");
					Pattern p = Pattern.compile("^([0-9]*),([0-9]*)$");
					Matcher m = p.matcher(location);

					if (m.matches()) {
						int xval = Integer.parseInt(m.group(1));
						int yval = Integer.parseInt(m.group(2));
						pressedLocations[i] = new Point(xval,yval);
					}
					else
						pressedLocations[i] = new Point(0,0);
					Log.v(TAG,"Parsed pressedLocation " + i + " = (" + pressedLocations[i].x + "," 
						+ pressedLocations[i].y + ")");
				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void parseArduino(JSONObject inputObject) {
		Log.v(TAG,"Parsing arduino commands from JSON");
		arduinoCommands = new ArduinoCommands(inputObject);
	}


	public InputTypes[] getInputType() {
		return inputTypes; 
	}

	//public  getOutputType() {
	//	return outputType; 
	//}

	public Imagemap getImagemap() {
		return imagemap; 
	}

	public int getNumberPressed() {
		if (pressedTitles==null)
			return 0;
		else
			return pressedTitles.length;
	}

	public String getPressedImageFilename(int num) {
		return pressedFilenames[num];
	}
	
	public Point getPressedLocation(int num) {
		return pressedLocations[num];
	}


	/**
	 * Check if there is a different image to display when a button is pressed
	 * (to give visual feedback that the button has been pressed)
	 * @param title The contents of the "title" field in the imagemap xml file
	 * @return The number of the image, if there is one, or -1 otherwise
	 */
	public int whichPressedImage(String title) {
		if (pressedTitles==null)
			return -1;
		for (int i=0; i<pressedTitles.length; i++) {
			if (pressedTitles[i].equals(title)) {
				return i;
			}
		}
		return -1;
	}
	
	public ArduinoCommand getArduinoCommandUp(String address) {
		ArduinoCommand a;
		if (arduinoCommands!=null)
			a = arduinoCommands.parseCommand(address,true);
		else {
			Log.e(TAG,"Cannot parse arduino command because commands are not defined");
			a = null;
		}
		return a;
	}

	
	public ArduinoCommand getArduinoCommand(String address) {
		ArduinoCommand a;
		if (arduinoCommands!=null)
			a = arduinoCommands.parseCommand(address,false);
		else {
			Log.e(TAG,"Cannot parse arduino command because commands are not defined");
			a = null;
		}
		return a;
	}
	
	public boolean isInputType(InputTypes inputType) {
		boolean result = false;
		for(int i=0;i<inputTypes.length;i++) {
			if (inputTypes[i]==inputType)
				result = true;
		}
		return result;
	}
}
