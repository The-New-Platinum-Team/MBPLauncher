package com.marbleblast.mblauncher;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import org.json.JSONObject;

public class ModManager {
	private static final String modNamesKey = "mods";
	private static final String modPathsKey = "modPaths";
	private static final String defaultModKey = "modDefault";
	
	private static final String modNameSeperator = " ";
	
	private ArrayList<GameMod> mods;
	private boolean needDefaultMods;
	
	public String defaultMod = Preferences.userRoot().get(defaultModKey, "pq");
	
	/**
	 * Construct a new ModManager with the mods loaded in the user's preferences.
	 */
	public ModManager() {
		needDefaultMods = true;
		this.loadMods();
	}
	
	/**
	 * Load the user's mod choices from their preferences
	 */
	private void loadMods() {
		mods = new ArrayList<GameMod>();
	
		//Get and parse the user's preferences
		String modnamespref = Preferences.userRoot().get(modNamesKey, "");
		String modpathspref = Preferences.userRoot().get(modPathsKey, "");
		String[] modnames = modnamespref.split(modNameSeperator);
		String[] modpaths = modpathspref.split(modNameSeperator);
		
		int modcount = Math.min(modnames.length, modpaths.length);
		if (modnames.length != modpaths.length) {
			//Oh shit!
		}
		
		//Add all the mods
		for (int i = 0; i < modcount; i ++) {
			String modname = modnames[i];
			String modpath = modpaths[i];
			
			//Throws an exception if it can't find the mod
			try {
				this.addMod(modname, modpath);
				needDefaultMods = false;
			} catch (Exception e) {
				e.printStackTrace();
				Main.log(e);
			}
		}
	}
	
	/**
	 * Save the user's mod choices into their preferences
	 */
	public void saveMods() {
		StringBuilder modNamesBuilder = new StringBuilder();
		StringBuilder modPathsBuilder = new StringBuilder();
		
		//Add all the mods from mods into the list
		for (GameMod mod : mods) {
			String key = mod.getName();
			
			modNamesBuilder.append(key + modNameSeperator);
			modPathsBuilder.append(mod.getPath().toString() + modNameSeperator);
		}
		
		//Remove trailing seperator
		if (modNamesBuilder.length() > 0) {
			modNamesBuilder.deleteCharAt(modNamesBuilder.length() - modNameSeperator.length());
		}
		if (modPathsBuilder.length() > 0) {
			modPathsBuilder.deleteCharAt(modPathsBuilder.length() - modNameSeperator.length());
		}
		
		//Put into prefs
		Preferences.userRoot().put(modNamesKey, modNamesBuilder.toString());
		Preferences.userRoot().put(modPathsKey, modPathsBuilder.toString());
	}
	
	/**
	 * Gets the mod with the given internal name
	 * @param name The internal name of the mod
	 * @return The mod with the given name
	 */
	public GameMod getMod(String name) {
		for (GameMod mod : mods) {
			if (mod.getName().equals(name)) {
				return mod;
			}
		}
		return null;
	}

	/**
	 * Gets all the mod names in a collection
	 * @return A collection of all the mod names
	 */
	public Collection<String> getModNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (GameMod mod : mods) {
			names.add(mod.getName());
		}
		return names;
	}
	
	/**
	 * Gets all the mods in a collection
	 * @return A collection of all the mods
	 */
	public Collection<GameMod> getMods() {
		return mods;
	}
	
	/**
	 * Add a mod to the mod manager with the given internal name and path
	 * @param name The mod's internal name
	 * @param path The path to the mod's config.json file
	 * @throws Exception
	 * @return The GameMod which was added
	 */
	public GameMod addMod(String name, String path) throws Exception {
		GameMod mod = new GameMod(name, new URI(path));
		mods.add(mod);
		
		//Save to prefs
		saveMods();
		return mod;
	}
	
	/**
	 * Add a mod, given only a path for the mod. The mod manager will attempt to
	 * determine a mod name from the path.
	 * @param path The path to the mod's config.json file
	 * @throws Exception
	 * @return The GameMod which was added
	 */
	public GameMod addMod(String path) throws Exception {	
		//Get the mod's contents so we can find its name
		String subDir = new String(path);
		subDir = subDir.replaceAll("[^A-Za-z0-9.]", "_");
		byte[] contents = Utils.downloadCachedFile(path, "mod/" + subDir);
		
		//Can't find it?
		if (contents == null) {
			JOptionPane.showMessageDialog(Main.cfgFrame, "Could not download config file for the mod.", "Error", JOptionPane.ERROR_MESSAGE);
			throw new Exception("Could not download config file for the mod.");
		}
		
		JSONObject object = new JSONObject(new String(contents));
		//Find its name
		if (object.has("name")) {
			String name = object.getString("name");
			
			//Make sure we don't already have it
			if (getMod(name) != null) {
				JOptionPane.showMessageDialog(Main.cfgFrame, "This mod is already installed.", "Error", JOptionPane.ERROR_MESSAGE);
				throw new Exception("Already have a mod with that name.");
			}
			
			//Add it for real now
			return addMod(name, path);
		} else {
			//No name?
			JOptionPane.showMessageDialog(Main.cfgFrame, "Incorrectly formatted config file: Must contain an \\\"name\\\" key.", "Error", JOptionPane.ERROR_MESSAGE);
			throw new Exception("Incorrectly formatted config file: Must contain an \"name\" key.");
		}
	}
	
	public boolean deleteMod(GameMod mod) throws Exception {
		//Make sure we're deleting something we have
		if (!mods.contains(mod)) {
			throw new Exception("Mod doesn't exist in the list?");
		}
		
		//Ask if we want to delete the mod
		int option = JOptionPane.showConfirmDialog(Main.cfgFrame, "Are you sure you want to delete the mod \"" + mod.getConfig("gamename") + "\"", "Marble Blast Launcher", JOptionPane.YES_NO_OPTION);

		//If they decline, stop
		if (option != JOptionPane.YES_OPTION) {
			return false;
		}
		
		//Ask if we want to delete the mod's directory
		File modPath = new File(mod.getGameDirectory(false));
		if (modPath.exists()) {
			option = JOptionPane.showConfirmDialog(Main.cfgFrame, "This mod is currently installed at:\n" +
					modPath.getAbsolutePath() + "\nDo you want to delete this installation?", "Marble Blast Launcher", JOptionPane.YES_NO_OPTION);

			//If they want to, delete it
			if (option == JOptionPane.YES_OPTION) {
				Utils.deleteDirectory(modPath);
			}
		}
		
		//Delete the mod from the list
		mods.remove(mod);
		
		//Delete the mod's cache directory
		Utils.deleteDirectory(new File(Utils.getCacheDirectory(mod.getName())));
		
		//Save the list
		saveMods();
		
		return true;
	}

	public boolean getNeedDefaultMods() {
		return needDefaultMods;
	}
	
	public String getDefaultModName() {
		return defaultMod;
	}
	
	public void setDefaultModName(String modName) {
		this.defaultMod = modName.toString();
		Preferences.userRoot().put(defaultModKey, modName.toString());
	}
}
