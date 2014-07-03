package com.curiousjason.accessibleremote;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import mcplib.general.ICommands;
import mcplib.general.Utils.DeviceOpcode;
import mcplib.server.MCPServer;

public class NetworkServer extends Service implements ICommands {

	//private final int WHICHDEVICE = 0;
	private final int ARMROTATE = 1;
	private final int GRIPPERUPDOWN = 2;
	private final int GRIPPERLEFTRIGHT = 3;
	private final int GRIPPERROTATE = 4;
	private final int ARM = 5;
	private final int GRIPPER = 6;
	private final int SETINFOLDEDPOSITION = 10; // STOP?

	private final int ZOOM = 20;
	private final int SNAP = 21;
	private final int CAMERAPOWER = 22;
	
	//private final int PLAYMIDIBUFFER = 30;

	private final String TAG = "NETWORKSERVER";
	
	private final String COMMANDRECEIVEDSENT = "com.curiousjason.AccessibleRemote.CommandReceivedSent";
	
	MCPServer m_mcpServer = null;
	
	public AccessoryConnector mBoundServiceArduino;
	public boolean mIsBoundArduino = false;
	
	@Override
	public void onCreate() {
		// Bind to the AccessoryConnector
		doBindServiceArduino();
		
		if (m_mcpServer==null) {
			m_mcpServer = new MCPServer(DeviceOpcode.RoboticArm, this);
			m_mcpServer.start();
			Log.v(TAG,"Network server created!");
		}
	}
	
	@Override
	public void onDestroy() {
		// Unbind to the AccessoryConnector (to stop leaking)
		doUnbindServiceArduino();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	// This is the object that receives interactions from clients.  
	private final IBinder mBinder = new LocalBinder();

	
	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		NetworkServer getService() {
			return NetworkServer.this;
		}
	}

	void sendCommand(int command, int value) {
		ArduinoCommand ac = new ArduinoCommand(command,value);
		mBoundServiceArduino.sendCommand(ac.commandNumber,ac.value);
		Log.v(TAG,"Forwarded command: " + command + "," + value);
		
		// Also send an intent to the screen
		Intent intent = new Intent(COMMANDRECEIVEDSENT);
		intent.putExtra("commandNumber", ac.commandNumber);
		intent.putExtra("commandNumber", ac.value);
		sendBroadcast(intent);
		}
	
	@Override
	public void ZoomIn() {
		sendCommand(ZOOM,1);
	}

	@Override
	public void ZoomOut() {
		sendCommand(ZOOM,-1);		
	}

	@Override
	public void armDown() {
		armDown(ROTATION_DEGREE);
	}

	@Override
	public void armDown(int angle) {
		sendCommand(ARM,-angle);		
	}
	
	@Override
	public void armUp() {
		armUp(ROTATION_DEGREE);		
	}

	@Override
	public void armUp(int angle) {
		sendCommand(ARM,angle);				
	}

	@Override
	public void armRotateLeft() {
		armRotateLeft(ROTATION_DEGREE);
	}

	@Override
	public void armRotateLeft(int angle) {
		sendCommand(ARMROTATE,-angle);				
	}

	@Override
	public void armRotateRight() {
		armRotateRight(ROTATION_DEGREE);
	}

	@Override
	public void armRotateRight(int angle) {
		sendCommand(ARMROTATE,angle);								
	}


	@Override
	public void gripperClose() {
		sendCommand(GRIPPER,1);
	}
	
	@Override
	public void gripperOpen() {
		sendCommand(GRIPPER,0);
	}


	@Override
	public void gripperMoveDown() {
		sendCommand(GRIPPERUPDOWN,-ROTATION_DEGREE);						
	}
	
	@Override
	public void gripperMoveUp() {
		sendCommand(GRIPPERUPDOWN,ROTATION_DEGREE);						
	}

	@Override
	public void gripperMoveLeft() {
		sendCommand(GRIPPERLEFTRIGHT,-ROTATION_DEGREE);						
	}

	@Override
	public void gripperMoveRight() {
		sendCommand(GRIPPERLEFTRIGHT,ROTATION_DEGREE);								
	}

	@Override
	public void gripperRotateLeft() {
		gripperRotateLeft(ROTATION_DEGREE);
	}

	@Override
	public void gripperRotateLeft(int angle) {
		sendCommand(GRIPPERROTATE,-angle);										
	}

	@Override
	public void gripperRotateRight() {
		gripperRotateRight(ROTATION_DEGREE);
	}

	@Override
	public void gripperRotateRight(int angle) {
		sendCommand(GRIPPERROTATE,angle);										
	}

	@Override
	public void playMidiBuffer(short[] buffer) {
		// Do nothing												
	}

	@Override
	public void setInAFoldedPosition() {
		sendCommand(SETINFOLDEDPOSITION,0);												
	}

	@Override
	public void snap() {
		sendCommand(SNAP,0);												
	}

	@Override
	public void turnCameraOff() {
		sendCommand(CAMERAPOWER,0);												
	}

	@Override
	public void turnCameraOn() {
		sendCommand(CAMERAPOWER,1);												
	}
	


	void doBindServiceArduino() {
		// Establish a connection with the service for communicating with the arduino.
		// We use an explicit class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		Log.v(TAG,"About to bind arduino service (from Network server)");
		bindService(new Intent(NetworkServer.this, 
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

	private ServiceConnection mConnectionArduino = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mBoundServiceArduino = ((AccessoryConnector.LocalBinder)service).getService();
			Log.v(TAG,"Arduino service is connected (from Network server)");
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mBoundServiceArduino = null;
		}
	};

	
}
