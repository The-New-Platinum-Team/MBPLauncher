package com.marbleblast.mblauncher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.Kernel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * ModButton is the class used in the SplashScreenFrame for each of the mod selector buttons.
 * The button draws a subset of the mod's image, giving a more immersive feel to the mod selector.
 * @author HiGuy Smith
 * @email higuymb@gmail.com
 * @date 2015-04-20
 */
public class ModButton extends JButton {
	private Image buttonImage;
	private JButton deleteButton;
	private JPanel bottomPanel;
	
	public static final int UNKNOWN_ACTION = 0;
	public static final int LAUNCH_MOD = 1;
	public static final int DELETE_MOD = 2;

	public ModButton() {
		super();
		
		setLayout(new BorderLayout());
		setRolloverEnabled(true);
		
		bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.setOpaque(false);
		bottomPanel.setBackground(new Color(0, 0, 0, 0));
		this.add(bottomPanel, BorderLayout.SOUTH);
		
		deleteButton = new JButton();
		deleteButton.setText("Remove Mod");
		
		//bottomPanel.add(deleteButton, BorderLayout.EAST);
	}
	
	@Override
	public void addActionListener(ActionListener l) {
		//Apply it to both ourselves and the delete button
		super.addActionListener(l);
		deleteButton.addActionListener(l);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		//Apply it to both ourselves and the delete button
		super.setEnabled(enabled);
		deleteButton.setEnabled(enabled);
	}
	
	/**
	 * Set the button's image
	 * @param image The image to set
	 */
	public void setImage(final Image image) {
		this.buttonImage = image;
		this.revalidate();
	}
	
	public BufferedImage generateBlurImage(int radius) { 
		float data[] = new float[radius * radius];
		float each = (1.0f / (radius * radius));
		for (int i = 0; i < radius * radius; i ++)
			data[i] = each;
		
		System.out.println("Generate blurred image of size " + buttonImage.getWidth(null) + " x " + buttonImage.getHeight(null));
		
		BufferedImage tmpSrc = new BufferedImage(buttonImage.getWidth(null), buttonImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = tmpSrc.createGraphics();
		g.drawImage(buttonImage, 0, 0, null);
		g.dispose();
		
		BufferedImage blurImage = new BufferedImage(buttonImage.getWidth(null), buttonImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Kernel kernel = new Kernel(radius, radius, data);
		ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		convolve.filter(tmpSrc, blurImage);
		
		return blurImage;
	}
	
	/**
	 * Get the button's image
	 * @return The button's image
	 */
	public Image getImage() {
		return this.buttonImage;
	}
	
	/**
	 * Get the action associated for the object given
	 * @param object The object whose action to determine 
	 * @return The action code for the object
	 */
	public int getAction(Object object) {
		if (object == this) {
			return ModButton.LAUNCH_MOD;
		}
		if (object == this.deleteButton) {
			return ModButton.DELETE_MOD;
		}
		return ModButton.UNKNOWN_ACTION;
	}
	
	@Override
	public void paintComponent(Graphics g) {
		if (buttonImage != null) {
	        //Get some base constraints to play with
	        Dimension dim = getSize();
	        Dimension imageSize = new Dimension(buttonImage.getWidth(null), buttonImage.getHeight(null));
	        Dimension superDim = getParent().getSize();
	        Point pos = getLocation();
	        
	        //X position is the center of the image
	        int xoff = (imageSize.width / 2) - (dim.width / 2);
	        //But then we offset it slightly based on the button's X position, so
	        // the leftmost button will have its image slightly to the left.
	        xoff += (pos.x - (superDim.width / 2) + (dim.width / 2)) / getParent().getComponents().length;
	        //Then we normalize it so we don't go offscreen
	        xoff = Math.max(xoff, 0);
	        xoff = Math.min(xoff, imageSize.width - dim.width);
	
	        //Y position is basic, just the center
	        int yoff = (imageSize.height / 2) - (dim.height / 2);
	        //Make sure we don't go off
	        yoff = Math.max(yoff, 0);
	        
	        //If the screen is larger than the image, we need to scale the image up. Only height matters here.
	        float scale = (dim.height > imageSize.height ? ((float)imageSize.height / (float)dim.height) : 1.0f);
	        //The scale is applied to both width and height so we don't get stretching.
	        Dimension sourceDim = new Dimension((int)((float)dim.width * scale), (int)((float)dim.height * scale));
	        
	        //Final rectangles for image source and destination
	        Rectangle sourceRect = new Rectangle(new Point(xoff, yoff), sourceDim);
	        Rectangle destinationRect = new Rectangle(new Point(0, 0), dim);        
	
	        //Draw the image on the screen
	        Image image = buttonImage;
	        g.drawImage(image, destinationRect.x, destinationRect.y, destinationRect.x + destinationRect.width, destinationRect.y + destinationRect.height,
	        		sourceRect.x, sourceRect.y, sourceRect.x + sourceRect.width, sourceRect.y + sourceRect.height, null);

	        if (getModel().isArmed()) {
	        	g.setColor(new Color(0, 0, 0, 100));
		        g.fillRect(destinationRect.x, destinationRect.y, destinationRect.x + destinationRect.width, destinationRect.y + destinationRect.height);
	        } else if (getModel().isRollover()) {
	        	g.setColor(new Color(0, 0, 0, 50));
	        	g.fillRect(destinationRect.x, destinationRect.y, destinationRect.x + destinationRect.width, destinationRect.y + destinationRect.height);
	        }

	        //Draw gray over the image if we're disabled
	        if (!isEnabled()) {
	        	g.setColor(new Color(255, 255, 255, 120));
	        	g.drawRect(0, 0, getWidth(), getHeight());
	        }
		}
		
        setBorderPainted(false);
        setContentAreaFilled(false);
        
        //Get font size and boundaries
        Rectangle bounds = getBounds();
        int fontSize = (bounds.width) / 14; //Warning: Magic number for font size
        
        //Text layout which will render our font for us
        FontRenderContext frc = new FontRenderContext(null, true, true);
        TextLayout layout = new TextLayout(getText(), new Font(Font.SANS_SERIF, Font.BOLD, fontSize), frc);

        //Where are we actually drawing the text 
        Rectangle textBounds = layout.getPixelBounds(frc, 0, 0);
        Point p = new Point((bounds.width - textBounds.width) / 2, (bounds.height + textBounds.height) / 2);

        //Draw a shadow behind the text
        g.setColor(Color.BLACK);
        layout.draw((Graphics2D)g, p.x + 1, p.y + 1); //Warning: Magic number for shadow size
        
        //Draw the actual text itself. Use a gray for when we've clicked the button
        g.setColor(Color.WHITE);
        layout.draw((Graphics2D)g, p.x, p.y);

        //Draw any subviews (delete, etc)
        super.paintChildren(g);
	}
}
