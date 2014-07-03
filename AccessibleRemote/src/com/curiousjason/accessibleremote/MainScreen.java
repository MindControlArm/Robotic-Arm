package com.curiousjason.accessibleremote;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.inputstick.api.ConnectionManager;
//import com.inputstick.api.ConnectionManager;
import com.inputstick.api.InputStickKeyboardListener;
import com.inputstick.api.InputStickStateListener;
//import com.inputstick.api.basic.InputStickConsumer;
import com.inputstick.api.basic.InputStickHID;
import com.inputstick.api.basic.InputStickKeyboard;
import com.curiousjason.accessibleremote.R;
import com.curiousjason.accessibleremote.RemoteControl.OutputTypes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Toast;

/**
 * Main screen of the application, where the interaction with the remote takes place
 * @author Jason Friedman
 *
 */
public class MainScreen extends Activity implements InputStickStateListener, InputStickKeyboardListener {

	final String TAG = "ACCESSIBLEREMOTE";

	RemoteControl r = null;

	private Bitmap theImage = null;
	private Rect theImageRect = null;
	private boolean[] pressedImages = null;
	private Bitmap[] pressedImagesBitmaps = null;
	private Rect[] pressedImagesRect = null;
	private Point[] pressedImagesLocation = null;

	SurfaceHolder surfaceHolder = null;


	boolean touching[] = {false,false,false,false,false,false,false,false,false,false};

	// size of the bitmap
	int pictureWidth = 0; 
	int pictureHeight = 0;

	// Size of the view it is drawn into
	int drawnWidth = 0;
	int drawnHeight = 0;

	// size of the bitmap
	int originalWidth = 0; 
	int originalHeight = 0;

	public static final String BUTTON_PRESSED="ButtonPressed";

	OrientationEventListener myOrientationEventListener;

	static Context applicationContext;

	public AccessoryConnector mBoundServiceArduino;
	public boolean mIsBoundArduino = false;

	public NetworkServer mBoundServiceNetworkServer;
	public boolean mIsBoundNetworkServer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String filename = intent.getStringExtra("RemoteControlFilename"); //if it's a string you stored.
		Log.v(TAG,"Received remote control filename:" + filename);

		applicationContext = getApplicationContext();
		setContentView(R.layout.activity_main_screen);

		r = new RemoteControl(this,filename);
		// TODO: If there is an error, should probably stop here
		if (r.errorMessage!=null) {
			Toast.makeText(this, "Error: " + r.errorMessage,Toast.LENGTH_LONG).show();
		}

		if (r.isInputType(RemoteControl.InputTypes.IMAGEMAP)) 
			setupImagemap();
		if (r.isInputType(RemoteControl.InputTypes.NETWORKSERVER))
			setupNetworkServer();
		if (r.usesInputStick) {	
			int state = InputStickHID.getState();
			switch (state) {
			case ConnectionManager.STATE_CONNECTED:
			case ConnectionManager.STATE_CONNECTING:
			case ConnectionManager.STATE_READY:
				// do nothing
				break;
			case ConnectionManager.STATE_DISCONNECTED:
			case ConnectionManager.STATE_FAILURE:	
				// connect
				InputStickHID.connect(MainScreen.this.getApplication());						
				break;											
			}										

		}
		if (r.usesArduino) {
			doBindServiceArduino();
		}

	}
	
	public void buttonPressedUp(String value) {
		buttonPressed(value,true);
	}
	
	public void buttonPressed(String value) {
		buttonPressed(value,false);
	}

	public void buttonPressed(String value,boolean isup) {
		Log.v(TAG,"Parsing output string " + value);
		// Parse the string
		Pattern p = Pattern.compile("^(.*)://(.*)$");
		Matcher m = p.matcher(value);
		if (m.matches()) {
			String protocol = m.group(1);			
			String address = m.group(2);

			switch (OutputTypes.fromString(protocol)) {
			case TOAST:
				Toast.makeText(this, "Pressed " + address,Toast.LENGTH_LONG).show();
				break;
			case LOG:
				Log.v(TAG,"Pressed " + address);
				break;
			case INPUTSTICK:
				InputStickKeyboard.typeASCII("Hello, world!");
				break;
			case ARDUINO:
				ArduinoCommand ac;
				if (isup)
					ac = r.getArduinoCommandUp(address);
				else
					ac = r.getArduinoCommand(address);

				if (ac!=null)
					mBoundServiceArduino.sendCommand(ac.commandNumber,ac.value); // For now just always trigger the relay
				break;
			case HTTP:
				// TODO: implement this!
				Toast.makeText(this, "HTTP not yet implemented " + value,Toast.LENGTH_LONG).show();
				break;
			case ERROR:
				Toast.makeText(this, "Error decoding " + value,Toast.LENGTH_LONG).show();
				break;
			}

		}
		else
		{
			Log.v(TAG,"href not in correct format: " + value);
		}

	}

	// TODO: Something should happen if it can not longer connect to the USB keyboard
	@Override
	public void onStateChanged(int state) {
	}

	// Don't need this function here
	@Override
	public void onLEDsChanged(boolean numLock, boolean capsLock, boolean scrollLock) {
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (r.usesInputStick) {
			InputStickKeyboard.addKeyboardListener(MainScreen.this);
			InputStickHID.addStateListener(MainScreen.this);
		}
	}

	@Override
	protected void onPause() {		
		super.onPause();
		if (r.usesInputStick) {
			InputStickKeyboard.removeKeyboardListener(this);
			InputStickHID.removeStateListener(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (r.isInputType(RemoteControl.InputTypes.IMAGEMAP)) {
			myOrientationEventListener.disable();
		}
		if (r.usesArduino) {
			//unregisterReceiver(mUsbReceiver);
			doUnbindServiceArduino();
		}
		if (r.isInputType(RemoteControl.InputTypes.NETWORKSERVER))
			doUnbindServiceNetworkServer();
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void setupImagemap()
	{
		Point size = new Point();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){
			getWindowManager().getDefaultDisplay().getSize(size);

			pictureWidth = size.x;
			pictureHeight = size.y; 
		}else{
			Display d = getWindowManager().getDefaultDisplay(); 
			pictureWidth = d.getWidth(); 
			pictureHeight = d.getHeight(); 
		}

		// Set the image
		final SurfaceView s = (SurfaceView) findViewById(R.id.fullscreen_content);
		String imageFilename = r.getImagemap().filename;
		BitmapWithSize b = loadBitmap(this,imageFilename,pictureWidth,pictureHeight);
		theImage = b.bitmap;
		originalWidth = b.width;
		originalHeight = b.height;

		theImageRect = new Rect(0,0,b.width,b.height);

		surfaceHolder = s.getHolder();
		surfaceHolder.addCallback(new SurfaceHolder.Callback(){

			@Override
			public void surfaceCreated(SurfaceHolder surfaceHolder) {
				drawBitmaps();
				// Remove the callback
				surfaceHolder.removeCallback(this);
			}

			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
					int arg3) {

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {

			}
		});


		//s.setBackground(new BitmapDrawable(getResources(),theImage));

		// Load the pressed images
		pressedImages = new boolean[r.getNumberPressed()];
		pressedImagesBitmaps = new Bitmap[r.getNumberPressed()];
		pressedImagesRect = new Rect[r.getNumberPressed()];
		pressedImagesLocation = new Point[r.getNumberPressed()];

		for (int i=0; i<r.getNumberPressed(); i++) {
			//BitmapWithSize b1 = loadBitmap(this,r.getPressedImageFilename(i),pictureWidth,pictureHeight);
			BitmapWithSize b1 = loadBitmap(this,r.getPressedImageFilename(i),-1,-1);
			pressedImagesBitmaps[i] = b1.bitmap;
			pressedImagesRect[i] = new Rect(0,0,b1.width,b1.height);
			pressedImagesLocation[i] = r.getPressedLocation(i);
			
			Log.v(TAG,"Created pressed bitmap " + i + " with size (" + b1.width + "," + b1.height + ") "
					+ "at location (" + pressedImagesLocation[i].x + "," + pressedImagesLocation[i].y +")");		
			pressedImages[i] = false;
		}

		myOrientationEventListener = new OrientationEventListener(this.getApplicationContext()) {
			@Override
			public void onOrientationChanged(int orientation) {
				Log.v(TAG,"Orientation is " + orientation);
			}
		};

		// Measure the size when the object gets placed
		s.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@SuppressLint("NewApi")
			@Override
			public void onGlobalLayout() {
				//now we can retrieve the width and height
				drawnWidth = s.getWidth();
				drawnHeight = s.getHeight();
				Log.v(TAG,"Width is " + drawnWidth + ", height is " + drawnHeight);
			}

		});
		s.setOnTouchListener(mTouchListener);
	}

	void setupNetworkServer() {
		doBindServiceNetworkServer();
	}

	// TODO check if this is causing leaking
	static Context getContext()
	{
		return applicationContext;
	}

	static public BitmapWithSize loadBitmap(Context context, String imageFilename, int pictureWidth, int pictureHeight) {
		BitmapWithSize theImage;
		File selectedImageFile = new File(context.getExternalFilesDir(null),"maps/" + imageFilename);
		Uri selectedImage = Uri.fromFile(selectedImageFile);
		String backgroundImageString = selectedImage.toString();

		Uri imageURI = Uri.parse(backgroundImageString);

		try {
			theImage = decodeUri(context, imageURI,pictureWidth);
		} catch (FileNotFoundException e1) {
			Log.w("LOADBITMAP", e1);
			theImage = new BitmapWithSize(Bitmap.createBitmap(pictureWidth,pictureHeight,Bitmap.Config.RGB_565),
					pictureWidth,pictureHeight);
		}
		return theImage;
	}

	// Resample image to avoid out of memory problems
	// (from http://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app-in-android )
	static private BitmapWithSize decodeUri(Context context, Uri selectedImage, int requiredSize) throws FileNotFoundException {

		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(context.getContentResolver().openInputStream(selectedImage), null, o);

		// Find the correct scale value. It should be the power of 2.
		int width_tmp = o.outWidth, height_tmp = o.outHeight;


		int scale = 1;
		if (requiredSize>-1) {
			while (true) {
				if (width_tmp / 2 < requiredSize
						|| height_tmp / 2 < requiredSize) {
					break;
				}
				width_tmp /= 2;
				height_tmp /= 2;
				scale *= 2;
			}
		}

		// Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;
		o2.inJustDecodeBounds = true;
		o2.inPreferredConfig = Bitmap.Config.RGB_565; // Uses less memory
		BitmapFactory.decodeStream(context.getContentResolver().openInputStream(selectedImage), null, o2);
		Log.w("DECODEURI","Image width = " + o2.outWidth + ", height = " + o2.outHeight);

		o2.inJustDecodeBounds = false;

		return new BitmapWithSize(BitmapFactory.decodeStream(context.getContentResolver().openInputStream(selectedImage), null, o2),
				o.outWidth,o.outHeight);

	}

	/**
	 * Touch listener 
	 */

	View.OnTouchListener mTouchListener = new View.OnTouchListener() {
		@SuppressLint("NewApi")
		@Override
		public boolean onTouch(View view, MotionEvent ev) {
			final int pointerCount = ev.getPointerCount();
			/*
			final int historySize = ev.getHistorySize();
			for (int h = 0; h < historySize; h++) {
				// long time = ev.getHistoricalEventTime(h);
				for (int p=0; p<pointerCount;p++) {
					float x = ev.getHistoricalX(p,h);
					float y = ev.getHistoricalY(p,h);
					// Do something with the historical x/y data here
				}
			} */
			//				long time = ev.getEventTime();
			int action = ev.getActionMasked();
			int actionIndex = ev.getActionIndex();

			switch(action){
			case MotionEvent.ACTION_DOWN:
				touching[ev.getPointerId(actionIndex)] = true;
				for (int p=0; p<pointerCount;p++) {
					// Convert from screen (view) coordinates to picture coordinates
					float x = ev.getX(p) / drawnWidth * originalWidth;
					float y = ev.getY(p) / drawnHeight * originalHeight;
					//Log.v(TAG,"Checking touched point x=" + x + ", y=" + y);
					// Loop through the areas to see if any are touched
					Area matchedArea = r.getImagemap().contains(x,y);
					if (matchedArea!=null) {
						// Change the image to the "pressedImage" if it exists
						int pressedImage = r.whichPressedImage(matchedArea.title);
						if (pressedImage>=0) {
							pressedImages[pressedImage] = true;
							drawBitmaps();
						}
						// Do the output action
						buttonPressed(matchedArea.href);
						break;
					}

				}
				break;
			case MotionEvent.ACTION_MOVE:
				touching[ev.getPointerId(actionIndex)] = true;
				break;
			case MotionEvent.ACTION_UP:
				/*
				for (int p=0; p<pointerCount;p++) {
					// Convert from screen (view) coordinates to picture coordinates
					float x = ev.getX(p) / drawnWidth * originalWidth;
					float y = ev.getY(p) / drawnHeight * originalHeight;
					//Log.v(TAG,"Checking touched point x=" + x + ", y=" + y);
					// Loop through the areas to see if any are touched
					Area matchedArea = r.getImagemap().contains(x,y);
					if (matchedArea!=null) {
						// Change the image to the "pressedImage" if it exists
						int pressedImage = r.whichPressedImage(matchedArea.title);
						if (pressedImage>=0) {
							pressedImages[pressedImage] = true;
							drawBitmaps();
						}
						// Do the output action
						buttonPressedUp(matchedArea.href);
						break;
					}
				}
				break;*/
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				touching[ev.getPointerId(actionIndex)] = false;
				for (int i=0;i<pressedImages.length;i++) {
					pressedImages[i] = false;
					drawBitmaps();
				}
				break;
			default: touching[ev.getPointerId(actionIndex)] = false;

			}


			return true;
		}
	};

	/**
	 * Draw bitmaps according to the current state
	 */
	void drawBitmaps() {
		// First draw the background image
		Canvas canvas = surfaceHolder.lockCanvas();
		final Rect dest = new Rect(0,0,pictureWidth,pictureHeight);
		canvas.drawBitmap(theImage,theImageRect,dest,null);

		for(int i=0;i<pressedImages.length;i++) {
			if (pressedImages[i]) {
				Rect srcP = pressedImagesRect[i];
				// Correct for actual size of image
				int left = pressedImagesLocation[i].x *  drawnWidth / originalWidth;
				int top = pressedImagesLocation[i].y * drawnHeight / originalHeight;
				int right = (srcP.right + pressedImagesLocation[i].x) * drawnWidth / originalWidth;
				int bottom =  (srcP.bottom + pressedImagesLocation[i].y) * drawnHeight / originalHeight;
				Rect destP = new Rect(left,top,right,bottom);
				canvas.drawBitmap(pressedImagesBitmaps[i],null,destP,null);
			}
		}
		surfaceHolder.unlockCanvasAndPost(canvas);
	}
	void doBindServiceArduino() {
		// Establish a connection with the service for communicating with the arduino.
		// We use an explicit class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Log.v(TAG,"About to bind arduino service");
		bindService(new Intent(MainScreen.this, 
				AccessoryConnector.class), mConnectionArduino, Context.BIND_AUTO_CREATE);
		mIsBoundArduino = true;
	}

	void doUnbindServiceArduino() {
		if (mIsBoundArduino) {
			// Detach our existing connection.
			unbindService(mConnectionArduino);
			mIsBoundArduino = false;
		}
	}

	void doBindServiceNetworkServer() {
		// Establish a connection with the service for communicating with the arduino.
		// We use an explicit class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Log.v(TAG,"About to bind network server service");
		bindService(new Intent(MainScreen.this, 
				NetworkServer.class), mConnectionNetworkServer, Context.BIND_AUTO_CREATE);
		mIsBoundNetworkServer = true;
	}

	void doUnbindServiceNetworkServer() {
		if (mIsBoundNetworkServer) {
			// Detach our existing connection.
			unbindService(mConnectionNetworkServer);
			mIsBoundNetworkServer = false;
		}
	}


	private ServiceConnection mConnectionArduino = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundServiceArduino = ((AccessoryConnector.LocalBinder)service).getService();
			Log.v(TAG,"Arduino service is connected");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundServiceArduino = null;
		}
	};

	private ServiceConnection mConnectionNetworkServer = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundServiceNetworkServer = ((NetworkServer.LocalBinder)service).getService();
			Log.v(TAG,"Network server service is connected");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundServiceNetworkServer = null;
		}
	};

	// To receive events from the AccessoryConnector service
	private class Myreceiver extends BroadcastReceiver{
	
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Received intent");
		}
	}
}
