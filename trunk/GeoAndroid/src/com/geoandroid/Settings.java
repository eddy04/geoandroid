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

}