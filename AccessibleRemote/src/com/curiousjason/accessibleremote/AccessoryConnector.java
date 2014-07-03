package com.curiousjason.accessibleremote;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

/** 
 * This service deals with connecting to the USB accessory (Arduino) 
 * 
 * @author Jason Friedman <write.to.jason@gmail.com>
 */
public class AccessoryConnector extends Service implements Runnable {

	public static final String TAG = "AccessoryConnector";

	public static final byte LED_SERVO_COMMAND = 2;
	public static final byte RELAY_COMMAND = 3;

	protected static final int MESSAGE_SWITCH = 0;
	ParcelFileDescriptor mFileDescriptor;
	public FileInputStream mInputStream;
	public FileOutputStream mOutputStream;
	UsbManager mUsbManager;
	UsbAccessory mAccessory = null;
	public boolean accessoryOpen = false;

	public static boolean[] switches = new boolean[5];

	protected class SwitchMsg {
		private byte sw;
		private byte state;

		public SwitchMsg(byte sw, byte state) {
			this.sw = sw;
			this.state = state;
		}

		public byte getSw() {
			return sw;
		}

		public byte getState() {
			return state;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * Open the USB accessory 
	 * @param accessory The accessory to open
	 * @return Whether the accessory was successfully opened
	 */
	public boolean openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Log.v(TAG, "accessory opened");
			// Run "run" to receive commands
			Thread thread = new Thread(null,this,"ReceiveArduinoMessages");
			thread.start();
			Toast.makeText(getApplicationContext(), "Connected to USB accessory", Toast.LENGTH_LONG).show();
			sendCommand(0,0); // Send a command of "which device?"
			return true;
		} else {
			Log.e(TAG, "accessory open fail");
			return false;
		}
	}
	
	/**
	 * Close the accessory
	 */
	public void closeAccessory() {

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
				Toast.makeText(getApplicationContext(), "Closed USB accessory", Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}


	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		AccessoryConnector getService() {
			return AccessoryConnector.this;
		}
	}


	public void sendCommand(final int command, final int value) {
		if (command<-128 || command>127)
			Log.e(TAG,"Command must be an integer between -128 and 127");
		if (value<-128 || value>127)
			Log.e(TAG,"Value must be an integer between -128 and 127");
		sendCommand((byte) command,(byte) value);
	}

	public void sendCommand(byte command, byte value) {
		byte[] buffer = new byte[3];
		if (value > 255)
			value = (byte) 255;
		buffer[0] = command;
		buffer[1] = value;
		buffer[2] = (byte) 0; // Currently unused, saved for later!
		Log.v(TAG,"Sending command " + command + "," + value + "," + 0);
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
		else
			Log.e(TAG,"Cannot send message to closed output stream in AccessoryConnector");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  
	private final IBinder mBinder = new LocalBinder();

	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	public void run() {
		Log.v(TAG,"Started the run() inside AccessoryConnector");
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {
				case 0x0:
					if (len >= 3) {
						int deviceNum = buffer[i+1];
						//int unused = buffer[i+2];
						Log.v(TAG,"Received response: device " + deviceNum + " connected.");
					}
					i += 3;
					break;

				case 0x1:
					if (len >= 3) {
						//Message m = Message.obtain(mHandler, MESSAGE_SWITCH);
						//m.obj = new SwitchMsg(buffer[i + 1], buffer[i + 2]);
						//switches[buffer[i+1]] = (buffer[i+2]==1?true:false);
						//Intent new_intent = new Intent();
						//new_intent.setAction(MainScreen.BUTTON_PRESSED);
						//sendBroadcast(new_intent);
						//mHandler.sendMessage(m);
					}
					i += 3;
					break;


				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}

	// TODO: Check message received works
		static Handler mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MESSAGE_SWITCH:
					SwitchMsg o = (SwitchMsg) msg.obj;
					handleSwitchMessage(o);
					break;

				}
			}
		};

		static protected void handleSwitchMessage(SwitchMsg o) {
			switches[o.getSw()] = (o.getState()==1?true:false);
	        //Intent new_intent = new Intent();
	        //TODO: To be implemented
	        //new_intent.setAction(MainScreen.BUTTON_PRESSED);
	        //getApplicationContext().sendBroadcast(new_intent);

		}
	 
}
