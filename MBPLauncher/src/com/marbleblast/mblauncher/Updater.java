package com.marbleblast.mblauncher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import org.json.JSONObject;

/**
 * The main updater class which does all the everything
 * @author HiGuy Smith
 * @email higuymb@gmail.com
 * @date 2014-08-07
 *
 */

public class Updater {
	
	private static final int MAX_TRIES = 4;
	
	GameMod mod;
	Migrator migrator;

	boolean checking, success, cancelUpdate, fullUpdate, ignoreCache;
	int fileCount = 0;
	int filesChecked = 0;
	String installDir;

	ArrayList<String> packagesToUpdate;
	ArrayList<String> filesToUpdate;
	Map<String, String> packageAssoc;
	Map<String, String> packageList;
	
	//Runs the actual updater
	final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			installDir = mod.getGameDirectory(false);
			boolean installExists = (new File(installDir)).exists();
			installDir = mod.getGameDirectory(true);

			Main.startProgressRegion(5);
			if (installExists) {
				if (ignoreCache) {
					Main.setSectionWeight(0, 0.05f);
					Main.setSectionWeight(1, 0.05f);
					Main.setSectionWeight(2, 0.1f);
					Main.setSectionWeight(3, 0.5f);
					Main.setSectionWeight(4, 0.3f);
				} else {
					Main.setSectionWeight(0, 0.05f);
					Main.setSectionWeight(1, 0.05f);
					Main.setSectionWeight(2, 0.1f);
					Main.setSectionWeight(3, 0.3f);
					Main.setSectionWeight(4, 0.5f);
				}
			} else {
				Main.setSectionWeight(0, 0.01f);
				Main.setSectionWeight(1, 0.01f);
				Main.setSectionWeight(2, 0.01f);
				Main.setSectionWeight(3, 0.1f);
				Main.setSectionWeight(4, 0.5f);
			}

			/*
			 * Step 0: Make sure we're on the right version
			 */
			migrator.checkMigration();
			
			/*
			 * Step 1: Download the package listing
			 */
			{
				Main.startProgressRegion(2);
				packageList = new HashMap<String, String>();
	
				byte[] packlisting = Utils.downloadCachedFile((String)mod.getConfig("packages"), mod.getName());
				if (packlisting == null) {
					Main.log("Could not find package listing for mod " + mod.getName() + ". The mod will not be able to be updated.");
					return;
				}
				Main.updateProgress(1);
				
				JSONObject object = new JSONObject(new String(packlisting));
				String names[] = JSONObject.getNames(object);
				
				for (String name : names) {
					//Basic lookup table
					Main.log("Got package name: " + name + ", address is " + object.getString(name));
					packageList.put(name, object.getString(name));
				}
				Main.endProgressRegion();
			}
			Main.updateProgress(1);
			pruneFiles();
			Main.updateProgress(2);

			byte[] listing = Utils.downloadFile((String)mod.getConfig("listing"));
			
			//If the listing doesn't download, we can't launch.
			if (listing == null) {
				//Let them know
				int response = JOptionPane.showOptionDialog(Main.frame, (String)mod.getConfig("offlinemessage"), (String)mod.getConfig("offlinetitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
				if (response == JOptionPane.YES_OPTION) {	
					Main.launch(mod, false, null);
				}
				
				return;
			}

			Main.updateProgress(3);
			
			//Set these
			checking = true;
			success = true;
			cancelUpdate = false;
			
			String cachedListingMD5 = Preferences.userRoot().get("listingMD5", "");
			String newListingMD5 = Utils.getMD5(listing);
			
			Preferences.userRoot().put("listingMD5", "");
			
			//If we don't have anything, download the whole game
			if (!installExists) {
				fullUpdate = true;
				success = false;
				installExists = true;
				
				Main.log("No game found; doing full update.");
			} else {
				fullUpdate = false;
				
				packagesToUpdate = new ArrayList<String>();
				filesToUpdate = new ArrayList<String>();
				packageAssoc = new HashMap<String, String>();

				if (!cachedListingMD5.equals(newListingMD5)) {
					Main.log("Listing doesn't match, updating files.");
					ignoreCache = true;
				}

				if (ignoreCache) {
					Main.log("Checking all files to see if any are out of date... This may take a bit.");

					//Get the JSON of the listing
					JSONObject object = new JSONObject(new String(listing));
		
					//How many files do we have?
					fileCount = countFiles(object);
		
					Main.startProgressRegion(fileCount);
		
					//And iterate over every file in our game!
					objectIterate(object, null);
		
					Main.endProgressRegion();

					//Unset
					checking = false;

					//What is the final status?
					Main.log("Final status: " + (success ? "true" : "false"));
				} else {
					Main.log("Listing matches, no need to update.");
				}
			}
			Main.updateProgress(4);
				
			//If we have a success, launch the game
			if (!success) {
				//Update instead
				updateFiles();
				
				//If the update fails
				if (cancelUpdate)
				{
					//It's not going to launch, is it
					Main.log("Launch error: Files invalid.");
					Main.setOffline(true);
					Main.endProgressRegion();
					return;
				}
			}

			Main.updateProgress(5);
			
			//Update the cached MD5
			Preferences.userRoot().put("listingMD5", newListingMD5);
			
			//Check and copy prefs
			copyPrefs();

			//Check prefs for an update as well
			migrator.copyPrefs(null, true);
			
			//Actual launch point
			Main.launch(mod, true, null);

			//And quit
			System.exit(0);
			
			return;
			
		}
	};
	
	/**
	 * Construct an Updater for a given mod
	 * @param mod The mod for the updater to use for config
	 */
	public Updater(GameMod mod) {
		this.mod = mod;
		this.migrator = new Migrator(mod);
	}
	
	/**
	 * Get the updater's migrator
	 * @return The updater's migrator
	 */
	public Migrator getMigrator() {
		return migrator;
	}

	/**
	 * Starts a Thread for the checker
	 */
	public void checkFiles(boolean ignoreCache) {
		//Run the updater
		Thread t = new Thread(runnable);
		t.start();
		this.ignoreCache = ignoreCache;
	}

	/**
	 * Iterate over a JSONObject
	 * @param object The object to iterate over
	 * @param path The base path for files
	 */
	public void objectIterate(JSONObject object, String path) {
		//Iterate over every object on object
		String names[] = JSONObject.getNames(object);
		if (filesChecked > fileCount) {
			filesChecked = 0;
		}

		String discLog = "";
		
		for (int i = 0; i < names.length; i++) {
			//For each of the names, get the info of the object
			Object sub = object.get(names[i]);

			if (sub instanceof JSONObject) {
				if (((JSONObject) sub).has("md5") && ((JSONObject) sub).has("package")) {

					// Skip the package if we are already going to update it
					String pkg = ((JSONObject) sub).getString("package").toLowerCase();
					if (packagesToUpdate.contains(pkg))
						continue;
					
					//Get our MD5
					String file;
					if (path == null)
						file = installDir.concat(String.format("/%s", names[i]));
					else
						file = installDir.concat(String.format("/%s/%s", path, names[i]));

					String md5 = Utils.getFileMD5(file);

					//Get the server MD5
					String serverMD5 = ((JSONObject) sub).getString("md5");

					//Compare them
					if (md5.equals(serverMD5)) {
						//Success, ignore
						//Main.log("File validated: " + names[i]);
					} else {
						try {
							if (Utils.isSymbolicLink(new File(file).getCanonicalPath()))
								continue;
						} catch (IOException e) {
							e.printStackTrace();
							Main.log(e);
						}

						//Discrepancy, we can't launch!
						discLog = discLog + "Discrepancy in file: " + names[i] + " (" + md5 + " != " + serverMD5 + ")\n";
						success = false;

						// Add the package to the list to we can skip it next time
						packagesToUpdate.add(pkg);
						
						//Add the file to the list to update
						if (path == null) {
							filesToUpdate.add(names[i]);
							packageAssoc.put(names[i], ((JSONObject) sub).getString("package"));
						} else {
							filesToUpdate.add(String.format("%s/%s", path, names[i]));
							packageAssoc.put(String.format("%s/%s", path, names[i]), ((JSONObject) sub).getString("package"));
						}
					}

					filesChecked ++;

					Main.updateProgress(filesChecked);
				} else {
					//If we get another JSONObject, it's a directory which we can iterate over
					if (path == null)
						objectIterate((JSONObject)sub, names[i]);
					else
						objectIterate((JSONObject)sub, path.concat(String.format("/%s", names[i])));
				}
			}

			//If we've hit an error, just fail here
			if (!checking)
				return;
		}

		//Only print out lines if we have some
		if (discLog.length() > 0) //Chop off the last \n
			Main.log(discLog.substring(0, discLog.length() - 1));
	}
	

	/**
	 * Counts the number of files in the JSONObject
	 * @param object The object to iterate over
	 */
	public int countFiles(JSONObject object) {
		//Iterate over every object on object
		String names[] = JSONObject.getNames(object);

		int count = 0;

		for (int i = 0; i < names.length; i++) {
			//For each of the names, get the info of the object
			Object sub = object.get(names[i]);

			if (sub instanceof String) {
				//Files just add
				count ++;
			} else if (sub instanceof JSONObject) {
				if (((JSONObject) sub).has("md5") && ((JSONObject) sub).has("package")) {
					count ++;
				} else {
					//If we get another JSONObject, it's a directory which we can iterate over
					count += countFiles((JSONObject)sub);
				}
			}
		}

		return count;
	}

	public void pruneFiles() {
		byte[] listing = Utils.downloadCachedFile((String)mod.getConfig("prunelist"), mod.getName());
		
		if (listing == null)
			return;
		
		//Get the JSON of the listing
		JSONObject object = new JSONObject(new String(listing));
		
		pruneIterate(object, null);
	}
	
	public void pruneIterate(JSONObject object, String path) {
		//Iterate over every object on object
		String names[] = JSONObject.getNames(object);
		
		if (names == null)
			return;

		for (int i = 0; i < names.length; i++) {
			//For each of the names, get the info of the object
			Object sub = object.get(names[i]);

			if (sub instanceof String) {
				//Plain strings are just files

				//Get our MD5
				String file;
				if (path == null)
					file = installDir.concat(String.format("/%s", names[i]));
				else
					file = installDir.concat(String.format("/%s/%s", path, names[i]));
				
				File f = new File(file);
				if (f.exists()) {
					Main.log("Deleting " + file);
					f.delete();
				}				
			} else if (sub instanceof JSONObject) {
				//If we get another JSONObject, it's a directory which we can iterate over
				if (path == null)
					pruneIterate((JSONObject)sub, names[i]);
				else
					pruneIterate((JSONObject)sub, path.concat(String.format("/%s", names[i])));
			}
		}
	}
	
	public void updateFiles() {
		if (fullUpdate) {
			Main.startProgressRegion(packageList.keySet().size());

			//And apply any needed patches
			int i = 0;
			for (String patch : packageList.keySet()) {
				getPatch(patch);
				if (cancelUpdate) {
					Main.endProgressRegion();
					return;
				}
				i ++;
				Main.updateProgress(i);
			}
			Main.endProgressRegion();
		} else {
			//We need to update the files in fileCount
			ArrayList<String> patches = new ArrayList<String>();

			for (String file : filesToUpdate) {
				Main.log("We need to update " + file);
				//Determine which patch it's in
				
				String pack = packageAssoc.get(file);
				if (!patches.contains(pack)) {
					patches.add(pack);
				}
			}

			Main.startProgressRegion(patches.size());

			//And apply any needed patches
			int i = 0;
			for (String patch : patches) {
				getPatch(patch + ".zip");
				if (cancelUpdate) {
					Main.endProgressRegion();
					return;
				}
				i ++;
				Main.updateProgress(i);
			}
			Main.endProgressRegion();
		}
	}

	public void getPatch(String patchName) {
		//Let us know
		Main.log("Getting patch " + patchName);

		//Check if we already have it
		if (new File(installDir + "/" + patchName).exists()) {
			Main.log("Applying previously found patch " + patchName);
			//And apply
			if (applyPatch(patchName)) {
				return;
			} else {
				new File(installDir + "/" + patchName).delete();
			}
			Main.log("Previously found patch had an error: " + patchName);
		}

		try {
			
			int tries;
			for (tries = 1; tries < MAX_TRIES; tries++)
			{
				// Ensure that there is a "Marble Blast Platinum" directory in program files!
				File directory = new File(installDir);
				if (!directory.exists())
					directory.mkdirs();
				
				//Hardcoded because lazy
				URL url = new URL(packageList.get(patchName));
	
				Main.log("Downloading " + packageList.get(patchName) + "...");
	
				//Create a new HTTPConnection and get the file
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("GET");
				connection.setDoOutput(true);
	
				//Streams and shit
				InputStream stream = connection.getInputStream();
				FileOutputStream output = new FileOutputStream(installDir + "/" + patchName);
	
				//How long is it going to be?
				int length = Integer.parseInt(connection.getHeaderField("Content-Length"));
				Main.startProgressRegion(length);

				//Some buffer
				byte[] buffer = new byte[4096];
	
				//Read it all!
				int len, offset = 0;
				while ((len = stream.read(buffer)) > 0) {
					//Instantly write it to disk
					output.write(buffer, 0, len);
	
					//Offset
					offset += len;
	
					//Tell us
					Main.updateDownloadProgress(100 * offset / length);
				}

				Main.endProgressRegion();

				//Close the file
				output.flush();
				output.close();
	
				//Tell us
				Main.log("Got " + offset + " bytes from server.");
	
				//Disconnect (almost forgot)
				connection.disconnect();
	
				//And apply the patch
				if (applyPatch(patchName))
				{
					break;
				} else {
					Main.log("Failed to apply patch " + patchName + ", redownloading. (Attempt " + (tries + 1) + " of " + MAX_TRIES);
				}
			}
			if (tries >= MAX_TRIES)
			{
				cancelUpdate = true;
				Main.log("Update failed after " + (tries + 1) + " tries to apply patch: " + patchName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Main.log(e);

			cancelUpdate = true;
			Main.log("Failed to download patch: " + patchName);
		}
	}

	public boolean applyPatch(String patchName) {
		Main.log("Applying patch " + patchName);
		try {
			//Open it and unzip
			FileInputStream fileInput = new FileInputStream(installDir + "/" + patchName);
			ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(fileInput)); 

			//Read every entry
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				if (entry.isDirectory()) {

					//Create directories if we need them
					File directory = new File(installDir + "/" + entry.getName());
					if (!directory.exists()) {

						//Make all dirs
						directory.mkdirs();
					}
				} else {

					//If we don't need to update this file, don't
					/*if (!fullUpdate && !filesToUpdate.contains(entry.getName())) {
						Main.log("Skipping " + entry.getName());
						continue;
					}*/
					
					File directory = new File(installDir + "/" + entry.getName()).getParentFile();
					if (!directory.exists()) {
						//Make all dirs
						directory.mkdirs();
					}

					//Do update it
					Main.log("Applying patch: " + entry.getName());

					//Streams for reading the zip
					int bufferSize = 2048;
					FileOutputStream fos = new FileOutputStream(installDir + "/" + entry.getName());
					BufferedOutputStream output = new BufferedOutputStream(fos, bufferSize);

					//Buffer
					int count;
					byte[] data = new byte[bufferSize];

					//Read the file
					while ((count = zipInput.read(data, 0, bufferSize)) != -1) {
						//Write it to disk instantly
						output.write(data, 0, count);
					}

					//And close
					output.flush();
					output.close();
				}
			}
			zipInput.close();
			
			// Remove downloaded zip files - yay more space!
			new File(installDir + "/" + patchName).delete();

		} catch (Exception e) {
			e.printStackTrace();
			Main.log(e);
			return false;
		}

		new File(installDir + "/" + patchName).delete();
		return true;
	}
	
	public void sendConsole() {
		//Quick config check
		if (!((String)mod.getConfig("doconsolepost")).equals("true"))
			return;
		installDir = mod.getGameDirectory(true);
		
		//Ask them if they want to send it (so we don't get any crazies)
		Preferences prefs = Preferences.userRoot();
		int response = prefs.getInt("sendConsole", JOptionPane.NO_OPTION);
		boolean notify = false;
		if (response == JOptionPane.NO_OPTION) {
			//But if they accept, don't bother them in the future.
			response = JOptionPane.showOptionDialog(Main.frame, (String)mod.getConfig("consolepostmessage"), (String)mod.getConfig("consoleposttitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
			prefs.putInt("sendConsole", response);
			notify = true;
			try {
				prefs.sync();
			} catch (BackingStoreException e) {
				Main.log(e);
				e.printStackTrace();
			}
		}
		
		//If they agreed, send that stuff right away
		if (response == JOptionPane.YES_OPTION) {
			ArrayList<File> consoleFiles = new ArrayList<File>();
			if (Utils.getOS().toLowerCase().startsWith("mac")) {
				// /Users/<user>/Library/Logs/DiagnosticReports
				File crashDir = new File(System.getProperty("user.home") + "/Library/Logs/DiagnosticReports");
				File[] conts = crashDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.contains("MarbleBlast Gold") && !name.startsWith(".");
					}
				});
				if (conts != null && conts.length > 0) {
					// The crash reporter keeps up to 10 dumps. Take the newest one.
					File newestCrash = Collections.max(Arrays.asList(conts), new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							return Long.compare(o1.lastModified(), o2.lastModified());
						}
					});
					consoleFiles.add(newestCrash);
				}
			} else if (Utils.getOS().toLowerCase().startsWith("win")) {
				File crashDir = new File(installDir + "/crashes");
				File[] crashes = crashDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".dmp");
					}
				});
				if (crashes != null && crashes.length > 0) {
					// The crash reporter keeps up to 10 dumps. Take the newest one.
					File newestCrash = Collections.max(Arrays.asList(crashes), new Comparator<File>() {
						@Override
						public int compare(File o1, File o2) {
							return Long.compare(o1.lastModified(), o2.lastModified());
						}
					});
					consoleFiles.add(newestCrash);
				} else {
					// Dumps contain the console log, so only add this if no dump was found or else there could be a mismatch
					consoleFiles.add(new File(installDir + "/console.log"));
				}
			}
			sendConsoleFiles(consoleFiles, (String)mod.getConfig("consolepostcrashattachmentname"), (String)mod.getConfig("consolepostcrashattachmentfile"), notify);
		}
	}
	
	protected void sendConsoleFiles(List<File> consoleFiles, String attachmentName, String attachmentFileName, boolean notify) {
		//Kudos to http://stackoverflow.com/a/11826317/214063
		//Essentials
		String crlf = "\r\n";
		String boundary =  "*****";

		try {
			//Zip consoles
			byte[] zipConts = Utils.zipFiles(consoleFiles);

			//Create a new POST request for sending the console
			HttpURLConnection connection = (HttpURLConnection)new URL((String)mod.getConfig("consolepost")).openConnection();
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			
			//Set properties
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Cache-Control", "no-cache");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			connection.addRequestProperty("Content-length", zipConts.length + "");

			//Start writing the file wrapper
			DataOutputStream request = new DataOutputStream(connection.getOutputStream());

			//Header
			request.writeBytes("--" + boundary + crlf);
			request.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\"" + crlf);
			request.writeBytes(crlf);
			
			request.write(zipConts);

			//Footer
			request.writeBytes(crlf);
			request.writeBytes("--" + boundary + "--" + crlf);
			
			//Clean up
			request.flush();
			request.close();
			connection.disconnect();
			
			//Check if it actually sents
	        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
		        	//Get the response, set up vars
		        	InputStream responseStream = new BufferedInputStream(connection.getInputStream());
		        	BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
		        	String line = "";
		        	StringBuilder stringBuilder = new StringBuilder();
	
		        	//Actually read the response
		        	while ((line = reader.readLine()) != null) {
		        	    stringBuilder.append(line).append("\n");
		        	}
		        	
		        	//Clean up
		        	reader.close();
		        	
		        	//Show the response as a message box
		        	if (notify) {
		        		JOptionPane.showMessageDialog(Main.frame, stringBuilder.toString());
		        	}
	        }
		} catch (IOException e) {
			e.printStackTrace();
			Main.log(e);
		}
	}
	
	public void copyPrefs() {
		if (!((String)mod.getConfig("docopyprefs")).equals("true"))
			return;
		
		//Check for prefs
		JSONObject copyList = (JSONObject)mod.getConfig("copydata");
		
		String names[] = JSONObject.getNames(copyList);
		if (names == null)
			return;
		
		boolean asked = false;
		
		for (int i = 0; i < names.length; i++) {
			//For each of the names, get the info of the object
			Object sub = copyList.get(names[i]);

			//names[i] => old position
			//sub => new position
			
			if (sub instanceof String) {
				//Plain strings are just files
				String file = installDir.concat(String.format("/%s", sub));
				
				File f = new File(file);
				if (!f.exists()) {
					//If the new build has no prefs, and the old build does, vacuum them up! 
					
					String oldFile = mod.getOldGameDirectory().concat(String.format("/%s", names[i]));
					File oldf = new File(oldFile);
					if (oldf.exists()) {
						//Ask them first
						if (!asked && ((String)mod.getConfig("copyprefsask")).equals("true")) {
							int response = JOptionPane.showOptionDialog(Main.frame, (String)mod.getConfig("copyprefsmessage"), (String)mod.getConfig("copyprefstitle"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
							if (response != JOptionPane.YES_OPTION) {
								return;
							}
							asked = true;
						}
						
						Main.log("Restoring " + file);
						
						try {
							//Copy the file over
							FileReader reader = new FileReader(oldf);
							FileWriter writer = new FileWriter(f);
							
							char[] buffer = new char[1024];
							int len = 0;
							while ((len = reader.read(buffer, 0, 1024)) > 0) { 
								writer.write(buffer, 0, len);
							}
							//Clean up
							reader.close();
							writer.flush();
							writer.close();
							
							Main.log("Restored " + file);
						} catch (Exception e) {
							e.printStackTrace();
							Main.log(e);
						}						
					}
				}
			} else {
				//What
				//Invalid file or something
			}
		}
	}
}