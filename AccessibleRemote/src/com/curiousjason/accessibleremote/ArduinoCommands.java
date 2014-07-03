package com.curiousjason.accessibleremote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ArduinoCommands {

	public final String TAG = "ARDUINOCOMMANDS";

	private String[] commandName;
	private int[] commandNumber;
	private int[] initialValue;
	private int[] minValue;
	private int[] maxValue;
	private int[] currentValue;

	public ArduinoCommands(JSONObject inputObject) {
		try {
			JSONObject arduino = inputObject.getJSONObject("arduino");
			JSONArray arduinoCommands = arduino.getJSONArray("commands");
			int numCommands = arduinoCommands.length();
			Log.v(TAG,"There are " + numCommands + " arduino commands");
			commandName = new String[numCommands];
			commandNumber = new int[numCommands];
			initialValue = new int[numCommands];
			minValue = new int[numCommands];
			maxValue = new int[numCommands];
			currentValue = new int[numCommands];


			for (int i=0; i < numCommands; i++) {
				JSONObject thisCommand = arduinoCommands.getJSONObject(i);
				commandName[i] = thisCommand.getString("name");
				commandNumber[i] = thisCommand.getInt("command");
				initialValue[i] = thisCommand.getInt("initialvalue");
				minValue[i] = thisCommand.getInt("minvalue");
				maxValue[i] = thisCommand.getInt("maxvalue");
				currentValue[i] = initialValue[i];
				Log.v(TAG,"Read command " + commandName[i] + 
						" (number " + commandNumber[i] + ") with initial value " + 
						initialValue[i]);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public ArduinoCommand parseCommand(String address,boolean isup) {
		// Parse the string
		ArduinoCommand a = null;
		
		Pattern p3 = Pattern.compile("^(.*)/(.*)\\.(.*)");
		Matcher m3 = p3.matcher(address);
		if (m3.matches()) {
			String command1String = m3.group(1);			
			String command2String = m3.group(2);
			int command1 = -1;
			int command2 = -1;
			for (int i=0; i<commandName.length; i++) {
				//Log.v(TAG,"commandName[i] is " + commandName[i]);
				//Log.v(TAG,"commandString is " + commandString);
				if (commandName[i].equals(command1String)) {
					//Log.v(TAG,"Arduino command matches " + commandName[i] + " = " + i);
					command1 = i;
				}
				if (commandName[i].equals(command2String)) {
					//Log.v(TAG,"Arduino command matches " + commandName[i] + " = " + i);
					command2 = i;
				}
			}
			if (isup) {
				if (command2==-1) {
					Log.e(TAG,"Could not find arduino command with name " + command2String);
					return a;
				}
				else a = applyCommand(command2,0,false);
			} else {
				if (command1==-1) {
					Log.e(TAG,"Could not find arduino command with name " + command1String);
					return a;
				}
				else a = applyCommand(command1,0,false);
			}

		}

		Pattern p = Pattern.compile("^(.*)/(.*)$");
		Matcher m = p.matcher(address);
		boolean relative = false;

		if (m.matches()) {
			String commandString = m.group(1);			
			String valueString = m.group(2);
			int command = -1;
			int value = -1;
			for (int i=0; i<commandName.length; i++) {
				//Log.v(TAG,"commandName[i] is " + commandName[i]);
				//Log.v(TAG,"commandString is " + commandString);
				if (commandName[i].equals(commandString)) {
					//Log.v(TAG,"Arduino command matches " + commandName[i] + " = " + i);
					command = i;
				}
			}
			if (command==-1) {
				Log.e(TAG,"Could not find arduino command with name " + commandString);
				return a;
			}
			
			Pattern p2 = Pattern.compile("^([\\+-][0-9]*)$");
			Matcher m2 = p2.matcher(valueString);
			int multiplyby = 1;
			if (m2.matches()) {
				relative = true;
				if (valueString.charAt(0)=='-') 
					multiplyby = -1;				
				valueString = valueString.substring(1);
				
			}
			try {
				value = Integer.parseInt(valueString);
				a = applyCommand(command,multiplyby * value,relative);
			}
			catch(NumberFormatException nfe) {
				System.out.println("Could not parse " + valueString);
			} 
		} else	{
			Log.v(TAG,"arduino command not in correct format: " + address);
		}
		return a;

	}

	/** 
	 * Apply the ardunio command
	 * @param command Command number (the command number defined in the JSON file)
	 * @param value The argument
	 * @param relative Where this is a relative command (i.e. +1 or -1, rather than a fixed value)
	 * @return An Arduino command
	 */
	ArduinoCommand applyCommand(int command,int value,boolean relative) {
		if (relative) {
			currentValue[command] = currentValue[command] + value;
		} else {
			currentValue[command] = value;
		}
		if (currentValue[command] > maxValue[command])
			currentValue[command] = maxValue[command];
		if (currentValue[command] < minValue[command])
			currentValue[command] = minValue[command];

		return new ArduinoCommand(commandNumber[command],currentValue[command]);
	}
}
