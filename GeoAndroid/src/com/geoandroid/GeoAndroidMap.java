package com.geoandroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import com.geoandroid.web.Friend;
import com.geoandroid.web.Ipoki;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayController;
import com.google.android.maps.Point;


public class GeoAndroidMap extends MapActivity {

	// ===========================================================
	// Fields
	// ===========================================================
	
	private static final String MY_LOCATION_CHANGED_ACTION = 
				new String("android.intent.action.LOCATION_CHANGED");
	
	/** Minimum distance in meters for a friend 
	 * to be recognize as a Friend to be drawn */
	protected static final int NEARFRIEND_MAX_DISTANCE = 10000000;  // 10.000km
	
	final Handler mHandler = new Handler();
	
	/*
	 * Stuff we need to save as fields, 
	 * because we want to use multiple 
	 * of them in different methods.  
	 */
	protected boolean doUpdates = true;
	protected MapView myMapView = null;
	protected MapController myMapController = null;
	protected OverlayController myOverlayController = null;

	protected LocationManager myLocationManager = null;
	protected Location myLocation = null;
	protected MyIntentReceiver myIntentReceiver = null; 
	protected final IntentFilter myIntentFilter = 
				new IntentFilter(MY_LOCATION_CHANGED_ACTION);
	
	/** List of friends in */
	protected ArrayList<Friend> allFriends = new ArrayList<Friend>();
	
	// ===========================================================
	// Extra-Classes
	// ===========================================================
	
	/**
	 * This tiny IntentReceiver updates
	 * our stuff as we receive the intents 
	 * (LOCATION_CHANGED_ACTION) we told the 
	 * myLocationManager to send to us. 
	 */
	class MyIntentReceiver extends IntentReceiver {
		@Override
		public void onReceiveIntent(Context context, Intent intent) {
			if(GeoAndroidMap.this.doUpdates)
				GeoAndroidMap.this.updateView();
		}
	}
	
	/**
	 * This method is so huge, 
	 * because it does a lot of FANCY painting.
	 * We could shorten this method to a few lines.
	 * But as users like eye-candy apps ;) ...
	 */
    protected class MyLocationOverlay extends Overlay {
        @Override
        public void draw(Canvas canvas, PixelCalculator calculator, boolean shadow) {
        	super.draw(canvas, calculator, shadow);
        	// Setup our "brush"/"pencil"/ whatever...
        	Paint paint = new Paint();
        	paint.setTextSize(14);
        	
        	// Create a Point that represents our GPS-Location
        	Double lat = GeoAndroidMap.this.myLocation.getLatitude() * 1E6;
        	Double lng = GeoAndroidMap.this.myLocation.getLongitude() * 1E6;
        	Point point = new Point(lat.intValue(), lng.intValue());
        	
        	int[] myScreenCoords = new int[2];
    		// Converts lat/lng-Point to OUR coordinates on the screen.
    		calculator.getPointXY(point, myScreenCoords);
    		
    		// Draw a circle for our location
    		RectF oval = new RectF(myScreenCoords[0] - 7, myScreenCoords[1] + 7, 
    							myScreenCoords[0] + 7, myScreenCoords[1] - 7);
    		
        	// Setup a color for our location
    		paint.setStyle(Style.FILL);
    		paint.setARGB(255, 80, 150, 30); // Nice strong Android-Green     
    		// Draw our name
    		canvas.drawText(getString(R.string.map_overlay_own_name),
    							myScreenCoords[0] +9, myScreenCoords[1], paint);
    		
    		// Change the paint to a 'Lookthrough' Android-Green 
    		paint.setARGB(80, 156, 192, 36);
        	paint.setStrokeWidth(1);
    		// draw an oval around our location
    		canvas.drawOval(oval, paint);
    		
    		 // With a black stroke around the oval we drew before.
    		paint.setARGB(255,0,0,0);
    		paint.setStyle(Style.STROKE);
    		canvas.drawCircle(myScreenCoords[0], myScreenCoords[1], 7, paint);
        	
        	int[] friendScreenCoords = new int[2];
        	//Draw each friend with a line pointing to our own location.
        	for(Friend aFriend : GeoAndroidMap.this.allFriends){
        		lat = aFriend.itsLocation.getLatitude() * 1E6;
        		lng = aFriend.itsLocation.getLongitude() * 1E6;
        		point = new Point(lat.intValue(), lng.intValue());

        		// Converts lat/lng-Point to coordinates on the screen.
        		calculator.getPointXY(point, friendScreenCoords);
        		if(Math.abs(friendScreenCoords[0]) < 2000 && Math.abs(friendScreenCoords[1]) < 2000){
	        		// Draw a circle for this friend and his name
	        		oval = new RectF(friendScreenCoords[0] - 7, friendScreenCoords[1] + 7, 
        							friendScreenCoords[0] + 7, friendScreenCoords[1] - 7);
	        		
	            	// Setup a color for all friends
	        		paint.setStyle(Style.FILL);
	        		paint.setARGB(255, 255, 0, 0); // Nice red      		
	        		canvas.drawText(aFriend.itsName, friendScreenCoords[0] +9,
	        							friendScreenCoords[1], paint);
	        		
	        		// Draw a line connecting us to the current Friend
	        		paint.setARGB(80, 255, 0, 0); // Nice red, more look through...

	            	paint.setStrokeWidth(2);
	        		canvas.drawLine(myScreenCoords[0], myScreenCoords[1], 
	        						friendScreenCoords[0], friendScreenCoords[1], paint);
	            	paint.setStrokeWidth(1);
	            	// draw an oval around our friends location
	        		canvas.drawOval(oval, paint);
	        		
	        		 // With a black stroke around the oval we drew before.
	        		paint.setARGB(255,0,0,0);
	        		paint.setStyle(Style.STROKE);
	        		canvas.drawCircle(friendScreenCoords[0], friendScreenCoords[1], 7, paint);
        		}
        	}
        }
    }

	// ===========================================================
	// """Constructor""" (or the Entry-Point of this class)
	// ===========================================================
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		/* Log on to web service */
		try {
			Ipoki.sendWebReg("dangrahn", "passpass");
		} catch(IOException e) {
			 Log.e(getString(R.string.map_title), e.toString(), e);
		}
		
		/* Create a new MapView and show it */
		//this.myMapView = new MapView(this);
		//this.setContentView(myMapView);
		
		this.setContentView(R.layout.map);
		
		this.myMapView = (MapView) findViewById(R.id.myMapView);
		//Default satellite view
		this.myMapView.toggleSatellite(); 
		
		/* Button listeners */
		findViewById(R.id.map_button_zoomin).setOnClickListener(new OnClickListener(){
             @Override
             public void onClick(View arg0) {
 		    	// Zoom not closer than possible
 		    	myMapController.zoomTo(Math.min(21, myMapView.getZoomLevel() + 1));
             }
        }); 
        findViewById(R.id.map_button_zoomout).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0) {
		    	// Zoom not more far than possible
		    	myMapController.zoomTo(Math.min(21, myMapView.getZoomLevel() - 1));
            }
       }); 
		/* MapController is capable of zooming 
		 * and animating and stuff like that */
		this.myMapController = this.myMapView.getController();
		
		/* With these objects we are capable of 
		 * drawing graphical stuff on top of the map */
		this.myOverlayController = this.myMapView.createOverlayController();
		MyLocationOverlay myLocationOverlay = new MyLocationOverlay();
		this.myOverlayController.add(myLocationOverlay, true);
		
		this.myMapController.zoomTo(2); // Far out
		
		// Initialize the LocationManager
		this.myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		this.updateView();
		
		/* Prepare the things, that will give 
		 * us the ability, to receive Information 
		 * about our GPS-Position. */
		this.setupForGPSAutoRefreshing();
		
		/* Update the list of our friends once on the start,
		 * as they are not(yet) moving, no updates to them are necessary */
		this.refreshFriendsList();
	}
	
	/**
	 * Restart the receiving, when we are back on line.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		/* Log on to web service 
		try {
			Ipoki.sendWebReg("dangrahn", "k4rd1na!");
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}*/
		
		this.doUpdates = true;
		
		/* As we only want to react on the LOCATION_CHANGED
		 * intents we made the OS send out, we have to 
		 * register it along with a filter, that will only
		 * "pass through" on LOCATION_CHANGED-Intents.
		 */
		registerReceiver(this.myIntentReceiver, this.myIntentFilter);
	}
	
	
	
	/**
	 * Make sure to stop the animation when we're no longer on screen,
	 * failing to do so will cause a lot of unnecessary cpu-usage!
	 */
	@Override
	public void onFreeze(Bundle icicle) {
		this.doUpdates = false;
		
		/* Log out of web service 
		try {
			Ipoki.sendWebDisconnection();
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}*/
		
		unregisterReceiver(this.myIntentReceiver);
		super.onFreeze(icicle);
	}
	
	// ===========================================================
	// Overridden methods
	// ===========================================================
	
	// Called only the first time the options menu is displayed.
	// Create the menu entries.
	// Menu adds items in the order shown.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.map_menu_toggle_street_satellite));
		menu.add(0, 1, getString(R.string.map_menu_toggle_traffic));
		menu.add(0, 2, getString(R.string.map_menu_friendlist));
		menu.add(0, 3, getString(R.string.map_menu_settings));
		return supRetVal;
	}
	
	@Override
	public boolean onOptionsItemSelected(Menu.Item item){
	    switch (item.getId()) {
		    case 0:
	        	// Toggle satellite view
	            myMapView.toggleSatellite();
		        return true;
		    case 1:
		    	//Toggle traffic view
		    	myMapView.toggleTraffic();
		    	return true;
		    case 2:
		    	startSubActivity(new Intent(this, FriendList.class), 0);
		        return true;
		    case 3:
		    	startSubActivity(new Intent(this, Settings.class), 0);
		        return true;
	    }
	    return false;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_I) {
	    	// Zoom not closer than possible
	    	this.myMapController.zoomTo(Math.min(21, this.myMapView.getZoomLevel() + 1));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_O) {
	    	// Zoom not farer than possible 
	    	this.myMapController.zoomTo(Math.max(1, this.myMapView.getZoomLevel() - 1));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_T) {
        	// Switch to satellite view
            myMapView.toggleSatellite();
            return true;
        }
        return false;
    }

	// ===========================================================
	// Methods
	// ===========================================================
	
	private void setupForGPSAutoRefreshing() {
		/* Register with out LocationManager to send us 
		 * an intent (whos Action-String we defined above)
		 * when  an intent to the location manager,
		 * that we want to get informed on changes to our own position.
		 * This is one of the hottest features in Android.
		 */
		final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 25; // in Meters
		final long MINIMUM_TIME_BETWEEN_UPDATE = 5000; // in Milliseconds
		// Get the first provider available
		List<LocationProvider> providers = this.myLocationManager.getProviders();
		LocationProvider provider = providers.get(0);
		
		this.myLocationManager.requestUpdates(provider, MINIMUM_TIME_BETWEEN_UPDATE,
				MINIMUM_DISTANCECHANGE_FOR_UPDATE, new Intent(MY_LOCATION_CHANGED_ACTION));
		
		/* Create an IntentReceiver, that will react on the
		 * Intents we made our LocationManager to send to us. 
		 */ 
		this.myIntentReceiver = new MyIntentReceiver();
	
		/* 
		 * In onResume() the following method will be called automatically!
		 * registerReceiver(this.myIntentReceiver, this.myIntentFilter); 
		 */
	}
	
	private void updateView() {
		// Refresh our gps-location
		this.myLocation = myLocationManager.getCurrentLocation("gps");
		
		/* Redraws the mapViee, which also makes our 
		 * OverlayController redraw our Circles and Lines */
		this.myMapView.invalidate();
		
		/* As the location of our Friends is static and 
		 * for performance-reasons, we do not call this */
		// this.refreshFriendsList(NEARFRIEND_MAX_DISTANCE);
	}
	
	/*private void refreshFriendsList(){
		Location friendLocation = new Location();
		friendLocation.setLongitude(13.3209228515625);
		friendLocation.setLatitude(55.85219164310742);
		allFriends.add(new Friend(friendLocation, "Bob Lund", "xxx"));
		
		friendLocation = new Location();
		friendLocation.setLongitude(13.49395751953125);
		friendLocation.setLatitude(55.49752723542657);
		allFriends.add(new Friend(friendLocation, "Su Ellen", "xxx"));
	}*/
	
	private void refreshFriendsList(){
		
		Friend[] friends = null;
				
		try {
			friends = Ipoki.getFriendsPos();
		} catch(IOException e) {
			 Log.e(getString(R.string.map_title), e.toString(), e);
		}
		
		if(friends != null) {
			for(int i = 0; i<friends.length; ++i) {
				allFriends.add(friends[i]);
			}
		}
	}
	
}
