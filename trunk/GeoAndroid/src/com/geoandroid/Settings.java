package com.geoandroid;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

public class Settings extends ListActivity {
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		this.setTheme(android.R.style.Theme_Dialog); 

	}
	
	/**
	 * Restart the receiving, when we are back on line.
	 */
	@Override
	public void onResume() {
		super.onResume();
		

	}
	
	/**
	 * Make sure to stop the animation when we're no longer on screen,
	 * failing to do so will cause a lot of unnecessary cpu-usage!
	 */
	@Override
	public void onFreeze(Bundle icicle) {

		super.onFreeze(icicle);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.settings_close));
		return supRetVal;
	}
	
	@Override
	public boolean onOptionsItemSelected(Menu.Item item){
	    switch (item.getId()) {
		    case 0:
		    	this.finish();
		        return true;
	    }
	    return false;
	}

}