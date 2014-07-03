package com.curiousjason.accessibleremote;

import java.io.File;
import java.io.FileFilter;

import com.curiousjason.accessibleremote.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.support.v4.content.LocalBroadcastManager;

public class RemotePicker extends Activity {

	final String TAG = "REMOTEPICKER";
	private static final String ACTION_USB_PERMISSION = "com.curiousjason.accessiblekeyboard.action.USB_PERMISSION";
	public static final String MAINSCREEN_DESTROYED = "com.curiousjason.accessiblekeyboard.action.MAINSCREEN_DESTROYED";

	String[] remoteFilenames; // filenames of the remotes

	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbManager mUsbManager;
	UsbAccessory mAccessory = null;
	public boolean accessoryOpen = false;

	private AccessoryConnector mBoundService;
	boolean mIsBound = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.remotepicker_screen);

		// Register to receive USB Accessory connected events
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		GridView gv = (GridView) findViewById(R.id.remotepicker);

		File f = new File(getExternalFilesDir(null), "/maps/"); 

		File[] files = f.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname)
			{
				String fileName = pathname.getName();
				int mid= fileName.lastIndexOf(".");
				String fileExtension = fileName.substring(mid+1,fileName.length());
				Log.v(TAG,"Comparing file extension of " + fileExtension);
				if (fileExtension.contentEquals("json"))
					return true;
				else
					return false;
			}


		});


		if (files==null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.noremotes_message)
			.setTitle(R.string.noremotes_title);
			builder.setPositiveButton(R.string.ok, null);
			AlertDialog dialog = builder.create();
			dialog.show();

		} else 	{
			String[] thumbnailFilenames = new String[files.length];
			remoteFilenames = new String[files.length];
			for (int k=0;k<files.length;k++) {
				// These are the names of the json files, we want the names of the thumbnails
				remoteFilenames[k] = files[k].getName();
				Log.v(TAG,"Requesting quick remote for file " + remoteFilenames[k]);
				RemoteControl r = new RemoteControl(this,remoteFilenames[k],true);				
				thumbnailFilenames[k] = r.thumbnailFilename;
			}
			gv.setAdapter(new RemoteImagesAdapter(this,thumbnailFilenames));

			gv.setOnItemClickListener(new OnItemClickListener() { 
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) { 
					Log.v(TAG, "Selection: " + position);
					Intent myIntent = new Intent(RemotePicker.this, MainScreen.class);
					myIntent.putExtra("RemoteControlFilename", remoteFilenames[position]); //Optional parameters
					startActivity(myIntent);

				} 
			}); 

		}		
		doBindService();
		Log.v(TAG,"started USB accessory service");
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mIsBound && 
				mBoundService.mInputStream != null && 
				mBoundService.mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				mAccessory = accessory;
				doBindService();
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}

		// Register the broadcast receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(mActivityDestroyed,new IntentFilter(MAINSCREEN_DESTROYED));
	}
	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityDestroyed);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUsbReceiver);
		doUnbindService();
	}

	public boolean isAccessoryOpen() {
		return accessoryOpen;
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
	        mBoundService = ((AccessoryConnector.LocalBinder)service).getService();
	        Log.v(TAG,"Service is bound to " + mBoundService);
			mIsBound = true;
	        if (!accessoryOpen && mAccessory != null)
	        	openAccessory(mAccessory);
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
	        mBoundService = null;
	    }
	};


	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(RemotePicker.this, 
				AccessoryConnector.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG,"in BroadcastReceiver, about to open accessory");
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				synchronized (this) {
					mAccessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (mAccessory!=null && intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(mAccessory);
						Log.d(TAG,"in BroadcastReceiver, opened accessory");
					} else {
						Log.d(TAG, "permission denied for accessory " + mAccessory);
					}
					//mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					mBoundService.closeAccessory();
					accessoryOpen = false;
				}
			}
		}
	};

	// This receives a message when the child activity has been destroyed
	private final BroadcastReceiver mActivityDestroyed = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG,"Received message that child destroyed");
		}
	};

	// Pass the work of opening the accessory to the service
	private void openAccessory(UsbAccessory accessory) {
		if (mIsBound)
        	mBoundService.openAccessory(mAccessory);
		else
			doBindService();
	}



}

