package com.marbleblast.mblauncher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.marbleblast.mblauncher.MBPLauncherFrame.ComboItem;
import com.jgoodies.forms.layout.FormSpecs;
import uriSchemeHandler.CouldNotRegisterUriSchemeHandler;
import uriSchemeHandler.URISchemeHandler;

import static uriSchemeHandler.WindowsURISchemeHandler.getCommandForUrl;

/**
 * The main frame class
 * 
 * @author HiGuy Smith
 * @email higuymb@gmail.com
 * @date 2014-08-07
 *
 */

public class MBPLauncherFrame extends LauncherFrame implements ActionListener, KeyListener, ItemListener {

	private static final long serialVersionUID = 1L;

	// Hardcoded because lazy
	static int width = 640;
	static int height = 480;

	// Controls
	JButton update;
	JProgressBar progressBar;
	JProgressBar dlProgressBar;
	private JTabbedPane mainPanel;
	private JPanel consolePanel;
	private JPanel changelogPanel;
	private JPanel newsPanel;
	private JScrollPane consoleScroll;
	private JTextArea consoleText;
	private JScrollPane newsScroll;
	private JScrollPane changelogScroll;
	private JEditorPane changelogView;
	private JEditorPane newsView;
	private JPanel panel_1;

	private Progress progress;
	private JPanel advancedPanel;
	private JScrollPane scrollPane;
	private JPanel panel_3;
	private JLabel lblGameLocation;
	private JButton btnChange;
	private JLabel lblImportTimes;
	private JButton btnImport;
	private JLabel lblRunFullUpdate;
	private JButton btnUpdate;
	private JButton btnOpenGameDir;
	private JLabel lblOpenGameFiles;
	private JLabel lblAskToSend;
	private JCheckBox chckbxAskToSend;
	private JLabel lblRegisterProtocol;
	private JButton btnRegisterProtocol;
//	private JLabel lblAddMod;
//	private JTextField txtAddMod;
//	private JButton btnAddMod;
	private JPanel topPanel;
	private JComboBox<ComboItem> gamePicker;
	private Component topPanelNorthSpacer;
	private Component topPanelSouthSpacer;
	private Component topPanelWestSpacer;
	private Component topPanelEastSpacer;
	private Component mainContentSpacerWest;
	private Component mainContentSpacerEast;
	private Component playButtonWestPadding;
	private Component playButtonEastPadding;
	private Component playButtonSouthPadding;
	private Component progressBarWestPadding;
	private Component progressBarEastPadding;
//	private Component verticalStrut;
//	private Component verticalStrut_1;
//	private Component verticalStrut_2;
//	private Component verticalStrut_3;
//	private Component verticalStrut_4;
//	private Component verticalStrut_5;

	public MBPLauncherFrame(final GameMod mod) {
		super(mod);

//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Get the screen size
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		// Default screen settings
		// this.setTitle((String)mod.getConfig("title"));
		this.setTitle("Marble Blast Launcher");
		this.setBounds((int) (screenSize.getWidth() - width) / 2, (int) (screenSize.getHeight() - height) / 2, width,
				height);

		// Set Minimum Screen Size
		this.setMinimumSize(new Dimension(width, height));

		// Remove default border and add custom border
		// setUndecorated(true);

		// Layout subviews
		getContentPane().setLayout(new BorderLayout(0, 0));

		// Buttons in their own panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		bottomPanel.setLayout(new BorderLayout(0, 0));

		JPanel progressBars = new JPanel();
		progressBars.setBorder(new EmptyBorder(5, 5, 5, 5));
		progressBars.setLayout(new BorderLayout(0, 3));

		dlProgressBar = new JProgressBar();
		dlProgressBar.setPreferredSize(new Dimension(146, 10));
		dlProgressBar.setForeground(new Color(0, 0, 255, 50));
		dlProgressBar.setValue(0);
		dlProgressBar.setMaximum(100);
		dlProgressBar.setVisible(false);
		dlProgressBar.setBorder(new EmptyBorder(0, 0, 0, 0));
		progressBars.add(dlProgressBar, BorderLayout.NORTH);

		progress = new Progress();

		topPanel = new JPanel();
		topPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		getContentPane().add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BorderLayout(0, 0));

		topPanelNorthSpacer = Box.createVerticalStrut(20);
		topPanelNorthSpacer.setPreferredSize(new Dimension(0, 10));
		topPanelNorthSpacer.setMinimumSize(new Dimension(0, 10));
		topPanel.add(topPanelNorthSpacer, BorderLayout.NORTH);

		gamePicker = new JComboBox<ComboItem>();

		// Collect Mods for Drop-down Display.
		ModManager modManager = Main.getModManager();
		Object[] ModList = modManager.getModNames().toArray();
		for (Object o : ModList) {
			String modID = o.toString();
			GameMod thisMod = modManager.getMod(modID);
			String modTitle = thisMod.getConfig("title").toString();
			gamePicker.addItem(new ComboItem(modTitle, modID));
		}
		final ComboItem thisItem = new ComboItem(this.mod.getConfig("title").toString(),
				this.mod.getConfig("name").toString());
		gamePicker.getModel().setSelectedItem(thisItem);

		// Configure Mods selection
		gamePicker.addItem(new ComboItem("-------------------------", "-------------------------"));
		gamePicker.addItem(new ComboItem("Configure Mods...", "config"));

		gamePicker.setFocusable(false);
//		gamePicker.setFont(new Font("Arial", Font.PLAIN, 18));
		topPanel.add(gamePicker, BorderLayout.CENTER);

		// Switch mods when dropdown changes
		gamePicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object item = gamePicker.getSelectedItem();
				String value = ((ComboItem) item).getValue();
				if (value.equals("config")) {
					gamePicker.getModel().setSelectedItem(thisItem);
					Main.configureMods();
				} else if (value.equals("-------------------------")) {
					gamePicker.getModel().setSelectedItem(thisItem);
				} else {
					Main.launchMod(((ComboItem) item).getValue());
				}
			}
		});

		topPanelSouthSpacer = Box.createVerticalStrut(20);
		topPanelSouthSpacer.setPreferredSize(new Dimension(0, 10));
		topPanelSouthSpacer.setMinimumSize(new Dimension(0, 10));
		topPanel.add(topPanelSouthSpacer, BorderLayout.SOUTH);

		topPanelWestSpacer = Box.createHorizontalStrut(20);
		topPanelWestSpacer.setPreferredSize(new Dimension(10, 0));
		topPanelWestSpacer.setMinimumSize(new Dimension(10, 0));
		topPanel.add(topPanelWestSpacer, BorderLayout.WEST);

		topPanelEastSpacer = Box.createHorizontalStrut(20);
		topPanelEastSpacer.setPreferredSize(new Dimension(10, 0));
		topPanelEastSpacer.setMinimumSize(new Dimension(10, 0));
		topPanel.add(topPanelEastSpacer, BorderLayout.EAST);

		mainPanel = new JTabbedPane(JTabbedPane.TOP);
		mainPanel.setFocusable(false);
		getContentPane().add(mainPanel, BorderLayout.CENTER);

		newsPanel = new JPanel();
		newsPanel.setFocusable(false);
		mainPanel.addTab("News", null, newsPanel, null);
		mainPanel.setEnabledAt(0, true);
		newsPanel.setLayout(new BorderLayout(0, 0));

		newsScroll = new JScrollPane();
		newsScroll.setFocusable(false);
		newsScroll.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		newsScroll.setBorder(null);
		newsPanel.add(newsScroll);

		newsView = new JEditorPane();
		newsView.setMargin(new Insets(3, 3, 3, 3));
		newsScroll.setViewportView(newsView);
		newsView.setEditable(false);
		newsView.setContentType("text/html");
		newsView.setText("<html><center>Connecting...</center></html>");
		newsView.setBorder(new EmptyBorder(0, 0, 0, 0));

		changelogPanel = new JPanel();
		changelogPanel.setFocusable(false);
		mainPanel.addTab("Game Updates", null, changelogPanel, null);
		changelogPanel.setLayout(new BorderLayout(0, 0));

		changelogScroll = new JScrollPane();
		changelogScroll.setFocusable(false);
		changelogScroll.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		changelogScroll.setBorder(null);
		changelogPanel.add(changelogScroll, BorderLayout.CENTER);

		changelogView = new JEditorPane();
		changelogScroll.setViewportView(changelogView);
		changelogView.setEditable(false);
		changelogView.setContentType("text/html");
		changelogView.setText("<html>Connecting...");
		changelogView.setBorder(new EmptyBorder(0, 0, 0, 0));

		consolePanel = new JPanel();
		consolePanel.setFocusable(false);
		mainPanel.addTab("Console", null, consolePanel, null);
		consolePanel.setLayout(new BorderLayout(0, 0));

		consoleScroll = new JScrollPane();
		consoleScroll.setFocusable(false);
		consoleScroll.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		consoleScroll.setBorder(null);
		consolePanel.add(consoleScroll);

		consoleText = new JTextArea();
		consoleText.setFont(new Font("Monospaced", Font.PLAIN, 12));
		consoleText.setForeground(Color.BLACK);
		consoleText.setBorder(null);
		consoleText.setBackground(Color.WHITE);
		consoleScroll.setViewportView(consoleText);
		consoleText.setEditable(false);

		advancedPanel = new JPanel();
		advancedPanel.setFocusable(false);
		advancedPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		mainPanel.addTab("Settings", null, advancedPanel, null);
		advancedPanel.setLayout(new BorderLayout(0, 0));

		scrollPane = new JScrollPane();
		scrollPane.setFocusable(false);
		scrollPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		scrollPane.setBorder(null);
		advancedPanel.add(scrollPane);

		panel_3 = new JPanel();
		panel_3.setBackground(Color.WHITE);
		scrollPane.setViewportView(panel_3);
		panel_3.setLayout(new FormLayout(
				new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
						FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC,},
				new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

		lblGameLocation = new JLabel("Game location: " + mod.getGameDirectory(false));
		panel_3.add(lblGameLocation, "2, 2, fill, default");

		btnChange = new JButton("Change");
		btnChange.setBackground(Color.WHITE);
		btnChange.addActionListener(this);
		panel_3.add(btnChange, "4, 2");

		btnOpenGameDir = new JButton("Open");
		btnOpenGameDir.setBackground(Color.WHITE);
		btnOpenGameDir.addActionListener(this);

		lblOpenGameFiles = new JLabel("Open game files:");
		panel_3.add(lblOpenGameFiles, "2, 4");
		panel_3.add(btnOpenGameDir, "4, 4");

		if (this.mod.getConfig("canimportprefs") != null) {
			lblImportTimes = new JLabel("Import times from past install:");
			panel_3.add(lblImportTimes, "2, 6");

			btnImport = new JButton("Import");
			btnImport.setBackground(Color.WHITE);
			btnImport.addActionListener(this);
			// Disable Imports for MBFubar
			btnImport.setEnabled(false);

			panel_3.add(btnImport, "4, 6");
		}

		lblRunFullUpdate = new JLabel("Run full update:");
		panel_3.add(lblRunFullUpdate, "2, 8");

		btnUpdate = new JButton("Update");
		btnUpdate.setBackground(Color.WHITE);
		panel_3.add(btnUpdate, "4, 8");
		btnUpdate.addActionListener(this);

		lblAskToSend = new JLabel("Ask to send crash logs:");
		panel_3.add(lblAskToSend, "2, 10");

		chckbxAskToSend = new JCheckBox("Ask to Send");
		chckbxAskToSend.setBackground(Color.WHITE);
		panel_3.add(chckbxAskToSend, "4, 10");
		chckbxAskToSend.addItemListener(this);
		chckbxAskToSend.setSelected(
				Preferences.userRoot().getInt("sendConsole", JOptionPane.NO_OPTION) == JOptionPane.NO_OPTION);

		if (Utils.getOS().equals("Windows")) {
			lblRegisterProtocol = new JLabel("Register Launch Protocol");
			panel_3.add(lblRegisterProtocol, "2, 12");

			btnRegisterProtocol = new JButton("Register");
			btnRegisterProtocol.setBackground(Color.WHITE);
			panel_3.add(btnRegisterProtocol, "4, 12");
			btnRegisterProtocol.addActionListener(this);
		}

//		verticalStrut = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut, "2, 11");
//		
//		verticalStrut_1 = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut_1, "2, 12");
//		
//		verticalStrut_2 = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut_2, "2, 13");
//		
//		verticalStrut_3 = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut_3, "2, 14");
//		
//		verticalStrut_4 = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut_4, "2, 15");
//		
//		verticalStrut_5 = Box.createVerticalStrut(20);
//		panel_3.add(verticalStrut_5, "2, 16");
//		
//		lblAddMod = new JLabel("Custom Mod:");
//		lblAddMod.setPreferredSize(new Dimension(100, 20));
//		panel_3.add(lblAddMod, "2, 17, left, default");
//		
//		txtAddMod = new JTextField("http://www.example.com/config.json");
//		txtAddMod.setPreferredSize(new Dimension(400, 20));
//		panel_3.add(txtAddMod, "2, 17, right, default");
//		
//		btnAddMod = new JButton("Add Mod");
//		btnAddMod.setBackground(Color.WHITE);
//		panel_3.add(btnAddMod, "4, 17");
//		btnAddMod.addActionListener(this);

		bottomPanel.add(progressBars, BorderLayout.CENTER);
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(146, 20));
		progressBars.add(progressBar, BorderLayout.SOUTH);
		progressBar.setForeground(new Color(0, 0, 255, 50));
		progressBar.setValue(0);
		progressBar.setMaximum(1000);
		progressBar.setBorder(new EmptyBorder(0, 0, 0, 0));

		getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		if (Config.online) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						changelogView.setPage((String) mod.getConfig("changelog"));
						newsView.setPage((String) mod.getConfig("news"));
					} catch (IOException e) {
						Main.log(e);
						changelogView.setContentType("text/html");
						changelogView.setText(
								"<html>ERROR: Unable to establish server at <a href='http://www.marbleblast.com'>MarbleBlast.com</a>.</html>");
						newsView.setContentType("text/html");
						newsView.setText(
								"<html>ERROR: Unable to establish server at <a href='http://www.marbleblast.com'>MarbleBlast.com</a>.</html>");
					}
				}
			}).start();
		} else {
			changelogView.setText("<html><h1><center>Offline</center></h1></html>");
			newsView.setText("<html><h1><center>Offline</center></h1></html>");
		}

		panel_1 = new JPanel();
		bottomPanel.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));

		// Update button
		update = new JButton();
		update.setText("");
		update.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		panel_1.add(update, BorderLayout.CENTER);
		update.addActionListener(this);
		update.setDefaultCapable(true);
		update.setActionCommand("Enter");
		update.setFocusable(false);

		getRootPane().setDefaultButton(update);

		playButtonWestPadding = Box.createRigidArea(new Dimension(20, 20));
		playButtonWestPadding.setPreferredSize(new Dimension(200, 20));
		panel_1.add(playButtonWestPadding, BorderLayout.WEST);

		playButtonEastPadding = Box.createRigidArea(new Dimension(20, 20));
		playButtonEastPadding.setPreferredSize(new Dimension(200, 20));
		panel_1.add(playButtonEastPadding, BorderLayout.EAST);

		playButtonSouthPadding = Box.createVerticalStrut(20);
		playButtonSouthPadding.setPreferredSize(new Dimension(0, 10));
		panel_1.add(playButtonSouthPadding, BorderLayout.SOUTH);

		progressBarWestPadding = Box.createRigidArea(new Dimension(20, 20));
		progressBarWestPadding.setPreferredSize(new Dimension(10, 10));
		bottomPanel.add(progressBarWestPadding, BorderLayout.WEST);

		progressBarEastPadding = Box.createRigidArea(new Dimension(20, 20));
		progressBarEastPadding.setPreferredSize(new Dimension(10, 10));
		bottomPanel.add(progressBarEastPadding, BorderLayout.EAST);

		mainContentSpacerWest = Box.createHorizontalStrut(10);
		getContentPane().add(mainContentSpacerWest, BorderLayout.WEST);

		mainContentSpacerEast = Box.createHorizontalStrut(10);
		getContentPane().add(mainContentSpacerEast, BorderLayout.EAST);

		setOffline(!Config.online);

		if (Utils.getOS().equals("Windows")) {
			tryRegisterProtocolHandler();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			actionPerformed(new ActionEvent(update, 0, ""));
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.update) {
			// Update check
			if (Config.online) {
				update.setEnabled(false);
				updater.checkFiles(false);
			} else {
				Main.launch(mod, false, null);
			}
		} else if (e.getSource() == this.btnUpdate) {
			update.setEnabled(false);
			updater.checkFiles(true);
		} else if (e.getSource() == this.btnImport && this.btnImport != null) {
			// Ask them for the file
			Main.log("Asking to import prefs...");
			JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				Main.log("Importing prefs...");
				updater.getMigrator().copyPrefs(file, false);
			} else {
				Main.log("Import prefs operation cancelled.");
			}
		} else if (e.getSource() == this.btnOpenGameDir) {
			// Open the directory
			String directory = mod.getGameDirectory(true).concat((String) mod.getConfig("opensub"));
			Utils.openDirectory(directory);
		} else if (e.getSource() == this.btnChange) {
			String oldGameDirectory = mod.getGameDirectory(false);
			// Find a new place to put the mod
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(false);
			chooser.setDialogTitle("Choose New Game Location");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setCurrentDirectory(new File(oldGameDirectory));

			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File newInstallDirectory = chooser.getSelectedFile();

				// Set and get the new directory
				mod.setInstallDirectory(newInstallDirectory.getAbsolutePath());
				String newGameDirectory = mod.getGameDirectory(false);

				try {
					Files.move(Paths.get(new File(oldGameDirectory).getPath()),
							Paths.get(new File(newGameDirectory).getPath()));
				} catch (IOException e1) {
					e1.printStackTrace();
					Main.log(e1);
					JOptionPane.showMessageDialog(Main.frame, "Could not move files, please move them manually.");
					// Force an update
					Preferences.userRoot().put("listingMD5", "");
				}

				Main.log("Moved to " + newGameDirectory);
			}

			lblGameLocation.setText("Game location: " + mod.getGameDirectory(false));
		} else if (e.getSource() == this.btnRegisterProtocol) {
			registerProtocolHandler();
		}
	}

	public void tryRegisterProtocolHandler() {
		String jarLocation = Utils.jarLocation();
		if (jarLocation.startsWith("/")) // remove leading slash cause lol
			jarLocation = jarLocation.substring(1);
		try {
			String cmd = getCommandForUrl(new URI("platinumquest://1234"));
			String jarPathCmd = null;
			jarPathCmd = cmd.substring(cmd.indexOf('"') + 1);
			jarPathCmd = jarPathCmd.substring(0, jarPathCmd.indexOf('"'));
			if (!jarPathCmd.equals(jarLocation)) {
				registerProtocolHandler(); // Jar got moved, re-register
			}

		} catch (Exception e) {
			registerProtocolHandler();
		}
    }

	public void registerProtocolHandler() {
		String jarLocation = Utils.jarLocation();
		if (jarLocation.startsWith("/")) // remove leading slash cause lol
			jarLocation = jarLocation.substring(1);
		URISchemeHandler urlHandler = new URISchemeHandler();
		try {
			urlHandler.register("platinumquest", jarLocation);
			JOptionPane.showMessageDialog(Main.frame, "Successfully registered protocol handler!");
		} catch (CouldNotRegisterUriSchemeHandler ex) {
			JOptionPane.showMessageDialog(Main.frame, "Failed to register protocol handler!");
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == this.chckbxAskToSend) {
			Preferences prefs = Preferences.userRoot();
			int newValue = e.getStateChange() == ItemEvent.DESELECTED ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION;
			prefs.putInt("sendConsole", newValue);
			try {
				prefs.sync();
			} catch (BackingStoreException ex) {
				Main.log(ex);
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Adds a log to the text view
	 * 
	 * @param log
	 */
	public void log(String log) {
		// Just append
		consoleText.append(log);
		// Scroll to bottom
		consoleText.setCaretPosition(consoleText.getText().length());
	}

	/**
	 * Start a region of the progress, with a max section count
	 * 
	 * @param sections Number of sub-sections in this region
	 */
	public void startProgressRegion(int sections) {
		progress.startProgressRegion(sections);
	}

	/**
	 * Set the weight of a section in the active progress region
	 * 
	 * @param section Section index
	 * @param weight  Section weight
	 */
	public void setSectionWeight(int section, float weight) {
		progress.setSectionWeight(section, weight);
	}

	/**
	 * End a region of the progress, popping the region stack
	 */
	public void endProgressRegion() {
		progress.endProgressRegion();
		dlProgressBar.setVisible(false);
	}

	/**
	 * Update the progress of the current region of the region stack
	 * 
	 * @param value New progress for the current region
	 */
	public void updateProgress(int value) {
		progress.updateProgress(value);
		progressBar.setValue((int) (progress.getProgress() * progressBar.getMaximum()));
	}

	/**
	 * Update the download progress.
	 * 
	 * @param value New progress for the current region
	 */
	public void updateDownloadProgress(int value) {
		dlProgressBar.setVisible(true);
		dlProgressBar.setValue(value);
	}

	/**
	 * Sets the offline state of the launcher
	 * 
	 * @param offline
	 */
	public void setOffline(boolean offline) {
		if (offline)
			update.setText("Play Offline");
		else
			update.setText("Play");
	}

	/**
	 * Destroy the current frame
	 */
	public void closeFrame() {
		super.dispose();
	}

	/**
	 * ComboItem Custom items for JComboBox that supports key-value pairs
	 * 
	 * @param key   String text to display on the ComboBox option
	 * @param value String ID to use as the ComboBox value
	 */
	class ComboItem {
		private String key;
		private String value;

		public ComboItem(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return key;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}
