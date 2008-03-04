package com.geoandroid;

import android.location.Location;

public class Friend {
	public Location itsLocation = null;
	public String itsName = null;
	public String itsSessionKey;
	public Friend(Location aLocation, String aName, String aSessionKey){
		this.itsLocation = aLocation;
		this.itsName = aName;
		this.itsSessionKey = aSessionKey;
	}
}