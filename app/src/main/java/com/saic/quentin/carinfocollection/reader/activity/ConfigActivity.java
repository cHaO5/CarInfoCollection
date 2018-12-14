
package com.saic.quentin.carinfocollection.reader.activity;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import com.saic.quentin.carinfocollection.commands.ObdCommand;
import com.saic.quentin.carinfocollection.R;
import com.saic.quentin.carinfocollection.reader.config.ObdConfig;

/**
 * Configuration activity.
 */
public class ConfigActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	public static final String BLUETOOTH_LIST_KEY = "bluetooth_list_preference";
	public static final String UPDATE_PERIOD_KEY = "update_period_preference";
	public static final String IMPERIAL_UNITS_KEY = "imperial_units_preference";
	public static final String COMMANDS_SCREEN_KEY = "obd_commands_screen";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * Read preferences resources available at res/xml/preferences.xml
		 */
		addPreferencesFromResource(R.xml.preferences);

		ArrayList<CharSequence> pairedDeviceStrings = new ArrayList<CharSequence>();
		ArrayList<CharSequence> vals = new ArrayList<CharSequence>();
		ListPreference listBtDevices = (ListPreference) getPreferenceScreen()
				.findPreference(BLUETOOTH_LIST_KEY);
		String[] prefKeys = new String[] { UPDATE_PERIOD_KEY};
		for (String prefKey : prefKeys) {
			EditTextPreference txtPref = (EditTextPreference) getPreferenceScreen()
					.findPreference(prefKey);
			txtPref.setOnPreferenceChangeListener(this);
		}

		/*
		 * Available OBD commands
		 * 
		 */
		ArrayList<ObdCommand> cmds = ObdConfig.getCommands();
		PreferenceScreen cmdScr = (PreferenceScreen) getPreferenceScreen()
				.findPreference(COMMANDS_SCREEN_KEY);
		for (ObdCommand cmd : cmds) {
			CheckBoxPreference cpref = new CheckBoxPreference(this);
			cpref.setTitle(cmd.getName());
			cpref.setKey(cmd.getName());
			cpref.setChecked(true);
			cmdScr.addPreference(cpref);
		}

		/*
		 * Let's use this device Bluetooth adapter to select which paired OBD-II
		 * compliant device we'll use.
		 */
		final BluetoothAdapter mBtAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBtAdapter == null) {
			listBtDevices.setEntries(pairedDeviceStrings
					.toArray(new CharSequence[0]));
			listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));

			// we shouldn't get here, still warn user
			Toast.makeText(this, "This device does not support Bluetooth.",
					Toast.LENGTH_LONG);

			return;
		}

		/*
		 * Listen for preferences click.
		 * 
		 */
		final Activity thisActivity = this;
		listBtDevices.setEntries(new CharSequence[1]);
		listBtDevices.setEntryValues(new CharSequence[1]);
		listBtDevices
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						// see what I mean in the previous comment?
						if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
							Toast.makeText(
									thisActivity,
									"This device does not support Bluetooth or it is disabled.",
									Toast.LENGTH_LONG);
							return false;
						}
						return true;
					}
				});

		/*
		 * Get paired devices and populate preference list.
		 */
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				pairedDeviceStrings.add(device.getName() + "\n"
						+ device.getAddress());
				vals.add(device.getAddress());
			}
		}
		listBtDevices.setEntries(pairedDeviceStrings
				.toArray(new CharSequence[0]));
		listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));
	}


	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (UPDATE_PERIOD_KEY.equals(preference.getKey())) {
			try {
				Double.parseDouble(newValue.toString());
				return true;
			} catch (Exception e) {
				Toast.makeText(
						this,
						"Couldn't parse '" + newValue.toString()
								+ "' as a number.", Toast.LENGTH_LONG).show();
			}
		}
		return false;
	}


	public static int getUpdatePeriod(SharedPreferences prefs) {
		String periodString = prefs.getString(ConfigActivity.UPDATE_PERIOD_KEY,
				"1"); // 1 as in seconds
		int period = 1000; // by default 1000ms

		try {
			period = Integer.parseInt(periodString) * 1000;
		} catch (Exception e) {
		}

		if (period <= 0) {
			period = 250;
		}

		return period;
	}



	public static ArrayList<ObdCommand> getObdCommands(SharedPreferences prefs) {
		ArrayList<ObdCommand> cmds = ObdConfig.getCommands();
		ArrayList<ObdCommand> ucmds = new ArrayList<ObdCommand>();
		for (int i = 0; i < cmds.size(); i++) {
			ObdCommand cmd = cmds.get(i);
			boolean selected = prefs.getBoolean(cmd.getName(), true);
			if (selected) {
				ucmds.add(cmd);
			}
		}
		return ucmds;
	}
}