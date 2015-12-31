package com.jcity.roads.gui;

import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.*;

@SuppressWarnings("serial")
public class RoadmapPanel extends JPanel implements KeyListener, MouseListener {

	protected static Logger log = Logger.getLogger(RoadmapPanel.class);

	/**
	 * Create a new panel
	 */
	protected RoadmapPanel() {

	}

	protected void setupGui(double width, double height) {
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.addKeyListener(this);
		f.addMouseListener(this);
		f.add(this);
		f.setLocationRelativeTo(null);
		f.setSize((int) width, (int) height);
		f.setVisible(true);
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	public void keyReleased(KeyEvent e) {

		if (e.getKeyCode() == 27) {
			System.exit(10);
		}

	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}
}
