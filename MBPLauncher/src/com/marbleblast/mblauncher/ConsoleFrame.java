package com.marbleblast.mblauncher;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ConsoleFrame extends JFrame {
	static final int width = 640;
	static final int height = 480;
	
	private JScrollPane consoleScroll;
	private JTextArea consoleText;

	public ConsoleFrame() {
		super("Marble Blast Launcher - Console");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		setSize(width, height);
		setResizable(true);
		setLocationRelativeTo(null);
		
		consoleScroll = new JScrollPane();
		add(consoleScroll);
		
		consoleText = new JTextArea();
		consoleScroll.setViewportView(consoleText);
		consoleText.setEditable(false);
	}
	
	public void log(String log) {
		//Just append
		consoleText.append(log);
		//Scroll to bottom
		consoleText.setCaretPosition(consoleText.getText().length());
	}
}
