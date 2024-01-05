package com.marbleblast.mblauncher;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public class ButtonsPanel extends JPanel {

	public ButtonsPanel() {
		super();
	}

	public ButtonsPanel(LayoutManager arg0) {
		super(arg0);
	}

	public ButtonsPanel(boolean arg0) {
		super(arg0);
	}

	public ButtonsPanel(LayoutManager arg0, boolean arg1) {
		super(arg0, arg1);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(new Color(1.0f, 1.0f, 1.0f, 0.8f));
		g.fillRect(0, 0, getWidth(), getHeight());
		super.paintComponent(g);
	}
}
