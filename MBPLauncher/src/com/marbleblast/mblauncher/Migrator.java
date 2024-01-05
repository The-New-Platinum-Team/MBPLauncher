package com.marbleblast.mblauncher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class Migrator {
	private GameMod mod;
	
	public Migrator(GameMod mod) {
		this.mod = mod;
	}
	
	public void checkMigration() {
		//Migrate directories and their contents if necessary

		String baseDirectory = mod.getGameDirectory(true);

		//Download migrations list
		String migrationsAddress = (String)mod.getConfig("migrations");
		byte[] migrationsContents = Utils.downloadCachedFile(migrationsAddress, mod.getName());
		
		if (migrationsContents == null) {
			return;
		}
		
		JSONObject migrationsObject = new JSONObject(new String(migrationsContents));
		
		//Get it as a list
		String migrations[] = JSONObject.getNames(migrationsObject);
		
		if (migrations == null) {
			return;
		}
		
		for (String migration : migrations) {
			File fromDirectory = new File(baseDirectory, migration);
			if (fromDirectory.exists()) {
				File toDirectory = new File(baseDirectory, migrationsObject.getString(migration));
				
				//Actual migration is in its own function
				migrateDirectory(fromDirectory, toDirectory);
			}
		}
	}
	
	public void migrateDirectory(File fromDirectory, File toDirectory) {
		//Make sure the directory exists that we're migrating to
		toDirectory.mkdirs();
		
		//Iterate over all files in this directory
		for (File fromFile : fromDirectory.listFiles()) {
			//Where are we copying to?
			File toFile = new File(toDirectory, fromFile.getName());
			
			//If it's a directory, recurse and conquer... I think that's how that saying goes
			if (fromFile.isDirectory()) {
				migrateDirectory(fromFile, toFile);
				continue;
			}
			
			//Try to copy the file
			try {
				Utils.copyFile(fromFile, toFile);

				//And delete the old one
				fromFile.delete();
			} catch (IOException e) {
				e.printStackTrace();
				Main.log(e);
			}
		}
		
		//And delete this directory, as it'll be empty
		fromDirectory.delete();
	}
	
	public void copyPrefs(File fromFile, boolean overwrite) {
		//Copy prefs from fromFile and paste them at the end of MBPPrefs.cs
		
		//Final file
		File toFile = new File(mod.getGameDirectory(true) + (String)mod.getConfig("prefsfile"));
		
		if (fromFile == null) {
			fromFile = toFile;
		}
		
		try {
			//Download searches list
			String searchesAddress = (String) mod.getConfig("searches");
			byte[] searchesContents = Utils.downloadCachedFile(searchesAddress, mod.getName());
			if (searchesContents == null) {
				return;
			}
			
			JSONObject searchesObject = new JSONObject(new String(searchesContents));
			
			//Get it as a list
			String searches[] = JSONObject.getNames(searchesObject);
			
			//Download conversions list
			String conversionsAddress = (String) mod.getConfig("conversions");
			byte[] conversionsContents = Utils.downloadCachedFile(conversionsAddress);
			if (conversionsContents == null) {
				return;
			}
			
			JSONObject conversions = new JSONObject(new String(conversionsContents));
			
			//If it's empty, then don't even bother
			if (JSONObject.getNames(conversions) == null) {
				return;
			}
			
			//Read the whole file and modify things
			BufferedReader reader = new BufferedReader(new FileReader(fromFile));
			ArrayList<String> additions = new ArrayList<String>();
			
			String line;
			while ((line = reader.readLine()) != null) {
				boolean added = false;
			
				//Check the line for various things
				for (String search : searches) {
					if (Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE).matcher(line).find()) {
						//Replace all the things
						for (String from : JSONObject.getNames(conversions)) {
							String to = conversions.getString(from);
							line = line.replaceAll("(?i)"+from, to);
						}
						
						//Now add it
						additions.add(line);
						added = true;
						break;
					}
				}
				
				if (!added && overwrite) {
					additions.add(line);
				}
			}
			
			reader.close();
			
			if (additions.size() > 0) {
				if (overwrite) {
					Utils.copyFile(toFile, new File(toFile.getPath() + ".old"));
				}
				
				//Yes, I did put the line ending in the config (it's \r\n atm)
				String lineEnd = (String)mod.getConfig("lineending");
			
				//Now we actually write out to the file
				BufferedWriter writer = new BufferedWriter(new FileWriter(toFile, !overwrite));
				for (String addition : additions) {
					writer.write(addition + lineEnd);
				}
				
				//Clean up
				writer.flush();
				writer.close();
				
				Main.log("Copied/replaced " + additions.size() + " pref lines.");
			} else {
				Main.log("Nothing to copy, continuing.");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			Main.log(e);
		}
	}
}
