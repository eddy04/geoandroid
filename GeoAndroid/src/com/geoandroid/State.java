package com.geoandroid;

import java.util.Hashtable;

public class State {
	private final static State INSTANCE = new State();
	
	public static String user = "user";
	public static String password = "pass";
	public static boolean connected = false;
	public static String positionSource = "GPS";
	public static String latitude = "";
	public static String longitude = "";
	public static String seconds = "";
	public static String minutes = "";
	public static boolean autoAlert = false;
	public static boolean mapAlert = false;
	public static boolean urlMapAlert = false;
	public static int alertWidth = 0;
	public static int alertHeight = 0;
	public static int alertTop = 0;
	public static int alertLeft = 0;

	public static Hashtable recordMaps = new Hashtable();
	public static int zoom = 8000;
	public static int connectionPeriod = 2000;
	public static String secureId = "";

	private static StringBuffer log = new StringBuffer("");

	private State() {}
	
	public static State getInstance()
	{
		return INSTANCE;
	}
	
	public synchronized static void addToLog(String text)
	{
		log.append(text + "\n");
	}
	
	public static String getLog()
	{
		return log.toString();
	}
	
	public static void resetLog()
	{
		log.delete(0, log.length() - 1);
	}
	
	// Load stored state
	public void loadConfiguration() throws Exception
	{

	}
	
	private void processRecord(byte[] record, int typeId)
	{
		// Each record type has a different id
		switch(typeId)
		{
		case RecordTypes.ALERTHEIGHT:
			State.alertHeight = bytesToInteger(record);
			break;
		case RecordTypes.ALERTLEFT:
			State.alertLeft = bytesToInteger(record);
			break;
		case RecordTypes.ALERTTOP:
			State.alertTop = bytesToInteger(record);
			break;
		case RecordTypes.ALERTWIDTH:
			State.alertWidth = bytesToInteger(record);
			break;
		case RecordTypes.AUTOALERT:
			State.autoAlert = bytesToBoolean(record);
			break;
		case RecordTypes.LATITUDE:
			State.latitude = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.LONGITUDE:
			State.longitude = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.MAPALERT:
			State.mapAlert = bytesToBoolean(record);
			break;
		case RecordTypes.MINUTES:
			State.minutes = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.PASSWORD:
			State.password = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.POSITIONSOURCE:
			State.positionSource = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.SECONDS:
			State.seconds = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.URLMAPALERT:
			State.urlMapAlert = bytesToBoolean(record);
			break;
		case RecordTypes.USER:
			State.user = new String(record, 1, record.length - 1);
			break;
		case RecordTypes.CONNECTIONPERIOD:
			State.connectionPeriod = bytesToInteger(record);
			break;
		}
	}
	
	private boolean bytesToBoolean(byte[] record)
	{
		String s = new String(record, 1, record.length - 1);
		return s.equals("1");
	}
	
	private int bytesToInteger(byte[] record)
	{
		int result = 0;
		String s = new String(record, 1, record.length - 1);
		try
		{
			result = Integer.parseInt(s);
		}
		catch(NumberFormatException nfe)
		{
			System.out.println(nfe.getMessage());
			nfe.printStackTrace();
		}
		return result;
	}
	
	private void createConfiguration()
	{
		/*createRecord(recordStore, Integer.toString(State.alertHeight), RecordTypes.ALERTHEIGHT);
		createRecord(recordStore, Integer.toString(State.alertLeft), RecordTypes.ALERTLEFT);
		createRecord(recordStore, Integer.toString(State.alertTop), RecordTypes.ALERTTOP);
		createRecord(recordStore, Integer.toString(State.alertWidth), RecordTypes.ALERTWIDTH);
		String autoAlert = State.autoAlert ? "1" : "0";
		createRecord(recordStore, autoAlert, RecordTypes.AUTOALERT);
		createRecord(recordStore, State.latitude, RecordTypes.LATITUDE);
		createRecord(recordStore, State.longitude, RecordTypes.LONGITUDE);
		String mapAlert = State.mapAlert ? "1" : "0";
		createRecord(recordStore, mapAlert, RecordTypes.MAPALERT);
		createRecord(recordStore, State.minutes, RecordTypes.MINUTES);
		createRecord(recordStore, State.password, RecordTypes.PASSWORD);
		createRecord(recordStore, State.positionSource, RecordTypes.POSITIONSOURCE);
		createRecord(recordStore, State.seconds, RecordTypes.SECONDS);
		String urlMapAlert = State.urlMapAlert ? "1" : "0";
		createRecord(recordStore, urlMapAlert, RecordTypes.URLMAPALERT);
		createRecord(recordStore, State.user, RecordTypes.USER);
		createRecord(recordStore, Integer.toString(State.connectionPeriod), RecordTypes.CONNECTIONPERIOD);*/
	}
	
	private void createRecord(String data, int recordType)
	{
		int recordLength = 1;
		byte[] dataBytes = data.getBytes();
		
		if (data.length() > 0)
			recordLength = dataBytes.length + 1;
		
		byte[] record = new byte[recordLength]; 
		record[0] = (byte)recordType;
		for(int i = 1; i < recordLength; i++)
			record[i] = dataBytes[i-1];
		/*try
		{
			recordStore.addRecord(record, 0, recordLength);
		}
		catch(RecordStoreException rse)
		{
			System.out.println(rse.getMessage());
			rse.printStackTrace();
		}*/
	}

}