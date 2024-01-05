package com.marbleblast.mblauncher;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.json.*;

public class GameMod implements Comparable<Object> {
	private static final String gameNameKey = "gamename";
	private String name;
	private URI path;
	private Map<String, Object> config;
	private boolean isRemote;
	
	/**
	 * Instantiates a new GameMod with a given name and configuration file URI
	 * @param name The internal name of the GameMod
	 * @param configLocation A URI (http https or file) that points to a JSON file with the mod's configuration
	 * @throws Exception
	 */
	public GameMod(String name, URI configLocation) throws Exception {
		this.name = name;
		this.path = configLocation;
		this.config = new HashMap<String, Object>();
		this.loadConfig(configLocation);
	}
	
	/**
	 * Get the mod's internal name
	 * @return The mod's internal name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Get the mod's config path
	 * @return The mod's config path
	 */
	public URI getPath() {
		return this.path;
	}
	
	/**
	 * Get whether the mod's path is at a remote file or on disk
	 * @return If the mod's path is remote
	 */
	public boolean getRemote() {
		return this.isRemote;
	}
	
	/**
	 * Get a value for a key in the mod's config 
	 * @param key The key to search
	 * @return The config value for the key
	 */
	public Object getConfig(String key) {
		return config.get(key);
	}
	
	/**
	 * Get the game's base installation directory
	 * @param makeDir Whether or not the directory should be created 
	 * @return The path to the game's base directory
	 */
	public String getGameDirectory(boolean makeDir) {
		String directory = getInstallDirectory();
		if (Utils.getOS().startsWith("Mac"))
			directory = directory + "/" + getConfig(gameNameKey) + ".app";

		if (directory != null) {
			if (makeDir) {
				// Ensure directory exists. If not, create it.
				File file = new File(directory);
				if (!file.exists() && makeDir) {
					file.mkdirs();
				}
			}
		}
		return directory;
	}
	
	public String getInstallDirectory() {
		String pref = Preferences.userRoot().get("modpath-" + this.name, null);
		if (pref == null) {
			if (Utils.getOS().startsWith("Mac")) {
				return Utils.getInstallDirectory();
			} else {
				return Utils.getInstallDirectory() + "/" + getConfig(gameNameKey);
			}
		} else {
			return pref;
		}
	}
	
	/**
	 * Set the game's new directory. Does not move any files on disk though
	 * @param newDirectory New directory
	 */
	public void setInstallDirectory(String newDirectory) {
		Preferences.userRoot().put("modpath-" + this.name, newDirectory);
	}
	
	/**
	 * Get the game's old base installation directory
	 * @return The path to the game's old base directory
	 */
	public String getOldGameDirectory() {
		if (Utils.getOS().startsWith("Mac"))
			return Utils.getOldInstallDirectory() + "/" + getConfig(gameNameKey) + ".app";
		else
			return Utils.getOldInstallDirectory() + "/" + getConfig(gameNameKey); 
	}
	
	/**
	 * Loads the mod's configuration from a given URI
	 * @param location The URI to read from for the config file
	 * @throws Exception
	 */
	private void loadConfig(URI location) throws Exception {
		byte[] contents = null;

		//Get the contents of location, whatever way we can
		if (location.getScheme().equals("file")) {
			isRemote = false;
			
			//Files can just readFile
			contents = Utils.readFile(location.toString());
		} else if (location.getScheme().equals("http") || location.getScheme().equals("https")) {
			isRemote = true;

			//HTTP we can just use Utils.downloadFile
			contents = Utils.downloadCachedFile(location.toString(), getName());
		}
		
		if (contents == null) {
			throw new NullPointerException("Could not fetch mod config.");
		}
		
		//Get its contents as a JSONObject
		JSONObject json = new JSONObject(new String(contents));
		
		//And load them
		this.parseConfig(json);
	}
	
	/**
	 * Parse the JSONObject generated in loadConfig() and store it into the mod's config
	 * @param json The JSONObject to parse
	 */
	private void parseConfig(JSONObject json) {
		String names[] = JSONObject.getNames(json);
		String thisOS = Utils.getOS();
		
		//Same as Config
		for (String name : names) {
			Object val = json.get(name);
			
			if (val instanceof String) {
				//Non-OS Specific
				config.put(name, json.get(name));
			} else if (val instanceof JSONObject) {
				//OS Specific
				if (((JSONObject) val).has(thisOS.toLowerCase())) {
					config.put(name, ((JSONObject) val).get(thisOS.toLowerCase()));
				} else if (((JSONObject) val).has("other")) {
					config.put(name, ((JSONObject) val).get("other"));
				} else {
					//Maybe not, just an array
					config.put(name, json.get(name));
				}
			}
		}
	}

	//Needed for sorting
	@Override
	public int compareTo(Object o) {
		if (o instanceof GameMod) {
			GameMod omod = (GameMod)o;
			return ((String)getConfig("gamename")).compareTo(((String)omod.getConfig("gamename")));
		}
		return 0;
	}
}
