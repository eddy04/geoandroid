package com.geoandroid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import com.google.android.maps.Point;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.People;
import android.telephony.PhoneStateIntentReceiver;
import android.telephony.ServiceState;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;

import com.geoandroid.web.*;

public class FriendList extends ListActivity {
	// ===========================================================
	// Fields
	// ===========================================================
	
	protected static final String MY_LOCATION_CHANGED_ACTION = new String("android.intent.action.LOCATION_CHANGED");
	protected LocationManager myLocationManager = null;
	protected Location myLocation = null;
	
	protected boolean doUpdates = true;
	protected MyIntentReceiver myIntentReceiver = null; 
	protected final IntentFilter myIntentFilter =  new IntentFilter(MY_LOCATION_CHANGED_ACTION);
	
	private PhoneStateIntentReceiver mPhoneStateReceiver;
	private static HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
	
	protected final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 25; // in Meters
	protected final long MINIMUM_TIME_BETWEEN_UPDATE = 2500; // in Milliseconds
    private static final int MY_NOTIFICATION_ID = 0x100;
    
	protected boolean gps = false;
	protected boolean cellgps = false;	
	
	/** Minimum distance in meters for a friend 
	 * to be recognize as a Friend to be drawn */
	protected static final int NEARFRIEND_MAX_DISTANCE = 100000000;  // 10.000km
	
	/** List of friends in */
	protected ArrayList<Friend> allFriends = new ArrayList<Friend>();
	
	// ===========================================================
	// Extra-Class
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
			if(FriendList.this.doUpdates)
				FriendList.this.updateList(); // Will simply update our list, when receiving an intent
		}
	}
	
	   private class ServiceStateHandler extends Handler {
	        public void handleMessage(Message msg) {
	            Log.i(getString(R.string.main_title), "Trying to detect Cell ID and LAC" + msg.what);
	        switch (msg.what) {
	            case MY_NOTIFICATION_ID:
	                ServiceState state = mPhoneStateReceiver.getServiceState();
	                int notification_cid = state.getCid();
	                int notification_lac = state.getLac();
	                try {
	                    convertCellID(notification_cid , notification_lac);
	                } catch (Exception e) {
	                    Log.e(getString(R.string.main_title), e.toString(), e);
	                }
	                break;
	        }
	    }
	   }
	   
        private static class MyRequestEntity implements RequestEntity {
            int cellId, lac;

            public MyRequestEntity(int cellId, int lac) {
                this.cellId = cellId;
                this.lac = lac;
            }

            public boolean isRepeatable() {
                return true;
            }

            public void writeRequest(OutputStream outputStream) throws IOException {
                DataOutputStream os = new DataOutputStream(outputStream);
                os.writeShort(21);
                os.writeLong(0);
                os.writeUTF("fr");
                os.writeUTF("Sony_Ericsson-K750");
                os.writeUTF("1.3.1");
                os.writeUTF("Web");
                os.writeByte(27);

                os.writeInt(0);
                os.writeInt(0);
                os.writeInt(3);
                os.writeUTF("");
                os.writeInt(cellId);  // CELL-ID
                os.writeInt(lac);     // LAC
                os.writeInt(0);
                os.writeInt(0);
                os.writeInt(0);
                os.writeInt(0);
                os.flush();
            }

            public long getContentLength() {
                return -1;
            }

            public String getContentType() {
                return "application/binary";
            }
        }

	// ===========================================================
	// """Constructors""" (or the Entry-Point of it all)
	// ===========================================================
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		/* Log on to web service */
		try {
			Ipoki.sendWebReg("dangrahn", "passpass");
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}
		
		/* The first thing we need to do is to setup our own 
		 * locationManager, that will support us with our own gps data */
		this.myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		/* Update the list of our friends once on the start,
		 * as they are not(yet) moving, no updates to them are necessary */
		this.refreshFriendsList();
		
		/* Initiate the update of the contactList
		 * manually for the first time */ 
		this.updateList();

		/* Prepare the things, that will give 
		 * us the ability, to receive Information 
		 * about our GPS-Position. */
		this.setupForGPSAutoRefreshing();
	}
	
	/**
	 * Restart the receiving, when we are back on line.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		/* Log on to web service */
		try {
			Ipoki.sendWebReg("dangrahn", "passpass");
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}
		
		this.doUpdates = true;
		
		/* As we only want to react on the LOCATION_CHANGED
		 * intents we made the OS send out, we have to 
		 * register it along with a filter, that will only
		 * "pass through" on LOCATION_CHANGED-Intents.
		 */
		this.registerReceiver(this.myIntentReceiver, this.myIntentFilter);
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
		
		this.unregisterReceiver(this.myIntentReceiver);
		super.onFreeze(icicle);
	}

	/** Register with our LocationManager to send us 
	 * an intent (who's Action-String we defined above)
	 * when  an intent to the location manager,
	 * that we want to get informed on changes to our own position.
	 * This is one of the hottest features in Android.
	 */
	private void setupForGPSAutoRefreshing() {
		final long MIN_DISTANCE_UPDATE = 20; // in Meters
		final long MINIMUM_TIME_UPDATE = 5000; // in Milliseconds
		
		// Get the first provider available
		List<LocationProvider> providers = this.myLocationManager.getProviders();
		LocationProvider provider = providers.get(0);
		
		// If GPS is available setup refresh
		if (myLocationManager.getProviderStatus("gps")==LocationProvider.AVAILABLE){
			this.gps = true;
			this.myLocationManager.requestUpdates(provider, MINIMUM_TIME_UPDATE,
					MIN_DISTANCE_UPDATE, new Intent(MY_LOCATION_CHANGED_ACTION));
			
			/* Intent receiver reacting on our update request. */ 
			this.myIntentReceiver = new MyIntentReceiver();
			Log.i(getString(R.string.main_title), "GPS Detected and setup for autorefreshing");
			}
		else {
			this.gps = false;
	        /*
	         * Set up the handler for receiving events for service state. Like Cell-Id and LAC to determine location
	         */
	        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, new ServiceStateHandler());
	        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.registerIntent();
			Log.i(getString(R.string.main_title), "Virtual GPS Detected and setup for autorefreshing");
		}
		/* 
		 * In onResume() the following method will be called automatically!
		 * registerReceiver(this.myIntentReceiver, this.myIntentFilter); 
		 */
	}

	/** Called only the first time the options menu is displayed.
	 * Create the menu entries.
	 *  Menus are added in the order they are hardcoded. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.main_menu_open_map));
		return supRetVal;
	}
	@Override
	public boolean onOptionsItemSelected(Menu.Item item) {
		switch (item.getId()) {
			case 0:
				startSubActivity(new Intent(this, GeoAndroidMap.class), 0);
				return true;
		}
		return false;
	}

	// ===========================================================
	// Methods
	// ===========================================================
	
	/*private void refreshFriendsList(){
		Location friendLocation = new Location();
		friendLocation.setLongitude(13.3209228515625);
		friendLocation.setLatitude(55.85219164310742);
		allFriends.add(new Friend(friendLocation, "Bob Lund", 1));
		
		friendLocation = new Location();
		friendLocation.setLongitude(13.49395751953125);
		friendLocation.setLatitude(55.49752723542657);
		allFriends.add(new Friend(friendLocation, "Su Ellen", 2));
	}*/

	private void refreshFriendsList(){
		
		Friend[] friends = null;
				
		try {
			friends = Ipoki.getFriendsPos();
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}
		
		if(friends != null) {
			for(int i = 0; i<friends.length; ++i) {
				allFriends.add(friends[i]);
			}
		}
	}
	
	private void updateList() {
		// Refresh our location...
		this.myLocation = myLocationManager.getCurrentLocation("gps");
		
		//Add new position to web service
		try {
			Ipoki.sendWebPos(Double.toString(this.myLocation.getLatitude()), Double.toString(this.myLocation.getLongitude()));
		} catch(IOException e) {
			 Log.e(getString(R.string.main_title), e.toString(), e);
		}
		
		ArrayList<String> listItems = new ArrayList<String>();
		
		// For each Friend
		for(Friend aNearFriend : this.allFriends){
			/* Load the row-entry-format defined as a String 
			 * and replace $name with the contact's name we 
			 * get from the cursor */
			String curLine = new String(getString(R.string.main_list_format));
			curLine = curLine.replace("$name", aNearFriend.itsName);
			
			if(aNearFriend.itsLocation != null){
				if( this.myLocation.distanceTo(aNearFriend.itsLocation) < 
									NEARFRIEND_MAX_DISTANCE){
					final DecimalFormat df = new DecimalFormat("####0.000");
					String formattedDistance = 
						df.format(this.myLocation.distanceTo(
										aNearFriend.itsLocation) / 1000);
					curLine = curLine.replace("$distance", formattedDistance);
				}
			}else{
				curLine = curLine.replace("$distance", 
						getString(R.string.main_list_geo_not_set));
			}
			
			listItems.add(curLine);
		}

		ArrayAdapter<String> notes =  new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, listItems);

		
		long beforeIndex = 0;
		if(this.getListAdapter() != null)
			beforeIndex = this.getSelectedItemId();
			
		this.setListAdapter(notes);
		
		try{
			this.setSelection((int)beforeIndex);
		}catch (Exception e){}
	}
	
	public void convertCellID(int cellid, int lac) throws Exception {
        String url = "http://www.google.com/glm/mmap";
        HttpURL httpURL = new HttpURL(url);
        HostConfiguration host = new HostConfiguration();
        host.setHost(httpURL.getHost(), httpURL.getPort());
        HttpConnection connection = connectionManager.getConnection(host);
        connection.open();

        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(new MyRequestEntity(cellid, lac));
        postMethod.execute(new HttpState(), connection);
        InputStream response = postMethod.getResponseBodyAsStream();
        DataInputStream dis = new DataInputStream(response);
        dis.readShort();
        dis.readByte();
        int code = dis.readInt();
        if (code == 0) {
            double lat = (double) dis.readInt() / 1E6;
            double lng = (double) dis.readInt() / 1E6;
            dis.readInt();
            dis.readInt();
            dis.readUTF();
            Log.i(getString(R.string.main_title), "Lat, Long: " + lat + "," + lng);

            /*
             * Store Lat y Long in MyLocation Object
             */
            myLocation.setLatitude(lat);
            myLocation.setLongitude(lng);
			this.cellgps = true;
        }
 		
        //myPoint = new Point((int) (myLocation.getLatitude() * 1E6), (int)
        //        (myLocation.getLongitude() * 1E6));
        
 		/*
 		 * Refresh the Location Objects Lists
 		 */
        connection.close();
        connection.releaseConnection();
		this.refreshFriendsList();
    }
}