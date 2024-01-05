package com.marbleblast.mblauncher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilities classes that perform basic functionality
 * @author higuymb@gmail.com
 */

public class Utils {
	/**
	 * Finds the location of the launcher from which we are running
	 * @return The location of the launcher
	 */
	public static String launcherLocation() {
		String path = SelfUpdater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		if (getOS().equals("Mac")) {
			path = new File(path).getParentFile().getParentFile().getParentFile().toURI().getPath();
		}

		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Main.log(e);
		}
		
		return path;
	}

	/**
	 * Finds the location of the JAR file from which we are running
	 * @return The JAR's location
	 */
	public static String jarLocation() {
		String path = SelfUpdater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Main.log(e);
		}
		
		return path;
	}
	
	/**
	 * Detect if a file is a Symbolic Link (Unix-only, Windows will return false).
	 * @param file The file to examine.
	 * @return Whether the file is a Symbolic Link.
	 */
	public static boolean isSymbolicLink(String file) {
		if (getOS().equals("Windows"))
			return false;

		//HUGE hack
		return exec("file", "-h", file).indexOf("symbolic link to") != -1;
	}
	
	/**
	 * Get the file permissions and statistics (Unix only).
	 * @param file The file to examine.
	 * @return An int[] containing the files permission bitset and its owner
	 */
	public static int[] getPermissions(String file) {
		String fullPath = (new File(file)).getAbsolutePath();

		String stat = exec("stat", "-l", fullPath);
		
		char[] out = stat.toCharArray();
		
		//-rwsrwsr-x
		//-rwSrwSr-x
		//0123456789
		
		int[] flags = new int[2];
		
		if (out[3] == 's' || out[3] == 'S') flags[0] |= 04000;
		if (out[6] == 's' || out[6] == 'S') flags[0] |= 02000;
		if (out[9] == 't' || out[9] == 'T') flags[0] |= 01000;
		if (out[1] == 'r') flags[0] |= 0400;
		if (out[2] == 'w') flags[0] |= 0200;
		if (out[3] == 'x' || out[3] == 's') flags[0] |= 0100;
		if (out[4] == 'r') flags[0] |= 040;
		if (out[5] == 'w') flags[0] |= 020;
		if (out[6] == 'x' || out[6] == 's') flags[0] |= 010;
		if (out[7] == 'r') flags[0] |= 04;
		if (out[8] == 'w') flags[0] |= 02;
		if (out[9] == 'x' || out[9] == 't') flags[0] |= 01;
		
		//-rwsrwsr-x 1 user group
		String[] words = stat.split(" ");
		
		String user = words[2];
		String id = exec("id", "-u", user);
		
		flags[1] = Integer.parseInt(id);
		
		return flags;			
	}
	
	/**
	 * Executes a command via ProcessBuilder, given a list of arguments.
	 * @param command The command to execute
	 * @param args The arguments for the command
	 * @return Either the output of the program, or its return code as a String 
	 */
	public static String exec(String command, String ... args) {
		return _exec(command, false, args);
	}
	
	/**
	 * Executes a command via ProcessBuilder, given a list of arguments, returning either the
	 * output of the program or its exit code.
	 * @param command The command to execute
	 * @param exitCode Whether the program's exit code should be returned
	 * @param args The arguments for the command
	 * @return Either the output of the program, or its return code as a String 
	 */
	public static String exec(String command, boolean exitCode, String ... args) {
		return _exec(command, exitCode, args);
	}
	
	/**
	 * Internal method for executing a command. Do not call this directly.
	 * @param command The command to execute
	 * @param exitCode Whether the program's exit code should be returned
	 * @param args The arguments for the command
	 * @return Either the output of the program, or its return code as a String 
	 */
	private static String _exec(String command, boolean exitCode, String args[]) {
		try {
			List<String> list = new ArrayList<String>();
			list.add(command);
			for (String arg : args) {
				list.add(arg);
			}

			Process p = new ProcessBuilder(list).start();
			InputStream stream = p.getInputStream();
			p.waitFor();
			
			StringBuilder output = new StringBuilder();
			
			int bufferSize = 4096;
			byte[] buffer = new byte[bufferSize];
			
			while (stream.read(buffer, 0, bufferSize) > 0) {
				for (byte b : buffer) {
					output.append(String.format("%c", b));
				}
			}
			
			stream.close();
			
			String out = output.toString().trim();
			if (out.length() == 0 || exitCode)
				out = Integer.toString(p.exitValue());
			
			return out;
		} catch (Exception e) {
			e.printStackTrace();
			Main.log(e);
		}
		
		return null;
	}
	
	/**
	 * Gets the install directory for all games
	 * @return The install directory
	 */
	public static String getInstallDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		String directory = null;

		if (os.startsWith("win")) {
			// Windows XP, Vista, 7, 8
			// Need to check for 32 bit or 64 bit.
			/*
			boolean is64bit = false;
			is64bit = (System.getenv("ProgramFiles(x86)") != null);
			if (is64bit)
				directory = System.getenv("ProgramFiles(x86)") + "/" + gameName;
			else
				directory = System.getenv("ProgramFiles") + "/" + gameName;
			*/
			
			directory = System.getenv("APPDATA");
		} else if (os.startsWith("mac")) {
			// Mac OS X
			directory = System.getProperty("user.home") + "/Applications";
		} else {
			// Linux / other weird OS's 
			directory = System.getProperty("user.home");
		}
		
		directory = directory.replace("\\", "/");

		return directory;
	}

	/**
	 * Gets the old install directory
	 * @return The old install directory
	 */
	public static String getOldInstallDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		String directory = null;

		if (os.startsWith("win")) {
			// Windows XP, Vista, 7, 8
			// Need to check for 32 bit or 64 bit.
			boolean is64bit = false;
			is64bit = (System.getenv("ProgramFiles(x86)") != null);
			if (is64bit)
				directory = System.getenv("ProgramFiles(x86)");
			else
				directory = System.getenv("ProgramFiles");
		} else if (os.startsWith("mac")) {
			// Mac OS X
			directory = System.getProperty("user.home") + "/Library/MarbleBlast";
		} else {
			// Linux / other weird OS's 
			directory = System.getProperty("user.home");
		}
		
		directory = directory.replace("\\", "/");

		return directory;
	}
	
	/**
	 * Get the launcher cache directory for storing cached files.
	 * @param subdir An optional subdirectory
	 * @return The directory as a String
	 */
	public static String getCacheDirectory(String subdir) {
		String os = System.getProperty("os.name").toLowerCase();
		String directory = null;
		
		if (os.startsWith("win")) {
			// Windows XP, Vista, 7, 8
			directory = System.getenv("APPDATA") + "\\.mblaunchercache";
		} else if (os.startsWith("mac")) {
			// Mac OS X
			directory = System.getProperty("user.home") + "/.mblaunchercache";
		} else {
			// Linux / other weird OS's 
			directory = System.getProperty("user.home") + "/.mblaunchercache";
		}
		directory = directory.replace("\\", "/");
		
		if (subdir != null)
			directory = directory + "/" + subdir;
		
		return directory;
	}
	
	/**
	 * Get the launcher cache directory for storing cached files.
	 * @return The directory as a String
	 */
	public static String getCacheDirectory() {
		return getCacheDirectory(null);
	}
	
	/**
	 * Opens a directory in the user's file browser
	 * @param path The path to open
	 */
	public static void openDirectory(String path) {
		if (getOS().equals("Mac") || getOS().equals("Macold")) {
			exec("open", "-R", path);
		} else if (getOS().equals("Windows")) {
			try {
				//HUGE hack, but exec doesn't fix \es in paths
				Runtime.getRuntime().exec("explorer.exe /select," + path.replace('/', '\\'));
			} catch (IOException e) {
				e.printStackTrace();
				Main.log(e);
			}
		}
	}
	

	/**
	 * Gets the system OS
	 * @return The system OS (as a simplified string)
	 */
	public static String getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.startsWith("win")) {
			return "Windows";      
		} else if (os.startsWith("mac")) {
			if (System.getProperty("os.version").startsWith("10.6") || 
				System.getProperty("os.version").startsWith("10.5"))
				return "Macold";
			return "Mac";
		} else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0 ) {
			return "Linux";
		} else if (os.indexOf("sunos") >= 0) {
			return "Solaris";
		} else {
			return "Other";
		}
	}

	/**
	 * Get the MD5 of a file
	 * @param path The file path
	 * @return String The MD5 of the file
	 */
	public static String getFileMD5(String path) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			InputStream stream = new FileInputStream(new File(path));

			int length;
			byte[] data = new byte[2048];
			while ((length = stream.read(data)) != -1) {
				digest.update(data, 0, length);
			}

			stream.close();

			byte[] response = digest.digest();
			StringBuilder sb = new StringBuilder();

			for (byte b : response) {
				sb.append(String.format("%02x", b));
			}

			return sb.toString();
		} catch (Exception e) {
			//Probably just doesn't exist
		}

		return "";
	}
	
	/**
	 * Get the MD5 of a byte array
	 * @param bytes The byte array
	 * @return String The MD5 of the byte array
	 */
	public static String getMD5(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			InputStream stream = new ByteArrayInputStream(bytes);

			int length;
			byte[] data = new byte[2048];
			while ((length = stream.read(data)) != -1) {
				digest.update(data, 0, length);
			}

			stream.close();

			byte[] response = digest.digest();
			StringBuilder sb = new StringBuilder();

			for (byte b : response) {
				sb.append(String.format("%02x", b));
			}

			return sb.toString();
		} catch (Exception e) {
			//Probably just doesn't exist
		}

		return "";
	}
	
	/**
	 * Get the MD5 of a cached file
	 * @param path The path of the file to get the MD5
	 * @param subdir An optional subdirectory to get the cached file under
	 * @return String The MD5 of the cached file
	 */
	public static String getCachedMD5(String path, String subdir) {
		//Null checking in case we're given an invalid path
		if (path == null) {
			return null;
		}
		
		//Update or read from the cache
		String cacheFile = getCacheDirectory(subdir) + "/" + new File(path).getName();

		//Check for a cached copy and return it if we prefer it
		return getFileMD5(cacheFile);
	}

	/**
	 * Downloads a file (synchronously) from the server
	 * @param path The URL path to the file
	 * @return The contents of the url
	 */
	public static byte[] downloadFile(String path) {
		Main.log("Downloading " + path + "...");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		//Try it all, this thing has SO many exceptions
		try {
			//Get it as a URL
			URL url = new URL(path);

			//And grab a stream for it
			URLConnection conn = url.openConnection();
			InputStream stream = conn.getInputStream();

			//Read max length
			int length = Integer.parseInt(conn.getHeaderField("Content-Length"));
			Main.startProgressRegion(length);
			
			int offset = 0;

			//Read buffer
			byte[] buffer = new byte[4096];
			int len;
			while ((len = stream.read(buffer)) > 0) {
				//Spit out buffer into total
				baos.write(buffer, 0, len);
				
				offset += len;
				Main.updateDownloadProgress((int)(offset * 100 / length));
				Main.updateProgress(offset);
			}
			Main.endProgressRegion();
		} catch (IOException e) {
			e.printStackTrace();
			Main.log(e);
			return null;
		}

		return baos.toByteArray();
	}
	
	/**
	 * Get the cached copy of a file as a byte[]
	 * @param path The path for the file to retrieve
	 * @param subdir An optional subdirectory to store the cached file under
	 * @return The contents of the file, or null
	 */
	public static byte[] getCachedFile(String path, String subdir) {
		//Null checking in case we're given an invalid path
		if (path == null) {
			return null;
		}
		
		//Update or read from the cache
		String cacheFile = getCacheDirectory(subdir) + "/" + new File(path).getName();

		//Check for a cached copy and return it if we prefer it
		return readFile(cacheFile);
	}
	
	/**
	 * Download a file and return its contents as a String. If the connection is
	 * offline, the file will attempt to be loaded from the user's Preferences.
	 * If neither method works, null is returned.
	 * @param path The path for the file to download (remote)
	 * @param preferCache Whether to use the cached version if one exists, and skip downloading
	 * @param subdir An optional subdirectory to store the cached file under
	 * @return The contents of the file, or null
	 */
	public static byte[] downloadCachedFile(String path, boolean preferCache, String subdir) {
		//Null checking in case we're given an invalid path
		if (path == null) {
			return null;
		}
		
		//Update or read from the cache
		String cacheFile = getCacheDirectory(subdir) + "/" + new File(path).getName();

		//Check for a cached copy and return it if we prefer it
		byte[] cachedCopy = readFile(cacheFile);
		if (cachedCopy != null && preferCache)
			return cachedCopy;
		
		//Try to download the file if we can
		byte[] contents = downloadFile(path);
		
		if (contents == null) {
			//If we can't download, then try the cached copy
			contents = cachedCopy;
		} else {
			//If we can download it, cache it for future use
			writeFile(contents, cacheFile);
		}
		
		//No matter the success state, return the contents
		return contents;
	}
	
	/**
	 * Download a file and return its contents as a String. If the connection is
	 * offline, the file will attempt to be loaded from the user's Preferences.
	 * If neither method works, null is returned.
	 * @param path The path for the file to download (remote)
	 * @param preferCache Whether to use the cached version if one exists, and skip downloading
	 * @return The contents of the file, or null
	 */
	public static byte[] downloadCachedFile(String path, boolean preferCache) {
		return downloadCachedFile(path, preferCache, null);
	}
	
	/**
	 * Download a file and return its contents as a String. If the connection is
	 * offline, the file will attempt to be loaded from the user's Preferences.
	 * If neither method works, null is returned.
	 * @param path The path for the file to download (remote)
	 * @param subdir An optional subdirectory to store the cached file under
	 * @return The contents of the file, or null
	 */
	public static byte[] downloadCachedFile(String path, String subdir) {
		return downloadCachedFile(path, false, subdir);
	}
	
	/**
	 * Download a file and return its contents as a String. If the connection is
	 * offline, the file will attempt to be loaded from the user's Preferences.
	 * If neither method works, null is returned.
	 * @param path The path for the file to download (remote)
	 * @return The contents of the file, or null
	 */
	public static byte[] downloadCachedFile(String path) {
		return downloadCachedFile(path, false, null);
	}
	
	/**
	 * Reads a file from disk.
	 * @param file The file to read
	 * @return The contents of the file
	 */
	public static byte[] readFile(File file) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			//Read the file from the URI
			FileInputStream reader = new FileInputStream(file);
					
			byte[] buffer = new byte[1024];

			while ((reader.read(buffer, 0, 1024)) > 0) {
				baos.write(buffer);
			}
			reader.close();
		} catch (IOException e) {
			//Probably just doesn't exist
			return null;
		}
		return baos.toByteArray();
	}
	
	/**
	 * Reads a file from disk.
	 * @param path The path of the file to read
	 * @return The contents of the file
	 */
	public static byte[] readFile(String path) {
		return readFile(new File(path));
	}
	
	/**
	 * Writes a file to disk.
	 * @param contents The string to write
	 * @param file The file to write
	 */
	public static void writeFile(byte[] contents, File file) {
		file.getParentFile().mkdirs();
		try {
			//Read the file from the URI
			FileOutputStream writer = new FileOutputStream(file);
			writer.write(contents);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			Main.log(e);
		}
	}
	
	/**
	 * Write a file to disk.
	 * @param contents The string to write
	 * @param path The path of the file to write
	 */
	public static void writeFile(byte[] contents, String path) {
		writeFile(contents, new File(path));
	}
	
	/**
	 * Creates and returns a zip archive of a given file.
	 * @param file The file to be contained in the archive.
	 * @return A byte representation of the archive.
	 */
	public static byte[] zipFile(File file) {
		//Create some streams for writing the zip file
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ZipOutputStream zipStream = new ZipOutputStream(stream);
		
		//Zip entry (temporary) for the console
		ZipEntry entry = new ZipEntry(file.getName());
		try {
			//Add entry to the zip
			zipStream.putNextEntry(entry);

			//Read the file into the zip
			InputStream fileStream = new FileInputStream(file);
			
			byte[] bytes = new byte[1024];
			int len = 0;
			while ((len = fileStream.read(bytes, 0, 1024)) != -1) {
				zipStream.write(bytes, 0, len);
			}
			
			//Close the streams and finish em off
			fileStream.close();
			zipStream.closeEntry();
			zipStream.flush();
			zipStream.finish();
			
			//Spit out the zip contents into a stream to read
			byte[] zipConts = stream.toByteArray();
			stream.close();
			zipStream.close();
			
			//And return the zip contents
			return zipConts;
		} catch (Exception e) {
			e.printStackTrace();
			Main.log(e);
		}
		return null;
	}

	/**
	 * Creates and returns a zip archive of a given file.
	 * @param file The file to be contained in the archive.
	 * @return A byte representation of the archive.
	 */
	public static byte[] zipFiles(List<File> files) {
		//Create some streams for writing the zip file
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ZipOutputStream zipStream = new ZipOutputStream(stream);
		
		for (File file : files) {
			//Zip entry (temporary) for the console
			ZipEntry entry = new ZipEntry(file.getName());
			try {
				//Add entry to the zip
				zipStream.putNextEntry(entry);
	
				//Read the file into the zip
				InputStream fileStream = new FileInputStream(file);
				
				byte[] bytes = new byte[1024];
				int len = 0;
				while ((len = fileStream.read(bytes, 0, 1024)) != -1) {
					zipStream.write(bytes, 0, len);
				}
				
				//Close the streams and finish em off
				fileStream.close();
				zipStream.closeEntry();
			} catch (Exception e) {
				e.printStackTrace();
				Main.log(e);
			}
		}
		try {
			zipStream.flush();
			zipStream.finish();
			
			//Spit out the zip contents into a stream to read
			byte[] zipConts = stream.toByteArray();
			stream.close();
			zipStream.close();
			
			//And return the zip contents
			return zipConts;
		} catch (Exception e) {
			e.printStackTrace();
			Main.log(e);
		}
		return null;
	}
	
	/**
	 * Copy a file from one location to another
	 * @param sourceFile The file to copy from
	 * @param destFile The file to copy to
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileInputStream fis = null;
	    FileOutputStream fos = null;
	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	    	fis = new FileInputStream(sourceFile);
	        source = fis.getChannel();
	        fos = new FileOutputStream(destFile);
	        destination = fos.getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	    	if (fis != null) {
	    		fis.close();
	    	}
	    	if (fos != null) {
	    		fos.close();
	    	}
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}

	/**
	 * Delete a directory, recursively.
	 * @param directory The directory to delete
	 * @return If there was success
	 */
	public static boolean deleteDirectory(File directory) {
		//Try to delete it normally
		if (directory.delete())
			return true;
		
		//Delete all subfiles
		File[] subs = directory.listFiles();
		for (File sub : subs) {
			//Try to delete the sub, if we fail then everything fails
			if (!deleteDirectory(sub))
				return false;
		}
		
		//And finally delete the empty directory
		return directory.delete();
	}
	
	/**
	 * Checks for server availability
	 * @return Whether or not the host "marbleblast.com" could be reached
	 */
	public static boolean hostAvailabilityCheck() { 
	    try {
	    	Socket s = new Socket("marbleblast.com", 80);
	    	s.close();
	        return true;
	    } catch (IOException e) {
	    	Main.log(e);
	    }
	    return false;
	}
}
