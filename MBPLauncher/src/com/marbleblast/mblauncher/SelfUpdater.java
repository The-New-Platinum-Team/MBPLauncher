package com.marbleblast.mblauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SelfUpdater {
	
	public static void check() {
		if (!((String)Config.get("selfupdate")).equals("true")) {
			Main.log("Automatic updates are disabled. If there's a new update, go download it manually.");
			return;
		}
		if (Utils.hostAvailabilityCheck()) {
			Main.log("Checking for a new launcher...");
			String jarLocation = Utils.jarLocation();
			File jarFile = new File(jarLocation);
			File tmpJarFile = new File(jarLocation + ".tmp");

			//We can't run this if we're not in a jar!
			if (jarFile.isDirectory()) {
				Main.log("Non-executable build, cannot update.");
				return;
			}
			
			String jarMD5 = Utils.getFileMD5(jarLocation);
			
			if (!jarMD5.equals((String)Config.get("launchermd5"))) {
				Main.log("Current launcher sumcheck (" + jarMD5 + ") does not match server version (" + (String)Config.get("launchermd5") + ")");
				
				//New jar version, go get it
				String newJarLocation = (String)Config.get("launcher");

				Main.log("Downloading " + newJarLocation + "...");
				
				try {
					//Create a new HTTPConnection and get the file
					HttpURLConnection connection = (HttpURLConnection)new URL(newJarLocation).openConnection();
					connection.setInstanceFollowRedirects(false);
					connection.setRequestMethod("GET");
					connection.setDoOutput(true);

					if (connection.getResponseCode() == 200) {
						//Streams and shit
						InputStream stream = connection.getInputStream();
						FileOutputStream output = new FileOutputStream(tmpJarFile);
		
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
							Main.updateProgress(offset);
						}
						Main.endProgressRegion();
						output.flush();
						output.close();
						
						stream.close();
		
						//New version, replace current file with it
						jarFile.delete();
						Utils.copyFile(tmpJarFile, jarFile);
						tmpJarFile.delete();
						
						Main.log("Downloaded a new build, ready to restart.");
						Main.restart();
					}
					connection.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
					Main.log(e);
				}
			} else {
				Main.log("Current launcher sumcheck matches that of the server. Nothing to update.");
			}
		}
	}
}
