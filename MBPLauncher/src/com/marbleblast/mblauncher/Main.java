package com.marbleblast.mblauncher;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.*;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

	public static boolean directoryExists = false;
	public static LauncherFrame frame;
	public static ConfigModFrame cfgFrame;

	private static ModManager modManager;
	private static WindowListener quitListener;
	
	/**
	 * It's the main function. Get over it.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (IllegalAccessException ex)
		{
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		// Parse args and check for marbleland
		if (args.length > 0)
		{
			if (args[0].startsWith("platinumquest://"))
			{
				String cmd = args[0].substring("platinumquest://".length());
				if (cmd.indexOf('/') != -1) {
					String[] split = cmd.split("/");
					split[0] = "-" + split[0];

					modManager = new ModManager();
					try {
						if (modManager.getNeedDefaultMods()) {
							modManager.addMod("http://marbleblast.com/pq/config/config.json");
							// modManager.addMod("http://fubar.marbleblast.com/config/config.json"); rip fubar
						}

                        // Check if PQ is already running so we can
                        try {
                            Socket rpcClient = new Socket("127.0.0.1", 20248);
                            PrintWriter osw = new PrintWriter(rpcClient.getOutputStream());
                            osw.println(cmd.replace('/', ' '));
                            osw.flush();
                            rpcClient.close();
                            return;
                        } catch (IOException io) {
                            // Launch PQ instead
                            launch(modManager.getMod("pq"), true, split);
                        }

					} catch (Exception e) {
						return;
					}
				}
			}
		}
		
		CookieManager manager = new CookieManager();
		manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(manager);
		
		modManager = new ModManager();
		quitListener = new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
			}
			@Override
			public void windowIconified(WindowEvent e) {
			}
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
			@Override
			public void windowActivated(WindowEvent e) {
			}
		};
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				Config.init();
				SelfUpdater.check();
				try {
					//TODO - We need to make this dynamically load official mods from the Marble Blast server.
					// This should not be hardcoded! 
					if (modManager.getNeedDefaultMods()) {
						modManager.addMod("http://marbleblast.com/pq/config/config.json");
						// modManager.addMod("http://fubar.marbleblast.com/config/config.json");
					}
					//frame = new MBPLauncherFrame(pq);
					//frame.setVisible(true);
					//frame.addWindowListener(quitListener);
					// Get default mod name and display launcher
					String defaultMod = modManager.getDefaultModName();
					if (defaultMod != null && !defaultMod.isEmpty()) {
						launchMod(defaultMod);
					} else {
						launchMod("pq");
					}
				} catch (Exception e) {
					Main.log(e);
					e.printStackTrace();
				}
			}
		}).start();

		//log(Utils.launcherLocation());

		//Create the new frame
		//frame = new LauncherFrame(pq);
		//frame.setVisible(true);
		
	}

	/**
	 * Launches the actual game
	 */
	public static void launch(final GameMod mod, final boolean online, final String[] argv) {
		if (frame != null) {
			frame.removeWindowListener(quitListener);
			frame.setVisible(false);
			frame.dispose();
		}
	
		String launchPath = (String)mod.getConfig("launchpath");
		if (Utils.getOS().startsWith("Mac")) {
			String extenderPath = mod.getGameDirectory(false) + launchPath;
			String executablePath = mod.getGameDirectory(false) + "/Contents/MacOS/MarbleBlast Gold";
			int[] perms = Utils.getPermissions(extenderPath);
			//Make sure we can run the damn thing!
			if ((perms[0] & 0111) != 0111) {
				Utils.exec("chmod", "+x", extenderPath);
			}
			
			perms = Utils.getPermissions(executablePath);
			//Make sure we can run the damn thing!
			if ((perms[0] & 0111) != 0111) {
				Utils.exec("chmod", "+x", executablePath);
			}
			
			String retVal;
			
			//Attempt to launch the game
			if (online)
				retVal = Utils.exec(mod.getGameDirectory(false) + launchPath, true, "-nohomedir");
			else
				retVal = Utils.exec(mod.getGameDirectory(false) + launchPath, true, "-nohomedir", "-offline");
			
			if (!retVal.equals("0")) {
				Updater updater = new Updater(mod);
				updater.sendConsole();
			}
		} else { 
			//winblows
	
			// fuck exec, i'll make my own one.
			Runtime runtime = Runtime.getRuntime();
			try {
				//Attempt to launch the game
				String path = mod.getGameDirectory(false) + launchPath;
				if (!online)
					path = path + " -offline";
				File pathtoExe = new File( mod.getGameDirectory(false) );

				String[] finalargs = new String[] { path };
				if (argv != null) {
					finalargs = (String[]) Stream.concat(Arrays.stream(finalargs), Arrays.stream(argv)).toArray(size -> (String[]) new String[size]);
				}
				
				//Fix by raptor -- using a String[] puts the path through Runtime's escaper
				Process obj = runtime.exec(finalargs,null,pathtoExe);
				if (obj.waitFor() != 0) {
					// The crash reporter is asynchronous, so invoke it with "-wait" to wait for any reporter to exit
					String crashReporterPath = mod.getGameDirectory(false) + "/plugins/PQCrashed.exe";
					if (new File(crashReporterPath).exists()) {
						Process reporter = runtime.exec(new String[]{crashReporterPath, "-wait"});
						reporter.waitFor();
					}
					// send the console.
					Updater updater = new Updater(mod);
					updater.sendConsole();
				}
				
				// Close launcher.
				//System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				Main.log(e.getMessage());
			}
		}
	}
	
	public static void restart() {
		Main.log("Restarting!");
		
		try {
			if (Utils.getOS().equals("Mac")) {
				new ProcessBuilder("sh", "-c", "sleep 1 ; open \"" + Utils.launcherLocation().replaceAll("\"", "\\\"") + "\"").start();
			} else {
				new ProcessBuilder(Utils.jarLocation()).start();
			}
		} catch (IOException e) {
			Main.log(e);
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	public static ModManager getModManager() {
		return modManager;
	}

	/**
	 * Adds a log to the main view
	 * @param log
	 */
	public static void log(String log) {
		//Send this along to the frame
		if (frame != null)
			frame.log(log + "\n");
	}
	
	/**
	 * Adds an exception log to the main view
	 * @param e
	 */
	public static void log(Exception e) {
		log("Exception Caught:");
		log(e.getMessage());
		
		for (StackTraceElement elem : e.getStackTrace()) {
			log(elem.toString());
		}
		log("");
	}
	
	/**
	 * Start a region of the progress, with a max section count
	 * @param sections Number of sub-sections in this region
	 */
	public static void startProgressRegion(int sections) {
		if (frame != null)
			frame.startProgressRegion(sections);
	}
	/**
	 * Set the weight of a section in the active progress region
	 * @param section Section index
	 * @param weight Section weight
	 */
	public static void setSectionWeight(int section, float weight) {
		if (frame != null)
			frame.setSectionWeight(section, weight);
	}
	/**
	 * End a region of the progress, popping the region stack
	 */
	public static void endProgressRegion() {
		if (frame != null)
			frame.endProgressRegion();
	}
	/**
	 * Update the progress of the current region of the region stack
	 * @param value New progress for the current region
	 */
	public static void updateProgress(int value) {
		if (frame != null)
			frame.updateProgress(value);
	}
	
	/**
	 * Update the download progress
	 * @param value New download progress
	 */
	public static void updateDownloadProgress(int value) {
		if (frame != null)
			frame.updateDownloadProgress(value);
	}

	/**
	 * Sets the offline state of the launcher
	 * @param offline
	 */
	public static void setOffline(boolean offline) {
		if (frame != null)
			frame.setOffline(offline);
	}

	/**
	 * Launch a mod with the given name
	 * @param modName The name of the mod to launch
	 */
	public static void launchMod(String modName) {
//		splash.setVisible(false);
		log("Load mod: " + Utils.launcherLocation());
		// Make sure no other existing frames are around
		if (frame != null) {
			frame.removeWindowListener(quitListener);
			frame.dispose();
		}
		//Create the new frame
		frame = new MBPLauncherFrame(modManager.getMod(modName));
		frame.setVisible(true);
		frame.addWindowListener(quitListener);
	}
	
	/**
	 * Display the Configure Mods frame.
	 */
	public static void configureMods() {
		if (cfgFrame != null) {
			cfgFrame.dispose();
		}
		cfgFrame = new ConfigModFrame();
		cfgFrame.setVisible(true);
	}
	
	/**
	 * Show the launcher console window
	 */
	public static void showConsole() {
	}
	
}
