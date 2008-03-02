package com.geoandroid;

import android.location.Location;

/**
 * Small class to combine 
 * a Name and a Location.
 * Hit me, I didn't use Getters & Setters. ;) 
 */
public class Friend{
	public Location itsLocation = null;
	public String itsName = null;
	public String itsSessionKey;
	public Friend(Location aLocation, String aName, String aSessionKey){
		this.itsLocation = aLocation;
		this.itsName = aName;
		this.itsSessionKey = aSessionKey;
	}
}