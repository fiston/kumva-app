/**
 * Copyright 2011 Rowan Seymour
 * 
 * This file is part of Kumva.
 *
 * Kumva is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kumva is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kumva. If not, see <http://www.gnu.org/licenses/>.
 */

package com.ijuru.kumva.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ijuru.kumva.Entry;
import com.ijuru.kumva.app.ui.Dialogs;
import com.ijuru.kumva.app.util.SizeLimitedUniqueHistory;
import com.ijuru.kumva.app.util.Utils;
import com.ijuru.kumva.remote.RemoteDictionary;
import com.ijuru.kumva.R;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;

/**
 * The main Kumva application
 */
public class KumvaApplication extends Application {
	
	private List<RemoteDictionary> dictionaries = new ArrayList<RemoteDictionary>();
	private RemoteDictionary activeDictionary = null;

	private Collection<String> recentSearches = new SizeLimitedUniqueHistory<String>(20);

	private Entry currentEntry;
	private MediaPlayer player;
	
	private final String PREF_FILE_DICTS = "dictionaries";

	private final String PREF_KEY_ACTIVEDICT = "active_dict";
	//private final String PREF_KEY_RECENTSEARCHES = "recent_searches";

	/**
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		loadPreferences();

		this.player = new MediaPlayer();
	}
		
	/**
	 * @see android.app.Application#onTerminate()
	 */
	@Override
	public void onTerminate() {
		savePreferences();

		super.onTerminate();
	}

	protected void loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		loadDictionaries();

		// Get active dictionary from default preferences
		String activeDictURL = prefs.getString(PREF_KEY_ACTIVEDICT, null);
		if (activeDictURL != null) {
			this.activeDictionary = getDictionaryByUrl(activeDictURL);
		}

		// Get recent searches from default preferences
		/*String recentSearhesCsv = prefs.getString(PREF_KEY_RECENTSEARCHES, null);
		if (recentSearhesCsv != null) {
			this.recentSearches = Utils.parseCSV(recentSearhesCsv);
		}*/
	}

	/**
	 * Loads the dictionaries from the dictionary preferences file
	 */
	protected void loadDictionaries() {
		// Get the dictionaries shared pref file
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_FILE_DICTS, MODE_PRIVATE);
		
		// Remove existing dictionaries
		this.dictionaries.clear();
		
		for (Object dictCSV : prefs.getAll().values()) {
			String[] fields = ((String)dictCSV).split(",");
			if (fields.length == 5) {
				String url = fields[0];
				String name = fields[1];
				String version = fields[2];
				String defLang = fields[3];
				String meanLang = fields[4];
				addDictionary(new RemoteDictionary(url, name, version, defLang, meanLang));
			}
			else {
				Dialogs.toast(this, getString(R.string.err_dictloading));
				break;
			}
		}

		// Add Kinyarwanda.net if there are no dictionaries
		if (this.dictionaries.size() == 0) {
			RemoteDictionary kinyaDict = new RemoteDictionary("http://kinyarwanda.net", "Kinyarwanda.net", "?", "rw", "en");
			this.dictionaries.add(kinyaDict);
			this.activeDictionary = kinyaDict;
		}
	}
	
	/**
	 * Saves the dictionaries to the dictionary preferences file
	 */
	public void savePreferences() {
		// Get the dictionaries a special preference file
		SharedPreferences prefs = getSharedPreferences(PREF_FILE_DICTS, MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.clear();
		int dict = 1;
		for (RemoteDictionary dictionary : dictionaries) {
			editor.putString("site" + (dict++), dictionary.toString());
		}

		editor.commit();
			
		// Save default preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		editor = prefs.edit();
		setPreference(editor, PREF_KEY_ACTIVEDICT, activeDictionary.getUrl());
		//setPreference(editor, PREF_KEY_RECENTSEARCHES, Utils.makeCSV(recentSearches));
		editor.commit();
	}

	/**
	 * Sets a single preference
	 * @param editor the editor
	 * @param key the preference key
	 * @param value the value
	 */
	protected void setPreference(Editor editor, String key, String value) {
		if (value != null) {
			editor.putString(key, value);
		}
		else {
			editor.remove(key);
		}
	}

	/**
	 * Gets the list of dictionaries available
	 * @return the dictionaries
	 */
	public List<RemoteDictionary> getDictionaries() {
		return dictionaries;
	}
	
	/**
	 * Gets the dictionary with the given url
	 * @return the dictionary or null
	 */
	public RemoteDictionary getDictionaryByUrl(String url) {
		for (RemoteDictionary dict : dictionaries)
			if (url.equals(dict.getUrl()))
				return dict;
		return null;
	}

	/**
	 * Gets the active dictionary
	 * @return the active dictionary
	 */
	public RemoteDictionary getActiveDictionary() {
		return this.activeDictionary;
	}
	
	/**
	 * Sets the active dictionary
	 * @param dictionary the active dictionary
	 */
	public void setActiveDictionary(RemoteDictionary dictionary) {
		this.activeDictionary = dictionary;
	}
	
	/**
	 * Adds the specified dictionary
	 * @param dictionary the dictionary to add
	 */
	public void addDictionary(RemoteDictionary dictionary) {
		// If its the only dictionary then make it active
		if (this.dictionaries.size() == 0)
			this.activeDictionary = dictionary;
		
		this.dictionaries.add(dictionary);
	}
	
	/**
	 * Removes the specified dictionary
	 * @param dictionary the dictionary to delete
	 */
	public void removeDictionary(RemoteDictionary dictionary) {
		this.dictionaries.remove(dictionary);
		
		if (dictionary == activeDictionary)
			activeDictionary = null;
	}

	/**
	 * Gets the recent searches list
	 * @return the list
	 */
	/*public Collection<String> getRecentSearches() {
		return recentSearches;
	}*/

	/**
	 * Adds a recent search
	 * @param query the query
	 */
	/*public void addRecentSearch(String query) {
		recentSearches.add(query);
	}*/

	/**
	 * Gets the currently viewed entry
	 * @return the entry
	 */
	public Entry getCurrentEntry() {
		return currentEntry;
	}

	/**
	 * Sets the currently viewed entry
	 * @param entry the entry
	 */
	public void setCurrentEntry(Entry entry) {
		this.currentEntry = entry;
	}
	
	/**
	 * Gets the media player
	 * @return the media player
	 */
	public MediaPlayer getMediaPlayer() {
		return player;
	}
}
