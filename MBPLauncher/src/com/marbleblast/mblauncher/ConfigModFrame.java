package com.marbleblast.mblauncher;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.marbleblast.mblauncher.MBPLauncherFrame.ComboItem;
import com.jgoodies.forms.layout.FormSpecs;
import javax.swing.GroupLayout.Alignment;

public class ConfigModFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private ModManager modManager = Main.getModManager();
	
	private JPanel installedModsPanel;
	private JPanel addModPanel;
	private JPanel addModButtonsPanel;
	private JTable tblModInfo;
	private JTextField txtAddMod;
	private JButton btnAddMod;
	private JButton btnSaveConfig;

	//Hardcoded because lazy
	static int width = 640;
	static int height = 480;
	private JLabel lblNewLabel;
	private JPanel modSettingsPanel;
	private Component rigidArea;
	private JPanel panelHeader;
	private Component rigidArea_1;
	private Component rigidArea_2;
	private Component rigidArea_3;
	private Component rigidArea_4;
	private Component rigidArea_5;
	private Component rigidArea_6;
	private Component rigidArea_7;
	private Component rigidArea_8;
	private Component rigidArea_9;
	private JButton btnSetDefault;
	private JButton btnOpenDir;
	private JButton btnRemoveMod;
	private Component rigidArea_10;
	private JPanel modSettingsButtons;
	
	public ConfigModFrame() {
		
		//Get the screen size
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		//Default screen settings
		this.setTitle("Marble Blast Launcher - Manage Marble Blast Mods");
		this.setBounds((int)(screenSize.getWidth() - width) / 2, (int)(screenSize.getHeight() - height) / 2, width, height);
		
		// Set Minimum Screen Size
		this.setMinimumSize(new Dimension(width, height));
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		panelHeader = new JPanel();
		getContentPane().add(panelHeader, BorderLayout.NORTH);
		panelHeader.setLayout(new BorderLayout(0, 0));
		
		rigidArea_1 = Box.createRigidArea(new Dimension(10, 10));
		panelHeader.add(rigidArea_1, BorderLayout.NORTH);
		
		rigidArea_2 = Box.createRigidArea(new Dimension(10, 10));
		panelHeader.add(rigidArea_2, BorderLayout.EAST);
		
		rigidArea_3 = Box.createRigidArea(new Dimension(10, 10));
		panelHeader.add(rigidArea_3, BorderLayout.SOUTH);
		
		rigidArea_4 = Box.createRigidArea(new Dimension(10, 10));
		panelHeader.add(rigidArea_4, BorderLayout.WEST);
		
		lblNewLabel = new JLabel("Installed Mods");
		panelHeader.add(lblNewLabel);
		
		installedModsPanel = new JPanel();
		getContentPane().add(installedModsPanel);
		installedModsPanel.setLayout(new BorderLayout(0, 0));
		
		String[] columnNames = {"Default","Mod ID","Mod Name","Mod Directory"};
		DefaultTableModel model = new DefaultTableModel(columnNames,0);
		tblModInfo = new JTable(model) {
			// Override default Column Classes so the first column can have an ImageIcon
			// The ImageIcon is to show the default mod selection.
			public Class getColumnClass(int column) {
				return (column == 0) ? Icon.class : Object.class;
			}
		};
		tblModInfo.setShowGrid(false);
		tblModInfo.setShowHorizontalLines(false);
		tblModInfo.setShowVerticalLines(false);
		tblModInfo.getColumnModel().getColumn(0).setPreferredWidth(0);
		tblModInfo.getColumnModel().getColumn(1).setPreferredWidth(40);
		tblModInfo.getColumnModel().getColumn(2).setPreferredWidth(150);
		tblModInfo.getColumnModel().getColumn(3).setPreferredWidth(400);
		tblModInfo.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		// Only allow users to pick one row at a time, otherwise bad stuff happens.
		tblModInfo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// Prevent users from dragging table headers around.
		tblModInfo.getTableHeader().setReorderingAllowed(false);
		// Disable Table Editing
		tblModInfo.setDefaultEditor(Object.class,  null);
		installedModsPanel.add(new JScrollPane(tblModInfo), BorderLayout.CENTER);
		installedModsPanel.add(tblModInfo.getTableHeader(), BorderLayout.NORTH);
		
		rigidArea_5 = Box.createRigidArea(new Dimension(10, 10));
		installedModsPanel.add(rigidArea_5, BorderLayout.WEST);
		
		rigidArea_6 = Box.createRigidArea(new Dimension(10, 10));
		installedModsPanel.add(rigidArea_6, BorderLayout.EAST);
		
		addModPanel = new JPanel();
		getContentPane().add(addModPanel, BorderLayout.SOUTH);;
		addModPanel.setLayout(new BorderLayout(0, 0));
		
		modSettingsPanel = new JPanel();
		addModPanel.add(modSettingsPanel, BorderLayout.NORTH);
		modSettingsPanel.setLayout(new BorderLayout(0, 0));
		
		rigidArea = Box.createRigidArea(new Dimension(10, 10));
		modSettingsPanel.add(rigidArea, BorderLayout.NORTH);
		
		modSettingsButtons = new JPanel();
		modSettingsPanel.add(modSettingsButtons, BorderLayout.CENTER);
		modSettingsButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		btnSetDefault = new JButton("Set Default");
		btnSetDefault.setEnabled(false);
		btnSetDefault.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnSetDefault.addActionListener(this);
		modSettingsButtons.add(btnSetDefault);
		
		btnOpenDir = new JButton("Open Directory");
		btnOpenDir.setEnabled(false);
		btnOpenDir.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnOpenDir.addActionListener(this);
		modSettingsButtons.add(btnOpenDir);
		
		btnRemoveMod = new JButton("Remove Mod");
		btnRemoveMod.setEnabled(false);
		btnRemoveMod.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnRemoveMod.addActionListener(this);
		modSettingsButtons.add(btnRemoveMod);
		
		rigidArea = Box.createRigidArea(new Dimension(10, 10));
		modSettingsPanel.add(rigidArea, BorderLayout.NORTH);
		rigidArea_10 = Box.createRigidArea(new Dimension(10, 10));
		modSettingsPanel.add(rigidArea_10, BorderLayout.SOUTH);
		
		addModButtonsPanel = new JPanel();
		addModPanel.add(addModButtonsPanel, BorderLayout.CENTER);
		addModButtonsPanel.setLayout(new BoxLayout(addModButtonsPanel, BoxLayout.X_AXIS));
		
		txtAddMod = new JTextField("http://www.example.com/config.json");
		addModButtonsPanel.add(txtAddMod);
		txtAddMod.setHorizontalAlignment(SwingConstants.LEFT);
		txtAddMod.setColumns(10);
		
		btnAddMod = new JButton("Add Mod");
		btnAddMod.addActionListener(this);
		addModButtonsPanel.add(btnAddMod);
		
		btnSaveConfig = new JButton("Done");
		btnSaveConfig.addActionListener(this);
		addModButtonsPanel.add(btnSaveConfig);
		
		rigidArea_7 = Box.createRigidArea(new Dimension(10, 10));
		addModPanel.add(rigidArea_7, BorderLayout.EAST);
		
		rigidArea_8 = Box.createRigidArea(new Dimension(10, 10));
		addModPanel.add(rigidArea_8, BorderLayout.WEST);
		
		rigidArea_9 = Box.createRigidArea(new Dimension(10, 10));
		addModPanel.add(rigidArea_9, BorderLayout.SOUTH);
		
		// Collect data and display in list
		String defaultMod = modManager.getDefaultModName();
		Object[] ModList = modManager.getModNames().toArray();
		for (Object o : ModList) {
			ImageIcon defaultIcon = null;
			String modID = o.toString();
			GameMod thisMod = modManager.getMod(modID);
			String modTitle = thisMod.getConfig("title").toString();
			String modPath = thisMod.getGameDirectory(false);
			if (modID.equals(defaultMod)) {
				defaultIcon = new ImageIcon(getClass().getResource("/launcher/mbicon.png"));
				Image img = defaultIcon.getImage();
				Image newimg = img.getScaledInstance(14,14,java.awt.Image.SCALE_SMOOTH);
				defaultIcon = new ImageIcon(newimg);
			} else {
				defaultIcon = null;
			}
			Object[] row = { defaultIcon, modID, modTitle, modPath };
			model.addRow(row);
		}
		
		// Table Row Listener
		tblModInfo.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent ev) {
				btnRemoveMod.setEnabled(true);
				btnOpenDir.setEnabled(true);
				// Try and change the default button.
				// This can throw an error if the row is deleted by another model.
				// I'd fix it but I am lazy, so here's a try-catch default disable.
				try {
					String defaultMod = modManager.getDefaultModName();
					String selectedMod = tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString();
					if (selectedMod.equals("pq")) {
						btnRemoveMod.setEnabled(false);
					}
					if (!selectedMod.equals(defaultMod) && selectedMod != "-1") {
						btnSetDefault.setEnabled(true);
					} else {
						btnSetDefault.setEnabled(false);
					}
				} catch (Exception ex) {
					btnSetDefault.setEnabled(false);
				}
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// -------------------------------------
		// SAVE CONFIGURATION
		// -------------------------------------
		if (e.getSource() == this.btnSaveConfig) {
			// Save Changes and exit
			// (Changes auto-save, so just exit.)
			closeFrame();
			// Launch selected mod.
			int selectedRow = tblModInfo.getSelectedRow();
			// Make sure there is a selected row.
			if (selectedRow != -1) {
				Main.launchMod(tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString());
			} else {
				Main.launchMod("pq");
			}
		// -------------------------------------
		// ADD MOD
		// -------------------------------------
		} else if (e.getSource() == this.btnAddMod) {
			// Make sure the config URL looks good.
			String url = txtAddMod.getText();
			if (url.equals("") || url.equals("http://www.example.com/config.json")) {
				// Bad URL.
				JOptionPane.showMessageDialog(this, "Mod URL is invalid.", "Error", JOptionPane.ERROR_MESSAGE);
			} else if (!url.contains(".json")) {
				// Not JSON Config.
				JOptionPane.showMessageDialog(this, "Not a valid Mod URL - expecting a .json configuration.", "Error", JOptionPane.ERROR_MESSAGE);
			} else {
				// Try to add the mod.
				try {
					GameMod newMod = modManager.addMod(url);
					String modID = newMod.getConfig("name").toString();
					String modTitle = newMod.getConfig("title").toString();
					String modPath = newMod.getGameDirectory(false);
					Object[] row = { "0", modID, modTitle, modPath };
					DefaultTableModel modelTable = (DefaultTableModel) tblModInfo.getModel();
					modelTable.addRow(row);
					JOptionPane.showMessageDialog(this, modTitle + " has been added to your Mods collection.", "Success", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					Main.log(ex);
					ex.printStackTrace();
				}
			}
		// -------------------------------------
		// REMOVE MOD
		// -------------------------------------
		} else if (e.getSource() == this.btnRemoveMod) {
			// Remove the selected game mod and update table row accordingly.
			GameMod mod = modManager.getMod(tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString());
			String delModName = tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString();
			String delModTitle = tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 2).toString();
			// Don't let them remove PQ, that will cause problems.
			if (delModName.equals("pq")) {
				return;
			}
			try {
				boolean modRemoved = modManager.deleteMod(mod);
				if (modRemoved) {
					// Reset the default mod setting to pq if this was the default mod.
					String defaultMod = modManager.getDefaultModName();
					if (delModName.equals(defaultMod)) {
						modManager.setDefaultModName("pq");
					}
					// Remove the GUI element
					int selectedRow = tblModInfo.getSelectedRow();
					// Make sure there is a selected row.
					if (selectedRow != -1) {
						DefaultTableModel modelTable = (DefaultTableModel) tblModInfo.getModel();
						modelTable.removeRow(selectedRow);
					}
					JOptionPane.showMessageDialog(this, delModTitle + " was removed from your Mods collection.", "Success", JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (Exception ex) {
				Main.log(ex);;
				ex.printStackTrace();
			}
		// -------------------------------------
		// OPEN GAME DIRECTORY
		// -------------------------------------
		} else if (e.getSource() == this.btnOpenDir) {
			//Open the directory
			GameMod mod = modManager.getMod(tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString());
			String directory = mod.getGameDirectory(true).concat((String)mod.getConfig("opensub"));
			Utils.openDirectory(directory);
		// -------------------------------------
		// SET DEFAULT MOD
		// -------------------------------------
		} else if (e.getSource() == this.btnSetDefault) {
			String defaultMod = modManager.getDefaultModName();
			String newDefaultMod = tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 1).toString();
			if (newDefaultMod != null && !newDefaultMod.isEmpty() && !newDefaultMod.equals(defaultMod)) {
				ImageIcon defaultIcon = null;
				modManager.setDefaultModName(newDefaultMod);
				btnSetDefault.setEnabled(false);
				// Make sure we update the GUI
				for (int row = 0; row < tblModInfo.getRowCount(); row++) {
					String rowMod = tblModInfo.getValueAt(row, 1).toString();
					if (rowMod.equals(newDefaultMod)) {
						defaultIcon = new ImageIcon(getClass().getResource("/launcher/mbicon.png"));
						Image img = defaultIcon.getImage();
						Image newimg = img.getScaledInstance(14,14,java.awt.Image.SCALE_SMOOTH);
						defaultIcon = new ImageIcon(newimg);
						tblModInfo.setValueAt(defaultIcon, row, 0);
					} else {
						defaultIcon = null;
						tblModInfo.setValueAt(defaultIcon, row, 0);
					}
				}
				JOptionPane.showMessageDialog(this, tblModInfo.getValueAt(tblModInfo.getSelectedRow(), 2).toString() + " has been set as your default mod.", "Success", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
	
	/**
	 * Destroy the current frame
	 */
	public void closeFrame(){
		super.dispose();
	}

}
