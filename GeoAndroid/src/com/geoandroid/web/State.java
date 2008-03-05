package com.geoandroid.web;

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
}