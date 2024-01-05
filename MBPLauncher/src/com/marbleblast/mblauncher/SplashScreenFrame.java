package com.marbleblast.mblauncher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main frame class
 * @author HiGuy Smith
 * @email higuymb@gmail.com
 * @date 2014-08-07
 *
 */

public class SplashScreenFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	static final int width = 900;
	static final int height = 600;
	
	private static final String version = "1.0";
	
	private static JLayeredPane layerPane;
	
	private static JPanel buttonsPanel;
	private static JPanel bottomPanel;

	private static JTextField addTextField;
	private static JButton addButton;
	private static JButton consoleButton;
	
	private static JLabel welcomeText;
	private static JButton utilities;
	
	private static Map<GameMod, JButton> modButtons;
	
	public SplashScreenFrame() {
		
		// Construct the Main Frame
		super("Marble Blast Launcher - Version "+version);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(width, height);
		setResizable(true);
		layerPane = new JLayeredPane();

		// Construct the Buttons Panel
		buttonsPanel = new JPanel(new GridLayout());
		buttonsPanel.setPreferredSize(new Dimension(width, height));
		buttonsPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		buttonsPanel.setVisible(true);
		
		ModManager manager = Main.getModManager();
		modButtons = new HashMap<GameMod, JButton>();
		Collection<GameMod> mods = manager.getMods();
		
		//Sort the mod list
		ArrayList<GameMod> modList = new ArrayList<GameMod>(mods);
		Collections.sort(modList);
		
		for (final GameMod mod : modList) {
			addModButton(mod);
		}
		
		addTextField = new JTextField("http://www.example.com/config.json");
		
		addButton = new JButton("Add Mod");
		addButton.setPreferredSize(new Dimension(100, 20));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String modPath = addTextField.getText();
				addMod(modPath);
			}
		});

		bottomPanel = new ButtonsPanel(new BorderLayout());
		
		SpringLayout springLayout = new SpringLayout();
		springLayout.putConstraint(SpringLayout.SOUTH, buttonsPanel, 0, SpringLayout.SOUTH, layerPane);
		springLayout.putConstraint(SpringLayout.NORTH, buttonsPanel, 0, SpringLayout.NORTH, layerPane);
		springLayout.putConstraint(SpringLayout.EAST, buttonsPanel, 0, SpringLayout.EAST, layerPane);
		springLayout.putConstraint(SpringLayout.WEST, buttonsPanel, 0, SpringLayout.WEST, layerPane);
		
		springLayout.putConstraint(SpringLayout.NORTH, bottomPanel, -30, SpringLayout.SOUTH, layerPane);
		springLayout.putConstraint(SpringLayout.WEST, bottomPanel, 0, SpringLayout.WEST, layerPane);
		springLayout.putConstraint(SpringLayout.SOUTH, bottomPanel, 0, SpringLayout.SOUTH, layerPane);
		springLayout.putConstraint(SpringLayout.EAST, bottomPanel, 0, SpringLayout.EAST, layerPane);

		layerPane.setLayout(springLayout);
		bottomPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		bottomPanel.setVisible(true);
		bottomPanel.setOpaque(false);
		
		consoleButton = new JButton("Console");
		consoleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Main.showConsole();
			}
		});
		bottomPanel.add(consoleButton, BorderLayout.WEST);
		bottomPanel.add(addTextField, BorderLayout.CENTER);
		bottomPanel.add(addButton, BorderLayout.EAST);
		
		layerPane.add(bottomPanel, new Integer(1), 1);
		
		// Construct the final view
		// Icon
		//ImageIcon img = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/marbleblast/mblauncher/res/fubar/icon.png")));
		//mainPanel.setIconImage(img.getImage());
		// Main Panel
		layerPane.add(buttonsPanel, new Integer(0), 0);
		setLocationRelativeTo(null);
		setVisible(true);
		
		getContentPane().add(layerPane);
	}
	
	/**
	 * Add a mod via the Mod Manager
	 * @param modPath
	 */
	public void addMod(String modPath) {
		ModManager manager = Main.getModManager();
		try {
			GameMod mod = manager.addMod(modPath);
			//Add the mod if we had success
			addModButton(mod);
		} catch (Exception e1) {
			Main.log(e1);
			JOptionPane.showMessageDialog(Main.frame, "Error Adding Mod: " + e1.getMessage());
		}
	}
	
	public void deleteMod(GameMod mod) {
		ModManager manager = Main.getModManager();
		try {
			if (manager.deleteMod(mod)) {
				deleteModButton(mod);
			}
		} catch (Exception e) {
			Main.log(e);
			JOptionPane.showMessageDialog(Main.frame, "Error Deleting Mod: " + e.getMessage());
		}
	}
	
	/**
	 * Adds the mod button to the list of mods
	 * @param mod The mod to add
	 */
	public void addModButton(final GameMod mod) {
		final ModButton button = new ModButton();
		button.setSize(150, 150);
		button.setVisible(true);
		button.setEnabled(false);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int action = button.getAction(e.getSource());
				switch (action) {
				case ModButton.LAUNCH_MOD:
					Main.launchMod(mod.getName());
					break;
				case ModButton.DELETE_MOD:
					deleteMod(mod);
					break;
				}
			}
		});
		
		button.setText((String)mod.getConfig("gamename"));
		
		final JFrame fthis = this;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String imagePath = (String) mod.getConfig("image");

					//See if we can load it from cache
					byte[] contents = Utils.getCachedFile(imagePath, mod.getName());
					if (contents != null) {
						Image image = Toolkit.getDefaultToolkit().createImage(contents);
						
						while (image.getWidth(null) == -1)
							Thread.sleep(100);

						button.setImage(image);
						button.setEnabled(isEnabled());
						getContentPane().invalidate();
					} else {
						//And then try and download it
						contents = Utils.downloadCachedFile(imagePath, false, mod.getName());
						if (contents == null) {
							return;
						}
						
						Image image = Toolkit.getDefaultToolkit().createImage(contents);
						
						while (image.getWidth(null) == -1)
							Thread.sleep(100);
						
						button.setImage(image);
						button.setEnabled(isEnabled());
						System.out.println("Load from net");
					}
				} catch (Exception e) {
					e.printStackTrace();
					Main.log(e);
				}
				
				Main.log("Loaded mod: " + mod.getName());
			}
		}).start();
		
		buttonsPanel.add(button);		
		modButtons.put(mod, button);
	}
	
	public void deleteModButton(GameMod mod) {
		//Delete the button
		JButton button = modButtons.get(mod);
		buttonsPanel.remove(button);
		button.invalidate();
		
		//Refresh the panel
		buttonsPanel.revalidate();
		buttonsPanel.repaint();
		
		//Remove it entirely
		modButtons.remove(mod);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		for (JButton button : modButtons.values()) {
			button.setEnabled(enabled);
		}
	}
}