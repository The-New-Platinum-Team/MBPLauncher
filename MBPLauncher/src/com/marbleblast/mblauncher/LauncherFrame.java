package com.marbleblast.mblauncher;

import javax.swing.*;

import java.security.InvalidParameterException;

public abstract class LauncherFrame extends JFrame {
	protected String name;
	protected GameMod mod;
	protected Updater updater;

	private static final long serialVersionUID = 1L;

	/**
	 * Construct a new LauncherFrame with a GameMod for information
	 * @param mod The LauncherFrame's mod
	 */
	public LauncherFrame(GameMod mod) {
		this.mod = mod;
		this.updater = new Updater(mod);
	}
	
	/**
	 * Generic constructor -- don't use this
	 * @throws InvalidParameterException
	 */
	public LauncherFrame() throws InvalidParameterException {
		throw new InvalidParameterException();
	}
	
	/**
	 * Adds a log to the text view
	 * @param log
	 */
	public abstract void log(String log);

	public abstract void startProgressRegion(int sections);
	public abstract void setSectionWeight(int section, float weight);
	public abstract void endProgressRegion();
	public abstract void updateProgress(int value);
	public abstract void updateDownloadProgress(int value);

	/**
	 * Sets the offline state of the launcher
	 * @param offline
	 */
	public abstract void setOffline(boolean offline);
	
	/**
	 * Set the frame's mod
	 * @param mod The new mod
	 */
	public void setMod(GameMod mod) {
		this.mod = mod;
	}
	
	/**
	 * Set the frame's name
	 * @param name The new name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the frame's mod
	 * @return The frame's mod
	 */
	public GameMod getMod() {
		return this.mod;
	}
	
	/**
	 * Get the frame's name
	 * @return The frame's name
	 */
	public String getName() {
		return this.name;
	}
}
